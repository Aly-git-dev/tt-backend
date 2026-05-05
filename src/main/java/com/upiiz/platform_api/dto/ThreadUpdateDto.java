package com.upiiz.platform_api.dto;

import lombok.Data;

@Data
public class ThreadUpdateDto {
    private String title;
    private String body;
    private Long categoryId;
    private Long subareaId;
    private String type;
}