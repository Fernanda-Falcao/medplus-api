package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.model.consulta.DisponibilidadeMedico;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.repository.DisponibilidadeMedicoRepository;
import br.com.ifpe.medplus_api.repository.MedicoRepository;
import br.com.ifpe.medplus_api.util.exception.EntidadeNaoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Serviço para gerenciar a disponibilidade dos médicos.
 */
@Service
public class DisponibilidadeMedicoService {

    private static final Logger logger = LoggerFactory.getLogger(DisponibilidadeMedicoService.class);

    private final DisponibilidadeMedicoRepository disponibilidadeRepository;
    private final MedicoRepository medicoRepository;

    
    public DisponibilidadeMedicoService(DisponibilidadeMedicoRepository disponibilidadeRepository,
                                        MedicoRepository medicoRepository) {
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.medicoRepository = medicoRepository;
    }

    /**
     * Adiciona um novo horário de disponibilidade para um médico.
     *
     * @param medicoId   ID do médico.
     * @param diaSemana  Dia da semana.
     * @param horaInicio Hora de início da disponibilidade.
     * @param horaFim    Hora de fim da disponibilidade.
     * @return A disponibilidade criada.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     * @throws IllegalArgumentException       Se o horário for inválido ou conflitante.
     */
    @Transactional
    public DisponibilidadeMedico adicionarDisponibilidade(Long medicoId, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId));

        if (horaInicio.isAfter(horaFim) || horaInicio.equals(horaFim)) {
            throw new IllegalArgumentException("Hora de início deve ser anterior à hora de fim.");
        }

        // Verificar conflitos com horários existentes do mesmo médico para o mesmo dia
        // O ID a excluir é 0L ou null pois é uma nova disponibilidade.
        if (disponibilidadeRepository.existeConflitoHorario(medicoId, diaSemana, horaInicio, horaFim, 0L)) {
            throw new IllegalArgumentException("Conflito de horário. Já existe uma disponibilidade ativa neste período para o médico.");
        }
        
        // Verifica se já existe um registro idêntico (mesmo que inativo, para evitar duplicidade lógica)
        // Poderia ser uma constraint no banco também.
        if (disponibilidadeRepository.findByMedicoIdAndDiaSemanaAndHoraInicioAndHoraFim(medicoId, diaSemana, horaInicio, horaFim).isPresent()){
             throw new IllegalArgumentException("Este exato horário de disponibilidade já está cadastrado para o médico.");
        }


        DisponibilidadeMedico disponibilidade = new DisponibilidadeMedico(medico, diaSemana, horaInicio, horaFim);
        disponibilidade.setAtivo(true); // Por padrão, nova disponibilidade é ativa

        DisponibilidadeMedico salvo = disponibilidadeRepository.save(disponibilidade);
        logger.info("Disponibilidade adicionada para Médico ID {}: {} de {} às {}", medicoId, diaSemana, horaInicio, horaFim);
        return salvo;
    }

    /**
     * Lista todas as disponibilidades de um médico.
     *
     * @param medicoId ID do médico.
     * @return Lista de disponibilidades.
     */
    @Transactional(readOnly = true)
    public List<DisponibilidadeMedico> listarDisponibilidadesPorMedico(Long medicoId) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + medicoId);
        }
        return disponibilidadeRepository.findByMedicoIdAndAtivoTrue(medicoId);
    }

    /**
     * Remove (desativa) um horário de disponibilidade.
     *
     * @param disponibilidadeId ID da disponibilidade a ser removida/desativada.
     * @throws EntidadeNaoEncontradaException Se a disponibilidade não for encontrada.
     */
    @Transactional
    public void removerDisponibilidade(Long disponibilidadeId) {
        DisponibilidadeMedico disponibilidade = disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Disponibilidade não encontrada com ID: " + disponibilidadeId));

        // Em vez de deletar, marcamos como inativa.
        // disponibilidadeRepository.delete(disponibilidade);
        disponibilidade.setAtivo(false);
        disponibilidadeRepository.save(disponibilidade);
        logger.info("Disponibilidade ID {} desativada.", disponibilidadeId);
    }
    
    /**
     * Atualiza um horário de disponibilidade.
     *
     * @param disponibilidadeId ID da disponibilidade a ser atualizada.
     * @param novoDiaSemana Novo dia da semana.
     * @param novaHoraInicio Nova hora de início.
     * @param novaHoraFim Nova hora de fim.
     * @param ativo Novo status de ativação.
     * @return A disponibilidade atualizada.
     * @throws EntidadeNaoEncontradaException Se a disponibilidade não for encontrada.
     * @throws IllegalArgumentException Se o novo horário for inválido ou conflitante.
     */
    @Transactional
    public DisponibilidadeMedico atualizarDisponibilidade(Long disponibilidadeId, DayOfWeek novoDiaSemana, LocalTime novaHoraInicio, LocalTime novaHoraFim, boolean ativo) {
        DisponibilidadeMedico disponibilidade = disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Disponibilidade não encontrada com ID: " + disponibilidadeId));

        if (novaHoraInicio.isAfter(novaHoraFim) || novaHoraInicio.equals(novaHoraFim)) {
            throw new IllegalArgumentException("Nova hora de início deve ser anterior à nova hora de fim.");
        }

        // Se estiver ativo, verificar conflitos, excluindo a própria disponibilidade da checagem
        if (ativo && disponibilidadeRepository.existeConflitoHorario(
                disponibilidade.getMedico().getId(), novoDiaSemana, novaHoraInicio, novaHoraFim, disponibilidadeId)) {
            throw new IllegalArgumentException("Conflito de horário. Já existe outra disponibilidade ativa neste novo período para o médico.");
        }
        
        // Verificar se a alteração não cria uma duplicata exata de outra disponibilidade ativa
        // (exceto ela mesma se os horários não mudarem)
        if (ativo) {
            disponibilidadeRepository.findByMedicoIdAndDiaSemanaAndHoraInicioAndHoraFim(
                disponibilidade.getMedico().getId(), novoDiaSemana, novaHoraInicio, novaHoraFim)
                .ifPresent(existente -> {
                    if (!existente.getId().equals(disponibilidadeId) && existente.isAtivo()) {
                        throw new IllegalArgumentException("Este exato horário de disponibilidade já está cadastrado e ativo para o médico.");
                    }
                });
        }


        disponibilidade.setDiaSemana(novoDiaSemana);
        disponibilidade.setHoraInicio(novaHoraInicio);
        disponibilidade.setHoraFim(novaHoraFim);
        disponibilidade.setAtivo(ativo);

        DisponibilidadeMedico atualizada = disponibilidadeRepository.save(disponibilidade);
        logger.info("Disponibilidade ID {} atualizada.", disponibilidadeId);
        return atualizada;
    }

    /**
     * Busca uma disponibilidade pelo ID.
     * @param disponibilidadeId ID da disponibilidade.
     * @return A disponibilidade encontrada.
     * @throws EntidadeNaoEncontradaException Se não encontrada.
     */
    @Transactional(readOnly = true)
    public DisponibilidadeMedico buscarPorId(Long disponibilidadeId) {
        return disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Disponibilidade não encontrada com ID: " + disponibilidadeId));
    }
}


