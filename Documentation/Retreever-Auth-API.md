# Retreever Auth API

This document describes the current authentication contract for Retreever UI integration.

It is written for the React UI and covers:

- which routes are public and which are protected
- the login, refresh, and logout APIs
- cookie behavior
- expected client flow
- all known response and error cases

## 1. Overview

Retreever authentication is a lightweight, library-level mechanism intended to protect Retreever's own backend APIs without introducing Spring Security into the host application.

Current behavior:

- the Retreever UI routes remain public
- protected Retreever data APIs return `401 Unauthorized` when the session is missing or invalid
- the UI is responsible for deciding when to show the login screen
- authentication state is stored in `HttpOnly` cookies
- refresh is explicit and must be called by the UI
- logout is explicit and must be called by the UI

## 2. Host Configuration

Retreever auth is enabled only when both username and password are configured.

```properties
retreever.auth.username=admin
retreever.auth.password=change-me
retreever.allow-cross-origin=http://localhost:5173
```

### Configuration Notes

- `retreever.auth.username` and `retreever.auth.password` enable auth when both are present.
- Retreever generates an in-memory secret from a startup UUID.
- All tokens become invalid after application restart.
- `retreever.allow-cross-origin` allows credentialed CORS only for explicitly listed origins.
- `retreever.allow-cross-origin` accepts a comma-separated list.
- Wildcard `*` is rejected because cookies are used for auth.
- Default access token TTL is `30 minutes`.
- Default refresh token TTL is `7 days`.

## 3. Route Model

### 3.1 Public UI Routes

These routes are public and should always load the React app shell:

- `GET /retreever`
- `GET /retreever/**`

Examples:

- `/retreever`
- `/retreever/login`
- `/retreever/workspace`
- `/retreever/some/client-side-route`

The backend forwards these to `index.html`.

### 3.2 Public Auth API Routes

These routes are public API endpoints:

- `POST /retreever/login`
- `POST /retreever/refresh`
- `POST /retreever/logout`

They are public in the sense that they are not blocked by the auth filter.

Important:

- `login` validates credentials
- `refresh` validates the refresh token cookie and device cookie
- `logout` clears cookies and attempts to revoke the current session

### 3.3 Protected Retreever Data Routes

These routes are currently protected by the auth filter:

- `GET /retreever/doc`
- `GET /retreever/ping`
- `GET /retreever/environment`

If authentication fails, these endpoints return:

```json
{
  "error": "unauthorized",
  "message": "Authentication is required for Retreever."
}
```

with status `401`.

## 4. Cookie Contract

Retreever uses three cookies.

| Cookie | Purpose | HttpOnly | SameSite | Path | Lifetime |
| --- | --- | --- | --- | --- | --- |
| `retreever_at` | Access token | Yes | `Lax` | `/retreever` | access token TTL |
| `retreever_rt` | Refresh token | Yes | `Lax` | `/retreever` | refresh token TTL |
| `retreever_did` | Device/session binding id | Yes | `Lax` | `/retreever` | refresh token TTL |

### Cookie Properties

- all three cookies are `HttpOnly`
- all three cookies use `SameSite=Lax`
- all three cookies are scoped to the `/retreever` path
- the `Secure` attribute is set only when the incoming request is HTTPS
- cookies are written and cleared by the server only

### UI Implication

Because the cookies are `HttpOnly`, the React app cannot read token values directly. That is expected.

The browser automatically sends them with requests to `/retreever/*` on the same site.

## 5. Session and Token Model

The current implementation is session-based with encrypted tokens.

### What happens on login

On successful login, the server:

1. validates the configured username and password
2. creates a new server-side session record
3. creates a unique session id
4. creates a unique device id
5. creates a unique refresh-token id
6. encrypts token payloads using AES-GCM
7. sends `retreever_at`, `retreever_rt`, and `retreever_did` cookies

### Important Characteristics

- multiple developers can log in using the same configured username and password
- each login gets a different session
- each login gets different access and refresh tokens
- each login gets a different device id
- refresh rotates the refresh token
- logout revokes the session from the in-memory session store

### Current Security Properties

- tokens are encrypted with AES-GCM
- access token is bound to the device id cookie
- refresh token is bound to the device id cookie
- refresh token rotation reduces replay risk
- logout invalidates the server-side session entry
- because sessions are stored in memory, this is best suited for a single-instance deployment

### Current Operational Limitations

- sessions are in memory only
- a server restart invalidates active sessions if the in-memory secret changes
- the startup-generated secret changes on every restart, so restart invalidates sessions
- this is not a clustered or distributed session system
- this is not a replacement for a full enterprise identity provider

## 6. Client Integration Flow

Recommended UI flow:

