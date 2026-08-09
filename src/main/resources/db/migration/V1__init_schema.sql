-- V1__init_schema.sql
-- Database Schema for Recruitment Management Platform

-- 1. ROLES TABLE
CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name text NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES 
('ROLE_ADMIN'), 
('ROLE_HR'), 
('ROLE_INTERVIEWER');

-- 2. USERS TABLE (One Role to Many Users)
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    email text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. REFRESH_TOKENS TABLE
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    token_hash text NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. JOBS TABLE
CREATE TABLE jobs (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title text NOT NULL,
    description TEXT NOT NULL,
    department text NOT NULL,
    location text NOT NULL,
    status text NOT NULL DEFAULT 'DRAFT',
    created_by_id VARCHAR(36) NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. CANDIDATES TABLE
CREATE TABLE candidates (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text NOT NULL UNIQUE,
    phone text,
    years_of_experience INT DEFAULT 0,
    cv_file_path text,
    cv_original_filename text,
    cv_file_type text,
    created_by_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. SKILLS TABLE (Normalized Skill Entities)
CREATE TABLE skills (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name text NOT NULL UNIQUE
);

-- 7. CANDIDATE_SKILLS JOIN TABLE (Many-To-Many Relationship)
CREATE TABLE candidate_skills (
    candidate_id VARCHAR(36) NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    skill_id VARCHAR(36) NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (candidate_id, skill_id)
);

-- 8. TAGS TABLE
CREATE TABLE tags (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name text NOT NULL UNIQUE
);

-- 9. CANDIDATE_TAGS JOIN TABLE
CREATE TABLE candidate_tags (
    candidate_id VARCHAR(36) NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    tag_id VARCHAR(36) NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (candidate_id, tag_id)
);

-- 10. APPLICATIONS TABLE
CREATE TABLE applications (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    candidate_id VARCHAR(36) NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_id VARCHAR(36) NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status text NOT NULL DEFAULT 'APPLIED',
    assigned_recruiter_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidate_job UNIQUE (candidate_id, job_id)
);

-- 11. APPLICATION_ASSIGNMENTS TABLE (Bonus ATS)
CREATE TABLE application_assignments (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    application_id VARCHAR(36) NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    interviewer_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_interviewer UNIQUE (application_id, interviewer_id)
);

-- 12. INTERVIEW_FEEDBACKS TABLE (Bonus ATS)
CREATE TABLE interview_feedbacks (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    application_id VARCHAR(36) NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    interviewer_id VARCHAR(36) NOT NULL REFERENCES users(id),
    overall_score NUMERIC(3, 2) NOT NULL,
    comments TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. AUDIT_LOGS TABLE (Append-Only)
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    event_type text NOT NULL,
    actor_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    entity_type text,
    entity_id text,
    from_state text,
    to_state text,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES FOR HIGH-PERFORMANCE SEARCH & LOOKUPS
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_candidates_email ON candidates(email);
CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_candidate_skills_skill ON candidate_skills(skill_id);
CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_applications_candidate ON applications(candidate_id);
CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
