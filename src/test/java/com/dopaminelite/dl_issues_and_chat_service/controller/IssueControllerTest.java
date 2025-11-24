package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueAssignRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueCreateRequest;
import com.dopaminelite.dl_issues_and_chat_service.dto.IssueUpdateStatusRequest;
import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.service.IssueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
public class IssueControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public IssueService issueService() {
            return Mockito.mock(IssueService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IssueService issueService;

    @Test
    public void getIssue_found_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        Issue issue = Issue.builder()
                .id(id)
                .title("Test Issue")
                .description("A test description")
                .studentId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(issueService.getIssueById(eq(id))).thenReturn(Optional.of(issue));

        mockMvc.perform(get("/issues/{issueId}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Test Issue"))
                .andExpect(jsonPath("$.description").value("A test description"));
    }

    @Test
    public void getIssue_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.when(issueService.getIssueById(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/issues/{issueId}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getIssue_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/issues/{issueId}", "not-a-uuid").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getIssue_serviceThrows_propagatesException() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.when(issueService.getIssueById(eq(id))).thenThrow(new RuntimeException("boom"));

        // Expect the exception to be propagated by MockMvc (controller does not handle runtime exceptions here)
        try {
            mockMvc.perform(get("/issues/{issueId}", id).accept(MediaType.APPLICATION_JSON));
            throw new AssertionError("Expected exception was not thrown");
        } catch (Exception ex) {
            // test passes when an exception is thrown
        }
    }

    @Test
    public void createIssue_success_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        IssueCreateRequest req = new IssueCreateRequest();
        req.setStudentId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        req.setTitle("New Issue");
        req.setDescription("Create description");

        Issue saved = Issue.builder()
                .id(id)
                .studentId(req.getStudentId())
                .title(req.getTitle())
                .description(req.getDescription())
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(issueService.createIssue(any())).thenReturn(saved);

        mockMvc.perform(post("/issues").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("New Issue"));
    }

    @Test
    public void listIssues_byStudent_returns200() throws Exception {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Issue issue = Issue.builder()
                .id(UUID.randomUUID())
                .title("List Issue")
                .description("desc")
                .studentId(studentId)
                .status(IssueStatus.OPEN)
                .assignmentStatus(IssueAssignmentStatus.UNASSIGNED)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(issueService.getIssuesByStudent(eq(studentId), any()))
                .thenReturn(new PageImpl<>(List.of(issue)));

        mockMvc.perform(get("/issues").param("studentId", studentId.toString()).param("offset", "0").param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("List Issue"));
    }

    @Test
    public void assignIssue_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        IssueAssignRequest req = new IssueAssignRequest();
        req.setAdminId(UUID.fromString("00000000-0000-0000-0000-000000000004"));

        Issue assigned = Issue.builder()
                .id(id)
                .assignedAdminId(req.getAdminId())
                .assignmentStatus(IssueAssignmentStatus.ASSIGNED)
                .title("Assigned")
                .description("d")
                .studentId(UUID.randomUUID())
                .status(IssueStatus.IN_PROGRESS)
                .isChatReadOnly(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(issueService.assignIssue(eq(id), any())).thenReturn(assigned);

        mockMvc.perform(post("/issues/{issueId}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAdminId").value(req.getAdminId().toString()))
                .andExpect(jsonPath("$.assignmentStatus").value("ASSIGNED"));
    }

    @Test
    public void assignIssue_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        IssueAssignRequest req = new IssueAssignRequest();
        req.setAdminId(UUID.randomUUID());

        Mockito.when(issueService.assignIssue(eq(id), any())).thenThrow(new RuntimeException("Issue not found"));

        mockMvc.perform(post("/issues/{issueId}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateIssueStatus_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        IssueUpdateStatusRequest req = new IssueUpdateStatusRequest();
        req.setStatus(IssueStatus.SOLVED);

        Issue updated = Issue.builder()
                .id(id)
                .status(IssueStatus.SOLVED)
                .assignmentStatus(IssueAssignmentStatus.ASSIGNED)
                .isChatReadOnly(true)
                .solvedAt(Instant.now())
                .title("t")
                .description("d")
                .studentId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(issueService.updateIssueStatus(eq(id), any())).thenReturn(updated);

        String content = mockMvc.perform(patch("/issues/{issueId}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> map = objectMapper.readValue(content, Map.class);
        // status should be the enum name
        assert map.get("status").toString().equals("SOLVED");
        // the boolean field might be serialized as 'chatReadOnly' or 'isChatReadOnly' depending on Jackson configuration
        boolean chatReadOnly = false;
        if (map.containsKey("chatReadOnly")) {
            chatReadOnly = Boolean.parseBoolean(map.get("chatReadOnly").toString());
        } else if (map.containsKey("isChatReadOnly")) {
            chatReadOnly = Boolean.parseBoolean(map.get("isChatReadOnly").toString());
        } else {
            throw new AssertionError("chatReadOnly field not found in response");
        }
        if (!chatReadOnly) throw new AssertionError("Expected chatReadOnly=true");
    }

    @Test
    public void updateIssueStatus_invalidTransition_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        IssueUpdateStatusRequest req = new IssueUpdateStatusRequest();
        req.setStatus(IssueStatus.OPEN);

        Mockito.when(issueService.updateIssueStatus(eq(id), any()))
                .thenThrow(new RuntimeException("Invalid status transition from IN_PROGRESS to OPEN"));

        mockMvc.perform(patch("/issues/{issueId}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateIssueStatus_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        IssueUpdateStatusRequest req = new IssueUpdateStatusRequest();
        req.setStatus(IssueStatus.SOLVED);

        Mockito.when(issueService.updateIssueStatus(eq(id), any()))
                .thenThrow(new RuntimeException("Issue not found"));

        mockMvc.perform(patch("/issues/{issueId}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
