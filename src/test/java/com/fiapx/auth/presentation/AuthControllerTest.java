package com.fiapx.auth.presentation;

import com.fiapx.auth.application.AuthApplicationService;
import com.fiapx.auth.application.dto.RegistroRequest;
import com.fiapx.auth.application.dto.TokenResponse;
import com.fiapx.auth.application.dto.UsuarioResponse;
import com.fiapx.auth.domain.Role;
import com.fiapx.auth.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthApplicationService applicationService;

    private MockMvc mockMvc;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(applicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("retorna 201 ao registrar usuario")
    void deveRegistrar() throws Exception {
        UUID id = UUID.randomUUID();
        when(applicationService.registrar(any())).thenReturn(
            new UsuarioResponse(id, "Maria", "maria@fiapx.com", "USER", true, LocalDateTime.now()));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.email").value("maria@fiapx.com"));
    }

    @Test
    @DisplayName("retorna 400 quando o payload de registro e invalido")
    void deveRejeitarRegistroInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"));
    }

    @Test
    @DisplayName("retorna token no login")
    void deveLogar() throws Exception {
        UsuarioResponse usuario = new UsuarioResponse(
            UUID.randomUUID(), "Maria", "maria@fiapx.com", "USER", true, LocalDateTime.now());
        when(applicationService.login(any())).thenReturn(TokenResponse.bearer("jwt", 7200, usuario));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("me devolve o usuario autenticado")
    void deveRetornarMe() {
        UUID id = UUID.randomUUID();
        UsuarioPrincipal principal = new UsuarioPrincipal(id, "Maria", "maria@fiapx.com", Role.USER);
        UsuarioResponse response = new UsuarioResponse(
            id, "Maria", "maria@fiapx.com", "USER", true, LocalDateTime.now());
        when(applicationService.buscarPorId(id)).thenReturn(response);

        var resultado = controller.me(principal);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("me sem principal devolve 401")
    void deveRetornar401SemPrincipal() {
        var resultado = controller.me(null);

        assertThat(resultado.getStatusCode().value()).isEqualTo(401);
    }
}
