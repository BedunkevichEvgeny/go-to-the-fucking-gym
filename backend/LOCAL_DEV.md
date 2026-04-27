# Backend Local Development

## Requirements
- Java 21
- Maven 3.9+
- PostgreSQL 16 for production-like runs
- Docker recommended for Testcontainers and local database bootstrapping

## Default authentication users
| Username | Password | User ID |
|----------|----------|---------|
| `user1` | `password1` | `11111111-1111-1111-1111-111111111111` |
| `user2` | `password2` | `22222222-2222-2222-2222-222222222222` |

## Run locally
```powershell
Set-Location backend
mvn spring-boot:run
```

By default the app uses in-memory H2 for quick local boot. To use PostgreSQL, set:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/gymtracker"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
mvn spring-boot:run
```

## Tests
```powershell
Set-Location backend
mvn test
```

## AI configuration
Optional environment variables for the async handoff integration:
- `AZURE_OPENAI_ENDPOINT`
- `AZURE_OPENAI_API_KEY`
- `AZURE_OPENAI_DEPLOYMENT`
- `AI_HANDOFF_TIMEOUT_SECONDS` (default `30`)
- `AI_HANDOFF_MAX_ATTEMPTS` (default `3`)

