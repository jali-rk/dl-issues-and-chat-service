package com.dopaminelite.dl_issues_and_chat_service.repository;

import com.dopaminelite.dl_issues_and_chat_service.entity.Issue;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueStatus;
import com.dopaminelite.dl_issues_and_chat_service.constants.IssueAssignmentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

    Page<Issue> findByStudentId(UUID studentId, Pageable pageable);

    Page<Issue> findByStatusAndAssignmentStatusAndAssignedAdminId(
            IssueStatus status,
            IssueAssignmentStatus assignmentStatus,
            UUID assignedAdminId,
            Pageable pageable
    );

    Optional<Issue> findById(UUID id);

    Page<Issue> findByAssignedAdminId(UUID adminId, Pageable pageable);

    Page<Issue> findByStatusAndAssignmentStatus(
            IssueStatus status,
            IssueAssignmentStatus assignmentStatus,
            Pageable pageable
    );

}
