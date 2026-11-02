-- =====================================================================
--  MediScan AI - 01 : schéma complet de la base mediscan_db
--  11 tables · clés étrangères · index · contraintes CHECK
--  ATTENTION : ce script SUPPRIME puis RECRÉE toutes les tables
--              (toutes les données sont perdues). Usage : développement.
-- =====================================================================

SET NAMES utf8mb4;
USE mediscan_db;

-- ---------------------------------------------------------------------
-- 0. Suppression des tables existantes (ordre inverse des dépendances)
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS device_tokens;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS partages;
DROP TABLE IF EXISTS rappels;
DROP TABLE IF EXISTS alertes;
DROP TABLE IF EXISTS scans;
DROP TABLE IF EXISTS medicaments;
DROP TABLE IF EXISTS anamneses;
DROP TABLE IF EXISTS medecins;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. users : table mère de Patient et Medecin (héritage JPA JOINED)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    nom                VARCHAR(100)  NOT NULL,
    prenom             VARCHAR(100)  NOT NULL,
    email              VARCHAR(150)  NOT NULL,
    mot_de_passe_hash  VARCHAR(255)  NOT NULL,
    role               ENUM('PATIENT','MEDECIN') NOT NULL,
    actif              BOOLEAN       NOT NULL DEFAULT TRUE,
    date_creation      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users        PRIMARY KEY (id),
    CONSTRAINT uk_users_email  UNIQUE (email),
    CONSTRAINT chk_users_email CHECK (email LIKE '%_@_%._%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. patients : données spécifiques au patient (id = id de users)
-- ---------------------------------------------------------------------
CREATE TABLE patients (
    id              BIGINT       NOT NULL,
    date_naissance  DATE         NULL,
    sexe            VARCHAR(10)  NULL,
    groupe_sanguin  VARCHAR(3)   NULL,
    telephone       VARCHAR(20)  NULL,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT fk_patients_users FOREIGN KEY (id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_patients_sexe
        CHECK (sexe IS NULL OR sexe IN ('HOMME','FEMME','AUTRE')),
    CONSTRAINT chk_patients_groupe
        CHECK (groupe_sanguin IS NULL OR groupe_sanguin IN
               ('A+','A-','B+','B-','AB+','AB-','O+','O-'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. medecins : données spécifiques au médecin (id = id de users)
-- ---------------------------------------------------------------------
CREATE TABLE medecins (
    id           BIGINT        NOT NULL,
    numero_rpps  VARCHAR(20)   NOT NULL,
    specialite   VARCHAR(100)  NOT NULL,
    telephone    VARCHAR(20)   NULL,
    CONSTRAINT pk_medecins      PRIMARY KEY (id),
    CONSTRAINT uk_medecins_rpps UNIQUE (numero_rpps),
    CONSTRAINT fk_medecins_users FOREIGN KEY (id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. anamneses : une seule anamnèse par patient (relation 1-1)
--    Listes saisies séparées par des virgules (exploitées par l'IA)
-- ---------------------------------------------------------------------
CREATE TABLE anamneses (
    id                      BIGINT    NOT NULL AUTO_INCREMENT,
    patient_id              BIGINT    NOT NULL,
    antecedents             TEXT      NULL,
    allergies               TEXT      NULL,
    pathologies_chroniques  TEXT      NULL,
    traitements_en_cours    TEXT      NULL,
    habitudes_vie           TEXT      NULL,
    date_maj                DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_anamneses         PRIMARY KEY (id),
    CONSTRAINT uk_anamneses_patient UNIQUE (patient_id),
    CONSTRAINT fk_anamneses_patients FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. medicaments : base médicamenteuse de référence (simulée)
--    "interactions" = liste de DCI interagissant, séparées par des virgules
-- ---------------------------------------------------------------------
CREATE TABLE medicaments (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    nom_commercial      VARCHAR(150)  NOT NULL,
    dci                 VARCHAR(150)  NOT NULL,
    dosage              VARCHAR(50)   NULL,
    forme_galenique     VARCHAR(50)   NULL,
    posologie           VARCHAR(255)  NULL,
    contre_indications  TEXT          NULL,
    interactions        TEXT          NULL,
    laboratoire         VARCHAR(100)  NULL,
    CONSTRAINT pk_medicaments          PRIMARY KEY (id),
    CONSTRAINT uk_medicaments_nom_dose UNIQUE (nom_commercial, dosage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_medicaments_dci ON medicaments (dci);
CREATE INDEX idx_medicaments_nom ON medicaments (nom_commercial);

-- ---------------------------------------------------------------------
-- 6. scans : photos de médicaments analysées par l'OCR
-- ---------------------------------------------------------------------
CREATE TABLE scans (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    patient_id       BIGINT        NOT NULL,
    medicament_id    BIGINT        NULL,
    image_url        VARCHAR(255)  NOT NULL,
    date_scan        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    texte_extrait    TEXT          NULL,
    numero_lot       VARCHAR(50)   NULL,
    date_expiration  DATE          NULL,
    score_confiance  DOUBLE        NULL,
    statut           ENUM('EN_COURS','TERMINE','A_VERIFIER','ECHEC')
                                   NOT NULL DEFAULT 'EN_COURS',
    CONSTRAINT pk_scans PRIMARY KEY (id),
    CONSTRAINT fk_scans_patients FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_scans_medicaments FOREIGN KEY (medicament_id)
        REFERENCES medicaments (id) ON DELETE SET NULL,
    CONSTRAINT chk_scans_score
        CHECK (score_confiance IS NULL OR (score_confiance >= 0 AND score_confiance <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_scans_patient_date ON scans (patient_id, date_scan);

-- ---------------------------------------------------------------------
-- 7. alertes : interactions, contre-indications, allergies, expiration
-- ---------------------------------------------------------------------
CREATE TABLE alertes (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    patient_id       BIGINT        NOT NULL,
    scan_id          BIGINT        NULL,
    medicament_id    BIGINT        NULL,
    type             ENUM('INTERACTION','CONTRE_INDICATION','ALLERGIE','EXPIRATION') NOT NULL,
    niveau_gravite   ENUM('FAIBLE','MOYEN','ELEVE') NOT NULL,
    message          VARCHAR(500)  NOT NULL,
    date_generation  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    traitee          BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_alertes PRIMARY KEY (id),
    CONSTRAINT fk_alertes_patients FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_alertes_scans FOREIGN KEY (scan_id)
        REFERENCES scans (id) ON DELETE SET NULL,
    CONSTRAINT fk_alertes_medicaments FOREIGN KEY (medicament_id)
        REFERENCES medicaments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_alertes_patient_traitee ON alertes (patient_id, traitee);

-- ---------------------------------------------------------------------
-- 8. rappels : rappels de prise médicamenteuse (envoyés par FCM)
-- ---------------------------------------------------------------------
CREATE TABLE rappels (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    patient_id              BIGINT        NOT NULL,
    medicament_id           BIGINT        NOT NULL,
    libelle                 VARCHAR(150)  NOT NULL,
    heure_prise             TIME          NOT NULL,
    frequence               VARCHAR(20)   NOT NULL DEFAULT 'QUOTIDIEN',
    date_debut              DATE          NOT NULL,
    date_fin                DATE          NULL,
    actif                   BOOLEAN       NOT NULL DEFAULT TRUE,
    derniere_notification   DATETIME      NULL,
    CONSTRAINT pk_rappels PRIMARY KEY (id),
    CONSTRAINT fk_rappels_patients FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_rappels_medicaments FOREIGN KEY (medicament_id)
        REFERENCES medicaments (id) ON DELETE RESTRICT,
    CONSTRAINT chk_rappels_frequence
        CHECK (frequence IN ('QUOTIDIEN','HEBDOMADAIRE','PONCTUEL')),
    CONSTRAINT chk_rappels_dates
        CHECK (date_fin IS NULL OR date_fin >= date_debut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_rappels_actif_heure ON rappels (actif, heure_prise);

-- ---------------------------------------------------------------------
-- 9. partages : partage patient -> médecin = CONSENTEMENT explicite
--    Une révocation ne supprime pas la ligne (traçabilité) : actif = FALSE
-- ---------------------------------------------------------------------
CREATE TABLE partages (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    patient_id         BIGINT        NOT NULL,
    medecin_id         BIGINT        NOT NULL,
    perimetre_donnees  VARCHAR(255)  NOT NULL,
    date_consentement  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_revocation    DATETIME      NULL,
    actif              BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_partages PRIMARY KEY (id),
    CONSTRAINT fk_partages_patients FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_partages_medecins FOREIGN KEY (medecin_id)
        REFERENCES medecins (id) ON DELETE CASCADE,
    CONSTRAINT chk_partages_revocation
        CHECK (date_revocation IS NULL OR date_revocation >= date_consentement),
    CONSTRAINT chk_partages_coherence
        CHECK ((actif = TRUE AND date_revocation IS NULL)
            OR (actif = FALSE AND date_revocation IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_partages_medecin_actif ON partages (medecin_id, actif);
CREATE INDEX idx_partages_patient_actif ON partages (patient_id, actif);

-- ---------------------------------------------------------------------
-- 10. notifications : historique des notifications envoyées
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    utilisateur_id  BIGINT        NOT NULL,
    titre           VARCHAR(150)  NOT NULL,
    message         VARCHAR(500)  NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    date_envoi      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lue             BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_users FOREIGN KEY (utilisateur_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user_lue ON notifications (utilisateur_id, lue);

-- ---------------------------------------------------------------------
-- 11. device_tokens : tokens FCM des navigateurs (Web Push)
-- ---------------------------------------------------------------------
CREATE TABLE device_tokens (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    utilisateur_id       BIGINT        NOT NULL,
    token                VARCHAR(512)  NOT NULL,
    navigateur           VARCHAR(100)  NULL,
    date_enregistrement  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_device_tokens       PRIMARY KEY (id),
    CONSTRAINT uk_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_users FOREIGN KEY (utilisateur_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Vérification
-- ---------------------------------------------------------------------
SHOW TABLES;