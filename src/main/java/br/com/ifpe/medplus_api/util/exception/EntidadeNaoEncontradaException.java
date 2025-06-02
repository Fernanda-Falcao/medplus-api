package br.com.ifpe.medplus_api.util.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção customizada para indicar que uma entidade não foi encontrada.
 * Resulta em um status HTTP 404 (Not Found).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntidadeNaoEncontradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    public EntidadeNaoEncontradaException(String entidade, Long id) {
        super(String.format("%s com ID %d não encontrado(a).", entidade, id));
    }

    public EntidadeNaoEncontradaException(String entidade, String identificador) {
        super(String.format("%s com identificador '%s' não encontrado(a).", entidade, identificador));
    }
}
