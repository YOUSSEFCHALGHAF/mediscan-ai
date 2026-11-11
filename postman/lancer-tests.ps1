<#
  MediScan AI - Exécution automatique des tests de l'API avec Newman
  -----------------------------------------------------------------
  Usage (depuis C:\dev\mediscan-ai) :
     powershell -ExecutionPolicy Bypass -File .\postman\lancer-tests.ps1
     powershell -ExecutionPolicy Bypass -File .\postman\lancer-tests.ps1 -Collection Complements
     powershell -ExecutionPolicy Bypass -File .\postman\lancer-tests.ps1 -SansReinitialisation

  Étapes :
     1. vérifie que Newman est installé
     2. vérifie que le backend répond (http://localhost:8081)
     3. pour chaque collection : recharge les données de démonstration (sauf -SansReinitialisation),
        puis l'exécute et génère un rapport HTML dans postman\rapports\

  Collections :
     Principale  : MediScan-AI.postman_collection.json            (étapes 5 à 7 : authentification, API REST)
     Complements : MediScan-AI-complements.postman_collection.json (étapes 13 à 19 : scan, alertes, rappels,
                   notifications, tableau de bord médecin, partages) - nécessite Tesseract (OCR)
#>
param(
    [ValidateSet("Toutes", "Principale", "Complements")]
    [string]$Collection = "Toutes",
    [switch]$SansReinitialisation
)

$ErrorActionPreference = "Continue"
$dossierPostman  = $PSScriptRoot
$racine          = Split-Path -Parent $dossierPostman
$environnement   = Join-Path $dossierPostman "MediScan-Local.postman_environment.json"
$dossierRapports = Join-Path $dossierPostman "rapports"
New-Item -ItemType Directory -Force -Path $dossierRapports | Out-Null

Write-Host "`n=== MediScan AI - Tests de l'API REST ===`n" -ForegroundColor Cyan

# 1. Newman installé ?
if (-not (Get-Command newman -ErrorAction SilentlyContinue)) {
    Write-Host "Newman n'est pas installé. Lancez : npm install -g newman newman-reporter-htmlextra" -ForegroundColor Red
    exit 1
}

# 2. Backend démarré ?
try {
    $sante = Invoke-RestMethod -Uri "http://localhost:8081/api/public/health" -TimeoutSec 5
    Write-Host "[OK] Backend en ligne (base : $($sante.database))" -ForegroundColor Green
} catch {
    Write-Host "[ECHEC] Le backend ne répond pas sur http://localhost:8080." -ForegroundColor Red
    Write-Host "        Lancez d'abord : cd mediscan-ai-backend ; mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Recharge les données de démonstration (chaque collection part d'un état connu)
function Reinitialiser-Donnees {
    if ($SansReinitialisation) { return $true }
    $script = (Join-Path $racine "database\02_donnees_test.sql") -replace '\\', '/'
    Write-Host "Réinitialisation des données de démonstration (mot de passe de mediscan_user demandé)..." -ForegroundColor Cyan
    # | Out-Host : l'affichage d'un programme externe ne doit pas devenir la valeur de retour de la fonction
    mysql -u mediscan_user -p --default-character-set=utf8mb4 -e "source $script" | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ECHEC] Réinitialisation MySQL impossible." -ForegroundColor Red
        return $false
    }
    Write-Host "[OK] Données de démonstration rechargées" -ForegroundColor Green
    return $true
}

# Exécute une collection et produit son rapport HTML ; renvoie le code de sortie de Newman
function Lancer-Collection([string]$fichier, [string]$titre, [string]$nomRapport) {
    Write-Host "`n--- $titre ---" -ForegroundColor Cyan
    if (-not (Reinitialiser-Donnees)) { return 1 }
    $horodatage = Get-Date -Format "yyyy-MM-dd_HH-mm"
    $rapport = Join-Path $dossierRapports "$($nomRapport)_$horodatage.html"
    # --working-dir : les fichiers envoyés (photo de test) sont cherchés dans le dossier postman
    newman run (Join-Path $dossierPostman $fichier) -e $environnement --working-dir $dossierPostman `
        -r "cli,htmlextra" `
        --reporter-htmlextra-export $rapport `
        --reporter-htmlextra-title "MediScan AI - $titre" `
        --reporter-htmlextra-browserTitle "MediScan AI - Tests API" | Out-Host
    $code = $LASTEXITCODE
    $script:rapports += $rapport
    return $code
}

$script:rapports = @()
$echecs = 0
if ($Collection -in @("Toutes", "Principale")) {
    if ((Lancer-Collection "MediScan-AI.postman_collection.json" "API REST (étapes 5 à 7)" "rapport-api") -ne 0) { $echecs++ }
}
if ($Collection -in @("Toutes", "Complements")) {
    if ((Lancer-Collection "MediScan-AI-complements.postman_collection.json" "Fonctionnalités (étapes 13 à 19)" "rapport-complements") -ne 0) { $echecs++ }
}

Write-Host ""
if ($echecs -eq 0) {
    Write-Host "Tous les tests sont passés." -ForegroundColor Green
} else {
    Write-Host "$echecs collection(s) avec des tests en échec : consultez les rapports." -ForegroundColor Yellow
}
foreach ($r in $script:rapports) {
    Write-Host "Rapport : $r"
    if (Test-Path $r) { Start-Process $r }
}
exit $echecs
