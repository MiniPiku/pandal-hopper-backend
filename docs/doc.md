# Running Pandal Hopper Locally

Setup guide for the Spring Boot backend. For the API contracts see
[apis.md](apis.md); for the Flutter client see [frontend.md](frontend.md).

> **Read [§7 Gotchas](#7-gotchas-read-before-you-start) first.** The default
> config is written for the deployed environment, and three settings will stop
> a local run cold if you don't override them. This guide works around all
> three.

---

## 1. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **17** | `java -version` must report 17.x. The build targets 17. |
| Maven | — | Not needed; use the bundled `./mvnw` wrapper. |
| Docker | any recent | Easiest way to get Postgres + Redis. |
| PostgreSQL | 14+ | Via Docker below, or a local install. |
| Redis | 7.x | Via Docker below. |

Windows: use Git Bash or WSL for the `./mvnw` commands. PowerShell works too,
but the shell snippets below are POSIX.

---

## 2. Quick start

Five steps from clone to a responding API.

```bash
# 1. Infrastructure
docker run -d --name ph-postgres \
  -e POSTGRES_DB=pandal_hopper \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16-alpine

docker run -d --name ph-redis -p 6379:6379 redis:7.2-alpine

# 2. Environment
cp .env.example .env
#    then edit .env — see §3

# 3. Local profile overrides (required — see §7)
#    create src/main/resources/application-local.properties from §4

# 4. Run. Creates the tables on first boot via ddl-auto=update.
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# 5. Seed data — the DB is empty and there are no write endpoints (§5)
psql postgresql://postgres:postgres@localhost:5432/pandal_hopper -f seed.sql
```

Verify:

```bash
curl http://localhost:8080/actuator/health     # {"status":"UP"}
curl http://localhost:8080/pandals             # seeded pandals
```

---

## 3. Environment variables

`cp .env.example .env`, then fill in. The app reads `.env` via the
`spring-dotenv` dependency — it is git-ignored and must stay that way.

| Variable | Local value | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/pandal_hopper` | JDBC URL, not a `postgres://` URI |
| `DB_USER` | `postgres` | |
| `DB_PASS` | `postgres` | |
| `jwt-Secret-Key` | generate — see below | **Must be ≥ 32 bytes** |
| `oauth2-google-clientId` | `dummy-client-id` | Any non-empty string is fine unless testing Google login |
| `oauth2-google-clientSecret` | `dummy-client-secret` | Same |
| `REDIS_URL` | `redis://localhost:6379` | Note: **no** `rediss://` locally |

Generate a valid JWT secret:

```bash
openssl rand -base64 48
```

Anything shorter than 256 bits makes JJWT throw `WeakKeyException` on the first
login attempt — not at startup, so it looks like a login bug.

Google OAuth2 credentials can stay as dummy strings. The client registers at
startup regardless; it only fails when you actually walk the Google flow. To
test that flow for real, create an OAuth2 client in Google Cloud Console with
redirect URI `http://localhost:8080/login/oauth2/code/google`.

---

## 4. Local profile overrides

Create `src/main/resources/application-local.properties`:

```properties
# --- Redis: production forces TLS; a local container doesn't speak it ---
spring.data.redis.ssl.enabled=false

# --- Don't let Boot auto-run docker-compose.yaml (it would build the app
#     container while you're running the app from Maven) ---
spring.docker.compose.enabled=false

# --- Quieter logs; flip show-sql back on when debugging queries ---
spring.jpa.show-sql=false

# --- Point OAuth2 + CORS at a local frontend ---
app.frontend.callback-url=http://localhost:5173/auth/callback
app.cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

Then always run with `SPRING_PROFILES_ACTIVE=local`. Profile-specific
properties override the base file, so nothing in `application.properties` needs
editing — which keeps your local setup out of commits.

This file contains no secrets and is safe to commit.

---

## 5. Database and seed data

### Schema

`spring.jpa.hibernate.ddl-auto=update` creates the tables on first boot. Start
the app once before seeding.

> This is convenient locally and a liability in production — Hibernate mutating
> a live schema. Migrating to Flyway is tracked as a known issue.

### Seeding

**There are no write endpoints.** No POST for pandals or metro stations exists,
so a fresh database returns `[]` from every endpoint and the app looks broken.
Seed via SQL.

Save as `seed.sql` and run it after the first boot:

```sql
-- Metro stations
INSERT INTO metro_station (metro_lat, metro_lon, metro_name, metro_station_code, metro_line) VALUES
  (22.6238, 88.3987, 'Dum Dum',          'DUM', 'Blue'),
  (22.5989, 88.3742, 'Shyambazar',       'SHY', 'Blue'),
  (22.5860, 88.3639, 'Girish Park',      'GIP', 'Blue'),
  (22.5645, 88.3512, 'Esplanade',        'ESP', 'Blue'),
  (22.5203, 88.3435, 'Kalighat',         'KAL', 'Blue'),
  (22.5108, 88.3462, 'Rabindra Sarobar', 'RBS', 'Blue');

-- Pandals, linked to the nearest station
INSERT INTO pandals
  (pandal_id, latitude, longitude, zone, city, name, address,
   search_score, metro_distance, metro_distance_unit, metro_id)
VALUES
  (gen_random_uuid(), 22.6021, 88.3654, 'north',   'Kolkata', 'Bagbazar Sarbojanin',   'Bagbazar Street',        4.8, 0.9, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'SHY')),
  (gen_random_uuid(), 22.5952, 88.3601, 'north',   'Kolkata', 'Kumartuli Park',        'Kumartuli',              4.6, 1.3, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'SHY')),
  (gen_random_uuid(), 22.5901, 88.3688, 'north',   'Kolkata', 'Ahiritola Sarbojanin',  'Ahiritola Street',       4.4, 0.7, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'GIP')),
  (gen_random_uuid(), 22.5745, 88.3620, 'central', 'Kolkata', 'Mohammad Ali Park',     'Central Avenue',         4.5, 1.1, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'GIP')),
  (gen_random_uuid(), 22.5622, 88.3701, 'central', 'Kolkata', 'Santosh Mitra Square',  'Sealdah',                4.7, 1.6, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'ESP')),
  (gen_random_uuid(), 22.5189, 88.3661, 'south',   'Kolkata', 'Ekdalia Evergreen',     'Ekdalia Road',           4.9, 1.4, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'KAL')),
  (gen_random_uuid(), 22.5142, 88.3552, 'south',   'Kolkata', 'Singhi Park',           'Dover Lane',             4.6, 1.0, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'KAL')),
  (gen_random_uuid(), 22.5098, 88.3471, 'south',   'Kolkata', 'Mudiali Club',          'Rabindra Sarobar',       4.5, 0.3, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'RBS')),
  (gen_random_uuid(), 22.5171, 88.3487, 'south',   'Kolkata', 'Deshapriya Park',       'Rashbehari Avenue',      4.8, 0.8, 'km',
     (SELECT metro_id FROM metro_station WHERE metro_station_code = 'RBS'));
