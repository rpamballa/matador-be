package com.matador.identity;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matador.identity.internal.VerificationSessionRepository;
import com.matador.support.AbstractIntegrationTest;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class VerificationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationSessionRepository sessions;

    @Test
    void startVerificationThenWebhookMarksCustomerVerified() throws Exception {
        String token = registerCustomer();
        UUID customerId = UUID.fromString(readProfile(token).get("id").asText());

        // Start verification -> session created, client_secret returned.
        mockMvc
            .perform(post("/api/customer/me/verification").header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.client_secret").exists())
            .andExpect(jsonPath("$.status").value("CREATED"));

        // The VerificationStarted event advances the customer to IN_PROGRESS.
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(
                () ->
                    org.assertj.core.api.Assertions.assertThat(readProfileStatus(token))
                        .isEqualTo("IN_PROGRESS"));

        // Simulate the provider webhook reporting a successful verification.
        String providerSessionId =
            sessions
                .findFirstByCustomerIdOrderByCreatedAtDesc(customerId)
                .orElseThrow()
                .getProviderSessionId();
        String webhook =
            """
            {
              "provider_session_id": "%s",
              "status": "VERIFIED",
              "license_number": "D9988776",
              "license_state": "NC",
              "license_expires_on": "2030-12-31"
            }
            """
                .formatted(providerSessionId);
        mockMvc
            .perform(
                post("/api/webhooks/stripe-identity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(webhook))
            .andExpect(status().isOk());

        // The VerificationCompleted event advances the customer to VERIFIED and enables booking.
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(
                () -> {
                    JsonNode profile = readProfile(token);
                    org.assertj.core.api.Assertions.assertThat(
                            profile.get("verification_status").asText())
                        .isEqualTo("VERIFIED");
                    org.assertj.core.api.Assertions.assertThat(profile.get("can_book").asBoolean())
                        .isTrue();
                });
    }

    private String registerCustomer() throws Exception {
        String body =
            """
            {
              "email": "verify@example.com",
              "phone": "+19195559000",
              "password": "SuperSecret123",
              "first_name": "Val",
              "last_name": "Id",
              "date_of_birth": "1992-03-04"
            }
            """;
        MvcResult result =
            mockMvc
                .perform(
                    post("/api/customer/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("access_token")
            .asText();
    }

    private JsonNode readProfile(String token) throws Exception {
        MvcResult result =
            mockMvc
                .perform(get("/api/customer/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String readProfileStatus(String token) throws Exception {
        return readProfile(token).get("verification_status").asText();
    }
}
