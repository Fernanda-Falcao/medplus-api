package br.com.ifpe.medplus_api.dto;

import br.com.ifpe.medplus_api.model.acesso.Perfil;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

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
    @Size(min = 6, max = 50, message = "Senha deve ter entre 6 e 50 caracteres")
    private String password;

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 14, message = "CPF deve estar no formato XXX.XXX.XXX-XX ou XXXXXXXXXXX")
    private String cpf;

    @PastOrPresent(message = "Data de nascimento deve ser no passado ou presente")
    private LocalDate dataNascimento;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @Valid
    @NotNull(message = "Endereço é obrigatório")
    private EnderecoRequest endereco;

    @Size(max = 5000, message = "Histórico médico deve ter no máximo 5000 caracteres")
    private String historicoMedico;

    public Paciente toPaciente() {
        Paciente paciente = new Paciente();
        paciente.setNome(this.nome);
        paciente.setEmail(this.email);
        paciente.setSenha(this.password); // 🔐 lembrar de criptografar depois
        paciente.setCpf(this.cpf);
        paciente.setDataNascimento(this.dataNascimento);
        paciente.setTelefone(this.telefone);
        paciente.setEndereco(this.endereco.toEndereco());
        paciente.setHistoricoMedico(this.historicoMedico);
        paciente.addPerfil(new Perfil(null, PerfilEnum.ROLE_PACIENTE));
        return paciente;
    }

    public CharSequence getSenha() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSenha'");
    }
}

