package com.dopaminelite.dl_issues_and_chat_service.client;

import com.dopaminelite.dl_issues_and_chat_service.dto.DropoutStudentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;
    private final String serviceToken;
    private final String serviceName;

    public PaymentServiceClient(
            RestTemplate restTemplate,
            @Value("${payment.service.url:http://localhost:8080}") String paymentServiceUrl,
            @Value("${payment.service.token:change-me-in-production}") String serviceToken,
            @Value("${payment.service.name:issues-service}") String serviceName) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
        this.serviceToken = serviceToken;
        this.serviceName = serviceName;
    }

    public List<DropoutStudentResponse> getDropoutStudents(List<UUID> includePortalIds, List<UUID> excludePortalIds) {
        if (includePortalIds == null || includePortalIds.isEmpty()) {
            log.warn("No include portal IDs provided");
            return Collections.emptyList();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(paymentServiceUrl + "/api/v1/submissions/analytics/dropouts");

            // Add includePortalIds as multiple query parameters
            includePortalIds.forEach(id -> builder.queryParam("includePortalIds", id.toString()));

            // Add excludePortalIds as multiple query parameters
            if (excludePortalIds != null && !excludePortalIds.isEmpty()) {
                excludePortalIds.forEach(id -> builder.queryParam("excludePortalIds", id.toString()));
            }

            String url = builder.toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.set("X-Service-Name", serviceName);
            headers.set("Content-Type", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Fetching dropout students from payment service");
            log.info("Include portals: {}, Exclude portals: {}", includePortalIds, excludePortalIds);
            log.info("Using URL: {}", url);

            ResponseEntity<DropoutStudentResponse[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DropoutStudentResponse[].class
            );

            if (response.getBody() != null) {
                List<DropoutStudentResponse> students = List.of(response.getBody());
                log.info("Successfully fetched {} dropout students", students.size());
                return students;
            }

            log.error("Failed to fetch dropout students: response body was null");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching dropout students from payment service: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
