package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.TeacherEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnalyticsQueryRepo extends JpaRepository<TeacherEvaluation, UUID> {

    @Query(value = """
        SELECT
            category_id AS "categoryId",
            category_code AS "categoryCode",
            category_name AS "categoryName",
            subarea_id AS "subareaId",
            subarea_name AS "subareaName",
            total_events::bigint AS "totalEvents",
            weighted_score::bigint AS "weightedScore",
            unique_users::bigint AS "uniqueUsers",
            last_event_at AS "lastEventAt"
        FROM vw_admin_topic_interest
        ORDER BY weighted_score DESC, total_events DESC
        """, nativeQuery = true)
    List<AdminTopicInterestProjection> findAdminTopicInterest();

    @Query(value = """
        SELECT
            category_id AS "categoryId",
            category_code AS "categoryCode",
            category_name AS "categoryName",
            subarea_id AS "subareaId",
            subarea_name AS "subareaName",
            total_reports::bigint AS "totalReports",
            avg_difficulty::numeric AS "avgDifficulty",
            affected_students::bigint AS "affectedStudents",
            last_report_at AS "lastReportAt"
        FROM vw_admin_topic_difficulty
        ORDER BY avg_difficulty DESC, total_reports DESC
        """, nativeQuery = true)
    List<AdminTopicDifficultyProjection> findAdminTopicDifficulty();

    @Query(value = """
        SELECT
            teacher_id AS "teacherId",
            full_name AS "fullName",
            email_inst AS "emailInst",
            total_evaluations::bigint AS "totalEvaluations",
            avg_clarity::numeric AS "avgClarity",
            avg_knowledge::numeric AS "avgKnowledge",
            avg_support::numeric AS "avgSupport",
            avg_punctuality::numeric AS "avgPunctuality",
            avg_global_score::numeric AS "avgGlobalScore",
            total_forum_posts::bigint AS "totalForumPosts",
            total_forum_threads::bigint AS "totalForumThreads",
            total_appointments_created::bigint AS "totalAppointmentsCreated",
            completed_appointments::bigint AS "completedAppointments",
            total_video_meetings::bigint AS "totalVideoMeetings",
            ended_video_meetings::bigint AS "endedVideoMeetings"
        FROM vw_teacher_performance
        ORDER BY avg_global_score DESC, total_evaluations DESC
        """, nativeQuery = true)
    List<TeacherPerformanceProjection> findTeacherPerformance();

    @Query(value = """
        SELECT
            teacher_id AS "teacherId",
            full_name AS "fullName",
            email_inst AS "emailInst",
            total_evaluations::bigint AS "totalEvaluations",
            avg_clarity::numeric AS "avgClarity",
            avg_knowledge::numeric AS "avgKnowledge",
            avg_support::numeric AS "avgSupport",
            avg_punctuality::numeric AS "avgPunctuality",
            avg_global_score::numeric AS "avgGlobalScore",
            total_forum_posts::bigint AS "totalForumPosts",
            total_forum_threads::bigint AS "totalForumThreads",
            total_appointments_created::bigint AS "totalAppointmentsCreated",
            completed_appointments::bigint AS "completedAppointments",
            total_video_meetings::bigint AS "totalVideoMeetings",
            ended_video_meetings::bigint AS "endedVideoMeetings"
        FROM vw_teacher_performance
        WHERE teacher_id = :teacherId
        """, nativeQuery = true)
    TeacherPerformanceProjection findTeacherPerformanceByTeacherId(@Param("teacherId") UUID teacherId);

    @Query(value = """
        SELECT
            teacher_id AS "teacherId",
            teacher_name AS "teacherName",
            category_id AS "categoryId",
            category_code AS "categoryCode",
            category_name AS "categoryName",
            subarea_id AS "subareaId",
            subarea_name AS "subareaName",
            total_difficulty_events::bigint AS "totalDifficultyEvents",
            avg_difficulty::numeric AS "avgDifficulty",
            last_event_at AS "lastEventAt"
        FROM vw_teacher_improvement_areas
        ORDER BY avg_difficulty DESC, total_difficulty_events DESC
        """, nativeQuery = true)
    List<TeacherImprovementAreaProjection> findTeacherImprovementAreas();

    @Query(value = """
        SELECT
            teacher_id AS "teacherId",
            teacher_name AS "teacherName",
            category_id AS "categoryId",
            category_code AS "categoryCode",
            category_name AS "categoryName",
            subarea_id AS "subareaId",
            subarea_name AS "subareaName",
            total_difficulty_events::bigint AS "totalDifficultyEvents",
            avg_difficulty::numeric AS "avgDifficulty",
            last_event_at AS "lastEventAt"
        FROM vw_teacher_improvement_areas
        WHERE teacher_id = :teacherId
        ORDER BY avg_difficulty DESC, total_difficulty_events DESC
        """, nativeQuery = true)
    List<TeacherImprovementAreaProjection> findTeacherImprovementAreasByTeacherId(@Param("teacherId") UUID teacherId);
}