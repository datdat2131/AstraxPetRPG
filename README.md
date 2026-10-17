# AstraxPetRPG 2.0.0

Plugin pet RPG cho **Paper 1.21.4 / Java 21**. Repository dùng đúng bố cục phẳng như mẫu: các file `.java`, `plugin.yml`, `config.yml`, `pom.xml` đều ở thư mục gốc.

## Build trên GitHub Actions
1. Upload toàn bộ nội dung ZIP này lên thư mục gốc repository, không upload thêm một thư mục bọc bên ngoài.
2. Vào **Actions** và chạy workflow **Build AstraxPetRPG**.
3. Khi workflow xanh, mở run thành công → **Artifacts** → `AstraxPetRPG-2.0.0`.
4. Đưa file JAR vào thư mục `plugins/` của server Paper 1.21.4 rồi khởi động lại server.

Build cục bộ: `mvn clean verify`; JAR dự kiến: `target/AstraxPetRPG-2.0.0.jar`.

## Các chức năng có trong source
- 31 mẫu pet, 7 độ hiếm, 6 hệ nguyên tố.
- Hồ sơ pet, EXP/cấp độ, bộ sưu tập, summon/dismiss, GUI và kỹ năng cơ bản.
- Gacha theo trọng số, pity Epic+, lưu SQLite.
- Các lệnh `/pet` và `/petadmin`.
- Workflow kiểm tra layout, test và xác nhận `plugin.yml` cùng class main trong JAR.

## Lưu ý về mức độ hoàn thiện
Đây là bản source nâng cấp, không phải JAR đã được kiểm thử trên server thật. Các phần trong `ROADMAP.md` như MySQL backend thực sự, tích hợp gameplay đầy đủ với MythicMobs/MythicLib, tiến hóa nhiều nhánh hoàn chỉnh, trang bị/nguyên liệu, nhiệm vụ ngày/tuần và leaderboard mở rộng chưa được xem là hoàn thành. `OptionalPluginHooks` hiện chỉ phát hiện plugin tùy chọn, không gọi API gameplay của chúng.

SQLite JDBC được đóng gói vì plugin cần driver; Paper API và plugin tùy chọn không được đóng gói vào JAR.
