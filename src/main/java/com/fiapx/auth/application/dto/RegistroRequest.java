package com.fiapx.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
    @NotBlank(message = "nome e obrigatorio")
    @Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
    String nome,

    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    @Size(max = 180, message = "email deve ter no maximo 180 caracteres")
    String email,

    @NotBlank(message = "senha e obrigatoria")
    @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
    String senha
) {
}
