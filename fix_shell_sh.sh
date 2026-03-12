#!/bin/bash
set -e
cd ~/Filey

FILE="core/src/main/kotlin/filey/app/core/security/sandbox/SandboxedCommandExecutor.kt"

if [ ! -f "$FILE" ]; then
  echo "Dosya bulunamadı: $FILE"
  # Tüm olası konumları tara
  find . -name "SandboxedCommandExecutor.kt" 2>/dev/null
  exit 1
fi

echo "=== Önce ==="
grep -n "Shell\." "$FILE"

# Shell.sh() → Shell.cmd() — libsu API'sinde sh() yok
sed -i 's/Shell\.sh(/Shell.cmd(/g' "$FILE"

echo ""
echo "=== Sonra ==="
grep -n "Shell\." "$FILE"

git add "$FILE"
git commit -m "fix(core): replace Shell.sh() with Shell.cmd() — libsu has no sh() method"
git push origin feat/sprint1-hardening
echo "DONE"
