# Retreever

Retreever is a Spring Boot library that turns your running application into a
live API documentation and testing workspace.

Add the dependency, start the app, open `/retreever`.

Retreever reads the code you already have: controllers, request and response
types, validation rules, headers, path variables, exception handlers, and nested
DTOs. No separate spec file is required.

## Install

```xml
<dependency>
    <groupId>dev.retreever</groupId>
    <artifactId>retreever</artifactId>
    <version>1.0.5</version>
</dependency>
```
### Explore

```text
http://localhost:8080/retreever
```

## What It Resolves

- `@RestController` endpoints
- request bodies and response types
- path variables, query parameters, and headers
- Jakarta Validation constraints
- `@RestControllerAdvice` and `@ExceptionHandler` responses
- records, nested DTOs, arrays, maps, and generic types
- optional Retreever metadata annotations

## Spring Security

If the host application uses Spring Security, allow Retreever routes through the
host security chain. Retreever's own optional auth runs inside the library after
that.

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(RetreeverPublicPaths.get()).permitAll()
                    .anyRequest().authenticated()
            )
            .build();
}
```

## Optional Annotations

Retreever works without annotations. Use them when you want cleaner grouping,
names, examples, or error documentation.

| Annotation | Purpose |
| --- | --- |
| `@ApiDoc` | API name, description, and version |
| `@ApiGroup` | Controller grouping |
| `@ApiEndpoint` | Endpoint name, description, status, security flag, headers, and mapped errors |
| `@ApiError` | Exception-handler status and description |
| `@FieldInfo` | Field description and example |
| `@Description` | Parameter or field description |

```java
@ApiGroup(name = "Product APIs")
@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    @ApiEndpoint(
            name = "Create Product",
            secured = true,
            headers = {HttpHeaders.AUTHORIZATION}
    )
    @PostMapping
    ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductRequest request) {
        return null;
    }
}
```

## Configuration

Configuration is optional. Retreever is designed to start with no YAML.

### Disable Retreever

```properties
retreever.enabled=false
```

This is a hard disable. Retreever registers no controllers, filters, resource
handlers, scanners, UI routes, or documentation endpoints. Requests to
`/retreever/**` fall through to the host application.

### Retreever Auth

Set both username and password to protect Retreever's internal APIs:

```yaml
retreever:
  auth:
    username: Admin
    password: Admin@123
    secret: 123e4567-e89b-12d3-a456-426614174000
    secure-cookies: true
```

`secret` signs Retreever auth tokens. Set the same UUID on every instance when
the app runs behind load balancing or across restarts where existing Retreever
sessions should remain valid. If omitted, Retreever generates a startup-only
secret and old sessions become invalid after restart.

`secure-cookies` adds the `Secure` attribute to Retreever auth cookies. Enable
it when Retreever is served over HTTPS.

If username or password is missing, Retreever auth is disabled. Full auth
behavior is documented in
[Documentation/Retreever-Auth-API.md](Documentation/Retreever-Auth-API.md).

### Environment Variables

Static values:

```yaml
retreever:
  env:
    variables:
      - name: device-id
        source:
          value: device-web-001
```

Values resolved from API responses:

```yaml
retreever:
  env:
    variables:
      - name: access-token
        source:
          request:
            endpoints:
              - /api/v1/public/login
              - /api/v1/public/login/refresh
            method: post
            response:
              body-attribute-paths:
                - data.access_token
                - accessToken

      - name: session-id
        source:
          request:
            endpoints:
              - /api/v1/public/login
            method: post
            response:
              header-attribute-paths:
                - X-Session-ID
                - x-session-id
```

A request-based variable resolves from body paths or header paths, not both.
`/retreever/environment` exposes response paths as arrays:
`body_attribute_paths` and `header_attribute_paths`.

## Compatibility

- Spring Boot `3.x`
- Java `17+`

## License

MIT
