package br.com.ifpe.medplus_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO para requisições de criação/atualização de DisponibilidadeMedico.
 */
@Data
public class DisponibilidadeRequest {

    @NotBlank(message = "Dia da semana é obrigatório.")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$",
             message = "Dia da semana deve ser um valor válido (ex: MONDAY, TUESDAY, etc.)")
    private String diaSemana; // Ex: "MONDAY", "TUESDAY"

    @NotBlank(message = "Hora de início é obrigatória.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Hora de início deve estar no formato HH:mm")
    private String horaInicio; // Ex: "09:00"

    @NotBlank(message = "Hora de fim é obrigatória.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Hora de fim deve estar no formato HH:mm")
    private String horaFim;    // Ex: "17:30"
    
    @NotNull(message = "O status 'ativo' é obrigatório.")
    private boolean ativo = true; // Default true, mas pode ser especificado no request
}


