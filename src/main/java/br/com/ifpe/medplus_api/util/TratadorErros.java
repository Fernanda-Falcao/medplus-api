package br.com.ifpe.medplus_api.util;

import br.com.ifpe.medplus_api.util.exception.EntidadeNaoEncontradaException;
import jakarta.persistence.EntityExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
//import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratador global de exceções para a API.
 * Captura exceções específicas e comuns, retornando respostas HTTP padronizadas.
 */
@ControllerAdvice // Indica que esta classe aconselha múltiplos controllers.
public class TratadorErros extends ResponseEntityExceptionHandler {

    private static final Logger loggerGlobal = LoggerFactory.getLogger(TratadorErros.class);

    /**
     * Representa o corpo da resposta de erro padrão.
     */
    private record ApiErrorResponse(
            int status,
            String error,
            String message,
            String path,
            LocalDateTime timestamp,
            Map<String, String> validationErrors // Para erros de validação
    ) {
        ApiErrorResponse(HttpStatus status, String message, String path, Map<String, String> validationErrors) {
            this(status.value(), status.getReasonPhrase(), message, path, LocalDateTime.now(), validationErrors);
        }
        ApiErrorResponse(HttpStatus status, String message, String path) {
            this(status, message, path, null);
        }
    }

    /**
     * Trata EntidadeNaoEncontradaException (HTTP 404 Not Found).
     */
    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public ResponseEntity<Object> handleEntidadeNaoEncontrada(EntidadeNaoEncontradaException ex, WebRequest request) {
        loggerGlobal.warn("Entidade não encontrada: {}", ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Trata EntityExistsException (HTTP 409 Conflict).
     * Usado quando se tenta criar uma entidade que já existe (ex: email duplicado).
     */
    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<Object> handleEntityExists(EntityExistsException ex, WebRequest request) {
        loggerGlobal.warn("Conflito ao criar entidade: {}", ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Trata BadCredentialsException (HTTP 401 Unauthorized).
     * Lançada pelo Spring Security quando as credenciais de login são inválidas.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        loggerGlobal.warn("Credenciais inválidas: {}", ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.", request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Trata AuthenticationException (HTTP 401 Unauthorized).
     * Captura outras exceções de autenticação do Spring Security.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        loggerGlobal.warn("Falha na autenticação: {}", ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Trata AccessDeniedException (HTTP 403 Forbidden).
     * Lançada pelo Spring Security quando um usuário autenticado tenta acessar um recurso
     * para o qual não tem permissão.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        loggerGlobal.warn("Acesso negado: {}", ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.FORBIDDEN, "Acesso negado.", request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Trata IllegalArgumentException (HTTP 400 Bad Request).
     * Útil para argumentos de método inválidos que não são cobertos por validações de bean.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        loggerGlobal.warn("Argumento ilegal: {}", ex.getMessage());
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Trata HttpMessageNotReadableException (HTTP 400 Bad Request).
     * Ocorre quando o corpo da requisição não pode ser lido/parseado (ex: JSON malformado).
     */
    
     @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        @NonNull HttpMessageNotReadableException ex, 
        @NonNull HttpHeaders headers, 
        @NonNull HttpStatusCode status, 
        @NonNull WebRequest request) {
    loggerGlobal.warn("Requisição malformada: {}", ex.getMessage());
    ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.BAD_REQUEST, "Formato da requisição inválido ou JSON malformado.", request.getDescription(false));
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, 
            @NonNull HttpHeaders headers, 
            @NonNull HttpStatusCode status, 
            @NonNull WebRequest request) {
        
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, 
                                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Valor inválido",
                                        (existingValue, newValue) -> existingValue + "; " + newValue)); // Em caso de múltiplos erros no mesmo campo
        
        loggerGlobal.warn("Erro de validação: {}", errors);
        ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.BAD_REQUEST, "Erro de validação nos dados de entrada.", request.getDescription(false), errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}

