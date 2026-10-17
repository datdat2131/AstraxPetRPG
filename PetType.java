package vn.soulpets;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/** Các loại pet (dùng mob vanilla làm hình dạng). */
public enum PetType {
    WOLF(EntityType.WOLF, "Sói Linh", ChatColor.AQUA, "Linh Kích", "Gây sát thương lên quái gần nhất.", PetRarity.COMMON, Role.ATTACK, Material.WOLF_SPAWN_EGG),
    CAT(EntityType.CAT, "Miêu Linh", ChatColor.LIGHT_PURPLE, "Ảnh Bộ", "Tăng tốc độ và độ bật nhảy cho chủ.", PetRarity.COMMON, Role.SPEED, Material.CAT_SPAWN_EGG),
    RABBIT(EntityType.RABBIT, "Nguyệt Thố", ChatColor.WHITE, "Nguyệt Bộ", "Tăng tốc độ và độ bật nhảy.", PetRarity.COMMON, Role.SPEED, Material.RABBIT_SPAWN_EGG),
    TURTLE(EntityType.TURTLE, "Huyền Quy", ChatColor.DARK_GREEN, "Quy Giáp", "Tăng chống chịu cho chủ.", PetRarity.COMMON, Role.DEFENSE, Material.TURTLE_SPAWN_EGG),

    FOX(EntityType.FOX, "Hồ Ly", ChatColor.GOLD, "Hồ Hỏa", "Gây sát thương và thiêu đốt mục tiêu.", PetRarity.RARE, Role.ATTACK, Material.FOX_SPAWN_EGG),
    BEE(EntityType.BEE, "Phong Ong", ChatColor.YELLOW, "Phong Châm", "Gây sát thương, thiêu đốt và làm chậm.", PetRarity.RARE, Role.ATTACK, Material.BEE_SPAWN_EGG),
    PARROT(EntityType.PARROT, "Thần Điểu", ChatColor.RED, "Vũ Phong", "Tăng tốc độ và tạo hiệu ứng gió.", PetRarity.RARE, Role.SPEED, Material.PARROT_SPAWN_EGG),
    LLAMA(EntityType.LLAMA, "Linh Lạc Đà", ChatColor.GRAY, "Linh Tiễn", "Phóng đòn đánh phép vào quái gần nhất.", PetRarity.RARE, Role.ATTACK, Material.LLAMA_SPAWN_EGG),
    GOAT(EntityType.GOAT, "Sơn Dương", ChatColor.WHITE, "Sơn Kích", "Húc mạnh gây sát thương lớn.", PetRarity.RARE, Role.ATTACK, Material.GOAT_SPAWN_EGG),

    ALLAY(EntityType.ALLAY, "Tinh Linh", ChatColor.GREEN, "Sinh Khí", "Hồi máu và tạo hồi phục cho chủ.", PetRarity.EPIC, Role.HEAL, Material.ALLAY_SPAWN_EGG),
    AXOLOTL(EntityType.AXOLOTL, "Axolotl Linh", ChatColor.AQUA, "Thủy Linh", "Hồi máu và tăng sinh lực.", PetRarity.EPIC, Role.HEAL, Material.AXOLOTL_SPAWN_EGG),
    CAMEL(EntityType.CAMEL, "Lạc Đà Sa Mạc", ChatColor.GOLD, "Sa Bộ", "Giảm sát thương nhận vào và tạo khiên.", PetRarity.EPIC, Role.DEFENSE, Material.CAMEL_SPAWN_EGG),
    PANDA(EntityType.PANDA, "Trúc Linh", ChatColor.GREEN, "Trúc Chấn", "Gây sát thương lên nhiều quái.", PetRarity.EPIC, Role.ATTACK, Material.PANDA_SPAWN_EGG),
    POLAR_BEAR(EntityType.POLAR_BEAR, "Băng Hùng", ChatColor.BLUE, "Băng Chưởng", "Gây sát thương mạnh và làm chậm.", PetRarity.EPIC, Role.ATTACK, Material.POLAR_BEAR_SPAWN_EGG),

