ALTER TABLE users DROP COLUMN password_hash;
alter table interview_feedbacks add column interview_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE interview_feedbacks ALTER COLUMN overall_score DROP NOT NULL;