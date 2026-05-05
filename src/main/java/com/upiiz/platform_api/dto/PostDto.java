package com.upiiz.platform_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PostDto {
    private Long id;
    private String body;
    private String status;
    private int score;
    private boolean acceptedAnswer;

    private String authorId;
    private String authorName;

    private Long parentPostId;
    private Instant createdAt;

    private List<AttachmentDto> attachments;

    private Instant updatedAt;
    private boolean likedByMe;
}
