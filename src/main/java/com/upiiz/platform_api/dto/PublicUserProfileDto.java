// dto/PublicUserProfileDto.java
package com.upiiz.platform_api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PublicUserProfileDto {
    private UUID id;
    private String fullName;
    private String emailInst;
    private String carrera;
    private String bio;
    private String interests;
    private String links;
    private String avatarUrl;
    private String coverUrl;
    private long threadsCount;
    private long postsCount;
}