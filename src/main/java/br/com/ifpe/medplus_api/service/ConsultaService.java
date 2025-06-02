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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    private final EmailService emailService; // Para notificações

    // Define a duração padrão de uma consulta (ex: 30 minutos, 1 hora)
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

    /**
     * Agenda uma nova consulta.
     *
     * @param pacienteId       ID do paciente.
     * @param medicoId         ID do médico.
     * @param dataHoraConsulta Data e hora da consulta.
     * @param observacoes      Observações para a consulta.
     * @return A consulta agendada.
     * @throws EntidadeNaoEncontradaException Se o paciente ou médico não forem encontrados.
     * @throws IllegalArgumentException       Se o horário não estiver disponível ou for inválido.
     */
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

        // 1. Validar se a dataHoraConsulta é futura
        if (dataHoraConsulta.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A data da consulta deve ser no futuro.");
        }

        // 2. Validar se o horário está dentro da disponibilidade do médico
        if (!isHorarioDisponivelParaMedico(medicoId, dataHoraConsulta)) {
            throw new IllegalArgumentException("Horário não disponível para o médico selecionado.");
        }

        // 3. Validar se o médico já tem consulta nesse horário (evitar duplicação)
        
        // 4. Validar se o paciente já tem consulta nesse horário
         if (consultaRepository.existsByPacienteIdAndDataHoraConsultaAndStatusNotIn(pacienteId, dataHoraConsulta,
                Arrays.asList(StatusConsulta.CANCELADA_ADMIN, StatusConsulta.CANCELADA_MEDICO, StatusConsulta.CANCELADA_PACIENTE, StatusConsulta.NAO_COMPARECEU, StatusConsulta.REAGENDADA))) {
            throw new IllegalArgumentException("Paciente já possui uma consulta agendada para este horário.");
        }


        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);
        consulta.setMedico(medico);
        consulta.setDataHoraConsulta(dataHoraConsulta);
        consulta.setObservacoes(observacoes);
        consulta.setStatus(StatusConsulta.AGENDADA);
        // consulta.setLinkAtendimentoOnline(...); // Gerar se for online

        Consulta consultaSalva = consultaRepository.save(consulta);
        logger.info("Consulta agendada ID {} para Paciente {} com Médico {} em {}",
                consultaSalva.getId(), paciente.getNome(), medico.getNome(), dataHoraConsulta);

        // Enviar email de confirmação
        emailService.sendConsultaAgendadaEmail(
                paciente.getEmail(),
                paciente.getNome(),
                medico.getNome(),
                dataHoraConsulta.toString() // Formatar adequadamente
        );
        // Notificar médico também, se necessário

        return consultaSalva;
    }

    /**
     * Verifica se um determinado horário está dentro da disponibilidade de um médico.
     *
     * @param medicoId         ID do médico.
     * @param dataHoraConsulta Data e hora da consulta desejada.
     * @return true se o horário estiver disponível, false caso contrário.
     */
    @Transactional(readOnly = true)
    public boolean isHorarioDisponivelParaMedico(Long medicoId, LocalDateTime dataHoraConsulta) {
        DayOfWeek diaDaSemana = dataHoraConsulta.getDayOfWeek();
        LocalTime horaDaConsulta = dataHoraConsulta.toLocalTime();
        LocalTime fimDaConsulta = horaDaConsulta.plus(DURACAO_CONSULTA_PADRAO); // Assumindo uma duração

        List<DisponibilidadeMedico> disponibilidades = disponibilidadeMedicoRepository
                .findByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, diaDaSemana);

        if (disponibilidades.isEmpty()) {
            return false; // Médico não tem disponibilidade cadastrada para este dia da semana.
        }

        for (DisponibilidadeMedico disp : disponibilidades) {
            // Verifica se a horaDaConsulta está DENTRO do intervalo [horaInicio, horaFim - DURACAO_CONSULTA_PADRAO]
            // E se a horaFimDaConsulta não ultrapassa a disp.horaFim
            if (!horaDaConsulta.isBefore(disp.getHoraInicio()) && !fimDaConsulta.isAfter(disp.getHoraFim())) {
                return true; // Encontrou um slot de disponibilidade que cobre a consulta.
            }
        }
        return false; // Nenhum slot de disponibilidade cobre o horário da consulta.
    }


    /**
     * Cancela uma consulta.
     *
     * @param consultaId O ID da consulta a ser cancelada.
     * @param motivo     O motivo do cancelamento.
     * @param canceladoPor Quem está cancelando (PACIENTE, MEDICO, ADMIN).
     * @return A consulta cancelada.
     * @throws EntidadeNaoEncontradaException Se a consulta não for encontrada.
     * @throws IllegalArgumentException       Se a consulta não puder ser cancelada (ex: já realizada).
     */
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

        // Notificar a outra parte (paciente ou médico) sobre o cancelamento
        // emailService.sendConsultaCanceladaEmail(...);

        return consultaCancelada;
    }

    /**
     * Reagenda uma consulta.
     *
     * @param consultaId      ID da consulta a ser reagendada.
     * @param novaDataHora    Nova data e hora para a consulta.
     * @param observacoesNova Novas observações (opcional).
     * @return A consulta reagendada.
     * @throws EntidadeNaoEncontradaException Se a consulta original não for encontrada.
     * @throws IllegalArgumentException       Se o novo horário não estiver disponível ou for inválido.
     */
    @Transactional
    public Consulta reagendarConsulta(Long consultaId, LocalDateTime novaDataHora, String observacoesNova) {
        Consulta consultaOriginal = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta original não encontrada com ID: " + consultaId));

        // Validar se a consulta pode ser reagendada (não cancelada, não realizada)
        if (consultaOriginal.getStatus() == StatusConsulta.REALIZADA ||
            consultaOriginal.getStatus().name().startsWith("CANCELADA") ||
            consultaOriginal.getStatus() == StatusConsulta.NAO_COMPARECEU) {
            throw new IllegalArgumentException("Consulta no status " + consultaOriginal.getStatus() + " não pode ser reagendada.");
        }

        // Utiliza a lógica de agendamento para validar o novo horário
        // e criar a nova consulta. Poderia ser mais complexo, envolvendo
        // a criação de uma nova consulta e o cancelamento/link da antiga.
        // Por simplicidade, vamos atualizar a existente.

        Long pacienteId = consultaOriginal.getPaciente().getId();
        Long medicoId = consultaOriginal.getMedico().getId();

        // 1. Validar se a novaDataHora é futura
        if (novaDataHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A nova data da consulta deve ser no futuro.");
        }

        // 2. Validar se o novo horário está dentro da disponibilidade do médico
        if (!isHorarioDisponivelParaMedico(medicoId, novaDataHora)) {
            throw new IllegalArgumentException("Novo horário não disponível para o médico selecionado.");
        }

        // 3. Validar se o médico já tem outra consulta nesse novo horário (excluindo a própria consulta sendo reagendada)
        Optional<Consulta> conflitoMedico = consultaRepository.findAll().stream()
            .filter(c -> c.getMedico().getId().equals(medicoId) &&
                         c.getDataHoraConsulta().equals(novaDataHora) &&
                         !c.getId().equals(consultaId) && // Exclui a própria consulta
                         Arrays.asList(StatusConsulta.AGENDADA, StatusConsulta.CONFIRMADA, StatusConsulta.REALIZADA).contains(c.getStatus()))
            .findFirst();
        if (conflitoMedico.isPresent()) {
            throw new IllegalArgumentException("Médico já possui outra consulta agendada para este novo horário.");
        }
        
        // 4. Validar se o paciente já tem outra consulta nesse novo horário (excluindo a própria consulta sendo reagendada)
        Optional<Consulta> conflitoPaciente = consultaRepository.findAll().stream()
            .filter(c -> c.getPaciente().getId().equals(pacienteId) &&
                         c.getDataHoraConsulta().equals(novaDataHora) &&
                         !c.getId().equals(consultaId) && // Exclui a própria consulta
                         Arrays.asList(StatusConsulta.AGENDADA, StatusConsulta.CONFIRMADA, StatusConsulta.REALIZADA).contains(c.getStatus()))
            .findFirst();
        if (conflitoPaciente.isPresent()) {
            throw new IllegalArgumentException("Paciente já possui outra consulta agendada para este novo horário.");
        }


        consultaOriginal.setDataHoraConsulta(novaDataHora);
        consultaOriginal.setStatus(StatusConsulta.REAGENDADA); // Ou voltar para AGENDADA
        if (observacoesNova != null && !observacoesNova.isBlank()) {
            consultaOriginal.setObservacoes(observacoesNova);
        }
        // Limpar motivo de cancelamento se houver
        consultaOriginal.setMotivoCancelamento(null);

        Consulta consultaReagendada = consultaRepository.save(consultaOriginal);
        logger.info("Consulta ID {} reagendada para {}", consultaId, novaDataHora);

        // Notificar paciente e médico sobre o reagendamento
        // emailService.sendConsultaReagendadaEmail(...);

        return consultaReagendada;
    }


    /**
     * Busca uma consulta pelo ID.
     *
     * @param id O ID da consulta.
     * @return A consulta encontrada.
     * @throws EntidadeNaoEncontradaException Se a consulta não for encontrada.
     */
    @Transactional(readOnly = true)
    public Consulta buscarPorId(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta não encontrada com ID: " + id));
    }

    /**
     * Lista todas as consultas de um paciente.
     * @param pacienteId ID do paciente.
     * @return Lista de consultas.
     */
    @Transactional(readOnly = true)
    public List<Consulta> listarConsultasPorPaciente(Long pacienteId) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new EntidadeNaoEncontradaException("Paciente não encontrado com ID: " + pacienteId);
        }
        return consultaRepository.findByPacienteIdOrderByDataHoraConsultaDesc(pacienteId);
    }

    /**
     * Lista todas as consultas de um médico.
     * @param medicoId ID do médico.
     * @return Lista de consultas.
     */
    @Transactional(readOnly = true)
    public List<Consulta> listarConsultasPorMedico(Long medicoId) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId);
        }
        return consultaRepository.findByMedicoIdOrderByDataHoraConsultaAsc(medicoId);
    }

    /**
     * Lista todas as consultas (para admin).
     * Considerar paginação.
     * @return Lista de todas as consultas.
     */
    @Transactional(readOnly = true)
    public List<Consulta> listarTodasConsultas() {
        return consultaRepository.findAll(); // Adicionar ordenação se necessário
    }

    /**
     * Atualiza o status de uma consulta.
     * @param consultaId ID da consulta.
     * @param novoStatus Novo status.
     * @return Consulta atualizada.
     */
    @Transactional
    public Consulta atualizarStatusConsulta(Long consultaId, StatusConsulta novoStatus) {
        Consulta consulta = buscarPorId(consultaId);
        // Adicionar validações de transição de status se necessário
        // Ex: não pode ir de CANCELADA para REALIZADA diretamente.
        consulta.setStatus(novoStatus);
        logger.info("Status da consulta ID {} atualizado para {}", consultaId, novoStatus);
        return consultaRepository.save(consulta);
    }
}

