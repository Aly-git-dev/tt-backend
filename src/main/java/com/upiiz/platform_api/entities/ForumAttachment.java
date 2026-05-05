package com.upiiz.platform_api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "forum_attachment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ForumAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private ForumThread thread;

    @Column(length = 20, nullable = false)
    private String kind; // IMAGEN / VIDEO / AUDIO / LINK

    @Column(columnDefinition = "text", nullable = false)
    private String url;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
