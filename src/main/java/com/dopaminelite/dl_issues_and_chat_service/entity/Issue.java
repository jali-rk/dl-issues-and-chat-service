package com.dopaminelite.dl_issues_and_chat_service.entity;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.dto.UploadedFileRef;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dopaminelite_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Issue {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, updatable = false)
    private Long issueNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private UUID studentId;

    private UUID assignedAdminId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.OPEN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueAssignmentStatus assignmentStatus = IssueAssignmentStatus.UNASSIGNED;

    @Builder.Default
    @Column(nullable = false)
    private boolean isChatReadOnly = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant solvedAt;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "dopaminelite_issue_attachments", joinColumns = @JoinColumn(name = "issue_id"))
    private List<UploadedFileRef> attachments = new ArrayList<>();

}
