package br.com.ifpe.medplus_api.model.consulta;

import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.util.entity.EntidadeAuditavel;
// import br.com.ifpe.medplus_api.util.entity.EntidadeNegocio;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
// import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma consulta médica agendada.
 * Herda de EntidadeNegocio (para ID) e EntidadeAuditavel (para datas de criação/atualização).
 */
@Getter
@Setter
@Entity
@Table(name = "tb_consulta")
public class Consulta extends EntidadeAuditavel {

    @NotNull(message = "Paciente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @NotNull(message = "Médico é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @NotNull(message = "Data e hora da consulta são obrigatórias")
    @FutureOrPresent(message = "A data da consulta deve ser no presente ou futuro")
    @Column(name = "data_hora_consulta", nullable = false)
    private LocalDateTime dataHoraConsulta;

    @NotNull(message = "Status da consulta é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status_consulta", nullable = false, length = 30)
    private StatusConsulta status;

    @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Size(max = 500, message = "Motivo do cancelamento deve ter no máximo 500 caracteres")
    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @Column(name = "link_atendimento_online", length = 255)
    private String linkAtendimentoOnline;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = StatusConsulta.AGENDADA;
        }
    }

    public Consulta() {}

    public Consulta(Paciente paciente, Medico medico, LocalDateTime dataHoraConsulta, String observacoes) {
        this.paciente = paciente;
        this.medico = medico;
        this.dataHoraConsulta = dataHoraConsulta;
        this.observacoes = observacoes;
        this.status = StatusConsulta.AGENDADA;
    }

    @Override
    public String toString() {
        return "Consulta{" +
                "id=" + getId() +
                ", paciente=" + (paciente != null ? paciente.getNome() : "N/A") +
                ", medico=" + (medico != null ? medico.getNome() : "N/A") +
                ", dataHoraConsulta=" + dataHoraConsulta +
                ", status=" + status +
                '}';
    }
}
