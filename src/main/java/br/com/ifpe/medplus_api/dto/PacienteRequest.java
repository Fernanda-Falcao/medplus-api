package br.com.ifpe.medplus_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para criar ou atualizar um Paciente.
 */
@Data
public class PacienteRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 50, message = "Senha deve ter entre 6 e 50 caracteres") // Max size para a entrada, o hash será maior
    private String senha;

    @NotBlank(message = "CPF é obrigatório")
    // Adicionar @CPF do Hibernate Validator se estiver usando ou uma validação customizada
    @Size(min = 11, max = 14, message = "CPF deve estar no formato XXX.XXX.XXX-XX ou XXXXXXXXXXX")
    private String cpf;

    @PastOrPresent(message = "Data de nascimento deve ser no passado ou presente")
    private LocalDate dataNascimento;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @Valid // Garante que o EnderecoRequest aninhado também seja validado
    @NotNull(message = "Endereço é obrigatório")
    private EnderecoRequest endereco;

    @Size(max = 5000, message = "Histórico médico deve ter no máximo 5000 caracteres")
    private String historicoMedico;

    // Não incluir 'ativo' aqui, pois geralmente é controlado internamente ou por um admin.
    // Perfis também são gerenciados internamente ou por um admin.
}

