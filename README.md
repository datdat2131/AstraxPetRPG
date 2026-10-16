# Astrax Pet RPG 1.2.0 — source project

Plugin pet RPG cho Paper 1.21.4 / Java 21. Dự án này mở rộng nền SoulPetsRPG có sẵn; chưa phải bản có đầy đủ trứng/độ hiếm, tiến hóa, trang bị, boss hoặc nhiệm vụ.

## Pet hiện có (16 loài)
- Wolf — Sói Linh: sát thương.
- Cat — Miêu Linh: tăng tốc và nhảy.
- Fox — Hồ Ly: sát thương và đốt mục tiêu.
- Allay — Tinh Linh: hồi máu.
- Axolotl — Thủy Linh: hồi máu/hồi phục.
- Bee — Phong Ong: sát thương, đốt/làm chậm.
- Camel — Lạc Đà Sa Mạc: tăng chống chịu.
- Goat — Sơn Dương: đòn tấn công mạnh.
- Llama — Linh Lạc Đà: đòn tấn công.
- Parrot — Thần Điểu: tăng tốc và nhảy.
- Rabbit — Nguyệt Thố: tăng tốc và nhảy.
- Turtle — Huyền Quy: tăng chống chịu.
- Panda — Trúc Linh: sát thương.
- Polar Bear — Băng Hùng: sát thương và làm chậm.
- Iron Golem — Thiết Vệ: đòn tấn công mạnh.
- Snow Golem — Tuyết Linh: sát thương và làm chậm.

## Build
Cần JDK 21 và Maven:
```bash
mvn clean package
```
JAR output: `target/AstraxPetRPG-1.2.0.jar`

## Commands
- `/pet` hoặc `/pet menu` — mở GUI cơ bản
- `/pet summon` — triệu hồi pet đã chọn
- `/pet dismiss` — thu hồi pet
- `/pet info` — xem cấp, XP, kỹ năng
- `/pet skill` — dùng kỹ năng pet
- `/pet rename <tên>` — đổi tên
- `/petadmin give <player> <pet_type>` — admin đổi loại pet
- `/petadmin xp <player> <amount>` — admin cộng XP
- `/petadmin reload` — tải lại config

## Permissions
- `soulpets.use` — mặc định true
- `soulpets.admin` — mặc định op

## Dữ liệu
SQLite: `plugins/AstraxPetRPG/pets.db`; cấu hình: `plugins/AstraxPetRPG/config.yml`.

## Lưu ý kiểm thử
Mã nguồn này nhắm tới Paper 1.21.4 và Java 21, nhưng môi trường tạo file hiện tại không có Maven/Paper API để build và chạy thử thật. Hãy build bằng Maven trên máy có kết nối repository, rồi thử trên server staging trước khi dùng production. Trứng/độ hiếm, tiến hóa/nâng sao, trang bị, boss và nhiệm vụ chưa được triển khai trong bản này.

## Build nhanh trên Windows
Cài JDK 21 và Apache Maven, đảm bảo lệnh `java` và `mvn` chạy được trong Command Prompt. Nhấp đúp `build-windows.bat`. Nếu build thành công, JAR sẽ ở `target/AstraxPetRPG-1.2.0.jar`.

Trên Linux/macOS chạy `./build-linux-macos.sh` sau khi cài JDK 21 và Maven. Cả hai cách cần kết nối Internet để Maven tải Paper API và thư viện SQLite.
