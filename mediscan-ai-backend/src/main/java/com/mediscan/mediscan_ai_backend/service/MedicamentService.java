package com.mediscan.mediscan_ai_backend.service;


import com.mediscan.mediscan_ai_backend.dto.response.MedicamentResponse;
import com.mediscan.mediscan_ai_backend.exception.RequeteInvalideException;
import com.mediscan.mediscan_ai_backend.exception.ResourceNotFoundException;
import com.mediscan.mediscan_ai_backend.repository.MedicamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consultation de la base médicamenteuse de référence. */
@Service
@RequiredArgsConstructor
public class MedicamentService {

    private static final int LONGUEUR_MIN = 2;
    private static final int RESULTATS_MAX = 20;

    private final MedicamentRepository medicamentRepository;

    /** Recherche par nom commercial ou DCI, insensible à la casse et aux accents. */
    @Transactional(readOnly = true)
    public List<MedicamentResponse> rechercher(String terme) {
        if (terme == null || terme.trim().length() < LONGUEUR_MIN) {
            throw new RequeteInvalideException(
                    "Saisissez au moins " + LONGUEUR_MIN + " caractères pour rechercher un médicament");
        }
        return medicamentRepository.rechercher(terme.trim()).stream()
                .limit(RESULTATS_MAX)
                .map(MedicamentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicamentResponse lire(Long id) {
        return medicamentRepository.findById(id)
                .map(MedicamentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable"));
    }
}