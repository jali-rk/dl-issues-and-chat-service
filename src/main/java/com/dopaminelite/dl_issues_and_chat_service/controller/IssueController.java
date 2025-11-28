package com.dopaminelite.dl_issues_and_chat_service.controller;

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
            @Valid IssueFilterRequest filter,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit
    ) {
        PageRequest pageable = PageRequest.of(offset, limit);

        Page<Issue> issues = resolveFilterQuery(filter, pageable);

        Page<IssueResponse> response = issues.map(IssueResponse::fromDomain);
        return ResponseEntity.ok(response);
    }

    private Page<Issue> resolveFilterQuery(IssueFilterRequest filter, PageRequest pageable) {
        if (filter.getStudentId() != null) {
            return issueService.getIssuesByStudent(filter.getStudentId(), pageable);
        }

        boolean noAdminFilters =
                filter.getStatus() == null &&
                        filter.getAssignmentStatus() == null &&
                        filter.getAssignedAdminId() == null;

        if (noAdminFilters) {
            return issueService.getAllIssues(pageable);
        }

        return issueService.getIssuesByAdminFilters(
                filter.getStatus(),
                filter.getAssignmentStatus(),
                filter.getAssignedAdminId(),
                pageable
        );
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

    @GetMapping("/{issueId}/messages")
    public ResponseEntity<?> listIssueMessages(
            @PathVariable UUID issueId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        // Check if issue exists
        if (issueService.getIssueById(issueId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorObject("Issue not found"));
        }
        PageRequest pageable = PageRequest.of(offset, limit);
        var messages = issueService.getMessagesByIssueId(issueId, pageable);
        IssueMessageListResponse response = IssueMessageListResponse.from(messages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{issueId}/report")
    public ResponseEntity<?> downloadIssueReport(@PathVariable UUID issueId) {
        var issueOpt = issueService.getIssueById(issueId);
        if (issueOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorObject("Issue not found"));
        }
        try {
            byte[] pdf = issueService.generateIssueReport(issueId);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=issue-report.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorObject(e.getMessage()));
        }
    }
}
