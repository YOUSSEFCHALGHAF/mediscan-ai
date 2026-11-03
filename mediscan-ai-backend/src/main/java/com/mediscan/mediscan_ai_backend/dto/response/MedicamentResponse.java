package com.mediscan.mediscan_ai_backend.dto.response;


import com.mediscan.mediscan_ai_backend.entity.Medicament;

/** Fiche complète d'un médicament. */
public record MedicamentResponse(
        Long id,
        String nomCommercial,
        String dci,
        String dosage,
        String formeGalenique,
        String posologie,
        String contreIndications,
        String interactions,
        String laboratoire
) {
    public static MedicamentResponse from(Medicament m) {
        return new MedicamentResponse(m.getId(), m.getNomCommercial(), m.getDci(), m.getDosage(),
                m.getFormeGalenique(), m.getPosologie(), m.getContreIndications(),
                m.getInteractions(), m.getLaboratoire());
    }
}