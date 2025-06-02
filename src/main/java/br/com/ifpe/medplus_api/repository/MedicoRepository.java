package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.medico.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Medico.
 * Herda as funcionalidades básicas do JpaRepository.
 * Métodos específicos para Medico podem ser adicionados aqui.
 */
@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    /**
     * Busca um médico pelo seu endereço de email.
     * @param email O email do médico.
     * @return Um Optional contendo o médico, se encontrado.
     */
    Optional<Medico> findByEmail(String email);

    /**
     * Busca um médico pelo seu CPF.
     * @param cpf O CPF do médico.
     * @return Um Optional contendo o médico, se encontrado.
     */
    Optional<Medico> findByCpf(String cpf);

    /**
     * Busca um médico pelo seu CRM.
     * @param crm O CRM do médico.
     * @return Um Optional contendo o médico, se encontrado.
     */
    Optional<Medico> findByCrm(String crm);

    /**
     * Verifica se existe um médico com o email fornecido.
     * @param email O email a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe um médico com o CPF fornecido.
     * @param cpf O CPF a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByCpf(String cpf);

    /**
     * Verifica se existe um médico com o CRM fornecido.
     * @param crm O CRM a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByCrm(String crm);

    /**
     * Busca médicos por especialidade.
     * @param especialidade A especialidade a ser buscada.
     * @return Uma lista de médicos com a especialidade informada.
     */
    List<Medico> findByEspecialidadeIgnoreCase(String especialidade);

    /**
     * Busca médicos ativos por especialidade.
     * @param especialidade A especialidade a ser buscada.
     * @return Uma lista de médicos ativos com a especialidade informada.
     */
    @Query("SELECT m FROM Medico m WHERE m.ativo = true AND lower(m.especialidade) = lower(:especialidade)")
    List<Medico> findAtivosByEspecialidade(@Param("especialidade") String especialidade);

    /**
     * Busca todos os médicos ativos.
     * @return Uma lista de todos os médicos ativos.
     */
    List<Medico> findAllByAtivoTrue();
}

