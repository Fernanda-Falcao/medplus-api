package br.com.ifpe.medplus_api.model.consulta;

import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.util.entity.EntidadeNegocio; // Apenas ID
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Entidade para registrar os horários de disponibilidade de um médico.
 * Um médico pode ter vários registros de disponibilidade (ex: Segundas das 08h às 12h, Terças das 14h às 18h).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_disponibilidade_medico", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"medico_id", "dia_semana", "hora_inicio", "hora_fim"}, name = "uk_disponibilidade_medico_horario")
})
public class DisponibilidadeMedico extends EntidadeNegocio { // EntidadeNegocio para o ID

    @NotNull(message = "Médico é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @NotNull(message = "Dia da semana é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 15) // Ex: MONDAY, TUESDAY
    private DayOfWeek diaSemana;

    @NotNull(message = "Hora de início é obrigatória")
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull(message = "Hora de fim é obrigatória")
    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true; // Se este slot de disponibilidade está ativo ou não

    // Campos de auditoria simples (sem herdar EntidadeAuditavel para manter a tabela mais leve, se desejado)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date dataCriacao;

    @Column(name = "data_atualizacao")
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataAtualizacao = dataCriacao = new java.util.Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = new java.util.Date();
    }

    // Construtor
    public DisponibilidadeMedico(Medico medico, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        this.medico = medico;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.ativo = true;
    }

    @Override
    public String toString() {
        return "DisponibilidadeMedico{" +
                "id=" + getId() +
                ", medico=" + (medico != null ? medico.getNome() : "N/A") +
                ", diaSemana=" + diaSemana +
                ", horaInicio=" + horaInicio +
                ", horaFim=" + horaFim +
                ", ativo=" + ativo +
                '}';
    }
}
