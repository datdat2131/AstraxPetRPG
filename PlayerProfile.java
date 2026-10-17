package vn.soulpets;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Toàn bộ dữ liệu pet của một người chơi. */
public final class PlayerProfile {
    private final UUID owner;
    private final Map<PetType, PetEntry> pets = new EnumMap<>(PetType.class);
    private PetType selected;
    private int souls;\n    private int pityCount;

    public PlayerProfile(UUID owner, PetType selected, int souls) {
        this.owner = owner;
        this.selected = selected == null ? PetType.WOLF : selected;
        this.souls = Math.max(0, souls);
    }

    public UUID owner() { return owner; }
    public Map<PetType, PetEntry> pets() { return pets; }
    public PetType selected() { return selected; }
    public int souls() { return souls; }\n    public int pityCount() { return pityCount; }\n    public void setPityCount(int value) { pityCount = Math.max(0, value); }\n    public void incrementPity() { pityCount++; }\n    public void resetPity() { pityCount = 0; }

    public boolean has(PetType type) { return pets.containsKey(type); }
    public PetEntry entry(PetType type) { return pets.get(type); }
    public void add(PetEntry entry) { pets.put(entry.type(), entry); }
    public PetEntry active() { return pets.get(selected); }

    public boolean select(PetType type) {
        if (!has(type)) return false;
        selected = type;
        return true;
    }

    public void addSouls(int amount) { souls = Math.max(0, souls + amount); }

    public boolean takeSouls(int amount) {
        if (souls < amount) return false;
        souls -= amount;
        return true;
    }

    /** Đảm bảo luôn có ít nhất một pet và pet đang chọn hợp lệ. */
    public void normalize() {
        if (pets.isEmpty()) add(new PetEntry(selected, 1, 0, null));
        if (!has(selected)) selected = pets.keySet().iterator().next();
    }
}
