# Graph Report - webflux-antipattern (2026-11-26)

## Corpus Check

- 25 files · ~8,346 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary

- 358 nodes · 951 edges · 15 communities (11 shown, 4 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 33 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)

- Metadata & Basic Unit Tests
- Tenant Filters & Exclusion Policy
- Service Unit Tests
- Integration & Filter Tests
- REST Resources & DTOs
- Tenant Context Holder
- README Antipattern Concepts
- Profile Annotations & Tenant Services
- JPA Entities & Repositories
- Scheduler & OpenAPI Config
- Application YAML Config
- Spring Boot Entry Point
- CreateTenantRequest DTO
- Maven POM Coordinates

## God Nodes (most connected - your core abstractions)

1. `TenantContext` - 18 edges
2. `User` - 17 edges
3. `UserServiceImplTest` - 16 edges
4. `UserServiceImplTest` - 14 edges
5. `UserServiceImpl` - 13 edges
6. `TenantIdentifierFilterTest` - 13 edges
7. `TenantIdentifierFilterTest` - 13 edges
8. `TenantServiceImplTest` - 12 edges
9. `AntiPattern` - 11 edges
10. `Recommended` - 11 edges

## Surprising Connections (you probably didn't know these)

- `Sample` --references--> `AntiPattern`  [EXTRACTED]
  src/test/java/com/hcltech/sample/reactive/webflux/annotation/AntiPatternTest.java →
  src/main/java/com/hcltech/sample/reactive/webflux/annotation/AntiPattern.java
- `Sample` --references--> `Recommended`  [EXTRACTED]
  src/test/java/com/hcltech/sample/reactive/webflux/annotation/RecommendedTest.java →
  src/main/java/com/hcltech/sample/reactive/webflux/annotation/Recommended.java
- `UserServiceImplTest` --references--> `UserRepository`  [EXTRACTED]
  src/test/java/com/hcltech/sample/reactive/webflux/service/antipattern/UserServiceImplTest.java →
  src/main/java/com/hcltech/sample/reactive/webflux/repository/UserRepository.java
- `UserServiceImplTest` --references--> `UserServiceImpl`  [EXTRACTED]
  src/test/java/com/hcltech/sample/reactive/webflux/service/antipattern/UserServiceImplTest.java →
  src/main/java/com/hcltech/sample/reactive/webflux/service/antipattern/UserServiceImpl.java
- `TenantResourceTest` --references--> `TenantResource`  [EXTRACTED]
  src/test/java/com/hcltech/sample/reactive/webflux/resource/TenantResourceTest.java →
  src/main/java/com/hcltech/sample/reactive/webflux/resource/TenantResource.java

## Import Cycles

- None detected.

## Hyperedges (group relationships)

- **Antipattern-profile code path (ThreadLocal-based tenant propagation)** — readme_tenantidentifierfilter_antipattern,
  readme_userserviceimpl_antipattern, readme_tenantcontextholder, readme_antipattern_annotation [EXTRACTED 1.00]
- **Recommended-profile code path (Reactor Context tenant propagation)** — readme_tenantidentifierfilter_recommended,
  readme_userserviceimpl_recommended, readme_tenantcontext, readme_reactor_context,
  readme_recommended_annotation [EXTRACTED 1.00]
- **Three tenant-context antipatterns and their shared Reactor Context / lazy-resolution fix** —
  readme_threadlocal_antipattern, readme_stale_pooled_thread_context_antipattern, readme_eager_evaluation_antipattern,
  readme_reactor_context, readme_lazy_resolution_defer [EXTRACTED 1.00]

## Communities (15 total, 4 thin omitted)

### Community 0 - "Metadata & Basic Unit Tests"

Cohesion: 0.07
Nodes (10): AntiPatternTest, Sample, RecommendedTest, Sample, TenantContextTest, TenantContextMissingExceptionTest,
TenantIdentifierFilterTest, TenantIdentifierFilterTest (+2 more)

