# webflux-antipattern

A demo repository that teaches common Spring WebFlux (Project Reactor) antipatterns, chiefly around **multi-tenant
context propagation**. Each feature is implemented twice - once the wrong way, once the
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

Read the test names and comments alongside the production code for the full explanation of *why*
each antipattern is wrong - the tests double as executable documentation.

**The fixes**

- **1-3:** carry the tenant in Reactor's `Context` and read it lazily with `Mono.deferContextual(...)`,
  so it is resolved per subscription, whichever thread runs the work.
- **4:** defer the call with `Mono.fromCallable(...)` and run it on a scheduler with `subscribeOn(...)`.

The `recommended` tests show the fixes hold, for example
`service/recommended/UserServiceImplTest#shouldNotLeakTenantAcrossReusedWorkerThread` and
`service/recommended/TenantServiceImplTest#shouldNotCallRepositoryIfNotSubscribed`.
Antipatterns 1 and 2 can also be watched over real HTTP:
see [Demonstrate antipattern command](#demonstrate-antipattern-command).

## Running the app

Pick the wiring with a profile. With none given, `application.yaml` defaults to `antipattern`
(`spring.profiles.default`), so a plain `./mvnw spring-boot:run` runs the **antipattern** code.

Requests need an `X-Tenant-ID` header holding a numeric tenant id, for example
`curl -H 'X-Tenant-ID: 1' localhost:8080/users`. Paths in `TenantIdentifierExclusionPolicy` do not:
`/tenants`, `/actuator/health`, `/v3/api-docs`, and Swagger UI at `/swagger-ui.html`. A missing, blank
or non-numeric header gets `400 Bad Request`; `GET /users/{id}` gets `404 Not Found` if the tenant has
no such user.

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

> **Note:** Libraries that keep contextual information in a `ThreadLocal`, such as logging MDCs and distributed
> tracing frameworks, run into the same thread-hop problem. Reactor can capture and restore such values across
> reactive execution boundaries once the appropriate accessors are registered. See
> [Configuring Thread Local Context Propagation](docs/context-propagation.md).

## Demonstrate recommended command

```bash
$ ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=recommended
```

Then execute requests for alternating tenants and verify that each returns only its own users:

```bash
curl -s -H 'X-Tenant-ID: 1' localhost:8080/users
curl -s -H 'X-Tenant-ID: 2' localhost:8080/users
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

## Documentation

- [Package layout](docs/package-layout.md)
- [API docs and Swagger UI](docs/api-docs.md)
