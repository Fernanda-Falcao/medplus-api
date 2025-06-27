package br.com.ifpe.medplus_api.dto;

import java.util.Set;

// Usando um record para um DTO imutável e conciso
public record PerfilResponseDTO(
    Long id,
    String nome,
    String email,
    String telefone,
    boolean ativo,
    String cpf,       // Será nulo se for médico
    String crm,       // Será nulo se for paciente/admin
    String especialidade, // Específico do médico
    Set<String> roles // Lista de perfis do usuário
) {
}