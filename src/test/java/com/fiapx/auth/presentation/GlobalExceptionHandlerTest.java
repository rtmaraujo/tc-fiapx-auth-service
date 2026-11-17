package com.fiapx.auth.presentation;

import com.fiapx.auth.application.CredenciaisInvalidasException;
import com.fiapx.auth.application.EmailJaCadastradoException;
import com.fiapx.auth.application.RecursoNaoEncontradoException;
import com.fiapx.auth.application.dto.RegistroRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");
    }

    @Test
    @DisplayName("trata erro de validacao com detalhes por campo")
    void deveTratarValidacao() throws Exception {
        Method method = Dummy.class.getDeclaredMethod("executar", RegistroRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            new RegistroRequest("Maria", "maria@fiapx.com", "senha1234"), "registroRequest");
        bindingResult.rejectValue("nome", "NotBlank", "nome e obrigatorio");

        var response = handler.tratarValidacao(
            new MethodArgumentNotValidException(parameter, bindingResult), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().detalhes()).containsEntry("nome", "nome e obrigatorio");
    }

    @Test
    @DisplayName("trata corpo JSON invalido")
    void deveTratarCorpoInvalido() {
        var response = handler.tratarCorpoInvalido(null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().erro()).isEqualTo("CORPO_INVALIDO");
    }

    @Test
    @DisplayName("trata credenciais invalidas")
    void deveTratarCredenciais() {
        var response = handler.tratarCredenciais(
            new CredenciaisInvalidasException("Email ou senha invalidos"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("trata email ja cadastrado")
    void deveTratarEmailDuplicado() {
        var response = handler.tratarEmailDuplicado(
            new EmailJaCadastradoException("Email ja cadastrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("trata recurso nao encontrado")
    void deveTratarNaoEncontrado() {
        var response = handler.tratarNaoEncontrado(
            new RecursoNaoEncontradoException("Usuario nao encontrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("trata erro inesperado")
    void deveTratarErroInesperado() {
        var response = handler.tratarErroInesperado(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().erro()).isEqualTo("ERRO_INTERNO");
    }

    static class Dummy {
        void executar(RegistroRequest request) {
        }
    }
}
