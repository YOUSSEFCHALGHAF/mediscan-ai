package com.mediscan.mediscan_ai_backend.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforme toutes les exceptions des contrôleurs en réponses JSON propres (ApiError).
 * Sera complété à l'étape 6.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400 : erreurs de validation (@NotBlank, @Email, @Size...) champ par champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,
                                               HttpServletRequest req) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return construire(HttpStatus.BAD_REQUEST,
                "Les données envoyées sont invalides", req, details);
    }

    /** 400 : JSON mal formé ou mauvais format (ex. date "12/04/1995" au lieu de "1995-04-12"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonIllisible(HttpMessageNotReadableException ex,
                                                  HttpServletRequest req) {
        return construire(HttpStatus.BAD_REQUEST,
                "Corps de requête illisible : vérifiez le JSON et le format des dates (AAAA-MM-JJ)",
                req, null);
    }

    /** 401 : email ou mot de passe incorrect (message volontairement vague). */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> identifiants(BadCredentialsException ex, HttpServletRequest req) {
        return construire(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect", req, null);
    }

    /** 403 : compte désactivé. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> compteDesactive(DisabledException ex, HttpServletRequest req) {
        return construire(HttpStatus.FORBIDDEN, "Ce compte est désactivé", req, null);
    }

    /** 404 */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> introuvable(ResourceNotFoundException ex, HttpServletRequest req) {
        return construire(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /** 409 */
    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ApiError> conflit(ConflitException ex, HttpServletRequest req) {
        return construire(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 500 : toute autre erreur. Le détail technique reste dans les logs, jamais dans la réponse. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erreurInterne(Exception ex, HttpServletRequest req) {
        log.error("Erreur interne sur {} : ", req.getRequestURI(), ex);
        return construire(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue", req, null);
    }

    private ResponseEntity<ApiError> construire(HttpStatus statut, String message,
                                                HttpServletRequest req, Map<String, String> details) {
        ApiError erreur = ApiError.of(statut.value(), statut.getReasonPhrase(),
                message, req.getRequestURI(), details);
        return ResponseEntity.status(statut).body(erreur);
    }
}