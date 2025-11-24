// ...existing code...
package com.dopaminelite.dl_issues_and_chat_service.service;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueCreateRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueUpdateStatusRequest;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private IssueService issueService;

    private Issue baseIssue;

    @BeforeEach
    void setUp() {
        baseIssue = Issue.builder()
                .id(UUID.randomUUID())
                .title("base")
                .description("d")
                .studentId(UUID.randomUUID())
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createIssue_populatesFieldsAndSaves() {
        IssueCreateRequest req = new IssueCreateRequest();
        req.setStudentId(UUID.randomUUID());
        req.setTitle("New");
        req.setDescription("desc");

        Issue saved = Issue.builder()
                .id(UUID.randomUUID())
                .studentId(req.getStudentId())
                .title(req.getTitle())
                .description(req.getDescription())
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(issueRepository.save(any(Issue.class))).thenReturn(saved);

        Issue result = issueService.createIssue(req);

        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        assertEquals(req.getTitle(), result.getTitle());
        assertEquals(IssueStatus.OPEN, result.getStatus());
        assertEquals(IssueAssignmentStatus.UNASSIGNED, result.getAssignmentStatus());
        verify(issueRepository, times(1)).save(any(Issue.class));
    }

    @Test
    void getIssuesByStudent_delegatesToRepository() {
        UUID studentId = UUID.randomUUID();
        when(issueRepository.findByStudentId(eq(studentId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(baseIssue)));

        var page = issueService.getIssuesByStudent(studentId, PageRequest.of(0, 10));
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(baseIssue.getTitle(), page.getContent().get(0).getTitle());
    }

    @Test
    void assignIssue_existing_assignsAndSaves() {
        UUID id = baseIssue.getId();
        UUID adminId = UUID.randomUUID();
        when(issueRepository.findById(eq(id))).thenReturn(Optional.of(baseIssue));

        Issue updated = Issue.builder()
                .id(id)
                .assignedAdminId(adminId)
                .assignmentStatus(IssueAssignmentStatus.ASSIGNED)
                .title(baseIssue.getTitle())
                .description(baseIssue.getDescription())
                .studentId(baseIssue.getStudentId())
                .status(IssueStatus.IN_PROGRESS)
                .createdAt(baseIssue.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(issueRepository.save(any(Issue.class))).thenReturn(updated);

        var req = new com.dopaminelite.dl_issues_and_chat_service.dto.IssueAssignRequest();
        req.setAdminId(adminId);

        Issue res = issueService.assignIssue(id, req);

        assertEquals(adminId, res.getAssignedAdminId());
        assertEquals(IssueAssignmentStatus.ASSIGNED, res.getAssignmentStatus());
        verify(issueRepository, times(1)).findById(eq(id));
        verify(issueRepository, times(1)).save(any(Issue.class));
    }

    @Test
    void assignIssue_missing_throws() {
        UUID id = UUID.randomUUID();
        when(issueRepository.findById(eq(id))).thenReturn(Optional.empty());

        var req = new com.dopaminelite.dl_issues_and_chat_service.dto.IssueAssignRequest();
        req.setAdminId(UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> issueService.assignIssue(id, req));
        verify(issueRepository, times(1)).findById(eq(id));
    }

    @Test
    void updateIssueStatus_validTransition_updatesAndSaves() {
        UUID id = baseIssue.getId();
        Issue current = Issue.builder()
                .id(id)
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .studentId(baseIssue.getStudentId())
                .title(baseIssue.getTitle())
                .description(baseIssue.getDescription())
                .createdAt(baseIssue.getCreatedAt())
                .updatedAt(baseIssue.getUpdatedAt())
                .build();

        when(issueRepository.findById(eq(id))).thenReturn(Optional.of(current));

        Issue after = Issue.builder()
                .id(id)
                .status(IssueStatus.SOLVED)
                .isChatReadOnly(true)
                .solvedAt(Instant.now())
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .studentId(current.getStudentId())
                .title(current.getTitle())
                .description(current.getDescription())
                .createdAt(current.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(issueRepository.save(any(Issue.class))).thenReturn(after);

        IssueUpdateStatusRequest req = new IssueUpdateStatusRequest();
        req.setStatus(IssueStatus.SOLVED);

        Issue res = issueService.updateIssueStatus(id, req);

        assertEquals(IssueStatus.SOLVED, res.getStatus());
        assertTrue(res.isChatReadOnly());
        assertNotNull(res.getSolvedAt());
        verify(issueRepository, times(1)).findById(eq(id));
        verify(issueRepository, times(1)).save(any(Issue.class));
    }

    @Test
    void updateIssueStatus_invalidTransition_throws() {
        UUID id = baseIssue.getId();
        Issue current = Issue.builder()
                .id(id)
                .status(IssueStatus.SOLVED)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .studentId(baseIssue.getStudentId())
                .title(baseIssue.getTitle())
                .description(baseIssue.getDescription())
                .createdAt(baseIssue.getCreatedAt())
                .updatedAt(baseIssue.getUpdatedAt())
                .build();

        when(issueRepository.findById(eq(id))).thenReturn(Optional.of(current));

        IssueUpdateStatusRequest req = new IssueUpdateStatusRequest();
        req.setStatus(IssueStatus.IN_PROGRESS);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> issueService.updateIssueStatus(id, req));
        assertTrue(ex.getMessage().contains("Cannot transition from SOLVED") || ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    void updateIssueStatus_missing_throws() {
        UUID id = UUID.randomUUID();
        when(issueRepository.findById(eq(id))).thenReturn(Optional.empty());

        IssueUpdateStatusRequest req = new IssueUpdateStatusRequest();
        req.setStatus(IssueStatus.SOLVED);

        assertThrows(RuntimeException.class, () -> issueService.updateIssueStatus(id, req));
    }
}