```

`gen_random_uuid()` is built in on Postgres 13+. On older versions run
`CREATE EXTENSION IF NOT EXISTS pgcrypto;` first.

The zone values here — `north`, `central`, `south` — are matched **exactly** by
the API. There is no endpoint listing valid zones, so whatever you seed is the
contract the frontend must hardcode.

Sanity check:

```bash
psql postgresql://postgres:postgres@localhost:5432/pandal_hopper \
  -c "SELECT zone, COUNT(*) FROM pandals GROUP BY zone;"
```

---

## 6. Running

### From Maven

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

### Build a jar

```bash
./mvnw clean package -DskipTests
SPRING_PROFILES_ACTIVE=local java -jar target/Pandal-Hopperv2-0.0.1-SNAPSHOT.jar
```

`-DskipTests` is needed because `PandalHopperv2ApplicationTests.contextLoads()`
requires live Postgres and Redis — see §8.

### Docker Compose

`docker-compose.yaml` **has no Postgres service**, only Redis and the app. It
cannot start a working stack on its own. Either add Postgres to it, or run the
containers from §2 and use Compose only for the app.

It also inherits the TLS problem: `application.properties` sets
`spring.data.redis.url` from `REDIS_URL`, which takes precedence over the
`SPRING_DATA_REDIS_HOST: redis` that Compose injects — so the container ignores
the Compose Redis and dials whatever `REDIS_URL` points at.

If you want Compose to work, add to `docker-compose.yaml`:

```yaml
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: pandal_hopper
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports: ["5432:5432"]
    volumes: [pg_data:/var/lib/postgresql/data]
