package com.upiiz.platform_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ThreadDetailDto {
    private Long id;
    private String title;
    private String body;
    private String type;
    private String status;
    private int score;
    private int answersCount;
    private int views;

    private Long categoryId;
    private String categoryName;
    private Long subareaId;
    private String subareaName;

    private String authorId;
    private String authorName;

    private Instant createdAt;
    private Instant updatedAt;

    private List<PostDto> posts;
    List<AttachmentDto> attachments;

    private boolean likedByMe;
}
