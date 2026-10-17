package vn.soulpets;

import org.bukkit.ChatColor;

/** Độ hiếm của pet: ảnh hưởng sức mạnh và tỉ lệ quay gacha. */
public enum PetRarity {
    COMMON("Thường", ChatColor.WHITE, 1.0, 60),
    RARE("Hiếm", ChatColor.AQUA, 1.25, 28),
    EPIC("Sử Thi", ChatColor.LIGHT_PURPLE, 1.6, 10),
    LEGENDARY("Huyền Thoại", ChatColor.GOLD, 2.2, 2);

    private final String display;
    private final ChatColor color;
    private final double multiplier;
    private final int defaultWeight;

    PetRarity(String display, ChatColor color, double multiplier, int defaultWeight) {
        this.display = display;
        this.color = color;
        this.multiplier = multiplier;
        this.defaultWeight = defaultWeight;
    }

    public String display() { return display; }
    public ChatColor color() { return color; }
    public double multiplier() { return multiplier; }
    public int defaultWeight() { return defaultWeight; }
}
