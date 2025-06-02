package br.com.ifpe.medplus_api.controller;

import br.com.ifpe.medplus_api.dto.DisponibilidadeRequest; // Será criado
import br.com.ifpe.medplus_api.dto.MedicoRequest;
import br.com.ifpe.medplus_api.dto.SenhaUpdateRequest;
import br.com.ifpe.medplus_api.model.consulta.Consulta;
import br.com.ifpe.medplus_api.model.consulta.DisponibilidadeMedico;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.service.*;
import br.com.ifpe.medplus_api.util.exception.EntidadeNaoEncontradaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para gerenciar endpoints relacionados a Médicos.
 */
@RestController
@RequestMapping("/medicos")
@Tag(name = "Médicos", description = "Endpoints para gerenciamento de médicos")
@SecurityRequirement(name = "bearerAuth")
public class MedicoController {

    private final MedicoService medicoService;
    private final ConsultaService consultaService;
    private final DisponibilidadeMedicoService disponibilidadeMedicoService;
    private final AuthService authService;

    
    public MedicoController(MedicoService medicoService,
                            ConsultaService consultaService,
                            DisponibilidadeMedicoService disponibilidadeMedicoService,
                            AuthService authService) {
        this.medicoService = medicoService;
        this.consultaService = consultaService;
        this.disponibilidadeMedicoService = disponibilidadeMedicoService;
        this.authService = authService;
    }

    /**
     * DTO para resposta de Médico (sem a senha).
     */
    private record MedicoResponse(Long id, String nome, String email, String cpf, String crm, String especialidade, String telefone) {
        static MedicoResponse fromMedico(Medico medico) {
            return new MedicoResponse(
                    medico.getId(),
                    medico.getNome(),
                    medico.getEmail(),
                    medico.getCpf(),
                    medico.getCrm(),
                    medico.getEspecialidade(),
                    medico.getTelefone()
            );
        }
    }

    /**
     * DTO para resposta de Consulta (visão do médico).
     */
    private record ConsultaMedicoResponse(Long id, String pacienteNome, String dataHora, String status, String observacoes) {
        static ConsultaMedicoResponse fromConsulta(Consulta consulta) {
            return new ConsultaMedicoResponse(
                consulta.getId(),
                consulta.getPaciente() != null ? consulta.getPaciente().getNome() : "N/A",
                consulta.getDataHoraConsulta() != null ? consulta.getDataHoraConsulta().toString() : "N/A",
                consulta.getStatus() != null ? consulta.getStatus().getDescricao() : "N/A",
                consulta.getObservacoes()
            );
        }
    }
    
    /**
     * DTO para resposta de DisponibilidadeMedico.
     */
    private record DisponibilidadeResponse(Long id, String diaSemana, String horaInicio, String horaFim, boolean ativo) {
        static DisponibilidadeResponse fromDisponibilidade(DisponibilidadeMedico d) {
            return new DisponibilidadeResponse(
                d.getId(),
                d.getDiaSemana().toString(),
                d.getHoraInicio().toString(),
                d.getHoraFim().toString(),
                d.isAtivo()
            );
        }
    }


    @Operation(summary = "Obter perfil do médico logado", description = "Retorna os dados do perfil do médico autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do médico retornado", content = @Content(schema = @Schema(implementation = MedicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Médico não encontrado")
    })
    @GetMapping("/meu-perfil")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> getMeuPerfil(Authentication authentication) {
        try {
            String email = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(email);
            return ResponseEntity.ok(MedicoResponse.fromMedico(medico));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Médico não encontrado."));
        }
    }

    @Operation(summary = "Atualizar perfil do médico logado", description = "Permite que o médico autenticado atualize seus dados cadastrais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(schema = @Schema(implementation = MedicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou erro de validação"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Médico não encontrado"),
            @ApiResponse(responseCode = "409", description = "Email, CPF ou CRM já em uso por outro usuário/médico")
    })
    @PutMapping("/meu-perfil")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> atualizarMeuPerfil(@Valid @RequestBody MedicoRequest medicoRequest, Authentication authentication) {
        try {
            String email = authentication.getName();
            Medico medicoLogado = medicoService.buscarPorEmail(email);
            Medico medicoAtualizado = medicoService.atualizarMedico(medicoLogado.getId(), medicoRequest);
            return ResponseEntity.ok(MedicoResponse.fromMedico(medicoAtualizado));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Médico não encontrado."));
        }
        // EntityExistsException será tratado pelo TratadorErros
    }

