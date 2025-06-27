
package br.com.ifpe.medplus_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO para solicitar criação ou atualização de disponibilidade médica.
 */
@Getter
@Setter
public class DisponibilidadeRequest {

    @NotBlank(message = "O dia da semana é obrigatório.")
    private String diaSemana;

    @NotBlank(message = "A hora de início é obrigatória.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Hora de início deve estar no formato HH:mm.")
    private String horaInicio;

    @NotBlank(message = "A hora de fim é obrigatória.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Hora de fim deve estar no formato HH:mm.")
    private String horaFim;

    // Campo opcional, mas útil para atualizações
    private boolean ativo = true;
}
