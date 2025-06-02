package br.com.ifpe.medplus_api.security;

import br.com.ifpe.medplus_api.model.acesso.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço para manipulação de JSON Web Tokens (JWT).
 * Responsável por gerar, validar e extrair informações de tokens JWT.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}") // Chave secreta para assinar o token, injetada do application.properties
    private String secretKey;

    @Value("${jwt.expiration.ms}") // Tempo de expiração do token em milissegundos
    private long jwtExpirationMs;

    // Se você implementar refresh tokens, precisará de uma expiração separada para eles.
    // @Value("${jwt.refresh.token.expiration.ms}")
    // private long refreshExpirationMs;

    /**
     * Extrai o nome de usuário (email) do token JWT.
     *
     * @param token O token JWT.
     * @return O nome de usuário (email).
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai um claim específico do token JWT.
     *
     * @param token          O token JWT.
     * @param claimsResolver Função para resolver o claim desejado.
     * @param <T>            O tipo do claim.
     * @return O claim extraído.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Gera um token JWT para um usuário.
     *
     * @param userDetails Detalhes do usuário.
     * @return O token JWT gerado.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof Usuario) {
            Usuario usuario = (Usuario) userDetails;
            extraClaims.put("userId", usuario.getId()); // Adiciona o ID do usuário ao token
            // Adiciona os perfis (roles) ao token
            String roles = usuario.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            extraClaims.put("roles", roles);
        }
        return generateToken(extraClaims, userDetails);
    }

    /**
     * Gera um token JWT com claims extras.
     *
     * @param extraClaims Claims adicionais a serem incluídos no token.
     * @param userDetails Detalhes do usuário.
     * @return O token JWT gerado.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpirationMs);
    }

    // Se implementar refresh token:
    // public String generateRefreshToken(UserDetails userDetails) {
    //     return buildToken(new HashMap<>(), userDetails, refreshExpirationMs);
    // }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // Define o "subject" do token (geralmente o email/username)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Data de emissão
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Data de expiração
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Assina o token
                .compact();
    }

    /**
     * Valida se um token JWT é válido para um usuário específico.
     *
     * @param token       O token JWT.
     * @param userDetails Detalhes do usuário.
     * @return true se o token for válido, false caso contrário.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica se um token JWT expirou.
     *
     * @param token O token JWT.
     * @return true se o token expirou, false caso contrário.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrai a data de expiração do token JWT.
     *
     * @param token O token JWT.
     * @return A data de expiração.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrai todos os claims (informações) do token JWT.
     *
     * @param token O token JWT.
     * @return Os claims do token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Obtém a chave de assinatura (Key) a partir da string secreta.
     *
     * @return A chave de assinatura.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extrai o ID do usuário do token JWT.
     *
     * @param token O token JWT.
     * @return O ID do usuário, ou null se não encontrado.
     */
    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            // Logar o erro ou tratar conforme necessário
            return null;
        }
    }

    /**
     * Extrai os perfis (roles) do usuário do token JWT.
     *
     * @param token O token JWT.
     * @return Uma string contendo os perfis separados por vírgula, ou null.
     */
    public String extractRoles(String token) {
         try {
            Claims claims = extractAllClaims(token);
            return claims.get("roles", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}



