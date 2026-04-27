# Backend Local Development

## Requirements
- Java 21
- Maven 3.9+
- PostgreSQL 16 for production-like runs (optional; tests use Testcontainers)
- Docker recommended for Testcontainers and local database bootstrapping

## Default Authentication Users (HTTP Basic Auth)

For local development, the following in-memory users are pre-configured:

| Username | Password | User ID |
|----------|----------|---------|
| `user1` | `password1` | `11111111-1111-1111-1111-111111111111` |
| `user2` | `password2` | `22222222-2222-2222-2222-222222222222` |

Use these credentials in HTTP Basic Auth headers when testing endpoints. Example:
```bash
curl -u user1:password1 http://localhost:8080/api/program-sessions/next
```

## PostgreSQL Setup

### Option 1: Docker (Recommended)

```powershell
# Start PostgreSQL container
docker run --name gymtracker-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=gymtracker -p 5432:5432 -d postgres:16

# Initialize schema (migrations run automatically on app startup via Flyway)
```

### Option 2: Local PostgreSQL Install

```powershell
# Create database and user
createdb gymtracker
createuser -P gymtracker_user  # enter password when prompted
psql -d gymtracker -c "ALTER ROLE gymtracker_user WITH ENCRYPTED PASSWORD 'gymtracker_pass';"
psql -d gymtracker -c "GRANT ALL PRIVILEGES ON DATABASE gymtracker TO gymtracker_user;"
```

## Running Backend Locally

### Quick Start (In-Memory H2)

For rapid iteration without database setup, use the default in-memory H2:

```powershell
Set-Location backend
mvn spring-boot:run
```

App will be available at `http://localhost:8080/api`

### Production-Like Run (PostgreSQL)

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/gymtracker"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
Set-Location backend
mvn spring-boot:run
```

Database migrations (Flyway) will run automatically on startup.

## Running Tests

### All Tests
```powershell
Set-Location backend
mvn test
```

### Unit Tests Only
```powershell
mvn test -Dgroups="!integration"
```

### Integration Tests Only (requires Docker/Testcontainers)
```powershell
mvn test -Dgroups="integration"
```

### Performance Tests
```powershell
mvn test -Dtest=*PerformanceTest
```

See `docs/performance-validation.md` for detailed performance test instructions and expected latency targets.

## Azure OpenAI Configuration (Optional)

The backend includes async AI handoff integration. These environment variables are optional but required if enabling Azure OpenAI features:

```powershell
$env:AZURE_OPENAI_ENDPOINT = "https://your-instance.openai.azure.com/"
$env:AZURE_OPENAI_API_KEY = "your-api-key"
$env:AZURE_OPENAI_DEPLOYMENT = "gpt-35-turbo"
$env:AI_HANDOFF_TIMEOUT_SECONDS = "30"  # default
$env:AI_HANDOFF_MAX_ATTEMPTS = "3"      # default
```

If these are not set, AI handoff will be disabled gracefully (no errors).

## Building & Packaging

### Build Only
```powershell
Set-Location backend
mvn clean package -DskipTests
```

### Build with Tests
```powershell
mvn clean package
```

Output JAR: `backend/target/gymtracker-api-*.jar`

## Troubleshooting

### "Port 8080 already in use"
```powershell
# Change the port
$env:SERVER_PORT = "8081"
mvn spring-boot:run
```

### "PostgreSQL connection refused"
- Ensure PostgreSQL is running: `docker ps` (Docker) or `sudo service postgresql status` (Linux/Mac)
- Verify connection string: `SPRING_DATASOURCE_URL`
- Check username/password: `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`

### "Migration failed or schema missing"
- Flyway migrations run automatically from `backend/src/main/resources/db/migration/`
- Check logs for migration errors
- Reset database: `dropdb gymtracker; createdb gymtracker` then restart app

### "HTTP 401 Unauthorized"
- Ensure HTTP Basic Auth header is included with request
- Use username/password from table above
- Example: `-u user1:password1` in curl

### "Tests fail with Testcontainers error"
- Ensure Docker is running: `docker ps`
- On Windows, verify Docker Desktop is started
- Check Docker resources (CPU/memory) are available

