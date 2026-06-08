package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.CreateTeacherEvaluationRequest;
import com.upiiz.platform_api.dto.CreateTopicDifficultyEventRequest;
import com.upiiz.platform_api.dto.CreateTopicInterestEventRequest;
import com.upiiz.platform_api.dto.UserSearchResponse;
import com.upiiz.platform_api.entities.TeacherEvaluation;
import com.upiiz.platform_api.entities.TopicDifficultyEvent;
import com.upiiz.platform_api.entities.TopicInterestEvent;
import com.upiiz.platform_api.models.TeacherEvaluationRating;
import com.upiiz.platform_api.repositories.AnalyticsQueryRepo;
import com.upiiz.platform_api.repositories.TeacherEvaluationRepo;
import com.upiiz.platform_api.repositories.TopicDifficultyEventRepo;
import com.upiiz.platform_api.repositories.TopicInterestEventRepo;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.repositories.AnalyticsModerationSummaryProjection;
import com.upiiz.platform_api.repositories.AdminTopicDifficultyProjection;
import com.upiiz.platform_api.repositories.AdminTopicInterestProjection;
import com.upiiz.platform_api.repositories.TeacherImprovementAreaProjection;
import com.upiiz.platform_api.repositories.TeacherPerformanceProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final TeacherEvaluationRepo teacherEvaluationRepo;
    private final TopicInterestEventRepo topicInterestEventRepo;
    private final TopicDifficultyEventRepo topicDifficultyEventRepo;
    private final AnalyticsQueryRepo analyticsQueryRepo;
    private final UserRepository userRepo;

    public AnalyticsService(
            TeacherEvaluationRepo teacherEvaluationRepo,
            TopicInterestEventRepo topicInterestEventRepo,
            TopicDifficultyEventRepo topicDifficultyEventRepo,
            AnalyticsQueryRepo analyticsQueryRepo,
            UserRepository userRepo
    ) {
        this.teacherEvaluationRepo = teacherEvaluationRepo;
        this.topicInterestEventRepo = topicInterestEventRepo;
        this.topicDifficultyEventRepo = topicDifficultyEventRepo;
        this.analyticsQueryRepo = analyticsQueryRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public TeacherEvaluation createTeacherEvaluation(CreateTeacherEvaluationRequest req) {
        validateTeacherEvaluation(req);

        if (req.getEvaluatorId() != null && req.getAppointmentId() != null) {
            teacherEvaluationRepo
                    .findByEvaluatorIdAndTeacherIdAndAppointmentId(
                            req.getEvaluatorId(),
                            req.getTeacherId(),
                            req.getAppointmentId()
                    )
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ya existe una evaluación para este profesor en esa cita por el mismo evaluador");
                    });
        }

        TeacherEvaluation entity = new TeacherEvaluation();
        entity.setTeacherId(req.getTeacherId());
        entity.setEvaluatorId(req.getEvaluatorId());
        entity.setAppointmentId(req.getAppointmentId());
        entity.setRatingClarity(req.getRatingClarity().shortValue());
        entity.setRatingKnowledge(req.getRatingKnowledge().shortValue());
        entity.setRatingSupport(req.getRatingSupport().shortValue());
        entity.setRatingPunctuality(req.getRatingPunctuality().shortValue());
        entity.setComment(req.getComment());
        entity.setAnonymous(Boolean.TRUE.equals(req.getAnonymous()));

        return teacherEvaluationRepo.save(entity);
    }

    @Transactional
    public TopicInterestEvent createTopicInterestEvent(CreateTopicInterestEventRequest req) {
        if (req.getUserId() == null) {
            throw new IllegalArgumentException("userId es obligatorio");
        }
        if (req.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId es obligatorio");
        }
        if (req.getSourceType() == null) {
            throw new IllegalArgumentException("sourceType es obligatorio");
        }

        TopicInterestEvent entity = new TopicInterestEvent();
        entity.setUserId(req.getUserId());
        entity.setCategoryId(req.getCategoryId());
        entity.setSubareaId(req.getSubareaId());
        entity.setThreadId(req.getThreadId());
        entity.setAppointmentId(req.getAppointmentId());
        entity.setVideoMeetingId(req.getVideoMeetingId());
        entity.setSourceType(req.getSourceType());
        entity.setWeight(req.getWeight() == null || req.getWeight() <= 0 ? 1 : req.getWeight());

        return topicInterestEventRepo.save(entity);
    }

    @Transactional
    public TopicDifficultyEvent createTopicDifficultyEvent(CreateTopicDifficultyEventRequest req) {
        if (req.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId es obligatorio");
        }
        if (req.getSourceType() == null) {
            throw new IllegalArgumentException("sourceType es obligatorio");
        }
        if (req.getDifficultyLevel() == null || req.getDifficultyLevel() < 1 || req.getDifficultyLevel() > 5) {
            throw new IllegalArgumentException("difficultyLevel debe estar entre 1 y 5");
        }

        TopicDifficultyEvent entity = new TopicDifficultyEvent();
        entity.setUserId(req.getUserId());
        entity.setTeacherId(req.getTeacherId());
        entity.setCategoryId(req.getCategoryId());
        entity.setSubareaId(req.getSubareaId());
        entity.setThreadId(req.getThreadId());
        entity.setAppointmentId(req.getAppointmentId());
        entity.setVideoMeetingId(req.getVideoMeetingId());
        entity.setSourceType(req.getSourceType());
        entity.setDifficultyLevel(req.getDifficultyLevel().shortValue());
        entity.setNotes(req.getNotes());

        return topicDifficultyEventRepo.save(entity);
    }

    @Transactional(readOnly = true)
    public List<AdminTopicInterestProjection> getAdminTopicInterest() {
        return analyticsQueryRepo.findAdminTopicInterest();
    }

    @Transactional(readOnly = true)
    public List<AdminTopicDifficultyProjection> getAdminTopicDifficulty() {
        return analyticsQueryRepo.findAdminTopicDifficulty();
    }

    @Transactional(readOnly = true)
    public AnalyticsModerationSummaryProjection getModerationSummary() {
        return analyticsQueryRepo.findModerationSummary();
    }

    @Transactional(readOnly = true)
    public List<TeacherPerformanceProjection> getTeacherPerformance() {
        return analyticsQueryRepo.findTeacherPerformance();
    }

    @Transactional(readOnly = true)
    public TeacherPerformanceProjection getTeacherPerformanceByTeacherId(UUID teacherId) {
        return analyticsQueryRepo.findTeacherPerformanceByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public List<TeacherImprovementAreaProjection> getTeacherImprovementAreas() {
        return analyticsQueryRepo.findTeacherImprovementAreas();
    }

    @Transactional(readOnly = true)
    public List<TeacherImprovementAreaProjection> getTeacherImprovementAreasByTeacherId(UUID teacherId) {
        return analyticsQueryRepo.findTeacherImprovementAreasByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchTeachers(String query) {
        String term = query == null ? "" : query.trim();
        return userRepo
                .searchActiveUsersByRoles(term, List.of("PROFESOR"), PageRequest.of(0, 20))
                .stream()
                .map(this::mapUserSearchResponse)
                .toList();
    }

    private void validateTeacherEvaluation(CreateTeacherEvaluationRequest req) {
        if (req.getTeacherId() == null) {
            throw new IllegalArgumentException("teacherId es obligatorio");
        }

        TeacherEvaluationRating.validate(req.getRatingClarity(), "ratingClarity");
        TeacherEvaluationRating.validate(req.getRatingKnowledge(), "ratingKnowledge");
        TeacherEvaluationRating.validate(req.getRatingSupport(), "ratingSupport");
        TeacherEvaluationRating.validate(req.getRatingPunctuality(), "ratingPunctuality");
    }

    private UserSearchResponse mapUserSearchResponse(Object[] row) {
        UserSearchResponse response = new UserSearchResponse();
        response.id = (UUID) row[0];
        response.email = (String) row[1];
        response.name = (String) row[2];
        return response;
    }
}
