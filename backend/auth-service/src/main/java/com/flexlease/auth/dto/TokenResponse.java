package com.flexlease.auth.dto;

public record TokenResponse(String accessToken, long expiresInSeconds) {
}
