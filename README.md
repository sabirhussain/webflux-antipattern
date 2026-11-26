# webflux-antipattern

A demo repository that teaches common Spring WebFlux (Project Reactor) antipatterns, chiefly around
**multi-tenant context propagation**. Each feature is implemented twice - once the wrong way, once the
recommended way - and a Spring profile (`antipattern` or `recommended`) picks which one runs.

The core lesson: WebFlux work hops between threads (`subscribeOn`, thread pools, event-loop workers),
so per-thread state such as a `ThreadLocal` cannot reliably carry request-scoped data (here, the
current tenant). Reactor's `Context` can, because it travels with the subscription instead of the thread.

## Antipatterns covered

Each antipattern lives in an `antipattern` package and has a fix in the matching `recommended`
package. Each is reproduced by a test, except part (c) of antipattern 4. The tests named below are
in `src/test/.../service/antipattern/`.

1. **`ThreadLocal` for request-scoped state.** The antipattern `TenantIdentifierFilter` stores the
   tenant in a `ThreadLocal`/`InheritableThreadLocal`. After a thread hop (`subscribeOn`, a pooled
   scheduler) a plain `ThreadLocal` value is simply gone, so the service finds no tenant.
   Test: `UserServiceImplTest#shouldReturnErrorEvenWhenTenantContextIsSetOnCallingThread`
2. **Stale data from a reused pooled thread.** An `InheritableThreadLocal` is copied to a worker
   thread once, when the thread is *created*. The antipattern `UserServiceImpl` subscribes on a pooled
   scheduler (`userServiceScheduler`), so a worker that inherited tenant A keeps serving tenant A's
   data after the request has moved on to tenant B: the *wrong tenant's data*, not an error.
   Test: `UserServiceImplTest#shouldReturnStaleTenantIdWhenPooledWorkerThreadIsReusedAcrossRequests`
3. **Eager evaluation of per-subscription state.** The antipattern `UserServiceImpl.getUser(long)`
   reads the tenant when the method is *called*, not when the `Mono` is *subscribed to*. If the
   context changes in between, the stale tenant is used.
   Test: `UserServiceImplTest#shouldResolveTenantIdEagerlyAtCallTimeNotAtSubscriptionTime`
4. **Blocking, eager, single-use `Flux`.** The antipattern `TenantServiceImpl.getAllTenants()` calls
   the blocking `findAll()` and wraps the result in `Flux.fromStream(...)`. So (a) the query runs when
   the method is called, not on subscription; (b) a `Stream` can be consumed only once, so a second
   subscription throws `IllegalStateException`; (c) the blocking call runs on the caller's thread,
   which in WebFlux is an event-loop thread.
   Tests: `TenantServiceImplTest#shouldCallRepositoryBeforeSubscription` for (a) and
   `TenantServiceImplTest#shouldFailSecondSubscriptionOnGetAllTenants` for (b). (c) is not tested.

**The fixes**

- **1-3:** carry the tenant in Reactor's `Context` and read it lazily with `Mono.deferContextual(...)`,
  so it is resolved per subscription, whichever thread runs the work.
- **4:** defer the call with `Mono.fromCallable(...)` and run it on a scheduler with `subscribeOn(...)`.

