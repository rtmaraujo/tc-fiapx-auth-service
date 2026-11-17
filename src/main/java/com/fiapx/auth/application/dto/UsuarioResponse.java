package com.fiapx.auth.application.dto;

import com.fiapx.auth.domain.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String nome,
    String email,
    String role,
    boolean ativo,
    LocalDateTime criadoEm
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getRole().name(),
            usuario.isAtivo(),
            usuario.getCriadoEm()
        );
    }
}
