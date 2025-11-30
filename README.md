# Retreever

**Instant API Documentation & Testing for Spring Boot — Zero YAML. Zero Manual Work.**

Retreever is a **lightweight, developer-first toolkit** that automatically **discovers, documents, and tests** your Spring Boot APIs — *without a single manual annotation or YAML file.*

It scans your controllers, request/response models, validation constraints, and exception handlers to build a **complete, accurate, always-up-to-date** API documentation model.

Think of it as **Swagger + Postman → merged, modernized, and simplified.**
Just add the dependency, start your app, and open:

```
/retreever
```

Done. Your entire API surface is ready — documented, organized, and instantly testable.

<br>

# ✨ Why Retreever?

Unlike Swagger/OpenAPI tools that require 20 lines of annotations per endpoint *and* a separate Postman collection you manually maintain, Retreever does all the work for you.

### Retreever gives you:

1.  ✔ **Automatic docs** (no annotation clutter)
2.  ✔ **Automatic examples** (via annotations + constraints)
3.  ✔ **Accurate generic resolution** (even nested)
4.  ✔ **Automatic error mapping** (directly from your exception handlers)
5.  ✔ **A modern Postman-like testing UI**
6.  ✔ **Zero YAML, zero configuration**
7.  ✔ **Reflection-accurate request & response schemas**
8.  ✔ **Blazing fast output (~30ms)**
9.  ✔ **A tiny JSON document (~45KB) for complex ~70 endpoint resolution**

Just write normal Spring code — Retreever fills in everything else.

<br>

# 🚀 Features

## ⚡ Zero Configuration

Drop it in your Spring Boot app. Retreever automatically discovers:

* `@RestController` classes
* Request bodies (`@RequestBody`)
* Response types (`ResponseEntity<T>` and raw DTOs)
* Path variables, query params, and headers
* Validation annotations
* Exception handlers (`@ExceptionHandler`)

No setup. No external config. No YAML.



## 🧩 Smart Schema Resolution

Automatically builds a predictable JSON schema for:

* Complex nested DTOs
* Lists, arrays, maps
* Records and plain classes
* Enums
* Nullable vs non-nullable fields
* Jakarta Validation constraints
* Field-level documentation (`@FieldInfo`)

Generic substitution is deeply supported:

```
ResponseEntity<Page<OrderItemResponse>>
```

…just works.


## 🛣️ Endpoint & Metadata Extraction

Every endpoint includes:

* HTTP method
* Full resolved path
* Params (path, query, header)
* Consumes / produces media types
* Security flags (`secured=true`, `@PreAuthorize`)
* Developer-friendly name & description (`@ApiEndpoint`)

Grouped automatically using `@ApiGroup`.



## ❗ Automatic Error Mapping

Declare your error responses *once* where they belong — your `@RestControllerAdvice`.

Retreever extracts:

* Error type
* HTTP status
* Description
* Error body schema

Your documentation stays **fully consistent** with your real exception flow.

Swagger can’t do this.
SpringDoc can’t do this.
Retreever does.


## 📄 Clean, Stable Output Document

Every part of the system flows into a final immutable DTO:

```
ApiDocument
```

Containing:

* Metadata
* Groups
* Endpoints
* Request schemas
* Response schemas
* Example objects
* Errors
* Validation constraints


# 📦 Installation

(*Publishing to Maven Central in progress*)

Soon you’ll simply add:

```xml
<dependency>
    <groupId>dev.retreever</groupId>
    <artifactId>retreever</artifactId>
    <version>1.0.0</version>
</dependency>
```



# 📄 Example Output

```json
{
  "name": "Example API",
  "version": "v1",
  "groups": [
    {
      "name": "Product APIs",
      "endpoints": [
        {
          "name": "Get Product",
          "method": "GET",
          "path": "/products/{id}",
          "request": { ... },
          "response": { ... },
          "errors": [ ... ]
        }
      ]
    }
  ]
}
```

Clean. Predictable. Easy to render.

<br>

# 📊 Comparison With Other Tools

Retreever replaces BOTH Swagger and Postman.

| Feature             | Swagger  | SpringDoc | Postman | **Retreever** |
| ------------------- | -------- | --------- | ------- | ------------- |
| Auto-generates docs | ✔        | ✔         | ❌       | **✔**         |
| Accurate examples   | ❌        | ❌         | Manual  | **✔**         |
| Generic resolution  | Weak     | Medium    | ❌       | **Strong**    |
| Error mapping       | Weak     | Weak      | ❌       | **Strong**    |
| Always up-to-date   | ❌        | ❌         | ❌       | **✔**         |
| Testing panel       | ❌        | ❌         | ✔       | **✔**         |
| Annotation clutter  | ❌        | ❌         | ✔       | **Minimal**   |
| Output size         | Bloated  | Bloated   | N/A     | **~45KB**     |
| YAML required       | ✔        | ✔         | ❌       | **❌**         |
| UI                  | Outdated | Outdated  | Modern  | **Modern**    |

<br>

# 🧭 Roadmap

-  ✅ Core backend
-  ✅ Schema resolution engine
-  ✅ Error mapping
-  🚧 Frontend UI
-  🔜 Microservice discovery
-  🔜 Polymorphic type support
-  🔜 Map & multi-generic improvements
-  🔜 Gradle plugin / IDE integration

<br>

# 🤝 Contributing

Contributions are welcome!

* Report issues
* Improve type resolution
* Add integration tests
* Suggest new annotations
* Help with frontend

Let’s make API documentation fast, clean, and fun.

<br>

# 📝 License

MIT — free for personal and commercial use.

<br>

# ❤️ Acknowledgement

Built for developers who are tired of stale documentation, duplicated effort, and YAML fatigue —
Retreever **fetches everything you need, instantly.**
