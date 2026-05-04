package com.dopaminelite.dl_issues_and_chat_service.client;

import com.dopaminelite.dl_issues_and_chat_service.dto.DropoutStudentResponse;
import com.dopaminelite.dl_issues_and_chat_service.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final UserServiceClient userServiceClient;
    
    @Value("${payment.service.url:http://localhost:8080}")
    private String paymentServiceUrl;
    
    @Value("${payment.service.token:change-me-in-production}")
    private String serviceToken;
    
    @Value("${payment.service.name:issues-service}")
    private String serviceName;

    /**
     * Fetches dropout students from Payment Service and enriches with user details.
     * 
     * @param includePortalIds portals where students must have APPROVED payments
     * @param excludePortalIds portals where students must NOT have APPROVED payments
     * @return list of dropout students with full user details
     */
    public List<DropoutStudentResponse> getDropoutStudents(List<UUID> includePortalIds, List<UUID> excludePortalIds) {
        if (includePortalIds == null || includePortalIds.isEmpty()) {
            log.warn("No include portal IDs provided for dropout analysis");
            return Collections.emptyList();
        }

        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(paymentServiceUrl + "/api/v1/submissions/analytics/dropouts");

            includePortalIds.forEach(id -> builder.queryParam("includePortalIds", id.toString()));
            
            if (excludePortalIds != null && !excludePortalIds.isEmpty()) {
                excludePortalIds.forEach(id -> builder.queryParam("excludePortalIds", id.toString()));
            }

            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.set("X-Service-Name", serviceName);
            headers.set("Content-Type", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Call Payment Service
            ResponseEntity<UUID[]> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    UUID[].class
            );

            if (response.getBody() == null || response.getBody().length == 0) {
                log.debug("Payment Service returned no dropout students");
                return Collections.emptyList();
            }

            UUID[] studentIds = response.getBody();
            log.debug("Fetched {} dropout student IDs from Payment Service", studentIds.length);
            
            // Fetch user details from User Service
            List<UUID> studentIdList = Arrays.asList(studentIds);
            Map<UUID, UserInfo> userMap = userServiceClient.fetchUsersByIds(studentIdList);
            
            // Map to DropoutStudentResponse with full user details
            List<DropoutStudentResponse> students = new ArrayList<>();
            for (UUID studentId : studentIds) {
                UserInfo userInfo = userMap.get(studentId);
                if (userInfo != null) {
                    DropoutStudentResponse dto = new DropoutStudentResponse();
                    dto.setStudentId(studentId);
                    dto.setFullName(userInfo.getFullName());
                    dto.setEmail(userInfo.getEmail());
                    dto.setWhatsappNumber(userInfo.getWhatsappNumber());
                    dto.setCodeNumber(userInfo.getCodeNumber());
                    students.add(dto);
                } else {
                    log.warn("User info not found for student ID: {}", studentId);
                }
            }
            
            log.debug("Returning {} dropout students with full details", students.size());
            return students;
            
        } catch (Exception e) {
            log.error("Failed to fetch dropout students from Payment Service: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
