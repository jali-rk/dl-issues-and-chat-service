package com.dopaminelite.dl_issues_and_chat_service.entity;

import com.dopaminelite.dl_issues_and_chat_service.constants.Role;
import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dopaminelite_issue_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class IssueMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID issueId;

    @Column(nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Embedded
    private UploadedFileRef attachment;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

