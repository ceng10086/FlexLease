package com.flexlease.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.user.support.TestJwtTokens;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:flexlease-user-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS users",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
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
                    "remark", "材料齐全"
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

    @Test
    void customerProfileLifecycle() throws Exception {
        UUID userId = UUID.randomUUID();
        String userToken = TestJwtTokens.bearerToken(userId, "customer-user", "USER");

        MvcResult initialResult = mockMvc.perform(get("/api/v1/customers/profile")
                .header(HttpHeaders.AUTHORIZATION, userToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode initialNode = readJson(initialResult);
        assertThat(initialNode.at("/data/userId").asText()).isEqualTo(userId.toString());
        assertThat(initialNode.at("/data/gender").asText()).isEqualTo("UNKNOWN");
        assertThat(initialNode.at("/data/fullName").isNull()).isTrue();

        var updatePayload = java.util.Map.of(
            "fullName", "李四",
            "gender", "MALE",
            "phone", "13800001111",
            "email", "lisi@example.com",
            "address", "北京市朝阳区望京"
        );

        MvcResult updateResult = mockMvc.perform(put("/api/v1/customers/profile")
                .header(HttpHeaders.AUTHORIZATION, userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode updateNode = readJson(updateResult);
        assertThat(updateNode.at("/data/fullName").asText()).isEqualTo("李四");
        assertThat(updateNode.at("/data/gender").asText()).isEqualTo("MALE");
        assertThat(updateNode.at("/data/phone").asText()).isEqualTo("13800001111");
        assertThat(updateNode.at("/data/email").asText()).isEqualTo("lisi@example.com");
        assertThat(updateNode.at("/data/address").asText()).isEqualTo("北京市朝阳区望京");
    }

    @Test
    void customerProfileRequiresAuthentication() throws Exception {
        var updatePayload = java.util.Map.of(
            "fullName", "anonymous",
            "gender", "UNKNOWN",
            "phone", "13900001111",
            "email", "anonymous@example.com",
            "address", "未知"
        );

        mockMvc.perform(get("/api/v1/customers/profile"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/customers/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void vendorCannotAccessCustomerProfile() throws Exception {
        UUID vendorId = UUID.randomUUID();
        String vendorToken = TestJwtTokens.bearerToken(vendorId, "vendor-only", "VENDOR");

        mockMvc.perform(get("/api/v1/customers/profile")
                .header(HttpHeaders.AUTHORIZATION, vendorToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(com.flexlease.common.exception.ErrorCode.FORBIDDEN.code()));

        mockMvc.perform(put("/api/v1/customers/profile")
                .header(HttpHeaders.AUTHORIZATION, vendorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "fullName", "张三",
                        "gender", "MALE",
                        "phone", "13600001111",
                        "email", "zhangsan@example.com",
                        "address", "上海市浦东新区"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(com.flexlease.common.exception.ErrorCode.FORBIDDEN.code()));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
