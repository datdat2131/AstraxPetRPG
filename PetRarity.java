package vn.soulpets;

import org.bukkit.ChatColor;

/** Độ hiếm, trọng số mặc định có thể ghi đè trong config.yml. */
public enum PetRarity {
    COMMON("Thường", ChatColor.WHITE, 1.0, 5200),
    UNCOMMON("Không Thường", ChatColor.GREEN, 1.12, 2600),
    RARE("Hiếm", ChatColor.AQUA, 1.3, 1300),
    EPIC("Sử Thi", ChatColor.LIGHT_PURPLE, 1.65, 600),
    LEGENDARY("Huyền Thoại", ChatColor.GOLD, 2.2, 250),
    MYTHIC("Thần Thoại", ChatColor.DARK_PURPLE, 3.0, 45),
    ANCIENT("Cổ Đại", ChatColor.RED, 4.0, 5);

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
