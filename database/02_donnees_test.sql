-- =====================================================================
--  MediScan AI - 02 : données de démonstration
--  Mot de passe de TOUS les comptes de test : MediScan2026!
--  (hash BCrypt, coût 10, compatible avec BCryptPasswordEncoder de Spring)
--
--  AVERTISSEMENT : données médicales SIMPLIFIÉES à but pédagogique.
--  Elles ne constituent pas une référence médicale.
-- =====================================================================

SET NAMES utf8mb4;
USE mediscan_db;

-- Vide les tables (ordre inverse des dépendances) et remet les compteurs à 1
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE device_tokens;
TRUNCATE TABLE notifications;
TRUNCATE TABLE partages;
TRUNCATE TABLE rappels;
TRUNCATE TABLE alertes;
TRUNCATE TABLE scans;
TRUNCATE TABLE medicaments;
TRUNCATE TABLE anamneses;
TRUNCATE TABLE medecins;
TRUNCATE TABLE patients;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. Utilisateurs (2 médecins + 3 patients)
-- ---------------------------------------------------------------------
SET @hash = '$2a$10$zsHN8Mw6YF12gwkoa9Ko2eVWdh9MVoKiBEPWyryo531d1TpwPuhHG';

INSERT INTO users (id, nom, prenom, email, mot_de_passe_hash, role, actif, date_creation) VALUES
(1, 'Mansour', 'Karim',   'karim.mansour@mediscan.test',   @hash, 'MEDECIN', TRUE, '2026-09-01 09:00:00'),
(2, 'Dubois',  'Claire',  'claire.dubois@mediscan.test',   @hash, 'MEDECIN', TRUE, '2026-09-02 10:30:00'),
(3, 'Haddad',  'Yasmine', 'yasmine.haddad@mediscan.test',  @hash, 'PATIENT', TRUE, '2026-09-10 14:00:00'),
(4, 'Lefèvre', 'Marc',    'marc.lefevre@mediscan.test',    @hash, 'PATIENT', TRUE, '2026-09-12 08:45:00'),
(5, 'Benali',  'Nadia',   'nadia.benali@mediscan.test',    @hash, 'PATIENT', TRUE, '2026-09-15 17:20:00');

INSERT INTO medecins (id, numero_rpps, specialite, telephone) VALUES
(1, '10001234567', 'Cardiologie',        '+33600000001'),
(2, '10007654321', 'Médecine générale',  '+33600000002');

INSERT INTO patients (id, date_naissance, sexe, groupe_sanguin, telephone) VALUES
(3, '1995-04-12', 'FEMME', 'A+', '+33600000003'),
(4, '1958-11-03', 'HOMME', 'O+', '+33600000004'),
(5, '1971-07-25', 'FEMME', 'B-', '+33600000005');

-- ---------------------------------------------------------------------
-- 2. Anamnèses (une par patient)
-- ---------------------------------------------------------------------
INSERT INTO anamneses (patient_id, antecedents, allergies, pathologies_chroniques,
                       traitements_en_cours, habitudes_vie, date_maj) VALUES
(3, 'Appendicectomie (2012)',
    'amoxicilline, pénicilline',
    'asthme',
    'salbutamol',
    'Non-fumeuse, sport 2 fois par semaine',
    '2026-09-10 14:30:00'),
(4, 'Infarctus du myocarde (2019), pose de stent',
    'aucune',
    'hypertension artérielle, fibrillation auriculaire',
    'warfarine, amlodipine',
    'Ancien fumeur (arrêt en 2019)',
    '2026-09-12 09:00:00'),
(5, 'Césarienne (2003)',
    'aucune',
    'diabète de type 2',
    'metformine',
    'Sédentaire, régime pauvre en sucres',
    '2026-09-15 17:40:00');

-- ---------------------------------------------------------------------
-- 3. Base médicamenteuse simulée (12 médicaments courants)
-- ---------------------------------------------------------------------
INSERT INTO medicaments (id, nom_commercial, dci, dosage, forme_galenique, posologie,
                         contre_indications, interactions, laboratoire) VALUES
(1,  'Doliprane', 'paracétamol', '1000 mg', 'comprimé',
     '1 comprimé toutes les 6 heures, maximum 3 g par jour',
     'insuffisance hépatique sévère, allergie au paracétamol',
     'warfarine', 'Sanofi'),
(2,  'Dafalgan', 'paracétamol', '500 mg', 'gélule',
     '1 à 2 gélules toutes les 6 heures, maximum 3 g par jour',
     'insuffisance hépatique sévère, allergie au paracétamol',
     'warfarine', 'UPSA'),
