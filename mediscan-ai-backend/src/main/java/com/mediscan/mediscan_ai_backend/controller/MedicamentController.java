package com.mediscan.mediscan_ai_backend.controller;


import com.mediscan.mediscan_ai_backend.dto.response.MedicamentResponse;
import com.mediscan.mediscan_ai_backend.service.MedicamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Base médicamenteuse : accessible à tout utilisateur connecté (patient ou médecin). */
@RestController
@RequestMapping("/api/medicaments")
@RequiredArgsConstructor
public class MedicamentController {

    /** GET /api/medicaments?q=doli */
    private final MedicamentService medicamentService;

    @GetMapping
    public List<MedicamentResponse> rechercher(@RequestParam("q") String terme) {
        return medicamentService.rechercher(terme);
    }

    @GetMapping("/{id}")
    public MedicamentResponse lire(@PathVariable Long id) {
        return medicamentService.lire(id);
    }
}