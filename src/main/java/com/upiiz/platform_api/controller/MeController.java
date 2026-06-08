package com.upiiz.platform_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upiiz.platform_api.dto.UpdateProfileRequest;
import com.upiiz.platform_api.dto.UserDTO;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.*;

@RestController
@RequestMapping("/upiiz/public/v1/me")
@Tag(name = "Perfil", description = "Operaciones relacionadas con el perfil del usuario autenticado")
public class MeController {

    private final UserRepository users;
    private final ObjectMapper om;

    public MeController(UserRepository users, ObjectMapper om) {
        this.users = users;
        this.om = om;
    }

    // OBTENER PERFIL
    @GetMapping
    @Operation(
            summary = "Obtener perfil del usuario actual",
            description = "Devuelve la información del perfil del usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil obtenido correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Perfil completo",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Perfil obtenido correctamente\",\n" +
                                            "  \"usuario\": {\n" +
                                            "    \"id\": \"f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012\",\n" +
                                            "    \"emailInst\": \"user@ipn.mx\",\n" +
                                            "    \"fullName\": \"Nombre Apellido\",\n" +
                                            "    \"active\": true,\n" +
                                            "    \"bio\": \"Estudiante de Ingeniería en Sistemas en la UPIIZ\",\n" +
                                            "    \"interests\": [\"Compiladores\", \"Redes\", \"Plataformas educativas\"],\n" +
                                            "    \"links\": [\n" +
                                            "      {\n" +
                                            "        \"label\": \"GitHub\",\n" +
                                            "        \"url\": \"https://github.com/user\"\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"avatarUrl\": \"https://cdn.plattform.com/avatars/user.png\",\n" +
                                            "    \"coverUrl\": \"https://cdn.plattform.com/covers/user-cover.png\",\n" +
                                            "    \"roles\": [\"ALUMNO\"]\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un usuario asociado al token actual",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Usuario no encontrado",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Usuario no encontrado\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al obtener el perfil del usuario",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Error al obtener perfil\",\n" +
                                            "  \"error\": \"Detalle técnico del error\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> me(
            @Parameter(hidden = true) Authentication auth
    ) {
        try {
            var u = users.findByEmailInst(auth.getName()).orElse(null);
            if (u == null) {
                return ResponseEntity.status(404).body(
                        Map.of("estado", 0, "mensaje", "Usuario no encontrado"));
            }
            return ResponseEntity.ok(
                    Map.of("estado", 1, "mensaje", "Perfil obtenido correctamente", "usuario", toDTO(u))
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("estado", 0, "mensaje", "Error al obtener perfil", "error", e.getMessage()));
        }
    }

