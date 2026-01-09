package com.flexlease.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 厂商提交/更新入驻申请请求。
 */
public record VendorApplicationRequest(
        @NotBlank(message = "companyName 不能为空")
        @Size(max = 200)
        String companyName,

        @NotBlank(message = "unifiedSocialCode 不能为空")
        @Size(max = 50)
        String unifiedSocialCode,

        @NotBlank(message = "contactName 不能为空")
        String contactName,

        @NotBlank(message = "contactPhone 不能为空")
        String contactPhone,

        @Email(message = "contactEmail 必须为邮箱格式")
        String contactEmail,

        String province,
        String city,
        String address
) {
}
