
package br.com.ifpe.medplus_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank; // Significa: não nulo E não apenas espaços em branco E não vazio
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PacienteRequest {

    @NotBlank(message = "Nome é obrigatório")
    // Se o frontend enviar "" (string vazia), @NotBlank FALHARÁ.
    // "" também FALHARÁ em @Size(min = 3).
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    // Se o frontend enviar "", @NotBlank FALHARÁ.
    // "" também FALHARÁ em @Email.
    @Email(message = "Email deve ser válido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    // Se o frontend enviar "", @NotBlank FALHARÁ.
    // "" também FALHARÁ em @Size(min = 6).
    @Size(min = 6, max = 50, message = "Senha deve ter entre 6 e 50 caracteres")
    private String senha;

    @NotBlank(message = "CPF é obrigatório")
    // O frontend envia o CPF apenas com dígitos (ex: "12345678900") ou "" se vazio.
    // Se for "", @NotBlank FALHARÁ.
    @Size(min = 11, max = 14, message = "CPF deve estar no formato XXX.XXX.XXX-XX ou XXXXXXXXXXX")
    private String cpf;

    // Se 'dataNascimento' for opcional: A configuração atual está OK.
    // O frontend envia "" se vazio, que o Spring provavelmente converterá para null para LocalDate.
    // @PastOrPresent só valida se não for null.
    // Se 'dataNascimento' FOR OBRIGATÓRIO, você PRECISA adicionar:
    // @NotNull(message = "Data de nascimento é obrigatória")
    @PastOrPresent(message = "Data de nascimento deve ser no passado ou presente")
    private LocalDate dataNascimento;

    // 'telefone' é opcional (só tem @Size). Enviar "" está OK.
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @Valid // IMPORTANTE: Isso valida os campos DENTRO de EnderecoRequest
    @NotNull(message = "Endereço é obrigatório") // O frontend sempre envia um objeto para 'endereco', então isso deve passar.
    private EnderecoRequest endereco; // **VERIFIQUE AS VALIDAÇÕES DENTRO DE EnderecoRequest.java**

    // 'historicoMedico' é opcional. Enviar "" está OK.
    @Size(max = 5000, message = "Histórico médico deve ter no máximo 5000 caracteres")
    private String historicoMedico;
}