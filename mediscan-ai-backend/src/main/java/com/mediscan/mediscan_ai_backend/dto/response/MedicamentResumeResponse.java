package com.mediscan.mediscan_ai_backend.dto.response;


import com.mediscan.mediscan_ai_backend.entity.Medicament;

/** Version courte d'un médicament (dans les listes de scans, rappels...). */
public record MedicamentResumeResponse(Long id, String nomCommercial, String dci, String dosage) {

    /** Renvoie null si le médicament est null (ex. scan non reconnu). */
    public static MedicamentResumeResponse from(Medicament m) {
        if (m == null) {
            return null;
        }
        return new MedicamentResumeResponse(m.getId(), m.getNomCommercial(), m.getDci(), m.getDosage());
    }
}