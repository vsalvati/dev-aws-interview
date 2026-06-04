# Task Manager API

A REST API for managing projects and tasks, built with Spring Boot 3.5.14 and Java 21. Deployed to AWS Fargate with Terraform.

## Tech Stack

- **Java 21** / **Spring Boot 3.5.14**
- **PostgreSQL** (RDS) with Flyway migrations
- **Amazon SQS** for async task event notifications (with DLQ)
- **Docker** / **AWS Fargate** for containerized deployment
- **Terraform** for infrastructure-as-code
- **GitHub Actions** for CI/CD
- **Testcontainers** for integration tests

## Running Locally

### With H2 (simplest)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

### With Docker Compose (Postgres + LocalStack)

```bash
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Running Tests

```bash
./mvnw test
```

Requires Docker (Testcontainers uses a real PostgreSQL instance).

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/users` | Create user |
| GET | `/api/users` | List users (paginated) |
| GET | `/api/users/{id}` | Get user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List projects (filter by status) |
| GET | `/api/projects/{id}` | Get project |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |
| POST | `/api/projects/{id}/members?userId=` | Add member |
| DELETE | `/api/projects/{id}/members/{userId}` | Remove member |
| POST | `/api/projects/{projectId}/tasks` | Create task |
| GET | `/api/projects/{projectId}/tasks` | List project tasks |
| GET | `/api/tasks` | Search tasks (status, priority, assigneeId) |
| GET | `/api/tasks/{id}` | Get task |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |
| PATCH | `/api/tasks/{id}/status` | Change task status |
| PATCH | `/api/tasks/{id}/assign` | Assign/unassign task |

## Deploying to AWS

### Prerequisites
- AWS CLI configured
- Terraform installed
- Docker installed

### Steps

```bash
# 1. Initialize Terraform
cd infra
terraform init

# 2. Apply infrastructure
terraform apply -var="db_password=YOUR_SECURE_PASSWORD" -var="container_image=PLACEHOLDER"

# 3. Build and push Docker image
aws ecr get-login-password | docker login --username AWS --password-stdin <ECR_URL>
docker build -t <ECR_URL>/task-manager:latest .
docker push <ECR_URL>/task-manager:latest

# 4. Update with real image
terraform apply -var="db_password=YOUR_SECURE_PASSWORD" -var="container_image=<ECR_URL>/task-manager:latest"
```

## Architecture

```
Client → ALB → ECS Fargate → RDS PostgreSQL
                    ↓
                   SQS → Consumer (notifications)
                    ↓ (on failure)
                   DLQ → CloudWatch Alarm
```
