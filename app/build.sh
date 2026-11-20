#!/bin/bash
set -e
export PATH=/usr/lib/jvm/java-17-openjdk-arm64/bin:$PATH
cd /root/workspace/dsh-voice
AJ=/root/workspace/android30.jar
R8=/root/workspace/r8.jar
OUT=build
rm -rf $OUT && mkdir -p $OUT/classes $OUT/dex

echo "[1/5] javac"
javac --release 8 -Xlint:-options -cp $AJ -d $OUT/classes $(find src -name '*.java')
jar cf $OUT/classes.jar -C $OUT/classes .

echo "[2/5] d8"
java -cp $R8 com.android.tools.r8.D8 --release --min-api 26 --lib $AJ --output $OUT/dex $OUT/classes.jar

echo "[3/5] aapt"
aapt package -f -M AndroidManifest.xml -S res -I $AJ -F $OUT/base.apk

echo "[4/5] merge + align"
python3 - <<'PY'
import zipfile, shutil
shutil.copyfile('build/base.apk','build/unsigned.apk')
with zipfile.ZipFile('build/unsigned.apk','a') as z:
    z.write('build/dex/classes.dex','classes.dex',zipfile.ZIP_DEFLATED)
PY
zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk

echo "[5/5] sign"
[ -f dsh.keystore ] || keytool -genkeypair -keystore dsh.keystore -alias dsh -keyalg RSA \
  -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=DSH, OU=App" >/dev/null 2>&1
apksigner sign --ks dsh.keystore --ks-pass pass:android --key-pass pass:android \
  --out dsh-voice.apk $OUT/aligned.apk
echo "OK -> $(pwd)/dsh-voice.apk"
