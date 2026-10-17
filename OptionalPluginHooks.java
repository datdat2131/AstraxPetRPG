package vn.soulpets;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hook an toàn: không liên kết compile-time tới API của plugin tùy chọn.
 * Chỉ bật hook khi plugin đã được nạp; thiếu plugin không ảnh hưởng khởi động.
 */
public final class OptionalPluginHooks {
    private final JavaPlugin plugin;
    public OptionalPluginHooks(JavaPlugin plugin) { this.plugin = plugin; }

    public void detect() {
        detect("MythicMobs");
        detect("MythicLib");
        detect("PlaceholderAPI");
    }

    private void detect(String name) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
        if (dependency != null && dependency.isEnabled()) {
            plugin.getLogger().info("Phát hiện " + name + " v" + dependency.getDescription().getVersion()
                    + ". Hook compile-time chưa bật; tính năng cốt lõi vẫn hoạt động độc lập.");
        } else {
            plugin.getLogger().info(name + " không được cài đặt; bỏ qua hook tùy chọn.");
        }
    }
}
