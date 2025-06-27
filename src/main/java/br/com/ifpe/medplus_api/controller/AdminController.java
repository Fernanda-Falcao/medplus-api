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
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Collections;
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
@PreAuthorize("hasRole('ADMIN')")
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

    // --- DTOs de Resposta (corrigidos para serem mais robustos) ---

    private record AdminResponse(Long id, String nome, String email, String cpf, Integer nivelAcesso, boolean ativo, List<String> roles) {
        static AdminResponse fromAdmin(Admin admin) {
            if (admin == null) return null;
            List<String> roles = Collections.emptyList();
            // CORREÇÃO: Acessar .getPerfis() diretamente do objeto admin.
            if (admin.getPerfis() != null) {
                roles = admin.getPerfis().stream()
                        .map(perfil -> perfil.getAuthority().replace("ROLE_", ""))
                        .collect(Collectors.toList());
            }
            return new AdminResponse(
                admin.getId(), admin.getNome(), admin.getEmail(), admin.getCpf(),
                admin.getNivelAcesso(), admin.isAtivo(), roles
            );
        }
    }

    private record PacienteAdminResponse(Long id, String nome, String email, String cpf, boolean ativo, List<String> roles) {
        static PacienteAdminResponse fromPaciente(Paciente paciente) {
            if (paciente == null) return null;
            List<String> roles = Collections.emptyList();
            // CORREÇÃO: Acessar .getPerfis() diretamente do objeto paciente.
            if (paciente.getPerfis() != null) {
                roles = paciente.getPerfis().stream()
                        .map(perfil -> perfil.getAuthority().replace("ROLE_", ""))
                        .collect(Collectors.toList());
            }
            return new PacienteAdminResponse(
                paciente.getId(), paciente.getNome(), paciente.getEmail(), paciente.getCpf(),
                paciente.isAtivo(), roles
            );
        }
    }

    private record MedicoAdminResponse(Long id, String nome, String email, String crm, String especialidade, boolean ativo, List<String> roles) {
        static MedicoAdminResponse fromMedico(Medico medico) {
            if (medico == null) return null;
            List<String> roles = Collections.emptyList();
            // CORREÇÃO: Acessar .getPerfis() diretamente do objeto medico.
            if (medico.getPerfis() != null) {
                roles = medico.getPerfis().stream()
                        .map(perfil -> perfil.getAuthority().replace("ROLE_", ""))
                        .collect(Collectors.toList());
            }
            return new MedicoAdminResponse(
                medico.getId(), medico.getNome(), medico.getEmail(), medico.getCrm(),
                medico.getEspecialidade(), medico.isAtivo(), roles
            );
        }
    }

    // --- DTO de Resposta para Consultas ---
    private record ConsultaAdminResponse(Long id, String pacienteNome, String medicoNome, String dataHora, String status) {
        static ConsultaAdminResponse fromConsulta(Consulta consulta) {
            if (consulta == null) return null; // Proteção contra entidade nula
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
    @GetMapping("/meu-perfil")
    public ResponseEntity<AdminResponse> getMeuPerfilAdmin(Authentication authentication) {
        Admin admin = adminService.buscarPorEmail(authentication.getName());
        return ResponseEntity.ok(AdminResponse.fromAdmin(admin));
    }

    @PutMapping("/meu-perfil")
    public ResponseEntity<AdminResponse> atualizarMeuPerfilAdmin(@Valid @RequestBody AdminRequest adminRequest, Authentication authentication) {
        Admin adminLogado = adminService.buscarPorEmail(authentication.getName());
        Admin adminAtualizado = adminService.atualizarAdmin(adminLogado.getId(), adminRequest);
        return ResponseEntity.ok(AdminResponse.fromAdmin(adminAtualizado));
    }

    @PostMapping("/meu-perfil/mudar-senha")
    public ResponseEntity<Map<String, String>> mudarMinhaSenhaAdmin(@Valid @RequestBody SenhaUpdateRequest senhaUpdateRequest, Authentication authentication) {
        authService.mudarSenha(authentication.getName(), senhaUpdateRequest.getSenhaAntiga(), senhaUpdateRequest.getNovaSenha());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }

    // --- Gerenciamento de Administradores ---
    @GetMapping("/usuarios/admins")
    public ResponseEntity<List<AdminResponse>> listarAdmins() {
        List<AdminResponse> admins = adminService.listarTodos().stream()
                .map(AdminResponse::fromAdmin)
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }
    
    // ... outros endpoints de admin ...

    // --- Gerenciamento de Pacientes pelo Admin ---
    @GetMapping("/usuarios/pacientes")
    public ResponseEntity<List<PacienteAdminResponse>> listarPacientes() {
        List<PacienteAdminResponse> pacientes = pacienteService.listarTodos().stream()
                .map(PacienteAdminResponse::fromPaciente)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pacientes);
    }
    
    @GetMapping("/usuarios/pacientes/{pacienteId}")
    public ResponseEntity<PacienteAdminResponse> buscarPacientePorId(@PathVariable Long pacienteId) {
        Paciente paciente = pacienteService.buscarPorId(pacienteId);
        return ResponseEntity.ok(PacienteAdminResponse.fromPaciente(paciente));
    }
    
    @PostMapping("/usuarios/pacientes")
    public ResponseEntity<PacienteAdminResponse> criarPaciente(@Valid @RequestBody PacienteRequest pacienteRequest) {
        Paciente novoPaciente = pacienteService.registrarPaciente(pacienteRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteAdminResponse.fromPaciente(novoPaciente));
    }
    
    @PutMapping("/usuarios/pacientes/{pacienteId}")
    public ResponseEntity<PacienteAdminResponse> atualizarPaciente(@PathVariable Long pacienteId, @Valid @RequestBody PacienteRequest pacienteRequest) {
        Paciente pacienteAtualizado = pacienteService.atualizarPaciente(pacienteId, pacienteRequest);
        return ResponseEntity.ok(PacienteAdminResponse.fromPaciente(pacienteAtualizado));
    }

    @PatchMapping("/usuarios/pacientes/{pacienteId}/desativar")
    public ResponseEntity<Map<String, String>> desativarPaciente(@PathVariable Long pacienteId) {
        pacienteService.desativarPaciente(pacienteId);
        return ResponseEntity.ok(Map.of("message", "Paciente ID " + pacienteId + " desativado."));
    }

    @PatchMapping("/usuarios/pacientes/{pacienteId}/ativar")
    public ResponseEntity<Map<String, String>> ativarPaciente(@PathVariable Long pacienteId) {
        // CORREÇÃO: A lógica de ativação deve estar no service para garantir a persistência.
        // Vamos supor que o PacienteService agora tem um método ativarPaciente.
        pacienteService.ativarPaciente(pacienteId); // O service deve buscar, setar ativo=true e salvar.
        return ResponseEntity.ok(Map.of("message", "Paciente ID " + pacienteId + " ativado."));
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


    // --- Gerenciamento de Médicos pelo Admin ---
    /* 
    @Operation(summary = "Listar todos os médicos")
    @GetMapping("/usuarios/medicos")
    public ResponseEntity<List<MedicoAdminResponse>> listarMedicos() {
        List<MedicoAdminResponse> medicos = medicoService.listarTodos().stream()
                .map(MedicoAdminResponse::fromMedico)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicos);
    }
    */


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


// Fim do Controller AdminController


