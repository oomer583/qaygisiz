# Downloads the public Azerbaijani SMS corpus used to measure the detector.
# Not redistributed here: it has no licence file, so it is fetched from its source
# and left untracked by git.
#   Shahbazov, V. (2026). SMS dataset for multi-class classification of ham, spam,
#   and smishing in Azerbaijani language. Problems of Information Technology,
#   17(1), 32-39. doi:10.25045/jpit.v17.i1.04
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$out  = Join-Path $root "corpus\dataset.csv"
New-Item -ItemType Directory -Force -Path (Split-Path $out) | Out-Null
$url = "https://raw.githubusercontent.com/vusalshahbaz/sms-classification-dataset-azerbaijan/main/dataset.csv"
Invoke-WebRequest -Uri $url -OutFile $out
$bytes = (Get-Item $out).Length
Write-Host "downloaded $out ($bytes bytes)"
if ($bytes -lt 100000) { Write-Host "that looks too small - check the download" -ForegroundColor Red }