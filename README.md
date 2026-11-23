# webflux-antipattern

A demo repository that teaches common Spring WebFlux (Project Reactor) antipatterns around **multi-tenant context
propagation** - by implementing the same feature twice: once the wrong
way, once the recommended way, switchable at runtime via a Spring profile.

The core lesson: WebFlux execution hops across threads (`subscribeOn`, thread pools, event-loop
workers), so anything stored per-thread - like Java's `ThreadLocal` - cannot reliably carry
request-scoped state (such as the current tenant) through a reactive pipeline. Reactor's own
`Context` can, because it travels with the subscription instead of the thread.

## Antipatterns covered

All of these live in the `antipattern`-profiled code paths (`filter.antipattern.*`,
`service.antipattern.*`); each has a recommended fix in the matching `recommended` package, and
a test that reproduces it deterministically, not just a claim that it's wrong:

1. **`ThreadLocal`/`InheritableThreadLocal` for request-scoped state in a reactive pipeline.**
   `TenantIdentifierFilter` (antipattern) stores the tenant in a `ThreadLocal` and relies on it
   still being there once execution continues - but WebFlux execution hops threads (`subscribeOn`, pooled schedulers),
   and a plain `ThreadLocal` does not travel with it.
2. **Stale data from reusing a pooled thread's inherited context.** Even with an
   `InheritableThreadLocal`, a worker thread only copies the value once, at the moment it's
   created. `UserServiceImpl` (antipattern) subscribes on a pooled `Scheduler`
   (`Schedulers.boundedElastic()`); a worker that inherited tenant A's context can keep serving
   tenant A's data forever, even once the real request thread has moved on to tenant B -
   returning the *wrong tenant's data*, not an error. Reproduced deterministically in
   `service/antipattern/UserServiceImplTest#shouldReturnStaleTenantIdWhenPooledWorkerThreadIsReusedAcrossRequests`.
3. **Eager evaluation of per-subscription state.** `UserServiceImpl.getUser(long id)`
   (antipattern) reads the tenant context synchronously the moment the method is called, before
   the returned `Mono` is even built - not deferred until something actually subscribes to it. If
   the context changes between that call and the eventual subscription, the stale, already-baked-in
   tenant is what gets used. Reproduced in
   `service/antipattern/UserServiceImplTest#shouldResolveTenantIdEagerlyAtCallTimeNotAtSubscriptionTime`.

The fix for all three is the same underlying idea: carry request-scoped state in Reactor's
`Context` (which propagates with the subscription, not the thread) and resolve it lazily, inside
`Mono.defer(...)` / `Mono.deferContextual(...)`, so it's read at subscription time - see the
`recommended` package equivalents.

## Package layout

Most antipattern-vs-recommended pairs live side by side under mirrored `antipattern` /
`recommended` sub-packages, selected by Spring profile (`@AntiPattern` / `@Recommended`, both
`@Profile`-gated annotations):

- `filter.antipattern.TenantIdentifierFilter` / `filter.recommended.TenantIdentifierFilter` -
  a `WebFilter` that reads the `X-Tenant-ID` header and makes it available downstream.
    - **Antipattern**: stores the tenant in a `ThreadLocal`/`InheritableThreadLocal`
      (`ctx.TenantContextHolder`), then calls `chain.filter(exchange)`. Once a downstream operator
      hops threads (e.g. `subscribeOn`), the value is gone - or worse, stale (see below).
    - **Recommended**: propagates the tenant via `contextWrite(...)` onto the Reactor `Context`,
      which flows with the subscription regardless of which thread actually executes the work.
- `service.antipattern.UserServiceImpl` / `service.recommended.UserServiceImpl` - looks up users
  for the current tenant.
    - **Antipattern**: reads `TenantContextHolder.getContext()` and calls
      `Schedulers.boundedElastic()` directly. This both reads the ThreadLocal eagerly (bugs if
      reading happens before the actual subscription) and reuses pooled worker threads, so a worker
      that inherited tenant A can keep serving tenant A's data forever, even after the real request
      thread has moved on to tenant B. `getUser(long id)` additionally resolves the tenant *synchronously at call time*
      instead of deferring it until subscription - a second, distinct
      antipattern (eager vs. lazy evaluation) called out in its own code comment.
    - **Recommended**: reads the tenant out of the Reactor `Context` lazily, via
      `Mono.deferContextual(...)`, so resolution always happens per-subscription, not per-call, and
      is immune to thread hand-off.
- `ctx.TenantContext` / `ctx.TenantContextHolder` - the `ThreadLocal`-based holder used by the
  antipattern code paths.
- `resource` - thin REST controllers (`UserResource`, `TenantResource`) that simply delegate to
  the services above. `/tenants` is in `TenantIdentifierFilter`'s `EXCLUDED_PATHS`, so the tenant
  list itself is browsable without an `X-Tenant-ID` header; `/users` is not excluded, so it
  requires one.
- `config.SchedulerConfig` - supplies the production `Scheduler` bean (`Schedulers.boundedElastic()`),
  injected into both `UserServiceImpl` variants rather than hardcoded, so tests can swap in a
  deterministic, test-owned scheduler instead of depending on the shared, JVM-wide
  `Schedulers.boundedElastic()` singleton (see `UserServiceImplTest` and `UserResource_IT` below).
- `annotation.AntiPattern` / `annotation.Recommended` - the two `@Profile`-gated marker
  annotations used throughout to select which implementation is active; `AntiPatternTest` /
  `RecommendedTest` just confirm their Spring metadata (`@Profile` value, `@Order`) is as
  expected.

Read the test names and their comments alongside the production code comments for the full
explanation of *why* each antipattern is wrong, not just *that* it's marked as one - the tests
are written to double as executable documentation of the bug they're paired with.

## Demonstrate antipattern command

```bash
$ ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=antipattern
```

## Demonstrate recommended command

```bash
$ ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=recommended
```

## Running the tests

Unit tests (fast, no Spring context, run on every build):

```bash
$ ./mvnw test
```

Integration tests - classes suffixed `_IT` - boot a real Spring context (and, for `UserResource_IT`,
a real HTTP server plus the H2 database seeded from `data.sql`). They're wired through Failsafe and
only run via the `integration-tests` Maven profile, so they never slow down or affect `./mvnw test`:

```bash
$ ./mvnw verify -Pintegration-tests
```

- `WebfluxAntipatternApplication_IT` - a basic context-loads smoke test (`recommended` profile).
- `resource/UserResource_IT` - boots the app once per profile (`@Nested` + `@ActiveProfiles`) and
  drives `/users` and `/users/{id}` over real HTTP with `WebTestClient`, proving functional parity:
  for an ordinary request, both profiles return the same, correct data for the tenant in the
  `X-Tenant-ID` header. Its `AntipatternProfile` nested class overrides the `Scheduler` bean with a
  dedicated, test-owned one (see the comment on `DeterministicSchedulerTestConfig` in that file) -
  otherwise this test would depend on the real, JVM-wide `Schedulers.boundedElastic()` singleton
  shared with every other Spring context and test in the same run. It deliberately does not attempt
  to reproduce the stale-tenant bug over HTTP (every call here uses a single tenant); that's covered
  deterministically at the unit level instead, in `service/antipattern/UserServiceImplTest`.