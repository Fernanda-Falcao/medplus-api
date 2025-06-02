package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.model.acesso.Usuario;
import br.com.ifpe.medplus_api.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para carregar detalhes do usuário para o Spring Security.
 * Implementa a interface UserDetailsService.
 */
@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carrega um usuário pelo seu nome de usuário (que neste caso é o email).
     * Este método é chamado pelo Spring Security durante o processo de autenticação.
     *
     * @param username O email do usuário.
     * @return Um objeto UserDetails contendo as informações do usuário.
     * @throws UsernameNotFoundException Se o usuário não for encontrado.
     */
    @Override
    @Transactional(readOnly = true) // Transação apenas para leitura
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Busca o usuário pelo email, garantindo que os perfis (roles) sejam carregados.
        // A entidade Usuario já deve ter FetchType.EAGER para perfis ou usar um método
        // do repositório que faça o JOIN FETCH.
        Usuario usuario = usuarioRepository.findByEmailWithPerfis(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));

        if (!usuario.isAtivo()) {
            throw new UsernameNotFoundException("Usuário com email " + username + " está desativado.");
        }
        
        // A entidade Usuario já implementa UserDetails, então podemos retorná-la diretamente.
        // Se não implementasse, precisaríamos criar um User do Spring Security:
        // return new org.springframework.security.core.userdetails.User(
        //         usuario.getEmail(),
        //         usuario.getSenha(),
        //         usuario.getAuthorities()
        // );
        return usuario;
    }

    /**
     * Busca um usuário pelo ID.
     * @param id O ID do usuário.
     * @return O usuário encontrado.
     * @throws UsernameNotFoundException Se o usuário não for encontrado.
     */
    @Transactional(readOnly = true)
    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o ID: " + id));
    }
}


