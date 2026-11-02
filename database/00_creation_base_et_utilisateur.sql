-- =====================================================================
--  MediScan AI - 00 : création de la base et de l'utilisateur dédié
--  À exécuter UNE SEULE FOIS, connecté en tant que root.
-- =====================================================================

-- 1. Base de données (utf8mb4 = accents + tous les caractères)
CREATE DATABASE IF NOT EXISTS mediscan_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Utilisateur dédié à l'application (jamais root dans le code !)
--    Remplacez le mot de passe par le vôtre (et reportez-le dans application-local.properties)
CREATE USER IF NOT EXISTS 'mediscan_user'@'localhost' IDENTIFIED BY 'MediScan#Dev2026';
CREATE USER IF NOT EXISTS 'mediscan_user'@'127.0.0.1' IDENTIFIED BY 'MediScan#Dev2026';

-- 3. Droits limités à la seule base mediscan_db
--    (développement : droits de structure inclus pour exécuter les scripts)
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, DROP, INDEX, REFERENCES
    ON mediscan_db.* TO 'mediscan_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, DROP, INDEX, REFERENCES
    ON mediscan_db.* TO 'mediscan_user'@'127.0.0.1';

FLUSH PRIVILEGES;

-- 4. Vérification
SHOW GRANTS FOR 'mediscan_user'@'localhost';