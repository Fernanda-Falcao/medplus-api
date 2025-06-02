package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Paciente.
 * Herda as funcionalidades básicas do JpaRepository.
 * Métodos específicos para Paciente podem ser adicionados aqui.
 */
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /**
     * Busca um paciente pelo seu endereço de email.
     * @param email O email do paciente.
     * @return Um Optional contendo o paciente, se encontrado.
     */
    Optional<Paciente> findByEmail(String email);

    /**
     * Busca um paciente pelo seu CPF.
     * @param cpf O CPF do paciente.
     * @return Um Optional contendo o paciente, se encontrado.
     */
    Optional<Paciente> findByCpf(String cpf);

    /**
     * Verifica se existe um paciente com o email fornecido.
     * @param email O email a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe um paciente com o CPF fornecido.
     * @param cpf O CPF a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByCpf(String cpf);
}
