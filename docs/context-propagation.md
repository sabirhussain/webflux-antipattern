# Configuring Thread Local Context Propagation

Some libraries rely on `ThreadLocal` storage to hold contextual information. Common examples include logging MDCs,
distributed tracing frameworks, and legacy framework integrations.

Since reactive pipelines frequently switch threads, `ThreadLocal` values are not automatically available throughout
request processing. To bridge this gap, Reactor can integrate with the `micrometer-context-propagation` library,
allowing registered `ThreadLocal` values to be captured and restored across reactive execution boundaries.

To see this in action, start the application with both the anti-pattern and thread-local propagation profiles enabled:

```bash
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=antipattern,thread-local-propagation
```

Then execute requests for alternating tenants:

```bash
curl -s -H 'X-Tenant-ID: 1' localhost:8080/users
curl -s -H 'X-Tenant-ID: 2' localhost:8080/users
```

With thread-local propagation enabled:

- Registered `ThreadLocal` values are captured and restored automatically across reactive thread switches.
- `TenantContextMissingException` no longer occurs.
- Requests consistently return data for the correct tenant.
- Existing `ThreadLocal`-based components can continue to operate with minimal or no code changes after the appropriate
  accessors are registered.

## Enabling Micrometer Context Propagation

The following steps enable Micrometer Context Propagation for custom ThreadLocal values within a Spring WebFlux
application.

### 1. Add the following dependency in your `../pom.xml` file:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>context-propagation</artifactId>
</dependency>
```

### 2. Enable automatic context propagation

```java
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
class ReactorHooksConfig {

    @PostConstruct
    void enableHooks() {
        Hooks.enableAutomaticContextPropagation();
    }
}
```

### 3. Register a ThreadLocalAccessor

```java
import com.hcltech.sample.reactive.webflux.ctx.TenantContextHolder;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
class ContextPropagationConfig {

    @PostConstruct
    void registerThreadLocalAccessors() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor("TenantContext", TenantContextHolder::getContext,
                        TenantContextHolder::setContext, TenantContextHolder::removeContext);
    }
}
```

## Why Reactor Context Is Still Preferred

Although context propagation makes `ThreadLocal`-based code work in reactive applications, it is primarily intended for
interoperability with existing libraries that depend on `ThreadLocal`.

For application-specific state such as tenant identifiers, or business context, Reactor's `Context` remains the
preferred approach because it is:

- Explicit rather than implicit.
- Immutable rather than mutable.
- Native to Reactor's programming model.
- Free from `ThreadLocal` registration and lifecycle concerns.
- Easier to reason about and test.
