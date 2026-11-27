# API docs and Swagger UI

springdoc (`springdoc-openapi-starter-webflux-ui`) generates an OpenAPI 3.1 document and a Swagger UI
from the controllers, under both profiles:

| URL                | What                                                        |
|--------------------|-------------------------------------------------------------|
| `/swagger-ui.html` | Swagger UI (redirects to `/swagger-ui/index.html`)          |
| `/v3/api-docs`     | The OpenAPI document as JSON (`/v3/api-docs.yaml` for YAML) |

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
