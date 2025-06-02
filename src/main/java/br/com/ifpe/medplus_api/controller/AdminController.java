package br.com.ifpe.medplus_api.controller;

import br.com.ifpe.medplus_api.dto.AdminRequest;
import br.com.ifpe.medplus_api.dto.MedicoRequest;
import br.com.ifpe.medplus_api.dto.PacienteRequest;
import br.com.ifpe.medplus_api.dto.SenhaUpdateRequest;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.admin.Admin;
import br.com.ifpe.medplus_api.model.consulta.Consulta;
import br.com.ifpe.medplus_api.model.consulta.StatusConsulta;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.service.*;
import br.com.ifpe.medplus_api.util.exception.EntidadeNaoEncontradaException;
import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.Parameter;
// import io.swagger.v3.oas.annotations.media.Content;
// import io.swagger.v3.oas.annotations.media.Schema;
// import io.swagger.v3.oas.annotations.responses.ApiResponse;
// import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para gerenciar endpoints administrativos.
 * Acesso restrito a usuários com perfil ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Administração", description = "Endpoints para gerenciamento do sistema por administradores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')") // Protege todos os endpoints deste controller
public class AdminController {

    private final AdminService adminService;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;
    private final ConsultaService consultaService;
    private final AuthService authService;


    public AdminController(AdminService adminService,
                           PacienteService pacienteService,
                           MedicoService medicoService,
                           ConsultaService consultaService,
                           AuthService authService) {
        this.adminService = adminService;
        this.pacienteService = pacienteService;
        this.medicoService = medicoService;
        this.consultaService = consultaService;
        this.authService = authService;
    }

    // DTOs de Resposta (simplificados para o contexto do admin)
    private record AdminResponse(Long id, String nome, String email, String cpf, Integer nivelAcesso, boolean ativo) {
        static AdminResponse fromAdmin(Admin admin) {
            return new AdminResponse(admin.getId(), admin.getNome(), admin.getEmail(), admin.getCpf(), admin.getNivelAcesso(), admin.isAtivo());
        }
    }

    private record PacienteAdminResponse(Long id, String nome, String email, String cpf, boolean ativo) {
        static PacienteAdminResponse fromPaciente(Paciente paciente) {
            return new PacienteAdminResponse(paciente.getId(), paciente.getNome(), paciente.getEmail(), paciente.getCpf(), paciente.isAtivo());
        }
    }

    private record MedicoAdminResponse(Long id, String nome, String email, String crm, String especialidade, boolean ativo) {
        static MedicoAdminResponse fromMedico(Medico medico) {
            return new MedicoAdminResponse(medico.getId(), medico.getNome(), medico.getEmail(), medico.getCrm(), medico.getEspecialidade(), medico.isAtivo());
        }
    }
    
    private record ConsultaAdminResponse(Long id, String pacienteNome, String medicoNome, String dataHora, String status) {
        static ConsultaAdminResponse fromConsulta(Consulta consulta) {
            return new ConsultaAdminResponse(
                consulta.getId(),
                consulta.getPaciente() != null ? consulta.getPaciente().getNome() : "N/A",
                consulta.getMedico() != null ? consulta.getMedico().getNome() : "N/A",
                consulta.getDataHoraConsulta() != null ? consulta.getDataHoraConsulta().toString() : "N/A",
                consulta.getStatus() != null ? consulta.getStatus().getDescricao() : "N/A"
            );
        }
    }


    // --- Gerenciamento do Próprio Perfil Admin ---
    @Operation(summary = "Obter perfil do administrador logado")
    @GetMapping("/meu-perfil")
    public ResponseEntity<?> getMeuPerfilAdmin(Authentication authentication) {
        try {
            Admin admin = adminService.buscarPorEmail(authentication.getName());
            return ResponseEntity.ok(AdminResponse.fromAdmin(admin));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Administrador não encontrado."));
        }
    }

