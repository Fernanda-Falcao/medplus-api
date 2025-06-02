package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.consulta.Consulta;
import br.com.ifpe.medplus_api.model.consulta.StatusConsulta;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para a entidade Consulta.
 */
@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    /**
     * Busca todas as consultas de um paciente específico.
     * @param paciente O paciente.
     * @return Lista de consultas do paciente.
     */
    List<Consulta> findByPaciente(Paciente paciente);

    /**
     * Busca todas as consultas de um paciente específico, ordenadas pela data da consulta.
     * @param pacienteId ID do paciente.
     * @return Lista de consultas do paciente ordenadas.
     */
    List<Consulta> findByPacienteIdOrderByDataHoraConsultaDesc(Long pacienteId);


    /**
     * Busca todas as consultas de um médico específico.
     * @param medico O médico.
     * @return Lista de consultas do médico.
     */
    List<Consulta> findByMedico(Medico medico);

    /**
     * Busca todas as consultas de um médico específico, ordenadas pela data da consulta.
     * @param medicoId ID do médico.
     * @return Lista de consultas do médico ordenadas.
     */
    List<Consulta> findByMedicoIdOrderByDataHoraConsultaAsc(Long medicoId);

    /**
     * Busca consultas por paciente e status.
     * @param paciente O paciente.
     * @param status O status da consulta.
     * @return Lista de consultas.
     */
    List<Consulta> findByPacienteAndStatus(Paciente paciente, StatusConsulta status);

    /**
     * Busca consultas por médico e status.
     * @param medico O médico.
     * @param status O status da consulta.
     * @return Lista de consultas.
     */
    List<Consulta> findByMedicoAndStatus(Medico medico, StatusConsulta status);

    /**
     * Busca consultas dentro de um período de tempo.
     * @param inicio Data e hora de início do período.
     * @param fim Data e hora de fim do período.
     * @return Lista de consultas no período.
     */
    List<Consulta> findByDataHoraConsultaBetween(LocalDateTime inicio, LocalDateTime fim);

    /**
     * Busca consultas de um médico específico dentro de um período de tempo.
     * @param medicoId ID do médico.
     * @param inicio Data e hora de início do período.
     * @param fim Data e hora de fim do período.
     * @return Lista de consultas.
     */
    @Query("SELECT c FROM Consulta c WHERE c.medico.id = :medicoId AND c.dataHoraConsulta >= :inicio AND c.dataHoraConsulta <= :fim ORDER BY c.dataHoraConsulta ASC")
    List<Consulta> findByMedicoIdAndDataHoraConsultaBetween(
            @Param("medicoId") Long medicoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Busca consultas de um paciente específico dentro de um período de tempo.
     * @param pacienteId ID do paciente.
     * @param inicio Data e hora de início do período.
     * @param fim Data e hora de fim do período.
     * @return Lista de consultas.
     */
    @Query("SELECT c FROM Consulta c WHERE c.paciente.id = :pacienteId AND c.dataHoraConsulta >= :inicio AND c.dataHoraConsulta <= :fim ORDER BY c.dataHoraConsulta ASC")
    List<Consulta> findByPacienteIdAndDataHoraConsultaBetween(
            @Param("pacienteId") Long pacienteId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Verifica se um médico possui alguma consulta agendada em um horário específico.
     * @param medicoId ID do médico.
     * @param dataHoraConsulta O horário da consulta.
     * @param statusExcluidos Lista de status que não devem ser considerados como conflito (ex: CANCELADA).
     * @return true se houver conflito, false caso contrário.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Consulta c WHERE c.medico.id = :medicoId " +
           "AND c.dataHoraConsulta = :dataHoraConsulta " +
           "AND c.status NOT IN :statusExcluidos")
    boolean existsByMedicoIdAndDataHoraConsultaAndStatusNotIn(
            @Param("medicoId") Long medicoId,
            @Param("dataHoraConsulta") LocalDateTime dataHoraConsulta,
            @Param("statusExcluidos") List<StatusConsulta> statusExcluidos
    );

     /**
     * Verifica se um paciente possui alguma consulta agendada em um horário específico.
     * @param pacienteId ID do paciente.
     * @param dataHoraConsulta O horário da consulta.
     * @param statusExcluidos Lista de status que não devem ser considerados como conflito (ex: CANCELADA).
     * @return true se houver conflito, false caso contrário.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Consulta c WHERE c.paciente.id = :pacienteId " +
           "AND c.dataHoraConsulta = :dataHoraConsulta " +
           "AND c.status NOT IN :statusExcluidos")
    boolean existsByPacienteIdAndDataHoraConsultaAndStatusNotIn(
            @Param("pacienteId") Long pacienteId,
            @Param("dataHoraConsulta") LocalDateTime dataHoraConsulta,
            @Param("statusExcluidos") List<StatusConsulta> statusExcluidos
    );

     boolean existsByMedicoIdAndDataHoraConsultaAndStatusNotInAndIdNot(Long medicoId, LocalDateTime novaDataHora,
            List<StatusConsulta> asList, Long consultaId);
}

