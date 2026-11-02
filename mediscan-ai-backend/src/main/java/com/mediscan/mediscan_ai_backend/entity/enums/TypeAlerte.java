package com.mediscan.mediscan_ai_backend.entity.enums;

/** Nature d'une alerte. Correspond à alertes. Type. */
public enum TypeAlerte {
    INTERACTION,        // deux médicaments incompatibles
    CONTRE_INDICATION,  // médicament déconseillé avec une pathologie du patient
    ALLERGIE,           // médicament contenant une substance allergène déclarée
    EXPIRATION          // médicament périmé ou proche de la date d'expiration
}