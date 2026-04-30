-- V10_analytics_fix.sql

-- EXTENSIÓN
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- TABLAS (solo si no existen)
CREATE TABLE IF NOT EXISTS teacher_evaluation (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL,
    evaluator_id UUID,
    appointment_id UUID,
    rating_clarity SMALLINT NOT NULL,
    rating_knowledge SMALLINT NOT NULL,
    rating_support SMALLINT NOT NULL,
    rating_punctuality SMALLINT NOT NULL,
    comment TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS topic_interest_event (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    category_id BIGINT,
    subarea_id BIGINT,
    thread_id BIGINT,
    appointment_id UUID,
    video_meeting_id UUID,
    source_type VARCHAR(30),
    weight INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS topic_difficulty_event (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    teacher_id UUID,
    category_id BIGINT,
    subarea_id BIGINT,
    thread_id BIGINT,
    appointment_id UUID,
    video_meeting_id UUID,
    source_type VARCHAR(30),
    difficulty_level SMALLINT,
    created_at TIMESTAMP DEFAULT NOW()
    );

-- VISTAS SEGURAS
CREATE OR REPLACE VIEW vw_admin_topic_interest AS
SELECT
    fc.id,
    fc.name,
    COUNT(*) AS total
FROM topic_interest_event tie
         JOIN forum_category fc ON fc.id = tie.category_id
GROUP BY fc.id, fc.name;

CREATE OR REPLACE VIEW vw_admin_topic_difficulty AS
SELECT
    fc.id,
    fc.name,
    COUNT(*) AS total,
    AVG(tde.difficulty_level) AS avg_difficulty
FROM topic_difficulty_event tde
         JOIN forum_category fc ON fc.id = tde.category_id
GROUP BY fc.id, fc.name;

CREATE OR REPLACE VIEW vw_teacher_performance AS
SELECT
    u.id,
    u.full_name,
    COUNT(te.id) AS total_evaluations,
    AVG(te.rating_clarity) AS avg_clarity
FROM users u
         LEFT JOIN teacher_evaluation te ON te.teacher_id = u.id
GROUP BY u.id, u.full_name;