    // ACTUALIZAR PERFIL
    @PutMapping
    @Operation(
            summary = "Actualizar perfil del usuario actual",
            description = "Permite actualizar datos del perfil como nombre completo, biografía, intereses y enlaces."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil actualizado correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Actualización exitosa",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Perfil actualizado correctamente\",\n" +
                                            "  \"usuario\": {\n" +
                                            "    \"id\": \"f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012\",\n" +
                                            "    \"boleta\": \"00000000\",\n" +
                                            "    \"emailInst\": \"user@ipn.mx\",\n" +
                                            "    \"fullName\": \"Nombre Apellido Actualizado\",\n" +
                                            "    \"programa\": \"SISTEMAS COMPUTACIONALES\",\n" +
                                            "    \"active\": true,\n" +
                                            "    \"bio\": \"Bio actualizada del usuario\",\n" +
                                            "    \"interests\": [\"Compiladores\", \"Redes\", \"Docencia\"],\n" +
                                            "    \"links\": [\n" +
                                            "      {\n" +
                                            "        \"label\": \"GitHub\",\n" +
                                            "        \"url\": \"https://github.com/user\"\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"label\": \"LinkedIn\",\n" +
                                            "        \"url\": \"https://linkedin.com/in/user\"\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"avatarUrl\": \"https://cdn.plattform.com/avatars/user.png\",\n" +
                                            "    \"coverUrl\": \"https://cdn.plattform.com/covers/user-cover.png\",\n" +
                                            "    \"roles\": [\"ALUMNO\"]\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un usuario asociado al token actual",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Usuario no encontrado",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Usuario no encontrado\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al actualizar el perfil del usuario",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Error al actualizar perfil\",\n" +
                                            "  \"error\": \"Detalle técnico del error\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> update(
            @Parameter(hidden = true) Authentication auth,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos a actualizar del perfil del usuario",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateProfileRequest.class)
                    )
            )
            @RequestBody UpdateProfileRequest req
    ) {
        try {
            var u = users.findByEmailInst(auth.getName()).orElse(null);
            if (u == null) {
                return ResponseEntity.status(404).body(
                        Map.of("estado", 0, "mensaje", "Usuario no encontrado"));
            }

            // nombre (opcional)
            if (req.fullName() != null && !req.fullName().isBlank()) {
                u.setNombre(req.fullName());
            }

            // bio
            if (req.bio() != null) {
                u.setBio(req.bio());
            }

            // intereses
            if (req.interests() != null) {
                u.setInterests(om.writeValueAsString(req.interests()));
            }

            // links
            if (req.links() != null) {
                u.setLinks(om.writeValueAsString(req.links()));
            }

            // avatar / cover
            if (req.avatarUrl() != null) {
                u.setAvatarUrl(req.avatarUrl());
            }
            if (req.coverUrl() != null) {
                u.setCoverUrl(req.coverUrl());
            }

            users.save(u);

            return ResponseEntity.ok(
                    Map.of("estado", 1, "mensaje", "Perfil actualizado correctamente", "usuario", toDTO(u))
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("estado", 0, "mensaje", "Error al actualizar perfil", "error", e.getMessage()));
        }
    }

    // CONVERSIÓN USER → DTO
    private UserDTO toDTO(User u) throws Exception {
        List<String> interests = u.getInterests() == null ? List.of() :
                om.readValue(u.getInterests(), new TypeReference<List<String>>() {});
        List<UserDTO.LinkDTO> links = u.getLinks() == null ? List.of() :
                om.readValue(u.getLinks(), new TypeReference<List<UserDTO.LinkDTO>>() {});
        List<String> roles = u.getRoles().stream().map(r -> r.getName()).toList();

        return new UserDTO(
                u.getId(),
                u.getEmailInst(),
                u.getNombre(),        // fullName
                u.isActive(),
                u.getBio(),
                interests,
                links,
                u.getAvatarUrl(),
                u.getCoverUrl(),
                roles,
                u.getExternalId(),
                u.getCarrera()
        );
    }
    // ---------- SUBIR AVATAR ----------
    @PostMapping("/avatar")
    @Operation(
            summary = "Subir avatar del usuario actual",
            description = "Permite subir una imagen de avatar y actualiza la URL del usuario."
    )
    public ResponseEntity<?> uploadAvatar(
            @Parameter(hidden = true) Authentication auth,
            @RequestParam("file") MultipartFile file
    ) {
        return handleImageUpload(auth, file, "avatar");
    }

    // ---------- SUBIR COVER ----------
    @PostMapping("/cover")
    @Operation(
            summary = "Subir portada del usuario actual",
            description = "Permite subir una imagen de portada y actualiza la URL del usuario."
    )
    public ResponseEntity<?> uploadCover(
            @Parameter(hidden = true) Authentication auth,
            @RequestParam("file") MultipartFile file
    ) {
        return handleImageUpload(auth, file, "cover");
    }

    private ResponseEntity<?> handleImageUpload(Authentication auth, MultipartFile file, String type) {
        try {
            var u = users.findByEmailInst(auth.getName()).orElse(null);
            if (u == null) {
                return ResponseEntity.status(404).body(
                        Map.of("estado", 0, "mensaje", "Usuario no encontrado"));
            }
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("estado", 0, "mensaje", "Archivo vacío"));
            }

            // validación muy básica de tipo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(
                        Map.of("estado", 0, "mensaje", "El archivo debe ser una imagen"));
            }

            // carpeta local (sirve en dev). Spring Boot sirve /static/** como /.
            Path uploadDir = Paths.get("src/main/resources/static/uploads");
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".png";

            String filename = u.getId() + "-" + type + "-" + System.currentTimeMillis() + ext;
            Path target = uploadDir.resolve(filename);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // URL pública (ej: http://localhost:8080/uploads/archivo.png)
            String url = "/uploads/" + filename;

            if ("avatar".equals(type)) {
                u.setAvatarUrl(url);
            } else if ("cover".equals(type)) {
                u.setCoverUrl(url);
            }

            users.save(u);

            UserDTO dto = toDTO(u);
            return ResponseEntity.ok(
                    Map.of(
                            "estado", 1,
                            "mensaje", "Imagen subida correctamente",
                            "url", url,
                            "usuario", dto
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("estado", 0, "mensaje", "Error al subir imagen", "error", e.getMessage()));
        }
    }

}