### Community 1 - "Tenant Filters & Exclusion Policy"

Cohesion: 0.14
Nodes (5): TenantIdentifierFilter, HeaderResolver, TenantIdentifierExclusionPolicy, TenantIdentifierFilter,
UserServiceImpl

### Community 2 - "Service Unit Tests"

Cohesion: 0.12
Nodes (3): TenantContextMissingException, TenantResourceTest, TenantServiceImplTest

### Community 3 - "Integration & Filter Tests"

Cohesion: 0.10
Nodes (4): AntipatternProfile, RecommendedProfile, UserResource_IT, WebfluxAntipatternApplication_IT

### Community 4 - "REST Resources & DTOs"

Cohesion: 0.12
Nodes (6): TenantDto, TenantResource, UserResource, UserServiceImpl, TenantService, UserResourceTest

### Community 5 - "Tenant Context Holder"

Cohesion: 0.15
Nodes (4): TenantContext, TenantContextHolder, TenantContextHolderTest, UserServiceImplTest

### Community 6 - "README Antipattern Concepts"

Cohesion: 0.12
Nodes (25): annotation.AntiPattern, AntiPatternTest, Demonstrate antipattern command (curl with alternating
X-Tenant-ID), DeterministicSchedulerTestConfig, filter.HeaderResolver, integration-tests Maven profile (Failsafe, _IT
classes), Multi-tenant context propagation, annotation.Recommended (+17 more)

### Community 7 - "Profile Annotations & Tenant Services"

Cohesion: 0.16
Nodes (6): AntiPattern, Recommended, UserDto, TenantServiceImpl, TenantServiceImpl, UserService

### Community 8 - "JPA Entities & Repositories"

Cohesion: 0.18
Nodes (4): Tenant, User, TenantRepository, UserRepository

### Community 9 - "Scheduler & OpenAPI Config"

Cohesion: 0.20
Nodes (3): OpenAPIConfig, SchedulerConfig, DeterministicSchedulerTestConfig

### Community 10 - "Application YAML Config"

Cohesion: 0.17
Nodes (11): name, ddl-auto, mode, hibernate, default, spring, application, jpa (+3 more)

## Knowledge Gaps

- **7 isolated node(s):** `com.hcltech.sample.reactive:webflux-antipattern`, `TenantContextMissingException`,
  `resource.TenantResource`, `AntiPatternTest`, `RecommendedTest` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 37 node(s) total
  have ≤1 connection when file, concept and rationale nodes are included.)
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions

_Questions this graph is uniquely positioned to answer:_

- **Why does `User` connect `JPA Entities & Repositories` to `Metadata & Basic Unit Tests`,
  `Tenant Filters & Exclusion Policy`, `Service Unit Tests`, `REST Resources & DTOs`, `Tenant Context Holder`?**
  _High betweenness centrality (0.043) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `TenantContext` (e.g. with
  `.shouldClearInheritableContextWhenRegularContextIsSet()` and
  `.shouldClearRegularContextWhenInheritableContextIsSet()`) actually correct?**
  _`TenantContext` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.hcltech.sample.reactive:webflux-antipattern`, `TenantContextMissingException`,
  `resource.TenantResource` to the rest of the system?**
  _7 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Metadata & Basic Unit Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.07246376811594203 - nodes in this community are weakly interconnected._
- **Why does `Recommended` connect `Profile Annotations & Tenant Services` to `Metadata & Basic Unit Tests`,
  `Tenant Filters & Exclusion Policy`, `REST Resources & DTOs`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Should `Tenant Filters & Exclusion Policy` be split into smaller, more focused modules?**
  _Cohesion score 0.14169570267131243 - nodes in this community are weakly interconnected._
- **Why does `UserServiceImplTest` connect `Tenant Context Holder` to `JPA Entities & Repositories`,
  `Scheduler & OpenAPI Config`, `Service Unit Tests`, `Tenant Filters & Exclusion Policy`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._