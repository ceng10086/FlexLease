package com.flexlease.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorUpdateRequest(
        @NotBlank(message = "联系人姓名不能为空")
        @Size(max = 100, message = "联系人姓名过长")
        String contactName,
        @NotBlank(message = "联系电话不能为空")
        @Size(max = 50, message = "联系电话过长")
        String contactPhone,
        @Email(message = "联系人邮箱格式不正确")
        @Size(max = 100, message = "联系人邮箱过长")
        String contactEmail,
        @Size(max = 100, message = "省份名称过长")
        String province,
        @Size(max = 100, message = "城市名称过长")
        String city,
        @Size(max = 255, message = "地址过长")
        String address
) {
}
