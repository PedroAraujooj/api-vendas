package com.exemplo.authservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"auth.username=aluno", "auth.password=Prova123!", "eureka.client.enabled=false"})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void loginRefreshAndReplayProtection() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"aluno\",\"password\":\"Prova123!\"}"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("token_type").value("Bearer")).andReturn();
        JsonNode original = mapper.readTree(login.getResponse().getContentAsString());
        String body = mapper.createObjectNode().put("refresh_token", original.get("refresh_token").asText()).toString();
        var refreshed = mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn();
        JsonNode renewed = mapper.readTree(refreshed.getResponse().getContentAsString());
        assertThat(renewed.get("access_token")).isNotEqualTo(original.get("access_token"));
        assertThat(renewed.get("refresh_token")).isNotEqualTo(original.get("refresh_token"));
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.createObjectNode().put("refresh_token", original.get("access_token").asText()).toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidCredentialsAndInvalidBodies() throws Exception {
        for (String body : new String[]{"{\"username\":\"aluno\",\"password\":\"errada\"}",
                "{\"username\":\"desconhecido\",\"password\":\"Prova123!\"}"}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refresh_token\":\"invalido\"}")).andExpect(status().isUnauthorized());
        for (String path : new String[]{"/auth/login", "/auth/refresh"}) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void jwksOnlyExposesPublicKey() throws Exception {
        mvc.perform(get("/auth/jwks")).andExpect(status().isOk())
            .andExpect(jsonPath("keys[0].kty").value("RSA"))
            .andExpect(jsonPath("keys[0].n").exists())
            .andExpect(jsonPath("keys[0].d").doesNotExist())
            .andExpect(jsonPath("keys[0].p").doesNotExist());
    }
}
