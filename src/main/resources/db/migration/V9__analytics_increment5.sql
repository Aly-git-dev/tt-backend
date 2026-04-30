-- ============================================================================
-- V7__analytics_increment5.sql
-- Incremento 5: Analítica general, desempeño docente y áreas de mejora
-- Requiere:
--   V1 users / roles
--   V2 forums
--   V6 agenda UUID
--   video_meetings
-- Objetivo:
--   1) Registrar evaluaciones docentes
--   2) Registrar eventos de interés por tema
--   3) Registrar eventos de dificultad por tema
--   4) Exponer vistas agregadas para dashboard admin/profesor
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Evaluación docente
--    Base directa para UC-24 y parte de UC-23
-- ----------------------------------------------------------------------------
CREATE TABLE teacher_evaluation (
                                    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    teacher_id          UUID NOT NULL
                                        REFERENCES users(id) ON DELETE CASCADE,

                                    evaluator_id        UUID
                                                             REFERENCES users(id) ON DELETE SET NULL,

                                    appointment_id      UUID
                                                             REFERENCES appointments(id) ON DELETE SET NULL,

    -- escala 1..5
                                    rating_clarity      SMALLINT NOT NULL,
                                    rating_knowledge    SMALLINT NOT NULL,
                                    rating_support      SMALLINT NOT NULL,
                                    rating_punctuality  SMALLINT NOT NULL,

                                    comment             TEXT,
                                    is_anonymous        BOOLEAN NOT NULL DEFAULT FALSE,

                                    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                    CONSTRAINT ck_teacher_eval_clarity
                                        CHECK (rating_clarity BETWEEN 1 AND 5),

                                    CONSTRAINT ck_teacher_eval_knowledge
                                        CHECK (rating_knowledge BETWEEN 1 AND 5),

                                    CONSTRAINT ck_teacher_eval_support
                                        CHECK (rating_support BETWEEN 1 AND 5),

                                    CONSTRAINT ck_teacher_eval_punctuality
                                        CHECK (rating_punctuality BETWEEN 1 AND 5)
);

CREATE INDEX idx_teacher_evaluation_teacher
    ON teacher_evaluation(teacher_id);

CREATE INDEX idx_teacher_evaluation_evaluator
    ON teacher_evaluation(evaluator_id);

CREATE INDEX idx_teacher_evaluation_appointment
    ON teacher_evaluation(appointment_id);

CREATE INDEX idx_teacher_evaluation_created
    ON teacher_evaluation(created_at DESC);

-- Evita que un mismo evaluador califique dos veces al mismo profesor
-- dentro de la misma cita (cuando aplica)
CREATE UNIQUE INDEX ux_teacher_eval_evaluator_teacher_appointment
    ON teacher_evaluation(evaluator_id, teacher_id, appointment_id)
    WHERE evaluator_id IS NOT NULL AND appointment_id IS NOT NULL;

DROP TRIGGER IF EXISTS set_timestamp_teacher_evaluation ON teacher_evaluation;
CREATE TRIGGER set_timestamp_teacher_evaluation
    BEFORE UPDATE ON teacher_evaluation
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();


-- ----------------------------------------------------------------------------
-- 2) Evento de interés por tema
--    Base para temas de interés / popularidad
--    Reutiliza forum_category y forum_subarea como dimensión académica
-- ----------------------------------------------------------------------------
CREATE TABLE topic_interest_event (
                                      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      user_id             UUID NOT NULL
                                          REFERENCES users(id) ON DELETE CASCADE,

                                      category_id         BIGINT NOT NULL
                                          REFERENCES forum_category(id) ON DELETE CASCADE,

                                      subarea_id          BIGINT
                                          REFERENCES forum_subarea(id) ON DELETE SET NULL,

                                      thread_id           BIGINT
                                          REFERENCES forum_thread(id) ON DELETE SET NULL,

                                      appointment_id      UUID
                                          REFERENCES appointments(id) ON DELETE SET NULL,

                                      video_meeting_id    UUID
                                          REFERENCES video_meetings(id) ON DELETE SET NULL,

                                      source_type         VARCHAR(30) NOT NULL,
                                      weight              INTEGER NOT NULL DEFAULT 1,

                                      created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                      CONSTRAINT ck_topic_interest_source_type
                                          CHECK (
                                              source_type IN (
                                                              'THREAD_VIEW',
                                                              'THREAD_CREATE',
                                                              'POST_CREATE',
                                                              'APPOINTMENT_JOIN',
                                                              'VIDEO_JOIN',
                                                              'MANUAL'
                                                  )
                                              ),

                                      CONSTRAINT ck_topic_interest_weight
                                          CHECK (weight > 0)
);

