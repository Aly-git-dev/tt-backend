package com.upiiz.platform_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upiiz.platform_api.dto.PostCreateDto;
import com.upiiz.platform_api.dto.ReportAdminActionDto;
import com.upiiz.platform_api.entities.ForumPost;
import com.upiiz.platform_api.entities.ForumReport;
import com.upiiz.platform_api.entities.ForumThread;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.models.ForumStatus;
import com.upiiz.platform_api.models.PostStatus;
import com.upiiz.platform_api.models.ReportStatus;
import com.upiiz.platform_api.repositories.ForumAttachmentRepository;
import com.upiiz.platform_api.repositories.ForumCategoryRepository;
import com.upiiz.platform_api.repositories.ForumPostRepository;
import com.upiiz.platform_api.repositories.ForumReportRepository;
import com.upiiz.platform_api.repositories.ForumSubareaRepository;
import com.upiiz.platform_api.repositories.ForumThreadRepository;
import com.upiiz.platform_api.repositories.PostVoteRepository;
import com.upiiz.platform_api.repositories.ThreadVoteRepository;
import com.upiiz.platform_api.repositories.UserInterestTagRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.storage.ForumFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumServiceTest {

    @Mock private ForumCategoryRepository categoryRepo;
    @Mock private ForumSubareaRepository subareaRepo;
    @Mock private ForumThreadRepository threadRepo;
    @Mock private ForumPostRepository postRepo;
    @Mock private ForumAttachmentRepository attachmentRepo;
    @Mock private ForumReportRepository reportRepo;
    @Mock private UserRepository userRepo;
    @Mock private UserInterestTagRepository interestRepo;
    @Mock private ThreadVoteRepository threadVoteRepo;
    @Mock private PostVoteRepository postVoteRepo;
    @Mock private NotificationService notificationService;
    @Mock private ForumFileStorage forumFileStorage;

    @Test
    void postCreateDtoAcceptsContentAlias() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        PostCreateDto dto = mapper.readValue("{\"content\":\"Respuesta desde frontend\"}", PostCreateDto.class);

        assertEquals("Respuesta desde frontend", dto.getBody());
    }

    @Test
    void createPostRejectsMissingBodyBeforeSaving() {
        ForumService service = newService();

        User author = User.builder()
                .id(UUID.randomUUID())
                .emailInst("author@ipn.mx")
                .nombre("Author")
                .active(true)
                .build();

        when(userRepo.findByEmailInst("author@ipn.mx")).thenReturn(Optional.of(author));

        PostCreateDto dto = new PostCreateDto();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPost(10L, dto, "author@ipn.mx")
        );

        assertEquals("Debes enviar el contenido de la respuesta", ex.getMessage());
        verify(threadRepo, never()).findById(any());
        verify(postRepo, never()).save(any());
    }

    @Test
    void createPostTrimsContentAliasBodyAndSavesPost() {
        ForumService service = newService();

        User author = User.builder()
                .id(UUID.randomUUID())
                .emailInst("author@ipn.mx")
                .nombre("Author")
                .active(true)
                .build();
        ForumThread thread = ForumThread.builder()
                .id(10L)
                .author(author)
                .status(ForumStatus.ABIERTO)
                .answersCount(0)
                .build();
        PostCreateDto dto = new PostCreateDto();
        dto.setBody("  Respuesta valida  ");

        when(userRepo.findByEmailInst("author@ipn.mx")).thenReturn(Optional.of(author));
        when(threadRepo.findById(10L)).thenReturn(Optional.of(thread));
        when(postRepo.save(any(ForumPost.class))).thenAnswer(invocation -> {
            ForumPost post = invocation.getArgument(0);
            post.setId(30L);
            post.setStatus(PostStatus.VISIBLE);
            return post;
        });
        when(threadRepo.save(any(ForumThread.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attachmentRepo.findByPostId(30L)).thenReturn(java.util.List.of());
        when(postVoteRepo.findByUserAndPost(any(User.class), any(ForumPost.class))).thenReturn(Optional.empty());

        var result = service.createPost(10L, dto, "author@ipn.mx");

        assertEquals("Respuesta valida", result.getBody());
        assertEquals(1, thread.getAnswersCount());
        verify(postRepo).save(any(ForumPost.class));
    }

    @Test
    void resolveThreadReportCanCloseThreadAndReturnsUpdatedReport() {
        ForumService service = newService();
        User admin = User.builder()
                .id(UUID.randomUUID())
                .emailInst("admin@ipn.mx")
                .nombre("Admin")
                .active(true)
                .build();
        User reporter = User.builder()
                .id(UUID.randomUUID())
                .emailInst("reporter@ipn.mx")
                .nombre("Reporter")
                .active(true)
                .build();
        User author = User.builder()
                .id(UUID.randomUUID())
                .emailInst("author@ipn.mx")
                .nombre("Author")
                .active(true)
                .build();
        ForumThread thread = ForumThread.builder()
                .id(10L)
                .author(author)
                .title("Hilo reportado")
                .status(ForumStatus.ABIERTO)
                .build();
        ForumReport report = ForumReport.builder()
                .id(20L)
                .reporter(reporter)
                .thread(thread)
                .reasonCode("OFENSIVO")
                .status(ReportStatus.PENDIENTE)
                .build();
        ReportAdminActionDto action = new ReportAdminActionDto();
        action.setDeleteContent(true);

        when(userRepo.findByEmailInst("admin@ipn.mx")).thenReturn(Optional.of(admin));
        when(reportRepo.findById(20L)).thenReturn(Optional.of(report));
        when(reportRepo.save(any(ForumReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.resolveReport(20L, action, "admin@ipn.mx");

        assertEquals(ReportStatus.RESUELTO, report.getStatus());
        assertEquals(ForumStatus.CERRADO, thread.getStatus());
        assertEquals("RESUELTO", result.getStatus());
        assertEquals(10L, result.getThreadId());
        assertEquals("Hilo reportado", result.getThreadTitle());
        assertEquals("Admin", result.getHandledByName());
        verify(threadRepo).save(thread);
        verify(notificationService).notifyForumReportResolved(author.getId());
    }

    private ForumService newService() {
        return new ForumService(
                categoryRepo,
                subareaRepo,
                threadRepo,
                postRepo,
                attachmentRepo,
                reportRepo,
                userRepo,
                interestRepo,
                threadVoteRepo,
                postVoteRepo,
                notificationService,
                forumFileStorage
        );
    }
}