    IRON_GOLEM(EntityType.IRON_GOLEM, "Thiết Vệ", ChatColor.GRAY, "Thiết Quyền", "Gây sát thương cực mạnh lên quái.", PetRarity.LEGENDARY, Role.ATTACK, Material.IRON_BLOCK),
    SNOW_GOLEM(EntityType.SNOW_GOLEM, "Tuyết Linh", ChatColor.WHITE, "Băng Tuyết", "Gây sát thương và làm chậm diện rộng.", PetRarity.LEGENDARY, Role.ATTACK, Material.SNOW_BLOCK),
    STRIDER(EntityType.STRIDER, "Hỏa Hành", ChatColor.RED, "Dung Nham Bộ", "Tăng sức mạnh hệ Lửa.", PetRarity.UNCOMMON, Role.SPEED, Material.STRIDER_SPAWN_EGG),
    BLAZE(EntityType.BLAZE, "Viêm Linh", ChatColor.RED, "Hỏa Cầu", "Tấn công bằng sức nóng.", PetRarity.RARE, Role.ATTACK, Material.BLAZE_SPAWN_EGG),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, "Dung Nham Thú", ChatColor.DARK_RED, "Dung Nham Chấn", "Sát thương bùng nổ.", PetRarity.EPIC, Role.ATTACK, Material.MAGMA_CREAM),
    GHAST(EntityType.GHAST, "U Minh Quỷ", ChatColor.GRAY, "U Minh Pháo", "Tấn công tầm xa.", PetRarity.LEGENDARY, Role.ATTACK, Material.GHAST_TEAR),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, "Hắc Cốt", ChatColor.DARK_GRAY, "Hắc Cốt Trảm", "Gây suy yếu mục tiêu.", PetRarity.MYTHIC, Role.ATTACK, Material.WITHER_SKELETON_SKULL),
    STRAY(EntityType.STRAY, "Băng Tiễn", ChatColor.AQUA, "Băng Tiễn", "Làm chậm mục tiêu.", PetRarity.UNCOMMON, Role.ATTACK, Material.STRAY_SPAWN_EGG),
    POLAR_BEAR_CUB(EntityType.POLAR_BEAR, "Bạch Hổ", ChatColor.BLUE, "Băng Nộ", "Phòng thủ và phản công.", PetRarity.RARE, Role.DEFENSE, Material.POLAR_BEAR_SPAWN_EGG),
    DROWNED(EntityType.DROWNED, "Hải U Linh", ChatColor.DARK_AQUA, "Thủy Lôi", "Tấn công kẻ địch gần.", PetRarity.UNCOMMON, Role.ATTACK, Material.DROWNED_SPAWN_EGG),
    GUARDIAN(EntityType.GUARDIAN, "Lôi Nhãn", ChatColor.YELLOW, "Lôi Quang", "Đánh dấu mục tiêu.", PetRarity.EPIC, Role.ATTACK, Material.GUARDIAN_SPAWN_EGG),
    BREEZE(EntityType.BREEZE, "Phong Lôi", ChatColor.WHITE, "Phong Bạo", "Tấn công diện rộng.", PetRarity.LEGENDARY, Role.ATTACK, Material.BREEZE_SPAWN_EGG),
    ENDERMAN(EntityType.ENDERMAN, "Ảnh Bộ", ChatColor.DARK_PURPLE, "Ảnh Dịch", "Cơ động né tránh.", PetRarity.RARE, Role.SPEED, Material.ENDERMAN_SPAWN_EGG),
    VEX(EntityType.VEX, "Ám Dực", ChatColor.DARK_PURPLE, "Ám Kích", "Đột kích mục tiêu.", PetRarity.MYTHIC, Role.ATTACK, Material.VEX_SPAWN_EGG),
    ALLAY_LIGHT(EntityType.ALLAY, "Quang Linh", ChatColor.YELLOW, "Quang Hộ", "Hồi phục và bảo vệ.", PetRarity.ANCIENT, Role.HEAL, Material.GLOWSTONE),
    MOOSHROOM(EntityType.MOOSHROOM, "Mộc Linh", ChatColor.GREEN, "Mộc Sinh", "Hỗ trợ hồi phục.", PetRarity.EPIC, Role.HEAL, Material.MOOSHROOM_SPAWN_EGG),
    SNIFFER(EntityType.SNIFFER, "Cổ Mộc", ChatColor.GREEN, "Cổ Mộc Sinh", "Tăng khả năng sinh tồn.", PetRarity.LEGENDARY, Role.DEFENSE, Material.SNIFFER_SPAWN_EGG);

    /** Vai trò của pet quyết định kỹ năng và hành vi tự động. */
    public enum Role {
        ATTACK("Tấn công"), HEAL("Hồi phục"), SPEED("Tốc độ"), DEFENSE("Phòng thủ");
        private final String display;
        Role(String display) { this.display = display; }
        public String display() { return display; }
    }

    private final EntityType entityType;
    private final String display;
    private final ChatColor color;
    private final String skillName;
    private final String skillDescription;
    private final PetRarity rarity;
    private final Role role;
    private final Material icon;

    PetType(EntityType entityType, String display, ChatColor color, String skillName, String skillDescription,
            PetRarity rarity, Role role, Material icon) {
        this.entityType = entityType;
        this.display = display;
        this.color = color;
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.rarity = rarity;
        this.role = role;
        this.icon = icon;
    }

    public EntityType entityType() { return entityType; }
    public String display() { return display; }
    public ChatColor color() { return color; }
    public String skillName() { return skillName; }
    public String skillDescription() { return skillDescription; }
    public PetRarity rarity() { return rarity; }
    public Role role() { return role; }
    public Material icon() { return icon; }

    /** Hệ nguyên tố dùng cho lọc PetDex, kỹ năng và cấu hình mở rộng. */
    public Element element() {
        return switch (this) {
            case FOX, BEE, STRIDER, BLAZE, MAGMA_CUBE, GHAST -> Element.FIRE;
            case TURTLE, POLAR_BEAR, SNOW_GOLEM, STRAY, POLAR_BEAR_CUB, DROWNED -> Element.ICE;
            case PARROT, GOAT, GUARDIAN, BREEZE -> Element.LIGHTNING;
            case ENDERMAN, VEX, WITHER_SKELETON -> Element.DARKNESS;
            case ALLAY, AXOLOTL, ALLAY_LIGHT -> Element.LIGHT;
            default -> Element.NATURE;
        };
    }

    public static PetType parse(String input) {
        if (input == null) return null;
        for (PetType type : values()) {
            if (type.name().equalsIgnoreCase(input) || type.display.equalsIgnoreCase(input)) return type;
        }
        return null;
    }
}
