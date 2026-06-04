package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/upiiz/public/v1/forums")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://148.204.142.20:3030")
@Tag(
        name = "forum-controller",
        description = "Operaciones relacionadas con los foros (hilos, respuestas, recomendaciones y reportes)"
)
@SecurityRequirement(name = "bearer-jwt") // ajusta el nombre si en tu OpenAPI se llama distinto
public class ForumController {

    private final ForumService forumService;
    private final UserRepository userRepository;

    private String getEmail(Authentication auth) {
        return auth.getName(); // asumiendo que el subject del JWT es email_inst
    }

    // =========================================================
    // Crear hilo
    // =========================================================
    @PostMapping(value = "/threads", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Crear un nuevo hilo de foro",
            description = """
                Crea un nuevo hilo de foro (tipo pregunta, discusión o anuncio) asociado a una categoría
                y opcionalmente a una subárea. El autor se toma del usuario autenticado (JWT).
                """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Hilo creado correctamente",
            content = @Content(schema = @Schema(implementation = ThreadDetailDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos (por ejemplo categoría o subárea inexistente)",
            content = @Content
    )
    public ResponseEntity<ThreadDetailDto> createThread(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos para crear un nuevo hilo de foro",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ThreadCreateDto.class))
            )
            ThreadCreateDto dto,

            @Parameter(hidden = true) Authentication auth
    ) {
        String email = getEmail(auth);
        ThreadDetailDto created = forumService.createThread(dto, email);
        return ResponseEntity
                .created(URI.create("/upiiz/public/v1/forums/threads/" + created.getId()))
                .body(created);
    }

    @PostMapping(value = "/threads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Crear hilo con archivos adjuntos",
            description = "Crea un hilo usando una parte JSON llamada thread y una o varias partes files."
    )
    public ResponseEntity<ThreadDetailDto> createThreadWithAttachments(
            @RequestPart("thread") ThreadCreateDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(hidden = true) Authentication auth
    ) throws Exception {
        String email = getEmail(auth);
        ThreadDetailDto created = forumService.createThreadWithFiles(dto, email, files);
        return ResponseEntity
                .created(URI.create("/upiiz/public/v1/forums/threads/" + created.getId()))
                .body(created);
    }

    @GetMapping("/threads/search")
    @Operation(summary = "Buscar hilos", description = "Busca hilos abiertos por texto en titulo, contenido, categoria, subarea o autor.")
    public ResponseEntity<Page<ThreadSummaryDto>> searchThreads(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Authentication auth
    ) {
        String email = getEmail(auth);
        Page<ThreadSummaryDto> threads = forumService.searchOpenThreads(email, q, page, size);
        return ResponseEntity.ok(threads);
    }

    // =========================================================
    // Obtener detalle de hilo
    // =========================================================
    @GetMapping("/threads/{id:\\d+}")
    @Operation(
            summary = "Obtener detalle de un hilo",
            description = """
                Obtiene la información completa de un hilo, incluyendo sus metadatos y la lista de respuestas
                asociadas, ordenadas por fecha de creación.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Hilo encontrado",
            content = @Content(schema = @Schema(implementation = ThreadDetailDto.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Hilo no encontrado",
            content = @Content
    )
    public ResponseEntity<ThreadDetailDto> getThread(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        ThreadDetailDto thread = forumService.getThread(id, email);
        return ResponseEntity.ok(thread);
    }

    // =========================================================
    // Crear respuesta / comentario en un hilo
    // =========================================================
    @PostMapping(value = "/threads/{id:\\d+}/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Responder a un hilo de foro",
            description = """
                Crea una nueva respuesta o comentario dentro de un hilo. Puede ser respuesta directa al hilo
                o respuesta anidada a otro post (parentPostId). El autor se toma del usuario autenticado.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Respuesta creada correctamente",
            content = @Content(schema = @Schema(implementation = PostDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o hilo/post padre inexistente",
            content = @Content
    )
    @ApiResponse(
            responseCode = "409",
            description = "El hilo está cerrado y no admite nuevas respuestas",
            content = @Content
    )
    public ResponseEntity<PostDto> createPost(
            @Parameter(
                    description = "Identificador del hilo donde se creará la respuesta",
                    required = true,
                    example = "1"
            )
            @PathVariable Long id,

            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Contenido de la respuesta y adjuntos opcionales",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PostCreateDto.class))
            )
            PostCreateDto dto,

            @Parameter(hidden = true) Authentication auth
    ) {
        String email = getEmail(auth);
        PostDto created = forumService.createPost(id, dto, email);
        return ResponseEntity.ok(created);
    }

    @PostMapping(value = "/threads/{id:\\d+}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Responder a un hilo con archivos adjuntos",
            description = "Crea una respuesta usando una parte JSON llamada post y una o varias partes files."
    )
    public ResponseEntity<PostDto> createPostWithAttachments(
            @PathVariable Long id,
            @RequestPart("post") PostCreateDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(hidden = true) Authentication auth
    ) throws Exception {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.createPostWithFiles(id, dto, email, files));
    }

    // =========================================================
    // Hilos recomendados para el dashboard
    // =========================================================
    @GetMapping("/recommended")
    @Operation(
            summary = "Obtener hilos recomendados para el usuario",
            description = """
                Devuelve una lista de hilos recomendados para el usuario autenticado. En esta iteración
                se implementa una estrategia sencilla basada en los hilos abiertos con mejor puntuación
                y fecha más reciente.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de hilos recomendados",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ThreadSummaryDto.class))
            )
    )
    public ResponseEntity<List<ThreadSummaryDto>> getRecommended(
            @Parameter(hidden = true) Authentication auth
    ) {
        String email = getEmail(auth);
        List<ThreadSummaryDto> threads = forumService.getRecommendedThreads(email);
        return ResponseEntity.ok(threads);
    }

    // =========================================================
    // Reportar contenido (hilo o post)
    // =========================================================
    @PostMapping("/reports")
    @Operation(
            summary = "Reportar contenido de foro",
            description = """
                Permite reportar un hilo o una respuesta/post por parte de un usuario autenticado.
                Se debe indicar el motivo del reporte (reasonCode) y opcionalmente una descripción.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Reporte registrado correctamente",
            content = @Content
    )
    @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o contenido no encontrado",
            content = @Content
    )
    public ResponseEntity<Void> reportContent(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del reporte (hilo o post, motivo y descripción opcional)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReportCreateDto.class))
            )
            ReportCreateDto dto,

            @Parameter(hidden = true) Authentication auth
    ) {
        String email = getEmail(auth);
        forumService.createReport(dto, email);
        return ResponseEntity.ok().build();
    }
    // =========================================================
    // Obtener resumen
    // =========================================================
    @GetMapping("/me/summary")
    @Operation(
            summary = "Resumen de actividad del usuario actual en foros",
            description = "Devuelve conteos de hilos creados, respuestas enviadas y foros seguidos."
    )
    public ResponseEntity<ForumUserSummaryDto> getMySummary(Authentication auth) {
        var email = auth.getName(); // lo pone tu JwtAuthFilter
        var user = userRepository.findByEmailInst(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        var dto = forumService.getUserSummary(user.getId());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/threads/{id:\\d+}")
    @Operation(summary = "Actualizar hilo", description = "Permite al autor o a un administrador modificar los datos de un hilo abierto.")
    public ResponseEntity<ThreadDetailDto> updateThread(
            @PathVariable Long id,
            @RequestBody ThreadUpdateDto dto,
            Authentication auth
    ) {
        String email = getEmail(auth);
        ThreadDetailDto updated = forumService.updateThread(id, dto, email);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/threads/{id:\\d+}")
    @Operation(summary = "Cerrar hilo", description = "Cierra un hilo mediante borrado logico.")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        forumService.deleteThread(id, email);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/posts/{id:\\d+}")
    @Operation(summary = "Actualizar respuesta", description = "Permite al autor o a un administrador modificar una respuesta visible.")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable Long id,
            @RequestBody PostUpdateDto dto,
            Authentication auth
    ) {
        String email = getEmail(auth);
        PostDto updated = forumService.updatePost(id, dto, email);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/posts/{id:\\d+}")
    @Operation(summary = "Ocultar respuesta", description = "Oculta una respuesta mediante borrado logico.")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        forumService.deletePost(id, email);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/threads/{id:\\d+}/like")
    @Operation(summary = "Dar like a hilo", description = "Registra un like del usuario autenticado sobre un hilo.")
    public ResponseEntity<ThreadDetailDto> likeThread(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.likeThread(id, email));
    }

    @DeleteMapping("/threads/{id:\\d+}/like")
    @Operation(summary = "Quitar like de hilo", description = "Elimina el like del usuario autenticado sobre un hilo.")
    public ResponseEntity<ThreadDetailDto> unlikeThread(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.unlikeThread(id, email));
    }

    @PostMapping("/posts/{id:\\d+}/like")
    @Operation(summary = "Dar like a respuesta", description = "Registra un like del usuario autenticado sobre una respuesta.")
    public ResponseEntity<PostDto> likePost(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.likePost(id, email));
    }

    @DeleteMapping("/posts/{id:\\d+}/like")
    @Operation(summary = "Quitar like de respuesta", description = "Elimina el like del usuario autenticado sobre una respuesta.")
    public ResponseEntity<PostDto> unlikePost(
            @PathVariable Long id,
            Authentication auth
    ) {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.unlikePost(id, email));
    }

    @PostMapping(value = "/threads/{id:\\d+}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Adjuntar archivos a un hilo existente",
            description = "Permite al autor o a un administrador agregar archivos a un hilo existente."
    )
    public ResponseEntity<ThreadDetailDto> addThreadAttachments(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files,
            Authentication auth
    ) throws Exception {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.addThreadAttachments(id, files, email));
    }

    @PostMapping(value = "/posts/{id:\\d+}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Adjuntar archivos a una respuesta existente",
            description = "Permite al autor o a un administrador agregar archivos a una respuesta existente."
    )
    public ResponseEntity<PostDto> addPostAttachments(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files,
            Authentication auth
    ) throws Exception {
        String email = getEmail(auth);
        return ResponseEntity.ok(forumService.addPostAttachments(id, files, email));
    }

    @GetMapping("/attachments/{id:\\d+}/download")
    @Operation(
            summary = "Descargar archivo adjunto de foro",
            description = "Descarga un archivo adjunto subido a un hilo o respuesta de foro."
    )
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long id,
            Authentication auth
    ) throws Exception {
        String email = getEmail(auth);
        return forumService.downloadAttachment(id, email);
    }

    @GetMapping("/threads")
    @Operation(summary = "Listar hilos abiertos", description = "Devuelve todos los hilos abiertos ordenados por fecha de creacion.")
    public ResponseEntity<List<ThreadSummaryDto>> getAllThreads(Authentication auth) {
        String email = getEmail(auth);
        List<ThreadSummaryDto> threads = forumService.getAllOpenThreads(email);
        return ResponseEntity.ok(threads);
    }
}
