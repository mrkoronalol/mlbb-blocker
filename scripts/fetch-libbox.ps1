$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $root 'app\libs'
$dest = Join-Path $libs 'libbox.aar'
$url = 'https://github.com/singbox-android/libbox/releases/download/1.14.3/libbox.aar'
$expected = '6F1F295B1D988D4875546A48CB90A90E2B5F70CA10432F1A8D95E757A76F01D9'

New-Item -ItemType Directory -Force -Path $libs | Out-Null

if (-not (Test-Path -LiteralPath $dest)) {
    Write-Host "Downloading $url"
    Invoke-WebRequest -Uri $url -OutFile $dest
}

$hash = (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash
if ($hash -ne $expected) {
    throw "libbox.aar SHA256 mismatch: $hash"
}

Write-Host "libbox.aar is ready: $dest"
