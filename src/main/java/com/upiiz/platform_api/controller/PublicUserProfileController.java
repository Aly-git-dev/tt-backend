// controller/PublicUserProfileController.java
package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.PublicUserProfileDto;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.ForumPostRepository;
import com.upiiz.platform_api.repositories.ForumThreadRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/users")
@RequiredArgsConstructor
@Tag(name = "Perfiles publicos", description = "Consulta de perfiles publicos de usuarios")
@SecurityRequirement(name = "bearer-jwt")
public class PublicUserProfileController {

    private final UserRepository userRepo;
    private final ForumThreadRepository threadRepo;
    private final ForumPostRepository postRepo;

    @GetMapping("/{id}/profile")
    @Operation(summary = "Obtener perfil publico", description = "Devuelve informacion publica y actividad de foros de un usuario.")
    public PublicUserProfileDto getPublicProfile(@PathVariable UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return PublicUserProfileDto.builder()
                .id(user.getId())
                .fullName(user.getNombre())
                .emailInst(user.getEmailInst())
                .carrera(user.getCarrera())
                .bio(user.getBio())
                .interests(user.getInterests())
                .links(user.getLinks())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .threadsCount(threadRepo.countByAuthorId(user.getId()))
                .postsCount(postRepo.countByAuthorId(user.getId()))
                .build();
    }
}
