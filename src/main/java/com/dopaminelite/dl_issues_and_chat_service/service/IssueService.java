package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueAssignRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueCreateRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueUpdateStatusRequest;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueMessage;
import com.dopaminelite.dl_issues_and_chat_service.entity.IssueNumberGenerator;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueMessageRepository;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueRepository;
import com.dopaminelite.dl_issues_and_chat_service.utils.PdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueMessageRepository issueMessageRepository;
    private final IssueNumberGenerator issueNumberGenerator;

    public Issue createIssue(IssueCreateRequest request) {
        log.debug("Creating issue for studentId: {}", request.getStudentId());
        Instant now = Instant.now();

        Issue issue = new Issue();
        issue.setStudentId(request.getStudentId());
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setStatus(IssueStatus.OPEN);
        issue.setAssignmentStatus(IssueAssignmentStatus.UNASSIGNED);
        issue.setChatReadOnly(false);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        issue.setIssueNumber(issueNumberGenerator.next());

        Issue saved = issueRepository.save(issue);
        log.debug("Created issue with id: {}", saved.getId());
        return saved;
    }

    public Page<Issue> getAllIssues(Pageable pageable) {
        log.debug("Fetching all issues with pageable: {}", pageable);
        return issueRepository.findAll(pageable);
    }

    public Page<Issue> getIssuesByStudent(UUID studentId, Pageable pageable) {
        log.debug("Fetching issues for studentId: {}, pageable: {}", studentId, pageable);
        return issueRepository.findByStudentId(studentId, pageable);
    }

    public Page<Issue> getIssuesByStudent(UUID studentId, IssueStatus status, Pageable pageable) {
        log.debug("Fetching issues for studentId: {} with optional status: {} pageable: {}", studentId, status, pageable);
        if (status != null) {
            return issueRepository.findByStudentIdAndStatus(studentId, status, pageable);
        }
        return issueRepository.findByStudentId(studentId, pageable);
    }

    public Page<Issue> getIssuesByAdminFilters(IssueStatus status,
                                               IssueAssignmentStatus assignmentStatus,
                                               UUID assignedAdminId,
                                               Pageable pageable) {
        log.debug("Fetching issues by admin filters: status: {}, assignmentStatus: {}, assignedAdminId: {}, pageable: {}",
                status, assignmentStatus, assignedAdminId, pageable);

        // If client supplied only assignedAdminId (no other filters)
        if (assignedAdminId != null && assignmentStatus == null && status == null) {
            return issueRepository.findByAssignedAdminId(assignedAdminId, pageable);
        }

        // No assigned admin provided -> handle status/assignment combinations across all admins
        if (assignedAdminId == null) {
            if (status != null && assignmentStatus == null) {
                // status only
                return issueRepository.findByStatus(status, pageable);
            }
            if (status == null && assignmentStatus != null) {
                // assignment status only
                return issueRepository.findByAssignmentStatus(assignmentStatus, pageable);
            }
            if (status != null) {
                // both status and assignmentStatus provided
                return issueRepository.findByStatusAndAssignmentStatus(status, assignmentStatus, pageable);
            }
            // fallback - should be handled earlier, but return all
            return issueRepository.findAll(pageable);
        }

        // assignedAdminId != null: handle combinations that include assigned admin
        if (assignedAdminId != null) {
            if (status != null && assignmentStatus == null) {
                return issueRepository.findByAssignedAdminIdAndStatus(assignedAdminId, status, pageable);
            }
            if (status == null && assignmentStatus != null) {
                return issueRepository.findByAssignedAdminIdAndAssignmentStatus(assignedAdminId, assignmentStatus, pageable);
            }
            // all three provided
            return issueRepository.findByStatusAndAssignmentStatusAndAssignedAdminId(status, assignmentStatus, assignedAdminId, pageable);
        }

        // default: return all
        return issueRepository.findAll(pageable);
    }

    public Optional<Issue> getIssueById(UUID issueId) {
        log.debug("Fetching issue by id: {}", issueId);
        return issueRepository.findById(issueId);
    }

    public Issue assignIssue(UUID issueId, IssueAssignRequest request) {
        log.debug("Assigning issueId: {} to adminId: {}", issueId, request.getAdminId());
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> {
                    log.error("Issue not found for id: {}", issueId);
                    return new RuntimeException("Issue not found");
                });

        issue.setAssignedAdminId(request.getAdminId());
        issue.setAssignmentStatus(IssueAssignmentStatus.ASSIGNED);
        issue.setUpdatedAt(Instant.now());

        Issue saved = issueRepository.save(issue);
        log.debug("Assigned issueId: {} to adminId: {}", saved.getId(), saved.getAssignedAdminId());
        return saved;
    }

    public Issue updateIssueStatus(UUID issueId, IssueUpdateStatusRequest request) {
        log.debug("Updating status of issueId: {} to {}", issueId, request.getStatus());
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> {
                    log.error("Issue not found for id: {}", issueId);
                    return new RuntimeException("Issue not found");
                });

        IssueStatus newStatus = request.getStatus();

        try {
            validateStatusTransition(issue.getStatus(), newStatus);
        } catch (RuntimeException e) {
            log.error("Invalid status transition for issueId: {}: {} → {}", issueId, issue.getStatus(), newStatus, e);
            throw e;
        }

        issue.setStatus(newStatus);
        if (newStatus == IssueStatus.SOLVED) {
            issue.setChatReadOnly(true);
            issue.setSolvedAt(Instant.now());
        }
        issue.setUpdatedAt(Instant.now());

        Issue saved = issueRepository.save(issue);
        log.debug("Updated issueId: {} status to {}", saved.getId(), saved.getStatus());
        return saved;
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

    public Page<IssueMessage> getMessagesByIssueId(UUID issueId, Pageable pageable) {
        log.debug("Fetching messages for issueId: {} with pageable: {}", issueId, pageable);
        return issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(issueId, pageable);
    }

    public byte[] generateIssueReport(UUID issueId) {
        log.debug("Generating PDF report for issueId: {}", issueId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> {
                    log.error("Issue not found for report generation, id: {}", issueId);
                    return new IllegalStateException("Issue not found");
                });

        if (issue.getStatus() != IssueStatus.SOLVED) {
            log.error("Cannot generate report for issueId: {} because status is {}", issueId, issue.getStatus());
            throw new IllegalStateException("Report cannot be generated unless issue is SOLVED");
        }

        List<IssueMessage> messages = issueMessageRepository.findByIssueIdOrderByCreatedAtAsc(issueId);

        log.debug("Generating PDF with {} messages for issueId: {}", messages.size(), issueId);
        return PdfGenerator.generateIssueReport(issue, messages);
    }
}
