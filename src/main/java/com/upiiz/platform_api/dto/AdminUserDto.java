package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminUserDto {
    public UUID id;
    public String emailInst;
    public String fullName;
    public Boolean active;
    public Boolean approved;
    public Boolean emailVerified;
    public Instant createdAt;
    public List<String> roles;
}