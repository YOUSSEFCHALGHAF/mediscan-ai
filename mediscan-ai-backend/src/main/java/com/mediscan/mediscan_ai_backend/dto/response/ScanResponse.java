package com.mediscan.mediscan_ai_backend.dto.response;



import com.mediscan.mediscan_ai_backend.entity.ScanMedicament;
import com.mediscan.mediscan_ai_backend.entity.enums.StatutScan;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un scan de médicament (historique ou détail). */
public record ScanResponse(
        Long id,
        LocalDateTime dateScan,
        StatutScan statut,
        Double scoreConfiance,
        String numeroLot,
        LocalDate dateExpiration,
        String texteExtrait,
        String imageUrl,
        MedicamentResumeResponse medicament
) {
    public static ScanResponse from(ScanMedicament s) {
        return new ScanResponse(s.getId(), s.getDateScan(), s.getStatut(), s.getScoreConfiance(),
                s.getNumeroLot(), s.getDateExpiration(), s.getTexteExtrait(), s.getImageUrl(),
                MedicamentResumeResponse.from(s.getMedicament()));
    }
}