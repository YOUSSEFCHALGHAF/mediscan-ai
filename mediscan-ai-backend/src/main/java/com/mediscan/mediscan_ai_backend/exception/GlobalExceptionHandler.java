package com.mediscan.mediscan_ai_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Transforme toutes les exceptions des contrôleurs en réponses JSON (ApiError). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 400 ====================

    /** Erreurs de validation (@NotBlank, @Email, @Size...) champ par champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> details.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return construire(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", req, details);
    }

    /** JSON mal formé ou mauvais format (dates AAAA-MM-JJ, heures HH : mm). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonIllisible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return construire(HttpStatus.BAD_REQUEST,
                "Corps de requête illisible : vérifiez le JSON, les dates (AAAA-MM-JJ) et les heures (HH:mm)",
                req, null);
    }

    /** Paramètre d'URL du mauvais type (ex. /api/medecin/patients/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> mauvaisType(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return construire(HttpStatus.BAD_REQUEST,
                "Paramètre « " + ex.getName() + " » invalide", req, null);
    }

    /** Paramètre obligatoire manquant (ex. ?q= absent). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametreManquant(MissingServletRequestParameterException ex,
                                                      HttpServletRequest req) {
        return construire(HttpStatus.BAD_REQUEST,
                "Paramètre obligatoire manquant : " + ex.getParameterName(), req, null);
    }

    /** Règle métier non respectée. */
    @ExceptionHandler(RequeteInvalideException.class)
    public ResponseEntity<ApiError> requeteInvalide(RequeteInvalideException ex, HttpServletRequest req) {
        return construire(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    // ==================== 401 / 403 ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> identifiants(BadCredentialsException ex, HttpServletRequest req) {
        return construire(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect", req, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> compteDesactive(DisabledException ex, HttpServletRequest req) {
        return construire(HttpStatus.FORBIDDEN, "Ce compte est désactivé", req, null);
    }

    /** Pas de consentement actif du patient. */
    @ExceptionHandler(ConsentementRequisException.class)
    public ResponseEntity<ApiError> consentement(ConsentementRequisException ex, HttpServletRequest req) {
        return construire(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    /** Refus d'accès levé dans un contrôleur (ex. @PreAuthorize). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesRefuse(AccessDeniedException ex, HttpServletRequest req) {
        return construire(HttpStatus.FORBIDDEN, "Accès refusé", req, null);
    }

    // ==================== 404 / 405 / 409 ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> introuvable(ResourceNotFoundException ex, HttpServletRequest req) {
        return construire(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /** URL inexistante (sans ce handler, elle tomberait dans le 500 générique). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> urlInconnue(NoResourceFoundException ex, HttpServletRequest req) {
        return construire(HttpStatus.NOT_FOUND, "Cette URL n'existe pas", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methode(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return construire(HttpStatus.METHOD_NOT_ALLOWED,
                "Méthode " + ex.getMethod() + " non autorisée sur cette URL", req, null);
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ApiError> conflit(ConflitException ex, HttpServletRequest req) {
        return construire(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** Contrainte MySQL violée (UNIQUE, CHECK, clé étrangère). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrite(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Contrainte de base violée sur {} : {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return construire(HttpStatus.CONFLICT,
                "Opération refusée : " + ex.getMostSpecificCause().getMessage(),
                request, null);
    }
    // ==================== 500 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erreurInterne(Exception ex, HttpServletRequest req) {
        log.error("Erreur interne sur {} : ", req.getRequestURI(), ex);
        return construire(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", req, null);
    }

    private ResponseEntity<ApiError> construire(HttpStatus statut, String message,
                                                HttpServletRequest req, Map<String, String> details) {
        return ResponseEntity.status(statut)
                .body(ApiError.of(statut.value(), statut.getReasonPhrase(), message, req.getRequestURI(), details));
    }
}