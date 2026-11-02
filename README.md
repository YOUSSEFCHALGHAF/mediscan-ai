# MediScan AI — Application web de santé intelligente

Application web avec une interface **Patient** et une interface **Médecin** :
collecte de l'anamnèse, scan de médicaments par IA/OCR (Tesseract, local et gratuit),
alertes d'interactions, rappels via Firebase Cloud Messaging (Web Push).

## Stack technique
| Couche | Technologie |
|---|---|
| Frontend | Angular (TypeScript) + Angular Material |
| Backend | Spring Boot 3 (Java 21) — JWT, BCrypt |
| Base de données | MySQL 8.4 |
| IA / OCR | Tesseract 5 via Tess4J |
| Notifications | Firebase Cloud Messaging (Web Push uniquement) |
| Tests API | Postman |

## Structure
- `mediscan-ai-backend/` — API REST Spring Boot
- `mediscan-ai-frontend/` — application Angular
- `database/` — scripts SQL de création
- `postman/` — collection Postman
- `documentation/` — diagrammes UML, captures, guides

## Prérequis
JDK 21, Maven 3.9, Node.js 24 LTS, Angular CLI, MySQL 8.4, Tesseract 5 (langues fra + eng).
Vérification : `powershell -ExecutionPolicy Bypass -File documentation/verifier-environnement.ps1`

## Lancement
*(complété aux étapes suivantes)*