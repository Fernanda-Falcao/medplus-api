
package br.com.ifpe.medplus_api.repository;

import br.com.ifpe.medplus_api.model.consulta.Consulta;
import br.com.ifpe.medplus_api.model.consulta.StatusConsulta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByPacienteIdOrderByDataHoraConsultaDesc(Long pacienteId);

    List<Consulta> findByMedicoIdOrderByDataHoraConsultaAsc(Long medicoId);

    @Query("SELECT c FROM Consulta c WHERE c.medico.id = :medicoId AND c.dataHoraConsulta >= :inicio AND c.dataHoraConsulta <= :fim ORDER BY c.dataHoraConsulta ASC")
    List<Consulta> findByMedicoIdAndDataHoraConsultaBetween(
            @Param("medicoId") Long medicoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c WHERE c.paciente.id = :pacienteId AND c.dataHoraConsulta >= :inicio AND c.dataHoraConsulta <= :fim ORDER BY c.dataHoraConsulta ASC")
    List<Consulta> findByPacienteIdAndDataHoraConsultaBetween(
            @Param("pacienteId") Long pacienteId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    List<Consulta> findByMedicoIdAndDataHoraConsultaAfterOrderByDataHoraConsultaAsc(Long medicoId, LocalDateTime dataHoraConsulta, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Consulta c WHERE c.medico.id = :medicoId " +
           "AND c.dataHoraConsulta = :dataHoraConsulta " +
           "AND c.status NOT IN :statusExcluidos")
    boolean existsByMedicoIdAndDataHoraConsultaAndStatusNotIn(
            @Param("medicoId") Long medicoId,
            @Param("dataHoraConsulta") LocalDateTime dataHoraConsulta,
            @Param("statusExcluidos") List<StatusConsulta> statusExcluidos);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Consulta c WHERE c.paciente.id = :pacienteId " +
           "AND c.dataHoraConsulta = :dataHoraConsulta " +
           "AND c.status NOT IN :statusExcluidos")
    boolean existsByPacienteIdAndDataHoraConsultaAndStatusNotIn(
            @Param("pacienteId") Long pacienteId,
            @Param("dataHoraConsulta") LocalDateTime dataHoraConsulta,
            @Param("statusExcluidos") List<StatusConsulta> statusExcluidos);

    // Métodos para verificar existência ignorando uma consulta específica (para reagendamento)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Consulta c WHERE c.medico.id = :medicoId " +
           "AND c.dataHoraConsulta = :dataHoraConsulta " +
           "AND c.status NOT IN :statusExcluidos " +
           "AND c.id <> :consultaId")
    boolean existsByMedicoIdAndDataHoraConsultaAndStatusNotInAndIdNot(
            @Param("medicoId") Long medicoId,
            @Param("dataHoraConsulta") LocalDateTime dataHoraConsulta,
            @Param("statusExcluidos") List<StatusConsulta> statusExcluidos,
            @Param("consultaId") Long consultaId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Consulta c WHERE c.paciente.id = :pacienteId " +
           "AND c.dataHoraConsulta = :dataHoraConsulta " +
           "AND c.status NOT IN :statusExcluidos " +
           "AND c.id <> :consultaId")
    boolean existsByPacienteIdAndDataHoraConsultaAndStatusNotInAndIdNot(
            @Param("pacienteId") Long pacienteId,
            @Param("dataHoraConsulta") LocalDateTime dataHoraConsulta,
            @Param("statusExcluidos") List<StatusConsulta> statusExcluidos,
            @Param("consultaId") Long consultaId);

}