(3,  'Advil', 'ibuprofène', '400 mg', 'comprimé enrobé',
     '1 comprimé toutes les 6 à 8 heures, maximum 1200 mg par jour',
     'ulcère gastroduodénal, insuffisance rénale sévère, insuffisance cardiaque sévère, grossesse à partir du 6e mois, asthme déclenché par les AINS',
     'warfarine, acide acétylsalicylique, méthotrexate, lithium', 'Haleon'),
(4,  'Aspirine UPSA', 'acide acétylsalicylique', '500 mg', 'comprimé effervescent',
     '1 à 2 comprimés toutes les 4 heures, maximum 3 g par jour',
     'ulcère gastroduodénal, troubles de la coagulation, grossesse à partir du 6e mois',
     'warfarine, ibuprofène, méthotrexate', 'UPSA'),
(5,  'Kardégic', 'acide acétylsalicylique', '75 mg', 'poudre pour solution buvable',
     '1 sachet par jour',
     'ulcère gastroduodénal, troubles de la coagulation',
     'warfarine, ibuprofène', 'Sanofi'),
(6,  'Clamoxyl', 'amoxicilline', '1 g', 'comprimé dispersible',
     '1 comprimé 2 à 3 fois par jour pendant 7 jours',
     'allergie aux pénicillines, mononucléose infectieuse',
     'méthotrexate, allopurinol', 'GSK'),
(7,  'Augmentin', 'amoxicilline, acide clavulanique', '1 g / 125 mg', 'sachet',
     '1 sachet 2 à 3 fois par jour pendant 7 jours',
     'allergie aux pénicillines, antécédent d''atteinte hépatique sous amoxicilline',
     'méthotrexate, allopurinol', 'GSK'),
(8,  'Coumadine', 'warfarine', '5 mg', 'comprimé sécable',
     'Dose individuelle adaptée selon l''INR',
     'grossesse, hémorragie en cours, ulcère gastroduodénal évolutif',
     'acide acétylsalicylique, ibuprofène, paracétamol, amoxicilline', 'Bristol-Myers Squibb'),
(9,  'Glucophage', 'metformine', '850 mg', 'comprimé pelliculé',
     '1 comprimé 2 à 3 fois par jour pendant ou après les repas',
     'insuffisance rénale sévère, acidocétose diabétique, insuffisance hépatique',
     'produits de contraste iodés, alcool', 'Merck'),
(10, 'Ventoline', 'salbutamol', '100 µg/dose', 'suspension pour inhalation',
     '1 à 2 bouffées en cas de crise',
     'hypersensibilité au salbutamol',
     'bêtabloquants', 'GSK'),
(11, 'Amlor', 'amlodipine', '5 mg', 'gélule',
     '1 gélule par jour',
     'hypotension sévère, choc cardiogénique',
     'simvastatine, clarithromycine', 'Pfizer'),
(12, 'Spasfon', 'phloroglucinol', '80 mg', 'comprimé orodispersible',
     '2 comprimés au moment de la crise, à renouveler si besoin',
     'hypersensibilité au phloroglucinol',
     NULL, 'Teva');

-- ---------------------------------------------------------------------
-- 4. Historique de scans
-- ---------------------------------------------------------------------
INSERT INTO scans (id, patient_id, medicament_id, image_url, date_scan, texte_extrait,
                   numero_lot, date_expiration, score_confiance, statut) VALUES
(1, 3, 1,  'uploads/scans/demo-doliprane.jpg', '2026-10-05 10:12:00',
    'DOLIPRANE 1000 mg paracétamol 8 comprimés LOT 24B117 EXP 03/2028',
    '24B117', '2028-03-31', 91.5, 'TERMINE'),
(2, 4, 3,  'uploads/scans/demo-advil.jpg', '2026-10-18 19:40:00',
    'ADVIL 400 mg ibuprofène 14 comprimés enrobés LOT 5K2290 EXP 11/2027',
    '5K2290', '2027-11-30', 87.0, 'TERMINE'),
(3, 5, 9,  'uploads/scans/demo-glucophage.jpg', '2026-10-20 08:05:00',
    'GLUCOPHAGE 850 mg metformine 30 comprimés pelliculés LOT G85A31 EXP 06/2027',
    'G85A31', '2027-06-30', 93.2, 'TERMINE'),
(4, 3, NULL, 'uploads/scans/demo-floue.jpg', '2026-10-22 16:30:00',
    'CLAM... 1g ... LOT ...',
    NULL, NULL, 34.0, 'A_VERIFIER');

