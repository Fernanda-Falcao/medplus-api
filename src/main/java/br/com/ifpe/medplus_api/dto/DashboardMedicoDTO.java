
package br.com.ifpe.medplus_api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class DashboardMedicoDTO {
    private int consultasHoje;
    private int pacientesDoDia;
    private List<ConsultaResumoDTO> proximasConsultas;

    public int getConsultasHoje() {
        return consultasHoje;
    }

    public void setConsultasHoje(int consultasHoje) {
        this.consultasHoje = consultasHoje;
    }

    public int getPacientesDoDia() {
        return pacientesDoDia;
    }

    public void setPacientesDoDia(int pacientesDoDia) {
        this.pacientesDoDia = pacientesDoDia;
    }

    public List<ConsultaResumoDTO> getProximasConsultas() {
        return proximasConsultas;
    }

    public void setProximasConsultas(List<ConsultaResumoDTO> proximasConsultas) {
        this.proximasConsultas = proximasConsultas;
    }
}
