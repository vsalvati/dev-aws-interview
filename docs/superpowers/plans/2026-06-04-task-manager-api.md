# Task Manager REST API — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot 3.5.14 / Java 21 Task Management REST API with PostgreSQL, SQS notifications, Terraform infrastructure, and CI/CD — deployed to AWS Fargate.

**Architecture:** Layered REST API (Controller → Service → Repository) with JPA entities, Flyway migrations, and async SQS event publishing for task state changes. DTOs are Java records. Global exception handling via @ControllerAdvice. Three Spring profiles: local (H2), dev (Docker Compose), prod (AWS).

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, SpringDoc OpenAPI, Spring Cloud AWS SQS, JUnit 5, Mockito, Testcontainers, Docker, Terraform, GitHub Actions.

---

## File Structure

### Source Files
| File | Responsibility |
|------|---------------|
| `pom.xml` | Maven build, all dependencies |
| `src/main/java/.../TaskManagerApplication.java` | Spring Boot entry point |
| `src/main/java/.../config/OpenApiConfig.java` | Swagger/OpenAPI configuration |
| `src/main/java/.../config/JpaAuditingConfig.java` | Enable @CreatedDate/@LastModifiedDate |
| `src/main/java/.../entity/User.java` | User JPA entity |
| `src/main/java/.../entity/Project.java` | Project JPA entity |
| `src/main/java/.../entity/Task.java` | Task JPA entity |
| `src/main/java/.../entity/UserRole.java` | ADMIN/MEMBER enum |
| `src/main/java/.../entity/ProjectStatus.java` | ACTIVE/ARCHIVED enum |
| `src/main/java/.../entity/TaskStatus.java` | TODO/IN_PROGRESS/DONE enum |
| `src/main/java/.../entity/TaskPriority.java` | LOW/MEDIUM/HIGH enum |
| `src/main/java/.../repository/UserRepository.java` | User Spring Data JPA repo |
| `src/main/java/.../repository/ProjectRepository.java` | Project Spring Data JPA repo |
| `src/main/java/.../repository/TaskRepository.java` | Task Spring Data JPA repo with custom queries |
| `src/main/java/.../dto/request/CreateUserRequest.java` | User creation DTO |
| `src/main/java/.../dto/request/UpdateUserRequest.java` | User update DTO |
| `src/main/java/.../dto/request/CreateProjectRequest.java` | Project creation DTO |
| `src/main/java/.../dto/request/UpdateProjectRequest.java` | Project update DTO |
| `src/main/java/.../dto/request/CreateTaskRequest.java` | Task creation DTO |
| `src/main/java/.../dto/request/UpdateTaskRequest.java` | Task update DTO |
| `src/main/java/.../dto/request/UpdateTaskStatusRequest.java` | Task status change DTO |
| `src/main/java/.../dto/request/AssignTaskRequest.java` | Task assignment DTO |
| `src/main/java/.../dto/response/UserResponse.java` | User response DTO |
| `src/main/java/.../dto/response/ProjectResponse.java` | Project response DTO |
| `src/main/java/.../dto/response/TaskResponse.java` | Task response DTO |
| `src/main/java/.../dto/response/ErrorResponse.java` | Standard error response DTO |
| `src/main/java/.../exception/ResourceNotFoundException.java` | 404 exception |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | @ControllerAdvice for all exceptions |
| `src/main/java/.../service/UserService.java` | User business logic |
| `src/main/java/.../service/ProjectService.java` | Project business logic |
| `src/main/java/.../service/TaskService.java` | Task business logic + event publishing |
| `src/main/java/.../messaging/TaskEvent.java` | SQS message record |
| `src/main/java/.../messaging/TaskEventPublisher.java` | Interface for publishing events |
| `src/main/java/.../messaging/SqsTaskEventPublisher.java` | SQS implementation |
| `src/main/java/.../messaging/NoOpTaskEventPublisher.java` | No-op for local profile |
| `src/main/java/.../messaging/TaskEventConsumer.java` | @SqsListener consumer |
| `src/main/java/.../controller/UserController.java` | User REST endpoints |
| `src/main/java/.../controller/ProjectController.java` | Project REST endpoints |
| `src/main/java/.../controller/TaskController.java` | Task REST endpoints |

### Resource Files
| File | Responsibility |
|------|---------------|
| `src/main/resources/application.yml` | Base config |
| `src/main/resources/application-local.yml` | H2 + no-op SQS config |
| `src/main/resources/application-dev.yml` | Docker Compose Postgres + LocalStack |
| `src/main/resources/application-prod.yml` | RDS + AWS SQS config |
| `src/main/resources/db/migration/V1__create_users_table.sql` | Users DDL |
| `src/main/resources/db/migration/V2__create_projects_and_members_tables.sql` | Projects + join table DDL |
| `src/main/resources/db/migration/V3__create_tasks_table.sql` | Tasks DDL |

### Test Files
| File | Responsibility |
|------|---------------|
| `src/test/java/.../service/UserServiceTest.java` | Unit tests for UserService |
| `src/test/java/.../service/ProjectServiceTest.java` | Unit tests for ProjectService |
| `src/test/java/.../service/TaskServiceTest.java` | Unit tests for TaskService |
| `src/test/java/.../controller/UserControllerIntegrationTest.java` | User endpoint integration tests |
| `src/test/java/.../controller/ProjectControllerIntegrationTest.java` | Project endpoint integration tests |
| `src/test/java/.../controller/TaskControllerIntegrationTest.java` | Task endpoint integration tests |
| `src/test/java/.../repository/TaskRepositoryTest.java` | Custom query tests |
| `src/test/resources/application-test.yml` | Test profile config |

### Infrastructure Files
| File | Responsibility |
|------|---------------|
| `Dockerfile` | Multi-stage Docker build |
| `docker-compose.yml` | Local dev (Postgres + LocalStack) |
| `infra/main.tf` | Terraform provider + backend config |
| `infra/variables.tf` | Input variables |
| `infra/outputs.tf` | Output values (ALB URL, etc.) |
| `infra/vpc.tf` | VPC, subnets, NAT gateway |
| `infra/ecr.tf` | Container registry |
| `infra/ecs.tf` | Fargate cluster, service, task definition |
| `infra/rds.tf` | PostgreSQL RDS instance |
| `infra/sqs.tf` | SQS queues + DLQ |
| `infra/alb.tf` | Application Load Balancer |
| `infra/iam.tf` | IAM roles and policies |
| `infra/cloudwatch.tf` | Log groups + DLQ alarm |
| `.github/workflows/ci.yml` | Build, test, deploy pipeline |

