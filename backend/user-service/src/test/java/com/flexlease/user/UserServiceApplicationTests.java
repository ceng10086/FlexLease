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
import com.flexlease.user.repository.VendorRepository;
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

    @Autowired
    private VendorRepository vendorRepository;

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

        UUID vendorId = vendorRepository.findAll().stream()
                .findFirst()
                .map(com.flexlease.user.domain.Vendor::getId)
                .orElseThrow();

        verify(authServiceClient).assignVendor(ownerId, vendorId);

        mockMvc.perform(get("/api/v1/vendors/" + vendorId)
                .header(HttpHeaders.AUTHORIZATION, vendorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(vendorId.toString()));

        var ownerUpdatePayload = java.util.Map.of(
            "contactName", "李运营",
            "contactPhone", "18800002222"
        );

        mockMvc.perform(put("/api/v1/vendors/" + vendorId)
                .header(HttpHeaders.AUTHORIZATION, vendorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownerUpdatePayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contactName").value("李运营"));

        String operatorToken = TestJwtTokens.bearerToken(UUID.randomUUID(), vendorId, "vendor-operator", "VENDOR");

        mockMvc.perform(get("/api/v1/vendors/" + vendorId)
                .header(HttpHeaders.AUTHORIZATION, operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(vendorId.toString()));

        var operatorUpdatePayload = java.util.Map.of(
            "contactName", "运营同学",
            "contactPhone", "17700003333"
        );

        mockMvc.perform(put("/api/v1/vendors/" + vendorId)
                .header(HttpHeaders.AUTHORIZATION, operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(operatorUpdatePayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contactPhone").value("17700003333"));

        String otherVendorToken = TestJwtTokens.bearerToken(UUID.randomUUID(), UUID.randomUUID(), "outsider", "VENDOR");

        mockMvc.perform(get("/api/v1/vendors/" + vendorId)
                .header(HttpHeaders.AUTHORIZATION, otherVendorToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(com.flexlease.common.exception.ErrorCode.FORBIDDEN.code()));

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
        assertThat(initialNode.at("/data/creditScore").asInt()).isEqualTo(60);
        assertThat(initialNode.at("/data/creditTier").asText()).isEqualTo("STANDARD");

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
        assertThat(updateNode.at("/data/creditScore").asInt()).isEqualTo(60);
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

    @Test
    void adminCanAdjustCreditAndInternalEndpointReturnsSnapshot() throws Exception {
        UUID userId = UUID.randomUUID();
        String userToken = TestJwtTokens.bearerToken(userId, "customer-user", "USER");
        String adminToken = TestJwtTokens.bearerToken(UUID.randomUUID(), "admin-user", "ADMIN");

        mockMvc.perform(get("/api/v1/customers/profile")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk());

        var adjustPayload = java.util.Map.of(
                "delta", 15,
                "reason", "按时支付奖励"
        );

        MvcResult adjustResult = mockMvc.perform(post("/api/v1/admin/users/" + userId + "/credit-adjustments")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustPayload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adjustNode = readJson(adjustResult);
        assertThat(adjustNode.at("/data/creditScore").asInt()).isEqualTo(75);
        assertThat(adjustNode.at("/data/creditTier").asText()).isEqualTo("STANDARD");

        MvcResult internalResult = mockMvc.perform(get("/api/v1/internal/users/" + userId + "/credit")
                        .header("X-Internal-Token", "flexlease-internal-secret"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode creditNode = readJson(internalResult);
        assertThat(creditNode.at("/data/creditScore").asInt()).isEqualTo(75);
        assertThat(creditNode.at("/data/creditTier").asText()).isEqualTo("STANDARD");
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
