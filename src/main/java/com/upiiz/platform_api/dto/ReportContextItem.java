package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReportContextItem {
    public short index;
    public Long messageId;
    public UUID senderIdSnapshot;
    public String senderRoleSnapshot;
    public String contentTypeSnapshot;
    public String contentSnapshot;
    public Instant createdAtSnapshot;
}
