package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.models.ForumStatus;
import com.upiiz.platform_api.models.PostStatus;
import com.upiiz.platform_api.models.ReportStatus;
import com.upiiz.platform_api.models.ThreadType;
import com.upiiz.platform_api.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumCategoryRepository categoryRepo;
    private final ForumSubareaRepository subareaRepo;
    private final ForumThreadRepository threadRepo;
    private final ForumPostRepository postRepo;
    private final ForumAttachmentRepository attachmentRepo;
    private final ForumReportRepository reportRepo;
    private final UserRepository userRepo;
    private final UserInterestTagRepository interestRepo;
    private final ThreadVoteRepository threadVoteRepo;
    private final PostVoteRepository postVoteRepo;
    private final NotificationService notificationService;

    @Transactional
    public ForumUserSummaryDto getUserSummary(UUID userId) {
        long threads = threadRepo.countByAuthorId(userId);
        long posts = postRepo.countByAuthorId(userId);
        long follows = interestRepo.countByUserId(userId);

        return new ForumUserSummaryDto(threads, posts, follows);
    }

    @Transactional
    public ThreadDetailDto createThread(ThreadCreateDto dto, String userEmail) {
        User author = findUserByEmail(userEmail);

        ForumCategory category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría inválida"));

        ForumSubarea subarea = null;
        if (dto.getSubareaId() != null) {
            subarea = subareaRepo.findById(dto.getSubareaId())
                    .orElseThrow(() -> new IllegalArgumentException("Subárea inválida"));
        }

        ThreadType type = ThreadType.valueOf(dto.getType().toUpperCase());

        ForumThread thread = ForumThread.builder()
                .author(author)
                .category(category)
                .subarea(subarea)
                .title(dto.getTitle().trim())
                .body(dto.getBody().trim())
                .type(type)
                .status(ForumStatus.ABIERTO)
                .score(0)
                .answersCount(0)
                .views(0)
                .build();

        ForumThread saved = threadRepo.save(thread);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            List<ForumAttachment> attachments = dto.getAttachments().stream()
                    .map(a -> ForumAttachment.builder()
                            .thread(saved)
                            .post(null)
                            .kind(a.getKind())
                            .url(a.getUrl())
                            .metadata(null)
                            .build())
                    .toList();

            attachmentRepo.saveAll(attachments);
        }

        return mapThreadToDetail(saved, List.of(), author);
    }

    @Transactional(readOnly = true)
    public ThreadDetailDto getThread(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumThread thread = threadRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));

        List<PostDto> posts = postRepo.findByThreadIdOrderByCreatedAtAsc(id)
                .stream()
                .map(p -> mapPostToDto(p, user))
                .toList();

        return mapThreadToDetail(thread, posts, user);
    }

    @Transactional
    public ThreadDetailDto updateThread(Long threadId, ThreadUpdateDto dto, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));

        validateAuthorOrAdmin(thread.getAuthor().getId(), user);

        if (thread.getStatus() != ForumStatus.ABIERTO) {
            throw new IllegalStateException("No se puede editar un hilo cerrado");
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            thread.setTitle(dto.getTitle().trim());
        }

        if (dto.getBody() != null && !dto.getBody().isBlank()) {
            thread.setBody(dto.getBody().trim());
        }

        if (dto.getCategoryId() != null) {
            ForumCategory category = categoryRepo.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría inválida"));
            thread.setCategory(category);
        }

        if (dto.getSubareaId() != null) {
            ForumSubarea subarea = subareaRepo.findById(dto.getSubareaId())
                    .orElseThrow(() -> new IllegalArgumentException("Subárea inválida"));
            thread.setSubarea(subarea);
        }

        if (dto.getType() != null && !dto.getType().isBlank()) {
            thread.setType(ThreadType.valueOf(dto.getType().toUpperCase()));
        }

        ForumThread saved = threadRepo.save(thread);

        List<PostDto> posts = postRepo.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream()
                .map(p -> mapPostToDto(p, user))
                .toList();

        return mapThreadToDetail(saved, posts, user);
    }

    @Transactional
    public void deleteThread(Long threadId, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));

        validateAuthorOrAdmin(thread.getAuthor().getId(), user);

        thread.setStatus(ForumStatus.CERRADO);
        threadRepo.save(thread);
    }

    @Transactional
    public PostDto createPost(Long threadId, PostCreateDto dto, String userEmail) {
        User author = findUserByEmail(userEmail);

        ForumThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));

        if (thread.getStatus() != ForumStatus.ABIERTO) {
            throw new IllegalStateException("Hilo cerrado");
        }

        ForumPost parent = null;
        if (dto.getParentPostId() != null) {
            parent = postRepo.findById(dto.getParentPostId())
                    .orElseThrow(() -> new IllegalArgumentException("Post padre no encontrado"));

            if (!parent.getThread().getId().equals(threadId)) {
                throw new IllegalArgumentException("El post padre no pertenece a este hilo");
            }
        }

        ForumPost post = ForumPost.builder()
                .thread(thread)
                .author(author)
                .parent(parent)
                .body(dto.getBody().trim())
                .score(0)
                .acceptedAnswer(false)
                .status(PostStatus.VISIBLE)
                .build();

        ForumPost savedPost = postRepo.save(post);

        // 🔔 NOTIFICACIÓN: respuesta a hilo
        if (!thread.getAuthor().getId().equals(author.getId())) {
            notificationService.notifyForumReply(
                    thread.getAuthor().getId(),
                    author.getNombre(),
                    thread.getId()
            );
        }

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            List<ForumAttachment> attachments = dto.getAttachments().stream()
                    .map(a -> ForumAttachment.builder()
                            .post(savedPost)
                            .kind(a.getKind())
                            .url(a.getUrl())
                            .metadata(null)
                            .build())
                    .toList();

            attachmentRepo.saveAll(attachments);
        }

        thread.setAnswersCount(thread.getAnswersCount() + 1);
        threadRepo.save(thread);

        return mapPostToDto(savedPost, author);
    }

    @Transactional
    public PostDto updatePost(Long postId, PostUpdateDto dto, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));

        validateAuthorOrAdmin(post.getAuthor().getId(), user);

        if (post.getStatus() != PostStatus.VISIBLE) {
            throw new IllegalStateException("No se puede editar un comentario oculto");
        }

        if (dto.getBody() != null && !dto.getBody().isBlank()) {
            post.setBody(dto.getBody().trim());
        }

        ForumPost saved = postRepo.save(post);
        return mapPostToDto(saved, user);
    }

    @Transactional
    public void deletePost(Long postId, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));

        validateAuthorOrAdmin(post.getAuthor().getId(), user);

        if (post.getStatus() == PostStatus.VISIBLE) {
            post.setStatus(PostStatus.OCULTO);
            postRepo.save(post);

            ForumThread thread = post.getThread();
            thread.setAnswersCount(Math.max(0, thread.getAnswersCount() - 1));
            threadRepo.save(thread);
        }
    }

    @Transactional
    public ThreadDetailDto likeThread(Long threadId, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));

        if (thread.getStatus() != ForumStatus.ABIERTO) {
            throw new IllegalStateException("No se puede dar like a un hilo cerrado");
        }

        if (threadVoteRepo.findByUserAndThread(user, thread).isEmpty()) {
            ThreadVote vote = ThreadVote.builder()
                    .user(user)
                    .thread(thread)
                    .value((short) 1)
                    .build();

            threadVoteRepo.save(vote);
            thread.setScore(thread.getScore() + 1);
            threadRepo.save(thread);

            // 🔔 NOTIFICACIÓN: like a hilo
            if (!thread.getAuthor().getId().equals(user.getId())) {
                notificationService.notifyForumLikeThread(
                        thread.getAuthor().getId(),
                        user.getNombre(),
                        thread.getId()
                );
            }
        }

        List<PostDto> posts = postRepo.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream()
                .map(p -> mapPostToDto(p, user))
                .toList();

        return mapThreadToDetail(thread, posts, user);
    }

    @Transactional
    public ThreadDetailDto unlikeThread(Long threadId, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));

        threadVoteRepo.findByUserAndThread(user, thread).ifPresent(vote -> {
            threadVoteRepo.delete(vote);
            thread.setScore(Math.max(0, thread.getScore() - 1));
            threadRepo.save(thread);
        });

        List<PostDto> posts = postRepo.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream()
                .map(p -> mapPostToDto(p, user))
                .toList();

        return mapThreadToDetail(thread, posts, user);
    }

    @Transactional
    public PostDto likePost(Long postId, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));

        if (post.getStatus() != PostStatus.VISIBLE) {
            throw new IllegalStateException("No se puede dar like a un comentario oculto");
        }

        if (postVoteRepo.findByUserAndPost(user, post).isEmpty()) {
            PostVote vote = PostVote.builder()
                    .user(user)
                    .post(post)
                    .value((short) 1)
                    .build();

            postVoteRepo.save(vote);
            post.setScore(post.getScore() + 1);
            postRepo.save(post);

            // 🔔 NOTIFICACIÓN: like a respuesta
            if (!post.getAuthor().getId().equals(user.getId())) {
                notificationService.notifyForumLikePost(
                        post.getAuthor().getId(),
                        user.getNombre(),
                        post.getThread().getId()
                );
            }
        }

        return mapPostToDto(post, user);
    }

    @Transactional
    public PostDto unlikePost(Long postId, String userEmail) {
        User user = findUserByEmail(userEmail);

        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));

        postVoteRepo.findByUserAndPost(user, post).ifPresent(vote -> {
            postVoteRepo.delete(vote);
            post.setScore(Math.max(0, post.getScore() - 1));
            postRepo.save(post);
        });

        return mapPostToDto(post, user);
    }

    @Transactional(readOnly = true)
    public List<ThreadSummaryDto> getRecommendedThreads(String userEmail) {
        User user = findUserByEmail(userEmail);

        List<Long> recommendedIds = threadRepo.findRecommendedThreadsForUser(user.getId())
                .stream()
                .map(ThreadRecommendationProjection::getThreadId)
                .toList();

        if (recommendedIds.isEmpty()) {
            return threadRepo.findTop5ByStatusOrderByScoreDescCreatedAtDesc(ForumStatus.ABIERTO)
                    .stream()
                    .map(t -> mapThreadToSummary(t, user))
                    .toList();
        }

        List<ForumThread> recommendedThreads = threadRepo.findAllById(recommendedIds);

        return recommendedIds.stream()
                .flatMap(id -> recommendedThreads.stream()
                        .filter(t -> t.getId().equals(id))
                        .findFirst()
                        .stream())
                .map(t -> mapThreadToSummary(t, user))
                .toList();
    }

    @Transactional
    public void createReport(ReportCreateDto dto, String userEmail) {
        User reporter = findUserByEmail(userEmail);

        ForumThread thread = null;
        ForumPost post = null;

        if (dto.getThreadId() != null) {
            thread = threadRepo.findById(dto.getThreadId())
                    .orElseThrow(() -> new IllegalArgumentException("Hilo no encontrado"));
        }

        if (dto.getPostId() != null) {
            post = postRepo.findById(dto.getPostId())
                    .orElseThrow(() -> new IllegalArgumentException("Post no encontrado"));
        }

        ForumReport report = ForumReport.builder()
                .reporter(reporter)
                .thread(thread)
                .post(post)
                .reasonCode(dto.getReasonCode())
                .description(dto.getDescription())
                .build();

        reportRepo.save(report);

        // 🔔 NOTIFICACIÓN: contenido reportado
        User reportedUser = null;

        if (post != null) {
            reportedUser = post.getAuthor();
        } else if (thread != null) {
            reportedUser = thread.getAuthor();
        }

        if (reportedUser != null && !reportedUser.getId().equals(reporter.getId())) {
            notificationService.notifyForumReported(reportedUser.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<AdminReportDto> getPendingReportsForAdmin() {
        return reportRepo.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDIENTE)
                .stream()
                .map(this::mapReportToAdminDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminReportDto> getAllReportsForAdmin() {
        return reportRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapReportToAdminDto)
                .toList();
    }

    @Transactional
    public void resolveReport(Long reportId, ReportAdminActionDto actionDto, String adminEmail) {
        User admin = findUserByEmail(adminEmail);

        ForumReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));

        ForumPost post = report.getPost();
        ForumThread thread = report.getThread();

        if (actionDto.isBanUser()) {
            User toBan;

            if (post != null) {
                toBan = post.getAuthor();
            } else if (thread != null) {
                toBan = thread.getAuthor();
            } else {
                throw new IllegalStateException("Reporte sin post ni thread asociado");
            }

            toBan.setActive(false);
            userRepo.save(toBan);
        }

        if (actionDto.isDeleteContent()) {
            if (post != null) {
                post.setStatus(PostStatus.OCULTO);
                postRepo.save(post);
            } else if (thread != null) {
                thread.setStatus(ForumStatus.CERRADO);
                threadRepo.save(thread);
            }
        }

        report.setStatus(ReportStatus.RESUELTO);
        report.setHandledBy(admin);
        report.setHandledAt(Instant.now());

        if (actionDto.getAdminNote() != null && !actionDto.getAdminNote().isBlank()) {
            report.setDescription(actionDto.getAdminNote());
        }

        // 🔔 NOTIFICACIÓN: reporte resuelto
        User reportedUser = null;

        if (post != null) {
            reportedUser = post.getAuthor();
        } else if (thread != null) {
            reportedUser = thread.getAuthor();
        }

        if (reportedUser != null) {
            notificationService.notifyForumReportResolved(reportedUser.getId());
        }

        reportRepo.save(report);
    }

    private User findUserByEmail(String email) {
        return userRepo.findByEmailInst(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void validateAuthorOrAdmin(UUID authorId, User user) {
        boolean isAuthor = authorId.equals(user.getId());

        boolean isAdmin = user.getRoles() != null &&
                user.getRoles().stream()
                        .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));

        if (!isAuthor && !isAdmin) {
            throw new SecurityException("No tienes permisos para modificar este contenido");
        }
    }

    private ThreadDetailDto mapThreadToDetail(
            ForumThread t,
            List<PostDto> posts,
            User currentUser
    ) {
        List<AttachmentDto> threadAttachments = attachmentRepo.findByThreadId(t.getId())
                .stream()
                .map(a -> AttachmentDto.builder()
                        .id(a.getId())
                        .kind(a.getKind())
                        .url(a.getUrl())
                        .build())
                .toList();

        boolean likedByMe = currentUser != null &&
                threadVoteRepo.findByUserAndThread(currentUser, t).isPresent();

        return ThreadDetailDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .body(t.getBody())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .score(t.getScore())
                .answersCount(t.getAnswersCount())
                .views(t.getViews())
                .categoryId(t.getCategory().getId())
                .categoryName(t.getCategory().getName())
                .subareaId(t.getSubarea() != null ? t.getSubarea().getId() : null)
                .subareaName(t.getSubarea() != null ? t.getSubarea().getName() : null)
                .authorId(t.getAuthor().getId().toString())
                .authorName(t.getAuthor().getNombre())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .likedByMe(likedByMe)
                .attachments(threadAttachments)
                .posts(posts)
                .build();
    }

    private ThreadSummaryDto mapThreadToSummary(ForumThread t, User currentUser) {
        boolean likedByMe = currentUser != null &&
                threadVoteRepo.findByUserAndThread(currentUser, t).isPresent();

        return ThreadSummaryDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .categoryName(t.getCategory().getName())
                .subareaName(t.getSubarea() != null ? t.getSubarea().getName() : null)
                .type(t.getType().name())
                .score(t.getScore())
                .answersCount(t.getAnswersCount())
                .views(t.getViews())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .likedByMe(likedByMe)
                .build();
    }

    private PostDto mapPostToDto(ForumPost p, User currentUser) {
        List<AttachmentDto> attachments = attachmentRepo.findByPostId(p.getId())
                .stream()
                .map(a -> AttachmentDto.builder()
                        .id(a.getId())
                        .kind(a.getKind())
                        .url(a.getUrl())
                        .build())
                .toList();

        boolean likedByMe = currentUser != null &&
                postVoteRepo.findByUserAndPost(currentUser, p).isPresent();

        return PostDto.builder()
                .id(p.getId())
                .body(p.getBody())
                .status(p.getStatus().name())
                .score(p.getScore())
                .acceptedAnswer(p.isAcceptedAnswer())
                .authorId(p.getAuthor().getId().toString())
                .authorName(p.getAuthor().getNombre())
                .parentPostId(p.getParent() != null ? p.getParent().getId() : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .likedByMe(likedByMe)
                .attachments(attachments)
                .build();
    }

    private AdminReportDto mapReportToAdminDto(ForumReport r) {
        ForumThread t = r.getThread();
        ForumPost p = r.getPost();

        User reportedUser = null;

        if (p != null) {
            reportedUser = p.getAuthor();
        } else if (t != null) {
            reportedUser = t.getAuthor();
        }

        return AdminReportDto.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId().toString())
                .reporterName(r.getReporter().getNombre())
                .threadId(t != null ? t.getId() : null)
                .threadTitle(t != null ? t.getTitle() : null)
                .postId(p != null ? p.getId() : null)
                .reportedUserId(reportedUser != null ? reportedUser.getId().toString() : null)
                .reportedUserName(reportedUser != null ? reportedUser.getNombre() : null)
                .reasonCode(r.getReasonCode())
                .description(r.getDescription())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .handledAt(r.getHandledAt())
                .handledByName(r.getHandledBy() != null ? r.getHandledBy().getNombre() : null)
                .build();
    }
    @Transactional(readOnly = true)
    public List<ThreadSummaryDto> getAllOpenThreads(String userEmail) {
        User user = findUserByEmail(userEmail);

        return threadRepo.findByStatusOrderByCreatedAtDesc(ForumStatus.ABIERTO)
                .stream()
                .map(t -> mapThreadToSummary(t, user))
                .toList();
    }
}