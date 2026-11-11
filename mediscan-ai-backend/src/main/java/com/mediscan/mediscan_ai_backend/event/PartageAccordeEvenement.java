package com.mediscan.mediscan_ai_backend.event;

/** Publié quand un patient donne accès à ses données à un médecin. */
public record PartageAccordeEvenement(Long medecinId, Long patientId) {
}