package com.fiapx.auth.security;

import com.fiapx.auth.domain.Role;

import java.util.UUID;

public record UsuarioPrincipal(UUID id, String nome, String email, Role role) {
}
