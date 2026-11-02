# ============================================================
#  MediScan AI - Vérification de l'environnement de développement
#  Usage : clic droit > "Exécuter avec PowerShell"
#          ou : powershell -ExecutionPolicy Bypass -File .\verifier-environnement.ps1
# ============================================================

$outils = @(
    @{ Nom = "Java (JDK 21)";   Cmd = "java";      Args = "-version" },
    @{ Nom = "Compilateur Java"; Cmd = "javac";    Args = "-version" },
    @{ Nom = "Maven";           Cmd = "mvn";       Args = "-v" },
    @{ Nom = "Node.js";         Cmd = "node";      Args = "-v" },
    @{ Nom = "npm";             Cmd = "npm";       Args = "-v" },
    @{ Nom = "Angular CLI";     Cmd = "ng";        Args = "version" },
    @{ Nom = "MySQL";           Cmd = "mysql";     Args = "--version" },
    @{ Nom = "Tesseract OCR";   Cmd = "tesseract"; Args = "--version" },
    @{ Nom = "Git";             Cmd = "git";       Args = "--version" }
)

Write-Host "`n=== Vérification de l'environnement MediScan AI ===`n" -ForegroundColor Cyan
$erreurs = 0

foreach ($outil in $outils) {
    if (Get-Command $outil.Cmd -ErrorAction SilentlyContinue) {
        $sortie = & $outil.Cmd $outil.Args 2>&1 | Select-Object -First 1
        Write-Host ("[OK]   {0,-18} {1}" -f $outil.Nom, $sortie) -ForegroundColor Green
    } else {
        Write-Host ("[ECHEC] {0,-17} introuvable (vérifiez l'installation et le PATH)" -f $outil.Nom) -ForegroundColor Red
        $erreurs++
    }
}

Write-Host "`n--- Variables d'environnement ---" -ForegroundColor Cyan
foreach ($var in @("JAVA_HOME", "MAVEN_HOME", "TESSDATA_PREFIX")) {
    $valeur = [Environment]::GetEnvironmentVariable($var)
    if ($valeur) {
        Write-Host ("[OK]   {0,-16} = {1}" -f $var, $valeur) -ForegroundColor Green
    } else {
        Write-Host ("[ECHEC] {0,-15} non définie" -f $var) -ForegroundColor Red
        $erreurs++
    }
}

Write-Host "`n--- Langues Tesseract ---" -ForegroundColor Cyan
if (Get-Command tesseract -ErrorAction SilentlyContinue) {
    $langues = tesseract --list-langs 2>&1
    foreach ($l in @("fra", "eng")) {
        if ($langues -match "^$l$") {
            Write-Host "[OK]   langue '$l' installée" -ForegroundColor Green
        } else {
            Write-Host "[ECHEC] langue '$l' manquante" -ForegroundColor Red
            $erreurs++
        }
    }
}

Write-Host ""
if ($erreurs -eq 0) {
    Write-Host "Tout est prêt ! Vous pouvez passer à l'étape 2." -ForegroundColor Green
} else {
    Write-Host "$erreurs problème(s) détecté(s). Corrigez-les puis relancez le script." -ForegroundColor Yellow
}