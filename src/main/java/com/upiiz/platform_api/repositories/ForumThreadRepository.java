package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.ForumThread;
import com.upiiz.platform_api.models.ForumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {

    List<ForumThread> findTop5ByStatusOrderByScoreDescCreatedAtDesc(ForumStatus status);

    List<ForumThread> findByCategoryId(Long categoryId);

    List<ForumThread> findByStatusOrderByCreatedAtDesc(ForumStatus status);

    long countByAuthorId(UUID authorId);

    @Query(value = """
        WITH explicit_interests AS (
            SELECT
                uit.category_id,
                uit.subarea_id,
                SUM(uit.weight)::numeric AS weight
            FROM user_interest_tag uit
            WHERE uit.user_id = :userId
            GROUP BY uit.category_id, uit.subarea_id
        ),
        behavioral_interests AS (
            SELECT
                tie.category_id,
                tie.subarea_id,
                SUM(tie.weight)::numeric AS weight
            FROM topic_interest_event tie
            WHERE tie.user_id = :userId
            GROUP BY tie.category_id, tie.subarea_id
        ),
        user_interests AS (
            SELECT
                category_id,
                subarea_id,
                SUM(weight)::numeric AS weight
            FROM (
                SELECT category_id, subarea_id, weight FROM explicit_interests
                UNION ALL
                SELECT category_id, subarea_id, weight FROM behavioral_interests
            ) x
            GROUP BY category_id, subarea_id
        ),
        thread_base AS (
            SELECT
                ft.id AS thread_id,
                ft.category_id,
                ft.subarea_id,
                ft.score,
                ft.answers_count,
                ft.views,
                ft.created_at,
                ft.author_id,
                COALESCE(SUM(ui.weight), 0)::numeric AS interest_weight,
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM user_roles ur
                        JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id = ft.author_id
                          AND r.name IN ('PROFESOR', 'ASESOR')
                    )
                    THEN 1
                    ELSE 0
                END AS academic_author
            FROM forum_thread ft
            LEFT JOIN user_interests ui
                ON ui.category_id = ft.category_id
               AND (
                    ui.subarea_id IS NULL
                    OR ft.subarea_id IS NULL
                    OR ui.subarea_id = ft.subarea_id
               )
            WHERE ft.status = 'ABIERTO'
            GROUP BY
                ft.id,
                ft.category_id,
                ft.subarea_id,
                ft.score,
                ft.answers_count,
                ft.views,
                ft.created_at,
                ft.author_id
        )
        SELECT
            tb.thread_id AS "threadId",
            (
                LEAST(tb.interest_weight / 10.0, 1.0) * 40.0
                +
                LEAST(
                    (
                        GREATEST(tb.score, 0)
                        + (tb.answers_count * 2)
                        + (tb.views * 0.05)
                    ) / 25.0,
                    1.0
                ) * 25.0
                +
                GREATEST(
                    0.0,
                    1.0 - (
                        EXTRACT(EPOCH FROM (NOW() - tb.created_at)) / 2592000.0
                    )
                ) * 20.0
                +
                (tb.academic_author * 15.0)
            )::float8 AS "recommendationScore"
        FROM thread_base tb
        ORDER BY
            "recommendationScore" DESC,
            tb.created_at DESC
        LIMIT 5
        """, nativeQuery = true)
    List<ThreadRecommendationProjection> findRecommendedThreadsForUser(@Param("userId") UUID userId);
}