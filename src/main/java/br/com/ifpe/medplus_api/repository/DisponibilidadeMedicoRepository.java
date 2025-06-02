package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.consulta.DisponibilidadeMedico;
import br.com.ifpe.medplus_api.model.medico.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade DisponibilidadeMedico.
 */
@Repository
public interface DisponibilidadeMedicoRepository extends JpaRepository<DisponibilidadeMedico, Long> {

    /**
     * Busca todas as disponibilidades de um médico específico.
     * @param medico O médico.
     * @return Lista de disponibilidades do médico.
     */
    List<DisponibilidadeMedico> findByMedico(Medico medico);

    /**
     * Busca todas as disponibilidades ativas de um médico específico.
     * @param medicoId O ID do médico.
     * @return Lista de disponibilidades ativas do médico.
     */
    List<DisponibilidadeMedico> findByMedicoIdAndAtivoTrue(Long medicoId);

    /**
     * Busca disponibilidades de um médico para um dia específico da semana.
     * @param medicoId O ID do médico.
     * @param diaSemana O dia da semana.
     * @return Lista de disponibilidades para o dia especificado.
     */
    List<DisponibilidadeMedico> findByMedicoIdAndDiaSemanaAndAtivoTrue(Long medicoId, DayOfWeek diaSemana);

    /**
     * Verifica se existe uma disponibilidade conflitante para um médico.
     * Conflito ocorre se o novo horário (horaInicioNova, horaFimNova) se sobrepõe
     * a um horário existente para o mesmo dia da semana.
     * Exclui a própria disponibilidade da verificação, caso esteja sendo atualizada.
     *
     * @param medicoId O ID do médico.
     * @param diaSemana O dia da semana.
     * @param horaInicioNova A nova hora de início.
     * @param horaFimNova A nova hora de fim.
     * @param idExcluir O ID da disponibilidade a ser excluída da verificação (útil ao atualizar).
     * @return true se houver conflito, false caso contrário.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
           "FROM DisponibilidadeMedico d " +
           "WHERE d.medico.id = :medicoId " +
           "AND d.diaSemana = :diaSemana " +
           "AND d.ativo = true " +
           "AND d.id <> :idExcluir " + // Exclui a própria disponibilidade se estiver atualizando
           "AND ((d.horaInicio < :horaFimNova AND d.horaFim > :horaInicioNova))")
    boolean existeConflitoHorario(
            @Param("medicoId") Long medicoId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicioNova") LocalTime horaInicioNova,
            @Param("horaFimNova") LocalTime horaFimNova,
            @Param("idExcluir") Long idExcluir // Passar 0L ou null se for uma nova disponibilidade
    );

    /**
     * Busca uma disponibilidade específica por médico, dia e horários.
     * @param medicoId ID do médico.
     * @param diaSemana Dia da semana.
     * @param horaInicio Hora de início.
     * @param horaFim Hora de fim.
     * @return Optional da disponibilidade.
     */
    Optional<DisponibilidadeMedico> findByMedicoIdAndDiaSemanaAndHoraInicioAndHoraFim(
            Long medicoId, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim
    );
}

