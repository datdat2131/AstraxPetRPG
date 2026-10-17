# AstraxPetRPG 1.5.0 — Source

Plugin khung Pet RPG dành cho **Paper 1.21.4 / Java 21**.

## Tính năng hiện có (giai đoạn 1)
- Nạp cấu hình chung từ `config.yml` và danh sách pet từ `pets.yml`.
- Registry 30 pet mẫu, độ hiếm và nguyên tố.
- Khởi tạo SQLite bất đồng bộ, lưu bảng pet và bộ đếm pity.
- Lệnh `/pet`, `/pet info <pet_type>` và các lệnh khung để phát triển tiếp.
- Unit test cơ bản cho `PetRarity`.

## Chưa triển khai
GUI, triệu hồi thực tế, chiến đấu, tiến hóa, boss/quest và tích hợp sâu với MythicMobs/PlaceholderAPI chưa được triển khai ở giai đoạn này. Plugin chỉ kiểm tra sự hiện diện của các plugin tùy chọn.

## Build
Yêu cầu JDK 21 và Maven 3.9+.

```bash
mvn clean verify
```

JAR dự kiến nằm tại `target/AstraxPetRPG-1.5.0.jar`. Mã nguồn này chưa được xác nhận build thành công trong môi trường đóng gói hiện tại; hãy chạy lệnh trên để kiểm tra trước khi đưa lên server.

## Cài đặt
1. Build JAR rồi chép vào thư mục `plugins` của Paper 1.21.4.
2. Khởi động server. Paper sẽ tải thư viện SQLite được khai báo trong `plugin.yml`; server cần kết nối Internet ở lần khởi động cần tải thư viện.
3. Chỉnh `plugins/AstraxPetRPG/config.yml` và `pets.yml` theo nhu cầu.

Mặc định dùng SQLite trong thư mục dữ liệu plugin. Cấu hình MySQL mẫu có trong `config.yml` nhưng adapter MySQL chưa được triển khai.
