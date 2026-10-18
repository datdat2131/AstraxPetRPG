#!/bin/sh
# Kiểm tra nội dung JAR sau khi build:  sh tools/verify-jar.sh target/AstraxPetRPG-2.0.0.jar
JAR="${1:-target/AstraxPetRPG-2.0.0.jar}"
[ -f "$JAR" ] || { echo "FAIL: không thấy $JAR (chạy: mvn clean package)"; exit 1; }
LIST=$(unzip -Z1 "$JAR") || exit 1
fail=0
need() { echo "$LIST" | grep -qx "$1" && echo "OK    $1" || { echo "FAIL  thiếu $1"; fail=1; }; }
need "plugin.yml"                                  # phải nằm ở ROOT của jar
need "config.yml"; need "pets.yml"; need "skills.yml"; need "eggs.yml"; need "equipment.yml"
need "quests.yml"; need "achievements.yml"; need "dungeons.yml"
MAIN=$(unzip -p "$JAR" plugin.yml | sed -n 's/^main:[[:space:]]*//p' | tr -d "\r'\"")
echo "main trong plugin.yml: $MAIN"
need "$(echo "$MAIN" | tr . /).class"                # class main phải tồn tại
for m in pet rarity egg evolution combat skills equipment mythicmobs mythiclib storage gui commands config; do
  echo "$LIST" | grep -q "^vn/soulpets/$m/.*\.class$" && echo "OK    module $m" || { echo "FAIL  module $m không có class"; fail=1; }
done
echo "$LIST" | grep -q "^src/\|\.java$" && { echo "WARN  jar chứa file nguồn"; }
[ $fail -eq 0 ] && echo "=> JAR HỢP LỆ" || { echo "=> JAR LỖI"; exit 1; }
