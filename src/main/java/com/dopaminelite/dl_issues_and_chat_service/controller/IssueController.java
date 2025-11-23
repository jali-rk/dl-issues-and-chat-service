package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/issues")
public class IssueController {

    private final IssueService issueService;

    @Autowired
    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    // Create a new issue (POST /issues)
    @PostMapping
    public ResponseEntity<Issue> createIssue(@Valid @RequestBody Issue issueRequest) {
        Issue createdIssue = issueService.createIssue(issueRequest);
        return new ResponseEntity<>(createdIssue, HttpStatus.CREATED);
    }

    // List issues with optional student filter (GET /issues)
    @GetMapping
    public ResponseEntity<Page<Issue>> listIssues(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssueAssignmentStatus assignmentStatus,
            @RequestParam(required = false) UUID assignedAdminId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit
    ) {
        PageRequest pageable = PageRequest.of(offset, limit);

        Page<Issue> issues;
        if (studentId != null) {
            issues = issueService.getIssuesByStudent(studentId, pageable);
        } else {
            issues = issueService.getIssuesByAdminFilters(status, assignmentStatus, assignedAdminId, pageable);
        }

        return ResponseEntity.ok(issues);
    }

    // Get issue details (GET /issues/{issueId})
    @GetMapping("/{issueId}")
    public ResponseEntity<Issue> getIssue(@PathVariable UUID issueId) {
        Optional<Issue> issue = issueService.getIssueById(issueId);
        return issue.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Assign an issue to an admin (POST /issues/{issueId}/assign)
    @PostMapping("/{issueId}/assign")
    public ResponseEntity<Issue> assignIssue(
            @PathVariable UUID issueId,
            @RequestParam UUID adminId
    ) {
        try {
            Issue assignedIssue = issueService.assignIssue(issueId, adminId);
            return ResponseEntity.ok(assignedIssue);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Update issue status (PATCH /issues/{issueId}/status)
    @PatchMapping("/{issueId}/status")
    public ResponseEntity<Issue> updateIssueStatus(
            @PathVariable UUID issueId,
            @RequestParam IssueStatus newStatus
    ) {
        try {
            Issue updatedIssue = issueService.updateIssueStatus(issueId, newStatus);
            return ResponseEntity.ok(updatedIssue);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid status transition")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
