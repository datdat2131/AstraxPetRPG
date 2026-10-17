package vn.soulpets;

/** Sáu hệ nguyên tố cốt lõi. */
public enum Element {
    FIRE("Lửa"), ICE("Băng"), LIGHTNING("Sét"), DARKNESS("Bóng tối"), LIGHT("Ánh sáng"), NATURE("Thiên nhiên");
    private final String display;
    Element(String display) { this.display = display; }
    public String display() { return display; }
}