    @Operation(summary = "Atualizar perfil do administrador logado")
    @PutMapping("/meu-perfil")
    public ResponseEntity<?> atualizarMeuPerfilAdmin(@Valid @RequestBody AdminRequest adminRequest, Authentication authentication) {
        try {
            Admin adminLogado = adminService.buscarPorEmail(authentication.getName());
            Admin adminAtualizado = adminService.atualizarAdmin(adminLogado.getId(), adminRequest);
            return ResponseEntity.ok(AdminResponse.fromAdmin(adminAtualizado));
        } catch (UsernameNotFoundException | EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Administrador não encontrado."));
        }
    }

    @Operation(summary = "Mudar senha do administrador logado")
    @PostMapping("/meu-perfil/mudar-senha")
    public ResponseEntity<?> mudarMinhaSenhaAdmin(@Valid @RequestBody SenhaUpdateRequest senhaUpdateRequest, Authentication authentication) {
        try {
            authService.mudarSenha(authentication.getName(), senhaUpdateRequest.getSenhaAntiga(), senhaUpdateRequest.getNovaSenha());
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // --- Gerenciamento de Administradores (por outros Admins com permissão adequada) ---
    @Operation(summary = "Listar todos os administradores")
    @GetMapping("/usuarios/admins")
    public ResponseEntity<List<AdminResponse>> listarAdmins() {
        List<AdminResponse> admins = adminService.listarTodos().stream()
                .map(AdminResponse::fromAdmin)
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }

    @Operation(summary = "Registrar novo administrador")
    @PostMapping("/usuarios/admins")
    public ResponseEntity<?> registrarAdmin(@Valid @RequestBody AdminRequest adminRequest) {
        // Adicionar verificação se o admin logado tem permissão para criar outros admins (ex: por nivelAcesso)
        Admin novoAdmin = adminService.registrarAdmin(adminRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.fromAdmin(novoAdmin));
    }

    @Operation(summary = "Desativar conta de administrador")
    @PatchMapping("/usuarios/admins/{adminId}/desativar")
    public ResponseEntity<?> desativarAdmin(@PathVariable Long adminId, Authentication authentication) {
        Admin adminLogado = adminService.buscarPorEmail(authentication.getName());
        if (adminLogado.getId().equals(adminId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Você não pode desativar sua própria conta por este endpoint."));
        }
        adminService.desativarAdmin(adminId);
        return ResponseEntity.ok(Map.of("message", "Administrador ID " + adminId + " desativado."));
    }

    @Operation(summary = "Ativar conta de administrador")
    @PatchMapping("/usuarios/admins/{adminId}/ativar")
    public ResponseEntity<?> ativarAdmin(@PathVariable Long adminId) {
        adminService.ativarAdmin(adminId);
        return ResponseEntity.ok(Map.of("message", "Administrador ID " + adminId + " ativado."));
    }


    // --- Gerenciamento de Pacientes pelo Admin ---
    @Operation(summary = "Listar todos os pacientes")
    @GetMapping("/usuarios/pacientes")
    public ResponseEntity<List<PacienteAdminResponse>> listarPacientes() {
        List<PacienteAdminResponse> pacientes = pacienteService.listarTodos().stream()
                .map(PacienteAdminResponse::fromPaciente)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pacientes);
    }

    @Operation(summary = "Buscar paciente por ID")
    @GetMapping("/usuarios/pacientes/{pacienteId}")
    public ResponseEntity<?> buscarPacientePorId(@PathVariable Long pacienteId) {
        Paciente paciente = pacienteService.buscarPorId(pacienteId);
        return ResponseEntity.ok(PacienteAdminResponse.fromPaciente(paciente));
    }
    
    @Operation(summary = "Criar novo paciente (admin)")
    @PostMapping("/usuarios/pacientes")
    public ResponseEntity<?> criarPaciente(@Valid @RequestBody PacienteRequest pacienteRequest) {
        Paciente novoPaciente = pacienteService.registrarPaciente(pacienteRequest); // Reutiliza o método de registro
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteAdminResponse.fromPaciente(novoPaciente));
    }

    @Operation(summary = "Atualizar dados de um paciente (admin)")
    @PutMapping("/usuarios/pacientes/{pacienteId}")
    public ResponseEntity<?> atualizarPaciente(@PathVariable Long pacienteId, @Valid @RequestBody PacienteRequest pacienteRequest) {
        Paciente pacienteAtualizado = pacienteService.atualizarPaciente(pacienteId, pacienteRequest);
        return ResponseEntity.ok(PacienteAdminResponse.fromPaciente(pacienteAtualizado));
    }

    @Operation(summary = "Desativar conta de paciente")
    @PatchMapping("/usuarios/pacientes/{pacienteId}/desativar")
    public ResponseEntity<?> desativarPaciente(@PathVariable Long pacienteId) {
        pacienteService.desativarPaciente(pacienteId);
        return ResponseEntity.ok(Map.of("message", "Paciente ID " + pacienteId + " desativado."));
    }

    @Operation(summary = "Ativar conta de paciente")
    @PatchMapping("/usuarios/pacientes/{pacienteId}/ativar")
    public ResponseEntity<?> ativarPaciente(@PathVariable Long pacienteId) {
        Paciente paciente = pacienteService.buscarPorId(pacienteId);
        paciente.setAtivo(true);
        // pacienteRepository.save(paciente) // O PacienteService deveria ter um método ativarPaciente
        // Por simplicidade, vamos assumir que o PacienteService terá um método para isso.
        // Se não, o código seria:
        // Paciente paciente = pacienteService.buscarPorId(pacienteId);
        // paciente.setAtivo(true);
        // pacienteService.salvar(paciente); // método genérico de save no service
        return ResponseEntity.ok(Map.of("message", "Paciente ID " + pacienteId + " ativado. (Implementar método 'ativar' no PacienteService)"));
    }


    // --- Gerenciamento de Médicos pelo Admin ---
    @Operation(summary = "Listar todos os médicos")
    @GetMapping("/usuarios/medicos")
    public ResponseEntity<List<MedicoAdminResponse>> listarMedicos() {
        List<MedicoAdminResponse> medicos = medicoService.listarTodos().stream()
                .map(MedicoAdminResponse::fromMedico)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicos);
    }

