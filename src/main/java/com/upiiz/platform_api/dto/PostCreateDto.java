package com.upiiz.platform_api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class PostCreateDto {
    @JsonAlias({"content", "message", "text"})
    private String body;
    private Long parentPostId; // opcional
    private List<AttachmentDto> attachments;
}
