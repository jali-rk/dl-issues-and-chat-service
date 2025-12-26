package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.dto.*;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/issues")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(@Valid @RequestBody IssueCreateRequest request) {
        log.debug("Creating issue for studentId: {}", request.getStudentId());
        Issue issue = issueService.createIssue(request);
        return new ResponseEntity<>(IssueResponse.fromDomain(issue), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<IssueResponse>> listIssues(
            @Valid IssueFilterRequest filter,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.debug("Listing issues with filters: {}, offset: {}, limit: {}", filter, offset, limit);

        // Default sort: createdAt DESC
        PageRequest pageable = PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Issue> issues = resolveFilterQuery(filter, pageable);
        Page<IssueResponse> response = issues.map(IssueResponse::fromDomain);

        return ResponseEntity.ok(response);
    }

    private Page<Issue> resolveFilterQuery(IssueFilterRequest filter, PageRequest pageable) {
        if (filter.getStudentId() != null) {
            log.debug("Fetching issues for studentId: {} with optional status: {}", filter.getStudentId(), filter.getStatus());
            // Use the overloaded service method that accepts an optional status
            return issueService.getIssuesByStudent(filter.getStudentId(), filter.getStatus(), pageable);
        }

        boolean noAdminFilters =
                filter.getStatus() == null &&
                        filter.getAssignmentStatus() == null &&
                        filter.getAssignedAdminId() == null;

        if (noAdminFilters) {
            log.debug("Fetching all issues without filters");
            return issueService.getAllIssues(pageable);
        }

        log.debug("Fetching issues using admin filters: status: {}, assignmentStatus: {}, assignedAdminId: {}",
                filter.getStatus(), filter.getAssignmentStatus(), filter.getAssignedAdminId());

        return issueService.getIssuesByAdminFilters(
                filter.getStatus(),
                filter.getAssignmentStatus(),
                filter.getAssignedAdminId(),
                pageable
        );
    }

    @GetMapping("/{issueId}")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable UUID issueId) {
        log.debug("Fetching issue with issueId: {}", issueId);

        return issueService.getIssueById(issueId)
                .map(IssueResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.error("Issue not found: issueId: {}", issueId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @PostMapping("/{issueId}/assign")
    public ResponseEntity<IssueResponse> assignIssue(
            @PathVariable UUID issueId,
            @Valid @RequestBody IssueAssignRequest request
    ) {
        log.debug("Assigning issueId: {} to adminId: {}", issueId, request.getAdminId());
        try {
            Issue assignedIssue = issueService.assignIssue(issueId, request);
            return ResponseEntity.ok(IssueResponse.fromDomain(assignedIssue));
        } catch (RuntimeException e) {
            log.error("Failed to assign issueId: {}: {}", issueId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PatchMapping("/{issueId}/status")
    public ResponseEntity<IssueResponse> updateIssueStatus(
            @PathVariable UUID issueId,
            @Valid @RequestBody IssueUpdateStatusRequest request
    ) {
        log.debug("Updating status of issueId: {} to {}", issueId, request.getStatus());
        try {
            Issue updatedIssue = issueService.updateIssueStatus(issueId, request);
            return ResponseEntity.ok(IssueResponse.fromDomain(updatedIssue));
        } catch (RuntimeException e) {
            log.error("Failed to update status for issueId: {}: {}", issueId, e.getMessage());
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
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.debug("Listing messages for issueId: {}, offset: {}, limit: {}", issueId, offset, limit);

        if (issueService.getIssueById(issueId).isEmpty()) {
            log.error("Issue not found when listing messages: issueId: {}", issueId);
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
        log.debug("Downloading report for issueId: {}", issueId);

        var issueOpt = issueService.getIssueById(issueId);
        if (issueOpt.isEmpty()) {
            log.error("Issue not found when generating report: issueId: {}", issueId);
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
            log.error("Failed to generate report for issueId: {}: {}", issueId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorObject(e.getMessage()));
        }
    }
}
