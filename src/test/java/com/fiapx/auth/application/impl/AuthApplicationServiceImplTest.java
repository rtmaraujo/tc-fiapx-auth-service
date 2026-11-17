package com.fiapx.auth.application.impl;

import com.fiapx.auth.application.CredenciaisInvalidasException;
import com.fiapx.auth.application.EmailJaCadastradoException;
import com.fiapx.auth.application.RecursoNaoEncontradoException;
import com.fiapx.auth.application.dto.LoginRequest;
import com.fiapx.auth.application.dto.RegistroRequest;
import com.fiapx.auth.application.dto.TokenResponse;
import com.fiapx.auth.application.dto.UsuarioResponse;
import com.fiapx.auth.domain.Role;
import com.fiapx.auth.domain.Usuario;
import com.fiapx.auth.infrastructure.persistence.UsuarioEntity;
import com.fiapx.auth.infrastructure.repository.UsuarioRepository;
import com.fiapx.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthApplicationServiceImpl(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("registra usuario novo com email normalizado e senha criptografada")
    void deveRegistrarUsuario() {
        when(usuarioRepository.existsByEmailIgnoreCase("maria@fiapx.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(UsuarioEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse response = service.registrar(
            new RegistroRequest("  Maria  ", "  MARIA@FIAPX.COM ", "senha1234"));

        assertThat(response.email()).isEqualTo("maria@fiapx.com");
        assertThat(response.nome()).isEqualTo("Maria");
        assertThat(response.role()).isEqualTo(Role.USER.name());
        assertThat(response.ativo()).isTrue();
        verify(usuarioRepository).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("recusa cadastro com email ja existente")
    void deveRecusarEmailDuplicado() {
        when(usuarioRepository.existsByEmailIgnoreCase("maria@fiapx.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(
            new RegistroRequest("Maria", "maria@fiapx.com", "senha1234")))
            .isInstanceOf(EmailJaCadastradoException.class)
            .hasMessageContaining("maria@fiapx.com");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("autentica usuario e devolve token")
    void deveAutenticar() {
        Usuario usuario = Usuario.novo("Maria", "maria@fiapx.com", "hash-bcrypt");
        when(usuarioRepository.findByEmailIgnoreCase("maria@fiapx.com"))
            .thenReturn(Optional.of(new UsuarioEntity(usuario)));
        when(passwordEncoder.matches("senha1234", "hash-bcrypt")).thenReturn(true);
        when(jwtService.gerarToken(any(Usuario.class))).thenReturn("jwt-token");
        when(jwtService.expiresInSeconds()).thenReturn(7200L);

        TokenResponse response = service.login(new LoginRequest(" MARIA@FIAPX.COM ", "senha1234"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200L);
        assertThat(response.usuario().id()).isEqualTo(usuario.getId());
    }

    @Test
    @DisplayName("recusa login de email inexistente")
    void deveRecusarEmailInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("naoexiste@fiapx.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("naoexiste@fiapx.com", "senha1234")))
            .isInstanceOf(CredenciaisInvalidasException.class)
            .hasMessage("Email ou senha invalidos");
    }

    @Test
    @DisplayName("recusa login com senha errada")
    void deveRecusarSenhaErrada() {
        Usuario usuario = Usuario.novo("Maria", "maria@fiapx.com", "hash-bcrypt");
        when(usuarioRepository.findByEmailIgnoreCase("maria@fiapx.com"))
            .thenReturn(Optional.of(new UsuarioEntity(usuario)));
        when(passwordEncoder.matches("errada", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@fiapx.com", "errada")))
            .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("recusa login de usuario inativo")
    void deveRecusarUsuarioInativo() {
        Usuario usuario = Usuario.novo("Maria", "maria@fiapx.com", "hash-bcrypt");
        UsuarioEntity entity = new UsuarioEntity(usuario);
        entity.desativar();
        when(usuarioRepository.findByEmailIgnoreCase("maria@fiapx.com")).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("senha1234", "hash-bcrypt")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@fiapx.com", "senha1234")))
            .isInstanceOf(CredenciaisInvalidasException.class)
            .hasMessage("Usuario inativo");
    }

    @Test
    @DisplayName("busca usuario por id")
    void deveBuscarPorId() {
        Usuario usuario = Usuario.novo("Maria", "maria@fiapx.com", "hash-bcrypt");
        when(usuarioRepository.findById(usuario.getId()))
            .thenReturn(Optional.of(new UsuarioEntity(usuario)));

        UsuarioResponse response = service.buscarPorId(usuario.getId());

        assertThat(response.id()).isEqualTo(usuario.getId());
        assertThat(response.email()).isEqualTo("maria@fiapx.com");
    }

    @Test
    @DisplayName("falha ao buscar usuario inexistente")
    void deveFalharBuscaPorIdInexistente() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("normaliza email nulo sem quebrar")
    void deveTratarEmailNulo() {
        when(usuarioRepository.existsByEmailIgnoreCase(null)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse response = service.registrar(new RegistroRequest("Maria", null, "senha1234"));

        assertThat(response.email()).isNull();
    }
}
