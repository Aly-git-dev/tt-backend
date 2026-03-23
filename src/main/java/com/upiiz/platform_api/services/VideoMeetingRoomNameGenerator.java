package com.upiiz.platform_api.services;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Component
public class VideoMeetingRoomNameGenerator {

    public String generate(String title, UUID appointmentId) {
        String slug = slugify(title);
        String shortHash = UUID.randomUUID().toString().substring(0, 6);
        return "upiiz-" + slug + "-" + appointmentId.toString().substring(0, 8) + "-" + shortHash;
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) return "meeting";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
    }
}