package com.matador.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matador.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class CustomerApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registerBody(String email, String phone, String password, String dob) {
        return """
            {
              "email": "%s",
              "phone": "%s",
              "password": "%s",
              "first_name": "Jane",
              "last_name": "Doe",
              "date_of_birth": "%s"
            }
            """
            .formatted(email, phone, password, dob);
    }

    private String register(String email, String phone) throws Exception {
        MvcResult result =
            mockMvc
                .perform(
                    post("/api/customer/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, phone, "SuperSecret123", "1990-05-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("access_token").asText();
    }

    @Test
    void registerThenFetchProfile() throws Exception {
        String token = register("jane1@example.com", "+19195550101");
        mockMvc
            .perform(get("/api/customer/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("jane1@example.com"))
            .andExpect(jsonPath("$.verification_status").value("UNVERIFIED"))
            .andExpect(jsonPath("$.can_book").value(false));
    }

    @Test
    void rejectsUnderageRegistration() throws Exception {
        mockMvc
            .perform(
                post("/api/customer/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody("kid@example.com", "+19195550102", "SuperSecret123", "2015-01-01")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("UNDERAGE"));
    }

    @Test
    void rejectsWeakPassword() throws Exception {
        mockMvc
            .perform(
                post("/api/customer/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody("weak@example.com", "+19195550103", "short", "1990-01-01")))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        register("dup@example.com", "+19195550104");
        mockMvc
            .perform(
                post("/api/customer/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody("dup@example.com", "+19195550105", "SuperSecret123", "1990-01-01")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        register("login@example.com", "+19195550106");
        mockMvc
            .perform(
                post("/api/customer/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"login@example.com\",\"password\":\"WrongPassword1\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAuthenticationForProfile() throws Exception {
        mockMvc.perform(get("/api/customer/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void addAndListAddresses() throws Exception {
        String token = register("addr@example.com", "+19195550107");
        String addressBody =
            """
            {
              "label": "Home",
              "line1": "123 Main St",
              "city": "Raleigh",
              "state": "NC",
              "postal_code": "27601",
              "country": "US",
              "lat": 35.7796,
              "lng": -78.6382,
              "is_default": true
            }
            """;
        mockMvc
            .perform(
                post("/api/customer/me/addresses")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(addressBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.city").value("Raleigh"))
            .andExpect(jsonPath("$.is_default").value(true));

        MvcResult list =
            mockMvc
                .perform(
                    get("/api/customer/me/addresses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = objectMapper.readTree(list.getResponse().getContentAsString());
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).get("lat").asDouble()).isEqualTo(35.7796);
    }
}
