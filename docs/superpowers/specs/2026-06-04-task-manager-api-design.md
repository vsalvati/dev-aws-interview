# Task Manager REST API — Design Spec

## Context

This project is an interview showcase for a position using Spring Boot 3.5.14, Java 21, and AWS. The goal is to demonstrate clean backend engineering, AWS integration, infrastructure-as-code, and professional development practices through a Task/Project Management REST API deployed to AWS Fargate.

## Tech Stack

- **Runtime:** Java 21, Spring Boot 3.5.14
- **Database:** PostgreSQL (RDS in prod, Testcontainers in test, H2 in local)
- **Messaging:** Amazon SQS (task event notifications + DLQ)
- **ORM:** Spring Data JPA / Hibernate
- **Migrations:** Flyway
- **API Docs:** SpringDoc OpenAPI (Swagger UI)
- **Testing:** JUnit 5, Mockito, Testcontainers
- **CI/CD:** GitHub Actions
- **Infrastructure:** Terraform (VPC, ECS Fargate, RDS, SQS, ECR, ALB)
- **Containerization:** Docker (multi-stage build)

## Domain Model

### Entities

**User**
- `id` (UUID, PK)
- `username` (String, unique, not blank)
- `email` (String, unique, valid email)
- `role` (Enum: ADMIN, MEMBER)
- `createdAt` (Instant)

**Project**
- `id` (UUID, PK)
- `name` (String, not blank, max 100)
- `description` (String, max 500)
- `status` (Enum: ACTIVE, ARCHIVED)
- `createdAt`, `updatedAt` (Instant)

**Task**
- `id` (UUID, PK)
- `title` (String, not blank, max 200)
- `description` (String, max 2000)
- `status` (Enum: TODO, IN_PROGRESS, DONE)
- `priority` (Enum: LOW, MEDIUM, HIGH)
- `dueDate` (LocalDate, optional)
- `project` (ManyToOne -> Project, required)
- `assignee` (ManyToOne -> User, optional)
- `createdAt`, `updatedAt` (Instant)

### Relationships
- Project has many Tasks (OneToMany)
- User can be assigned many Tasks (OneToMany from User perspective)
- Project has many Users as members (ManyToMany via join table `project_members`)

## API Endpoints

### Users
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/users` | Create a user |
| GET | `/api/users` | List users (paginated) |
| GET | `/api/users/{id}` | Get user by ID |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

### Projects
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/projects` | Create a project |
| GET | `/api/projects` | List projects (paginated, filter by status) |
| GET | `/api/projects/{id}` | Get project by ID |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |
| POST | `/api/projects/{id}/members` | Add member to project |
| DELETE | `/api/projects/{id}/members/{userId}` | Remove member |

### Tasks
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/projects/{projectId}/tasks` | Create task in project |
| GET | `/api/projects/{projectId}/tasks` | List tasks in project (paginated) |
| GET | `/api/tasks` | Search tasks (filter by status, priority, assigneeId; paginated) |
| GET | `/api/tasks/{id}` | Get task by ID |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |
| PATCH | `/api/tasks/{id}/status` | Update task status (triggers SQS event) |
| PATCH | `/api/tasks/{id}/assign` | Assign task to user (triggers SQS event) |

## SQS Integration — Task Event Notifications

### Event Flow
1. When a task's status changes or a task is assigned, the service layer publishes a `TaskEvent` message to an SQS queue
2. A `@SqsListener` consumer in the same application reads events and processes them (logs the notification; simulates email/Slack dispatch)
3. Failed messages retry up to 3 times, then move to the Dead Letter Queue (DLQ)

### TaskEvent Message Schema
```json
{
  "eventType": "TASK_STATUS_CHANGED | TASK_ASSIGNED",
  "taskId": "uuid",
  "projectId": "uuid",
  "assigneeId": "uuid | null",
  "oldStatus": "TODO | null",
  "newStatus": "IN_PROGRESS",
  "timestamp": "2026-06-04T12:00:00Z"
}
```

### DLQ
- Separate SQS queue with redrive policy on the main queue
- `maxReceiveCount: 3` — after 3 failed processing attempts, message moves to DLQ
- CloudWatch alarm on DLQ message count (in Terraform)

## Architecture Layers

```
Controller (@RestController)
    ↓ validates input via @Valid
DTO layer (Java records for request/response)
    ↓ mapped in controller or service
Service (@Service, @Transactional)
    ↓ business logic, publishes SQS events
Repository (Spring Data JPA interfaces)
    ↓
Entity (JPA @Entity classes)
    ↓