CREATE INDEX idx_topic_interest_user
    ON topic_interest_event(user_id);

CREATE INDEX idx_topic_interest_category
    ON topic_interest_event(category_id);

CREATE INDEX idx_topic_interest_subarea
    ON topic_interest_event(subarea_id);

CREATE INDEX idx_topic_interest_created
    ON topic_interest_event(created_at DESC);

CREATE INDEX idx_topic_interest_source
    ON topic_interest_event(source_type);


-- ----------------------------------------------------------------------------
-- 3) Evento de dificultad por tema
--    Base para UC-25: identificar áreas/temas con mayor dificultad
-- ----------------------------------------------------------------------------
CREATE TABLE topic_difficulty_event (
                                        id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                        user_id             UUID
                                                                        REFERENCES users(id) ON DELETE SET NULL,

                                        teacher_id          UUID
                                                                        REFERENCES users(id) ON DELETE SET NULL,

                                        category_id         BIGINT NOT NULL
                                            REFERENCES forum_category(id) ON DELETE CASCADE,

                                        subarea_id          BIGINT
                                                                        REFERENCES forum_subarea(id) ON DELETE SET NULL,

                                        thread_id           BIGINT
                                                                        REFERENCES forum_thread(id) ON DELETE SET NULL,

                                        appointment_id      UUID
                                                                        REFERENCES appointments(id) ON DELETE SET NULL,

                                        video_meeting_id    UUID
                                                                        REFERENCES video_meetings(id) ON DELETE SET NULL,

                                        source_type         VARCHAR(30) NOT NULL,
                                        difficulty_level    SMALLINT NOT NULL,
                                        notes               TEXT,

                                        created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                        CONSTRAINT ck_topic_difficulty_source_type
                                            CHECK (
                                                source_type IN (
                                                                'SELF_REPORT',
                                                                'FORUM_QUESTION',
                                                                'LOW_PERFORMANCE',
                                                                'TEACHER_OBSERVATION',
                                                                'MANUAL'
                                                    )
                                                ),

                                        CONSTRAINT ck_topic_difficulty_level
                                            CHECK (difficulty_level BETWEEN 1 AND 5)
);

CREATE INDEX idx_topic_difficulty_user
    ON topic_difficulty_event(user_id);

CREATE INDEX idx_topic_difficulty_teacher
    ON topic_difficulty_event(teacher_id);

CREATE INDEX idx_topic_difficulty_category
    ON topic_difficulty_event(category_id);

CREATE INDEX idx_topic_difficulty_subarea
    ON topic_difficulty_event(subarea_id);

CREATE INDEX idx_topic_difficulty_created
    ON topic_difficulty_event(created_at DESC);

CREATE INDEX idx_topic_difficulty_source
    ON topic_difficulty_event(source_type);


