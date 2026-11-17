package com.fiapx.auth.presentation;

import com.fiapx.auth.application.AuthApplicationService;
import com.fiapx.auth.application.dto.LoginRequest;
import com.fiapx.auth.application.dto.RegistroRequest;
import com.fiapx.auth.application.dto.TokenResponse;
import com.fiapx.auth.application.dto.UsuarioResponse;
import com.fiapx.auth.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Cadastro, login e dados do usuario autenticado")
public class AuthController {

    private final AuthApplicationService applicationService;

    public AuthController(AuthApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra um novo usuario")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse response = applicationService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuario e devolve um JWT")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(applicationService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados do usuario do token")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(applicationService.buscarPorId(principal.id()));
    }
}
