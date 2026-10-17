package vn.soulpets;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class PetStorage implements AutoCloseable {
    /** Một dòng bảng xếp hạng. */
    public record TopEntry(UUID owner, PetType type, int level, int xp) {}

    private final JavaPlugin plugin;
    private Connection connection;

    public PetStorage(JavaPlugin plugin) { this.plugin = plugin; }

    public synchronized void open() throws SQLException {
        File db = new File(plugin.getDataFolder(), "pets.db");
        File parent = db.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new SQLException("Cannot create data folder");
        connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pet_players ("
                    + "owner_uuid TEXT PRIMARY KEY,"
                    + "selected TEXT NOT NULL,"
                    + "souls INTEGER NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pet_meta (owner_uuid TEXT PRIMARY KEY, pity_count INTEGER NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pet_collection ("
                    + "owner_uuid TEXT NOT NULL,"
                    + "pet_type TEXT NOT NULL,"
                    + "level INTEGER NOT NULL DEFAULT 1,"
                    + "xp INTEGER NOT NULL DEFAULT 0,"
                    + "custom_name TEXT NOT NULL DEFAULT '',"
                    + "PRIMARY KEY (owner_uuid, pet_type))");
        }
    }

    public synchronized PlayerProfile load(UUID owner) {
        if (connection == null) return null;
        String id = owner.toString();
        try {
            PetType selected;
            int souls;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT selected, souls FROM pet_players WHERE owner_uuid=?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    selected = PetType.parse(rs.getString("selected"));
                    souls = rs.getInt("souls");
                }
            }
            PlayerProfile profile = new PlayerProfile(owner, selected, souls);
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT pet_type, level, xp, custom_name FROM pet_collection WHERE owner_uuid=?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PetType type = PetType.parse(rs.getString("pet_type"));
                        if (type == null) continue;
                        profile.add(new PetEntry(type, rs.getInt("level"), rs.getInt("xp"), rs.getString("custom_name")));
                    }
                }
            }
            profile.normalize();
            try (PreparedStatement meta = connection.prepareStatement("SELECT pity_count FROM pet_meta WHERE owner_uuid=?")) {
                meta.setString(1, id);
                try (ResultSet rs = meta.executeQuery()) { if (rs.next()) profile.setPityCount(rs.getInt(1)); }
            }
            return profile;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed loading profile " + owner, ex);
            return null;
        }
    }

    public synchronized void save(PlayerProfile profile) {
        if (connection == null || profile == null) return;
        String id = profile.owner().toString();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO pet_players(owner_uuid, selected, souls) VALUES(?,?,?) "
                            + "ON CONFLICT(owner_uuid) DO UPDATE SET selected=excluded.selected, souls=excluded.souls")) {
                ps.setString(1, id);
                ps.setString(2, profile.selected().name());
                ps.setInt(3, profile.souls());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO pet_collection(owner_uuid, pet_type, level, xp, custom_name) VALUES(?,?,?,?,?) "
                            + "ON CONFLICT(owner_uuid, pet_type) DO UPDATE SET level=excluded.level, "
                            + "xp=excluded.xp, custom_name=excluded.custom_name")) {
                for (PetEntry entry : profile.pets().values()) {
                    ps.setString(1, id);
                    ps.setString(2, entry.type().name());
                    ps.setInt(3, entry.level());
                    ps.setInt(4, entry.xp());
                    ps.setString(5, entry.customName());
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement meta = connection.prepareStatement(
                    "INSERT INTO pet_meta(owner_uuid,pity_count) VALUES(?,?) ON CONFLICT(owner_uuid) DO UPDATE SET pity_count=excluded.pity_count")) {
                meta.setString(1, id);
                meta.setInt(2, profile.pityCount());
                meta.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            try { connection.rollback(); } catch (SQLException ignored) { /* ignore */ }
            plugin.getLogger().log(Level.SEVERE, "Failed saving profile " + id, ex);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) { /* ignore */ }
        }
    }

    public synchronized List<TopEntry> top(int limit) {
        List<TopEntry> result = new ArrayList<>();
        if (connection == null) return result;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT owner_uuid, pet_type, level, xp FROM pet_collection "
                        + "ORDER BY level DESC, xp DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PetType type = PetType.parse(rs.getString("pet_type"));
                    if (type == null) continue;
                    try {
                        result.add(new TopEntry(UUID.fromString(rs.getString("owner_uuid")), type,
                                rs.getInt("level"), rs.getInt("xp")));
                    } catch (IllegalArgumentException ignored) { /* bad uuid */ }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed loading leaderboard", ex);
        }
        return result;
    }

    @Override public synchronized void close() {
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException ex) { plugin.getLogger().log(Level.WARNING, "Failed closing SQLite", ex); }
            connection = null;
        }
    }
}