All Java source files use base package: `com.example.taskmanager`
Abbreviated as `...` in paths above = `com/example/taskmanager`

---

## Task 1: Project Scaffolding

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/taskmanager/TaskManagerApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `.gitignore`

- [ ] **Step 1: Initialize git repo**

```bash
cd /Users/vinnysalvati/dev-interview
git init
```

- [ ] **Step 2: Create .gitignore**

Create `.gitignore`:

```
target/
*.class
*.jar
*.war
.idea/
*.iml
.DS_Store
.env
*.log
.terraform/
*.tfstate
*.tfstate.backup
.terraform.lock.hcl
```

- [ ] **Step 3: Create pom.xml**

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>task-manager</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>task-manager</name>
    <description>Task Management REST API — Interview Showcase</description>

    <properties>
        <java.version>21</java.version>
        <spring-cloud-aws.version>3.3.0</spring-cloud-aws.version>
        <springdoc.version>2.8.6</springdoc.version>
        <testcontainers.version>1.20.6</testcontainers.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- JPA + Postgres -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- H2 for local profile -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- SQS -->
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        </dependency>

        <!-- OpenAPI / Swagger -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>${spring-cloud-aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create main application class**

Create `src/main/java/com/example/taskmanager/TaskManagerApplication.java`:

```java
package com.example.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
```

- [ ] **Step 5: Create application.yml (base config)**

Create `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: task-manager
  jpa:
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 6: Create application-local.yml**

Create `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:taskmanager
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration
  h2:
    console:
      enabled: true
  cloud:
    aws:
      sqs:
        enabled: false

# SQS disabled in local profile — NoOpTaskEventPublisher is used
```

- [ ] **Step 7: Create directory structure**

```bash
mkdir -p src/main/java/com/example/taskmanager/{config,controller,dto/request,dto/response,entity,exception,repository,service,messaging}
mkdir -p src/main/resources/db/migration
mkdir -p src/test/java/com/example/taskmanager/{controller,service,repository}
mkdir -p src/test/resources
```

- [ ] **Step 8: Verify the project compiles**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS (with warnings about missing Flyway migrations, which is fine for now).

Note: If `mvnw` doesn't exist, generate it:
```bash
mvn wrapper:wrapper -Dmaven=3.9.9
```

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "chore: scaffold Spring Boot 3.5.14 project with Maven dependencies"
```

---

## Task 2: JPA Entities + Flyway Migrations

**Files:**
- Create: `src/main/java/com/example/taskmanager/entity/UserRole.java`
- Create: `src/main/java/com/example/taskmanager/entity/ProjectStatus.java`
- Create: `src/main/java/com/example/taskmanager/entity/TaskStatus.java`
- Create: `src/main/java/com/example/taskmanager/entity/TaskPriority.java`
- Create: `src/main/java/com/example/taskmanager/entity/User.java`
- Create: `src/main/java/com/example/taskmanager/entity/Project.java`
- Create: `src/main/java/com/example/taskmanager/entity/Task.java`
- Create: `src/main/java/com/example/taskmanager/config/JpaAuditingConfig.java`
- Create: `src/main/resources/db/migration/V1__create_users_table.sql`
- Create: `src/main/resources/db/migration/V2__create_projects_and_members_tables.sql`
- Create: `src/main/resources/db/migration/V3__create_tasks_table.sql`

- [ ] **Step 1: Create enums**

Create `src/main/java/com/example/taskmanager/entity/UserRole.java`:

```java
package com.example.taskmanager.entity;

public enum UserRole {
    ADMIN, MEMBER
}
```

Create `src/main/java/com/example/taskmanager/entity/ProjectStatus.java`:

```java
package com.example.taskmanager.entity;

public enum ProjectStatus {
    ACTIVE, ARCHIVED
}
```

Create `src/main/java/com/example/taskmanager/entity/TaskStatus.java`:

```java
package com.example.taskmanager.entity;

public enum TaskStatus {
    TODO, IN_PROGRESS, DONE
}
```

Create `src/main/java/com/example/taskmanager/entity/TaskPriority.java`:

```java
package com.example.taskmanager.entity;

public enum TaskPriority {
    LOW, MEDIUM, HIGH
}
```

- [ ] **Step 2: Create JPA Auditing config**

Create `src/main/java/com/example/taskmanager/config/JpaAuditingConfig.java`:

```java
package com.example.taskmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 3: Create User entity**

Create `src/main/java/com/example/taskmanager/entity/User.java`:

```java
package com.example.taskmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {}

    public User(String username, String email, UserRole role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Create Project entity**

Create `src/main/java/com/example/taskmanager/entity/Project.java`:

```java
package com.example.taskmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProjectStatus status;

    @ManyToMany
    @JoinTable(
        name = "project_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected Project() {}

    public Project(String name, String description, ProjectStatus status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public Set<User> getMembers() { return members; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void addMember(User user) { members.add(user); }
    public void removeMember(User user) { members.remove(user); }
}
```

- [ ] **Step 5: Create Task entity**

Create `src/main/java/com/example/taskmanager/entity/Task.java`:

```java
package com.example.taskmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority;

    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected Task() {}

    public Task(String title, String description, TaskStatus status, TaskPriority priority,
                LocalDate dueDate, Project project) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.project = project;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Project getProject() { return project; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 6: Create Flyway migrations**

Create `src/main/resources/db/migration/V1__create_users_table.sql`:

```sql
CREATE TABLE users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    role       VARCHAR(10)  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
```

Create `src/main/resources/db/migration/V2__create_projects_and_members_tables.sql`:

```sql
CREATE TABLE projects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE project_members (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, user_id)
);
```

Create `src/main/resources/db/migration/V3__create_tasks_table.sql`:

```sql
CREATE TABLE tasks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(15)   NOT NULL,
    priority    VARCHAR(10)   NOT NULL,
    due_date    DATE,
    project_id  UUID          NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assignee_id UUID          REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_tasks_status ON tasks(status);
```

- [ ] **Step 7: Verify compilation**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat: add JPA entities, enums, auditing config, and Flyway migrations"
```

---

## Task 3: DTOs + Exception Handling

**Files:**
- Create: all files in `src/main/java/com/example/taskmanager/dto/request/`
- Create: all files in `src/main/java/com/example/taskmanager/dto/response/`
- Create: `src/main/java/com/example/taskmanager/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/example/taskmanager/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create request DTOs**

Create `src/main/java/com/example/taskmanager/dto/request/CreateUserRequest.java`:

```java
package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Email @Size(max = 100) String email,
    @NotNull UserRole role
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/UpdateUserRequest.java`:

```java
package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Email @Size(max = 100) String email,
    @NotNull UserRole role
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/CreateProjectRequest.java`:

```java
package com.example.taskmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/UpdateProjectRequest.java`:

```java
package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull ProjectStatus status
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/CreateTaskRequest.java`:

```java
package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull TaskPriority priority,
    LocalDate dueDate
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/UpdateTaskRequest.java`:

```java
package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull TaskStatus status,
    @NotNull TaskPriority priority,
    LocalDate dueDate
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/UpdateTaskStatusRequest.java`:

```java
package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
    @NotNull TaskStatus status
) {}
```

Create `src/main/java/com/example/taskmanager/dto/request/AssignTaskRequest.java`:

```java
package com.example.taskmanager.dto.request;

