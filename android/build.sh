#!/bin/bash
# 그림책 미술키트 APK 만들기: ./build.sh 버전코드 버전이름
set -e
cd "$(dirname "$0")"
VC=${1:-1}; VN=${2:-1.0}
rm -rf out && mkdir -p out/flat out/classes
./aapt2 compile --dir proj/res -o out/flat/res.zip
./aapt2 link -I android-34.jar --manifest proj/AndroidManifest.xml -A proj/assets out/flat/res.zip \
  --min-sdk-version 24 --target-sdk-version 34 --version-code $VC --version-name $VN \
  --java out/gen -o out/base.apk
javac -nowarn -source 8 -target 8 -bootclasspath android-34.jar -d out/classes \
  $(find proj/src out/gen -name '*.java') 2>&1 | grep -v 'warning' || true
java -cp dx.jar com.android.dx.command.Main --dex --min-sdk-version=24 --output=out/classes.dex out/classes
(cd out && cp base.apk unsigned.apk && zip -q -j unsigned.apk classes.dex)
python3 tools/align.py out/unsigned.apk out/aligned.apk
javac -nowarn -cp apksig.jar -d out/tools tools/Sign.java
java --add-exports java.base/sun.security.x509=ALL-UNNAMED --add-exports java.base/sun.security.pkcs=ALL-UNNAMED --add-exports java.base/sun.security.util=ALL-UNNAMED --add-opens java.base/sun.security.x509=ALL-UNNAMED --add-opens java.base/sun.security.pkcs=ALL-UNNAMED --add-opens java.base/sun.security.util=ALL-UNNAMED -cp apksig.jar:out/tools Sign out/aligned.apk out/artkit-$VN.apk "$KEYSTORE" "$KEYPASS" artkit
python3 tools/check_align.py out/artkit-$VN.apk
ls -la out/artkit-$VN.apk