    @Operation(summary = "Mudar senha do médico logado", description = "Permite que o médico autenticado altere sua senha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: senha antiga incorreta, nova senha não atende critérios)"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @PostMapping("/meu-perfil/mudar-senha")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> mudarMinhaSenha(@Valid @RequestBody SenhaUpdateRequest senhaUpdateRequest, Authentication authentication) {
        try {
            authService.mudarSenha(authentication.getName(), senhaUpdateRequest.getSenhaAntiga(), senhaUpdateRequest.getNovaSenha());
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Excluir (desativar) conta do médico logado", description = "Permite que o médico autenticado desative sua própria conta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conta desativada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Médico não encontrado")
    })
    @DeleteMapping("/meu-perfil")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> excluirMinhaConta(Authentication authentication) {
        try {
            String email = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(email);
            medicoService.desativarMedico(medico.getId());
            // Adicionar lógica para lidar com consultas futuras, etc.
            return ResponseEntity.ok(Map.of("message", "Sua conta foi desativada com sucesso."));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Médico não encontrado."));
        }
    }

    // --- Endpoints de Consulta para Médicos ---

    @Operation(summary = "Listar minhas consultas agendadas (médico)", description = "Retorna a lista de todas as consultas agendadas para o médico logado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de consultas retornada", content = @Content(schema = @Schema(implementation = ConsultaMedicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Médico não encontrado")
    })
    @GetMapping("/minhas-consultas")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> getMinhasConsultas(Authentication authentication) {
        try {
            String email = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(email);
            List<Consulta> consultas = consultaService.listarConsultasPorMedico(medico.getId());
            List<ConsultaMedicoResponse> response = consultas.stream().map(ConsultaMedicoResponse::fromConsulta).collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Médico não encontrado."));
        }
    }

    @Operation(summary = "Cancelar consulta agendada (médico)", description = "Permite que o médico logado cancele uma consulta agendada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta cancelada com sucesso", content = @Content(schema = @Schema(implementation = ConsultaMedicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Não é possível cancelar a consulta"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Médico não tem permissão para cancelar esta consulta"),
            @ApiResponse(responseCode = "404", description = "Consulta não encontrada")
    })
    @PatchMapping("/consultas/{consultaId}/cancelar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> cancelarConsultaAgendada(
            @Parameter(description = "ID da consulta a ser cancelada", required = true) @PathVariable Long consultaId,
            @Parameter(description = "Motivo do cancelamento", required = true) @RequestParam String motivo,
            Authentication authentication) {
        try {
            String emailMedico = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(emailMedico);
            Consulta consulta = consultaService.buscarPorId(consultaId);

            if (!consulta.getMedico().getId().equals(medico.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Você não tem permissão para cancelar esta consulta."));
            }

            Consulta consultaCancelada = consultaService.cancelarConsulta(consultaId, motivo, br.com.ifpe.medplus_api.model.acesso.PerfilEnum.ROLE_MEDICO);
            return ResponseEntity.ok(ConsultaMedicoResponse.fromConsulta(consultaCancelada));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    // --- Endpoints de Disponibilidade para Médicos ---

    @Operation(summary = "Adicionar horário de disponibilidade (médico)", description = "Permite que o médico logado adicione um novo horário à sua agenda de disponibilidade.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Disponibilidade adicionada com sucesso", content = @Content(schema = @Schema(implementation = DisponibilidadeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou conflito de horário"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Médico não encontrado")
    })
    @PostMapping("/disponibilidade")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> adicionarDisponibilidade(@Valid @RequestBody DisponibilidadeRequest request, Authentication authentication) {
        try {
            String emailMedico = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(emailMedico);
            
            DayOfWeek diaSemana = DayOfWeek.valueOf(request.getDiaSemana().toUpperCase());
            LocalTime horaInicio = LocalTime.parse(request.getHoraInicio());
            LocalTime horaFim = LocalTime.parse(request.getHoraFim());

            DisponibilidadeMedico novaDisponibilidade = disponibilidadeMedicoService.adicionarDisponibilidade(
                    medico.getId(), diaSemana, horaInicio, horaFim);
            return ResponseEntity.status(HttpStatus.CREATED).body(DisponibilidadeResponse.fromDisponibilidade(novaDisponibilidade));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Listar meus horários de disponibilidade (médico)", description = "Retorna a lista de horários de disponibilidade do médico logado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de disponibilidades retornada", content = @Content(schema = @Schema(implementation = DisponibilidadeResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Médico não encontrado")
    })
    @GetMapping("/disponibilidade")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> listarMinhasDisponibilidades(Authentication authentication) {
        try {
            String emailMedico = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(emailMedico);
            List<DisponibilidadeMedico> disponibilidades = disponibilidadeMedicoService.listarDisponibilidadesPorMedico(medico.getId());
            List<DisponibilidadeResponse> response = disponibilidades.stream()
                                                                  .map(DisponibilidadeResponse::fromDisponibilidade)
                                                                  .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Remover (desativar) horário de disponibilidade (médico)", description = "Permite que o médico logado remova (desative) um de seus horários de disponibilidade.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disponibilidade removida/desativada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Médico não tem permissão para remover esta disponibilidade"),
        @ApiResponse(responseCode = "404", description = "Disponibilidade ou Médico não encontrado")
    })
    @DeleteMapping("/disponibilidade/{disponibilidadeId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> removerDisponibilidade(
            @Parameter(description = "ID da disponibilidade a ser removida", required = true) @PathVariable Long disponibilidadeId,
            Authentication authentication) {
        try {
            String emailMedico = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(emailMedico);
            DisponibilidadeMedico disponibilidade = disponibilidadeMedicoService.buscarPorId(disponibilidadeId);

            if (!disponibilidade.getMedico().getId().equals(medico.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Você não tem permissão para remover esta disponibilidade."));
            }
            disponibilidadeMedicoService.removerDisponibilidade(disponibilidadeId);
            return ResponseEntity.ok(Map.of("message", "Disponibilidade removida com sucesso."));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(summary = "Atualizar horário de disponibilidade (médico)", description = "Permite que o médico logado atualize um de seus horários de disponibilidade.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disponibilidade atualizada com sucesso", content = @Content(schema = @Schema(implementation = DisponibilidadeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou conflito de horário"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Médico não tem permissão para atualizar esta disponibilidade"),
        @ApiResponse(responseCode = "404", description = "Disponibilidade ou Médico não encontrado")
    })
    @PutMapping("/disponibilidade/{disponibilidadeId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<?> atualizarDisponibilidade(
            @Parameter(description = "ID da disponibilidade a ser atualizada", required = true) @PathVariable Long disponibilidadeId,
            @Valid @RequestBody DisponibilidadeRequest request,
            Authentication authentication) {
        try {
            String emailMedico = authentication.getName();
            Medico medico = medicoService.buscarPorEmail(emailMedico);
            DisponibilidadeMedico disponibilidadeExistente = disponibilidadeMedicoService.buscarPorId(disponibilidadeId);

            if (!disponibilidadeExistente.getMedico().getId().equals(medico.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Você não tem permissão para atualizar esta disponibilidade."));
            }

            DayOfWeek diaSemana = DayOfWeek.valueOf(request.getDiaSemana().toUpperCase());
            LocalTime horaInicio = LocalTime.parse(request.getHoraInicio());
            LocalTime horaFim = LocalTime.parse(request.getHoraFim());
            // O campo 'ativo' pode vir do request ou ser gerenciado de outra forma.
            // Por simplicidade, vamos assumir que se está atualizando, quer manter/tornar ativo.
            // Se o request tiver um campo 'ativo', use-o: boolean ativo = request.isAtivo();
            boolean ativo = request.isAtivo();


            DisponibilidadeMedico disponibilidadeAtualizada = disponibilidadeMedicoService.atualizarDisponibilidade(
                    disponibilidadeId, diaSemana, horaInicio, horaFim, ativo);
            return ResponseEntity.ok(DisponibilidadeResponse.fromDisponibilidade(disponibilidadeAtualizada));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // O DTO DisponibilidadeRequest precisa ser criado.
}

