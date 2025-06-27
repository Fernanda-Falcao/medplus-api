package br.com.ifpe.medplus_api.model.paciente;

import br.com.ifpe.medplus_api.model.acesso.Usuario;
import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.model.consulta.Consulta; // Será criada depois
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um Paciente no sistema.
 * Herda de Usuario e adiciona campos específicos do paciente.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_paciente")
@PrimaryKeyJoinColumn(name = "usuario_id") // Define a chave primária como sendo a mesma da tabela pai (tb_usuario)
public class Paciente extends Usuario {

    @Column(name = "historico_medico", columnDefinition = "TEXT")
    private String historicoMedico;

    // Relacionamento com Consultas (um paciente pode ter várias consultas)
    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Consulta> consultas = new ArrayList<>();

    // Construtor
    public Paciente(String nome, String email, String senha, String cpf, LocalDate dataNascimento, String telefone, Endereco endereco, String historicoMedico) {
        super(nome, email, senha, cpf, dataNascimento, telefone, endereco);
        this.historicoMedico = historicoMedico;
    }

    // Métodos utilitários para gerenciar consultas
    public void adicionarConsulta(Consulta consulta) {
        consultas.add(consulta);
        consulta.setPaciente(this);
    }

    public void removerConsulta(Consulta consulta) {
        consultas.remove(consulta);
        consulta.setPaciente(null);
    }

    @Override
    public String toString() {
        return "Paciente{" +
                "id=" + getId() +
                ", nome='" + getNome() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", historicoMedico='" + (historicoMedico != null ? historicoMedico.substring(0, Math.min(historicoMedico.length(), 50)) + "..." : "N/A") + '\'' +
                '}';
    }

    public Usuario getUsuario() {
        throw new UnsupportedOperationException("Unimplemented method 'getUsuario'");
    }
}
