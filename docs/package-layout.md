# Package layout

Antipattern/recommended pairs sit in mirrored `antipattern` / `recommended` sub-packages. Two
`@Profile`-gated annotations, `@AntiPattern` and `@Recommended`, select them by profile. Both tenant
filters run at `@Order(Ordered.HIGHEST_PRECEDENCE)`, read the `X-Tenant-ID` header, and answer
`400 Bad Request` if it is missing, blank or not a number.

| Pair                                                      | Antipattern                                                                                          | Recommended                                                                                                  |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `filter.{antipattern,recommended}.TenantIdentifierFilter` | Stores the tenant in `ctx.TenantContextHolder` (a `ThreadLocal`).                                    | Puts it on the Reactor `Context` with `contextWrite(...)`.                                                   |
| `service.{antipattern,recommended}.UserServiceImpl`       | Reads the `ThreadLocal` and subscribes on a pooled scheduler; `getUser` resolves the tenant eagerly. | Reads the tenant lazily from the Reactor `Context`; blocking repository calls run on `userServiceScheduler`. |
| `service.{antipattern,recommended}.TenantServiceImpl`     | See antipattern 4.                                                                                   | `Mono.fromCallable(findAll)` on `tenantServiceScheduler`.                                                    |

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
  [API docs and Swagger UI](api-docs.md).
- `annotation.AntiPattern` / `annotation.Recommended` - the two profile markers; `AntiPatternTest` and
  `RecommendedTest` only check the profile name.