    @Operation(summary = "Buscar médico por ID")
    @GetMapping("/usuarios/medicos/{medicoId}")
    public ResponseEntity<?> buscarMedicoPorId(@PathVariable Long medicoId) {
        Medico medico = medicoService.buscarPorId(medicoId);
        return ResponseEntity.ok(MedicoAdminResponse.fromMedico(medico));
    }

    @Operation(summary = "Registrar novo médico (admin)")
    @PostMapping("/usuarios/medicos")
    public ResponseEntity<?> registrarMedico(@Valid @RequestBody MedicoRequest medicoRequest) {
        Medico novoMedico = medicoService.registrarMedico(medicoRequest); // Reutiliza o método de registro
        return ResponseEntity.status(HttpStatus.CREATED).body(MedicoAdminResponse.fromMedico(novoMedico));
    }

    @Operation(summary = "Atualizar dados de um médico (admin)")
    @PutMapping("/usuarios/medicos/{medicoId}")
    public ResponseEntity<?> atualizarMedico(@PathVariable Long medicoId, @Valid @RequestBody MedicoRequest medicoRequest) {
        Medico medicoAtualizado = medicoService.atualizarMedico(medicoId, medicoRequest);
        return ResponseEntity.ok(MedicoAdminResponse.fromMedico(medicoAtualizado));
    }

    @Operation(summary = "Desativar conta de médico")
    @PatchMapping("/usuarios/medicos/{medicoId}/desativar")
    public ResponseEntity<?> desativarMedico(@PathVariable Long medicoId) {
        medicoService.desativarMedico(medicoId);
        return ResponseEntity.ok(Map.of("message", "Médico ID " + medicoId + " desativado."));
    }

    @Operation(summary = "Ativar conta de médico")
    @PatchMapping("/usuarios/medicos/{medicoId}/ativar")
    public ResponseEntity<?> ativarMedico(@PathVariable Long medicoId) {
        medicoService.ativarMedico(medicoId); // Supondo que este método existe no MedicoService
        return ResponseEntity.ok(Map.of("message", "Médico ID " + medicoId + " ativado."));
    }

