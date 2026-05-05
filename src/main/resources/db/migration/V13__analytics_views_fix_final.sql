-- V10__analytics_views_align.sql

DROP VIEW IF EXISTS vw_teacher_improvement_areas CASCADE;
DROP VIEW IF EXISTS vw_teacher_performance CASCADE;
DROP VIEW IF EXISTS vw_admin_topic_difficulty CASCADE;
DROP VIEW IF EXISTS vw_admin_topic_interest CASCADE;

CREATE VIEW vw_admin_topic_interest AS
SELECT
    fc.id AS category_id,
    COALESCE(fc.code, fc.id::text) AS category_code,
    fc.name AS category_name,
    fs.id AS subarea_id,
    fs.name AS subarea_name,
    COUNT(tie.id)::bigint AS total_events,
    COALESCE(SUM(tie.weight), 0)::bigint AS weighted_score,
    COUNT(DISTINCT tie.user_id)::bigint AS unique_users,
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
    COUNT(tde.id)::bigint AS total_reports,
    COALESCE(ROUND(AVG(tde.difficulty_level)::numeric, 2), 0)::numeric AS avg_difficulty,
    COUNT(DISTINCT tde.user_id)::bigint AS affected_students,
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

    COUNT(te.id)::bigint AS total_evaluations,

    COALESCE(ROUND(AVG(te.rating_clarity)::numeric, 2), 0)::numeric AS avg_clarity,
    COALESCE(ROUND(AVG(te.rating_knowledge)::numeric, 2), 0)::numeric AS avg_knowledge,
    COALESCE(ROUND(AVG(te.rating_support)::numeric, 2), 0)::numeric AS avg_support,
    COALESCE(ROUND(AVG(te.rating_punctuality)::numeric, 2), 0)::numeric AS avg_punctuality,

    COALESCE(
            ROUND((
                      AVG(te.rating_clarity)
                          + AVG(te.rating_knowledge)
                          + AVG(te.rating_support)
                          + AVG(te.rating_punctuality)
                      ) / 4.0::numeric, 2),
            0
    )::numeric AS avg_global_score,

    COUNT(DISTINCT fp.id)::bigint AS total_forum_posts,
    COUNT(DISTINCT ft.id)::bigint AS total_forum_threads,
    COUNT(DISTINCT a.id)::bigint AS total_appointments_created,
    COUNT(DISTINCT CASE WHEN a.status = 'COMPLETED' THEN a.id END)::bigint AS completed_appointments,
    COUNT(DISTINCT vm.id)::bigint AS total_video_meetings,
    COUNT(DISTINCT CASE WHEN vm.status = 'ENDED' THEN vm.id END)::bigint AS ended_video_meetings

FROM users u
         LEFT JOIN user_roles ur ON ur.user_id = u.id
         LEFT JOIN roles r ON r.id = ur.role_id

         LEFT JOIN teacher_evaluation te ON te.teacher_id = u.id
         LEFT JOIN forum_post fp ON fp.author_id = u.id
         LEFT JOIN forum_thread ft ON ft.author_id = u.id
         LEFT JOIN appointments a ON a.created_by = u.id
         LEFT JOIN video_meetings vm ON vm.host_user_id = u.id

WHERE r.name = 'PROFESOR'
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
    COUNT(tde.id)::bigint AS total_difficulty_events,
    COALESCE(ROUND(AVG(tde.difficulty_level)::numeric, 2), 0)::numeric AS avg_difficulty,
    MAX(tde.created_at) AS last_event_at
FROM topic_difficulty_event tde
         JOIN users u ON u.id = tde.teacher_id
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
WHERE tde.teacher_id IS NOT NULL
GROUP BY tde.teacher_id, u.full_name, fc.id, fc.code, fc.name, fs.id, fs.name;