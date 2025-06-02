package br.com.ifpe.medplus_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para autenticação JWT.
 * Este filtro intercepta todas as requisições, verifica a presença de um token JWT no header Authorization,
 * valida o token e, se válido, configura o SecurityContextHolder com os detalhes do usuário autenticado.
 */
@Component // Registra o filtro como um componente Spring para ser gerenciado pelo contêiner IoC.
@RequiredArgsConstructor // Lombok para gerar construtor com os campos final.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Injetado a partir da ApplicationConfiguration

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);
        final String jwt;
        final String userEmail;

        // Se não houver header Authorization ou não começar com "Bearer ", continua a cadeia de filtros.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrai o token JWT (remove o prefixo "Bearer ").
        jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            userEmail = jwtService.extractUsername(jwt); // Extrai o email do usuário do token.

            // Se o email foi extraído e não há autenticação no contexto de segurança atual.
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Carrega os detalhes do usuário a partir do email.
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Valida o token.
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Se o token for válido, cria um objeto de autenticação.
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,       // Principal (o objeto UserDetails)
                            null,              // Credenciais (não necessárias após autenticação por token)
                            userDetails.getAuthorities() // Autoridades (roles)
                    );
                    // Define detalhes adicionais da autenticação (ex: endereço IP, etc.).
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Define o objeto de autenticação no SecurityContextHolder.
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Em caso de exceção na validação do token (expirado, inválido, etc.),
            // não configuramos o SecurityContext e a requisição não será autenticada.
            // Pode-se logar a exceção aqui.
            logger.warn("Não foi possível processar o token JWT: " + e.getMessage());
            // response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token JWT inválido ou expirado");
            // return; // Opcional: pode retornar um erro 401 aqui ou deixar o Spring Security tratar
        }

        // Continua a cadeia de filtros.
        filterChain.doFilter(request, response);
    }
}

