package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.dto.*;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/issues")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(@Valid @RequestBody IssueCreateRequest request) {
        Issue issue = issueService.createIssue(request);
        return new ResponseEntity<>(IssueResponse.fromDomain(issue), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<IssueResponse>> listIssues(
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

        Page<IssueResponse> response = issues.map(IssueResponse::fromDomain);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{issueId}")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable UUID issueId) {
        return issueService.getIssueById(issueId)
                .map(IssueResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{issueId}/assign")
    public ResponseEntity<IssueResponse> assignIssue(
            @PathVariable UUID issueId,
            @Valid @RequestBody IssueAssignRequest request
    ) {
        try {
            Issue assignedIssue = issueService.assignIssue(issueId, request);
            return ResponseEntity.ok(IssueResponse.fromDomain(assignedIssue));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PatchMapping("/{issueId}/status")
    public ResponseEntity<IssueResponse> updateIssueStatus(
            @PathVariable UUID issueId,
            @Valid @RequestBody IssueUpdateStatusRequest request
    ) {
        try {
            Issue updatedIssue = issueService.updateIssueStatus(issueId, request);
            return ResponseEntity.ok(IssueResponse.fromDomain(updatedIssue));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Cannot transition from") || msg.contains("Invalid status transition")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
