-- =====================================================================
--  MediScan AI - 03 : requêtes de vérification
--  Ces requêtes préfigurent ce que fera l'API (fiche patient, dashboard...)
-- =====================================================================

SET NAMES utf8mb4;
USE mediscan_db;

-- 1. Comptes de test et leur rôle
SELECT u.id, CONCAT(u.prenom, ' ', u.nom) AS utilisateur, u.email, u.role
FROM users u
ORDER BY u.role, u.id;

-- 2. Fiche synthétique d'un patient (anamnèse) - ici Marc Lefèvre (id 4)
SELECT CONCAT(u.prenom, ' ', u.nom) AS patient,
       TIMESTAMPDIFF(YEAR, p.date_naissance, CURDATE()) AS age,
       p.groupe_sanguin, a.allergies, a.pathologies_chroniques, a.traitements_en_cours
FROM patients p
JOIN users u     ON u.id = p.id
JOIN anamneses a ON a.patient_id = p.id
WHERE p.id = 4;

-- 3. Patients visibles par le Dr Mansour (id 1) : consentement ACTIF uniquement
--    => doit afficher Marc Lefèvre, mais PAS Nadia Benali (consentement révoqué)
SELECT CONCAT(u.prenom, ' ', u.nom) AS patient, pa.perimetre_donnees, pa.date_consentement
FROM partages pa
JOIN users u ON u.id = pa.patient_id
WHERE pa.medecin_id = 1 AND pa.actif = TRUE;

-- 4. Historique des scans avec le médicament reconnu
SELECT s.id, CONCAT(u.prenom, ' ', u.nom) AS patient, s.date_scan,
       COALESCE(m.nom_commercial, '(non reconnu)') AS medicament,
       s.score_confiance, s.statut
FROM scans s
JOIN users u            ON u.id = s.patient_id
LEFT JOIN medicaments m ON m.id = s.medicament_id
ORDER BY s.date_scan DESC;

-- 5. Alertes NON traitées (pour le dashboard médecin)
SELECT al.id, CONCAT(u.prenom, ' ', u.nom) AS patient, al.type, al.niveau_gravite, al.message
FROM alertes al
JOIN users u ON u.id = al.patient_id
WHERE al.traitee = FALSE
ORDER BY FIELD(al.niveau_gravite, 'ELEVE', 'MOYEN', 'FAIBLE'), al.date_generation DESC;

-- 6. Rappels actifs à envoyer à 20:00 (ce que fera le @Scheduled à l'étape 17)
SELECT r.id, CONCAT(u.prenom, ' ', u.nom) AS patient, r.libelle, m.nom_commercial, r.heure_prise
FROM rappels r
JOIN users u       ON u.id = r.patient_id
JOIN medicaments m ON m.id = r.medicament_id
WHERE r.actif = TRUE AND r.heure_prise = '20:00:00';

-- 7. Recherche dans la base médicamenteuse (ce que fera l'OCR) : par nom ou DCI
SELECT id, nom_commercial, dci, dosage
FROM medicaments
WHERE nom_commercial LIKE '%advil%' OR dci LIKE '%ibuprof%';