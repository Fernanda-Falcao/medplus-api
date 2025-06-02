
package br.com.ifpe.medplus_api.model.acesso;

import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.util.entity.EntidadeAuditavel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade base para todos os usuários do sistema (Paciente, Médico, Admin).
 * Implementa UserDetails para integração com o Spring Security.
 * Utiliza estratégia de herança JOINED para mapear as subclasses em tabelas separadas.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_usuario", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uk_usuario_email"),
        @UniqueConstraint(columnNames = "cpf", name = "uk_usuario_cpf")
})
@Inheritance(strategy = InheritanceType.JOINED) // Herança JOINED: cria tabelas separadas para subclasses.
public abstract class Usuario extends EntidadeAuditavel implements UserDetails {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @Column(nullable = false, length = 255) // Armazena o hash da senha
    private String senha;

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 14, message = "CPF deve estar no formato XXX.XXX.XXX-XX ou XXXXXXXXXXX")
    @Column(nullable = false, length = 14, unique = true)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(length = 20)
    private String telefone;

    @Embedded
    private Endereco endereco;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    /**
     * Perfis (roles) do usuário.
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "tb_usuario_perfil",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "perfil_id")
    )
    private Set<Perfil> perfis = new HashSet<>();

    // Implementação de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.perfis;
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Ajuste conforme regras de negócio
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Ajuste conforme regras de negócio
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Ajuste conforme regras de negócio
    }

    @Override
    public boolean isEnabled() {
        return this.ativo;
    }

    // Construtor personalizado (opcional)
    public Usuario(String nome, String email, String senha, String cpf, LocalDate dataNascimento, String telefone, Endereco endereco) {
        this.nome = nome;
        this.email = email;
        this.senha = senha; // Lembre-se de codificar a senha antes de salvar!
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone;
        this.endereco = endereco;
        this.ativo = true;
    }

    public void addPerfil(Perfil perfil) {
        this.perfis.add(perfil);
    }

    public void removePerfil(Perfil perfil) {
        this.perfis.remove(perfil);
    }
}
