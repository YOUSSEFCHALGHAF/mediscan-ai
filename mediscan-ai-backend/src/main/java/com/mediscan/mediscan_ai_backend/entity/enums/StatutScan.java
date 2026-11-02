package com.mediscan.mediscan_ai_backend.entity.enums;

/** État d'un scan. Correspond à scans. Statut. */
public enum StatutScan {
    EN_COURS,     // image reçue, analyse OCR en cours
    TERMINE,      // médicament reconnu et vérifié
    A_VERIFIER,   // OCR incertain : le patient doit confirmer
    ECHEC         // image illisible ou erreur technique
}