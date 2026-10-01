#!/usr/bin/env bash
# Downloads the public Azerbaijani SMS corpus used to measure the detector.
# Not redistributed here: it has no licence file, so it is fetched from its source
# and left untracked by git.
#   Shahbazov, V. (2026). SMS dataset for multi-class classification of ham, spam,
#   and smishing in Azerbaijani language. Problems of Information Technology,
#   17(1), 32-39. doi:10.25045/jpit.v17.i1.04
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$root/corpus"
curl -fsSL -o "$root/corpus/dataset.csv" \
  "https://raw.githubusercontent.com/vusalshahbaz/sms-classification-dataset-azerbaijan/main/dataset.csv"
echo "downloaded $root/corpus/dataset.csv ($(wc -c < "$root/corpus/dataset.csv") bytes)"