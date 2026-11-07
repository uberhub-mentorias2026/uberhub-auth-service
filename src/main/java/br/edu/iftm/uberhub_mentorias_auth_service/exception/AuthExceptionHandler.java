package br.edu.iftm.uberhub_mentorias_auth_service.exception;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import br.edu.iftm.uberhub_mentorias_auth_service.controller.AuthController;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Object> handleAuth(AuthException exception, WebRequest request) {
        return response(exception.code(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        return response(AuthErrorCode.INTERNAL_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = problem(AuthErrorCode.VALIDATION_ERROR, request);
        problem.setProperty("fieldErrors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message",
                        error.getDefaultMessage() == null ? "Campo inválido." : error.getDefaultMessage()))
                .toList());
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(AuthErrorCode.MALFORMED_REQUEST, request);
    }

    private ResponseEntity<Object> response(AuthErrorCode code, WebRequest request) {
        var problem = problem(code, request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private ProblemDetail problem(AuthErrorCode code, WebRequest request) {
        HttpStatus status = switch (code) {
            case VALIDATION_ERROR, MALFORMED_REQUEST -> HttpStatus.BAD_REQUEST;
            case INVALID_FIREBASE_TOKEN, INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
            case ACCOUNT_INACTIVE -> HttpStatus.FORBIDDEN;
            case ACCOUNT_ALREADY_EXISTS, REGISTRATION_REQUIRED -> HttpStatus.CONFLICT;
            case DEPENDENCY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        String detail = switch (code) {
            case VALIDATION_ERROR -> "Verifique os campos informados.";
            case MALFORMED_REQUEST -> "Corpo JSON inválido.";
            case INVALID_FIREBASE_TOKEN -> "Token Firebase inválido ou expirado.";
            case INVALID_REFRESH_TOKEN -> "Refresh token inválido ou expirado.";
            case ACCOUNT_INACTIVE -> "Conta bloqueada ou inativa.";
            case ACCOUNT_ALREADY_EXISTS -> "Esta identidade já possui cadastro no UberHub.";
            case REGISTRATION_REQUIRED -> "Conclua seu cadastro no UberHub para iniciar a sessão.";
            case DEPENDENCY_UNAVAILABLE -> "Serviço temporariamente indisponível. Tente novamente mais tarde.";
            case INTERNAL_ERROR -> "Não foi possível concluir a operação.";
        };
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code.name());
        problem.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        return problem;
    }
}
