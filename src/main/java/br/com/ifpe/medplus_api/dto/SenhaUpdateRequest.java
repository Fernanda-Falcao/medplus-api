package br.com.ifpe.medplus_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para requisições de atualização de senha.
 */
@Data
public class SenhaUpdateRequest {

    @NotBlank(message = "Senha antiga é obrigatória.")
    private String senhaAntiga;

    @NotBlank(message = "Nova senha é obrigatória.")
    @Size(min = 6, max = 50, message = "Nova senha deve ter entre 6 e 50 caracteres.")
    private String novaSenha;

    // Opcional: campo de confirmação da nova senha, a validação seria feita no serviço.
    // @NotBlank(message = "Confirmação da nova senha é obrigatória.")
    // private String confirmacaoNovaSenha;
}