import java.util.UUID;

public record AssignTaskRequest(
    UUID assigneeId
) {}
```

- [ ] **Step 2: Create response DTOs**

Create `src/main/java/com/example/taskmanager/dto/response/UserResponse.java`:

```java
package com.example.taskmanager.dto.response;

import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    UserRole role,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
```

Create `src/main/java/com/example/taskmanager/dto/response/ProjectResponse.java`:

```java
package com.example.taskmanager.dto.response;

import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String description,
    ProjectStatus status,
    int memberCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getStatus(),
            project.getMembers().size(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
```

Create `src/main/java/com/example/taskmanager/dto/response/TaskResponse.java`:

```java
package com.example.taskmanager.dto.response;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    UUID projectId,
    UUID assigneeId,
    String assigneeUsername,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            task.getProject().getId(),
            task.getAssignee() != null ? task.getAssignee().getId() : null,
            task.getAssignee() != null ? task.getAssignee().getUsername() : null,
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
```

Create `src/main/java/com/example/taskmanager/dto/response/ErrorResponse.java`:

```java
package com.example.taskmanager.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    int status,
    String message,
    Instant timestamp,
    List<String> errors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now(), List.of());
    }

    public ErrorResponse(int status, String message, List<String> errors) {
        this(status, message, Instant.now(), errors);
    }
}
```

- [ ] **Step 3: Create exceptions and global handler**

Create `src/main/java/com/example/taskmanager/exception/ResourceNotFoundException.java`:

```java
package com.example.taskmanager.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " not found with id: " + id);
    }
}
```

Create `src/main/java/com/example/taskmanager/exception/GlobalExceptionHandler.java`:

```java
package com.example.taskmanager.exception;

import com.example.taskmanager.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Validation failed", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, ex.getMessage()));
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add DTOs (records), exception classes, and global exception handler"
```

---

## Task 4: Repositories

**Files:**
- Create: `src/main/java/com/example/taskmanager/repository/UserRepository.java`
- Create: `src/main/java/com/example/taskmanager/repository/ProjectRepository.java`
- Create: `src/main/java/com/example/taskmanager/repository/TaskRepository.java`

- [ ] **Step 1: Create UserRepository**

Create `src/main/java/com/example/taskmanager/repository/UserRepository.java`:

```java
package com.example.taskmanager.repository;

import com.example.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 2: Create ProjectRepository**

Create `src/main/java/com/example/taskmanager/repository/ProjectRepository.java`:

```java
package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);
}
```

- [ ] **Step 3: Create TaskRepository**

Create `src/main/java/com/example/taskmanager/repository/TaskRepository.java`:

```java
package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        """)
    Page<Task> searchTasks(
        @Param("status") TaskStatus status,
        @Param("priority") TaskPriority priority,
        @Param("assigneeId") UUID assigneeId,
        Pageable pageable
    );
}
```

- [ ] **Step 4: Verify compilation**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add Spring Data JPA repositories with custom task search query"
```

---

## Task 5: SQS Messaging Layer

**Files:**
- Create: `src/main/java/com/example/taskmanager/messaging/TaskEvent.java`
- Create: `src/main/java/com/example/taskmanager/messaging/TaskEventPublisher.java`
- Create: `src/main/java/com/example/taskmanager/messaging/SqsTaskEventPublisher.java`
- Create: `src/main/java/com/example/taskmanager/messaging/NoOpTaskEventPublisher.java`
- Create: `src/main/java/com/example/taskmanager/messaging/TaskEventConsumer.java`

- [ ] **Step 1: Create TaskEvent record**

Create `src/main/java/com/example/taskmanager/messaging/TaskEvent.java`:

```java
package com.example.taskmanager.messaging;

import java.time.Instant;
import java.util.UUID;

public record TaskEvent(
    String eventType,
    UUID taskId,
    UUID projectId,
    UUID assigneeId,
    String oldStatus,
    String newStatus,
    Instant timestamp
) {
    public static TaskEvent statusChanged(UUID taskId, UUID projectId, String oldStatus, String newStatus) {
        return new TaskEvent("TASK_STATUS_CHANGED", taskId, projectId, null, oldStatus, newStatus, Instant.now());
    }

    public static TaskEvent assigned(UUID taskId, UUID projectId, UUID assigneeId) {
        return new TaskEvent("TASK_ASSIGNED", taskId, projectId, assigneeId, null, null, Instant.now());
    }
}
```

- [ ] **Step 2: Create TaskEventPublisher interface**

Create `src/main/java/com/example/taskmanager/messaging/TaskEventPublisher.java`:

```java
package com.example.taskmanager.messaging;

public interface TaskEventPublisher {

