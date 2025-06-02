package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.acesso.Perfil;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Perfil.
 */
@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    /**
     * Busca um perfil pelo seu nome (enum).
     * @param nomeEnum O enum do perfil (ex: PerfilEnum.ROLE_ADMIN).
     * @return Um Optional contendo o perfil, se encontrado.
     */
    Optional<Perfil> findByNome(PerfilEnum nomeEnum);
}
