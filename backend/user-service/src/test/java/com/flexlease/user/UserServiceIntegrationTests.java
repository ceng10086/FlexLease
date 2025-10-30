package com.flexlease.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.user.domain.VendorApplication;
import com.flexlease.user.integration.AuthServiceClient;
import com.flexlease.user.repository.VendorApplicationRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceIntegrationTests {

    static {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VendorApplicationRepository vendorApplicationRepository;

    @MockBean
    private AuthServiceClient authServiceClient;

    @Test
    void approvingVendorApplicationCreatesVendorRecord() throws Exception {
        UUID ownerId = UUID.randomUUID();
        VendorApplication application = vendorApplicationRepository.save(VendorApplication.submit(
                ownerId,
                "测试厂商",
                "USC-123456",
                "张三",
                "13800000000",
                "vendor@example.com",
                "广东省",
                "深圳市",
                "南山区科技园"
        ));

        doNothing().when(authServiceClient).activateAccount(ownerId);

        mockMvc.perform(post("/api/v1/vendors/applications/" + application.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "reviewerId", UUID.randomUUID().toString(),
                                "remark", "通过"
                        ))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listNode = objectMapper.readTree(listResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode content = listNode.at("/data/content");
        assertThat(content.isArray()).isTrue();
        boolean matched = false;
        for (JsonNode vendorNode : content) {
            if ("测试厂商".equals(vendorNode.get("companyName").asText())) {
                matched = true;
                break;
            }
        }
        assertThat(matched).as("列表中应包含当前审核通过的厂商").isTrue();
    }

    @Test
    void customerProfileUpsertAndAdminListing() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/customers/profile")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "fullName", "李四",
                                "gender", "MALE",
                                "phone", "13900000000",
                                "email", "user@example.com",
                                "address", "上海市浦东新区"
                        ))))
                .andExpect(status().isOk());

        MvcResult profileResult = mockMvc.perform(get("/api/v1/customers/profile")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode profileNode = objectMapper.readTree(profileResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(profileNode.at("/data/fullName").asText()).isEqualTo("李四");

        MvcResult adminList = mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminNode = objectMapper.readTree(adminList.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(adminNode.at("/data/content").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void adminCanUpdateUserStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(authServiceClient).updateAccountStatus(userId, "ENABLED");

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "status", "enabled"
                        ))))
                .andExpect(status().isOk());

        verify(authServiceClient).updateAccountStatus(userId, "ENABLED");
    }
}
