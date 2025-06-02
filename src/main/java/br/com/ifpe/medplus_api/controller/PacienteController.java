package br.com.ifpe.medplus_api.controller;

import br.com.ifpe.medplus_api.dto.PacienteRequest;
import br.com.ifpe.medplus_api.dto.SenhaUpdateRequest; // Será criado
import br.com.ifpe.medplus_api.model.consulta.Consulta;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.service.AuthService;
import br.com.ifpe.medplus_api.service.ConsultaService;
import br.com.ifpe.medplus_api.service.PacienteService;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para gerenciar endpoints relacionados a Pacientes.
 */
@RestController
@RequestMapping("/pacientes")
@Tag(name = "Pacientes", description = "Endpoints para gerenciamento de pacientes")
@SecurityRequirement(name = "bearerAuth") // Indica que os endpoints aqui exigem autenticação Bearer
public class PacienteController {

    private final PacienteService pacienteService;
    private final ConsultaService consultaService;
    private final AuthService authService;

    
    public PacienteController(PacienteService pacienteService, ConsultaService consultaService, AuthService authService) {
        this.pacienteService = pacienteService;
        this.consultaService = consultaService;
        this.authService = authService;
    }

    /**
     * DTO para resposta de Paciente (sem a senha).
     */
    private record PacienteResponse(Long id, String nome, String email, String cpf, String telefone, String historicoMedico) {
        static PacienteResponse fromPaciente(Paciente paciente) {
            return new PacienteResponse(
                    paciente.getId(),
                    paciente.getNome(),
                    paciente.getEmail(),
                    paciente.getCpf(),
                    paciente.getTelefone(),
                    paciente.getHistoricoMedico()
            );
        }
    }
    
    /**
     * DTO para resposta simplificada de Consulta.
     */
    private record ConsultaResponse(Long id, String medicoNome, String especialidadeMedico, String dataHora, String status, String observacoes) {
        static ConsultaResponse fromConsulta(Consulta consulta) {
            return new ConsultaResponse(
                consulta.getId(),
                consulta.getMedico() != null ? consulta.getMedico().getNome() : "N/A",
                consulta.getMedico() != null ? consulta.getMedico().getEspecialidade() : "N/A",
                consulta.getDataHoraConsulta() != null ? consulta.getDataHoraConsulta().toString() : "N/A",
                consulta.getStatus() != null ? consulta.getStatus().getDescricao() : "N/A",
                consulta.getObservacoes()
            );
        }
    }


