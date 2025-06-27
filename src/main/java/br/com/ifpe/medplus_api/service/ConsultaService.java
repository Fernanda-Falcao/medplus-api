package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.consulta.Consulta;
import br.com.ifpe.medplus_api.model.consulta.DisponibilidadeMedico;
import br.com.ifpe.medplus_api.model.consulta.StatusConsulta;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.repository.ConsultaRepository;
import br.com.ifpe.medplus_api.repository.DisponibilidadeMedicoRepository;
import br.com.ifpe.medplus_api.repository.MedicoRepository;
import br.com.ifpe.medplus_api.repository.PacienteRepository;
import br.com.ifpe.medplus_api.util.exception.EntidadeNaoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Serviço para gerenciar operações relacionadas a Consultas.
 */
@Service
public class ConsultaService {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaService.class);

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final DisponibilidadeMedicoRepository disponibilidadeMedicoRepository;
    private final EmailService emailService;

    private static final Duration DURACAO_CONSULTA_PADRAO = Duration.ofMinutes(30);

    public ConsultaService(ConsultaRepository consultaRepository,
                           PacienteRepository pacienteRepository,
                           MedicoRepository medicoRepository,
                           DisponibilidadeMedicoRepository disponibilidadeMedicoRepository,
                           EmailService emailService) {
        this.consultaRepository = consultaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.disponibilidadeMedicoRepository = disponibilidadeMedicoRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Consulta agendarConsulta(Long pacienteId, Long medicoId, LocalDateTime dataHoraConsulta, String observacoes) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Paciente não encontrado com ID: " + pacienteId));
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId));

        if (!medico.isAtivo()) {
            throw new IllegalArgumentException("Médico " + medico.getNome() + " não está ativo.");
        }
        if (!paciente.isAtivo()) {
            throw new IllegalArgumentException("Paciente " + paciente.getNome() + " não está ativo.");
        }

        if (dataHoraConsulta.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A data da consulta deve ser no futuro.");
        }

        if (!isHorarioDisponivelParaMedico(medicoId, dataHoraConsulta)) {
            throw new IllegalArgumentException("Horário não disponível para o médico selecionado.");
        }

        // Validar se o médico já tem consulta nesse horário (status relevantes)
        List<StatusConsulta> statusExcluidos = Arrays.asList(
            StatusConsulta.CANCELADA_ADMIN, StatusConsulta.CANCELADA_MEDICO,
            StatusConsulta.CANCELADA_PACIENTE, StatusConsulta.NAO_COMPARECEU, StatusConsulta.REAGENDADA);

        boolean medicoTemConsultaConflito = consultaRepository.existsByMedicoIdAndDataHoraConsultaAndStatusNotIn(
            medicoId, dataHoraConsulta, statusExcluidos);

        if (medicoTemConsultaConflito) {
            throw new IllegalArgumentException("Médico já possui uma consulta agendada para este horário.");
        }

        boolean pacienteTemConsultaConflito = consultaRepository.existsByPacienteIdAndDataHoraConsultaAndStatusNotIn(
            pacienteId, dataHoraConsulta, statusExcluidos);

        if (pacienteTemConsultaConflito) {
            throw new IllegalArgumentException("Paciente já possui uma consulta agendada para este horário.");
        }

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);
        consulta.setMedico(medico);
        consulta.setDataHoraConsulta(dataHoraConsulta);
        consulta.setObservacoes(observacoes);
        consulta.setStatus(StatusConsulta.AGENDADA);

        Consulta consultaSalva = consultaRepository.save(consulta);
        logger.info("Consulta agendada ID {} para Paciente {} com Médico {} em {}",
                consultaSalva.getId(), paciente.getNome(), medico.getNome(), dataHoraConsulta);

        emailService.sendConsultaAgendadaEmail(paciente.getEmail(), paciente.getNome(), medico.getNome(), dataHoraConsulta.toString());

        // Notificar médico, se desejar

        return consultaSalva;
    }

    @Transactional(readOnly = true)
    public boolean isHorarioDisponivelParaMedico(Long medicoId, LocalDateTime dataHoraConsulta) {
        DayOfWeek diaDaSemana = dataHoraConsulta.getDayOfWeek();
        LocalTime horaDaConsulta = dataHoraConsulta.toLocalTime();
        LocalTime fimDaConsulta = horaDaConsulta.plus(DURACAO_CONSULTA_PADRAO);

        List<DisponibilidadeMedico> disponibilidades = disponibilidadeMedicoRepository.findByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, diaDaSemana);

        if (disponibilidades.isEmpty()) {
            return false;
        }

        for (DisponibilidadeMedico disp : disponibilidades) {
            if (!horaDaConsulta.isBefore(disp.getHoraInicio()) && !fimDaConsulta.isAfter(disp.getHoraFim())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarConsultasPorMedicoEData(Long medicoId, LocalDate data) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId);
        }
        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.atTime(23, 59, 59);
        return consultaRepository.findByMedicoIdAndDataHoraConsultaBetween(medicoId, inicioDoDia, fimDoDia);
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarProximasConsultasPorMedico(Long medicoId, LocalDateTime aPartirDe, int limite) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId);
        }
        Pageable pageable = PageRequest.of(0, limite);
        return consultaRepository.findByMedicoIdAndDataHoraConsultaAfterOrderByDataHoraConsultaAsc(medicoId, aPartirDe, pageable);
    }

    @Transactional
    public Consulta cancelarConsulta(Long consultaId, String motivo, PerfilEnum canceladoPor) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta não encontrada com ID: " + consultaId));

        if (consulta.getStatus() == StatusConsulta.REALIZADA ||
            consulta.getStatus() == StatusConsulta.NAO_COMPARECEU) {
            throw new IllegalArgumentException("Não é possível cancelar uma consulta que já foi realizada ou marcada como não compareceu.");
        }
        if (consulta.getStatus().name().startsWith("CANCELADA")) {
             throw new IllegalArgumentException("Consulta já está cancelada.");
        }

        switch (canceladoPor) {
            case ROLE_PACIENTE:
                consulta.setStatus(StatusConsulta.CANCELADA_PACIENTE);
                break;
            case ROLE_MEDICO:
                consulta.setStatus(StatusConsulta.CANCELADA_MEDICO);
                break;
            case ROLE_ADMIN:
                consulta.setStatus(StatusConsulta.CANCELADA_ADMIN);
                break;
            default:
                throw new IllegalArgumentException("Perfil inválido para cancelamento.");
        }

        consulta.setMotivoCancelamento(motivo);
        Consulta consultaCancelada = consultaRepository.save(consulta);
        logger.info("Consulta ID {} cancelada por {}. Motivo: {}", consultaId, canceladoPor.getNome(), motivo);

        // Notificar paciente e médico

        return consultaCancelada;
    }

    @Transactional
    public Consulta reagendarConsulta(Long consultaId, LocalDateTime novaDataHora, String observacoesNova) {
        Consulta consultaOriginal = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta original não encontrada com ID: " + consultaId));

        if (consultaOriginal.getStatus() == StatusConsulta.REALIZADA ||
            consultaOriginal.getStatus().name().startsWith("CANCELADA") ||
            consultaOriginal.getStatus() == StatusConsulta.NAO_COMPARECEU) {
            throw new IllegalArgumentException("Consulta no status " + consultaOriginal.getStatus() + " não pode ser reagendada.");
        }

        if (novaDataHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A nova data da consulta deve ser no futuro.");
        }

        Long medicoId = consultaOriginal.getMedico().getId();
        Long pacienteId = consultaOriginal.getPaciente().getId();

        if (!isHorarioDisponivelParaMedico(medicoId, novaDataHora)) {
            throw new IllegalArgumentException("Novo horário não disponível para o médico selecionado.");
        }

        List<StatusConsulta> statusValidos = Arrays.asList(StatusConsulta.AGENDADA, StatusConsulta.CONFIRMADA, StatusConsulta.REALIZADA);

        List<StatusConsulta> statusExcluidos = null;
        boolean medicoConflito = consultaRepository.existsByMedicoIdAndDataHoraConsultaAndStatusNotInAndIdNot(
            medicoId, novaDataHora, statusExcluidos, consultaId);

        if (medicoConflito) {
            throw new IllegalArgumentException("Médico já possui outra consulta agendada para este novo horário.");
        }

        boolean pacienteConflito = consultaRepository.existsByPacienteIdAndDataHoraConsultaAndStatusNotInAndIdNot(
            pacienteId, novaDataHora, statusExcluidos, consultaId);

        if (pacienteConflito) {
            throw new IllegalArgumentException("Paciente já possui outra consulta agendada para este novo horário.");
        }

        consultaOriginal.setDataHoraConsulta(novaDataHora);
        consultaOriginal.setStatus(StatusConsulta.REAGENDADA);
        if (observacoesNova != null && !observacoesNova.isBlank()) {
            consultaOriginal.setObservacoes(observacoesNova);
        }
        consultaOriginal.setMotivoCancelamento(null);

        Consulta consultaReagendada = consultaRepository.save(consultaOriginal);
        logger.info("Consulta ID {} reagendada para {}", consultaId, novaDataHora);

        // Notificar paciente e médico

        return consultaReagendada;
    }

    @Transactional(readOnly = true)
    public Consulta buscarPorId(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarConsultasPorPaciente(Long pacienteId) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new EntidadeNaoEncontradaException("Paciente não encontrado com ID: " + pacienteId);
        }
        return consultaRepository.findByPacienteIdOrderByDataHoraConsultaDesc(pacienteId);
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarConsultasPorMedico(Long medicoId) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId);
        }
        return consultaRepository.findByMedicoIdOrderByDataHoraConsultaAsc(medicoId);
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarTodasConsultas() {
        return consultaRepository.findAll();
    }

    @Transactional
    public Consulta atualizarStatusConsulta(Long consultaId, StatusConsulta novoStatus) {
        Consulta consulta = buscarPorId(consultaId);
        consulta.setStatus(novoStatus);
        logger.info("Status da consulta ID {} atualizado para {}", consultaId, novoStatus);
        return consultaRepository.save(consulta);
    }
}

// Fim do código

