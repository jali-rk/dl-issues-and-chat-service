package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IssueService {

    private final IssueRepository issueRepository;

    @Autowired
    public IssueService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public Issue createIssue(Issue issue) {
        issue.setStatus(IssueStatus.OPEN);
        issue.setAssignmentStatus(IssueAssignmentStatus.UNASSIGNED);
        issue.setCreatedAt(Instant.now());
        issue.setUpdatedAt(Instant.now());
        return issueRepository.save(issue);
    }

    public Page<Issue> getIssuesByStudent(UUID studentId, Pageable pageable) {
        return issueRepository.findByStudentId(studentId, pageable);
    }

    public Page<Issue> getIssuesByAdminFilters(IssueStatus status, IssueAssignmentStatus assignmentStatus,
                                               UUID assignedAdminId, Pageable pageable) {
        return issueRepository.findByStatusAndAssignmentStatusAndAssignedAdminId(status, assignmentStatus,
                assignedAdminId, pageable);
    }

    public Optional<Issue> getIssueById(UUID issueId) {
        return issueRepository.findById(issueId);
    }

    public Issue assignIssue(UUID issueId, UUID adminId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        issue.setAssignedAdminId(adminId);
        issue.setAssignmentStatus(IssueAssignmentStatus.ASSIGNED);
        issue.setUpdatedAt(Instant.now());
        return issueRepository.save(issue);
    }

    public Issue updateIssueStatus(UUID issueId, IssueStatus newStatus) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        // Validate status transition
        if (issue.getStatus() == IssueStatus.OPEN && newStatus != IssueStatus.IN_PROGRESS &&
                newStatus != IssueStatus.SOLVED) {
            throw new RuntimeException("Invalid status transition");
        }
        if (issue.getStatus() == IssueStatus.IN_PROGRESS && newStatus != IssueStatus.SOLVED) {
            throw new RuntimeException("Invalid status transition");
        }

        issue.setStatus(newStatus);
        if (newStatus == IssueStatus.SOLVED) {
            issue.setChatReadOnly(true);
            issue.setSolvedAt(Instant.now());
        }
        issue.setUpdatedAt(Instant.now());
        return issueRepository.save(issue);
    }
}
