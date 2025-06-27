
package br.com.ifpe.medplus_api.config;

import br.com.ifpe.medplus_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor // Usa Lombok para criar um construtor para as dependências 'final'
public class ApplicationConfiguration {

    // Em vez de injetar o UserDetailsService, injetamos o Repositório para poder criá-lo.
    private final UsuarioRepository usuarioRepository;

    // =================================================================
    // == ESTE É O BEAN QUE ESTAVA FALTANDO E QUE PRECISAMOS CRIAR ==
    // =================================================================
    @Bean
    public UserDetailsService userDetailsService() {
        // Usamos uma expressão lambda para implementar o método 'loadUserByUsername'
        return username -> usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));
    }

    // ===================================================================================
    // == Este bean agora usa os outros beans definidos DENTRO desta mesma classe ==
    // ===================================================================================
    @Bean
    public AuthenticationProvider authenticationProvider() { // Retornar a interface é uma boa prática
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // Chama o método userDetailsService() acima
        authProvider.setPasswordEncoder(passwordEncoder());   // Chama o método passwordEncoder() abaixo
        return authProvider;
    }
    
    // Este bean está correto
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // O bean do AuthenticationManager que você tem na sua SecurityConfiguration
    // pode permanecer lá ou ser movido para cá, se preferir.
    // Ex:
    // @Bean
    // public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    //     return config.getAuthenticationManager();
    // }
}