The `recommended` tests show the fixes hold, for example
`service/recommended/UserServiceImplTest#shouldNotLeakTenantAcrossReusedWorkerThread` and
`service/recommended/TenantServiceImplTest#shouldNotCallRepositoryIfNotSubscribed`.
Antipatterns 1 and 2 can also be watched over real HTTP:
see [Demonstrate antipattern command](#demonstrate-antipattern-command).

## Package layout

Antipattern/recommended pairs sit in mirrored `antipattern` / `recommended` sub-packages. Two
`@Profile`-gated annotations, `@AntiPattern` and `@Recommended`, select them by profile. Both tenant
filters run at `@Order(Ordered.HIGHEST_PRECEDENCE)`, read the `X-Tenant-ID` header, and answer
`400 Bad Request` if it is missing, blank or not a number.

| Pair | Antipattern | Recommended |
|---|---|---|
| `filter.{antipattern,recommended}.TenantIdentifierFilter` | Stores the tenant in `ctx.TenantContextHolder` (a `ThreadLocal`). | Puts it on the Reactor `Context` with `contextWrite(...)`. |
| `service.{antipattern,recommended}.UserServiceImpl` | Reads the `ThreadLocal` and subscribes on a pooled scheduler; `getUser` resolves the tenant eagerly. | Reads the tenant lazily from the Reactor `Context`; blocking repository calls run on `userServiceScheduler`. |
| `service.{antipattern,recommended}.TenantServiceImpl` | See antipattern 4. | `Mono.fromCallable(findAll)` on `tenantServiceScheduler`. |

Shared classes:

- `filter.policy.TenantIdentifierExclusionPolicy` - paths that skip the tenant check: `/swagger-ui`,
  `/swagger-ui.html`, `/v3/api-docs`, `/actuator` and `/tenants` (prefix match, hard-coded for now).
- `filter.HeaderResolver` - reads a request header; a missing or blank value counts as absent.
- `ctx.TenantContext` - the tenant value object (on the recommended path, it is what is stored in the
  Reactor `Context`). `ctx.TenantContextHolder` - the `ThreadLocal` holder, used only by antipattern code.
- `resource` - thin controllers `UserResource` and `TenantResource`. `/tenants` is excluded, so it works
  without the header; `/users` needs it. `UserResource` answers `404 Not Found` when the tenant has no
  such user.
- `config.SchedulerConfig` - declares two owned bounded-elastic pools as beans, `userServiceScheduler`
  and `tenantServiceScheduler` (`Schedulers.newBoundedElastic(10, 1_000, ...)`: up to 10 threads and
  1,000 queued tasks), disposed with the context. They are injected, not hard-coded, into both
  `UserServiceImpl` variants and the recommended `TenantServiceImpl`, so tests can substitute their
  own scheduler.
- `config.OpenAPIConfig` - declares the `X-Tenant-ID` OpenAPI parameter; see
  [API docs and Swagger UI](#api-docs-and-swagger-ui).
- `annotation.AntiPattern` / `annotation.Recommended` - the two profile markers; `AntiPatternTest` and
  `RecommendedTest` only check the profile name.

Read the test names and comments alongside the production code for the full explanation of *why*
each antipattern is wrong - the tests double as executable documentation.

## Running the app

Pick the wiring with a profile. With none given, `application.yaml` defaults to `antipattern`
(`spring.profiles.default`), so a plain `./mvnw spring-boot:run` runs the **antipattern** code.

Requests need an `X-Tenant-ID` header holding a numeric tenant id, for example
`curl -H 'X-Tenant-ID: 1' localhost:8080/users`. Paths in `TenantIdentifierExclusionPolicy` do not:
`/tenants`, `/actuator/health`, `/v3/api-docs`, and Swagger UI at `/swagger-ui.html`. A missing, blank
or non-numeric header gets `400 Bad Request`; `GET /users/{id}` gets `404 Not Found` if the tenant has
no such user.

## API docs and Swagger UI

springdoc (`springdoc-openapi-starter-webflux-ui`) generates an OpenAPI 3.1 document and a Swagger UI
from the controllers, under both profiles:

| URL | What |
|---|---|
| `/swagger-ui.html` | Swagger UI (redirects to `/swagger-ui/index.html`) |
| `/v3/api-docs` | The OpenAPI document as JSON (`/v3/api-docs.yaml` for YAML) |

Both are in `TenantIdentifierExclusionPolicy`, so they open without an `X-Tenant-ID` header.

`/users` and `/users/{userId}` do need that header, so `config.OpenAPIConfig` declares it once as a
reusable parameter named `TenantHeader` (required, `in: header`, example `1`), and `UserResource`
references it on each operation:

```java
@Operation(parameters = {@Parameter(ref = "#/components/parameters/TenantHeader")})
```

Add the same line to any new endpoint that needs a tenant. `/tenants` needs no header and does not
declare it.

Good to know: the document keeps springdoc's default title (`OpenAPI definition`, `v0`), lists only the
`200` response (not the `400` and `404` the endpoints can return), and does not declare the header's
type. The server itself requires a number and answers `400` otherwise.

## Demonstrate antipattern command

```bash
$ ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=antipattern
```

Then ask for the users of alternating tenants:

```bash
$ curl -s -H 'X-Tenant-ID: 1' localhost:8080/users
$ curl -s -H 'X-Tenant-ID: 2' localhost:8080/users
```

Which pooled worker thread serves a request is not deterministic, so results can vary. With sequential
requests you will typically see the tenant 2 request return **tenant 1's users with a `200`**
(antipattern 2: wrong data, no error). You may also see a `500` (`TenantContextMissingException`: the
context did not survive the thread hop, antipattern 1). Under the `recommended` profile each request
returns only the users of the tenant named in its header.

## Demonstrate recommended command

```bash
$ ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=recommended
```

## Running the tests

Unit tests (fast, no Spring context):

```bash
$ ./mvnw test
```

Integration tests - classes ending in `_IT` - start a real Spring context. They run through Failsafe and
only with the `integration-tests` Maven profile, so they never slow down `./mvnw test`:

```bash
$ ./mvnw verify -Pintegration-tests
```

- `WebfluxAntipatternApplication_IT` - checks that the context loads (`recommended` profile).
- `resource/UserResource_IT` - starts the app once per profile (`@Nested` + `@ActiveProfiles`) with a
  real HTTP server and the H2 database seeded from `data.sql`, and calls `/users` and `/users/{id}` with
  `WebTestClient`. Both profiles must behave the same: the right tenant's users, `400` for a missing or
  invalid header, `404` for an unknown user. In the `antipattern` profile the scheduler bean is replaced
  by a single-thread one (see `DeterministicSchedulerTestConfig`), because the production pool reuses
  threads and would make that run non-deterministic. It does not try to reproduce the stale-tenant bug
  over HTTP (one tenant per call); `service/antipattern/UserServiceImplTest` does that at unit level.
