package com.flexlease.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.user.support.TestJwtTokens;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.flexlease.user.integration.AuthServiceClient authServiceClient;

    @Test
    void vendorApplicationLifecycle() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String vendorToken = TestJwtTokens.bearerToken(ownerId, "vendor-user", "VENDOR");
        String adminToken = TestJwtTokens.bearerToken(UUID.randomUUID(), "admin-user", "ADMIN");

        var requestPayload = java.util.Map.of(
            "companyName", "测试科技有限公司",
            "unifiedSocialCode", "91330100792301234X",
            "contactName", "张三",
            "contactPhone", "18800001111",
            "contactEmail", "contact@example.com",
            "province", "浙江省",
            "city", "杭州市",
            "address", "未来科技城"
        );

        MvcResult submitResult = mockMvc.perform(post("/api/v1/vendors/applications")
                .header(HttpHeaders.AUTHORIZATION, vendorToken)
                .header("X-User-Id", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestPayload)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode submitNode = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String applicationId = submitNode.at("/data/id").asText();
        assertThat(applicationId).isNotBlank();

        MvcResult approveResult = mockMvc.perform(post("/api/v1/vendors/applications/" + applicationId + "/approve")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "remark", "材料齐全",
                    "reviewerId", UUID.randomUUID().toString()
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode approveNode = objectMapper.readTree(approveResult.getResponse().getContentAsString());
        assertThat(approveNode.at("/data/status").asText()).isEqualTo("APPROVED");

        verify(authServiceClient).activateAccount(ownerId);

        mockMvc.perform(get("/api/v1/vendors/applications/" + applicationId)
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isOk());
    }
}
