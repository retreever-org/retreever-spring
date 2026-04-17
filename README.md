<div align="center">

![Retreever Banner](Documentation/Logo/retreever-banner.svg)

</div>

# Retreever

Retreever is a lightweight, developer-first toolkit for generating live API
documentation and API testing UI for Spring Boot applications.

It scans your controllers, request and response models, validation constraints,
and exception handlers to build documentation from the application code that is
already running.

Add the dependency, start the application, open `/retreever`.

## Zero Config First

Retreever is designed to work with zero configuration.

- No YAML is required
- No annotations are required
- No separate documentation layer is required

Annotations and properties are optional add-ons for improving documentation,
grouping, examples, auth, and testing experience.

The main integration case to handle manually is when the host application uses
Spring Security. In that case, Retreever's public routes need to be allowed.

## What Retreever Resolves

Retreever automatically reads and resolves:

- `@RestController` endpoints
- Request bodies and response types
- Path variables, query parameters, and headers
- Validation constraints from Jakarta Validation annotations
- Exception mappings from `@RestControllerAdvice` and `@ExceptionHandler`
- Nested DTOs, arrays, maps, records, and generic types
- Endpoint metadata such as name, description, status, and security flags

The generated document includes:

- API name, description, and version
- Endpoint groups
- HTTP method, path, consumes, and produces metadata
- Request and response schemas
- Parameter and header metadata
- Error responses

## Installation

Retreever is published to Maven Central.

```xml
<dependency>
    <groupId>dev.retreever</groupId>
    <artifactId>retreever</artifactId>
    <version>1.0.0</version>
</dependency>
```

After adding the dependency, start the application and open:

```text
/retreever
```

## Spring Security Integration

If the host application uses Spring Security, Retreever's public routes must be
allowed through the application's security configuration.

Use `RetreeverPublicPaths.get()`:

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

This is the main setup step required when Retreever is added to a secured host
application.

## Optional Annotations

Retreever works without custom annotations, but these help produce cleaner and
more intentional documentation:

| Annotation | Purpose |
| --- | --- |
| `@ApiDoc` | Sets top-level API name, description, and version |
| `@ApiGroup` | Groups controller endpoints under a named section |
| `@ApiEndpoint` | Adds endpoint name, description, status, security flag, headers, and mapped errors |
| `@ApiError` | Documents exception-handler status and description |
| `@FieldInfo` | Adds field descriptions and example values |
| `@Description` | Adds descriptions to request parameters and fields |

## Example Usage

### Application Metadata

```java
@SpringBootApplication
@ApiDoc(
        name = "Catalog API",
        description = "Live API docs for the catalog service",
        version = "v1"
)
public class CatalogApplication {
}
```

### Group, Endpoint, Errors, and Field Metadata

```java
@ApiGroup(
        name = "Product Variant APIs",
        description = "APIs for managing product variants"
)
@RestController
@RequestMapping("/api/v1")
public class ProductVariantController {

    @ApiEndpoint(
            name = "Create Product Variant",
            description = "Create a new product variant",
            secured = true,
            headers = {HttpHeaders.AUTHORIZATION, "X-Device-ID"},
            errors = {
                    AccessDeniedException.class,
                    ProductNotFoundException.class,
                    MethodArgumentNotValidException.class
            }
    )
    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @Description("ID/Primary Key of the product to which the variant should belong.")
            @PathVariable Long productId,
            @RequestBody @Valid ProductVariantRequest request
    ) {
        return null;
    }

    @ApiError(
            status = HttpStatus.FORBIDDEN,
            description = "Access Denied"
    )
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.build("Access Denied", ex.getMessage()));
    }
}
```

```java
public record ProductVariantRequest(
        @FieldInfo(
                example = "Red Mug - Ceramic",
                description = "User-friendly name of the product variant. Required."
        )
        @NotBlank(message = "Title must not be blank")
        @Size(min = 1, max = 255, message = "Title length must be between 1 and 255 characters")
        @JsonProperty("title")
        String title
) {}
```

## Optional Configuration

All Retreever configuration is optional.

Use configuration only when you want to enhance documentation or behavior.

### Optional Retreever Auth

If you want authentication enforced for Retreever in the host application, set:

```properties
retreever.auth.username=Admin
retreever.auth.password=Admin@123
```

Token TTLs default to:

- access token: `30 minutes`
- refresh token: `7 days`

Override them only if you actually need different values.

Detailed auth behavior is documented in
[Documentation/Retreever-Auth-API.md](Documentation/Retreever-Auth-API.md).

### Optional Environment Variable Resolution

Retreever can define static environment values or extract them from API
responses for testing workflows.

Example:

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
              body-attribute-path: data.access_token

      - name: refresh-token
        source:
          request:
            endpoints:
              - /api/v1/public/login
              - /api/v1/public/login/refresh
            method: post
            response:
              body-attribute-path: data.refresh_token

      - name: device-id
        source:
          value: device-web-001
```

## Compatibility

- Release line `1.x` targets Spring Boot `3.x`
- Minimum Java version is `17`
- Spring Boot `4.x` support is planned for a future major version

## Contributing

Issues, bug reports, integration tests, resolver improvements, and UI feedback
are all welcome.

## License

MIT