    // --- Gerenciamento de Consultas pelo Admin ---
    @Operation(summary = "Listar todas as consultas do sistema")
    @GetMapping("/consultas")
    public ResponseEntity<List<ConsultaAdminResponse>> listarTodasConsultas() {
        List<ConsultaAdminResponse> consultas = consultaService.listarTodasConsultas().stream()
                .map(ConsultaAdminResponse::fromConsulta)
                .collect(Collectors.toList());
        return ResponseEntity.ok(consultas);
    }

    @Operation(summary = "Buscar consulta por ID")
    @GetMapping("/consultas/{consultaId}")
    public ResponseEntity<?> buscarConsultaPorId(@PathVariable Long consultaId) {
        Consulta consulta = consultaService.buscarPorId(consultaId);
        return ResponseEntity.ok(ConsultaAdminResponse.fromConsulta(consulta));
    }

    @Operation(summary = "Agendar nova consulta (admin)", description = "Permite ao admin agendar uma consulta para qualquer paciente com qualquer médico.")
    @PostMapping("/consultas/agendar")
    public ResponseEntity<?> agendarConsultaAdmin(
            @RequestParam Long pacienteId,
            @RequestParam Long medicoId,
            @RequestParam String dataHoraConsulta, // Formato YYYY-MM-DDTHH:MM:SS
            @RequestParam(required = false) String observacoes) {
        try {
            LocalDateTime dataHora = LocalDateTime.parse(dataHoraConsulta);
            Consulta novaConsulta = consultaService.agendarConsulta(pacienteId, medicoId, dataHora, observacoes);
            return ResponseEntity.status(HttpStatus.CREATED).body(ConsultaAdminResponse.fromConsulta(novaConsulta));
        } catch (DateTimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Formato de dataHoraConsulta inválido. Use YYYY-MM-DDTHH:MM:SS"));
        } catch (IllegalArgumentException | EntidadeNaoEncontradaException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Cancelar consulta (admin)")
    @PatchMapping("/consultas/{consultaId}/cancelar")
    public ResponseEntity<?> cancelarConsultaAdmin(@PathVariable Long consultaId, @RequestParam String motivo) {
        try {
            Consulta consultaCancelada = consultaService.cancelarConsulta(consultaId, motivo, PerfilEnum.ROLE_ADMIN);
            return ResponseEntity.ok(ConsultaAdminResponse.fromConsulta(consultaCancelada));
        } catch (IllegalArgumentException | EntidadeNaoEncontradaException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Reagendar consulta (admin)")
    @PatchMapping("/consultas/{consultaId}/reagendar")
    public ResponseEntity<?> reagendarConsultaAdmin(
            @PathVariable Long consultaId,
            @RequestParam String novaDataHora, // Formato YYYY-MM-DDTHH:MM:SS
            @RequestParam(required = false) String observacoes) {
        try {
            LocalDateTime dataHora = LocalDateTime.parse(novaDataHora);
            Consulta consultaReagendada = consultaService.reagendarConsulta(consultaId, dataHora, observacoes);
            return ResponseEntity.ok(ConsultaAdminResponse.fromConsulta(consultaReagendada));
        } catch (DateTimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Formato de novaDataHora inválido. Use YYYY-MM-DDTHH:MM:SS"));
        } catch (IllegalArgumentException | EntidadeNaoEncontradaException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(summary = "Atualizar status de uma consulta (admin)")
    @PatchMapping("/consultas/{consultaId}/status")
    public ResponseEntity<?> atualizarStatusConsultaAdmin(
            @PathVariable Long consultaId,
            @RequestParam String novoStatus) {
        try {
            StatusConsulta status = StatusConsulta.valueOf(novoStatus.toUpperCase());
            Consulta consultaAtualizada = consultaService.atualizarStatusConsulta(consultaId, status);
            return ResponseEntity.ok(ConsultaAdminResponse.fromConsulta(consultaAtualizada));
        } catch (IllegalArgumentException e) { // Captura erro do valueOf do Enum também
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Status inválido: " + novoStatus + ". Erro: " + e.getMessage()));
        } catch (EntidadeNaoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}

