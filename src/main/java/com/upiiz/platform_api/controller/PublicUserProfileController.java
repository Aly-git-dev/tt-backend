// controller/PublicUserProfileController.java
package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.PublicUserProfileDto;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.ForumPostRepository;
import com.upiiz.platform_api.repositories.ForumThreadRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://148.204.142.20:3030")
public class PublicUserProfileController {

    private final UserRepository userRepo;
    private final ForumThreadRepository threadRepo;
    private final ForumPostRepository postRepo;

    @GetMapping("/{id}/profile")
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