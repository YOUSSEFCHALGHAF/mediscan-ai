# Lance la collection Postman de MediScan AI avec Newman
$dossier = $PSScriptRoot

# 1. Verifier que Newman est installe
if (-not (Get-Command newman -ErrorAction SilentlyContinue)) {
    Write-Host "Installation de Newman..." -ForegroundColor Yellow
    npm install -g newman
}

# 2. Verifier que le backend repond
try {
    Invoke-RestMethod -Uri "http://localhost:8081/api/public/health" -TimeoutSec 5 | Out-Null
    Write-Host "[OK] Backend demarre sur le port 8081" -ForegroundColor Green
} catch {
    Write-Host "[ECHEC] Le backend ne repond pas sur http://localhost:8081. Demarrez-le d'abord." -ForegroundColor Red
    exit 1
}

# 3. Trouver la collection exportee
$collection = Get-ChildItem $dossier -Filter "*.json" | Where-Object { $_.Name -notlike "*environment*" } | Select-Object -First 1
if (-not $collection) {
    Write-Host "[ECHEC] Aucune collection .json dans $dossier. Exportez-la depuis Postman." -ForegroundColor Red
    exit 1
}
Write-Host "Collection : $($collection.Name)"

# 4. Lancer les tests (avec l'environnement s'il existe)
$env = Get-ChildItem $dossier -Filter "*environment*.json" | Select-Object -First 1
if ($env) {
    newman run $collection.FullName -e $env.FullName
} else {
    newman run $collection.FullName
}
exit $LASTEXITCODE
