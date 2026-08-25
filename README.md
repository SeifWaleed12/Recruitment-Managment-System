# 🏢 Banque Misr — Recruitment Management Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F.svg?style=flat&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![OpenLDAP](https://img.shields.io/badge/OpenLDAP-Directory-red.svg?style=flat)](https://www.openldap.org/)
[![Gemini AI](https://img.shields.io/badge/Google%20Gemini-2.5%20Flash-blueviolet.svg?style=flat&logo=google)](https://deepmind.google/technologies/gemini/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0202.svg?style=flat&logo=flyway)](https://flywaydb.org/)

An enterprise-grade, secure, and intelligent backend platform designed for **Banque Misr** to automate talent acquisition, resume ingestion, applicant stage tracking, and technical interview evaluations.

---

## 📑 Table of Contents
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Design Patterns & Architecture Highlights](#-design-patterns--architecture-highlights)
- [Technology Stack](#-technology-stack)
- [Database Schema & ERD](#-database-schema--erd)
- [REST API Reference](#-rest-api-reference)
- [Security & Authentication Architecture](#-security--authentication-architecture)
- [Getting Started & Local Setup](#-getting-started--local-setup)
- [Environment Configuration](#-environment-configuration)
- [Documentation & Resources](#-documentation--resources)

---

## 🚀 Key Features

### 🔐 Enterprise Security & Identity
- **Centralized OpenLDAP Directory**: Corporate authentication directly integrated with OpenLDAP using `LdapTemplate` and `BindAuthenticator`.
- **Stateless JWT with Refresh Token Rotation (RTR)**: Short-lived HMAC-SHA256 Access Tokens (5 minutes) coupled with single-use, database-persisted SHA-256 hashed Refresh Tokens (24 hours).
- **Role-Based Access Control (RBAC)**: Fine-grained method-level security (`@PreAuthorize`) enforcing segregation of duties across `ROLE_ADMIN`, `ROLE_HR`, and `ROLE_INTERVIEWER`.
- **Zero-Password Storage**: Application database contains zero user password hashes, removing credential compromise liabilities.

### 🤖 AI-Powered In-Memory CV Parsing
- **Zero File Retention (GDPR Article 5 & 17 Compliant)**: Processes uploaded resumes entirely within memory buffers (`byte[]` streams) without storing physical files on server disks.
- **Multi-Format Parsing**: Strategy-based text extraction supporting PDF (Apache PDFBox) and DOCX (Apache POI).
- **Gemini 2.5 Flash LLM Extraction**: Sends extracted resume text with a strict JSON schema prompt to Google Gemini AI to accurately extract candidate details (`firstName`, `lastName`, `email`, `phone`, `yearsOfExperience`, `skills`).
- **Asynchronous Bulk Ingestion**: Non-blocking batch processing of up to 20 CVs simultaneously using bounded thread pools with live job polling endpoints.

### 📋 Applicant Tracking System (ATS) & State Machine
- **Recruitment State Machine**: Enforces a strict finite state machine on candidate applications:
  $$\text{APPLIED} \longrightarrow \text{SCREENING} \longrightarrow \text{INTERVIEW} \longrightarrow \text{OFFER} \longrightarrow \text{HIRED}$$
  *(with branching to $\text{REJECTED}$ or $\text{WITHDRAWN}$ at each phase)*.
- **Dynamic Search Engine**: Composable dynamic filtering via **JPA Specifications** (free-text name, skill set matching, tag combinations, experience ranges, and application stages) without raw SQL concatenation.
- **Interview Scheduling & Automated Emails**: Automatically provisions feedback entries and dispatches transactional candidate email invitations upon moving to the `INTERVIEW` phase (via MailDev SMTP).

---

## 🏛️ System Architecture

```
+-----------------------------------------------------------------------------------+
|                                   CLIENT LAYER                                    |
|             Web Clients (React/Angular) | Swagger UI | Postman Collections        |
+------------------------------------------+----------------------------------------+
                                           | HTTPS / REST JSON
                                           v
+-----------------------------------------------------------------------------------+
|                            SPRING SECURITY FILTER CHAIN                           |
|  [CorsFilter] -> [JwtAuthenticationFilter] -> [UsernamePasswordAuthenticationFilter] |
|              - Validates HMAC-SHA256 Signature & Expiration                       |
|              - Populates SecurityContextHolder with Authorities                   |
+------------------------------------------+----------------------------------------+
                                           | Authenticated Request
                                           v
+-----------------------------------------------------------------------------------+
|                                 CONTROLLER LAYER                                  |
|   AuthController | CandidateController | JobController | ApplicationController    |
|   UserController | RoleController      | TagController | InterviewFeedbackCtrl    |
+------------------------------------------+----------------------------------------+
                                           | DTOs (Request / Response)
                                           v
+-----------------------------------------------------------------------------------+
|                                  SERVICE LAYER                                    |
|  LoginService | RefreshTokenService | LdapUserService | CandidateService          |
|  CvParsingService | BulkCvProcessingService | JobService | ApplicationService     |
|  InterviewFeedbackService | EmailService | PasswordResetService                   |
+-------------------+----------------------+--------------------+-------------------+
                    |                      |                    |
                    v                      v                    v
+-----------------------+  +-------------------+  +---------------------------------+
|  PERSISTENCE LAYER    |  |    AI / PARSING   |  |     CORPORATE DIRECTORY         |
|  Spring Data JPA      |  |  Apache PDFBox    |  |  OpenLDAP (osixia/openldap)     |
|  Flyway Migrations    |  |  Apache POI       |  |  Spring LDAP (LdapTemplate)     |
|  PostgreSQL 16        |  |  Gemini 2.5 Flash |  |  MailDev (Local SMTP Server)    |
+-----------------------+  +-------------------+  +---------------------------------+
```

---

## 💡 Design Patterns & Architecture Highlights

| Design Pattern | Implementation Location | Architectural Justification |
| :--- | :--- | :--- |
| **Chain of Responsibility** | `SecurityFilterChain` & `JwtAuthenticationFilter` | Modular request inspection (CORS, token parsing, authentication context population). |
| **Strategy Pattern** | `CvParser` (`PdfCvParser`, `DocxCvParser`) + `CvParserRegistry` | File format parsing strategies selected dynamically at runtime; extensible without modifying existing code (OCP). |
| **State Pattern** | `ApplicationStatus` Enum (`nextStates()`, `canTransitionTo()`) | Domain-level validation preventing illegal application lifecycle jumps. |
| **Specification Pattern** | `CandidateSpecification` + JPA Criteria API | Type-safe, dynamic SQL WHERE predicate composition avoiding SQL injection risks. |
| **Adapter Pattern** | `DocxCvParser` & `PdfCvParser` | Decouples third-party parsing libraries (PDFBox, POI) behind our domain interfaces. |
| **Factory / Builder** | Lombok `@Builder`, `PageResponse.of()`, `Jwts.builder()` | Clean, immutable instantiation of complex data transfers and tokens. |
| **Proxy Pattern (AOP)** | `@Transactional`, `@PreAuthorize`, `@Async` | Declarative transaction boundaries, authorization gates, and background execution. |

---

## 🛠️ Technology Stack

- **Core Runtime**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4.x / Spring Framework 6.x
- **Security**: Spring Security 6.x (Lambda DSL, Stateless Session Policy)
- **Database**: PostgreSQL 16 (Alpine)
- **Schema Management**: Flyway Database Migrations
- **Directory Service**: OpenLDAP (`osixia/openldap:1.5.0`)
- **AI / LLM Integration**: Google Gemini 2.5 Flash (`RestClient`)
- **Document Parsing**: Apache PDFBox 3.0.x, Apache POI 5.2.x
- **Email Testing**: MailDev SMTP & Web Dashboard
- **Documentation**: Springdoc OpenAPI / Swagger UI 3

---

## 🗄️ Database Schema & ERD

```
       +--------------------+                    +--------------------+
       |       users        | 1                1 |   refresh_tokens   |
       +--------------------+------------------->+--------------------+
       | PK id (UUID)       |                    | PK id (UUID)       |
       |    email (UNIQUE)  |                    | FK user_id (UNIQUE)|
       |    first_name      |                    |    token_hash(UNIQ)|
       |    last_name       |                    |    expires_at      |
       |    role            |                    +--------------------+
       |    enabled         |
       +---------+----------+
                 | 1 (createdBy)
                 v *
       +--------------------+              +--------------------+
       |        jobs        | 1          * |    applications    |
       +--------------------+--------------+--------------------+
       | PK id (UUID)       |              | PK id (UUID)       |
       |    title           |              | FK candidate_id    |
       |    description     |              | FK job_id          |
       |    department      |              |    status          |
       |    status          |              | FK assigned_recr_id|
       +--------------------+              +---+-----------+----+
                                               | 1         | 1
                                               |           |
                        +----------------------+           +----------------------+
                        | *                                                       | *
                        v                                                         v
             +----------------------+                                  +----------------------+
             | interview_feedbacks  |                                  | application_assignm. |
             +----------------------+                                  +----------------------+
             | PK id (UUID)         |                                  | PK id (UUID)         |
             | FK application_id    |                                  | FK application_id    |
             | FK interviewer_id    |                                  | FK interviewer_id    |
             |    interview_date    |                                  +----------------------+
             |    overall_score     |
             |    comments          |
             +----------------------+

       +--------------------+              +--------------------+              +--------------------+
       |       skills       |              |     candidates     |              |        tags        |
       +--------------------+              +--------------------+              +--------------------+
       | PK id (UUID)       |              | PK id (UUID)       |              | PK id (UUID)       |
       |    name (UNIQUE)   |              |    email (UNIQUE)  |              |    name (UNIQUE)   |
       +---------+----------+              |    first_name      |              +---------+----------+
                 | *                       |    last_name       |                        | *
                 |                         |    phone           |                        |
                 |                         |    years_experience|                        |
                 v                         +---+-------------+--+                        v
       +--------------------+                  ^             ^                 +--------------------+
       |  candidate_skills  |                  |             |                 |   candidate_tags   |
       +--------------------+                  |             |                 +--------------------+
       | PK/FK candidate_id |------------------+             +-----------------| PK/FK candidate_id |
       | PK/FK skill_id     |                                                  | PK/FK tag_id       |
       +--------------------+                                                  +--------------------+
```

---

## 📡 REST API Reference

### 1. Authentication & Security (`/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Public | Authenticates against OpenLDAP, synchronizes user JIT, returns JWT & Refresh Token |
| `POST` | `/auth/refresh` | Public | Rotates Refresh Token and issues new Access + Refresh Token pair |
| `POST` | `/auth/forgot-password` | Public | Sends JWT password reset link via email (Enumeration-protected) |
| `POST` | `/auth/reset-password` | Public | Updates user password in OpenLDAP using signed reset token |

### 2. Candidate Management (`/api/v1/candidates`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/candidates` | Admin, HR, Interviewer | Returns paginated list of candidate profiles |
| `GET` | `/api/v1/candidates/search` | Admin, HR, Interviewer | Dynamic JPA Specification multi-criteria candidate search |
| `GET` | `/api/v1/candidates/{id}` | Admin, HR, Interviewer | Retrieves candidate details by ID |
| `POST` | `/api/v1/candidates/upload` | Admin, HR | Uploads single CV (PDF/DOCX) and parses via Gemini AI in-memory |
| `POST` | `/api/v1/candidates/bulk-upload` | Admin, HR | Asynchronously parses up to 20 CVs; returns trackable `jobId` |
| `GET` | `/api/v1/candidates/bulk-upload/{jobId}/status` | Admin, HR | Polls real-time progress of bulk CV parsing job |
| `POST` | `/api/v1/candidates` | Admin, HR | Manually registers candidate profile |
| `PUT` | `/api/v1/candidates/{id}` | Admin, HR | Updates candidate profile |
| `DELETE` | `/api/v1/candidates/{id}` | Admin, HR | Deletes candidate and associated join references |
| `POST` | `/api/v1/candidates/{id}/skills/{skillId}` | Admin, HR | Associates skill with candidate |
| `DELETE` | `/api/v1/candidates/{id}/skills/{skillId}` | Admin, HR | Removes skill from candidate |
| `POST` | `/api/v1/candidates/{id}/tags/{tagId}` | Admin, HR | Associates tag with candidate |
| `DELETE` | `/api/v1/candidates/{id}/tags/{tagId}` | Admin, HR | Removes tag from candidate |

### 3. Requisition & Job Openings (`/api/v1/jobs`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/jobs` | Admin, HR, Interviewer | Retrieves jobs (optional `status` query filter) |
| `GET` | `/api/v1/jobs/{id}` | Admin, HR, Interviewer | Retrieves single job details |
| `POST` | `/api/v1/jobs` | Admin, HR | Creates new job requisition |
| `PUT` | `/api/v1/jobs/{id}` | Admin, HR | Updates job opening |
| `PATCH` | `/api/v1/jobs/{id}/status` | Admin, HR | Updates job status (`DRAFT`, `PUBLISHED`, `CLOSED`) |
| `DELETE` | `/api/v1/jobs/{id}` | Admin, HR | Removes job requisition |

### 4. Application Tracking & Interview Management (`/api/v1/applications`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/applications` | Admin, HR | Submits application for candidate to job |
| `GET` | `/api/v1/applications/{id}` | Admin, HR, Interviewer | Retrieves application details |
| `GET` | `/api/v1/applications/job/{jobId}` | Admin, HR, Interviewer | Lists all applications submitted to a specific job |
| `GET` | `/api/v1/applications/candidate/{candidateId}` | Admin, HR, Interviewer | Lists all applications for a specific candidate |
| `PATCH` | `/api/v1/applications/{id}/status` | Admin, HR | Transitions state; auto-schedules interview & sends candidate email |
| `PATCH` | `/api/v1/applications/{id}/assign` | Admin, HR | Assigns recruiter to application |
| `DELETE` | `/api/v1/applications/{id}` | Admin, HR | Deletes application record |

### 5. Interview Feedback (`/api/v1/interview-feedbacks`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `PATCH` | `/api/v1/interview-feedbacks/{id}` | Interviewer | Submits ratings & remarks (enforces interviewer ownership) |
| `GET` | `/api/v1/interview-feedbacks/application/{appId}` | Admin, HR, Interviewer | Retrieves all recorded feedback for an application |

### 6. User Management & Administration (`/api/v1/users`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | Admin | Lists all system users |
| `GET` | `/api/v1/users/{id}` | Admin | Retrieves user profile by ID |
| `POST` | `/api/v1/users` | Admin | Provisions user in OpenLDAP & Postgres |
| `POST` | `/api/v1/users/interviewer` | Admin, HR | Quick-creates interviewer account |
| `PATCH` | `/api/v1/users/{id}/role` | Admin | Modifies user role permissions |
| `PATCH` | `/api/v1/users/{id}/enabled` | Admin | Enables/disables user account |
| `DELETE` | `/api/v1/users/{id}` | Admin | Deletes user from OpenLDAP and Postgres |

---

## 🔒 Security & Authentication Architecture

### 1. Stateless Access Tokens (5 Minutes)
Access tokens are signed using HMAC-SHA256. They contain authenticated claims:
```json
{
  "sub": "admin@banquemisr.com",
  "userId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "role": "ROLE_ADMIN",
  "iat": 1756130000,
  "exp": 1756130300
}
```
Validation occurs purely in memory inside `JwtAuthenticationFilter`, eliminating database lookups on every request.

### 2. Refresh Token Rotation (RTR)
```
  Client                          Backend Server                         Database
    |                                   |                                    |
    |-- 1. POST /auth/refresh(RT_1) --->|                                    |
    |                                   |-- 2. Lookup SHA-256(RT_1) -------->|
    |                                   |<-- Token Record Found & Valid -----|
    |                                   |                                    |
    |                                   |-- 3. Replace with SHA-256(RT_2) -->|
    |                                   |<-- Saved --------------------------|
    |<-- 4. Return (AT_2, RT_2) --------|                                    |
```
* **Single-Use Principle**: When `RT_1` is exchanged, it is instantly revoked and replaced with `RT_2`.
* **Theft Detection**: If an attacker attempts to replay old `RT_1`, the lookup fails immediately and access is denied.
* **Hashed at Rest**: Stored as a **SHA-256 hash** in PostgreSQL (`token_hash`), protecting credentials even in the event of database leaks.

---

## 🚀 Getting Started & Local Setup

### Prerequisites
- **Java 21 JDK** installed (`java -version`)
- **Docker & Docker Compose** installed and running
- **Maven** (or use included `./mvnw`)

### 1. Clone the Repository
```bash
git clone https://github.com/SeifWaleed12/Recruitment-Managment-System.git
cd Recruitment-Managment-System
```

### 2. Start Supporting Infrastructure
Run Docker Compose to spin up **PostgreSQL 16**, **OpenLDAP**, and **MailDev**:
```bash
docker compose up -d
```

Verify running containers:
```bash
docker compose ps
```
- **PostgreSQL**: `localhost:5432` (db: `recruitment_db`, user/pass: `postgres/postgres`)
- **OpenLDAP**: `localhost:389` (`dc=banquemisr,dc=com`)
- **MailDev UI**: `http://localhost:1080` (SMTP at `localhost:1025`)

### 3. Build & Run Application
```bash
./mvnw spring-boot:run
```
*(On Windows: `.\mvnw.cmd spring-boot:run`)*

The service will start on **`http://localhost:8080`**.

### 4. Interactive API Documentation (Swagger UI)
Visit the live Swagger UI dashboard in your browser:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

---

## ⚙️ Environment Configuration

Configuration values can be overridden via `application.yml` or environment variables:

| Variable | Default Value | Purpose |
| :--- | :--- | :--- |
| `JWT_SECRET` | `404E6352...` (256-bit Hex) | Secret key for signing Access Tokens |
| `LLM_API_KEY` | *(Configured Gemini Key)* | Google Gemini 2.5 Flash API Key |
| `LDAP_URLS` | `ldap://localhost:389` | OpenLDAP connection URL |
| `LDAP_BASE` | `dc=banquemisr,dc=com` | LDAP Base DN |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/recruitment_db` | PostgreSQL JDBC connection URL |

---

## 📚 Documentation & Resources

- 📬 **[Postman Collection](./Recruitment_Platform.postman_collection.json)** — Pre-configured request collection for all endpoints.
- 📜 **[LDIF Initial Directory Seed](./src/main/resources/users.ldif)** — Pre-populated LDAP accounts and role groups.

---

## 👥 Contributors & Authors
Developed for **Banque Misr Backend Engineering Track**.
- **Owner**: Seif Waleed
- **Supervising Architect**: Mohanad Magdy