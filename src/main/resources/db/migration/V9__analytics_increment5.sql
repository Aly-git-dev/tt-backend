-- V9__analytics_increment5.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE teacher_evaluation (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    evaluator_id UUID REFERENCES users(id) ON DELETE SET NULL,
                                    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
                                    rating_clarity SMALLINT NOT NULL CHECK (rating_clarity BETWEEN 1 AND 5),
                                    rating_knowledge SMALLINT NOT NULL CHECK (rating_knowledge BETWEEN 1 AND 5),
                                    rating_support SMALLINT NOT NULL CHECK (rating_support BETWEEN 1 AND 5),
                                    rating_punctuality SMALLINT NOT NULL CHECK (rating_punctuality BETWEEN 1 AND 5),
                                    comment TEXT,
                                    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
                                    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teacher_evaluation_teacher ON teacher_evaluation(teacher_id);
CREATE INDEX idx_teacher_evaluation_evaluator ON teacher_evaluation(evaluator_id);
CREATE INDEX idx_teacher_evaluation_appointment ON teacher_evaluation(appointment_id);
CREATE INDEX idx_teacher_evaluation_created ON teacher_evaluation(created_at DESC);

CREATE TABLE topic_interest_event (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      user_id UUID REFERENCES users(id) ON DELETE SET NULL,
                                      category_id BIGINT NOT NULL REFERENCES forum_category(id) ON DELETE CASCADE,
                                      subarea_id BIGINT REFERENCES forum_subarea(id) ON DELETE SET NULL,
                                      thread_id BIGINT REFERENCES forum_thread(id) ON DELETE SET NULL,
                                      appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
                                      video_meeting_id UUID REFERENCES video_meetings(id) ON DELETE SET NULL,
                                      source_type VARCHAR(30) NOT NULL,
                                      weight INTEGER NOT NULL DEFAULT 1 CHECK (weight > 0),
                                      created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_topic_interest_user ON topic_interest_event(user_id);
CREATE INDEX idx_topic_interest_category ON topic_interest_event(category_id);
CREATE INDEX idx_topic_interest_subarea ON topic_interest_event(subarea_id);
CREATE INDEX idx_topic_interest_created ON topic_interest_event(created_at DESC);

CREATE TABLE topic_difficulty_event (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        user_id UUID REFERENCES users(id) ON DELETE SET NULL,
                                        teacher_id UUID REFERENCES users(id) ON DELETE SET NULL,
                                        category_id BIGINT NOT NULL REFERENCES forum_category(id) ON DELETE CASCADE,
                                        subarea_id BIGINT REFERENCES forum_subarea(id) ON DELETE SET NULL,
                                        thread_id BIGINT REFERENCES forum_thread(id) ON DELETE SET NULL,
                                        appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
                                        video_meeting_id UUID REFERENCES video_meetings(id) ON DELETE SET NULL,
                                        source_type VARCHAR(30) NOT NULL,
                                        difficulty_level SMALLINT NOT NULL CHECK (difficulty_level BETWEEN 1 AND 5),
                                        notes TEXT,
                                        created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_topic_difficulty_user ON topic_difficulty_event(user_id);
CREATE INDEX idx_topic_difficulty_teacher ON topic_difficulty_event(teacher_id);
CREATE INDEX idx_topic_difficulty_category ON topic_difficulty_event(category_id);
CREATE INDEX idx_topic_difficulty_subarea ON topic_difficulty_event(subarea_id);
CREATE INDEX idx_topic_difficulty_created ON topic_difficulty_event(created_at DESC);

CREATE TABLE analytics_refresh_log (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       process_name VARCHAR(80) NOT NULL,
                                       status VARCHAR(20) NOT NULL CHECK (status IN ('STARTED', 'SUCCESS', 'FAILED')),
                                       details TEXT,
                                       started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                       finished_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_analytics_refresh_log_process ON analytics_refresh_log(process_name);
CREATE INDEX idx_analytics_refresh_log_started ON analytics_refresh_log(started_at DESC);

CREATE VIEW vw_admin_topic_interest AS
SELECT
    fc.id AS category_id,
    COALESCE(fc.code, fc.id::text) AS category_code,
    fc.name AS category_name,
    fs.id AS subarea_id,
    fs.name AS subarea_name,
    COUNT(tie.id) AS total_events,
    COALESCE(SUM(tie.weight), 0) AS weighted_score,
    COUNT(DISTINCT tie.user_id) AS unique_users,
    MAX(tie.created_at) AS last_event_at
FROM topic_interest_event tie
         JOIN forum_category fc ON fc.id = tie.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tie.subarea_id
GROUP BY fc.id, fc.code, fc.name, fs.id, fs.name;

CREATE VIEW vw_admin_topic_difficulty AS
SELECT
    fc.id AS category_id,
    COALESCE(fc.code, fc.id::text) AS category_code,
    fc.name AS category_name,
    fs.id AS subarea_id,
    fs.name AS subarea_name,
    COUNT(tde.id) AS total_reports,
    COALESCE(ROUND(AVG(tde.difficulty_level)::numeric, 2), 0) AS avg_difficulty,
    COUNT(DISTINCT tde.user_id) AS affected_students,
    MAX(tde.created_at) AS last_report_at
FROM topic_difficulty_event tde
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
GROUP BY fc.id, fc.code, fc.name, fs.id, fs.name;

CREATE VIEW vw_teacher_performance AS
SELECT
    u.id AS teacher_id,
    u.full_name,
    u.email_inst,
    COUNT(te.id) AS total_evaluations,
    COALESCE(ROUND(AVG(te.rating_clarity)::numeric, 2), 0) AS avg_clarity,
    COALESCE(ROUND(AVG(te.rating_knowledge)::numeric, 2), 0) AS avg_knowledge,
    COALESCE(ROUND(AVG(te.rating_support)::numeric, 2), 0) AS avg_support,
    COALESCE(ROUND(AVG(te.rating_punctuality)::numeric, 2), 0) AS avg_punctuality,
    COALESCE(ROUND((
                       AVG(te.rating_clarity)
                           + AVG(te.rating_knowledge)
                           + AVG(te.rating_support)
                           + AVG(te.rating_punctuality)
                       ) / 4.0::numeric, 2), 0) AS avg_global_score,
    0::bigint AS total_forum_posts,
    0::bigint AS total_forum_threads,
    0::bigint AS total_appointments_created,
    0::bigint AS completed_appointments,
    0::bigint AS total_video_meetings,
    0::bigint AS ended_video_meetings
FROM users u
         LEFT JOIN teacher_evaluation te ON te.teacher_id = u.id
GROUP BY u.id, u.full_name, u.email_inst;

CREATE VIEW vw_teacher_improvement_areas AS
SELECT
    tde.teacher_id,
    u.full_name AS teacher_name,
    fc.id AS category_id,
    COALESCE(fc.code, fc.id::text) AS category_code,
    fc.name AS category_name,
    fs.id AS subarea_id,
    fs.name AS subarea_name,
    COUNT(tde.id) AS total_difficulty_events,
    COALESCE(ROUND(AVG(tde.difficulty_level)::numeric, 2), 0) AS avg_difficulty,
    MAX(tde.created_at) AS last_event_at
FROM topic_difficulty_event tde
         JOIN users u ON u.id = tde.teacher_id
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
WHERE tde.teacher_id IS NOT NULL
GROUP BY tde.teacher_id, u.full_name, fc.id, fc.code, fc.name, fs.id, fs.name;