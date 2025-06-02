package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.dto.AuthRequest;
import br.com.ifpe.medplus_api.model.acesso.Perfil;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.acesso.Usuario;
import br.com.ifpe.medplus_api.repository.PerfilRepository; // Será criado
import br.com.ifpe.medplus_api.repository.UsuarioRepository;
import br.com.ifpe.medplus_api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Serviço responsável pela autenticação e registro de usuários.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository; // Será criado
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager; // Para autenticar usuários

    
    public AuthService(UsuarioRepository usuarioRepository,
                       PerfilRepository perfilRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Autentica um usuário e gera um token JWT.
     *
     * @param authRequest Contém email e senha para autenticação.
     * @return O token JWT se a autenticação for bem-sucedida.
     * @throws org.springframework.security.core.AuthenticationException Se as credenciais forem inválidas.
     */
    @Transactional(readOnly = true)
    public String login(AuthRequest authRequest) {
        // O AuthenticationManager verifica as credenciais usando o UserDetailsService (UsuarioService)
        // e o PasswordEncoder configurados.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getSenha())
        );

        // Se a autenticação for bem-sucedida, o Spring Security armazena o principal autenticado.
        // Podemos obter os UserDetails a partir daí ou recarregar.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return jwtService.generateToken(userDetails);
    }


    /**
     * Método auxiliar para registrar um novo usuário genérico.
     * Este método é um exemplo e pode precisar ser adaptado ou movido para serviços específicos
     * (PacienteService, MedicoService, AdminService) dependendo da lógica de criação de cada tipo de usuário.
     *
     * @param usuario O objeto Usuario a ser registrado.
     * @param perfilEnum O perfil inicial do usuário.
     * @return O usuário salvo.
     * @throws RuntimeException se o email ou CPF já existirem, ou se o perfil não for encontrado.
     */
    @Transactional
    public Usuario registrarUsuarioGenerico(Usuario usuario, PerfilEnum perfilEnum) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Erro: Email já está em uso!");
        }
        if (usuarioRepository.existsByCpf(usuario.getCpf())) {
            throw new RuntimeException("Erro: CPF já está em uso!");
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setAtivo(true); // Define o usuário como ativo por padrão

        Set<Perfil> perfis = new HashSet<>();
        Perfil perfilUsuario = perfilRepository.findByNome(perfilEnum)
                .orElseThrow(() -> new RuntimeException("Erro: Perfil " + perfilEnum.getNome() + " não encontrado."));
        perfis.add(perfilUsuario);
        usuario.setPerfis(perfis);

        return usuarioRepository.save(usuario);
    }

    // Métodos para recuperação de senha (esqueci minha senha, redefinir senha)
    // exigiriam uma lógica mais complexa, como:
    // 1. Geração de token de redefinição de senha (diferente do JWT de sessão).
    // 2. Envio de email com o link/token de redefinição.
    // 3. Validação do token de redefinição.
    // 4. Permissão para o usuário definir uma nova senha.

    // Exemplo de método para mudar senha (usuário logado)
    @Transactional
    public void mudarSenha(String emailUsuarioLogado, String senhaAntiga, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAntiga, usuario.getPassword())) {
            throw new RuntimeException("Senha antiga incorreta.");
        }

        // Adicionar validações para a nova senha (força, etc.)
        if (novaSenha == null || novaSenha.length() < 6) {
            throw new RuntimeException("Nova senha deve ter pelo menos 6 caracteres.");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }
}