-- ----------------------------------------------------------------------------
-- 4) Tabla opcional para refresco / auditoría de generación de analítica
--    Útil si luego haces jobs programados o caché de dashboard
-- ----------------------------------------------------------------------------
CREATE TABLE analytics_refresh_log (
                                       id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       process_name        VARCHAR(80) NOT NULL,
                                       status              VARCHAR(20) NOT NULL,
                                       details             TEXT,
                                       started_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                       finished_at         TIMESTAMP WITHOUT TIME ZONE,

                                       CONSTRAINT ck_analytics_refresh_status
                                           CHECK (status IN ('STARTED', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_analytics_refresh_log_process
    ON analytics_refresh_log(process_name);

CREATE INDEX idx_analytics_refresh_log_started
    ON analytics_refresh_log(started_at DESC);


-- ----------------------------------------------------------------------------
-- 5) Validaciones auxiliares
-- ----------------------------------------------------------------------------

-- El teacher_id de teacher_evaluation debe pertenecer al rol PROFESOR
CREATE OR REPLACE FUNCTION trg_validate_teacher_evaluation_teacher()
RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id
        WHERE ur.user_id = NEW.teacher_id
          AND r.name = 'PROFESOR'
    ) THEN
        RAISE EXCEPTION 'teacher_id debe pertenecer a un usuario con rol PROFESOR';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS validate_teacher_evaluation_teacher ON teacher_evaluation;
CREATE TRIGGER validate_teacher_evaluation_teacher
    BEFORE INSERT OR UPDATE ON teacher_evaluation
                         FOR EACH ROW EXECUTE FUNCTION trg_validate_teacher_evaluation_teacher();


-- Si se manda subarea, debe corresponder a la categoría enviada
CREATE OR REPLACE FUNCTION trg_validate_topic_event_subarea()
RETURNS trigger AS $$
DECLARE
v_category_id BIGINT;
BEGIN
    IF NEW.subarea_id IS NOT NULL THEN
SELECT category_id
INTO v_category_id
FROM forum_subarea
WHERE id = NEW.subarea_id;

IF v_category_id IS NULL THEN
            RAISE EXCEPTION 'La subárea indicada no existe';
END IF;

        IF v_category_id <> NEW.category_id THEN
            RAISE EXCEPTION 'La subárea no pertenece a la categoría indicada';
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS validate_topic_interest_subarea ON topic_interest_event;
CREATE TRIGGER validate_topic_interest_subarea
    BEFORE INSERT OR UPDATE ON topic_interest_event
                         FOR EACH ROW EXECUTE FUNCTION trg_validate_topic_event_subarea();

DROP TRIGGER IF EXISTS validate_topic_difficulty_subarea ON topic_difficulty_event;
CREATE TRIGGER validate_topic_difficulty_subarea
    BEFORE INSERT OR UPDATE ON topic_difficulty_event
                         FOR EACH ROW EXECUTE FUNCTION trg_validate_topic_event_subarea();


-- ----------------------------------------------------------------------------
-- 6) Vistas para dashboard ADMIN
-- ----------------------------------------------------------------------------

-- Temas de interés más fuertes
CREATE OR REPLACE VIEW vw_admin_topic_interest AS
SELECT
    fc.id                  AS category_id,
    fc.code                AS category_code,
    fc.name                AS category_name,
    fs.id                  AS subarea_id,
    fs.name                AS subarea_name,
    COUNT(tie.id)          AS total_events,
    COALESCE(SUM(tie.weight), 0) AS weighted_score,
    COUNT(DISTINCT tie.user_id)  AS unique_users,
    MAX(tie.created_at)    AS last_event_at
FROM topic_interest_event tie
         JOIN forum_category fc ON fc.id = tie.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tie.subarea_id
GROUP BY
    fc.id, fc.code, fc.name,
    fs.id, fs.name;

-- Temas con mayor dificultad
CREATE OR REPLACE VIEW vw_admin_topic_difficulty AS
SELECT
    fc.id                               AS category_id,
    fc.code                             AS category_code,
    fc.name                             AS category_name,
    fs.id                               AS subarea_id,
    fs.name                             AS subarea_name,
    COUNT(tde.id)                       AS total_reports,
    ROUND(AVG(tde.difficulty_level)::numeric, 2) AS avg_difficulty,
    COUNT(DISTINCT tde.user_id)         AS affected_students,
    MAX(tde.created_at)                 AS last_report_at
FROM topic_difficulty_event tde
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
GROUP BY
    fc.id, fc.code, fc.name,
    fs.id, fs.name;

