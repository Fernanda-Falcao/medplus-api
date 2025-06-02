package br.com.ifpe.medplus_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisições de autenticação (login).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Formato de email inválido.")
    @Size(max = 100, message = "Email pode ter no máximo 100 caracteres.")
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    // Em um cenário real, a validação do tamanho máximo da senha pode não ser necessária aqui,
    // pois o backend irá compará-la com o hash armazenado.
    private String senha;
}


