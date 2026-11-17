package com.fiapx.auth.application.impl;

import com.fiapx.auth.application.AuthApplicationService;
import com.fiapx.auth.application.CredenciaisInvalidasException;
import com.fiapx.auth.application.EmailJaCadastradoException;
import com.fiapx.auth.application.RecursoNaoEncontradoException;
import com.fiapx.auth.application.dto.LoginRequest;
import com.fiapx.auth.application.dto.RegistroRequest;
import com.fiapx.auth.application.dto.TokenResponse;
import com.fiapx.auth.application.dto.UsuarioResponse;
import com.fiapx.auth.domain.Usuario;
import com.fiapx.auth.infrastructure.persistence.UsuarioEntity;
import com.fiapx.auth.infrastructure.repository.UsuarioRepository;
import com.fiapx.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthApplicationServiceImpl(UsuarioRepository usuarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailJaCadastradoException("Email ja cadastrado: " + email);
        }

        Usuario usuario = Usuario.novo(
            request.nome().trim(),
            email,
            passwordEncoder.encode(request.senha())
        );
        UsuarioEntity salvo = usuarioRepository.save(new UsuarioEntity(usuario));
        return UsuarioResponse.from(salvo.toDomain());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        UsuarioEntity entity = usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha invalidos"));

        if (!passwordEncoder.matches(request.senha(), entity.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Email ou senha invalidos");
        }
        if (!entity.isAtivo()) {
            throw new CredenciaisInvalidasException("Usuario inativo");
        }

        Usuario usuario = entity.toDomain();
        String token = jwtService.gerarToken(usuario);
        return TokenResponse.bearer(token, jwtService.expiresInSeconds(), UsuarioResponse.from(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .map(UsuarioEntity::toDomain)
            .map(UsuarioResponse::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + usuarioId));
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
