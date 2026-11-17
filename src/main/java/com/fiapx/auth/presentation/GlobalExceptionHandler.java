package com.fiapx.auth.presentation;

import com.fiapx.auth.application.CredenciaisInvalidasException;
import com.fiapx.auth.application.EmailJaCadastradoException;
import com.fiapx.auth.application.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(erro -> detalhes.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Dados de entrada invalidos",
            request.getRequestURI(), detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "CORPO_INVALIDO", "Corpo da requisicao invalido",
            request.getRequestURI(), null);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciais(CredenciaisInvalidasException ex,
                                                          HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", ex.getMessage(),
            request.getRequestURI(), null);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponse> tratarEmailDuplicado(EmailJaCadastradoException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO", ex.getMessage(),
            request.getRequestURI(), null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                            HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", ex.getMessage(),
            request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
            "Erro interno ao processar a requisicao", request.getRequestURI(), null);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String erro, String mensagem,
                                                   String caminho, Map<String, String> detalhes) {
        return ResponseEntity.status(status).body(new ErroResponse(
            LocalDateTime.now(), status.value(), erro, mensagem, caminho, detalhes));
    }
}
