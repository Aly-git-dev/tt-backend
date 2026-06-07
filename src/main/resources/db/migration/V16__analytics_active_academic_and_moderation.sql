DROP VIEW IF EXISTS vw_analytics_moderation_summary CASCADE;
DROP VIEW IF EXISTS vw_teacher_improvement_areas CASCADE;
DROP VIEW IF EXISTS vw_teacher_performance CASCADE;
DROP VIEW IF EXISTS vw_admin_topic_difficulty CASCADE;
DROP VIEW IF EXISTS vw_admin_topic_interest CASCADE;

CREATE VIEW vw_admin_topic_interest AS
WITH filtered_interest_events AS (
    SELECT tie.*
    FROM topic_interest_event tie
             LEFT JOIN users event_user ON event_user.id = tie.user_id
             LEFT JOIN forum_thread event_thread ON event_thread.id = tie.thread_id
             LEFT JOIN users thread_author ON thread_author.id = event_thread.author_id
    WHERE (tie.user_id IS NULL OR event_user.active = TRUE)
      AND (
        tie.thread_id IS NULL
            OR (event_thread.status = 'ABIERTO' AND thread_author.active = TRUE)
        )
)
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
FROM filtered_interest_events tie
         JOIN forum_category fc ON fc.id = tie.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tie.subarea_id
GROUP BY fc.id, fc.code, fc.name, fs.id, fs.name;

CREATE VIEW vw_admin_topic_difficulty AS
WITH filtered_difficulty_events AS (
    SELECT tde.*
    FROM topic_difficulty_event tde
             LEFT JOIN users event_user ON event_user.id = tde.user_id
             LEFT JOIN users teacher_user ON teacher_user.id = tde.teacher_id
             LEFT JOIN forum_thread event_thread ON event_thread.id = tde.thread_id
             LEFT JOIN users thread_author ON thread_author.id = event_thread.author_id
    WHERE (tde.user_id IS NULL OR event_user.active = TRUE)
      AND (tde.teacher_id IS NULL OR teacher_user.active = TRUE)
      AND (
        tde.thread_id IS NULL
            OR (event_thread.status = 'ABIERTO' AND thread_author.active = TRUE)
        )
)
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
FROM filtered_difficulty_events tde
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
GROUP BY fc.id, fc.code, fc.name, fs.id, fs.name;

CREATE VIEW vw_teacher_performance AS
WITH teacher_users AS (
    SELECT DISTINCT
        u.id,
        u.full_name,
        u.email_inst
    FROM users u
             JOIN user_roles ur ON ur.user_id = u.id
             JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'PROFESOR'
      AND u.active = TRUE
),
evaluation_stats AS (
    SELECT
        te.teacher_id,
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
        )::numeric AS avg_global_score
    FROM teacher_evaluation te
             JOIN teacher_users tu ON tu.id = te.teacher_id
             LEFT JOIN users evaluator_user ON evaluator_user.id = te.evaluator_id
    WHERE te.evaluator_id IS NULL OR evaluator_user.active = TRUE
    GROUP BY te.teacher_id
),
forum_post_stats AS (
    SELECT
        fp.author_id AS teacher_id,
        COUNT(fp.id)::bigint AS total_forum_posts
    FROM forum_post fp
             JOIN teacher_users tu ON tu.id = fp.author_id
             JOIN forum_thread ft ON ft.id = fp.thread_id
    WHERE fp.status = 'VISIBLE'
      AND ft.status = 'ABIERTO'
    GROUP BY fp.author_id
),
forum_thread_stats AS (
    SELECT
        ft.author_id AS teacher_id,
        COUNT(ft.id)::bigint AS total_forum_threads
    FROM forum_thread ft
             JOIN teacher_users tu ON tu.id = ft.author_id
    WHERE ft.status = 'ABIERTO'
    GROUP BY ft.author_id
),
appointment_participation AS (
    SELECT
        ap.user_id AS teacher_id,
        ap.appointment_id
    FROM appointment_participants ap
             JOIN teacher_users tu ON tu.id = ap.user_id
    UNION
    SELECT
        a.created_by AS teacher_id,
        a.id AS appointment_id
    FROM appointments a
             JOIN teacher_users tu ON tu.id = a.created_by
),
appointment_stats AS (
    SELECT
        ap.teacher_id,
        COUNT(DISTINCT a.id)::bigint AS total_appointments_created,
        COUNT(DISTINCT CASE WHEN a.status = 'COMPLETED' THEN a.id END)::bigint AS completed_appointments
    FROM appointment_participation ap
             JOIN appointments a ON a.id = ap.appointment_id
    GROUP BY ap.teacher_id
),
video_meeting_participation AS (
    SELECT
        vm.host_user_id AS teacher_id,
        vm.id AS video_meeting_id
    FROM video_meetings vm
             JOIN teacher_users tu ON tu.id = vm.host_user_id
    UNION
    SELECT
        ap.user_id AS teacher_id,
        vm.id AS video_meeting_id
    FROM video_meetings vm
             JOIN appointment_participants ap ON ap.appointment_id = vm.appointment_id
             JOIN teacher_users tu ON tu.id = ap.user_id
    UNION
    SELECT
        vma.user_id AS teacher_id,
        vma.video_meeting_id
    FROM video_meeting_attendance vma
             JOIN teacher_users tu ON tu.id = vma.user_id
),
video_meeting_stats AS (
    SELECT
        vmp.teacher_id,
        COUNT(DISTINCT vm.id)::bigint AS total_video_meetings,
        COUNT(DISTINCT CASE WHEN vm.status = 'ENDED' THEN vm.id END)::bigint AS ended_video_meetings
    FROM video_meeting_participation vmp
             JOIN video_meetings vm ON vm.id = vmp.video_meeting_id
    GROUP BY vmp.teacher_id
)
SELECT
    u.id AS teacher_id,
    u.full_name,
    u.email_inst,

    COALESCE(es.total_evaluations, 0)::bigint AS total_evaluations,
    COALESCE(es.avg_clarity, 0)::numeric AS avg_clarity,
    COALESCE(es.avg_knowledge, 0)::numeric AS avg_knowledge,
    COALESCE(es.avg_support, 0)::numeric AS avg_support,
    COALESCE(es.avg_punctuality, 0)::numeric AS avg_punctuality,
    COALESCE(es.avg_global_score, 0)::numeric AS avg_global_score,

    COALESCE(fps.total_forum_posts, 0)::bigint AS total_forum_posts,
    COALESCE(fts.total_forum_threads, 0)::bigint AS total_forum_threads,
    COALESCE(aps.total_appointments_created, 0)::bigint AS total_appointments_created,
    COALESCE(aps.completed_appointments, 0)::bigint AS completed_appointments,
    COALESCE(vms.total_video_meetings, 0)::bigint AS total_video_meetings,
    COALESCE(vms.ended_video_meetings, 0)::bigint AS ended_video_meetings
