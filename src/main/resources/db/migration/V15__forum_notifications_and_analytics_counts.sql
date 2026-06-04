-- Allow notification targets to reference UUID-based modules and numeric forum ids.
ALTER TABLE notifications
    ALTER COLUMN target_id TYPE VARCHAR(80)
    USING target_id::text;

DROP VIEW IF EXISTS vw_teacher_performance CASCADE;

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
    GROUP BY te.teacher_id
),
forum_post_stats AS (
    SELECT
        fp.author_id AS teacher_id,
        COUNT(fp.id)::bigint AS total_forum_posts
    FROM forum_post fp
    GROUP BY fp.author_id
),
forum_thread_stats AS (
    SELECT
        ft.author_id AS teacher_id,
        COUNT(ft.id)::bigint AS total_forum_threads
    FROM forum_thread ft
    GROUP BY ft.author_id
),
appointment_participation AS (
    SELECT
        ap.user_id AS teacher_id,
        ap.appointment_id
    FROM appointment_participants ap
    UNION
    SELECT
        a.created_by AS teacher_id,
        a.id AS appointment_id
    FROM appointments a
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
    UNION
    SELECT
        ap.user_id AS teacher_id,
        vm.id AS video_meeting_id
    FROM video_meetings vm
             JOIN appointment_participants ap ON ap.appointment_id = vm.appointment_id
    UNION
    SELECT
        vma.user_id AS teacher_id,
        vma.video_meeting_id
    FROM video_meeting_attendance vma
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
