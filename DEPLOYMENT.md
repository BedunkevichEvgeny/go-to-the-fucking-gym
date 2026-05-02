# Deployment Guide: Workout Tracker

## Overview

This guide covers deploying the Workout Tracker application to production environments. The application consists of:

- **Backend**: Java 21 Spring Boot 4.0.5 microservice
- **Frontend**: React 18 TypeScript SPA
- **Database**: PostgreSQL 16
- **AI Integration**: Azure OpenAI (optional)

**Recommended Deployment Target**: Containerized (Docker) on Kubernetes or Cloud platform (Azure Container Instances, AWS ECS, etc.)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Backend Deployment](#backend-deployment)
3. [Frontend Deployment](#frontend-deployment)
4. [Database Deployment](#database-deployment)
5. [Environment Configuration](#environment-configuration)
6. [Docker Compose](#docker-compose-local-staging)
7. [Azure Deployment](#azure-deployment-example)
8. [Monitoring & Scaling](#monitoring--scaling)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Tools Required

- Docker & Docker Compose (for containerization)
- Kubernetes CLI (`kubectl`) if deploying to K8s
- Azure CLI (`az`) if deploying to Azure
- Git for version control

### Accounts Required

- Docker Hub or private container registry (for image storage)
- PostgreSQL hosting (managed service or self-hosted)
- Azure account (if using Azure OpenAI, Azure Database for PostgreSQL, etc.)

---

## Backend Deployment

### 1. Build Docker Image

#### Step 1: Create Dockerfile

**File**: `backend/Dockerfile`

```dockerfile
# Multi-stage build for optimized image size
FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /build

# Copy pom.xml and resolve dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/gymtracker-api-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.launch.JarLauncher actuator || exit 1

# Run application
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

#### Step 2: Build Image

```bash
cd backend
docker build -t gymtracker-api:latest .

# Tag for registry (example: Docker Hub)
docker tag gymtracker-api:latest myregistry/gymtracker-api:latest
```

#### Step 3: Push to Registry

```bash
# Login to registry
docker login

# Push image
docker push myregistry/gymtracker-api:latest
```

### 2. Run Backend Container

```bash
docker run -d \
  --name gymtracker-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://db-host:5432/gymtracker" \
  -e SPRING_DATASOURCE_USERNAME="gymtracker_user" \
  -e SPRING_DATASOURCE_PASSWORD="secure_password" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  -e AZURE_OPENAI_ENDPOINT="https://your-instance.openai.azure.com/" \
  -e AZURE_OPENAI_API_KEY="your-api-key" \
  -e AZURE_OPENAI_DEPLOYMENT="gpt-35-turbo" \
  myregistry/gymtracker-api:latest
```

### 3. Kubernetes Deployment (Optional)

**File**: `backend/k8s-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gymtracker-api
  namespace: default
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: gymtracker-api
  template:
    metadata:
      labels:
        app: gymtracker-api
    spec:
      containers:
      - name: gymtracker-api
        image: myregistry/gymtracker-api:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: gymtracker-db
              key: connection-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: gymtracker-db
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: gymtracker-db
              key: password
        - name: AZURE_OPENAI_ENDPOINT
          valueFrom:
            secretKeyRef:
              name: azure-openai
              key: endpoint
        - name: AZURE_OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: azure-openai
              key: api-key
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: gymtracker-api
spec:
  selector:
    app: gymtracker-api
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
  type: LoadBalancer
```

Deploy:
```bash
kubectl apply -f backend/k8s-deployment.yaml
kubectl get pods -l app=gymtracker-api
kubectl get svc gymtracker-api
```

---

## Frontend Deployment

### 1. Build SPA

```bash
cd frontend
npm install
npm run build
```

Output: `frontend/dist/` directory (static files)

### 2. Create Dockerfile for Frontend

**File**: `frontend/Dockerfile`

```dockerfile
# Build stage
FROM node:20-alpine as builder

WORKDIR /build

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Serve stage (nginx)
FROM nginx:alpine

# Copy nginx config
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf /etc/nginx/conf.d/default.conf

# Copy built SPA
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**File**: `frontend/nginx.conf`

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
  worker_connections 1024;
}

http {
  include /etc/nginx/mime.types;
  default_type application/octet-stream;

  log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                  '$status $body_bytes_sent "$http_referer" '
                  '"$http_user_agent" "$http_x_forwarded_for"';

  access_log /var/log/nginx/access.log main;

  sendfile on;
  tcp_nopush on;
  tcp_nodelay on;
  keepalive_timeout 65;
  types_hash_max_size 2048;
  client_max_body_size 20M;

  gzip on;
  gzip_vary on;
  gzip_proxied any;
  gzip_comp_level 6;
  gzip_types text/plain text/css text/xml text/javascript 
             application/json application/javascript application/xml+rss 
             application/rss+xml font/truetype font/opentype 
             application/vnd.ms-fontobject image/svg+xml;

  include /etc/nginx/conf.d/*.conf;
}
```

**File**: `frontend/default.conf`

```nginx
server {
  listen 80;
  server_name _;

  root /usr/share/nginx/html;
  index index.html;

  # SPA routing: serve index.html for all routes
  location / {
    try_files $uri $uri/ /index.html;
  }

  # API proxy (optional, if not using CORS)
  location /api/ {
    proxy_pass http://backend-service:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  # Cache static assets
  location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
  }

  # Health check endpoint
  location /health {
    access_log off;
    return 200 "healthy\n";
    add_header Content-Type text/plain;
  }
}
```

### 3. Build & Push Frontend Image

```bash
cd frontend
docker build -t gymtracker-web:latest .
docker tag gymtracker-web:latest myregistry/gymtracker-web:latest
docker push myregistry/gymtracker-web:latest
```

### 4. Deploy Frontend

```bash
docker run -d \
  --name gymtracker-web \
  -p 80:80 \
  -e VITE_API_BASE_URL="https://api.yourhost.com/api" \
  myregistry/gymtracker-web:latest
```

### 5. Kubernetes Deployment for Frontend

**File**: `frontend/k8s-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gymtracker-web
  namespace: default
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gymtracker-web
  template:
    metadata:
      labels:
        app: gymtracker-web
    spec:
      containers:
      - name: gymtracker-web
        image: myregistry/gymtracker-web:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 80
          name: http
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
        livenessProbe:
          httpGet:
            path: /health
            port: 80
          initialDelaySeconds: 10
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: gymtracker-web
spec:
  selector:
    app: gymtracker-web
  ports:
  - port: 80
    targetPort: 80
    protocol: TCP
  type: LoadBalancer
```

---

## Database Deployment

### 1. PostgreSQL Setup

#### Option A: Managed Service (Recommended)

**Azure Database for PostgreSQL**:

```bash
# Create resource group
az group create --name gymtracker-rg --location eastus

# Create PostgreSQL server
az postgres server create \
  --resource-group gymtracker-rg \
  --name gymtracker-db-server \
  --location eastus \
  --admin-user dbadmin \
  --admin-password SecurePassword123! \
  --sku-name B_Gen5_2 \
  --storage-size 51200 \
  --backup-retention 7 \
  --geo-redundant-backup Disabled

# Create database
az postgres db create \
  --resource-group gymtracker-rg \
  --server-name gymtracker-db-server \
  --name gymtracker

# Get connection string
az postgres server show-connection-string \
  --admin-user dbadmin \
  --name gymtracker-db-server
```

#### Option B: Docker Compose (Local/Staging)

**File**: `docker-compose.yml` (see Docker Compose section below)

### 2. Database Migrations

Migrations run automatically on backend startup via Flyway:

1. Spring Boot detects pending migrations in `src/main/resources/db/migration/`
2. Flyway applies migrations in version order
3. Schema is ready before first request

To manually run migrations:

```bash
# Connect to database
psql -h db-host -U postgres -d gymtracker

# Check Flyway history
SELECT * FROM flyway_schema_history;

# Verify schema
\dt
```

### 3. Backup Strategy

```bash
# Daily backup (cron job on database host)
0 2 * * * pg_dump -U postgres -d gymtracker -F c > /backups/gymtracker-$(date +\%Y\%m\%d).dump

# Restore from backup
pg_restore -U postgres -d gymtracker /backups/gymtracker-20260427.dump
```

---

## Environment Configuration

### Required Variables

| Variable | Example Value | Notes |
|----------|---------------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db-host:5432/gymtracker` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `gymtracker_user` | DB user (non-admin) |
| `SPRING_DATASOURCE_PASSWORD` | `SecurePassword123!` | DB password (use secrets manager) |
| `SPRING_PROFILES_ACTIVE` | `prod` | Production Spring profile |
| `SERVER_PORT` | `8080` | Backend port (keep internal) |
| `VITE_API_BASE_URL` | `https://api.yourhost.com/api` | Frontend API endpoint |

### Optional Variables (AI Integration)

| Variable | Example Value | Notes |
|----------|---------------|-------|
| `AZURE_OPENAI_ENDPOINT` | `https://your-instance.openai.azure.com/` | Azure OpenAI service endpoint |
| `AZURE_OPENAI_API_KEY` | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` | Azure OpenAI key (use secrets manager) |
| `AZURE_OPENAI_DEPLOYMENT` | `gpt-35-turbo` | Deployment name in Azure |
| `AI_HANDOFF_TIMEOUT_SECONDS` | `30` | Timeout for AI processing |
| `AI_HANDOFF_MAX_ATTEMPTS` | `3` | Retry attempts for AI handoff |

### Feature 002 Rollout Notes (Profile Goal Onboarding)

- Keep `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, and `AZURE_OPENAI_DEPLOYMENT` configured in all environments where onboarding proposal generation is enabled.
- Flyway migration `V002__profile_goal_onboarding.sql` creates onboarding attempt/proposal/feedback/activation tables and must run before backend pods start serving traffic.
- Existing feature-001 tables are unchanged by `V002`; rollback should be handled as a forward-fix migration instead of deleting applied migration history.
- During canary rollout, validate acceptance flow with one test user and confirm `/api/program-sessions/next` returns data from the activated program after onboarding acceptance.

### Secrets Management

**Azure Key Vault** (Recommended):

```bash
# Create key vault
az keyvault create --resource-group gymtracker-rg --name gymtracker-vault

# Store secrets
az keyvault secret set --vault-name gymtracker-vault \
  --name db-password \
  --value "SecurePassword123!"

az keyvault secret set --vault-name gymtracker-vault \
  --name azure-openai-key \
  --value "your-api-key"

# Reference in deployment (Kubernetes)
# Use Secrets Store CSI Driver or Pod Identity
```

**Docker Secrets** (Compose):

```bash
docker secret create db_password -
docker secret create azure_key -

# In docker-compose.yml
environment:
  SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/db_password
```

---

## Docker Compose (Local & Staging)

**File**: `docker-compose.yml`

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: gymtracker-db
    environment:
      POSTGRES_DB: gymtracker
      POSTGRES_USER: gymtracker_user
      POSTGRES_PASSWORD: gymtracker_pass
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/src/main/resources/db/migration:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gymtracker_user -d gymtracker"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: gymtracker-api
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gymtracker
      SPRING_DATASOURCE_USERNAME: gymtracker_user
      SPRING_DATASOURCE_PASSWORD: gymtracker_pass
      SPRING_PROFILES_ACTIVE: dev
      AZURE_OPENAI_ENDPOINT: ${AZURE_OPENAI_ENDPOINT:-}
      AZURE_OPENAI_API_KEY: ${AZURE_OPENAI_API_KEY:-}
      AZURE_OPENAI_DEPLOYMENT: ${AZURE_OPENAI_DEPLOYMENT:-}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: gymtracker-web
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

**Run Compose Stack**:

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend

# Stop all services
docker-compose down
```

---

## Azure Deployment Example

### 1. Create Azure Container Registry

```bash
# Create registry
az acr create --resource-group gymtracker-rg \
  --name gymtrackeracr \
  --sku Basic

# Login
az acr login --name gymtrackeracr
```

### 2. Build & Push Images

```bash
# Backend
az acr build --registry gymtrackeracr \
  --image gymtracker-api:latest \
  --file backend/Dockerfile \
  ./backend

# Frontend
az acr build --registry gymtrackeracr \
  --image gymtracker-web:latest \
  --file frontend/Dockerfile \
  ./frontend
```

### 3. Deploy to Azure Container Instances

```bash
# Backend
az container create \
  --resource-group gymtracker-rg \
  --name gymtracker-api-container \
  --image gymtrackeracr.azurecr.io/gymtracker-api:latest \
  --cpu 1 --memory 1 \
  --registry-login-server gymtrackeracr.azurecr.io \
  --registry-username <username> \
  --registry-password <password> \
  --port 8080 \
  --environment-variables \
    SPRING_DATASOURCE_URL="jdbc:postgresql://gymtracker-db-server.postgres.database.azure.com:5432/gymtracker" \
    SPRING_DATASOURCE_USERNAME="dbadmin@gymtracker-db-server" \
    SPRING_PROFILES_ACTIVE="prod"

# Frontend
az container create \
  --resource-group gymtracker-rg \
  --name gymtracker-web-container \
  --image gymtrackeracr.azurecr.io/gymtracker-web:latest \
  --cpu 0.5 --memory 0.5 \
  --registry-login-server gymtrackeracr.azurecr.io \
  --registry-username <username> \
  --registry-password <password> \
  --port 80 \
  --environment-variables \
    VITE_API_BASE_URL="http://gymtracker-api-container:8080/api"
```

### 4. Setup Application Gateway (Load Balancer)

```bash
az network application-gateway create \
  --resource-group gymtracker-rg \
  --name gymtracker-gateway \
  --location eastus \
  --http-settings-cookie-based-affinity Disabled \
  --frontend-port 443 \
  --http-settings-port 80 \
  --sku Standard_Small \
  --cert-file /path/to/cert.pfx \
  --cert-password <password>
```

---

## Monitoring & Scaling

### Application Insights (Azure)

```bash
# Create Application Insights
az monitor app-insights component create \
  --app gymtracker-insights \
  --location eastus \
  --resource-group gymtracker-rg \
  --application-type web

# Add instrumentation key to backend
export APPINSIGHTS_INSTRUMENTATIONKEY="<key>"
```

### Horizontal Pod Autoscaling (Kubernetes)

**File**: `backend/k8s-hpa.yaml`

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: gymtracker-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gymtracker-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

Apply:
```bash
kubectl apply -f backend/k8s-hpa.yaml
```

### Logging & Alerting

**ELK Stack** (Elasticsearch, Logstash, Kibana):

Configure Logstash to collect Spring Boot logs:

```xml
<!-- application.properties -->
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.level.com.gymtracker=DEBUG
logging.level.org.springframework=INFO
```

Send logs to ELK:
```bash
docker run -d --name logstash -p 5000:5000/tcp logstash:8.0 -e 'input { tcp { port => 5000 } } output { elasticsearch { hosts => ["es:9200"] } }'
```

---

## Troubleshooting

### Backend Container Won't Start

```bash
# Check logs
docker logs gymtracker-api

# Common issues:
# 1. Database connection failed
#    → Verify SPRING_DATASOURCE_URL, credentials, firewall
# 2. Migration failed
#    → Check flyway_schema_history table, SQL syntax errors
# 3. Out of memory
#    → Increase container memory limit
```

### Frontend Shows "API Error"

```bash
# Check VITE_API_BASE_URL
docker inspect gymtracker-web | grep VITE_API_BASE_URL

# Verify backend is reachable
curl http://backend:8080/api/health

# Check browser console for CORS errors
# Add CORS headers to backend if needed:
# corsFilter.setAllowedOrigins(["https://yourfrontend.com"])
```

### Database Locks

```bash
# List active queries
SELECT * FROM pg_stat_activity WHERE state = 'active';

# Kill blocking query
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid <> pg_backend_pid();
```

### Slow Queries

```bash
# Enable query logging
log_statement = 'all'
log_min_duration_statement = 1000  # 1 second

# Analyze slow query
EXPLAIN ANALYZE SELECT ...;
```

---

## Post-Deployment Checklist

- [ ] Database migrations completed successfully
- [ ] Backend health check passing (`/actuator/health`)
- [ ] Frontend renders and calls API successfully
- [ ] Authentication works (HTTP Basic Auth)
- [ ] Can log program session end-to-end
- [ ] Can view history and progression
- [ ] Monitoring/alerting configured
- [ ] Backups scheduled and tested
- [ ] SSL/TLS certificates valid
- [ ] Scaling policies configured (HPA if Kubernetes)
- [ ] Logs aggregated to central logging
- [ ] Runbooks documented for ops team