    void publish(TaskEvent event);
}
```

- [ ] **Step 3: Create NoOpTaskEventPublisher (local profile)**

Create `src/main/java/com/example/taskmanager/messaging/NoOpTaskEventPublisher.java`:

```java
package com.example.taskmanager.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class NoOpTaskEventPublisher implements TaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpTaskEventPublisher.class);

    @Override
    public void publish(TaskEvent event) {
        log.info("SQS disabled (local profile). Event: {}", event);
    }
}
```

- [ ] **Step 4: Create SqsTaskEventPublisher**

Create `src/main/java/com/example/taskmanager/messaging/SqsTaskEventPublisher.java`:

```java
package com.example.taskmanager.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class SqsTaskEventPublisher implements TaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsTaskEventPublisher.class);

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public SqsTaskEventPublisher(SqsTemplate sqsTemplate, ObjectMapper objectMapper,
                                  @Value("${app.sqs.task-events-queue}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    @Override
    public void publish(TaskEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            sqsTemplate.send(queueName, message);
            log.info("Published task event to SQS: {}", event.eventType());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task event", e);
            throw new RuntimeException("Failed to serialize task event", e);
        }
    }
}
```

- [ ] **Step 5: Create TaskEventConsumer**

Create `src/main/java/com/example/taskmanager/messaging/TaskEventConsumer.java`:

```java
package com.example.taskmanager.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);
    private final ObjectMapper objectMapper;

    public TaskEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SqsListener("${app.sqs.task-events-queue}")
    public void handleTaskEvent(String message) throws JsonProcessingException {
        TaskEvent event = objectMapper.readValue(message, TaskEvent.class);
        log.info("Received task event: type={}, taskId={}, projectId={}",
            event.eventType(), event.taskId(), event.projectId());

        switch (event.eventType()) {
            case "TASK_STATUS_CHANGED" -> log.info("Task {} status changed: {} -> {}",
                event.taskId(), event.oldStatus(), event.newStatus());
            case "TASK_ASSIGNED" -> log.info("Task {} assigned to user {}",
                event.taskId(), event.assigneeId());
            default -> log.warn("Unknown event type: {}", event.eventType());
        }
    }
}
```

- [ ] **Step 6: Add SQS config to application profiles**

Append to `src/main/resources/application.yml` (add at end):

```yaml
app:
  sqs:
    task-events-queue: task-events
```

Create `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskmanager
    username: taskmanager
    password: taskmanager
  jpa:
    hibernate:
      ddl-auto: validate

cloud:
  aws:
    region:
      static: us-east-1
    sqs:
      endpoint: http://localhost:4566
    credentials:
      access-key: test
      secret-key: test
```

Create `src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${RDS_HOSTNAME}:${RDS_PORT}/${RDS_DB_NAME}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate

cloud:
  aws:
    region:
      static: ${AWS_REGION:us-east-1}

logging:
  pattern:
    console: '{"timestamp":"%d","level":"%p","logger":"%logger","message":"%m"}%n'
```

- [ ] **Step 7: Verify compilation**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat: add SQS messaging layer with publisher interface, SQS impl, no-op, and consumer"
```

---

## Task 6: Service Layer (TDD)

**Files:**
- Create: `src/main/java/com/example/taskmanager/service/UserService.java`
- Create: `src/main/java/com/example/taskmanager/service/ProjectService.java`
- Create: `src/main/java/com/example/taskmanager/service/TaskService.java`
- Create: `src/test/java/com/example/taskmanager/service/UserServiceTest.java`
- Create: `src/test/java/com/example/taskmanager/service/ProjectServiceTest.java`
- Create: `src/test/java/com/example/taskmanager/service/TaskServiceTest.java`

### 6a: UserService

- [ ] **Step 1: Write UserService tests**

Create `src/test/java/com/example/taskmanager/service/UserServiceTest.java`:

```java
package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.request.UpdateUserRequest;
import com.example.taskmanager.dto.response.UserResponse;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldSaveAndReturnResponse() {
        var request = new CreateUserRequest("john", "john@example.com", UserRole.MEMBER);
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(request);

        assertThat(response.username()).isEqualTo("john");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo(UserRole.MEMBER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUser_whenExists_shouldReturnResponse() {
        var id = UUID.randomUUID();
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUser(id);

        assertThat(response.username()).isEqualTo("john");
    }

    @Test
    void getUser_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void listUsers_shouldReturnPage() {
        var pageable = PageRequest.of(0, 20);
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> page = userService.listUsers(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().username()).isEqualTo("john");
    }

    @Test
    void updateUser_whenExists_shouldUpdateAndReturn() {
        var id = UUID.randomUUID();
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var request = new UpdateUserRequest("jane", "jane@example.com", UserRole.ADMIN);
        UserResponse response = userService.updateUser(id, request);

        assertThat(response.username()).isEqualTo("jane");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void deleteUser_whenExists_shouldDelete() {
        var id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./mvnw test -pl . -Dtest=UserServiceTest -Dspring.profiles.active=local
```

Expected: Compilation error — `UserService` does not exist.

- [ ] **Step 3: Implement UserService**

Create `src/main/java/com/example/taskmanager/service/UserService.java`:

