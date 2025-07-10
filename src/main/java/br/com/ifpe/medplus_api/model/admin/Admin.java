package br.com.ifpe.medplus_api.model.admin;

import br.com.ifpe.medplus_api.model.acesso.Usuario;
import br.com.ifpe.medplus_api.model.common.Endereco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entidade que representa um Administrador no sistema.
 * Herda de Usuario e pode ter campos específicos do administrador, se necessário.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_admin")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Admin extends Usuario {

    @Column(name = "nivel_acesso")
    private Integer nivelAcesso;

    public Admin(String nome, String email, String senha, String cpf, LocalDate dataNascimento, String telefone, Endereco endereco, Integer nivelAcesso) {
        super(nome, email, senha, cpf, dataNascimento, telefone, endereco);
        this.nivelAcesso = nivelAcesso;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + getId() +
                ", nome='" + getNome() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", nivelAcesso=" + nivelAcesso +
                '}';
    }
}