-- Desempeño docente agregado
CREATE OR REPLACE VIEW vw_teacher_performance AS
WITH teacher_base AS (
    SELECT DISTINCT
        u.id,
        u.full_name,
        u.email_inst
    FROM users u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'PROFESOR'
),
evaluation_stats AS (
    SELECT
        te.teacher_id,
        COUNT(*) AS total_evaluations,
        ROUND(AVG(te.rating_clarity)::numeric, 2)     AS avg_clarity,
        ROUND(AVG(te.rating_knowledge)::numeric, 2)   AS avg_knowledge,
        ROUND(AVG(te.rating_support)::numeric, 2)     AS avg_support,
        ROUND(AVG(te.rating_punctuality)::numeric, 2) AS avg_punctuality,
        ROUND((
            AVG(te.rating_clarity)
          + AVG(te.rating_knowledge)
          + AVG(te.rating_support)
          + AVG(te.rating_punctuality)
        ) / 4.0::numeric, 2) AS avg_global_score
    FROM teacher_evaluation te
    GROUP BY te.teacher_id
),
forum_stats AS (
    SELECT
        fp.author_id AS teacher_id,
        COUNT(*) AS total_forum_posts
    FROM forum_post fp
    GROUP BY fp.author_id
),
thread_stats AS (
    SELECT
        ft.author_id AS teacher_id,
        COUNT(*) AS total_forum_threads
    FROM forum_thread ft
    GROUP BY ft.author_id
),
appointment_stats AS (
    SELECT
        a.created_by AS teacher_id,
        COUNT(*) AS total_appointments_created,
        COUNT(*) FILTER (WHERE a.status = 'COMPLETED') AS completed_appointments
    FROM appointments a
    GROUP BY a.created_by
),
meeting_stats AS (
    SELECT
        vm.host_user_id AS teacher_id,
        COUNT(*) AS total_video_meetings,
        COUNT(*) FILTER (WHERE vm.status = 'ENDED') AS ended_video_meetings
    FROM video_meetings vm
    GROUP BY vm.host_user_id
)
SELECT
    tb.id AS teacher_id,
    tb.full_name,
    tb.email_inst,

    COALESCE(es.total_evaluations, 0)          AS total_evaluations,
    COALESCE(es.avg_clarity, 0)                AS avg_clarity,
    COALESCE(es.avg_knowledge, 0)              AS avg_knowledge,
    COALESCE(es.avg_support, 0)                AS avg_support,
    COALESCE(es.avg_punctuality, 0)            AS avg_punctuality,
    COALESCE(es.avg_global_score, 0)           AS avg_global_score,

    COALESCE(fs.total_forum_posts, 0)          AS total_forum_posts,
    COALESCE(ts.total_forum_threads, 0)        AS total_forum_threads,
    COALESCE(ap.total_appointments_created, 0) AS total_appointments_created,
    COALESCE(ap.completed_appointments, 0)     AS completed_appointments,
    COALESCE(ms.total_video_meetings, 0)       AS total_video_meetings,
    COALESCE(ms.ended_video_meetings, 0)       AS ended_video_meetings
FROM teacher_base tb
         LEFT JOIN evaluation_stats es ON es.teacher_id = tb.id
         LEFT JOIN forum_stats fs      ON fs.teacher_id = tb.id
         LEFT JOIN thread_stats ts     ON ts.teacher_id = tb.id
         LEFT JOIN appointment_stats ap ON ap.teacher_id = tb.id
         LEFT JOIN meeting_stats ms     ON ms.teacher_id = tb.id;


-- ----------------------------------------------------------------------------
-- 7) Vista para áreas de mejora por profesor
--    Relaciona eventos de dificultad asociados a teacher_id
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_teacher_improvement_areas AS
SELECT
    tde.teacher_id,
    u.full_name AS teacher_name,
    fc.id       AS category_id,
    fc.code     AS category_code,
    fc.name     AS category_name,
    fs.id       AS subarea_id,
    fs.name     AS subarea_name,
    COUNT(tde.id) AS total_difficulty_events,
    ROUND(AVG(tde.difficulty_level)::numeric, 2) AS avg_difficulty,
    MAX(tde.created_at) AS last_event_at
FROM topic_difficulty_event tde
         JOIN users u ON u.id = tde.teacher_id
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
WHERE tde.teacher_id IS NOT NULL
GROUP BY
    tde.teacher_id,
    u.full_name,
    fc.id, fc.code, fc.name,
    fs.id, fs.name;


-- ----------------------------------------------------------------------------
-- 8) Semillas opcionales mínimas para EVALDOC como soporte analítico
-- ----------------------------------------------------------------------------
-- Ya existe EVALDOC en forum_category desde V2. Aquí no insertamos más seeds
-- para evitar duplicados o alterar tu catálogo actual.

-- ============================================================================
-- FIN V7__analytics_increment5.sql
-- ============================================================================