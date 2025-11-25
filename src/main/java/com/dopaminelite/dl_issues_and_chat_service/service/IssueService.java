package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueAssignRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueCreateRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueUpdateStatusRequest;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;

    public Issue createIssue(IssueCreateRequest request) {
        Issue issue = new Issue();
        issue.setStudentId(request.getStudentId());
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setStatus(IssueStatus.OPEN);
        issue.setAssignmentStatus(IssueAssignmentStatus.UNASSIGNED);
        issue.setChatReadOnly(false);
        Instant now = Instant.now();
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        // TODO: handle attachments separately if needed
        return issueRepository.save(issue);
    }

    public Page<Issue> getIssuesByStudent(UUID studentId, Pageable pageable) {
        return issueRepository.findByStudentId(studentId, pageable);
    }

    public Page<Issue> getIssuesByAdminFilters(IssueStatus status,
                                               IssueAssignmentStatus assignmentStatus,
                                               UUID assignedAdminId,
                                               Pageable pageable) {
        if (assignedAdminId != null && assignmentStatus == null && status == null) {
            return issueRepository.findByAssignedAdminId(assignedAdminId, pageable);
        }
        if (assignedAdminId == null) {
            assignmentStatus = IssueAssignmentStatus.UNASSIGNED;
            status = IssueStatus.OPEN;
            return issueRepository.findByStatusAndAssignmentStatus(status, assignmentStatus, pageable);
        }
        return issueRepository.findByStatusAndAssignmentStatusAndAssignedAdminId(status, assignmentStatus,
                assignedAdminId, pageable);
    }

    public Optional<Issue> getIssueById(UUID issueId) {
        return issueRepository.findById(issueId);
    }

    public Issue assignIssue(UUID issueId, IssueAssignRequest request) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        issue.setAssignedAdminId(request.getAdminId());
        issue.setAssignmentStatus(IssueAssignmentStatus.ASSIGNED);
        issue.setUpdatedAt(Instant.now());
        return issueRepository.save(issue);
    }

    public Issue updateIssueStatus(UUID issueId, IssueUpdateStatusRequest request) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        IssueStatus newStatus = request.getStatus();

        // Validate status transition
        validateStatusTransition(issue.getStatus(), newStatus);

        issue.setStatus(newStatus);
        if (newStatus == IssueStatus.SOLVED) {
            issue.setChatReadOnly(true);
            issue.setSolvedAt(Instant.now());
        }
        issue.setUpdatedAt(Instant.now());
        return issueRepository.save(issue);
    }

    private void validateStatusTransition(IssueStatus current, IssueStatus next) {
        switch (current) {
            case OPEN -> {
                if (next != IssueStatus.IN_PROGRESS && next != IssueStatus.SOLVED) {
                    throw new RuntimeException("Invalid status transition from OPEN to " + next);
                }
            }
            case IN_PROGRESS -> {
                if (next != IssueStatus.SOLVED) {
                    throw new RuntimeException("Invalid status transition from IN_PROGRESS to " + next);
                }
            }
            case SOLVED -> throw new RuntimeException("Cannot transition from SOLVED");
        }
    }
}
