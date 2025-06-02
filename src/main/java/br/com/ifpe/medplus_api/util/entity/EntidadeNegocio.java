package br.com.ifpe.medplus_api.util.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Superclasse abstrata para entidades de negócio que possuem um ID.
 * Pode ser estendida por EntidadeAuditavel se a auditoria também for necessária.
 */
@Getter
@Setter
@MappedSuperclass // Indica que os campos desta classe devem ser mapeados nas subclasses.
public abstract class EntidadeNegocio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de ID (auto-incremento).
    private Long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntidadeNegocio that = (EntidadeNegocio) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id=" + id +
                '}';
    }
}