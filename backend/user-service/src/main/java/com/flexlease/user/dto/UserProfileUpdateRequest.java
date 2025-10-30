package com.flexlease.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "姓名过长")
        String fullName,
        @NotBlank(message = "性别不能为空")
        @Pattern(regexp = "UNKNOWN|MALE|FEMALE", message = "性别取值应为 UNKNOWN/MALE/FEMALE")
        String gender,
        @NotBlank(message = "手机号不能为空")
        @Size(max = 20, message = "手机号过长")
        String phone,
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱过长")
        String email,
        @Size(max = 255, message = "地址过长")
        String address
) {
}
