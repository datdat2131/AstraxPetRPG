package vn.soulpets;

/** Dữ liệu của một pet trong bộ sưu tập của người chơi. */
public final class PetEntry {
    public static final int MAX_LEVEL = 100;

    private final PetType type;
    private int level;
    private int xp;
    private String customName;

    public PetEntry(PetType type, int level, int xp, String customName) {
        this.type = type == null ? PetType.WOLF : type;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.xp = Math.max(0, xp);
        this.customName = customName == null || customName.isBlank() ? this.type.display() : customName;
    }

    public PetType type() { return type; }
    public int level() { return level; }
    public int xp() { return xp; }
    public String customName() { return customName; }
    public void setCustomName(String name) {
        this.customName = name == null || name.isBlank() ? type.display() : name;
    }

    public int xpToNextLevel() { return level >= MAX_LEVEL ? 0 : 100 + level * 25; }

    /** Cộng XP, trả về số cấp đã lên. */
    public int addXp(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= xpToNextLevel()) {
            xp -= xpToNextLevel();
            level++;
            gained++;
        }
        if (level >= MAX_LEVEL) xp = 0;
        return gained;
    }

    /** Giai đoạn tiến hóa 1-4 theo cấp. */
    public int stage() {
        if (level >= 75) return 4;
        if (level >= 50) return 3;
        if (level >= 25) return 2;
        return 1;
    }

    public String stageName() {
        return switch (stage()) {
            case 2 -> "Trưởng Thành";
            case 3 -> "Cường Giả";
            case 4 -> "Thần Thú";
            default -> "Sơ Sinh";
        };
    }
}