-- ---------------------------------------------------------------------
-- 5. Alertes
-- ---------------------------------------------------------------------
INSERT INTO alertes (patient_id, scan_id, medicament_id, type, niveau_gravite,
                     message, date_generation, traitee) VALUES
(4, 2, 3, 'INTERACTION', 'ELEVE',
    'Interaction entre ibuprofène (Advil) et warfarine (traitement en cours) : risque hémorragique accru.',
    '2026-10-18 19:40:05', FALSE),
(4, 2, 3, 'CONTRE_INDICATION', 'MOYEN',
    'Advil est déconseillé en cas d''insuffisance cardiaque sévère : vérifier l''état cardiaque du patient.',
    '2026-10-18 19:40:05', FALSE),
(3, 4, NULL, 'ALLERGIE', 'MOYEN',
    'Scan à vérifier : le texte « CLAM... » pourrait correspondre à Clamoxyl (amoxicilline), allergie déclarée.',
    '2026-10-22 16:30:04', TRUE);

-- ---------------------------------------------------------------------
-- 6. Rappels de prise
-- ---------------------------------------------------------------------
INSERT INTO rappels (patient_id, medicament_id, libelle, heure_prise, frequence,
                     date_debut, date_fin, actif) VALUES
(4, 8, 'Coumadine du soir',        '20:00:00', 'QUOTIDIEN', '2026-09-12', NULL,         TRUE),
(5, 9, 'Glucophage petit-déjeuner', '08:00:00', 'QUOTIDIEN', '2026-09-15', NULL,         TRUE),
(5, 9, 'Glucophage dîner',          '20:00:00', 'QUOTIDIEN', '2026-09-15', NULL,         TRUE),
(3, 1, 'Doliprane si douleur',      '12:00:00', 'PONCTUEL',  '2026-10-05', '2026-10-08', FALSE);

-- ---------------------------------------------------------------------
-- 7. Partages / consentements
-- ---------------------------------------------------------------------
INSERT INTO partages (patient_id, medecin_id, perimetre_donnees, date_consentement,
                      date_revocation, actif) VALUES
(3, 2, 'ANAMNESE,SCANS,ALERTES', '2026-09-20 11:00:00', NULL, TRUE),
(4, 1, 'ANAMNESE,SCANS,ALERTES,RAPPELS', '2026-09-13 09:30:00', NULL, TRUE),
-- Consentement RÉVOQUÉ : le Dr Mansour ne doit plus voir Nadia Benali
(5, 1, 'ANAMNESE,SCANS', '2026-09-16 10:00:00', '2026-10-01 18:00:00', FALSE);

-- ---------------------------------------------------------------------
-- 8. Notifications (historique)
-- ---------------------------------------------------------------------
INSERT INTO notifications (utilisateur_id, titre, message, type, date_envoi, lue) VALUES
(4, 'Alerte médicamenteuse', 'Interaction détectée entre Advil et votre traitement (warfarine).', 'ALERTE',  '2026-10-18 19:40:06', FALSE),
(1, 'Nouvelle alerte patient', 'Une alerte de gravité élevée concerne Marc Lefèvre.',            'ALERTE',  '2026-10-18 19:40:06', FALSE),
(5, 'Rappel de prise', 'Il est 08:00 : prenez votre Glucophage 850 mg.',                         'RAPPEL',  '2026-10-30 08:00:01', TRUE),
(2, 'Nouveau partage', 'Yasmine Haddad a partagé ses données avec vous.',                        'PARTAGE', '2026-09-20 11:00:02', TRUE);

-- device_tokens : laissée vide (les tokens réels viendront du navigateur, étape 17)

-- Recale les compteurs AUTO_INCREMENT après les insertions avec id explicite
ALTER TABLE users       AUTO_INCREMENT = 6;
ALTER TABLE medicaments AUTO_INCREMENT = 13;
ALTER TABLE scans       AUTO_INCREMENT = 5;

-- Résumé
SELECT 'users' AS table_name, COUNT(*) AS lignes FROM users
UNION ALL SELECT 'patients',      COUNT(*) FROM patients
UNION ALL SELECT 'medecins',      COUNT(*) FROM medecins
UNION ALL SELECT 'anamneses',     COUNT(*) FROM anamneses
UNION ALL SELECT 'medicaments',   COUNT(*) FROM medicaments
UNION ALL SELECT 'scans',         COUNT(*) FROM scans
UNION ALL SELECT 'alertes',       COUNT(*) FROM alertes
UNION ALL SELECT 'rappels',       COUNT(*) FROM rappels
UNION ALL SELECT 'partages',      COUNT(*) FROM partages
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications
UNION ALL SELECT 'device_tokens', COUNT(*) FROM device_tokens;