```java
package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.request.UpdateUserRequest;
import com.example.taskmanager.dto.response.UserResponse;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {
        var user = new User(request.username(), request.email(), request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return UserResponse.from(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        var user = findUserOrThrow(id);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -pl . -Dtest=UserServiceTest
```

Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add UserService with unit tests"
```

### 6b: ProjectService

- [ ] **Step 6: Write ProjectService tests**

Create `src/test/java/com/example/taskmanager/service/ProjectServiceTest.java`:

```java
package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_shouldSaveWithActiveStatus() {
        var request = new CreateProjectRequest("My Project", "Description");
        var project = new Project("My Project", "Description", ProjectStatus.ACTIVE);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponse response = projectService.createProject(request);

        assertThat(response.name()).isEqualTo("My Project");
        assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void getProject_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listProjects_withStatusFilter_shouldDelegateToRepo() {
        var pageable = PageRequest.of(0, 20);
        var project = new Project("P1", "Desc", ProjectStatus.ACTIVE);
        when(projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable))
            .thenReturn(new PageImpl<>(List.of(project)));

        var page = projectService.listProjects(ProjectStatus.ACTIVE, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(projectRepository).findByStatus(ProjectStatus.ACTIVE, pageable);
    }

    @Test
    void listProjects_withoutFilter_shouldReturnAll() {
        var pageable = PageRequest.of(0, 20);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        projectService.listProjects(null, pageable);

        verify(projectRepository).findAll(pageable);
    }

    @Test
    void updateProject_shouldUpdateFields() {
        var id = UUID.randomUUID();
        var project = new Project("Old", "Old desc", ProjectStatus.ACTIVE);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var request = new UpdateProjectRequest("New", "New desc", ProjectStatus.ARCHIVED);
        ProjectResponse response = projectService.updateProject(id, request);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void addMember_shouldAddUserToProject() {
        var projectId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var project = new Project("P1", "Desc", ProjectStatus.ACTIVE);
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(projectRepository.save(project)).thenReturn(project);

        projectService.addMember(projectId, userId);

        assertThat(project.getMembers()).contains(user);
    }
}
```

- [ ] **Step 7: Run tests to verify they fail**

```bash
./mvnw test -pl . -Dtest=ProjectServiceTest
```

Expected: Compilation error — `ProjectService` does not exist.

- [ ] **Step 8: Implement ProjectService**

Create `src/main/java/com/example/taskmanager/service/ProjectService.java`:

```java
package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        var project = new Project(request.name(), request.description(), ProjectStatus.ACTIVE);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        return ProjectResponse.from(findProjectOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(ProjectStatus status, Pageable pageable) {
        Page<Project> page = (status != null)
            ? projectRepository.findByStatus(status, pageable)
            : projectRepository.findAll(pageable);
        return page.map(ProjectResponse::from);
    }

    public ProjectResponse updateProject(UUID id, UpdateProjectRequest request) {
        var project = findProjectOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public void deleteProject(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        projectRepository.deleteById(id);
    }

    public void addMember(UUID projectId, UUID userId) {
        var project = findProjectOrThrow(projectId);
        var user = userService.findUserOrThrow(userId);
        project.addMember(user);
        projectRepository.save(project);
    }

    public void removeMember(UUID projectId, UUID userId) {
        var project = findProjectOrThrow(projectId);
        var user = userService.findUserOrThrow(userId);
        project.removeMember(user);
        projectRepository.save(project);
    }

    Project findProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

```bash
./mvnw test -pl . -Dtest=ProjectServiceTest
```

Expected: All 6 tests PASS.

- [ ] **Step 10: Commit**

```bash
git add .
git commit -m "feat: add ProjectService with member management and unit tests"
```

### 6c: TaskService

- [ ] **Step 11: Write TaskService tests**

Create `src/test/java/com/example/taskmanager/service/TaskServiceTest.java`:

```java
package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.AssignTaskRequest;
import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.*;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.messaging.TaskEvent;
import com.example.taskmanager.messaging.TaskEventPublisher;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectService projectService;
    @Mock private UserService userService;
    @Mock private TaskEventPublisher eventPublisher;

    @InjectMocks
    private TaskService taskService;

    private Project sampleProject() {
        return new Project("Test Project", "Desc", ProjectStatus.ACTIVE);
    }

    private User sampleUser() {
        return new User("john", "john@example.com", UserRole.MEMBER);
    }

    @Test
    void createTask_shouldCreateWithTodoStatus() {
        var projectId = UUID.randomUUID();
        var project = sampleProject();
        when(projectService.findProjectOrThrow(projectId)).thenReturn(project);

        var request = new CreateTaskRequest("My Task", "Description", TaskPriority.HIGH, LocalDate.of(2026, 7, 1));
        var task = new Task("My Task", "Description", TaskStatus.TODO, TaskPriority.HIGH,
            LocalDate.of(2026, 7, 1), project);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.createTask(projectId, request);

        assertThat(response.title()).isEqualTo("My Task");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void updateTaskStatus_shouldPublishEvent() {
        var taskId = UUID.randomUUID();
        var project = sampleProject();
        var task = new Task("Task", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null, project);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        taskService.updateTaskStatus(taskId, request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        var captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_STATUS_CHANGED");
        assertThat(captor.getValue().oldStatus()).isEqualTo("TODO");
        assertThat(captor.getValue().newStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void assignTask_shouldSetAssigneeAndPublishEvent() {
        var taskId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var project = sampleProject();
        var user = sampleUser();
        var task = new Task("Task", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null, project);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(taskRepository.save(task)).thenReturn(task);

        taskService.assignTask(taskId, new AssignTaskRequest(userId));

        assertThat(task.getAssignee()).isEqualTo(user);

        var captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_ASSIGNED");
    }

    @Test
    void assignTask_withNullAssigneeId_shouldUnassign() {
        var taskId = UUID.randomUUID();
        var project = sampleProject();
        var task = new Task("Task", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null, project);
        task.setAssignee(sampleUser());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.assignTask(taskId, new AssignTaskRequest(null));

        assertThat(task.getAssignee()).isNull();
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void getTask_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 12: Run tests to verify they fail**

```bash
./mvnw test -pl . -Dtest=TaskServiceTest
```

Expected: Compilation error — `TaskService` does not exist.

- [ ] **Step 13: Implement TaskService**

Create `src/main/java/com/example/taskmanager/service/TaskService.java`:

```java
package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.*;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.messaging.TaskEvent;
import com.example.taskmanager.messaging.TaskEventPublisher;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final TaskEventPublisher eventPublisher;

    public TaskService(TaskRepository taskRepository, ProjectService projectService,
                       UserService userService, TaskEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        var project = projectService.findProjectOrThrow(projectId);
        var task = new Task(
            request.title(), request.description(),
            TaskStatus.TODO, request.priority(),
            request.dueDate(), project
        );
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        return TaskResponse.from(findTaskOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listTasksByProject(UUID projectId, Pageable pageable) {
        return taskRepository.findByProjectId(projectId, pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(TaskStatus status, TaskPriority priority,
                                           UUID assigneeId, Pageable pageable) {
        return taskRepository.searchTasks(status, priority, assigneeId, pageable)
            .map(TaskResponse::from);
    }

    public TaskResponse updateTask(UUID id, UpdateTaskRequest request) {
        var task = findTaskOrThrow(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse updateTaskStatus(UUID id, UpdateTaskStatusRequest request) {
        var task = findTaskOrThrow(id);
        String oldStatus = task.getStatus().name();
        task.setStatus(request.status());
        var saved = taskRepository.save(task);

        eventPublisher.publish(TaskEvent.statusChanged(
            task.getId(), task.getProject().getId(), oldStatus, request.status().name()
        ));

        return TaskResponse.from(saved);
    }

    public TaskResponse assignTask(UUID id, AssignTaskRequest request) {
        var task = findTaskOrThrow(id);

        if (request.assigneeId() != null) {
            var user = userService.findUserOrThrow(request.assigneeId());
            task.setAssignee(user);
            taskRepository.save(task);

            eventPublisher.publish(TaskEvent.assigned(
                task.getId(), task.getProject().getId(), user.getId()
            ));
        } else {
            task.setAssignee(null);
            taskRepository.save(task);
        }

        return TaskResponse.from(task);
    }

    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
    }

    private Task findTaskOrThrow(UUID id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
```

- [ ] **Step 14: Run tests to verify they pass**

```bash
./mvnw test -pl . -Dtest=TaskServiceTest
```

Expected: All 5 tests PASS.

- [ ] **Step 15: Run all service tests**

```bash
./mvnw test -pl . -Dtest="com.example.taskmanager.service.*"
```

Expected: All 17 tests PASS.

- [ ] **Step 16: Commit**

```bash
git add .
git commit -m "feat: add TaskService with SQS event publishing and unit tests"
```

---

## Task 7: REST Controllers

**Files:**
- Create: `src/main/java/com/example/taskmanager/controller/UserController.java`
- Create: `src/main/java/com/example/taskmanager/controller/ProjectController.java`
- Create: `src/main/java/com/example/taskmanager/controller/TaskController.java`
- Create: `src/main/java/com/example/taskmanager/config/OpenApiConfig.java`

- [ ] **Step 1: Create OpenAPI config**

Create `src/main/java/com/example/taskmanager/config/OpenApiConfig.java`:

```java
package com.example.taskmanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Task Manager API")
                .version("1.0")
                .description("REST API for managing projects and tasks"));
    }
}
```

- [ ] **Step 2: Create UserController**

Create `src/main/java/com/example/taskmanager/controller/UserController.java`:

```java
package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.request.UpdateUserRequest;
import com.example.taskmanager.dto.response.UserResponse;
import com.example.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public Page<UserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}
```

- [ ] **Step 3: Create ProjectController**

Create `src/main/java/com/example/taskmanager/controller/ProjectController.java`:

```java
package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.entity.ProjectStatus;
import com.example.taskmanager.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping
    public Page<ProjectResponse> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return projectService.listProjects(status, pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable UUID id) {
        return projectService.getProject(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable UUID id, @RequestParam UUID userId) {
        projectService.addMember(id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        projectService.removeMember(id, userId);
    }
}
```

- [ ] **Step 4: Create TaskController**

Create `src/main/java/com/example/taskmanager/controller/TaskController.java`:

```java
package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.*;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable UUID projectId,
                                    @Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(projectId, request);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public Page<TaskResponse> listProjectTasks(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.listTasksByProject(projectId, pageable);
    }

    @GetMapping("/tasks")
    public Page<TaskResponse> searchTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.searchTasks(status, priority, assigneeId, pageable);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse getTask(@PathVariable UUID id) {
        return taskService.getTask(id);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse updateTaskStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateTaskStatus(id, request);
    }

    @PatchMapping("/tasks/{id}/assign")
    public TaskResponse assignTask(@PathVariable UUID id,
                                    @Valid @RequestBody AssignTaskRequest request) {
        return taskService.assignTask(id, request);
    }
}
```

- [ ] **Step 5: Verify compilation**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add REST controllers for users, projects, and tasks with OpenAPI config"
```

---

## Task 8: Integration Tests

**Files:**
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/java/com/example/taskmanager/controller/UserControllerIntegrationTest.java`
- Create: `src/test/java/com/example/taskmanager/controller/TaskControllerIntegrationTest.java`
- Create: `src/test/java/com/example/taskmanager/repository/TaskRepositoryTest.java`

- [ ] **Step 1: Create test application config**

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  cloud:
    aws:
      sqs:
        enabled: false
  flyway:
    locations: classpath:db/migration
```

- [ ] **Step 2: Create base test config to use Testcontainers + NoOp SQS**

Create `src/test/java/com/example/taskmanager/BaseIntegrationTest.java`:

```java
package com.example.taskmanager;

import com.example.taskmanager.messaging.TaskEvent;
import com.example.taskmanager.messaging.TaskEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Import(BaseIntegrationTest.TestSqsConfig.class)
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("taskmanager_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class TestSqsConfig {
        @Bean
        @Primary
        public TaskEventPublisher testTaskEventPublisher() {
            return event -> {}; // no-op for tests
        }
    }
}
```

- [ ] **Step 3: Create UserController integration test**

Create `src/test/java/com/example/taskmanager/controller/UserControllerIntegrationTest.java`:

```java
package com.example.taskmanager.controller;

import com.example.taskmanager.BaseIntegrationTest;
import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_withValidRequest_shouldReturn201() throws Exception {
        var request = new CreateUserRequest("john", "john@example.com", UserRole.MEMBER);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("john"))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createUser_withBlankUsername_shouldReturn400() throws Exception {
        var request = new CreateUserRequest("", "john@example.com", UserRole.MEMBER);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getUser_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000001"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    void listUsers_shouldReturnPaginatedResults() throws Exception {
        var request = new CreateUserRequest("alice", "alice@example.com", UserRole.ADMIN);
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].username").value("alice"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
```

- [ ] **Step 4: Create TaskController integration test**

Create `src/test/java/com/example/taskmanager/controller/TaskControllerIntegrationTest.java`:

```java
package com.example.taskmanager.controller;

import com.example.taskmanager.BaseIntegrationTest;
import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TaskControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Create a project to use in tests
        var projectRequest = new CreateProjectRequest("Test Project", "For testing");
        MvcResult result = mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        projectId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createTask_shouldReturn201() throws Exception {
        var request = new CreateTaskRequest("Build feature", "Build the thing",
            TaskPriority.HIGH, LocalDate.of(2026, 7, 15));

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Build feature"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.projectId").value(projectId));
    }

    @Test
    void updateTaskStatus_shouldChangeStatusAndReturn200() throws Exception {
        // Create task
        var createRequest = new CreateTaskRequest("Task 1", "Desc", TaskPriority.MEDIUM, null);
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Update status
        var statusRequest = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void searchTasks_withFilters_shouldReturnFiltered() throws Exception {
        // Create two tasks with different priorities
        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateTaskRequest("High task", "Desc", TaskPriority.HIGH, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateTaskRequest("Low task", "Desc", TaskPriority.LOW, null))))
            .andExpect(status().isCreated());

        // Search by priority
        mockMvc.perform(get("/api/tasks?priority=HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("High task"));
    }

    @Test
    void listProjectTasks_shouldReturnOnlyProjectTasks() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateTaskRequest("My task", "Desc", TaskPriority.MEDIUM, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("My task"));
    }
}
```

- [ ] **Step 5: Create TaskRepository test**

Create `src/test/java/com/example/taskmanager/repository/TaskRepositoryTest.java`:

```java
package com.example.taskmanager.repository;

import com.example.taskmanager.BaseIntegrationTest;
import com.example.taskmanager.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRepositoryTest extends BaseIntegrationTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("john", "john@example.com", UserRole.MEMBER));
        project = projectRepository.save(new Project("Test", "Desc", ProjectStatus.ACTIVE));
    }

    @Test
    void searchTasks_byStatus_shouldReturnMatching() {
        var todo = new Task("Todo task", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, project);
        var done = new Task("Done task", "Desc", TaskStatus.DONE, TaskPriority.LOW, null, project);
        taskRepository.save(todo);
        taskRepository.save(done);

        var page = taskRepository.searchTasks(TaskStatus.TODO, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Todo task");
    }

    @Test
    void searchTasks_byAssignee_shouldReturnMatching() {
        var assigned = new Task("Assigned", "Desc", TaskStatus.TODO, TaskPriority.HIGH, null, project);
        assigned.setAssignee(user);
        var unassigned = new Task("Unassigned", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, project);
        taskRepository.save(assigned);
        taskRepository.save(unassigned);

        var page = taskRepository.searchTasks(null, null, user.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Assigned");
    }

    @Test
    void searchTasks_withNoFilters_shouldReturnAll() {
        taskRepository.save(new Task("T1", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, project));
        taskRepository.save(new Task("T2", "Desc", TaskStatus.DONE, TaskPriority.HIGH, null, project));

        var page = taskRepository.searchTasks(null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
    }
}
```

- [ ] **Step 6: Run all tests**

```bash
./mvnw test
```

Expected: All unit tests and integration tests PASS. (Integration tests require Docker running for Testcontainers.)

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: add integration tests with Testcontainers for controllers and repositories"
```

---

## Task 9: Docker + Docker Compose

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`

- [ ] **Step 1: Create Dockerfile**

Create `Dockerfile`:

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create docker-compose.yml**

Create `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: taskmanager
      POSTGRES_USER: taskmanager
      POSTGRES_PASSWORD: taskmanager
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  localstack:
    image: localstack/localstack:3
    environment:
      SERVICES: sqs
      DEFAULT_REGION: us-east-1
    ports:
      - "4566:4566"
    volumes:
      - localstack:/var/lib/localstack
      - ./infra/localstack-init.sh:/etc/localstack/init/ready.d/init.sh

volumes:
  pgdata:
  localstack:
```

- [ ] **Step 3: Create LocalStack init script**

```bash
mkdir -p infra
```

Create `infra/localstack-init.sh`:

```bash
#!/bin/bash
awslocal sqs create-queue --queue-name task-events-dlq
awslocal sqs create-queue --queue-name task-events \
  --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:task-events-dlq\",\"maxReceiveCount\":\"3\"}"
  }'
echo "SQS queues created."
```

```bash
chmod +x infra/localstack-init.sh
```

- [ ] **Step 4: Verify Docker build**

```bash
docker build -t task-manager .
```

Expected: Image builds successfully.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add multi-stage Dockerfile and Docker Compose for local dev"
```

---

## Task 10: Terraform Infrastructure

**Files:**
- Create: `infra/main.tf`
- Create: `infra/variables.tf`
- Create: `infra/outputs.tf`
- Create: `infra/vpc.tf`
- Create: `infra/ecr.tf`
- Create: `infra/ecs.tf`
- Create: `infra/rds.tf`
- Create: `infra/sqs.tf`
- Create: `infra/alb.tf`
- Create: `infra/iam.tf`
- Create: `infra/cloudwatch.tf`

- [ ] **Step 1: Create main.tf**

Create `infra/main.tf`:

```hcl
terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}
```

- [ ] **Step 2: Create variables.tf**

Create `infra/variables.tf`:

```hcl
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "task-manager"
}

variable "db_username" {
  description = "RDS master username"
  type        = string
  default     = "taskmanager"
  sensitive   = true
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "Docker image URI for the task manager app"
  type        = string
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}
```

- [ ] **Step 3: Create vpc.tf**

Create `infra/vpc.tf`:

```hcl
data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "${var.project_name}-vpc" }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "${var.project_name}-igw" }
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = { Name = "${var.project_name}-public-${count.index}" }
}

resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index + 10)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "${var.project_name}-private-${count.index}" }
}

resource "aws_eip" "nat" {
  domain = "vpc"
  tags   = { Name = "${var.project_name}-nat-eip" }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = { Name = "${var.project_name}-nat" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "${var.project_name}-public-rt" }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = { Name = "${var.project_name}-private-rt" }
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
```

- [ ] **Step 4: Create ecr.tf**

Create `infra/ecr.tf`:

```hcl
resource "aws_ecr_repository" "app" {
  name                 = var.project_name
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = var.project_name }
}
```

- [ ] **Step 5: Create sqs.tf**

Create `infra/sqs.tf`:

```hcl
resource "aws_sqs_queue" "task_events_dlq" {
  name                      = "${var.project_name}-task-events-dlq"
  message_retention_seconds = 1209600 # 14 days

  tags = { Name = "${var.project_name}-task-events-dlq" }
}

resource "aws_sqs_queue" "task_events" {
  name                       = "${var.project_name}-task-events"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600 # 4 days

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.task_events_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${var.project_name}-task-events" }
}
```

- [ ] **Step 6: Create rds.tf**

Create `infra/rds.tf`:

```hcl
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "${var.project_name}-db-subnet" }
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Allow inbound from Fargate"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  tags = { Name = "${var.project_name}-rds-sg" }
}