    @Operation(summary = "Obter perfil do paciente logado", description = "Retorna os dados do perfil do paciente autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do paciente retornado", content = @Content(schema = @Schema(implementation = PacienteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    @GetMapping("/meu-perfil")
    @PreAuthorize("hasRole('PACIENTE')") // Apenas pacientes podem acessar seu próprio perfil
    public ResponseEntity<?> getMeuPerfil(Authentication authentication) {
        try {
            String email = authentication.getName();
            Paciente paciente = pacienteService.buscarPorEmail(email);
            return ResponseEntity.ok(PacienteResponse.fromPaciente(paciente));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paciente não encontrado."));
        }
    }

    @Operation(summary = "Atualizar perfil do paciente logado", description = "Permite que o paciente autenticado atualize seus dados cadastrais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(schema = @Schema(implementation = PacienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou erro de validação"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado"),
            @ApiResponse(responseCode = "409", description = "Email ou CPF já em uso por outro usuário")
    })
    @PutMapping("/meu-perfil")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> atualizarMeuPerfil(@Valid @RequestBody PacienteRequest pacienteRequest, Authentication authentication) {
        try {
            String email = authentication.getName();
            Paciente pacienteLogado = pacienteService.buscarPorEmail(email);
            Paciente pacienteAtualizado = pacienteService.atualizarPaciente(pacienteLogado.getId(), pacienteRequest);
            return ResponseEntity.ok(PacienteResponse.fromPaciente(pacienteAtualizado));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paciente não encontrado."));
        }
        // EntityExistsException será tratado pelo TratadorErros
    }

    @Operation(summary = "Mudar senha do paciente logado", description = "Permite que o paciente autenticado altere sua senha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: senha antiga incorreta, nova senha não atende critérios)"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @PostMapping("/meu-perfil/mudar-senha")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> mudarMinhaSenha(@Valid @RequestBody SenhaUpdateRequest senhaUpdateRequest, Authentication authentication) {
        try {
            authService.mudarSenha(authentication.getName(), senhaUpdateRequest.getSenhaAntiga(), senhaUpdateRequest.getNovaSenha());
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
        } catch (RuntimeException e) { // Captura BadCredentialsException ou outras do AuthService
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Excluir (desativar) conta do paciente logado", description = "Permite que o paciente autenticado desative sua própria conta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conta desativada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    @DeleteMapping("/meu-perfil")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> excluirMinhaConta(Authentication authentication) {
        try {
            String email = authentication.getName();
            Paciente paciente = pacienteService.buscarPorEmail(email);
            pacienteService.desativarPaciente(paciente.getId());
            return ResponseEntity.ok(Map.of("message", "Sua conta foi desativada com sucesso."));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paciente não encontrado."));
        }
    }

    // --- Endpoints de Consulta para Pacientes ---

    @Operation(summary = "Listar minhas consultas (paciente)", description = "Retorna a lista de todas as consultas agendadas para o paciente logado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de consultas retornada", content = @Content(schema = @Schema(implementation = ConsultaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    @GetMapping("/minhas-consultas")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> getMinhasConsultas(Authentication authentication) {
        try {
            String email = authentication.getName();
            Paciente paciente = pacienteService.buscarPorEmail(email);
            List<Consulta> consultas = consultaService.listarConsultasPorPaciente(paciente.getId());
            List<ConsultaResponse> response = consultas.stream().map(ConsultaResponse::fromConsulta).collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paciente não encontrado."));
        }
    }

    @Operation(summary = "Agendar nova consulta (paciente)", description = "Permite que o paciente logado agende uma nova consulta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Consulta agendada com sucesso", content = @Content(schema = @Schema(implementation = ConsultaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, horário indisponível ou conflito de agendamento"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Paciente ou Médico não encontrado")
    })
    @PostMapping("/consultas/agendar")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> agendarConsulta(
            @Parameter(description = "ID do médico para a consulta", required = true) @RequestParam Long medicoId,
            @Parameter(description = "Data e hora da consulta (formato ISO: yyyy-MM-ddTHH:mm:ss)", required = true) @RequestParam String dataHoraConsulta, // Receber como String e parsear
            @Parameter(description = "Observações para a consulta") @RequestParam(required = false) String observacoes,
            Authentication authentication) {
        try {
            String emailPaciente = authentication.getName();
            Paciente paciente = pacienteService.buscarPorEmail(emailPaciente);
            
            java.time.LocalDateTime dataHora;
            try {
                dataHora = java.time.LocalDateTime.parse(dataHoraConsulta);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Formato de dataHoraConsulta inválido. Use yyyy-MM-ddTHH:mm:ss"));
            }

            Consulta novaConsulta = consultaService.agendarConsulta(paciente.getId(), medicoId, dataHora, observacoes);
            return ResponseEntity.status(HttpStatus.CREATED).body(ConsultaResponse.fromConsulta(novaConsulta));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Cancelar minha consulta (paciente)", description = "Permite que o paciente logado cancele uma de suas consultas agendadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta cancelada com sucesso", content = @Content(schema = @Schema(implementation = ConsultaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Não é possível cancelar a consulta (ex: já realizada, status inválido)"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Paciente não tem permissão para cancelar esta consulta"),
            @ApiResponse(responseCode = "404", description = "Consulta não encontrada")
    })
    @PatchMapping("/consultas/{consultaId}/cancelar")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> cancelarMinhaConsulta(
            @Parameter(description = "ID da consulta a ser cancelada", required = true) @PathVariable Long consultaId,
            @Parameter(description = "Motivo do cancelamento", required = true) @RequestParam String motivo,
            Authentication authentication) {
        try {
            String emailPaciente = authentication.getName();
            Paciente paciente = pacienteService.buscarPorEmail(emailPaciente);
            Consulta consulta = consultaService.buscarPorId(consultaId);

            if (!consulta.getPaciente().getId().equals(paciente.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Você não tem permissão para cancelar esta consulta."));
            }

            Consulta consultaCancelada = consultaService.cancelarConsulta(consultaId, motivo, br.com.ifpe.medplus_api.model.acesso.PerfilEnum.ROLE_PACIENTE);
            return ResponseEntity.ok(ConsultaResponse.fromConsulta(consultaCancelada));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    // Endpoints para listar médicos disponíveis por especialidade (útil para o paciente agendar)
    // Este endpoint pode ser público ou requerer autenticação de paciente.
    // Vamos assumir que requer autenticação de paciente para este exemplo.
    @Operation(summary = "Listar médicos por especialidade", description = "Retorna médicos ativos de uma determinada especialidade.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Médicos encontrados"),
        @ApiResponse(responseCode = "401", description = "Não Autorizado")
    })
    @GetMapping("/medicos/especialidade/{especialidade}")
    @PreAuthorize("isAuthenticated()") // Ou hasRole('PACIENTE') se for exclusivo para pacientes
    public ResponseEntity<?> listarMedicosPorEspecialidade(@PathVariable String especialidade) {
        // Usar MedicoService para buscar. Criar um MedicoResponseDTO para não expor dados sensíveis.
        // Exemplo:
        // List<Medico> medicos = medicoService.listarAtivosPorEspecialidade(especialidade);
        // List<MedicoResponseDTO> response = medicos.stream().map(MedicoResponseDTO::fromMedico).collect(Collectors.toList());
        // return ResponseEntity.ok(response);
        return ResponseEntity.ok(Map.of("message", "Endpoint para listar médicos por especialidade: " + especialidade + " - Implementar com MedicoService e DTO de resposta."));
    }


    // O DTO SenhaUpdateRequest precisa ser criado.
}

