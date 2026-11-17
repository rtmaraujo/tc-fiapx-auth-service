package com.fiapx.auth.application;

import com.fiapx.auth.application.dto.LoginRequest;
import com.fiapx.auth.application.dto.RegistroRequest;
import com.fiapx.auth.application.dto.TokenResponse;
import com.fiapx.auth.application.dto.UsuarioResponse;

import java.util.UUID;

public interface AuthApplicationService {

    UsuarioResponse registrar(RegistroRequest request);

    TokenResponse login(LoginRequest request);

    UsuarioResponse buscarPorId(UUID usuarioId);
}
