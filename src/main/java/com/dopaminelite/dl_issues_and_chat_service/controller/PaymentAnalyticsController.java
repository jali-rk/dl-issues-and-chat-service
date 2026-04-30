package com.dopaminelite.dl_issues_and_chat_service.controller;

import com.dopaminelite.dl_issues_and_chat_service.client.PaymentServiceClient;
import com.dopaminelite.dl_issues_and_chat_service.dto.DropoutStudentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/payment/analytics")
public class PaymentAnalyticsController {

    private final PaymentServiceClient paymentServiceClient;

    @GetMapping("/dropouts")
    public ResponseEntity<List<DropoutStudentResponse>> getDropoutStudents(
            @RequestParam List<UUID> includePortalIds,
            @RequestParam(required = false) List<UUID> excludePortalIds
    ) {
        log.debug("Fetching dropout students - Include portals: {}, Exclude portals: {}", 
                includePortalIds, excludePortalIds);

        List<DropoutStudentResponse> dropoutStudents = paymentServiceClient.getDropoutStudents(
                includePortalIds, 
                excludePortalIds
        );

        return ResponseEntity.ok(dropoutStudents);
    }
}
