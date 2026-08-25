# Banque Misr Recruitment Management Platform
## Master Defense & Technical Presentation Guide

---

## 📑 Table of Contents
1. [Executive Summary & System Architecture](#1-executive-summary--system-architecture)
2. [Software Engineering & Design Patterns](#2-software-engineering--design-patterns)
3. [Database Architecture & Entity Relationships (ERD)](#3-database-architecture--entity-relationships-erd)
4. [Security Deep Dive: Authentication, JWT, and Refresh Tokens](#4-security-deep-dive-authentication-jwt-and-refresh-tokens)
   - *Why Stateless JWT?*
   - *Access Token vs. Refresh Token Anatomy*
   - *Why Refresh Token Rotation (RTR)?*
   - *Why We Hash Refresh Tokens in the Database*
   - *Spring Security Filter Chain & Context Execution Flow*
   - *LDAP Directory Integration vs. Traditional DB Auth*
   - *Role-Based Access Control (RBAC) & Method Security*
5. [AI-Powered CV Parsing & Asynchronous Processing Engine](#5-ai-powered-cv-parsing--asynchronous-processing-engine)
6. [Services & Business Logic Walkthrough](#6-services--business-logic-walkthrough)
7. [Error Handling & API Design Standards](#7-error-handling--api-design-standards)
8. [Common Defense Questions & Model Answers](#8-common-defense-questions--model-answers)

---

# 1. Executive Summary & System Architecture

### What is the Recruitment Management Platform?
The **Banque Misr Recruitment Management Platform** is an enterprise-grade, secure, production-ready backend system designed to streamline recruitment, applicant tracking, and interview workflows. It eliminates recruitment bottlenecks by:
1. Integrating with **OpenLDAP** for corporate identity management.
2. Automating resume intake via an **AI-powered in-memory CV parsing engine** (Gemini 2.5 Flash + Apache PDFBox / POI).
3. Enforcing an **Application State Machine** for recruitment stages.
4. Implementing a high-performance **JPA Specification search engine** across candidates, skills, tags, and experience.

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|           Frontend Web Apps (React/Angular) / Swagger UI / Postman Clients        |
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

### Key Technology Stack Choices
- **Language**: Java 21 (Records, Pattern Matching, Text Blocks, Sealed Types).
- **Framework**: Spring Boot 3.4 / 4.x + Spring Security 6 / 7 (Lambda DSL).
- **Database & Migration**: PostgreSQL 16 managed strictly through versioned **Flyway** scripts (`V1__init_schema.sql`, `V2__drop_password_hash.sql`).
- **Identity & Directory**: OpenLDAP via Spring LDAP (`LdapTemplate`, `BindAuthenticator`).
- **AI / LLM**: Google Gemini 2.5 Flash (`RestClient`) for structured entity extraction.
- **Parsing**: Apache PDFBox (PDFs) & Apache POI (DOCX).
- **Mailing**: Spring Mail with MailDev SMTP server.

---

# 2. Software Engineering & Design Patterns

We applied recognized design patterns and SOLID principles to ensure loose coupling, high testability, and clear separation of concerns.

| Pattern | Where It Is Used | Why We Used It |
| :--- | :--- | :--- |
| **Chain of Responsibility** | `SecurityFilterChain` & `JwtAuthenticationFilter` | Each filter examines the HTTP request, handles its specific responsibility (CORS, JWT validation, authentication), and decides whether to terminate or pass down the chain. |
| **Strategy Pattern** | `CvParser` interface (`PdfCvParser`, `DocxCvParser`) & `CvParserRegistry` | The parsing algorithm is selected dynamically at runtime based on file MIME type/extension. Adding a new format (e.g., `.txt` or `.rtf`) requires adding a new strategy class without modifying existing parsers (Open/Closed Principle). |
| **State Pattern / Finite State Machine** | `ApplicationStatus` Enum (`APPLIED`, `SCREENING`, `INTERVIEW`, `OFFER`, `HIRED`, `REJECTED`, `WITHDRAWN`) | Legal transitions are hard-coded in the enum (`nextStates()`). The entity guards against invalid state transitions (e.g., jumping from `APPLIED` straight to `HIRED`), throwing `IllegalStateTransitionException`. |
| **Specification Pattern** | `CandidateSpecification` with JPA Criteria API | Dynamically builds complex SQL `WHERE` clauses with composable predicates (free-text name search, skills, tags, experience ranges, application subqueries) without string concatenation, eliminating SQL injection risks. |
| **Adapter Pattern** | `DocxCvParser` & `PdfCvParser` | Wraps complex third-party library APIs (Apache PDFBox, Apache POI) behind our clean application interface `CvParser`. |
| **Factory / Builder Pattern** | Lombok `@Builder`, `PageResponse.of()`, `Jwts.builder()` | Simplifies immutable object creation and clean payload building without cumbersome constructors. |
| **Proxy Pattern (AOP)** | `@Transactional`, `@PreAuthorize`, `@Async` | Spring wraps bean methods with dynamic proxies to handle transaction boundaries, security checks, and background thread delegation transparently. |
| **Data Transfer Object (DTO)** | `*Request` and `*Respond` classes | Isolates database persistence entities from API contracts, preventing over-exposure of internal database fields and circular serialization loops. |

---

# 3. Database Architecture & Entity Relationships (ERD)

The database schema is fully normalized to Third Normal Form (3NF), with unique constraints and foreign key cascade rules.

```
       +--------------------+
       |       users        |
       +--------------------+
       | PK id (UUID)       |
       |    email (UNIQUE)  |
       |    first_name      |
       |    last_name       |
       |    role            |
       |    enabled         |
       +---------+----------+
                 | 1
                 |
                 | 1 (One-to-One)
                 v
       +--------------------+
       |   refresh_tokens   |
       +--------------------+
       | PK id (UUID)       |
       | FK user_id (UNIQUE)|
       |    token_hash(UNIQ)|
       |    expires_at      |
       +--------------------+

       +--------------------+              +--------------------+
       |       users        |              |       users        |
       +---------+----------+              +---------+----------+
                 | 1 (createdBy)                     | 1 (assignedRecruiter)
                 |                                   |
                 v *                                 v *
       +--------------------+              +--------------------+
       |        jobs        | 1          * |    applications    |
       +--------------------+--------------+--------------------+
       | PK id (UUID)       |<------------+| PK id (UUID)       |
       |    title           | (Job Has     | FK candidate_id    |
       |    description     |  Many Apps)  | FK job_id          |
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

### Entity Summary & Relational Mapping
1. **`UserEntity`**: Represents administrative users, HR personnel, and interviewers. Password hashes are stored in OpenLDAP, while metadata (name, email, role, enabled) lives in PostgreSQL.
2. **`RefreshTokenEntity`**: Enforces a strict **1-to-1** relationship with `UserEntity`. Stored with a SHA-256 hash of the refresh token.
3. **`JobEntity`**: Represents job openings created by HR/Admin (`ManyToOne` with `UserEntity`).
4. **`CandidateEntity`**: Stores parsed or manually created applicant profiles. Employs a **Zero-File-Retention** strategy (does not persist raw files to disk to respect data privacy laws).
5. **`SkillEntity` & `TagEntity`**: Independent lookup tables joined via `candidate_skills` and `candidate_tags` (`ManyToMany`).
6. **`ApplicationEntity`**: Connects a candidate to a job opening. Has a unique constraint `uk_candidate_job(candidate_id, job_id)` so a candidate cannot apply to the same job multiple times.
7. **`InterviewFeedbackEntity`**: Created when an application transitions to the `INTERVIEW` state. Only the designated interviewer can record comments and scores.

---

# 4. Security Deep Dive: Authentication, JWT, and Refresh Tokens

> ⚠️ **Defense Critical Section**: Master the following concepts and diagrams to confidently answer questions about authorization and cryptography.

---

### 4.1 Why Stateless JWT Authentication?
In traditional session-based authentication:
- The server stores session objects in memory (`HttpSession`) or Redis.
- Every client request includes a `JSESSIONID` cookie, requiring a lookup on every single request.
- This creates state on the server, making horizontal scaling complex.

In our **Stateless JWT Architecture**:
- The server issues a cryptographically signed JSON Web Token containing claims (`sub`, `userId`, `role`, `exp`).
- On every subsequent request, the client sends `Authorization: Bearer <accessToken>`.
- The server validates the token mathematically using its secret key (`HMAC-SHA256`).
- **No database query is needed to verify the access token!** This delivers ultra-low latency and seamless horizontal scaling.

---

### 4.2 Anatomy of an Access Token vs. Refresh Token

#### The Access Token (JWT)
- **Format**: `Base64URL(Header) . Base64URL(Payload) . Base64URL(Signature)`
- **Lifespan**: Very short (**5 minutes** = 300,000 ms).
- **Contents**:
  - `Header`: Algorithm (`HS256`), Type (`JWT`).
  - `Payload`: Claims (`sub: "user@banquemisr.com"`, `userId: "uuid"`, `role: "ROLE_HR"`, `iat`, `exp`).
  - `Signature`: `HMACSHA256(Base64(Header) + "." + Base64(Payload), SECRET_KEY)`
- **Storage**: Held in memory on client side.

#### The Refresh Token (Opaque High-Entropy String)
- **Format**: Double UUID string (`UUID.randomUUID() + UUID.randomUUID()`), e.g., `d9b4c2a1-7e8f-...`.
- **Lifespan**: Longer (**24 hours** = 86,400,000 ms).
- **Storage**: Sent to client, stored in database as a **SHA-256 Hash**.

---

### 4.3 Why Refresh Token Rotation (RTR)?
#### *The Core Question: "When I refresh my token, why do I generate a NEW access token AND a NEW refresh token?"*

```
               CLIENT                                                SERVER
                 |                                                     |
                 |--- 1. POST /auth/refresh?refreshToken=RT_1 ------->|
                 |                                                     | (Checks SHA-256(RT_1) in DB)
                 |                                                     | (Valid & Not Expired)
                 |                                                     | [INVALIDATES RT_1]
                 |                                                     | [GENERATES RT_2 & AT_2]
                 |                                                     | [SAVES SHA-256(RT_2) to DB]
                 |<-- 2. Returns AuthResponse(AT_2, RT_2) ------------|
                 |                                                     |
                 |=== IF AN ATTACKER TRIES TO REUSE OLD RT_1: ========|
                 |                                                     |
  [Attacker] --->|--- 3. POST /auth/refresh?refreshToken=RT_1 ------->|
                 |                                                     | (Hashes RT_1 -> Not Found!)
                 |<-- 4. 401 Unauthorized ("Invalid refresh token") ---| (Attack Neutralized!)
```

#### The 3 Crucial Security Reasons for Refresh Token Rotation:
1. **Single-Use Invalidation (Revocation of Old Tokens)**:
   - If an access token were long-lived (e.g., 30 days) and an attacker intercepted it from network traffic or local storage, they would possess unrestricted access for 30 days.
   - By keeping access tokens short (5 min) and rotating the refresh token upon every use, each refresh token is **single-use only**. Once exchanged, the old refresh token is destroyed immediately.
2. **Detection & Neutralization of Token Theft**:
   - If an attacker intercepts `RT_1` and the legitimate user uses `RT_1` first, `RT_1` is replaced with `RT_2` in the database.
   - When the attacker subsequently attempts to refresh using `RT_1`, the lookup fails, immediately preventing unauthorized access.
3. **Limiting the Window of Exposure**:
   - Even if a client is compromised, the attacker only has minutes before the access token expires, and cannot silently persist forever because the refresh token has already moved forward.

---

### 4.4 Why We Hash Refresh Tokens in the Database (`SHA-256`)
- **Defense in Depth**: We treat refresh tokens with the same sensitivity as passwords.
- If an attacker performs an SQL injection or acquires a raw database backup dump, they will only see `token_hash` (one-way cryptographic hash: `SHA-256`).
- Because SHA-256 cannot be reversed, the attacker cannot forge HTTP refresh requests against `/auth/refresh` without the plaintext raw token that exists only on the client device.

---

### 4.5 The Spring Security Filter Chain & Request Lifecycle

```
[Incoming HTTP Request] (e.g. GET /api/v1/candidates)
       |
       v
[CorsFilter] -----------------------------------> Verifies Origin (localhost:3000/4200)
       |
       v
[JwtAuthenticationFilter] ----------------------> Intercepts request BEFORE UsernamePasswordAuthFilter
       |
       +---> Check "Authorization" Header
       |        |
       |        +-- Absent or not "Bearer "? -> Skip filter (pass downstream)
       |        |
       |        +-- Present: Extract Token -> Validate Signature & Expiry (JwtService.isTokenValid)
       |                |
       |                +-- Invalid / Expired: Let chain continue (SecurityContext remains empty)
       |                |
       |                +-- Valid:
       |                     1. Extract username & role claim (e.g., "ROLE_HR")
       |                     2. Create UsernamePasswordAuthenticationToken(username, null, authorities)
       |                     3. Set details (remote IP, session details)
       |                     4. SecurityContextHolder.getContext().setAuthentication(auth)
       v
[AuthorizationFilter / FilterSecurityInterceptor]
       |
       +---> Evaluates Route Matchers (.requestMatchers("/auth/**").permitAll())
       |
       +---> Evaluates Method Security (@PreAuthorize("hasRole('ADMIN')"))
       |        |
       |        +-- Unauthenticated? -> Triggers AuthenticationEntryPoint (401 JSON)
       |        +-- Insufficient Role? -> Triggers AccessDeniedHandler (403 JSON)
       v
[DispatcherServlet -> CandidateController.getAllCandidates()]
```

---

### 4.6 LDAP Directory Integration vs. Database Auth

```
+-----------------------------------------------------------------------------------+
|                        BANQUE MISR ENTERPRISE DIRECTORY                           |
|                                                                                   |
|   dc=banquemisr,dc=com                                                            |
|   ├── ou=people (Users)                                                           |
|   │   ├── uid=admin (mail: admin@banquemisr.com, userPassword: {SSHA}...)         |
|   │   ├── uid=hr_lead (mail: hr@banquemisr.com, userPassword: {SSHA}...)          |
|   └── ou=groups (Roles)                                                           |
|       ├── cn=ROLE_ADMIN (member: uid=admin,ou=people,dc=banquemisr,dc=com)        |
|       ├── cn=ROLE_HR                                                              |
|       └── cn=ROLE_INTERVIEWER                                                     |
+-----------------------------------------------------------------------------------+
```

#### Why did we drop `password_hash` from the PostgreSQL `users` table?
In banking and enterprise environments:
1. **Centralized Identity Management**: Employees log in with their corporate Active Directory / LDAP credentials. Passwords should never be duplicated across individual application databases.
2. **Zero Password Liability**: If the recruitment database is ever compromised, no user credentials exist in PostgreSQL.
3. **Single Sign-On (SSO) & Policy Enforcement**: Enterprise password policies (complexity, rotation, account lockouts) are handled globally by LDAP.
4. **On-Demand Just-In-Time (JIT) User Provisioning**: When a user logs in through LDAP for the first time, `LoginService.syncLdapUserToDatabase()` dynamically provisions their profile in PostgreSQL, extracting their role directly from LDAP group memberships.

---

### 4.7 Route vs. Method Security (`@PreAuthorize`)

| Security Level | Implementation | Example | Best Used For |
| :--- | :--- | :--- | :--- |
| **Route Level** | `SecurityConfig.securityFilterChain()` with `.requestMatchers()` | `.requestMatchers("/auth/**", "/swagger-ui/**").permitAll()` | Coarse-grained firewall filtering for public endpoints, documentation, and health probes. |
| **Method Level** | `@PreAuthorize("hasRole('ADMIN')")` | `@PreAuthorize("hasAnyRole('ADMIN', 'HR')")` | Fine-grained, declarative business access control placed directly on controllers and services. |

---

# 5. AI-Powered CV Parsing & Asynchronous Processing Engine

```
[Client] ---> POST /api/v1/candidates/bulk-upload (List<MultipartFile> up to 20 files)
   |
   v
[CandidateController] ---> Validates File Size (<= 10MB) & Allowed MIME (PDF, DOCX)
   |
   v
[CandidateService] ------> Reads byte arrays in-memory (InMemoryFile record)
   |
   +---> Creates BulkUploadJob(jobId, totalFiles) in ConcurrentHashMap Registry
   |
   +---> Dispatches BulkCvProcessingService.processBulkUploadAsync(jobId, files)
   |
   +---> Returns HTTP 202 Accepted with trackable BulkUploadProgressRespond
```

### The In-Memory Extraction Pipeline (Per CV)
```
  [InMemory ByteArray]
           |
           v
  [CvParserRegistry] ------> Selects Strategy: PdfCvParser (PDFBox) or DocxCvParser (POI)
           |
           v
     [Raw Text] (Truncated to 15,000 characters to prevent prompt bloat)
           |
           v
   [CvDataExtractor] ------> System Prompt: "Extract candidate details as STRICT JSON"
           |
           v
  [GeminiLlmClient] -------> Google Gemini 2.5 Flash API (REST JSON over HTTPS)
           |
           v
  [ParsedCv DTO] ----------> { firstName, lastName, email, phone, yearsOfExperience, skills }
           |
           v
  [CandidateEntity] -------> Deduplicates Email, Saves Normalized Skills, Persists to DB
                             (Zero File Retention: cvFilePath = null)
```

#### Why Zero File Retention?
In compliance with **GDPR Article 5 & 17** (Data Minimization and Storage Limitation):
- Resumes contain Sensitive Personal Identifiable Information (PII).
- Instead of storing gigabytes of unmanaged PDFs on the server filesystem, our system extracts structured attributes directly from memory streams and discards the raw file buffer immediately.

---

# 6. Services & Business Logic Walkthrough

| Service Name | Primary Responsibilities |
| :--- | :--- |
| **`LoginService`** | Authenticates credentials against LDAP via `AuthenticationManager`, synchronizes LDAP user to Postgres database JIT, verifies `enabled` flag, and triggers token issuance. |
| **`RefreshTokenService`** | Generates UUID pairs, computes SHA-256 token hashes, performs refresh token rotation (RTR), validates expiration timestamps, and revokes tokens upon logout. |
| **`PasswordResetService`** | Implements secure password recovery using short-lived (15 min) HMAC-signed JWT reset tokens and updates credentials directly in the LDAP directory. Protects against User Enumeration. |
| **`LdapUserService`** | Manages LDAP user lifecycles: bindings, DN resolution, password updates (`ModificationItem`), and entry deletion via `LdapTemplate`. |
| **`CandidateService`** | Manages manual candidate registration, single/bulk CV uploads, skill/tag associations, and dynamic JPA Specification searches. |
| **`BulkCvProcessingService`** | Manages asynchronous bulk processing on bounded thread pools (`cvBulkAsyncExecutor`), tracking job progress in a thread-safe `ConcurrentHashMap`. |
| **`CvParsingService`** | Coordinates in-memory text parsing across registered parser strategies and invokes the AI data extractor. |
| **`JobService`** | Handles job requisition CRUD, status lifecycle management (`DRAFT`, `PUBLISHED`, `CLOSED`), and creator linkage. |
| **`ApplicationService`** | Manages job applications, enforces unique applicant-job constraints, triggers state machine transitions, schedules interviews, and sends automated candidate email invites. |
| **`InterviewFeedbackService`** | Enforces interviewer ownership so only the assigned interviewer can submit interview ratings and comments. |
| **`EmailService`** | Formats and dispatches transactional HTML/Text emails for password resets and interview invitations via JavaMail / MailDev. |

---

# 7. Error Handling & API Design Standards

All exceptions are intercepted centrally by **`GlobalExceptionHandler`** (`@RestControllerAdvice`), returning RFC 7807 problem details:

```json
{
  "timestamp": "2026-08-25T13:45:00.123Z",
  "status": 400,
  "error": "Validation Failed",
  "details": {
    "email": "Email must be valid format",
    "password": "Password must be at least 8 characters"
  }
}
```

### Standard HTTP Status Code Conventions
- `200 OK`: Successful synchronous retrieval or update.
- `201 Created`: Resource successfully created (User, Job, Candidate, Application).
- `202 Accepted`: Asynchronous batch job accepted for background execution (Bulk CV Upload).
- `204 No Content`: Successful deletion or password update without return payload.
- `400 Bad Request`: Validation failure (`MethodArgumentNotValidException`) or malformed parameters.
- `401 Unauthorized`: Bad credentials, missing token, or expired refresh token.
- `403 Forbidden`: Insufficient role permissions or account disabled.
- `404 Not Found`: Entity not found (`ResourceNotFoundException`).
- `409 Conflict`: Duplicate email, already applied for job, or illegal state machine transition.

---

# 8. Common Defense Questions & Model Answers

### Q1: "Why did you choose JWT over traditional server sessions?"
> **Answer**: "Server sessions require in-memory session state on the server or a shared session cluster like Redis. In a stateless microservice or modern cloud architecture, JWT allows the server to verify authenticity via cryptographic signature verification without performing database I/O for every request, delivering superior horizontal scalability."

### Q2: "What prevents a user from modifying their JWT payload to change their role to ROLE_ADMIN?"
> **Answer**: "The JWT payload is Base64Url encoded, meaning anyone can read it, but it is cryptographically signed using HMAC-SHA256 with our private server secret key. If a user modifies a single bit in the payload (like changing their role), the cryptographic signature computed by the server during `JwtAuthenticationFilter` will no longer match the token signature, and the request is immediately rejected with a 401 Unauthorized error."

### Q3: "Explain why Refresh Token Rotation is essential."
> **Answer**: "Because access tokens are short-lived (5 minutes), clients must use refresh tokens to obtain new access tokens. If a refresh token were static and hijacked, the attacker would have perpetual access. With Refresh Token Rotation (RTR), every time a refresh token is used, it is revoked and replaced with a brand-new refresh token. If an attacker tries to use an already-used refresh token, the server detects the invalid token and blocks the request."

### Q4: "Why store the refresh token as a SHA-256 hash in PostgreSQL?"
> **Answer**: "It follows the principle of Defense in Depth. Just as passwords should never be stored in plaintext, refresh tokens have high privilege. If our database is dumped or compromised, the attacker cannot use the stored hash strings to invoke `/auth/refresh` because the API requires the raw, unhashed token."

### Q5: "How does the Application State Machine prevent invalid transitions?"
> **Answer**: "We implemented the State Pattern inside the `ApplicationStatus` enum. Each status explicitly defines its allowable successor states in `nextStates()`. Inside `ApplicationEntity.transitionTo(newStatus)`, if the transition is not in the allowed set, it throws an `IllegalStateTransitionException`, preventing illegal workflows such as jumping directly from `APPLIED` to `HIRED`."

### Q6: "Why do you use JPA Specifications instead of writing custom SQL strings for candidate search?"
> **Answer**: "JPA Specifications provide type-safe, composable query predicates using the JPA Criteria API. It enables dynamic combining of name, skill, tag, and experience filters based on whatever parameters the client passes, without building concatenated SQL query strings which are prone to SQL injection vulnerabilities and syntax bugs."
