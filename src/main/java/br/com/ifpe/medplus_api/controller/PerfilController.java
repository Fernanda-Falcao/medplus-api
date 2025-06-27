package br.com.ifpe.medplus_api.controller;

import br.com.ifpe.medplus_api.service.UsuarioService; // Importe seu serviço de usuário
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("") // A base da URL
@Tag(name = "Perfil do Usuário", description = "Endpoints para gerenciamento do próprio perfil do usuário")
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService; // Assumindo que você tem um serviço para buscar usuários

    /**
     * Endpoint para retornar os dados do perfil do usuário autenticado.
     * Este endpoint é protegido e requer um token JWT válido.
     *
     * @return ResponseEntity com os dados do perfil do usuário.
     */
    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()") // Garante que apenas usuários logados (qualquer perfil) podem acessar
    @Operation(summary = "Obter dados do perfil", description = "Retorna as informações detalhadas do usuário atualmente logado.")
    @SecurityRequirement(name = "bearerAuth") // Informa ao Swagger que este endpoint é protegido
    public ResponseEntity<?> getMeuPerfil() {
        try {
            // Pega o contexto de segurança do usuário que fez a requisição
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName(); // O "name" é o email/username usado no login

            // Usa o email para buscar o objeto completo do usuário no banco de dados.
            // É importante que este método retorne um DTO (Data Transfer Object)
            // com todos os campos necessários para a página de perfil.
            Object perfilDto = usuarioService.buscarPorEmailComoDto(userEmail);

            if (perfilDto == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(perfilDto);

        } catch (Exception e) {
            // LINHA CRUCIAL PARA DEBUG: Imprime o erro completo no console
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Ocorreu um erro ao processar sua solicitação de perfil.");
        }
    }
    
    // Futuramente, você pode adicionar o endpoint de ATUALIZAÇÃO do perfil aqui também:
    //@PutMapping("/perfil")
    // public ResponseEntity<?> updateMeuPerfil(@RequestBody PerfilUpdateRequestDTO dto) { ... }
}