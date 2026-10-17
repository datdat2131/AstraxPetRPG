# AstraxPetRPG 1.4.0 — source preview

Plugin companion RPG cho **Paper 1.21.4 / Java 21**. Đây là gói source đã chỉnh sửa; do môi trường hiện tại không có Maven và không truy cập được Maven repository, **chưa thể xác nhận biên dịch hoặc tạo JAR trong phiên này**. GitHub Actions được cấu hình để chạy `mvn clean verify` trên JDK 21 và chỉ upload JAR nếu build/test thành công.

## Đã bổ sung trong source

- 30 cấu hình pet trong `src/main/resources/pets.yml`, có rarity Common → Ancient, 6 hệ nguyên tố, role tank/healer/DPS/support và chỉ số mẫu cấu hình được.
- Các lệnh `/pet stats`, `/pet egg`, `/pet evolve <guardian|assault|arcane>`, `/pet equip`, `/pet dex`, `/pet select` bên cạnh menu/summon/dismiss/skill/rename.
- Trứng có trọng số rarity trong `config.yml`, pity theo người chơi được lưu trong database, hiệu ứng nhận pet hiếm và PetDex lưu số lượng pet đã nhận theo loại.
- Level/XP, điểm kỹ năng, sao, nhánh tiến hóa, trang bị và chỉ số dẫn xuất; chi phí tiến hóa/trang bị có thể chỉnh trong config.
- SQLite mặc định có WAL; MySQL tùy chọn qua `database.*`; câu lệnh SQL dùng prepared statements và có migration cho các cột mới.
- Tấn công tự động giới hạn theo khoảng cách và cooldown; hook MythicMobs dùng reflection và fallback skill mặc định khi API không tương thích.
- PlaceholderAPI được khai báo `provided`, hook chỉ được nạp nếu plugin hiện diện; placeholders: `%astraxpet_name%`, `%astraxpet_type%`, `%astraxpet_level%`, `%astraxpet_rarity%`, `%astraxpet_element%`, `%astraxpet_hp%`, `%astraxpet_atk%`, `%astraxpet_def%`, `%astraxpet_speed%`, `%astraxpet_stars%`, `%astraxpet_evolution%`, `%astraxpet_skill_points%`.
- GitHub Actions chạy JDK 21, `mvn clean verify`, kiểm tra `plugin.yml` ở root và main class, rồi upload artifact.

## Build

```bash
java -version   # phải là JDK 21
mvn --batch-mode clean verify
```

Artifact mong đợi: `target/AstraxPetRPG-1.4.0.jar`. Đừng cài bản này lên server production trước khi workflow thành công và kiểm tra trên server staging.

## Cài đặt

1. Chạy workflow **Build AstraxPetRPG** trên GitHub.
2. Tải artifact `AstraxPetRPG-1.4.0` từ run thành công.
3. Sao lưu thư mục plugin/database hiện tại.
4. Cài JAR vào `plugins/`, khởi động server một lần để tạo config.
5. Kiểm tra console và chạy `/pet stats`, `/pet dex`.

## Lệnh

- `/pet` hoặc `/pet menu` — mở menu.
- `/pet summon`, `/pet dismiss`, `/pet info`, `/pet stats`, `/pet skill`.
- `/pet egg` — ấp một pet theo trọng số config; mặc định không yêu cầu item, bật `eggs.require-item` để yêu cầu vật liệu.
- `/pet evolve guardian|assault|arcane` — chọn nhánh ở lần tiến hóa đầu; cần cấp 20/40/60 và nguyên liệu.
- `/pet equip power_core|guardian_charm|none` — trang bị hoặc tháo trang bị.
- `/pet dex`, `/pet select <pet_type>`, `/pet rename <tên>`.
- `/petadmin give <player> <pet_type>`, `/petadmin xp <player> <amount>`, `/petadmin reload`.

## Cấu hình database

SQLite là mặc định. Để dùng MySQL, đổi `database.type: mysql` và cấu hình host/port/database/username/password. Dùng tài khoản DB riêng với quyền tối thiểu và không chia sẻ mật khẩu trong log hoặc repository. Hãy sao lưu DB trước khi đổi backend; không có tự động di chuyển dữ liệu SQLite sang MySQL.

## MythicMobs / MythicLib

- **MythicMobs:** đặt tên skill hợp lệ trong `mythicmobs.skills.<pet_type>` hoặc `mythicmobs.default-skill`. Ví dụ config plugin:

```yaml
mythicmobs:
  skills:
    wolf: AstraxWolfPulse
```

  Ví dụ skill file MythicMobs (đặt theo cấu trúc được phiên bản MythicMobs của server hỗ trợ):

```yaml
AstraxWolfPulse:
  Skills:
  - effect:particles{particle=crit;amount=12;hS=0.3;vS=0.3} @self
  - damage{amount=5} @EntitiesInRadius{r=4;type=MONSTER}
```

  Cú pháp skill phải được kiểm tra theo phiên bản MythicMobs cài thực tế. Hook hiện dò `castSkill(Entity,String)` bằng reflection và tự fallback nếu không tìm được API phù hợp.
- **MythicLib:** hiện chỉ phát hiện plugin và ghi log; **chưa áp dụng stat/damage modifier qua MythicLib**, vì API tương thích chưa được xác minh. Chỉ bật tích hợp thật sau khi kiểm tra đúng phiên bản thư viện trên server.
- **PlaceholderAPI:** nếu cài plugin này, expansion `astraxpet` được đăng ký tự động. Nếu thiếu PlaceholderAPI, plugin vẫn chạy.

## Debug

- Lỗi `plugin.yml` không tìm thấy: tải đúng artifact workflow mới, kiểm tra `jar tf AstraxPetRPG-1.4.0.jar` phải có `plugin.yml` ở root và `vn/soulpets/SoulPetsPlugin.class`.
- `UnsupportedClassVersionError`: dùng Java 21 cho server.
- Lỗi database: kiểm tra quyền ghi thư mục plugin hoặc thông tin MySQL, sau đó xem log `AstraxPetRPG`.
- Hook MythicMobs không dùng được: kiểm tra skill ID, phiên bản plugin và log fallback; plugin không yêu cầu MythicMobs để khởi động.
- Trước khi báo lỗi, gửi đoạn log bắt đầu từ `Caused by:` cùng phiên bản Paper, Java và các plugin liên quan; không gửi mật khẩu database.

## Phạm vi chưa hoàn tất / cần xác minh

Đây **chưa phải bản hoàn chỉnh của toàn bộ checklist MMO**. Boss/dungeon, quest ngày/tuần, achievement, leaderboard và hệ kỹ năng chủ động/bị động đa tầng chưa được triển khai đầy đủ. GUI hiện là menu cơ bản, chưa có pagination/confirmation flow hoàn chỉnh. Lưu trữ JDBC hiện gọi đồng bộ từ các lệnh/event nên cần profiling và chuyển sang repository async trước khi dùng trên server đông người. Chưa chạy được Maven build hoặc test server trong môi trường này; không khẳng định các mục đó đã pass. MythicLib stat integration còn tắt cho đến khi API được xác minh.
