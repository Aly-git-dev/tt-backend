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
            category_id,
            category_name,
            category_code,
            subarea_id,
            subarea_name,
            total_events,
            weighted_score,
            unique_users,
            last_event_at
        FROM vw_admin_topic_interest
        ORDER BY weighted_score DESC, total_events DESC
        """, nativeQuery = true)
    List<AdminTopicInterestProjection> findAdminTopicInterest();

    @Query(value = """
        SELECT
            category_id,
            category_name,
            category_code,
            subarea_id,
            subarea_name,
            total_reports,
            avg_difficulty,
            affected_students,
            last_report_at
        FROM vw_admin_topic_difficulty
        ORDER BY avg_difficulty DESC, total_reports DESC
        """, nativeQuery = true)
    List<AdminTopicDifficultyProjection> findAdminTopicDifficulty();

    @Query(value = """
        SELECT
            teacher_id,
            full_name,
            email_inst,
            total_evaluations,
            avg_clarity,
            avg_knowledge,
            avg_support,
            avg_punctuality,
            avg_global_score,
            total_forum_posts,
            total_forum_threads,
            total_appointments_created,
            completed_appointments,
            total_video_meetings,
            ended_video_meetings
        FROM vw_teacher_performance
        ORDER BY avg_global_score DESC, total_evaluations DESC
        """, nativeQuery = true)
    List<TeacherPerformanceProjection> findTeacherPerformance();

    @Query(value = """
        SELECT
            teacher_id,
            full_name,
            email_inst,
            total_evaluations,
            avg_clarity,
            avg_knowledge,
            avg_support,
            avg_punctuality,
            avg_global_score,
            total_forum_posts,
            total_forum_threads,
            total_appointments_created,
            completed_appointments,
            total_video_meetings,
            ended_video_meetings
        FROM vw_teacher_performance
        WHERE teacher_id = :teacherId
        """, nativeQuery = true)
    TeacherPerformanceProjection findTeacherPerformanceByTeacherId(@Param("teacherId") UUID teacherId);

    @Query(value = """
        SELECT
            teacher_id,
            teacher_name,
            category_id,
            category_name,
            category_code,
            subarea_id,
            subarea_name,
            total_difficulty_events,
            avg_difficulty,
            last_event_at
        FROM vw_teacher_improvement_areas
        ORDER BY avg_difficulty DESC, total_difficulty_events DESC
        """, nativeQuery = true)
    List<TeacherImprovementAreaProjection> findTeacherImprovementAreas();

    @Query(value = """
        SELECT
            teacher_id,
            teacher_name,
            category_id,
            category_name,
            category_code,
            subarea_id,
            subarea_name,
            total_difficulty_events,
            avg_difficulty,
            last_event_at
        FROM vw_teacher_improvement_areas
        WHERE teacher_id = :teacherId
        ORDER BY avg_difficulty DESC, total_difficulty_events DESC
        """, nativeQuery = true)
    List<TeacherImprovementAreaProjection> findTeacherImprovementAreasByTeacherId(@Param("teacherId") UUID teacherId);
}