1. Load the public React route under `/retreever`.
2. Call a protected API such as `GET /retreever/doc`.
3. If it returns `200`, proceed to the workspace.
4. If it returns `401`, show the login page in the React UI.
5. On login success, retry the protected API call.
6. During normal usage, if a protected API returns `401`, call `POST /retreever/refresh`.
7. If refresh returns `200`, retry the original protected API request.
8. If refresh returns `401`, redirect to the React login screen and require login again.
9. On explicit logout, call `POST /retreever/logout` and transition the UI to the login screen.

## 7. Request Rules for the UI

### 7.1 Same-Site Requests

If the React app is served from the same site as Retreever, the browser will normally send cookies automatically.

### 7.2 Fetch Example

Use explicit credentials in frontend code for clarity:

```ts
await fetch("/retreever/doc", {
  method: "GET",
  credentials: "include"
});
```

### 7.3 Axios Example

```ts
await axios.get("/retreever/doc", {
  withCredentials: true
});
```

Use `credentials: "include"` or `withCredentials: true` consistently on:

- login
- refresh
- logout
- protected Retreever API calls

If the UI is served from a different origin during development, that origin must be listed in `retreever.allow-cross-origin`.

## 8. API Reference

## 8.1 Login

Authenticates a user using the configured shared username and password.

### Endpoint

`POST /retreever/login`

### Request Headers

```http
Content-Type: application/json
```

### Request Body

```json
{
  "username": "admin",
  "password": "change-me"
}
```

### Success Response

Status: `200 OK`

Response body:

```json
{
  "authenticated": true,
  "accessTokenExpiresAt": "2026-04-15T08:30:00Z",
  "refreshTokenExpiresAt": "2026-04-22T08:00:00Z"
}
```

Response cookies:

- `retreever_at`
- `retreever_rt`
- `retreever_did`

### Error Responses

#### Invalid credentials

Status: `401 Unauthorized`

```json
{
  "error": "invalid_credentials",
  "message": "Invalid username or password."
}
```

#### Auth disabled in host app

Status: `404 Not Found`

No response body is guaranteed.

This happens when `retreever.auth.username` and `retreever.auth.password` are not configured.

### Client Notes

- on success, do not attempt to read cookies in JavaScript
- just rely on the browser cookie jar
- after success, call a protected endpoint such as `/retreever/doc`

## 8.2 Refresh

Refreshes the session using the refresh token cookie and device id cookie.

### Endpoint

`POST /retreever/refresh`

### Request Body

No request body.

### Required Cookies

- `retreever_rt`
- `retreever_did`

The access token cookie is not required for refresh.

### Success Response

Status: `200 OK`

```json
{
  "authenticated": true,
  "accessTokenExpiresAt": "2026-04-15T09:00:00Z",
  "refreshTokenExpiresAt": "2026-04-22T08:45:00Z"
}
```

Response cookies:

- a new `retreever_at`
- a new `retreever_rt`
- `retreever_did` is re-issued and remains the same logical device binding for the session

### Error Responses

#### Missing, expired, tampered, or invalid refresh token

Status: `401 Unauthorized`

```json
{
  "error": "invalid_refresh_token",
  "message": "Refresh token is missing, expired, or invalid."
}
```

Additional behavior:

- the server clears `retreever_at`
- the server clears `retreever_rt`
- the server clears `retreever_did`

#### Auth disabled in host app

Status: `404 Not Found`

No response body is guaranteed.

### What counts as invalid refresh

Refresh fails when any of the following is true:

- `retreever_rt` cookie is missing
- `retreever_did` cookie is missing
- refresh token cannot be decrypted
- refresh token has the wrong internal token type
- refresh token is expired
- device id does not match the token payload
- session is no longer present in the server-side in-memory store
- refresh token id does not match the latest server-side session state

## 8.3 Logout

Explicitly logs out the current client session.

### Endpoint

`POST /retreever/logout`

### Request Body

No request body.

### Recommended Request Cookies

- `retreever_at`
- `retreever_rt`
- `retreever_did`

### Success Response

Status: `204 No Content`

Response body: none

Additional behavior:

- server attempts to revoke the associated session
- server clears `retreever_at`
- server clears `retreever_rt`
- server clears `retreever_did`

### Error Responses

#### Auth disabled in host app

Status: `404 Not Found`

No response body is guaranteed.

### Important Logout Note

`logout` is intentionally tolerant.

If auth is enabled, the endpoint returns `204 No Content` even when:

- cookies are already missing
- access token is expired
- refresh token is expired
- session was already removed

The main contract is: clear cookies and move the UI back to login state.

## 9. Protected API Error Contract

Currently protected endpoints:

- `GET /retreever/doc`
- `GET /retreever/ping`
- `GET /retreever/environment`

### Unauthorized Response

Status: `401 Unauthorized`

```json
{
  "error": "unauthorized",
  "message": "Authentication is required for Retreever."
}
```

### What causes this `401`

Any of the following:

