package com.mediscan.mediscan_ai_backend.event;

/** Publié quand le moteur de règles a généré au moins une alerte de gravité ÉLEVÉE pour un patient. */
public record AlertesGravesEvenement(Long patientId, int nombre) {
}