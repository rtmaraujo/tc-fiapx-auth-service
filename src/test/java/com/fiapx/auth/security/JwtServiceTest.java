package com.fiapx.auth.security;

import com.fiapx.auth.domain.Role;
import com.fiapx.auth.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("fiapx-test-secret-key-with-at-least-32-bytes");
        properties.setExpiration(Duration.ofHours(2));
        properties.setIssuer("fiapx-auth");
    }

    @Test
    @DisplayName("gera e valida token com os claims do usuario")
    void deveGerarEValidarToken() {
        JwtService service = new JwtService(properties);
        Usuario usuario = Usuario.novo("Maria", "maria@fiapx.com", "hash");

        String token = service.gerarToken(usuario);
        UsuarioPrincipal principal = service.extrairPrincipal(token);

        assertThat(principal.id()).isEqualTo(usuario.getId());
        assertThat(principal.nome()).isEqualTo("Maria");
        assertThat(principal.email()).isEqualTo("maria@fiapx.com");
        assertThat(principal.role()).isEqualTo(Role.USER);
        assertThat(service.expiresInSeconds()).isEqualTo(7200);
        assertThat(service.validar(token).getPayload().getIssuer()).isEqualTo("fiapx-auth");
    }

    @Test
    @DisplayName("usa duas horas quando a expiracao nao foi configurada")
    void deveUsarExpiracaoPadrao() {
        properties.setExpiration(null);
        JwtService service = new JwtService(properties);

        assertThat(service.expiresInSeconds()).isEqualTo(7200);
    }

    @Test
    @DisplayName("rejeita segredo com menos de 32 bytes")
    void deveRejeitarSegredoCurto() {
        properties.setSecret("curto");

        assertThatThrownBy(() -> new JwtService(properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("rejeita segredo nulo")
    void deveRejeitarSegredoNulo() {
        properties.setSecret(null);

        assertThatThrownBy(() -> new JwtService(properties))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("rejeita token invalido")
    void deveRejeitarTokenInvalido() {
        JwtService service = new JwtService(properties);

        assertThatThrownBy(() -> service.validar("nao-e-um-jwt"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("rejeita token com issuer diferente")
    void deveRejeitarIssuerDiferente() {
        JwtProperties outro = new JwtProperties();
        outro.setSecret(properties.getSecret());
        outro.setExpiration(Duration.ofHours(2));
        outro.setIssuer("outro-issuer");
        String token = new JwtService(outro).gerarToken(Usuario.novo("Maria", "maria@fiapx.com", "hash"));
        JwtService service = new JwtService(properties);

        assertThatThrownBy(() -> service.validar(token))
            .isInstanceOf(RuntimeException.class);
    }
}
