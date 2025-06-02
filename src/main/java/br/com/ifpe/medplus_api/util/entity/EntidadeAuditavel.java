
package br.com.ifpe.medplus_api.util.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Superclasse abstrata para entidades que precisam de auditoria de criação e modificação.
 * Utiliza as anotações do Spring Data JPA para preencher automaticamente os campos de data.
 * A auditoria de usuário (@CreatedBy, @LastModifiedBy) pode ser adicionada aqui se necessário,
 * mas requer configuração adicional para o AuditorAware.
 */
@Getter
@Setter
@MappedSuperclass // Indica que esta classe não é uma entidade por si só, mas seus campos são mapeados nas subclasses.
@EntityListeners(AuditingEntityListener.class) // Habilita auditoria JPA para esta entidade e subclasses.
public abstract class EntidadeAuditavel extends EntidadeNegocio {

    private static final long serialVersionUID = 1L;

    @CreatedDate
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(name = "data_atualizacao", nullable = true)
    private LocalDateTime dataAtualizacao;

    // Para @CreatedBy e @LastModifiedBy, você precisaria implementar um AuditorAware<String>
    // e registrar como um bean no Spring. Exemplo:
    // @CreatedBy
    // @Column(name = "criado_por", updatable = false)
    // private String criadoPor;
    //
    // @LastModifiedBy
    // @Column(name = "atualizado_por")
    // private String atualizadoPor;
}

