ALTER TABLE users DROP COLUMN password_hash;
alter table interview_feedbacks add column interview_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table interview_feedbacks add column overall_score NUMERIC(3, 2);