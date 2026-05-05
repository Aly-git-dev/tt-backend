package com.upiiz.platform_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ThreadSummaryDto {
    private Long id;
    private String title;
    private String categoryName;
    private String subareaName;
    private String type;
    private int score;
    private int answersCount;
    private int views;
    private String status;
    private Instant createdAt;
    private boolean likedByMe;
}