FROM teacher_users u
         LEFT JOIN evaluation_stats es ON es.teacher_id = u.id
         LEFT JOIN forum_post_stats fps ON fps.teacher_id = u.id
         LEFT JOIN forum_thread_stats fts ON fts.teacher_id = u.id
         LEFT JOIN appointment_stats aps ON aps.teacher_id = u.id
         LEFT JOIN video_meeting_stats vms ON vms.teacher_id = u.id;

CREATE VIEW vw_teacher_improvement_areas AS
WITH filtered_difficulty_events AS (
    SELECT tde.*
    FROM topic_difficulty_event tde
             LEFT JOIN users event_user ON event_user.id = tde.user_id
             LEFT JOIN forum_thread event_thread ON event_thread.id = tde.thread_id
             LEFT JOIN users thread_author ON thread_author.id = event_thread.author_id
    WHERE (tde.user_id IS NULL OR event_user.active = TRUE)
      AND (
        tde.thread_id IS NULL
            OR (event_thread.status = 'ABIERTO' AND thread_author.active = TRUE)
        )
)
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
FROM filtered_difficulty_events tde
         JOIN users u ON u.id = tde.teacher_id
         JOIN forum_category fc ON fc.id = tde.category_id
         LEFT JOIN forum_subarea fs ON fs.id = tde.subarea_id
WHERE tde.teacher_id IS NOT NULL
  AND u.active = TRUE
GROUP BY tde.teacher_id, u.full_name, fc.id, fc.code, fc.name, fs.id, fs.name;

CREATE VIEW vw_analytics_moderation_summary AS
SELECT
    (SELECT COUNT(*) FROM users WHERE active = FALSE)::bigint AS banned_users,
    (SELECT COUNT(*) FROM users WHERE active = TRUE)::bigint AS active_users,
    (SELECT COUNT(*) FROM forum_report WHERE status = 'RESUELTO')::bigint AS resolved_forum_reports,
    (SELECT COUNT(*) FROM forum_report WHERE status IN ('DESCARTADO', 'DESESTIMADO'))::bigint AS dismissed_forum_reports,
    (SELECT COUNT(*) FROM chat_message_report WHERE status = 'RESUELTO')::bigint AS resolved_message_reports,
    (SELECT COUNT(*) FROM chat_message_report WHERE status IN ('DESESTIMADO', 'DESCARTADO'))::bigint AS dismissed_message_reports;
