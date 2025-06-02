package br.com.ifpe.medplus_api.controller;

import br.com.ifpe.medplus_api.dto.AuthRequest;
import br.com.ifpe.medplus_api.dto.PacienteRequest; // Para registro de paciente
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.service.AuthService;
import br.com.ifpe.medplus_api.service.PacienteService; // Para registrar paciente
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador para endpoints de autenticação e registro.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints para login e registro de usuários")
public class AuthController {

    private final AuthService authService;
    private final PacienteService pacienteService; // Injetado para o endpoint de registro de paciente

    
    public AuthController(AuthService authService, PacienteService pacienteService) {
        this.authService = authService;
        this.pacienteService = pacienteService;
    }

    /**
     * Autentica um usuário e retorna um token JWT.
     *
     * @param authRequest DTO contendo email e senha.
     * @return ResponseEntity com o token JWT em caso de sucesso, ou erro em caso de falha.
     */
    @Operation(summary = "Autenticar usuário", description = "Realiza o login do usuário e retorna um token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido, token JWT retornado"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            String token = authService.login(authRequest);
            // Retorna o token em um objeto JSON para melhor manuseio no frontend
            return ResponseEntity.ok(Map.of("token", token));
        } catch (AuthenticationException e) {
            // A exceção BadCredentialsException (que herda de AuthenticationException)
            // será tratada pelo TratadorErros global, retornando 401.
            // Se quiser uma mensagem customizada aqui, pode tratar especificamente.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciais inválidas."));
        }
    }

    /**
     * Registra um novo paciente.
     * Este endpoint é público, conforme definido em SecurityConfiguration.
     *
     * @param pacienteRequest DTO com os dados do paciente.
     * @return ResponseEntity com o paciente criado ou mensagem de erro.
     */
    @Operation(summary = "Registrar novo paciente", description = "Cria uma nova conta de paciente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Paciente registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos ou erro de validação"),
            @ApiResponse(responseCode = "409", description = "Email ou CPF já cadastrado")
    })
    @PostMapping("/registrar/paciente") // Endpoint público conforme SecurityConfig
    public ResponseEntity<?> registrarPaciente(@Valid @RequestBody PacienteRequest pacienteRequest) {
        try {
            Paciente novoPaciente = pacienteService.registrarPaciente(pacienteRequest);
            // Não retornar a senha no response
            // Poderia criar um PacienteResponseDTO se necessário
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", novoPaciente.getId(),
                "nome", novoPaciente.getNome(),
                "email", novoPaciente.getEmail(),
                "message", "Paciente registrado com sucesso!"
            ));
        } catch (Exception e) {
            // Exceções como EntityExistsException serão tratadas pelo TratadorErros.
            // Aqui é um fallback ou para log específico se necessário.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // Outros endpoints de autenticação podem ser adicionados aqui:
    // - /auth/refresh-token (se implementar refresh tokens)
    // - /auth/esqueci-senha
    // - /auth/redefinir-senha
}

