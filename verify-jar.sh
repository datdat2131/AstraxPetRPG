#!/bin/sh
# Kiểm tra nội dung JAR sau khi build:  sh tools/verify-jar.sh target/AstraxPetRPG-2.0.0.jar
JAR="${1:-$(ls target/AstraxPetRPG-*.jar 2>/dev/null | head -n1)}"
[ -f "$JAR" ] || { echo "FAIL: không thấy $JAR (chạy: mvn clean package)"; exit 1; }

echo "JAR: $JAR"
unzip -Z1 "$JAR" | grep -qx "plugin.yml" || { echo "plugin.yml không nằm ở root JAR"; exit 1; }
MAIN=$(unzip -p "$JAR" plugin.yml | sed -n 's/^main:[[:space:]]*//p' | tr -d '\r')
[ -n "$MAIN" ] || { echo "plugin.yml thiếu main"; exit 1; }
unzip -Z1 "$JAR" | grep -qx "$(echo "$MAIN" | tr . /).class" || { echo "Không có class main $MAIN"; exit 1; }
for k in version api-version commands permissions; do
  unzip -p "$JAR" plugin.yml | grep -q "^$k:" || { echo "plugin.yml thiếu $k"; exit 1; }
done

echo "OK: main=$MAIN"
