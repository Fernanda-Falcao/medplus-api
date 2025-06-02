package br.com.ifpe.medplus_api.model.consulta;

/**
 * Enumeração para representar os possíveis status de uma consulta.
 */
public enum StatusConsulta {
    AGENDADA("Agendada"),          // A consulta foi marcada.
    CONFIRMADA("Confirmada"),      // O paciente/médico confirmou a presença.
    REALIZADA("Realizada"),        // A consulta ocorreu.
    CANCELADA_PACIENTE("Cancelada pelo Paciente"), // A consulta foi cancelada pelo paciente.
    CANCELADA_MEDICO("Cancelada pelo Médico"),   // A consulta foi cancelada pelo médico.
    CANCELADA_ADMIN("Cancelada pelo Administrador"), // A consulta foi cancelada pelo administrador.
    REAGENDADA("Reagendada"),      // A consulta foi remarcada.
    NAO_COMPARECEU("Não Compareceu"); // O paciente não compareceu à consulta.

    private final String descricao;

    StatusConsulta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
