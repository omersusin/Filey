#!/bin/bash
set -e
cd ~/Filey

# search-semantic modülü mlkit kullanmıyor, smart-tags kullanıyor.
# O satırı sil.
sed -i '/libs\.mlkit\.textRecognition/d' feature/search-semantic/build.gradle.kts
sed -i '/libs\.mlkit\.text.recognition/d' feature/search-semantic/build.gradle.kts

echo "=== Sonuç ==="
cat feature/search-semantic/build.gradle.kts

git add feature/search-semantic/build.gradle.kts
git commit -m "fix(search-semantic): remove mlkit dep, OCR is in smart-tags module"
git push origin feat/sprint1-hardening
echo "DONE"
