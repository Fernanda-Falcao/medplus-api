
package br.com.ifpe.medplus_api.model.acesso;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;

/**
 * Entidade que representa um perfil (role) de usuário no sistema.
 * Implementa GrantedAuthority para integração com o Spring Security.
 */
@Entity
@Table(name = "tb_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Perfil implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private PerfilEnum nome; // Exemplo: ROLE_ADMIN, ROLE_PACIENTE, ROLE_MEDICO

    /**
     * Retorna o nome do perfil no formato esperado pelo Spring Security.
     * Este método é exigido pela interface GrantedAuthority.
     *
     * @return O nome da role (ex: "ROLE_ADMIN").
     */
    @Override
    public String getAuthority() {
        return nome.name();
    }

    /**
     * Implementação de equals e hashCode baseada no id e nome,
     * garantindo a correta comparação em conjuntos (HashSet, etc.).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Perfil perfil)) return false;
        return Objects.equals(id, perfil.id) && nome == perfil.nome;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome);
    }

    @Override
    public String toString() {
        return "Perfil{" +
                "id=" + id +
                ", nome=" + nome +
                '}';
    }
}
