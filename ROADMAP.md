# Roadmap kỹ thuật

Bản 2.0.0 hiện tập trung nâng cấp cấu trúc, catalog, rarity/element, pity persistence và CI. Các phần sau là công việc tiếp theo, chưa được đánh dấu hoàn thành:

1. Tách `SoulPetsPlugin` thành `pet`, `egg`, `evolution`, `combat`, `skills`, `equipment`, `storage`, `gui`, `commands`.
2. Thiết kế schema versioned cho pet instance, stats, điểm kỹ năng, nhánh tiến hóa, sao, trang bị, nguyên liệu, PetDex và nhiệm vụ.
3. Làm EggService dùng item có PersistentDataContainer, ấp theo scheduler với task lifecycle rõ ràng và pity theo banner.
4. Tách CombatService có giới hạn target scan, cooldown theo UUID, whitelist mob mục tiêu, không chạy quét toàn world mỗi tick.
5. Chọn API version chính thức của MythicMobs/MythicLib sau khi xác định version server mục tiêu; chỉ compile với API dependency `provided` và kiểm tra plugin runtime trước khi đăng ký hook.
6. Tích hợp PlaceholderAPI qua expansion tùy chọn; không cần để plugin chạy khi thiếu PAPI.
7. Hoàn thiện MySQL backend, transaction/retry và kiểm thử restart/offline.
8. Viết integration tests trên Paper test server; unit tests hiện tại không thay thế được kiểm thử server thật.