```

and set, in the `backend` service environment:

```yaml
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATA_REDIS_URL: redis://redis:6379
      DB_URL: jdbc:postgresql://postgres:5432/pandal_hopper
```

plus `postgres` under `depends_on`. Then `docker compose up --build`.

---

## 7. Gotchas (read before you start)

Ordered by how much time they'll cost you.

**1. Redis TLS is hardcoded on.**
`spring.data.redis.ssl.enabled=true` sits in `application.properties` with no
profile guard. Against a local non-TLS Redis every connection fails. Because
cached endpoints now degrade to the database rather than erroring, the symptom
is *silent*: everything works but nothing is ever cached, and the log fills with
`Cache GET failed ... falling back to source`. Fixed by the §4 override.

**2. The JWT secret must be ≥ 32 bytes.**
Shorter secrets throw `WeakKeyException` on first login, not at startup.

**3. Boot will try to run docker-compose.yaml at startup.**
`spring.docker.compose.enabled=true` plus the `spring-boot-docker-compose`
dependency means starting the app locally tries to bring up the Compose
file — including the `backend` service, i.e. a second copy of the app.
Disabled by the §4 override.

**4. An empty database looks like a broken app.**
No write endpoints exist. Every endpoint returns `[]` until you seed (§5).

**5. `./mvnw -o` fails for some goals.**
The repo has been built offline; `-o` works for `compile` and `test` but not
for goals whose plugins were never downloaded (`dependency:tree`). Drop `-o`
when you hit `Cannot access central ... in offline mode`.

---

## 8. Tests

```bash
./mvnw test
```

**This currently fails**, and not because of your setup.
`PandalHopperv2ApplicationTests.contextLoads()` is a `@SpringBootTest` that
boots the whole context, so it needs a live Postgres, a live Redis, and every
environment variable present. On a clean checkout it cannot pass.

The cache tests are self-contained and need no infrastructure:

```bash
./mvnw test -Dtest='ResilientCacheErrorHandlerTest,CacheFallbackIntegrationTest'
```

To run the full suite, have the §2 containers up and `.env` populated.

---

## 9. Project layout

```
src/main/java/org/minipiku/pandalhopperv2/
├── Cache/         Redis cache config + error handler (DB fallback)
├── Controller/    REST endpoints — see apis.md
├── DTOs/          Request/response shapes
│   ├── AuthDTO/         login, signup
│   ├── MetroPandalDTO/  lightweight map payloads
│   └── RouteDTO/        route request/response
├── Entity/        JPA entities (User, Pandal, MetroStation)
├── Repository/    Spring Data JPA repositories
├── Security/      JWT filter, OAuth2, auth service, security config
├── Service/       Business logic (Pandal, Metro, Routing)
└── Utility/       Haversine distance, TSP nearest-neighbour solver
```

Key files:

| File | Role |
|---|---|
| `Security/WebSecurityConfig.java` | Which endpoints are public vs. authenticated; CORS |
| `Security/JwtAuthFilter.java` | Bearer token validation, 401 on invalid |
| `Cache/CacheConfig.java` | Redis cache manager + DB-fallback error handler |
| `Service/PandalService.java` | Cached pandal/zone queries |
| `Utility/TSPSolver.java` | Route ordering heuristic |
| `resources/application.properties` | Base config; override per-profile, don't edit |

---

## 10. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `WeakKeyException` on login | JWT secret < 32 bytes | Regenerate (§3) |
| `RedisConnectionFailureException` in logs, app still responds | TLS mismatch | §4 override |
| Every endpoint returns `[]` | No seed data | Run `seed.sql` (§5) |
| `Connection refused: localhost:5432` | Postgres not running | `docker start ph-postgres` |
| `Port 8080 already in use` | Another process | `--server.port=8081` |
| `contextLoads()` fails | Needs live infra | §8 |
| `500` on signup | Username already taken — returns 500, not 409 | Known issue; use a different username |
| `500` on login | Unknown *username* returns 500; wrong *password* returns 401 | Known issue; check the username exists |
| `Cannot access central ... offline mode` | `-o` flag | Drop `-o` |
| CORS blocked from browser | Origin not allowlisted | Add to `app.cors.allowed-origins` (§4) |

Useful logging while debugging:

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.minipiku.pandalhopperv2=DEBUG
spring.jpa.show-sql=true
```
