# Build script for oxdm Windows Setup Installer
[CmdletBinding()]
param(
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

function Step($m) { Write-Host "`n==> $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "✓ $m" -ForegroundColor Green }
function Info($m) { Write-Host "    $m" -ForegroundColor DarkGray }
function Warn($m) { Write-Host "! $m" -ForegroundColor Yellow }

$rootDir = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $rootDir

Step "Checking compiler and tools"
$env:PATH = "$HOME\.cargo\bin;$env:PATH"

$isccCandidates = @(
    (Get-Command iscc.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source),
    "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
    "C:\Program Files\Inno Setup 6\ISCC.exe",
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
) | Where-Object { $_ -and (Test-Path $_) }

$iscc = ($isccCandidates | Select-Object -First 1)
if (-not $iscc) {
    throw "Inno Setup compiler (ISCC.exe) not found. Please install Inno Setup 6."
}
Ok "Using Inno Setup compiler: $iscc"

$oxdmExe = Join-Path $rootDir "target\release\oxdm.exe"
$hostExe = Join-Path $rootDir "target\release\oxdm-native-host.exe"

if ($Rebuild -or (-not (Test-Path $oxdmExe)) -or (-not (Test-Path $hostExe))) {
    Step "Building release binaries with cargo"
    cargo build --release --bins
    if ($LASTEXITCODE -ne 0) { throw "cargo build failed with exit code $LASTEXITCODE" }
    Ok "Compiled target/release/oxdm.exe and target/release/oxdm-native-host.exe"
} else {
    Ok "Using existing release binaries in target/release"
}

Step "Compiling Inno Setup package"
$issPath = Join-Path $rootDir "tools\windows\oxdm.iss"
& $iscc $issPath
if ($LASTEXITCODE -ne 0) { throw "ISCC compilation failed with exit code $LASTEXITCODE" }

$distDir = Join-Path $rootDir "dist"
$setupExe = Join-Path $distDir "oxdm-setup.exe"

if (Test-Path $setupExe) {
    $size = (Get-Item $setupExe).Length / 1MB
    $hash = (Get-FileHash -Path $setupExe -Algorithm SHA256).Hash
    Ok "Successfully generated installer: $setupExe"
    Info ("File size: {0:N2} MB" -f $size)
    Info "SHA256: $hash"
} else {
    throw "Setup executable not found at $setupExe"
}
