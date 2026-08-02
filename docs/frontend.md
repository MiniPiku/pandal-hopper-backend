# Pandal Hopper — Flutter Frontend Guide

Everything a Flutter client needs to talk to this backend: API contracts, auth
flow, screen-by-screen user journey, and the backend quirks you must code
around.

This describes the API **as it currently behaves**, including rough edges.
Where the backend does something surprising, it is called out rather than
smoothed over — see [Known rough edges](#known-rough-edges) before you start.

---

## 1. What the app does

Pandal Hopper helps users discover Durga Puja pandals around Kolkata metro
stations and plan an efficient walking route between them.

The core loop:

> pick a zone → see which metro stations serve it → pick a station → see the
> pandals near it → select the ones you want → get an optimised visiting order

---

## 2. Base configuration

```dart
// lib/core/config.dart
class ApiConfig {
  static const baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080', // Android emulator → host machine
  );
}
```

Run with `flutter run --dart-define=API_BASE_URL=https://your-api.example.com`.

| Target | Base URL |
|---|---|
| Android emulator | `http://10.0.2.2:8080` |
| iOS simulator | `http://localhost:8080` |
| Physical device (same LAN) | `http://<your-machine-ip>:8080` |
| Production | your deployed host |

**Android:** cleartext HTTP is blocked by default. For local dev add
`android:usesCleartextTraffic="true"` to the `<application>` tag in
`android/app/src/main/AndroidManifest.xml`, and remove it before release.

**CORS applies to Flutter Web only.** Native builds are unaffected. If you
target web, your origin must be listed in the backend's
`app.cors.allowed-origins` (comma-separated) — the previous wildcard is gone,
so an unlisted origin will fail preflight.

---

## 3. Authentication

### 3.1 The important thing first

**Every read endpoint is public.** Pandals, metros, zones, and route
optimisation all work with no token. Auth is currently only needed for
`/actuator/**` (admin).

Build the app so browsing works fully signed-out, and treat login as optional
personalisation. Do not gate the map behind a login wall — nothing behind it
requires one.

### 3.2 Token model

- JWT, sent as `Authorization: Bearer <jwt>`.
- **Expires 1 hour after issue. There is no refresh token.** On expiry the user
  must log in again. Plan for this: keep an `exp` check client-side and route
  to login rather than letting requests fail.
- Store with `flutter_secure_storage`, never `SharedPreferences`.

```yaml
dependencies:
  dio: ^5.4.0
  flutter_secure_storage: ^9.0.0
  flutter_web_auth_2: ^3.1.0
  url_launcher: ^6.2.0
```

### 3.3 Email + password

**Sign up** — `POST /auth/signup`

```json
{ "username": "riya", "email": "riya@example.com", "password": "hunter2" }
```

`200 OK`:

```json
{ "id": 7, "username": "riya", "email": "riya@example.com", "password": null }
```

> The `password` field is always `null` — a leftover in the response DTO.
> Ignore it; do not map it into your model.

Signup does **not** return a JWT. Call `/auth/login` afterwards.

**Log in** — `POST /auth/login`

```json
{ "username": "riya", "password": "hunter2" }
```

`200 OK`:

```json
{ "jwt": "eyJhbGciOiJIUzI1NiJ9...", "userId": 7 }
```

Note the field is `jwt`, not `token` or `access_token`.

### 3.4 Google OAuth2 — code exchange flow

The backend **no longer puts the JWT in the redirect URL**. It redirects with a
single-use `code` which you exchange for the token.

```
1. App opens:  GET {baseUrl}/oauth2/authorization/google
2. User completes Google consent in a system browser tab
3. Backend redirects to:  {app.frontend.callback-url}?code=<opaque>
4. App captures the code from the redirect
5. App POSTs it to /auth/exchange and receives the JWT
```

**The code is valid for 60 seconds and dies on first use.** Exchange it
immediately; never persist or retry it.

`POST /auth/exchange`

```json
{ "code": "9fK2s...-opaque" }
```

`200 OK` → `{ "jwt": "...", "userId": 7 }`
`401` → `{ "status": 401, "error": "Unauthorized", "message": "Invalid or expired code" }`

#### Backend change required for mobile

`app.frontend.callback-url` currently points at the web app
(`https://pandal-hopper.vercel.app/auth/callback`). A Flutter mobile app cannot
receive that. Set it to a custom scheme:

```properties
app.frontend.callback-url=pandalhopper://auth/callback
```

Then register `pandalhopper` in `AndroidManifest.xml` (intent filter) and
`Info.plist` (`CFBundleURLSchemes`), and capture it:

```dart
final result = await FlutterWebAuth2.authenticate(
  url: '${ApiConfig.baseUrl}/oauth2/authorization/google',
  callbackUrlScheme: 'pandalhopper',
);

final code = Uri.parse(result).queryParameters['code'];
final res = await dio.post('/auth/exchange', data: {'code': code});
final jwt = res.data['jwt'] as String;
await storage.write(key: 'jwt', value: jwt);
```

If you need one backend to serve both web and mobile, that single property
isn't enough — it needs to become a per-request allowlisted `redirect_uri`.
Flag this to whoever owns the backend.

### 3.5 Dio setup

```dart
final dio = Dio(BaseOptions(baseUrl: ApiConfig.baseUrl))
  ..interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final jwt = await storage.read(key: 'jwt');
      if (jwt != null) options.headers['Authorization'] = 'Bearer $jwt';
      handler.next(options);
    },
    onError: (e, handler) async {
      if (e.response?.statusCode == 401) {
        // Expired or invalid token. No refresh exists — clear and re-auth.
        await storage.delete(key: 'jwt');
        authState.signedOut();
      }
      handler.next(e);
    },
  ));
```

A malformed or expired token now returns a **JSON 401**, not a silent
downgrade to anonymous:

```json
{ "status": 401, "error": "Unauthorized", "message": "Token has expired", "path": "/pandals" }
```

`message` is `"Token has expired"` or `"Invalid token"` — useful for deciding
whether to show "session expired" vs. a generic error.

---

## 4. API reference

### 4.1 Pandals

#### `GET /pandals`
All pandals, everywhere. **No pagination — this returns the entire table.**
Call once at startup and cache in memory; do not call per map pan.

```json
[ { "name": "Bagbazar Sarbojanin", "latitude": 22.6021, "longitude": 88.3654 } ]
```

#### `GET /pandals/zone/{zone}/simple`
Same shape, filtered by zone. **Prefer this over `/pandals`.**

#### `GET /pandals/zone/{zone}`
⚠️ **Do not use.** Returns a JSON object keyed by a stringified Java entity:

```json
{ "MetroStation(metroId=3, metroLat=22.58, ...)": [ { ... } ] }
```

The keys are unparseable. Use `/zone/{zone}/metros/simple` plus
`/zone/{zone}/metro/{metroId}/pandals/simple` instead.

### 4.2 Zones

#### `GET /zone/{zone}/metros/simple`
Metro stations serving a zone — use this to drop station markers.

```json
[ { "metroId": 3, "metroName": "Shyambazar", "metroLat": 22.5989, "metroLon": 88.3742 } ]
```

#### `GET /zone/{zone}/metro/{metroId}/pandals/simple`
Pandals near one station — use this to drop pandal markers.

```json
[ { "name": "Bagbazar Sarbojanin", "latitude": 22.6021, "longitude": 88.3654 } ]
```

#### `GET /zone/{zone}/metros` and `/zone/{zone}/metro/{metroId}/pandals`
Full entity versions of the two above. Heavier payloads, and `Pandal` embeds a
nested `metroStation` object. Use them only if you need `address`, `city`,
`metroDistance`, or `metroStationCode`.

<details>
<summary>Full entity shapes</summary>

```json
// MetroStation
{ "metroId": 3, "metroLat": 22.5989, "metroLon": 88.3742,
  "metroName": "Shyambazar", "metroStationCode": "SHY", "metroLine": "Blue" }

// Pandal
{ "pandalId": "3f2a...-uuid", "latitude": 22.6021, "longitude": 88.3654,
  "zone": "north", "city": "Kolkata", "name": "Bagbazar Sarbojanin",
  "address": "Bagbazar Street", "searchScore": 4.7,
  "metroDistance": 0.8, "metroDistanceUnit": "km",
  "metroStation": { "metroId": 3, "...": "..." } }
```
</details>

> **Zone values are free-text strings** matched exactly against a DB column.
> There is no endpoint that lists valid zones. Hardcode the list in the app and
> keep it in sync with the data, or ask the backend for a `/zones` endpoint.

### 4.3 Metro lookup

#### `GET /metro/nearest?lat={lat}&lon={lon}`
Nearest station to a coordinate. Returns a full `MetroStation`.

⚠️ If the table is empty this returns **`200` with an empty body**, not 404.
Guard against a null/empty response before decoding.

#### `GET /metro/nearest/location?lat={lat}&lon={lon}`
Lightweight version. Returns `404` when nothing is found (inconsistent with the
above — handle both).

```json
{ "name": 3, "lat": 22.5989, "lon": 88.3742 }
```

⚠️ **`name` is a number — it is the metro ID, misnamed in the DTO.** Map it to
`metroId` in your Dart model.

### 4.4 Route optimisation

#### `POST /api/route/optimal`

```json
{
  "startPoint": { "lat": 22.5989, "lon": 88.3742, "name": "Shyambazar Metro" },
  "pandals": [
    { "lat": 22.6021, "lon": 88.3654, "name": "Bagbazar Sarbojanin" },
    { "lat": 22.5950, "lon": 88.3700, "name": "Hatibagan" }
  ]
}
```

`200 OK` — the input reordered into an efficient visiting sequence:

```json
{
  "origin":      { "lat": 22.5989, "lon": 88.3742, "name": "Shyambazar Metro" },
  "destination": { "lat": 22.5950, "lon": 88.3700, "name": "Hatibagan" },
  "waypoints":   [ { "lat": 22.6021, "lon": 88.3654, "name": "Bagbazar Sarbojanin" } ]
}
```

- `origin` is always your `startPoint`.
- `waypoints` is the middle of the route, **already ordered**. Empty when you
  send a single pandal.
- Reconstruct the full path as `[origin, ...waypoints, destination]`.

**Constraints** — all return `400` with `{ "status": 400, "error": "Bad Request", "message": "..." }`:

| Condition | Message |
|---|---|
| `startPoint` missing | `startPoint is required` |
| `pandals` null or empty | `At least one pandal is required` |
| More than 100 pandals | `Too many pandals: N (max 100)` |
| A null entry in `pandals` | `pandals must not contain null entries` |

Validate ≤100 client-side and disable the "Build route" button past that.

> This is a **nearest-neighbour heuristic, not true TSP** — good, not optimal.
> It also returns straight-line ordering with no road network, so the sequence
> ignores rivers, one-ways, and walls. Present it as a "suggested order",
> not a turn-by-turn route.

### 4.5 What the backend does and does not give you

It gives you an **ordering**. It does not give you a drawable path.

`RouteResponseDTO` is your own input points, resequenced. There is no geometry,
no distance, no duration, no road awareness. Rendering that as a route is
entirely the frontend's job — see §5.

---

## 5. Google Maps integration

### 5.1 Division of labour

```
Backend  /api/route/optimal   →  visiting ORDER (straight-line heuristic)
Google   Directions API       →  road-following GEOMETRY between those stops
Google   Maps SDK             →  rendering markers + polylines
```

The backend orders the stops; Google draws the actual walking path between
them. You need both.

### 5.2 Packages

```yaml
dependencies:
  google_maps_flutter: ^2.6.0
  flutter_polyline_points: ^2.0.0   # decodes Google's encoded polylines
  geolocator: ^11.0.0               # device location for "Near me"
  url_launcher: ^6.2.0              # optional handoff to the Maps app
```

### 5.3 Platform setup

**Android** — `android/app/src/main/AndroidManifest.xml`, inside `<application>`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

Requires `minSdkVersion 21` in `android/app/build.gradle`.

**iOS** — `ios/Runner/AppDelegate.swift`:

```swift
import GoogleMaps

GMSServices.provideAPIKey("YOUR_KEY")   // before super.application(...)
```

Location permission strings go in `ios/Runner/Info.plist`
(`NSLocationWhenInUseUsageDescription`).

**Google Cloud Console** — enable: *Maps SDK for Android*, *Maps SDK for iOS*,
and *Directions API* (or the newer *Routes API*). All are billed beyond the
free tier.

### 5.4 ⚠️ Do not call the Directions API from the app

This matters and is easy to get wrong.

Maps SDK keys can be locked to your package name + SHA-1 (Android) or bundle ID
(iOS). **Web-service keys — which is what the Directions API uses — cannot be.**
They only support IP allowlisting, which is meaningless for mobile clients. A
Directions key shipped in a Flutter app is extractable and billable by anyone
who pulls it out of your APK.

**Proxy Directions through your own backend instead.** Ask for an endpoint like:

```
POST /api/route/directions
  { "points": [ {lat, lon}, ... ], "mode": "walking" }
→ { "encodedPolyline": "...", "distanceMeters": 4210, "durationSeconds": 3120 }
```

The key then lives in server config, is IP-restrictable, and you get one place
to cache responses. This endpoint does not exist yet — it needs building.

Until it does, use §5.7 (straight-line polylines, no Directions call, no key,
no cost) and treat road geometry as a follow-up.

### 5.5 Rendering the map

```dart
GoogleMap(
  initialCameraPosition: const CameraPosition(
    target: LatLng(22.5726, 88.3639),   // Kolkata
    zoom: 12,
  ),
  markers: _markers,
  polylines: _polylines,
  myLocationEnabled: true,
  onMapCreated: (c) => _controller.complete(c),
)
```

Fit the camera to the route once it is built:

```dart
LatLngBounds boundsOf(List<LatLng> points) {
  final lats = points.map((p) => p.latitude);
  final lngs = points.map((p) => p.longitude);
  return LatLngBounds(
    southwest: LatLng(lats.reduce(min), lngs.reduce(min)),
    northeast: LatLng(lats.reduce(max), lngs.reduce(max)),
  );
}

final c = await _controller.future;
await c.animateCamera(CameraUpdate.newLatLngBounds(boundsOf(routePoints), 60));
```

### 5.6 Numbered checkpoint markers

The whole point of the ordering is that stop 1 → 2 → 3 is visible. Default pins
don't convey that, so render numbered ones:

```dart
Future<BitmapDescriptor> numberedMarker(int n, Color color) async {
  const size = 96.0;
  final recorder = ui.PictureRecorder();
  final canvas = Canvas(recorder);

  canvas.drawCircle(const Offset(size / 2, size / 2), size / 2, Paint()..color = color);
  canvas.drawCircle(
    const Offset(size / 2, size / 2), size / 2,
    Paint()..color = Colors.white..style = PaintingStyle.stroke..strokeWidth = 6,
  );

  final painter = TextPainter(
    text: TextSpan(
      text: '$n',
      style: const TextStyle(
        fontSize: 46, color: Colors.white, fontWeight: FontWeight.bold),
    ),
    textDirection: TextDirection.ltr,
  )..layout();
  painter.paint(
    canvas,
    Offset((size - painter.width) / 2, (size - painter.height) / 2),
  );

  final image = await recorder.endRecording().toImage(size.toInt(), size.toInt());
  final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
  return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
}
```

`BitmapDescriptor.bytes()` needs `google_maps_flutter` ≥ 2.6. On older
versions use `BitmapDescriptor.fromBytes()`.

Build the marker set from the route:

```dart
final stops = [route.origin, ...route.waypoints, route.destination];

for (var i = 0; i < stops.length; i++) {
  final stop = stops[i];
  _markers.add(Marker(
    markerId: MarkerId('stop_$i'),
    position: LatLng(stop.lat, stop.lon),
    icon: await numberedMarker(
      i,
      i == 0 ? Colors.green : Colors.deepOrange,   // 0 = the metro station
    ),
    infoWindow: InfoWindow(
      title: i == 0 ? '${stop.name} (start)' : '${i}. ${stop.name}',
    ),
  ));
}
```

Note `stops[0]` is the **metro station**, not a pandal — label it as the start
and number pandals from 1.

### 5.7 Drawing the path

**Option A — straight lines (no key, no cost, ships today).**
Honest about what the backend actually computed, since its ordering is
straight-line distance anyway:

```dart
_polylines.add(Polyline(
  polylineId: const PolylineId('route'),
  points: stops.map((s) => LatLng(s.lat, s.lon)).toList(),
  width: 4,
  color: Colors.deepOrange,
  patterns: [PatternItem.dash(20), PatternItem.gap(10)],  // dashed = approximate
));
```

Use a dashed line so users don't read it as a walking path.

**Option B — road geometry via the proxied Directions endpoint (§5.4).**

```dart
final res = await dio.post('/api/route/directions', data: {
  'points': stops.map((s) => {'lat': s.lat, 'lon': s.lon}).toList(),
  'mode': 'walking',
});

final decoded = PolylinePoints()
    .decodePolyline(res.data['encodedPolyline'] as String);

_polylines.add(Polyline(
  polylineId: const PolylineId('route'),
  points: decoded.map((p) => LatLng(p.latitude, p.longitude)).toList(),
  width: 5,
  color: Colors.deepOrange,
));
```

Solid line here — it is a real path.

### 5.8 Waypoint limits and chunking

The backend accepts up to **100** pandals. The Directions API accepts far
fewer per request — on the order of 25 waypoints plus origin and destination
(confirm against current Google quota docs before relying on it).

So a 100-stop route cannot be one Directions call. Split the ordered stop list
into consecutive legs, request each, and concatenate the decoded polylines:

```dart
List<List<Point>> legs(List<Point> stops, {int maxPerLeg = 25}) {
  final out = <List<Point>>[];
  for (var i = 0; i < stops.length - 1; i += maxPerLeg - 1) {
    out.add(stops.sublist(i, min(i + maxPerLeg, stops.length)));
  }
  return out;
}
```

Each leg must **start on the previous leg's last stop** (hence `maxPerLeg - 1`),
or the path breaks at the seams.

### 5.9 Optional: let Google do the ordering instead

The Directions API can optimise waypoint order itself
(`waypoints=optimize:true|...`, returning `waypoint_order`). It is road-aware,
so it will generally beat the backend's straight-line nearest-neighbour
heuristic — which ignores rivers, one-ways, and walls.

Two viable architectures:

| | Ordering | Geometry | Notes |
|---|---|---|---|
| **A** (current) | Backend TSP | Directions | Free ordering; ordering can be visibly wrong near barriers |
| **B** | Directions `optimize:true` | Directions | Better routes, one call, more Directions spend; `/api/route/optimal` becomes unused |

Start with A since the endpoint exists. If users report the order looking odd,
B is a small change on the proxy endpoint. Worth raising with the backend owner
before building heavily on either.

### 5.10 Handing off to the Google Maps app

For actual turn-by-turn, launch the installed app:

```dart
final waypointsParam = route.waypoints.map((p) => '${p.lat},${p.lon}').join('|');
final url = Uri.parse(
  'https://www.google.com/maps/dir/?api=1'
  '&origin=${route.origin.lat},${route.origin.lon}'
  '&destination=${route.destination.lat},${route.destination.lon}'
  '&waypoints=$waypointsParam'
  '&travelmode=walking',
);
await launchUrl(url, mode: LaunchMode.externalApplication);
```

The URL scheme caps waypoints well below the Directions API (single digits).
For longer routes, offer "Navigate to next stop" per-leg rather than one link.

### 5.11 Cost control

Dynamic map loads and Directions calls are both billed.

- Cache the Directions response keyed by the ordered stop list — the same
  selection must never re-request.
- Don't rebuild the route on every marker tap; only on an explicit action.
- Set a billing alert in Google Cloud before shipping.

---

## 6. User flow

```
┌─────────────┐
│   Splash    │  read stored JWT, validate exp (no refresh available)
└──────┬──────┘
       ▼
┌─────────────┐
│    Home     │  zone picker + "Near me"        ← fully usable signed-out
└──┬───────┬──┘
   │       └──────────────────────────┐
   ▼                                  ▼
┌─────────────┐                ┌──────────────┐
│ Zone → map  │                │  Near me     │
│ metro pins  │                │ GET /metro/  │
└──────┬──────┘                │   nearest    │
       ▼                       └──────┬───────┘
┌─────────────┐                       │
│ Tap station │◄──────────────────────┘
│ pandal pins │
└──────┬──────┘
       ▼
┌─────────────┐
│  Select     │  multi-select, running count, cap 100
│  pandals    │
└──────┬──────┘
       ▼
┌─────────────┐
│ Build route │  POST /api/route/optimal
└──────┬──────┘
       ▼
┌─────────────┐
│ Route view  │  ordered list + polyline + "Open in Maps"
└─────────────┘
```

### Screen by screen

**1. Splash / bootstrap**
Read JWT from secure storage, decode `exp`, drop it if expired. Optionally
prefetch `GET /pandals` for offline browsing. Never block on auth.

**2. Home**
Zone selector (hardcoded list — see the zone note above) and a "Find pandals
near me" action. Show a signed-out affordance in the corner, not a gate.

**3. Zone map** — `GoogleMap`, station markers
`GET /zone/{zone}/metros/simple` → one marker per station
(`BitmapDescriptor.defaultMarkerWithHue(hueAzure)` reads as "transit"). Camera-fit
via §5.5. Empty array is a legitimate result for an unknown zone string — show
"No stations found in this zone", not an error.

**4. Station detail** — same map, pandal markers layered on
`GET /zone/{zone}/metro/{metroId}/pandals/simple` → pandal markers in a distinct
hue, keeping the selected station pin visible so users retain their anchor.
`DraggableScrollableSheet` over the map with the same pandals as a checkbox list;
tapping a row and tapping its marker must drive the same selection state.

**5. Selection**
Hold selections in app state across stations — a user may pick pandals near two
different stations in one trip. Change marker colour on selection so the map
reflects it. Show `n selected` and hard-stop at 100 (§4.4).

**6. Route build**
Start point = the chosen metro station, or the device location (`geolocator`) if
the user came via "Near me". POST to `/api/route/optimal`, show a loader — this
is a synchronous solve, and if you also fetch geometry (§5.7 option B) that is a
second round trip.

**7. Route result** — the payoff screen
Numbered checkpoint markers (§5.6) with the metro as stop 0 and pandals from 1,
a polyline over `[origin, ...waypoints, destination]` (dashed if straight-line,
solid if road geometry), and a synced ordered list below the map — tapping a
list row animates the camera to that marker. Add "Open in Google Maps" (§5.10).
Let the user drop a stop and re-solve; invalidate the cached geometry when they
do.

**8. Login (optional, entered from a profile corner)**
Email/password or the Google flow from §3.4. On success store the JWT and
return the user to exactly where they were.

---

## 7. Known rough edges

These are real backend behaviours you must code around today. They are tracked
in the repo's remediation plan but are **not fixed yet**.

| Behaviour | What you'll see | Client workaround |
|---|---|---|
| **Signup with a taken username returns `500`, not `409`** | Opaque server error | Treat `500` on `/auth/signup` as "username may be taken" and say so gently |
| **Login with an unknown username returns `500`; wrong password returns `401`** | Inconsistent codes for the same user-facing mistake | Show one message — "Incorrect username or password" — for both `401` and `500` on `/auth/login` |
| **OAuth2 with an email already registered under a different provider returns `500`** | Login just fails | Offer an email/password fallback on repeated OAuth2 failure |
| **No global error handler** | Most unexpected failures are `500` with a Spring error body, not your shape | Never parse error bodies except on `401`/`400`, which are structured |
| **No input validation** | Empty username/password reach the DB and may `500` | Validate non-empty client-side |
| **No pagination anywhere** | `GET /pandals` returns everything | Fetch once, cache, filter locally |
| **JWT expires in 1h, no refresh** | Sudden `401` mid-session | Pre-emptively check `exp`; re-auth silently if you can |
| **`MetroLocationDTO.name` is the numeric ID** | `"name": 3` | Map to `metroId` |
| **`SignupResponseDTO.password` is always `null`** | Dead field | Don't map it |
| **`/pandals/zone/{zone}` has unparseable object keys** | Garbage keys | Use the `/zone/...` endpoints |
| **`/metro/nearest` returns `200` + empty body when no data** | Decode failure | Null-check before parsing |

### Error response shapes

Structured (safe to parse):

```json
{ "status": 401, "error": "Unauthorized", "message": "Invalid token", "path": "/pandals" }
{ "status": 400, "error": "Bad Request", "message": "Too many pandals: 240 (max 100)" }
```

Unstructured — anything else, typically a default Spring error body. Treat as
"something went wrong".

---

## 8. Suggested project structure

```
lib/
├── core/
│   ├── config.dart              # base URL
│   ├── dio_client.dart          # interceptors, 401 handling
│   └── secure_storage.dart
├── models/
│   ├── simple_pandal.dart       # name, latitude, longitude
│   ├── metro_by_zone.dart       # metroId, metroName, metroLat, metroLon
│   ├── metro_station.dart       # full entity
│   ├── pandal.dart              # full entity
│   ├── point.dart               # lat, lon, name — route I/O
│   └── route_response.dart      # origin, destination, waypoints
├── services/
│   ├── auth_service.dart        # login, signup, google, exchange
│   ├── pandal_service.dart
│   ├── metro_service.dart
│   ├── routing_service.dart     # /api/route/optimal
│   └── directions_service.dart  # proxied geometry (§5.4) + response cache
├── maps/
│   ├── marker_factory.dart      # numbered BitmapDescriptors (§5.6)
│   ├── camera_utils.dart        # boundsOf / fitToRoute (§5.5)
│   └── polyline_builder.dart    # straight-line vs decoded (§5.7), leg chunking
├── state/
│   ├── auth_state.dart
│   └── selection_state.dart     # selected pandals across screens
└── screens/
    ├── splash/  home/  zone_map/  station_detail/  route/  login/
```

Keep everything Google-Maps-specific under `maps/`. Marker generation is async
and easy to accidentally re-run per rebuild — cache the `BitmapDescriptor`s by
number so a route rebuild doesn't re-rasterise them.

`PointDTO` is used for both route request and response — one `Point` model
serves both directions.

---

## 9. Local backend for development

```bash
cp .env.example .env      # fill in real values
docker compose up --build
```

Two things to know:

1. **`docker-compose.yaml` has no Postgres service.** You need a Postgres
   running separately and `DB_URL` pointed at it.
2. **There is no seed data and no write endpoints.** A fresh database returns
   empty arrays from every endpoint. You need pandal/metro rows loaded directly
   into Postgres before the app shows anything.

For UI work, mock the service layer rather than depending on a populated DB.

Health check: `GET /actuator/health` is public and needs no token.
