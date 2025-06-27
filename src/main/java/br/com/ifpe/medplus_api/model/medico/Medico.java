package br.com.ifpe.medplus_api.model.medico;

import br.com.ifpe.medplus_api.model.acesso.Usuario;
import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.model.consulta.Consulta; // Será criada depois
import br.com.ifpe.medplus_api.model.consulta.DisponibilidadeMedico; // Será criada depois
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um Médico no sistema.
 * Herda de Usuario e adiciona campos específicos do médico.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_medico")
@PrimaryKeyJoinColumn(name = "usuario_id") // Chave primária é a mesma da tabela pai (tb_usuario)
public class Medico extends Usuario {

    @NotBlank(message = "CRM é obrigatório")
    @Size(max = 20, message = "CRM deve ter no máximo 20 caracteres")
    @Column(name = "crm", nullable = false, unique = true, length = 20)
    private String crm;

    @NotBlank(message = "Especialidade é obrigatória")
    @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
    @Column(name = "especialidade", nullable = false, length = 100)
    private String especialidade;

    // Relacionamento com Consultas (um médico pode ter várias consultas)
    @OneToMany(mappedBy = "medico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Consulta> consultasAgendadas = new ArrayList<>();

    // Relacionamento com Disponibilidade (um médico pode ter vários horários de disponibilidade)
    @OneToMany(mappedBy = "medico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DisponibilidadeMedico> disponibilidades = new ArrayList<>();

    // Construtor
    public Medico(String nome, String email, String senha, String cpf, LocalDate dataNascimento, String telefone, Endereco endereco, String crm, String especialidade) {
        super(nome, email, senha, cpf, dataNascimento, telefone, endereco);
        this.crm = crm;
        this.especialidade = especialidade;
    }

    // Métodos utilitários
    public void adicionarConsulta(Consulta consulta) {
        consultasAgendadas.add(consulta);
        consulta.setMedico(this);
    }

    public void removerConsulta(Consulta consulta) {
        consultasAgendadas.remove(consulta);
        consulta.setMedico(null);
    }

    public void adicionarDisponibilidade(DisponibilidadeMedico disponibilidade) {
        disponibilidades.add(disponibilidade);
        disponibilidade.setMedico(this);
    }

    public void removerDisponibilidade(DisponibilidadeMedico disponibilidade) {
        disponibilidades.remove(disponibilidade);
        disponibilidade.setMedico(null);
    }

    @Override
    public String toString() {
        return "Medico{" +
                "id=" + getId() +
                ", nome='" + getNome() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", crm='" + crm + '\'' +
                ", especialidade='" + especialidade + '\'' +
                '}';
    }

    public Usuario getUsuario() {
        throw new UnsupportedOperationException("Unimplemented method 'getUsuario'");
    }
}
