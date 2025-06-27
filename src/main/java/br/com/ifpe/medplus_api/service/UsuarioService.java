package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.dto.PerfilResponseDTO;
import br.com.ifpe.medplus_api.model.acesso.Usuario;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.Set;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailWithPerfis(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));

        if (!usuario.isAtivo()) {
            throw new UsernameNotFoundException("Usuário com email " + username + " está desativado.");
        }
        
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o ID: " + id));
    }

    /**
     * Busca um usuário pelo email e o converte para um DTO de perfil.
     *
     * @param userEmail O email do usuário a ser buscado.
     * @return um PerfilResponseDTO com os dados do usuário.
     */
    @Transactional(readOnly = true)
    public PerfilResponseDTO buscarPorEmailComoDto(String userEmail) {
        // Usa o mesmo método do repositório para garantir que os perfis sejam carregados
        Usuario usuario = usuarioRepository.findByEmailWithPerfis(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + userEmail));
        
        // Converte a entidade Usuario para o nosso DTO de resposta
        return toPerfilResponseDTO(usuario);
    }

    /**
     * Método auxiliar privado para converter a entidade Usuario para PerfilResponseDTO.
     */
    private PerfilResponseDTO toPerfilResponseDTO(Usuario usuario) {
        // Extrai os nomes dos perfis (roles)
        Set<String> roles = usuario.getPerfis().stream()
                                   .map(perfil -> perfil.getNome().toString()) // ex: "ROLE_ADMIN"
                                   .collect(Collectors.toSet());

        String cpf = null;
        String crm = null;
        String especialidade = null;

        // Verifica o tipo de usuário para preencher os campos específicos
        if (usuario instanceof Paciente) {
            cpf = ((Paciente) usuario).getCpf();
        } else if (usuario instanceof Medico) {
            crm = ((Medico) usuario).getCrm();
            especialidade = ((Medico) usuario).getEspecialidade();
        }
        // Para Admin, os campos específicos podem ser nulos ou preenchidos se existirem

        return new PerfilResponseDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getTelefone(),
            usuario.isAtivo(),
            cpf,
            crm,
            especialidade,
            roles
        );
    }
}