resource "aws_db_instance" "main" {
  identifier             = var.project_name
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  db_name                = "taskmanager"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  skip_final_snapshot    = true
  publicly_accessible    = false

  tags = { Name = var.project_name }
}
```

- [ ] **Step 7: Create alb.tf**

Create `infra/alb.tf`:

```hcl
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Allow HTTP inbound"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-alb-sg" }
}

resource "aws_lb" "main" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = { Name = "${var.project_name}-alb" }
}

resource "aws_lb_target_group" "app" {
  name        = "${var.project_name}-tg"
  port        = var.app_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
  }

  tags = { Name = "${var.project_name}-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
```

- [ ] **Step 8: Create iam.tf**

Create `infra/iam.tf`:

```hcl
# ECS Task Execution Role (pulling images, writing logs)
resource "aws_iam_role" "ecs_execution" {
  name = "${var.project_name}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ECS Task Role (app-level permissions: SQS)
resource "aws_iam_role" "ecs_task" {
  name = "${var.project_name}-ecs-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "ecs_task_sqs" {
  name = "${var.project_name}-sqs-access"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueUrl",
        "sqs:GetQueueAttributes"
      ]
      Resource = [
        aws_sqs_queue.task_events.arn,
        aws_sqs_queue.task_events_dlq.arn
      ]
    }]
  })
}
```

- [ ] **Step 9: Create ecs.tf**

Create `infra/ecs.tf`:

```hcl
resource "aws_security_group" "ecs" {
  name        = "${var.project_name}-ecs-sg"
  description = "Allow inbound from ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = var.app_port
    to_port         = var.app_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-ecs-sg" }
}

