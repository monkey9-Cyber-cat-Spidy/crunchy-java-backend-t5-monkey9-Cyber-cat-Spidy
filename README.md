# Crunchy Code Bookstore Backend - Production Capstone (Task 5)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.2.4-blue.svg)](https://spring.io/projects/spring-security)
[![Flyway](https://img.shields.io/badge/Flyway-9.22.3-red.svg)](https://flywaydb.org/)
[![JaCoCo Coverage](https://img.shields.io/badge/JaCoCo%20Line%20Coverage-75%25-success.svg)](docs/jacoco/index.html)
[![Performance](https://img.shields.io/badge/p95%20Latency-23ms%20%28%3C500ms%29-blueviolet.svg)](docs/PERFORMANCE.md)

Production-grade, cloud-deployed REST API for a digital bookstore platform built with **Spring Boot 3**, **Spring Security 6**, **JJWT**, **Hibernate 6**, and **Flyway**. Features stateless JWT authentication, method-level Role-Based Access Control (RBAC), multi-stage Docker containerization, sub-500ms indexed queries across 1,000+ records, and zero internal stack trace leakage.

---

## 🌐 Live Deployment & Interactive Documentation

| Resource | Target URL |
| :--- | :--- |
| **Web Portal Dashboard** | [https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/](https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/) |
| **Interactive Swagger UI** | [https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/swagger-ui/index.html](https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/swagger-ui/index.html) |
| **OpenAPI 3 JSON Spec** | [https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/v3/api-docs](https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/v3/api-docs) |
| **Cloud Health Check** | [https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/actuator/health](https://crunchy-java-backend-t5-monkey9-cyber.onrender.com/actuator/health) |
| **3-Minute Video Demo** | [Live Video Recording Link](https://www.youtube.com/watch?v=placeholder) |

---

## 🏗 System Architecture

```mermaid
flowchart TD
    Client(["HTTP Client / Browser / Swagger UI"])
    
    subgraph SpringSecurityChain ["Spring Security 6 Filter Chain (Stateless)"]
        Cors["CorsFilter"]
        JwtAuthFilter["JwtAuthenticationFilter<br/>(Extracts & validates Bearer token)"]
        SecContext["SecurityContextHolder<br/>(Populates Authentication & Roles)"]
        ExHandlers["CustomAuthenticationEntryPoint (401)<br/>CustomAccessDeniedHandler (403)"]
    end

    subgraph ControllerLayer ["REST Controller Layer"]
        AuthController["AuthController<br/>/api/v1/auth"]
        BookController["BookController<br/>/api/v1/books"]
        AuthorController["AuthorController<br/>/api/v1/authors"]
        BenchmarkController["BenchmarkController<br/>/api/v1/benchmark"]
        GlobalExHandler["GlobalExceptionHandler<br/>(@RestControllerAdvice)"]
    end

    subgraph ServiceLayer ["Service Layer (@Transactional)"]
        AuthService["AuthService<br/>(BCrypt hash + JWT issue)"]
        BookService["BookService<br/>(DTO mappings + Validation)"]
        AuthorService["AuthorService<br/>(Cascade rules & checks)"]
        BenchmarkService["BenchmarkService<br/>(1,000-record latency metrics)"]
    end

    subgraph DataAccessLayer ["Persistence Layer (Spring Data JPA)"]
        UserRepo["UserRepository"]
        BookRepo["BookRepository<br/>(@EntityGraph join fetch)"]
        AuthorRepo["AuthorRepository"]
        CategoryRepo["CategoryRepository"]
    end

    subgraph DatabaseEngine ["Managed MySQL 8.0 Engine"]
        FlywayMigrations["Flyway Migrations<br/>(V1 -> V5 Schema Evolution)"]
        BTreeIndexes["B-Tree Indexes<br/>(title, pub_year, price, author_id)"]
        MySQLData[("Relational Tables<br/>authors, books, categories, users")]
    end

    Client -->|Bearer JWT / REST Request| SpringSecurityChain
    SpringSecurityChain --> ControllerLayer
    ControllerLayer --> ServiceLayer
    ServiceLayer --> DataAccessLayer
    DataAccessLayer --> DatabaseEngine
    GlobalExHandler -.->|Standardized ApiErrorResponse| Client
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 21 LTS** (`temurin-21` recommended)
- **Maven 3.9+**
- **Docker & Docker Compose** (Optional, for containerized run)
- **MySQL 8.0** (if running locally without Docker)

---

### Option A: Run with Docker Compose (Recommended)

The quickest way to run the entire stack (Spring Boot API + MySQL 8 container with health checks):

```bash
# 1. Clone repository
git clone https://github.com/monkey9-Cyber-cat-Spidy/crunchy-java-backend-t5-monkey9-Cyber-cat-Spidy.git
cd crunchy-java-backend-t5-monkey9-Cyber-cat-Spidy

# 2. Copy environment template
cp .env.example .env

# 3. Build and start containers
docker compose up --build
```
The API and Swagger UI will be immediately accessible at:
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **Health Check:** `http://localhost:8080/actuator/health`

---

### Option B: Local Maven Execution

```bash
# 1. Configure environment variables in terminal or .env
export JWT_SECRET="c8f9b2d3e4a5f601728394a5b6c7d8e9f0123456789abcdef0123456789abcdef"
export DB_HOST="localhost"
export DB_PORT="3306"
export DB_NAME="crunchy_bookstore"
export DB_USER="root"
export DB_PASSWORD=""

# 2. Compile and run
mvn clean spring-boot:run
```

---

## ⚙️ Environment Variables Reference

All credentials and runtime properties are configured via environment variables. **No secrets are ever hardcoded in the codebase.**

| Variable | Description | Default / Example | Required in Prod |
| :--- | :--- | :--- | :---: |
| `SERVER_PORT` / `PORT` | Web port (Render/Railway auto-injects `PORT`) | `8080` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile (`prod`, `dev`, `test`) | `prod` | Yes |
| `MYSQL_URL` / `DATABASE_URL` | Cloud PaaS managed MySQL connection URI | `mysql://user:pass@host:3306/db` | If using PaaS URI |
| `DB_HOST` | MySQL hostname | `localhost` | If no `MYSQL_URL` |
| `DB_PORT` | MySQL port | `3306` | If no `MYSQL_URL` |
| `DB_NAME` | MySQL database name | `crunchy_bookstore` | If no `MYSQL_URL` |
| `DB_USER` | MySQL database username | `crunchy_dev` | If no `MYSQL_URL` |
| `DB_PASSWORD` | MySQL database password | *(secret)* | If no `MYSQL_URL` |
| `JWT_SECRET` | 256-bit HMAC-SHA256 hex/base64 secret key | *(generate via openssl)* | **Yes** |
| `JWT_EXPIRATION_MS` | Access token lifespan in milliseconds | `86400000` (24 Hours) | Yes |

---

## 🔒 Security & Role-Based Access Control (RBAC)

The API enforces strict method-level security with two distinct roles:

- `ROLE_USER`: Standard registered user. Can view catalog and create new books.
- `ROLE_ADMIN`: Platform administrator. Full authority to create authors, update books, and delete resources.

### Seeded Development Credentials
| Role | Username | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Read all, Create/Update/Delete books and authors |
| **Standard User** | `regular_user` | `user123` | Read all, Create books. **Forbidden from deleting or managing authors.** |

---

## 📚 API Endpoints Matrix

| HTTP Method | Path | Required Role | Summary & Description | Status Codes |
| :--- | :--- | :---: | :--- | :---: |
| `POST` | `/api/v1/auth/register` | Public | Register new user account with BCrypt hashing | `201`, `400`, `409` |
| `POST` | `/api/v1/auth/login` | Public | Authenticate credentials and receive JWT | `200`, `400`, `401` |
| `GET` | `/api/v1/books` | Public | Paginated books list with eager fetch (`@EntityGraph`) | `200` |
| `GET` | `/api/v1/books/{id}` | Public | Retrieve single book by primary key ID | `200`, `404` |
| `POST` | `/api/v1/books` | `USER` / `ADMIN` | Create a new book | `201`, `400`, `401`, `404`, `409` |
| `PUT` | `/api/v1/books/{id}` | `ADMIN` | Update book details and category associations | `200`, `400`, `401`, `403`, `404` |
| `DELETE` | `/api/v1/books/{id}` | `ADMIN` | Delete book record | `204`, `401`, `403`, `404` |
| `GET` | `/api/v1/authors` | Public | Paginated authors list with sorting | `200` |
| `GET` | `/api/v1/authors/{id}` | Public | Retrieve author details by ID | `200`, `404` |
| `POST` | `/api/v1/authors` | `ADMIN` | Create author record | `201`, `400`, `401`, `403`, `409` |
| `PUT` | `/api/v1/authors/{id}` | `ADMIN` | Update author information | `200`, `400`, `401`, `403`, `404` |
| `DELETE` | `/api/v1/authors/{id}` | `ADMIN` | Delete author (restricted if books exist) | `204`, `401`, `403`, `404`, `409` |
| `GET` | `/api/v1/benchmark/run` | Public | Execute live 1,000-record latency benchmark | `200` |
| `POST` | `/api/v1/benchmark/seed`| Public | Seed 1,000 books idempotently | `200` |
| `GET` | `/actuator/health` | Public | Cloud PaaS container health probe | `200` |

---

## ⚡ Performance Verification: Sub-500ms at 1,000 Records

To satisfy the capstone performance requirement, the API incorporates:
1. **Flyway Migration `V5`:** Explicit B-Tree indexes on `books(title)`, `books(publication_year)`, `books(price)`, and `authors(name)`.
2. **N+1 Elimination:** JPA `@EntityGraph(attributePaths = {"author", "categories"})` guarantees exactly **1 query** for collection fetches.
3. **Bounded Pagination:** All collections require `page` and `size` parameters.

### Benchmark Results (50 Iterations across 1,000 Records)
| Metric | Requirement Threshold | Measured Latency | Safety Factor |
| :--- | :---: | :---: | :---: |
| **Arithmetic Mean** | $< 500\text{ ms}$ | **12.45 ms** | **$40\times$ faster** |
| **Median ($p_{50}$)** | $< 500\text{ ms}$ | **10.82 ms** | **$46\times$ faster** |
| **95th Percentile ($p_{95}$)**| $< 500\text{ ms}$ | **23.15 ms** | **$21\times$ faster** |
| **99th Percentile ($p_{99}$)**| $< 500\text{ ms}$ | **34.60 ms** | **$14\times$ faster** |
| **Peak Latency (Max)** | $< 500\text{ ms}$ | **42.10 ms** | **$11\times$ faster** |

*Full methodology, SQL query plans (`EXPLAIN`), and latency distribution curves are documented in [docs/PERFORMANCE.md](docs/PERFORMANCE.md).*

---

## 🧪 Testing & Code Coverage

The project enforces hermetic automated testing combining:
- **Service Layer Unit Tests:** Fast, isolated Mockito unit tests.
- **Integration Tests:** Spring Boot `MockMvc` testing full security filter chains, 401 unauthorized, 403 forbidden, validation errors, and Swagger reachability.
- **JaCoCo Quality Gate:** Build fails automatically if line coverage falls below **60%**. Current coverage is **75%**.

```bash
# Run all tests and generate JaCoCo report
mvn clean test
```
The HTML coverage report is automatically generated at:
`docs/jacoco/index.html`

---

## ⚠️ Known Limitations & Design Trade-offs

1. **Stateless JWT Invalidation:**
   - *Trade-off:* JWTs are cryptographically self-contained for maximum horizontal scalability. 
   - *Limitation:* Tokens cannot be instantaneously invalidated prior to their 24-hour expiration without introducing state (e.g. a shared Redis distributed blacklist).
2. **PaaS Free-Tier Sleep Latency:**
   - *Limitation:* Free-tier hosting providers (e.g., Render) spin down inactive containers after 15 minutes of inactivity. Initial "cold start" requests can take 30-45 seconds. Subsequent requests maintain sub-30ms performance.
3. **Database Connection Limits:**
   - *Design Choice:* HikariCP is capped at `maximumPoolSize = 10` to avoid exhausting connection pools on free managed databases (such as Railway or Render MySQL).

---

## 📜 Demo Video Walkthrough

The 3-minute live API walkthrough covers:
1. Public endpoint inspection and OpenAPI documentation navigation.
2. User registration, credential login, and JWT Bearer authorization.
3. Role-based access control enforcement (403 Forbidden for `ROLE_USER` vs successful mutation for `ROLE_ADMIN`).
4. Live execution of the 1,000-record performance benchmark proving sub-500ms latency.
