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
    <version>1.1.2</version>
</dependency>
```
### Explore

```text
http://localhost:8080/retreever
```

**Read Detailed Docs Here:** https://docs.retreever.dev

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

| Annotation       | Purpose                                                                       |
|------------------|-------------------------------------------------------------------------------|
| `@ApiDoc`        | API name, description, and version                                            |
| `@ApiGroup`      | Controller grouping                                                           |
| `@ApiEndpoint`   | Endpoint name, description, status, security flag, headers, and mapped errors |
| `@ApiError`      | Exception-handler status and description                                      |
| `@FieldInfo`     | Field description and example                                                 |
| `@Description`   | Parameter or field description                                                |
| `@RetreeverSkip` | Excludes a controller or endpoint from Retreever documentation                |

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

### Exclude Endpoints From Documentation

Use `@RetreeverSkip` on a controller class or method when endpoints must never
appear in Retreever documentation, even if they also have `@ApiEndpoint`.

```java
@RetreeverSkip
@RestController
@RequestMapping("/internal")
class InternalController {

    @GetMapping("/reindex")
    void reindex() {
    }
}
```

Host applications can also exclude paths centrally:

```yaml
retreever:
  docs:
    skip:
      - /internal/reindex
      - /users/{userId}
      - /admin/**
      - 'regex:^/reports/[{]reportId[}]/export$'
```

`retreever.docs.skip` accepts exact paths, Spring/Ant-style patterns such as
`/users/{userId}` and `/admin/**`, and regex entries prefixed with `regex:`.
Regex entries are evaluated against Retreever's resolved endpoint path template.
This gives teams a compliance control for keeping sensitive or operational
endpoints out of generated API documentation without changing the endpoints
themselves.

### Retreever Auth

Set both username and password to protect Retreever's internal APIs with
Retreever's built-in static authentication:

```yaml
retreever:
  auth:
    username: Admin
    password: Admin@123
    secret: 123e4567-e89b-12d3-a456-426614174000
```

`secret` signs Retreever auth tokens. Set the same UUID on every instance when
the app runs behind load balancing or across restarts where existing Retreever
sessions should remain valid. If omitted, Retreever generates a startup-only
secret and old sessions become invalid after restart.

Retreever auth cookies use the `Secure` attribute by default. In case a local
HTTP-only development setup does not retain the Retreever login session, use
`retreever.auth.secure-cookies=false` temporarily for that local setup only.

If username or password is missing and no host authenticator bean exists,
Retreever auth is disabled. Full auth behavior is documented in
[Documentation/Retreever-Auth-API.md](Documentation/Retreever-Auth-API.md).

Host applications can also provide their own login validation by registering a
single `RetreeverAuthenticator` bean. When this bean exists, it takes precedence
over `retreever.auth.username` and `retreever.auth.password`; Retreever still
issues and validates its own HttpOnly auth cookies after successful login.

```java
@Bean
RetreeverAuthenticator retreeverAuthenticator(HostUserService users) {
    return request -> {
        boolean valid = users.authenticate(request.principal(), request.credential());
        if (!valid) {
            return RetreeverAuthenticationResult.unauthenticated();
        }
        return RetreeverAuthenticationResult.authenticated(request.principal());
    };
}
```

The `/retreever/login` HTTP payload remains `{ "username": "...", "password": "..." }`
for compatibility. Retreever maps those values to the host-auth request as
`principal` and `credential`.

### Environment Variables

Static values:

```yaml
retreever:
  env:
    variable:
      name: device-id
      value: device-web-001
```

Properties form: `retreever.env.variable.name` and
`retreever.env.variable.value`.

Values resolved from API responses:

```yaml
retreever:
  env:
    variables:
      - name: access-token
        from:
          endpoints:
            - '[POST] /api/v1/public/login'
            - '[GET] /api/v1/public/login/refresh'
          extract:
            - '[BODY] data.access_token'
            - '[BODY] accessToken'

      - name: session-id
        from:
          endpoints:
            - '[POST] /api/v1/public/login'
          extract:
            - '[HEADER] X-Session-ID'
            - '[HEADER] x-session-id'
```

## Compatibility

- Spring Boot `3.x`
- Java `17+`

## License

MIT