resource "aws_ecs_cluster" "main" {
  name = var.project_name
  tags = { Name = var.project_name }
}

resource "aws_ecs_task_definition" "app" {
  family                   = var.project_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = var.project_name
    image = var.container_image
    portMappings = [{
      containerPort = var.app_port
      protocol      = "tcp"
    }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "RDS_HOSTNAME", value = aws_db_instance.main.address },
      { name = "RDS_PORT", value = tostring(aws_db_instance.main.port) },
      { name = "RDS_DB_NAME", value = "taskmanager" },
      { name = "RDS_USERNAME", value = var.db_username },
      { name = "RDS_PASSWORD", value = var.db_password },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "APP_SQS_TASK_EVENTS_QUEUE", value = aws_sqs_queue.task_events.name },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

resource "aws_ecs_service" "app" {
  name            = var.project_name
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.ecs.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = var.project_name
    container_port   = var.app_port
  }

  depends_on = [aws_lb_listener.http]
}
```

- [ ] **Step 10: Create cloudwatch.tf**

Create `infra/cloudwatch.tf`:

```hcl
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 14

  tags = { Name = var.project_name }
}

resource "aws_cloudwatch_metric_alarm" "dlq_messages" {
  alarm_name          = "${var.project_name}-dlq-messages"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Messages in DLQ — task event processing failures"

  dimensions = {
    QueueName = aws_sqs_queue.task_events_dlq.name
  }

  tags = { Name = "${var.project_name}-dlq-alarm" }
}
```

- [ ] **Step 11: Create outputs.tf**

Create `infra/outputs.tf`:

```hcl
output "alb_dns_name" {
  description = "ALB DNS name — your API endpoint"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.app.repository_url
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "sqs_queue_url" {
  description = "SQS task events queue URL"
  value       = aws_sqs_queue.task_events.url
}
```

- [ ] **Step 12: Validate Terraform**

```bash
cd infra && terraform init && terraform validate
```

Expected: "Success! The configuration is valid."

- [ ] **Step 13: Commit**

```bash
cd /Users/vinnysalvati/dev-interview
git add .
git commit -m "feat: add Terraform infrastructure for VPC, ECS Fargate, RDS, SQS, ALB, and CloudWatch"
```

---

## Task 11: GitHub Actions CI/CD

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create CI/CD workflow**

```bash
mkdir -p .github/workflows
```

Create `.github/workflows/ci.yml`:

```yaml
name: CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        run: ./mvnw verify -B

      - name: Build Docker image
        run: docker build -t task-manager:${{ github.sha }} .

  deploy:
    needs: build-and-test
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest

    permissions:
      id-token: write
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Login to ECR
        id: ecr-login
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build JAR
        run: ./mvnw package -DskipTests -B

      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.ecr-login.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/task-manager:$IMAGE_TAG .
          docker push $ECR_REGISTRY/task-manager:$IMAGE_TAG

      - name: Update ECS service
        env:
          ECR_REGISTRY: ${{ steps.ecr-login.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          aws ecs update-service \
            --cluster task-manager \
            --service task-manager \
            --force-new-deployment
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add GitHub Actions CI/CD pipeline for build, test, and deploy"
```

---

## Task 12: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Create README**

Create `README.md`:

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "docs: add README with setup, API reference, and deployment instructions"
```

---

## Verification Plan

1. **Compile:** `./mvnw compile` — BUILD SUCCESS
2. **Unit tests:** `./mvnw test -Dtest="com.example.taskmanager.service.*"` — all pass
3. **Integration tests:** `./mvnw test` — all pass (requires Docker)
4. **Local run:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` — Swagger UI at http://localhost:8080/swagger-ui.html
5. **Docker build:** `docker build -t task-manager .` — image builds
6. **Docker Compose:** `docker-compose up -d` then run with `dev` profile — Postgres + SQS working
7. **Terraform validate:** `cd infra && terraform init && terraform validate` — valid
8. **Full deploy:** Apply Terraform, push image to ECR, verify ALB returns API responses