- no `retreever_at` cookie
- no `retreever_did` cookie
- access token cannot be decrypted
- access token has wrong internal type
- access token is expired
- access token device id does not match `retreever_did`
- session does not exist in the server-side in-memory store
- session exists but does not match the token payload

### Important Behavior

Protected APIs do not clear cookies on `401`.

That is intentional so the UI can:

1. detect the `401`
2. call `/retreever/refresh`
3. retry the original request if refresh succeeds
4. redirect to login only if refresh also fails

## 10. Error Matrix

| Scenario | Endpoint | Status | Body | Cookies changed |
| --- | --- | --- | --- | --- |
| Correct credentials | `POST /retreever/login` | `200` | `authenticated=true` + expiries | set `at`, `rt`, `did` |
| Wrong credentials | `POST /retreever/login` | `401` | `invalid_credentials` | none guaranteed |
| Auth disabled | `POST /retreever/login` | `404` | none guaranteed | none |
| Valid refresh | `POST /retreever/refresh` | `200` | `authenticated=true` + expiries | rotate `at`, `rt`, reissue `did` |
| Missing/expired/invalid refresh | `POST /retreever/refresh` | `401` | `invalid_refresh_token` | clears `at`, `rt`, `did` |
| Auth disabled | `POST /retreever/refresh` | `404` | none guaranteed | none |
| Logout while auth enabled | `POST /retreever/logout` | `204` | none | clears `at`, `rt`, `did` |
| Auth disabled | `POST /retreever/logout` | `404` | none guaranteed | none |
| Accessing protected API without valid session | protected API | `401` | `unauthorized` | none |

## 11. Recommended UI State Machine

### Initial App Load

1. Load `/retreever`
2. Call `/retreever/doc`
3. If `200`, enter workspace
4. If `401`, enter login screen

### Login Submit

1. Call `POST /retreever/login`
2. If `200`, call `/retreever/doc`
3. If `401`, show invalid credentials message
4. If `404`, show configuration error because auth is disabled or unavailable

### Token Expiry During Usage

1. Any protected API returns `401`
2. Call `POST /retreever/refresh`
3. If refresh returns `200`, retry the failed request once
4. If refresh returns `401`, clear UI auth state and go to login
5. If refresh returns `404`, treat as server configuration issue

### Logout

1. Call `POST /retreever/logout`
2. Ignore body because there is none
3. Move UI to login state
4. Optionally clear any client-only cached workspace state

## 12. UI Implementation Notes

### 12.1 Do not store tokens in local storage

The server already stores auth in `HttpOnly` cookies. Do not duplicate auth state in:

- `localStorage`
- `sessionStorage`
- IndexedDB

### 12.2 Keep only derived UI state on the client

Safe client-side state examples:

- `isAuthenticated` inferred from successful protected API calls
- `loginError`
- `refreshInFlight`
- `workspaceReady`

### 12.3 Retry refresh only once per failed request

Do not create infinite refresh loops.

Recommended rule:

- if a protected request returns `401`, try refresh once
- if the retried request still returns `401`, go to login

### 12.4 Prevent concurrent refresh storms

If many requests fail at once, use a shared refresh promise or request queue so only one refresh happens at a time.

## 13. Example Integration Snippets

## 13.1 Login

```ts
async function login(username: string, password: string) {
  const response = await fetch("/retreever/login", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ username, password })
  });

  if (response.status === 200) {
    return response.json();
  }

  if (response.status === 401) {
    const error = await response.json();
    throw new Error(error.message);
  }

  throw new Error(`Unexpected login status: ${response.status}`);
}
```

## 13.2 Protected Request with Refresh Fallback

```ts
async function fetchRetreeverDoc() {
  let response = await fetch("/retreever/doc", {
    method: "GET",
    credentials: "include"
  });

  if (response.status === 200) {
    return response.json();
  }

  if (response.status !== 401) {
    throw new Error(`Unexpected status: ${response.status}`);
  }

  const refreshResponse = await fetch("/retreever/refresh", {
    method: "POST",
    credentials: "include"
  });

  if (refreshResponse.status !== 200) {
    throw new Error("Session expired");
  }

  response = await fetch("/retreever/doc", {
    method: "GET",
    credentials: "include"
  });

  if (response.status !== 200) {
    throw new Error("Retry after refresh failed");
  }

  return response.json();
}
```

## 13.3 Logout

```ts
async function logout() {
  await fetch("/retreever/logout", {
    method: "POST",
    credentials: "include"
  });
}
```

## 14. Backend Contract Summary

This is the simplest reliable UI contract to build against:

- UI routes under `/retreever/**` are public
- protected Retreever APIs return `401` when auth is missing or invalid
- login sets `HttpOnly` cookies
- refresh rotates cookies
- logout clears cookies
- the UI never reads tokens directly
- the UI responds only to HTTP status codes and normal JSON error payloads
