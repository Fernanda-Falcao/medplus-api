package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.acesso.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Usuario.
 * Fornece métodos para buscar usuários, incluindo a busca por email para autenticação.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca um usuário pelo seu endereço de email, junto com os perfis.
     * Usado no processo de login para carregar as roles (perfis) do usuário.
     *
     * @param email O email do usuário a ser buscado.
     * @return Um Optional contendo o usuário, caso encontrado.
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.perfis WHERE u.email = :email")
    Optional<Usuario> findByEmailWithPerfis(@Param("email") String email);

    /**
     * Busca um usuário pelo seu endereço de email.
     * Útil para validações ou buscas simples (sem os perfis explicitamente carregados).
     *
     * @param email O email do usuário.
     * @return Um Optional contendo o usuário.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica se existe um usuário com o email fornecido.
     *
     * @param email O email a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe um usuário com o CPF fornecido.
     *
     * @param cpf O CPF a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByCpf(String cpf);

    /**
     * Busca um usuário pelo seu CPF.
     *
     * @param cpf O CPF do usuário.
     * @return Um Optional contendo o usuário.
     */
    Optional<Usuario> findByCpf(String cpf);
}
