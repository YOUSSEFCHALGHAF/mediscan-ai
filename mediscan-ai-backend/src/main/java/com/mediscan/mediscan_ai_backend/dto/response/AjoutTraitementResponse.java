package com.mediscan.mediscan_ai_backend.dto.response;

/**
 * Résultat de l'ajout d'un médicament scanné aux traitements en cours.
 *
 * @param substance   substance(s) active(s) concernée(s), ex. "ibuprofène"
 * @param dejaPresent vrai si elle figurait déjà dans les traitements (rien n'a été modifié)
 * @param anamnese    l'anamnèse à jour
 */
public record AjoutTraitementResponse(String substance, boolean dejaPresent, AnamneseResponse anamnese) {
}