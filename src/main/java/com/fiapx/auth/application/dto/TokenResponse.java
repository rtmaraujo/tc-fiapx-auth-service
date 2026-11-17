package com.fiapx.auth.application.dto;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UsuarioResponse usuario
) {
    public static TokenResponse bearer(String token, long expiresIn, UsuarioResponse usuario) {
        return new TokenResponse(token, "Bearer", expiresIn, usuario);
    }
}
