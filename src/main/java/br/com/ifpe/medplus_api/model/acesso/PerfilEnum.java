
package br.com.ifpe.medplus_api.model.acesso;

/**
 * Enumeração para os tipos de perfil de usuário no sistema.
 * Define as roles (cargos) que um usuário pode ter no sistema.
 */
public enum PerfilEnum {

    ROLE_PACIENTE("PACIENTE"),
    ROLE_MEDICO("MEDICO"),
    ROLE_ADMIN("ADMIN");

    private final String nome; // Nome amigável do perfil, sem o prefixo ROLE_

    PerfilEnum(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /**
     * Retorna o nome do perfil no formato esperado pelo Spring Security (ex: "ROLE_ADMIN").
     * @return O nome da role (com prefixo "ROLE_").
     */
    public String getRoleName() {
        return this.name(); // Exemplo: "ROLE_ADMIN"
    }

    /**
     * Converte uma string para o enum PerfilEnum.
     * Útil para quando o nome do perfil vem de uma fonte externa.
     *
     * @param nome O nome do perfil (ex: "ADMIN", "PACIENTE", "MEDICO").
     * @return O PerfilEnum correspondente.
     * @throws IllegalArgumentException se o nome do perfil for inválido.
     */
    public static PerfilEnum fromString(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do perfil não pode ser nulo ou vazio.");
        }

        for (PerfilEnum perfil : PerfilEnum.values()) {
            // Compara ignorando o prefixo "ROLE_" e sem considerar letras maiúsculas/minúsculas
            if (perfil.name().equalsIgnoreCase("ROLE_" + nome) || perfil.getNome().equalsIgnoreCase(nome)) {
                return perfil;
            }
        }

        throw new IllegalArgumentException("Perfil inválido: " + nome);
    }
}