PostgreSQL
```

### Cross-Cutting
- **Global exception handler** — `@ControllerAdvice` returning consistent error JSON (`status`, `message`, `timestamp`, `errors[]`)
- **Validation** — Bean Validation annotations on DTOs
- **Auditing** — `@CreatedDate`, `@LastModifiedDate` via Spring Data JPA auditing
- **Pagination** — Spring `Pageable` with default page size 20
- **Health checks** — Spring Actuator `/actuator/health` (used by ALB health checks)
- **Logging** — SLF4J + Logback, structured JSON in prod profile

## Spring Profiles

| Profile | Database | SQS | Use Case |
|---------|----------|-----|----------|
| `local` | H2 in-memory | Disabled (no-op publisher) | Local dev, no Docker needed |
| `dev` | PostgreSQL via Docker Compose | LocalStack SQS | Integration testing locally |
| `prod` | RDS PostgreSQL | AWS SQS | Production on Fargate |

## Project Structure

```
dev-interview/
├── src/
│   ├── main/
│   │   ├── java/com/example/taskmanager/
│   │   │   ├── TaskManagerApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── JpaAuditingConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── UserController.java
│   │   │   │   ├── ProjectController.java
│   │   │   │   └── TaskController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── CreateUserRequest.java
│   │   │   │   │   ├── CreateProjectRequest.java
│   │   │   │   │   ├── CreateTaskRequest.java
│   │   │   │   │   ├── UpdateTaskStatusRequest.java
│   │   │   │   │   └── AssignTaskRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── UserResponse.java
│   │   │   │       ├── ProjectResponse.java
│   │   │   │       ├── TaskResponse.java
│   │   │   │       └── ErrorResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Project.java
│   │   │   │   ├── Task.java
│   │   │   │   ├── TaskStatus.java
│   │   │   │   ├── TaskPriority.java
│   │   │   │   ├── ProjectStatus.java
│   │   │   │   └── UserRole.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   └── TaskRepository.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   ├── ProjectService.java
│   │   │   │   ├── TaskService.java
│   │   │   │   └── TaskEventPublisher.java
│   │   │   └── messaging/
│   │   │       ├── TaskEvent.java
│   │   │       ├── SqsTaskEventPublisher.java
│   │   │       ├── NoOpTaskEventPublisher.java
│   │   │       └── TaskEventConsumer.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_projects_table.sql
│   │           ├── V3__create_tasks_table.sql
│   │           └── V4__create_project_members_table.sql
│   └── test/java/com/example/taskmanager/
│       ├── controller/
│       │   └── TaskControllerIntegrationTest.java
│       ├── service/
│       │   └── TaskServiceTest.java
│       └── repository/
│           └── TaskRepositoryTest.java
├── infra/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── vpc.tf
│   ├── ecs.tf
│   ├── rds.tf
│   ├── sqs.tf
│   ├── ecr.tf
│   ├── alb.tf
│   ├── iam.tf
│   └── cloudwatch.tf
├── .github/
│   └── workflows/
│       └── ci.yml
├── docker-compose.yml       # Local dev: Postgres + LocalStack
├── Dockerfile
├── pom.xml
└── README.md
```

## Testing Strategy

### Unit Tests (Service layer)
- Mock repositories and SQS publisher
- Test business logic: task state transitions, assignment validation, project membership checks
- Test edge cases: assign to non-member, delete project with active tasks

### Integration Tests (Repository + Controller layers)
- Testcontainers with PostgreSQL
- `@SpringBootTest` with `WebTestClient` / `MockMvc`
- Test full request/response cycle including validation errors
- Test pagination and filtering

### CI Pipeline (GitHub Actions)
```yaml
on: [push, pull_request]
jobs:
  build:
    - Checkout
    - Set up Java 21
    - Maven build + test
    - Build Docker image
  deploy (main branch only):
    - Push to ECR
    - Update Fargate service
```

## Infrastructure (Terraform)

### Resources
- **VPC** — 2 public subnets, 2 private subnets, NAT gateway
- **ECS Fargate** — Cluster, service, task definition (512 CPU, 1024 MB memory)
- **RDS PostgreSQL** — db.t3.micro, private subnet, 20GB storage
- **SQS** — Main queue + DLQ with redrive policy (maxReceiveCount: 3)
- **ECR** — Container registry for Docker images
- **ALB** — Application Load Balancer in public subnets, routes to Fargate
- **IAM** — Task execution role (ECR pull, CloudWatch logs), task role (SQS access)
- **CloudWatch** — Log group for Fargate, DLQ alarm
- **Security Groups** — ALB (80/443 inbound), Fargate (8080 from ALB only), RDS (5432 from Fargate only)

### Terraform State
- Local state for simplicity (interview project, not team collaboration)

## Verification Plan

1. **Local:** `mvn spring-boot:run -Dspring-boot.run.profiles=local` — starts with H2, Swagger UI at `/swagger-ui.html`
2. **Docker Compose:** `docker-compose up` + run with `dev` profile — tests Postgres + LocalStack SQS locally
3. **Tests:** `mvn test` — runs all unit and integration tests (Testcontainers spins up Postgres)
4. **CI:** Push to GitHub — Actions pipeline builds, tests, and (on main) deploys
5. **AWS:** Hit the ALB URL — API responds, check CloudWatch for logs, send task status change and verify SQS message flow
