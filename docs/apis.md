# Pandal Hopper — API Reference

Every endpoint, what it does, and copy-pasteable payloads for Postman.

Setup: [doc.md](doc.md) · Flutter client: [frontend.md](frontend.md)

- **Base URL (local):** `http://localhost:8080`
- **Content type:** `application/json`
- **Auth:** `Authorization: Bearer <jwt>` where required

---

## Contents

| # | Endpoint | Method | Auth | Purpose |
|---|---|---|---|---|
| [1](#1-post-authsignup) | `/auth/signup` | POST | — | Register |
| [2](#2-post-authlogin) | `/auth/login` | POST | — | Get a JWT |
| [3](#3-get-oauth2authorizationgoogle) | `/oauth2/authorization/google` | GET | — | Start Google login |
| [4](#4-post-authexchange) | `/auth/exchange` | POST | — | Trade OAuth2 code for a JWT |
| [5](#5-get-pandals) | `/pandals` | GET | — | All pandals |
| [6](#6-get-pandalszonezonesimple) | `/pandals/zone/{zone}/simple` | GET | — | Pandals in a zone |
| [7](#7-get-pandalszonezone) | `/pandals/zone/{zone}` | GET | — | ⚠️ Broken response shape |
| [8](#8-get-zonezonemetrossimple) | `/zone/{zone}/metros/simple` | GET | — | Stations in a zone (light) |
| [9](#9-get-zonezonemetros) | `/zone/{zone}/metros` | GET | — | Stations in a zone (full) |
| [10](#10-get-zonezonemetrometroidpandalssimple) | `/zone/{zone}/metro/{metroId}/pandals/simple` | GET | — | Pandals at a station (light) |
| [11](#11-get-zonezonemetrometroidpandals) | `/zone/{zone}/metro/{metroId}/pandals` | GET | — | Pandals at a station (full) |
| [12](#12-get-metronearest) | `/metro/nearest` | GET | — | Nearest station (full) |
| [13](#13-get-metronearestlocation) | `/metro/nearest/location` | GET | — | Nearest station (light) |
| [14](#14-post-apirouteoptimal) | `/api/route/optimal` | POST | — | Order stops into a route |
| [15](#15-get-actuatorhealth) | `/actuator/health` | GET | — | Health check |
| [16](#16-get-actuator) | `/actuator/**` | GET | **ADMIN** | Ops endpoints |

> **All read endpoints are public.** Only `/actuator/**` beyond health/info
> requires authentication. Login is optional for browsing.

---

# Authentication

## 1. `POST /auth/signup`

Registers a username/password account. Does **not** return a JWT — call
`/auth/login` afterwards.

**Request**

```json
{
  "username": "riya",
  "email": "riya@example.com",
  "password": "hunter2"
}
```

**`200 OK`**

```json
{
  "id": 7,
  "username": "riya",
  "email": "riya@example.com",
  "password": null
}
```

> `password` is always `null` — a vestigial field in the response DTO. Ignore it.

**Errors**

| Code | When | Body |
|---|---|---|
| `500` | Username already taken | Generic Spring error |

> ⚠️ A duplicate username returns **500, not 409**, because the service throws a
> bare `RuntimeException` and there is no global exception handler. There is
> also no validation: empty username or password is accepted here and fails
> later.

**curl**

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"riya","email":"riya@example.com","password":"hunter2"}'
```

---

## 2. `POST /auth/login`

Exchanges credentials for a JWT. **The token expires in 1 hour and there is no
refresh token.**

**Request**

```json
{
  "username": "riya",
  "password": "hunter2"
}
```

**`200 OK`**

```json
{
  "jwt": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyaXlhIiwiVXNlcklkIjoiNyIsImlhdCI6MTcuLi59.abc123",
  "userId": 7
}
```

The field is `jwt` — not `token` or `access_token`.

**Errors**

| Code | When |
|---|---|
| `401` | Username exists, password wrong |
| `500` | Username does **not** exist |

> ⚠️ The same user mistake produces two different codes. `UserDetailService`
> calls `.orElseThrow()`, which raises `NoSuchElementException` instead of
> `UsernameNotFoundException`, so Spring Security never converts it to a 401.
> Clients should treat both as "incorrect username or password".

**Postman tip** — auto-capture the token in *Scripts → Post-response*:

```javascript
if (pm.response.code === 200) {
    pm.environment.set("jwt", pm.response.json().jwt);
}
```

Then use `{{jwt}}` in the Authorization header of other requests.

---

## 3. `GET /oauth2/authorization/google`

Starts the Google OAuth2 flow. **Browser-only — not testable in Postman**, as it
depends on redirects and Google consent.

Flow:

```
GET /oauth2/authorization/google
  → Google consent screen
  → GET /login/oauth2/code/google?code=...   (handled by Spring)
  → 302 to {app.frontend.callback-url}?code=<opaque>
```

The redirect carries a **single-use exchange code, not the JWT**. Redeem it via
endpoint 4.

To test locally, register `http://localhost:8080/login/oauth2/code/google` as an
authorised redirect URI in Google Cloud Console and open the URL in a browser.

**Errors**

| Code | When |
|---|---|
| `401` | Google rejected the login |
| `500` | Email already registered under a different provider |

---

## 4. `POST /auth/exchange`

Redeems the code from the OAuth2 redirect for a JWT. **The code lives 60
seconds in Redis and is destroyed on first use.**

**Request**

```json
{ "code": "9fK2sL-pQm4xR7vN0aB3cD8eF1gH5iJ6kL9mN2oP4qR" }
```

**`200 OK`**

```json
{ "jwt": "eyJhbGciOiJIUzI1NiJ9...", "userId": 7 }
```

**`401`**

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired code"
}
```

Returned for unknown, expired, or already-redeemed codes alike.

> Requires Redis. If Redis is down this endpoint fails — unlike the cached read
> endpoints, it has no database fallback, which is correct: a one-time code has
> nowhere else to live.

---

# Pandals

## 5. `GET /pandals`

Every pandal in the database, minimal fields. **Cached in Redis (60 min TTL).**

⚠️ **No pagination.** Returns the entire table. Intended to be fetched once and
cached client-side.

**`200 OK`**

```json
[
  { "name": "Bagbazar Sarbojanin", "latitude": 22.6021, "longitude": 88.3654 },
  { "name": "Kumartuli Park",      "latitude": 22.5952, "longitude": 88.3601 }
]
```

```bash
curl http://localhost:8080/pandals
```

---

## 6. `GET /pandals/zone/{zone}/simple`

Pandals in one zone. Same shape as endpoint 5, filtered. **Cached per zone.**
Prefer this over `/pandals`.

**Path params** — `zone`: exact string match, e.g. `north`, `central`, `south`.
Case- and spelling-sensitive; an unknown zone returns `[]`, not a 404.

**`200 OK`**

```json
[
  { "name": "Bagbazar Sarbojanin", "latitude": 22.6021, "longitude": 88.3654 },
  { "name": "Kumartuli Park",      "latitude": 22.5952, "longitude": 88.3601 },
  { "name": "Ahiritola Sarbojanin","latitude": 22.5901, "longitude": 88.3688 }
]
```

```bash
curl http://localhost:8080/pandals/zone/north/simple
```

---

## 7. `GET /pandals/zone/{zone}`

Pandals grouped by their nearest metro station.

> ### ⚠️ Do not use this endpoint
>
> It returns `Map<MetroStation, List<Pandal>>`. Jackson has no way to serialise
> an entity as a JSON **key**, so it falls back to Lombok's `toString()`:
>
> ```json
> {
>   "MetroStation(metroId=2, metroLat=22.5989, metroLon=88.3742, metroName=Shyambazar, metroStationCode=SHY, metroLine=Blue)": [
>     { "pandalId": "...", "name": "Bagbazar Sarbojanin", "...": "..." }
>   ]
> }
> ```
>
> The keys are unparseable in any client. Use endpoints **8 + 10** instead —
> they express the same relationship in two clean calls. This endpoint is also
> the only uncached one in `PandalService`.

---

# Zones

## 8. `GET /zone/{zone}/metros/simple`

Metro stations serving a zone, minimal fields. Use for map markers.

**`200 OK`**

```json
[
  { "metroId": 2, "metroName": "Shyambazar", "metroLat": 22.5989, "metroLon": 88.3742 },
  { "metroId": 3, "metroName": "Girish Park","metroLat": 22.5860, "metroLon": 88.3639 }
]
```

```bash
curl http://localhost:8080/zone/north/metros/simple
```

> Note: this endpoint is annotated `@Cacheable` but calls another cached method
> on `this`, so Spring's proxy is bypassed and the inner cache never engages.
> It works correctly, just hits the DB every time.

---

## 9. `GET /zone/{zone}/metros`

Same as 8 but full `MetroStation` entities. **Cached.**

**`200 OK`**

```json
[
  {
    "metroId": 2,
    "metroLat": 22.5989,
    "metroLon": 88.3742,
    "metroName": "Shyambazar",
    "metroStationCode": "SHY",
    "metroLine": "Blue"
  }
]
```

Use only if you need `metroStationCode` or `metroLine`.

---

## 10. `GET /zone/{zone}/metro/{metroId}/pandals/simple`

Pandals near a specific station within a zone. The main drill-down call.

**Path params** — `zone` (string), `metroId` (long, from endpoint 8).

**`200 OK`**

```json
[
  { "name": "Bagbazar Sarbojanin", "latitude": 22.6021, "longitude": 88.3654 },
  { "name": "Kumartuli Park",      "latitude": 22.5952, "longitude": 88.3601 }
]
```

```bash
curl http://localhost:8080/zone/north/metro/2/pandals/simple
```

Mismatched `zone`/`metroId` returns `[]`, not an error.

---

## 11. `GET /zone/{zone}/metro/{metroId}/pandals`

Same as 10 but full `Pandal` entities, each embedding its `metroStation`.
**Cached.**

**`200 OK`**

```json
[
  {
    "pandalId": "3f2a7c81-9b4e-4d2a-8c1f-5e6d7a8b9c0d",
    "latitude": 22.6021,
    "longitude": 88.3654,
    "zone": "north",
    "city": "Kolkata",
    "name": "Bagbazar Sarbojanin",
    "address": "Bagbazar Street",
    "searchScore": 4.8,
    "metroDistance": 0.9,
    "metroDistanceUnit": "km",
    "metroStation": {
      "metroId": 2,
      "metroLat": 22.5989,
      "metroLon": 88.3742,
      "metroName": "Shyambazar",
      "metroStationCode": "SHY",
      "metroLine": "Blue"
    }
  }
]
```

Heavier payload — use only for `address`, `city`, `searchScore`, or
`metroDistance`.

---

# Metro lookup

## 12. `GET /metro/nearest`

Nearest metro station to a coordinate, by Haversine distance. **Not cached** —
loads every station into memory and linear-scans on each call.

**Query params** — `lat` (double, required), `lon` (double, required).

**`200 OK`**

```json
{
  "metroId": 2,
  "metroLat": 22.5989,
  "metroLon": 88.3742,
  "metroName": "Shyambazar",
  "metroStationCode": "SHY",
  "metroLine": "Blue"
}
```

⚠️ With an empty `metro_station` table this returns **`200` with an empty
body**, not 404. Null-check before parsing.

```bash
curl "http://localhost:8080/metro/nearest?lat=22.6021&lon=88.3654"
```

**Errors** — missing `lat`/`lon` → `400` (Spring's missing-parameter error);
non-numeric values → `400`.

---

## 13. `GET /metro/nearest/location`

Lightweight version of 12.

**`200 OK`**

```json
{ "name": 2, "lat": 22.5989, "lon": 88.3742 }
```

⚠️ **`name` is a number — it is the metro ID**, misnamed in `MetroLocationDTO`.
Map it to `metroId` client-side.

Returns **`404`** when no station is found — inconsistent with endpoint 12,
which returns 200. Handle both.

```bash
curl "http://localhost:8080/metro/nearest/location?lat=22.6021&lon=88.3654"
```

---

# Routing

## 14. `POST /api/route/optimal`

Reorders a set of stops into an efficient visiting sequence, starting from
`startPoint`.

Uses a **nearest-neighbour heuristic over straight-line (Haversine) distance**.
It is not true TSP and is not road-aware — no streets, rivers, or one-ways.
Treat the output as a suggested order, not navigation.

**Request**

```json
{
  "startPoint": {
    "lat": 22.5989,
    "lon": 88.3742,
    "name": "Shyambazar Metro"
  },
  "pandals": [
    { "lat": 22.6021, "lon": 88.3654, "name": "Bagbazar Sarbojanin" },
    { "lat": 22.5952, "lon": 88.3601, "name": "Kumartuli Park" },
    { "lat": 22.5901, "lon": 88.3688, "name": "Ahiritola Sarbojanin" }
  ]
}
```

`name` is passed through untouched — use it to correlate the response.

**`200 OK`**

```json
{
  "origin":      { "lat": 22.5989, "lon": 88.3742, "name": "Shyambazar Metro" },
  "destination": { "lat": 22.5952, "lon": 88.3601, "name": "Kumartuli Park" },
  "waypoints": [
    { "lat": 22.6021, "lon": 88.3654, "name": "Bagbazar Sarbojanin" },
    { "lat": 22.5901, "lon": 88.3688, "name": "Ahiritola Sarbojanin" }
  ]
}
```

- `origin` is always your `startPoint`.
- `waypoints` is the middle of the route, **already ordered**. Empty when you
  send exactly one pandal.
- Full path = `[origin, ...waypoints, destination]`.
- No distance or duration is returned.

**Errors** — all `400`:

```json
{ "status": 400, "error": "Bad Request", "message": "Too many pandals: 240 (max 100)" }
```

| Condition | `message` |
|---|---|
| `startPoint` missing/null | `startPoint is required` |
| `pandals` missing, null, or `[]` | `At least one pandal is required` |
| More than 100 entries | `Too many pandals: N (max 100)` |
| A `null` inside `pandals` | `pandals must not contain null entries` |

The 100 cap exists because the solver is O(n²) on a public endpoint.

**Minimal test payload** (single pandal — expect empty `waypoints`):

```json
{
  "startPoint": { "lat": 22.5989, "lon": 88.3742, "name": "Start" },
  "pandals": [ { "lat": 22.6021, "lon": 88.3654, "name": "Only Stop" } ]
}
```

**curl**

```bash
curl -X POST http://localhost:8080/api/route/optimal \
  -H "Content-Type: application/json" \
  -d '{
    "startPoint": {"lat":22.5989,"lon":88.3742,"name":"Shyambazar Metro"},
    "pandals": [
      {"lat":22.6021,"lon":88.3654,"name":"Bagbazar Sarbojanin"},
      {"lat":22.5952,"lon":88.3601,"name":"Kumartuli Park"}
    ]
  }'
```

---

# Operations

## 15. `GET /actuator/health`

Public. No token.

```json
{ "status": "UP" }
```

## 16. `GET /actuator/**`

Everything except `/health` and `/info` requires a JWT whose user holds
`ROLE_ADMIN`. Without one: `401`. With a non-admin token: `403`.

> Roles are only assigned at signup (`USER`). There is no endpoint to grant
> `ADMIN` — set it directly in the database to test.

---

# Testing with Postman

## Environment

Create an environment with:

| Variable | Initial value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `jwt` | *(leave empty — populated by the login script)* |

## Auth header

For endpoints needing auth, set *Authorization → Bearer Token* to `{{jwt}}`.

Since a bad token now returns a structured 401, you can assert on it:

```json
{ "status": 401, "error": "Unauthorized", "message": "Invalid token", "path": "/actuator/metrics" }
```

`message` is `"Token has expired"` or `"Invalid token"`.

## Importable collection

Save as `pandal-hopper.postman_collection.json` and import via
**File → Import**.

```json
{
  "info": {
    "name": "Pandal Hopper",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    { "key": "baseUrl", "value": "http://localhost:8080" },
    { "key": "zone", "value": "north" },
    { "key": "metroId", "value": "2" }
  ],
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Signup",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/auth/signup",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"riya\",\n  \"email\": \"riya@example.com\",\n  \"password\": \"hunter2\"\n}"
            }
          }
        },
        {
          "name": "Login",
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "if (pm.response.code === 200) {",
                  "    pm.collectionVariables.set('jwt', pm.response.json().jwt);",
                  "    pm.test('got a jwt', () => pm.expect(pm.response.json().jwt).to.be.a('string'));",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/auth/login",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"riya\",\n  \"password\": \"hunter2\"\n}"
            }
          }
        },
        {
          "name": "Exchange OAuth2 code",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/auth/exchange",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"code\": \"paste-code-from-redirect\"\n}"
            }
          }
        }
      ]
    },
    {
      "name": "Pandals",
      "item": [
        {
          "name": "All pandals",
          "request": { "method": "GET", "url": "{{baseUrl}}/pandals" }
        },
        {
          "name": "Pandals by zone (simple)",
          "request": { "method": "GET", "url": "{{baseUrl}}/pandals/zone/{{zone}}/simple" }
        }
      ]
    },
    {
      "name": "Zones",
      "item": [
        {
          "name": "Metros for zone (simple)",
          "request": { "method": "GET", "url": "{{baseUrl}}/zone/{{zone}}/metros/simple" }
        },
        {
          "name": "Metros for zone (full)",
          "request": { "method": "GET", "url": "{{baseUrl}}/zone/{{zone}}/metros" }
        },
        {
          "name": "Pandals by zone + metro (simple)",
          "request": { "method": "GET", "url": "{{baseUrl}}/zone/{{zone}}/metro/{{metroId}}/pandals/simple" }
        },
        {
          "name": "Pandals by zone + metro (full)",
          "request": { "method": "GET", "url": "{{baseUrl}}/zone/{{zone}}/metro/{{metroId}}/pandals" }
        }
      ]
    },
    {
      "name": "Metro",
      "item": [
        {
          "name": "Nearest metro",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/metro/nearest?lat=22.6021&lon=88.3654",
              "host": ["{{baseUrl}}"],
              "path": ["metro", "nearest"],
              "query": [
                { "key": "lat", "value": "22.6021" },
                { "key": "lon", "value": "88.3654" }
              ]
            }
          }
        },
        {
          "name": "Nearest metro (location only)",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/metro/nearest/location?lat=22.6021&lon=88.3654",
              "host": ["{{baseUrl}}"],
              "path": ["metro", "nearest", "location"],
              "query": [
                { "key": "lat", "value": "22.6021" },
                { "key": "lon", "value": "88.3654" }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Routing",
      "item": [
        {
          "name": "Optimal route",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/api/route/optimal",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"startPoint\": { \"lat\": 22.5989, \"lon\": 88.3742, \"name\": \"Shyambazar Metro\" },\n  \"pandals\": [\n    { \"lat\": 22.6021, \"lon\": 88.3654, \"name\": \"Bagbazar Sarbojanin\" },\n    { \"lat\": 22.5952, \"lon\": 88.3601, \"name\": \"Kumartuli Park\" },\n    { \"lat\": 22.5901, \"lon\": 88.3688, \"name\": \"Ahiritola Sarbojanin\" }\n  ]\n}"
            }
          }
        },
        {
          "name": "Optimal route - empty pandals (expect 400)",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/api/route/optimal",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"startPoint\": { \"lat\": 22.5989, \"lon\": 88.3742, \"name\": \"Start\" },\n  \"pandals\": []\n}"
            }
          }
        }
      ]
    },
    {
      "name": "Ops",
      "item": [
        {
          "name": "Health",
          "request": { "method": "GET", "url": "{{baseUrl}}/actuator/health" }
        },
        {
          "name": "Metrics (needs ADMIN)",
          "request": {
            "method": "GET",
            "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{jwt}}" }] },
            "url": "{{baseUrl}}/actuator/metrics"
          }
        }
      ]
    }
  ]
}
```

## Suggested run order

1. **Health** — confirm the app is up
2. **Signup** → **Login** — populates `{{jwt}}`
3. **All pandals** — confirm seed data exists (empty `[]` means unseeded, see [doc.md §5](doc.md))
4. **Metros for zone** — note a `metroId` for `{{metroId}}`
5. **Pandals by zone + metro**
6. **Optimal route** — paste coordinates from step 5
7. **Optimal route (empty)** — confirm the 400 guard

---

## Response-shape quirks at a glance

| Endpoint | Quirk |
|---|---|
| `/auth/signup` | `password` field always `null` |
| `/auth/login` | Token field is `jwt`; unknown username → 500, wrong password → 401 |
| `/pandals/zone/{zone}` | Object keys are stringified Java entities — unusable |
| `/metro/nearest` | Empty table → `200` with empty body |
| `/metro/nearest/location` | `name` is the numeric metro ID; empty → `404` |
| `/api/route/optimal` | No distance or duration; ordering only |
| All list endpoints | No pagination; unknown zone → `[]`, never 404 |
