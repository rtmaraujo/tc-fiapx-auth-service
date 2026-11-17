package com.fiapx.auth.presentation;

import com.fiapx.auth.domain.Usuario;
import com.fiapx.auth.infrastructure.persistence.UsuarioEntity;
import com.fiapx.auth.infrastructure.repository.UsuarioRepository;
import com.fiapx.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("fluxo completo: cadastro, login e consulta do usuario")
    void deveExecutarFluxoCompleto() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("maria@fiapx.com"))
            .andExpect(jsonPath("$.role").value("USER"));

        assertThat(usuarioRepository.findByEmailIgnoreCase("maria@fiapx.com"))
            .get()
            .satisfies(entity -> assertThat(passwordEncoder.matches("senha1234", entity.getSenhaHash())).isTrue());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
            login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("maria@fiapx.com"));
    }

    @Test
    @DisplayName("recusa cadastro duplicado")
    void deveRecusarCadastroDuplicado() throws Exception {
        String body = "{\"nome\":\"Maria\",\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    @DisplayName("recusa login com senha errada")
    void deveRecusarSenhaErrada() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@fiapx.com\",\"senha\":\"errada123\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("recusa login de usuario inativo")
    void deveRecusarUsuarioInativo() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isCreated());

        UsuarioEntity entity = usuarioRepository.findByEmailIgnoreCase("maria@fiapx.com").orElseThrow();
        entity.desativar();
        usuarioRepository.save(entity);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@fiapx.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protege /me sem token")
    void deveProtegerMeSemToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("retorna 404 para usuario de token inexistente")
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        String token = jwtService.gerarToken(Usuario.novo("Fantasma", "fantasma@fiapx.com", "hash"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("retorna 400 para payload invalido")
    void deveRetornar400ParaPayloadInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"));
    }

    @Test
    @DisplayName("retorna 400 para JSON malformado")
    void deveRetornar400ParaJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("CORPO_INVALIDO"));
    }
}
