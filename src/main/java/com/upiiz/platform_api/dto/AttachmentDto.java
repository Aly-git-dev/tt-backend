package com.upiiz.platform_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentDto {
    private Long id;
    private String kind;
    private String url;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
}
