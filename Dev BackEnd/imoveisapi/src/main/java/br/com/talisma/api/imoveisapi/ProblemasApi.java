package br.com.talisma.api.imoveisapi;

import java.util.LinkedHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ProblemasApi extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var campos = new LinkedHashMap<String, String>();
        erro.getBindingResult().getFieldErrors().forEach(campo -> campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        erro.getBody().setProperty("campos", campos);
        erro.getBody().setDetail("Verifique os campos informados.");
        return handleExceptionInternal(erro, erro.getBody(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problema) {
            HttpStatus padrao = HttpStatus.resolve(status.value());
            problema.setProperty("codigo", padrao == null ? "HTTP_" + status.value() : padrao.name());
        }
        return super.createResponseEntity(body, headers, status, request);
    }
}
