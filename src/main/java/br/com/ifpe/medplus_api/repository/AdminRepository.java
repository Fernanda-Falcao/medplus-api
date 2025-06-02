package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.admin.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Admin.
 * Herda as funcionalidades básicas do JpaRepository.
 * Métodos específicos para Admin podem ser adicionados aqui.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Busca um administrador pelo seu endereço de email.
     * @param email O email do administrador.
     * @return Um Optional contendo o administrador, se encontrado.
     */
    Optional<Admin> findByEmail(String email);

    /**
     * Busca um administrador pelo seu CPF.
     * @param cpf O CPF do administrador.
     * @return Um Optional contendo o administrador, se encontrado.
     */
    Optional<Admin> findByCpf(String cpf);

    /**
     * Verifica se existe um administrador com o email fornecido.
     * @param email O email a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe um administrador com o CPF fornecido.
     * @param cpf O CPF a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByCpf(String cpf);
}

