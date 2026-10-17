package vn.soulpets;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public final class SoulPetsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private enum MenuKind { MAIN, COLLECTION }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuKind kind;
        private Inventory inventory;
        MenuHolder(MenuKind kind) { this.kind = kind; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private PetStorage storage;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private final Map<UUID, UUID> activePetByOwner = new HashMap<>();
    private final Map<UUID, Long> skillCooldowns = new HashMap<>();
    private final Random random = new Random();
    private NamespacedKey petMarker, ownerMarker, buffHealthKey, buffSpeedKey, buffDamageKey;
    private int tickCounter = 0;

    // ------------------------------------------------------------------ lifecycle

    @Override public void onEnable() {
        saveDefaultConfig();
        petMarker = new NamespacedKey(this, "soulpet");
        ownerMarker = new NamespacedKey(this, "pet_owner");
        buffHealthKey = new NamespacedKey(this, "pet_buff_health");
        buffSpeedKey = new NamespacedKey(this, "pet_buff_speed");
        buffDamageKey = new NamespacedKey(this, "pet_buff_damage");
        new OptionalPluginHooks(this).detect();
        storage = new PetStorage(this);
        try {
            storage.open();
        } catch (SQLException ex) {
            getLogger().severe("Could not open SQLite: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Objects.requireNonNull(getCommand("pet")).setExecutor(this);
        Objects.requireNonNull(getCommand("pet")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("petadmin")).setExecutor(this);
        Objects.requireNonNull(getCommand("petadmin")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);

        // dọn pet mồ côi còn sót lại từ lần chạy trước
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isPet(entity)) entity.remove();
            }
        }
        // xoá buff cũ của người chơi đang online (khi /reload)
        for (Player online : Bukkit.getOnlinePlayers()) removeBuffs(online);

        Bukkit.getScheduler().runTaskTimer(this, this::tickPets, 20L, 20L);
        long autosave = Math.max(1, getConfig().getInt("settings.autosave-minutes", 5)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, this::saveAll, autosave, autosave);
        getLogger().info("Astrax Pet RPG 2.0.0 enabled.");
    }

    @Override public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        for (Player player : Bukkit.getOnlinePlayers()) dismiss(player, false);
        if (storage != null) {
            saveAll();
            storage.close();
        }
    }

    private void saveAll() {
        for (PlayerProfile profile : profiles.values()) storage.save(profile);
    }

    // ------------------------------------------------------------------ helpers

    private PlayerProfile profile(UUID id) {
        PlayerProfile cached = profiles.get(id);
        if (cached != null) return cached;
        PlayerProfile loaded = storage.load(id);
        if (loaded == null) {
            loaded = new PlayerProfile(id, PetType.WOLF, getConfig().getInt("settings.starting-souls", 100));
            loaded.normalize();
            storage.save(loaded);
        }
        profiles.put(id, loaded);
        return loaded;
    }

    private boolean isPet(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(petMarker, PersistentDataType.BYTE);
    }

    private UUID ownerOf(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(ownerMarker, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ex) { return null; }
    }

    private void updateName(Entity entity, PetEntry entry) {
        PetRarity rarity = entry.type().rarity();
        entity.setCustomName(entry.type().color() + entry.customName() + ChatColor.GRAY + " [Lv." + entry.level() + "] "
                + rarity.color() + rarity.display());
        entity.setCustomNameVisible(true);
    }

    private double maxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr == null ? 20.0 : attr.getValue();
    }

    private Location behind(Player player) {
        Location loc = player.getLocation();
        Vector dir = loc.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, 1);
        dir.normalize().multiply(-1.5);
        return loc.clone().add(dir);
    }

    private boolean heavy(PetType type) {
        return type == PetType.IRON_GOLEM || type == PetType.POLAR_BEAR || type == PetType.GOAT;
    }

    private double skillDamage(PetEntry entry) {
        double base = getConfig().getDouble("settings.base-skill-damage", 3.0)
                + entry.level() * getConfig().getDouble("settings.damage-per-level", 0.35);
        double damage = base * entry.type().rarity().multiplier() * (1.0 + 0.1 * (entry.stage() - 1));
        if (heavy(entry.type())) damage *= 1.5;
        return damage;
    }

    // ------------------------------------------------------------------ passive buffs

    /** @return {maxHealthBonus, speedBonus(scalar), attackDamageBonus} */
    private double[] passiveValues(PetEntry entry) {
        double mult = entry.type().rarity().multiplier();
        int level = entry.level();
        double health = level * getConfig().getDouble("passive.health-per-level", 0.05) * mult;
        double speed = level * getConfig().getDouble("passive.speed-per-level", 0.001) * mult;
        double damage = level * getConfig().getDouble("passive.damage-per-level", 0.02) * mult;
        switch (entry.type().role()) {
            case HEAL -> health *= 2.0;
            case DEFENSE -> health *= 1.5;
            case SPEED -> speed *= 2.0;
            case ATTACK -> damage *= 2.0;
        }
        health = Math.min(getConfig().getDouble("passive.max-health-bonus", 20.0), health);
        speed = Math.min(0.3, speed);
        damage = Math.min(getConfig().getDouble("passive.max-damage-bonus", 6.0), damage);
        return new double[] { health, speed, damage };
    }

    private void addModifier(Player player, Attribute attribute, NamespacedKey key, double amount,
                             AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || amount <= 0) return;
        instance.addModifier(new AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY));
    }

    private void applyBuffs(Player player, PetEntry entry) {
        removeBuffs(player);
        if (!getConfig().getBoolean("passive.enabled", true)) return;
        double[] values = passiveValues(entry);
        addModifier(player, Attribute.MAX_HEALTH, buffHealthKey, values[0], AttributeModifier.Operation.ADD_NUMBER);
        addModifier(player, Attribute.MOVEMENT_SPEED, buffSpeedKey, values[1], AttributeModifier.Operation.ADD_SCALAR);
        addModifier(player, Attribute.ATTACK_DAMAGE, buffDamageKey, values[2], AttributeModifier.Operation.ADD_NUMBER);
    }

    private void removeBuffs(Player player) {
        List<Attribute> attributes = List.of(Attribute.MAX_HEALTH, Attribute.MOVEMENT_SPEED, Attribute.ATTACK_DAMAGE);
        for (Attribute attribute : attributes) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                NamespacedKey key = modifier.getKey();
                if (key.equals(buffHealthKey) || key.equals(buffSpeedKey) || key.equals(buffDamageKey)) {
                    instance.removeModifier(modifier);
                }
            }
        }
        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null && player.getHealth() > health.getValue()) player.setHealth(health.getValue());
    }

    // ------------------------------------------------------------------ summon / dismiss

    private void summon(Player player, boolean quiet) {
        PlayerProfile profile = profile(player.getUniqueId());
        PetEntry entry = profile.active();
        if (entry == null) return;
        UUID currentId = activePetByOwner.get(player.getUniqueId());
        if (currentId != null) {
            Entity current = Bukkit.getEntity(currentId);
            if (current != null && current.isValid()) {
                if (!quiet) player.sendMessage(ChatColor.YELLOW + "Bạn đã có pet đang hoạt động. Dùng /pet dismiss trước.");
                return;
            }
            activePetByOwner.remove(player.getUniqueId());
        }
        Entity entity = player.getWorld().spawnEntity(behind(player), entry.type().entityType());
        entity.getPersistentDataContainer().set(petMarker, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(ownerMarker, PersistentDataType.STRING, player.getUniqueId().toString());
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        if (entity instanceof LivingEntity living) living.setCollidable(false);
        if (entity instanceof Ageable ageable) ageable.setAdult();
        if (entity instanceof Tameable tameable && (entity instanceof Wolf || entity instanceof Cat)) {
            tameable.setTamed(true);
            tameable.setOwner(player);
        }
        if (entity instanceof Sittable sittable) sittable.setSitting(false);
        if (entity instanceof Wolf wolf) { wolf.setAngry(false); wolf.setCollarColor(DyeColor.CYAN); }
        if (entity instanceof Fox fox) fox.setFoxType(Fox.Type.RED);
        updateName(entity, entry);
        activePetByOwner.put(player.getUniqueId(), entity.getUniqueId());
        applyBuffs(player, entry);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 12, .4, .4, .4, .02);
        if (!quiet) {
            player.sendMessage(ChatColor.GREEN + "Đã triệu hồi " + entry.type().color() + entry.customName()
                    + ChatColor.GREEN + " cấp " + entry.level() + " (" + entry.type().rarity().color()
                    + entry.type().rarity().display() + ChatColor.GREEN + ").");
        }
    }

    private void dismiss(Player player, boolean message) {
        UUID id = activePetByOwner.remove(player.getUniqueId());
        if (id != null) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
        removeBuffs(player);
        if (message) player.sendMessage(ChatColor.GRAY + "Đã thu hồi pet.");
    }

    private void selectPet(Player player, PetType type) {
        PlayerProfile profile = profile(player.getUniqueId());
        if (!profile.has(type)) {
            player.sendMessage(ChatColor.RED + "Bạn chưa sở hữu " + type.display() + ". Hãy quay gacha để nhận pet mới!");
            return;
        }
        boolean wasActive = activePetByOwner.containsKey(player.getUniqueId());
        if (wasActive) dismiss(player, false);
        profile.select(type);
        storage.save(profile);
        player.sendMessage(ChatColor.GREEN + "Đã chọn pet " + type.color() + type.display() + ChatColor.GREEN + ".");
        if (wasActive) summon(player, true);
    }

    // ------------------------------------------------------------------ tick: follow + auto skill

    private void tickPets() {
        tickCounter++;
        double teleportDistance = getConfig().getDouble("settings.teleport-distance", 14.0);
        double followDistance = getConfig().getDouble("settings.follow-distance", 3.0);
        boolean auto = getConfig().getBoolean("auto-skill.enabled", true);
        int autoEvery = Math.max(1, getConfig().getInt("auto-skill.interval-seconds", 4));
        for (Map.Entry<UUID, UUID> link : new HashMap<>(activePetByOwner).entrySet()) {
            Player owner = Bukkit.getPlayer(link.getKey());
            if (owner == null || !owner.isOnline()) {
                Entity orphan = Bukkit.getEntity(link.getValue());
                if (orphan != null) orphan.remove();
                activePetByOwner.remove(link.getKey());
                continue;
            }
            Entity entity = Bukkit.getEntity(link.getValue());
            if (entity == null || !entity.isValid()) {
                activePetByOwner.remove(link.getKey());
                if (getConfig().getBoolean("settings.auto-respawn", true)) summon(owner, true);
                continue;
            }
            if (!owner.getWorld().equals(entity.getWorld())) {
                entity.teleport(behind(owner));
                continue;
            }
            double distance = entity.getLocation().distance(owner.getLocation());
            if (distance > teleportDistance) {
                entity.teleport(behind(owner));
            } else if (distance > followDistance && entity instanceof Mob mob) {
                mob.getPathfinder().moveTo(owner.getLocation(), 1.15);
            }
            if (auto && tickCounter % autoEvery == 0) autoSkill(owner, entity);
        }
    }

    private void autoSkill(Player owner, Entity petEntity) {
        PetEntry entry = profile(owner.getUniqueId()).active();
        if (entry == null) return;
        switch (entry.type().role()) {
            case ATTACK -> {
                double range = getConfig().getDouble("settings.skill-range", 8.0);
                List<LivingEntity> targets = nearestEnemies(owner, range, 1);
                if (targets.isEmpty()) return;
                double damage = skillDamage(entry) * getConfig().getDouble("auto-skill.damage-multiplier", 0.4);
                strike(owner, petEntity, targets.get(0), entry, damage);
            }
            case HEAL -> {
                double max = maxHealth(owner);
                if (owner.getHealth() < max * 0.7) {
                    double heal = Math.min(4.0, 1.0 + entry.level() * 0.03) * entry.type().rarity().multiplier();
                    owner.setHealth(Math.min(max, owner.getHealth() + heal));
                    owner.getWorld().spawnParticle(Particle.HEART, owner.getLocation().add(0, 1.5, 0), 3, .3, .3, .3, 0);
                }
            }
            default -> { }
        }
    }

    private List<LivingEntity> nearestEnemies(Player player, double range, int count) {
        List<LivingEntity> found = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof Enemy) || isPet(e)) continue;
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) e;
            if (living.isDead()) continue;
            found.add(living);
        }
        Location origin = player.getLocation();
        found.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin)));
        if (found.size() > count) return new ArrayList<>(found.subList(0, count));
        return found;
    }

    private void strike(Player owner, Entity petEntity, LivingEntity target, PetEntry entry, double damage) {
        PetType type = entry.type();
        target.damage(damage, owner);
        if (type == PetType.FOX || type == PetType.BEE) target.setFireTicks(type == PetType.FOX ? 60 : 20);
        if (type == PetType.POLAR_BEAR || type == PetType.SNOW_GOLEM || type == PetType.BEE) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
        }
        Particle particle = (type == PetType.FOX || type == PetType.BEE) ? Particle.FLAME
                : (type == PetType.POLAR_BEAR || type == PetType.SNOW_GOLEM) ? Particle.SNOWFLAKE : Particle.CRIT;
        beam(petEntity.getLocation().add(0, 0.8, 0), target.getLocation().add(0, 1, 0), particle);
        target.getWorld().spawnParticle(particle, target.getLocation().add(0, 1, 0), 10, .3, .4, .3, .04);
    }

    private void beam(Location from, Location to, Particle particle) {
        World world = from.getWorld();
        if (world == null || !world.equals(to.getWorld())) return;
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length < 0.5) return;
        direction.normalize();
        int steps = (int) Math.min(12, length * 2);
        for (int i = 1; i <= steps; i++) {
            Location point = from.clone().add(direction.clone().multiply(length * i / steps));
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ manual skill

    private void useSkill(Player player) {
        UUID petId = activePetByOwner.get(player.getUniqueId());
        Entity pet = petId == null ? null : Bukkit.getEntity(petId);
        if (pet == null || !pet.isValid()) {
            player.sendMessage(ChatColor.RED + "Hãy triệu hồi pet trước bằng /pet summon.");
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = skillCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            player.sendMessage(ChatColor.YELLOW + "Kỹ năng hồi chiêu còn " + Math.max(1, (readyAt - now + 999) / 1000) + " giây.");
            return;
        }
        PlayerProfile profile = profile(player.getUniqueId());
        PetEntry entry = profile.active();
        if (entry == null) return;
        PetType type = entry.type();
        double mult = type.rarity().multiplier();
        int duration = 100 + entry.level() * 2;

        switch (type.role()) {
            case HEAL -> {
                double heal = Math.min(12.0, 3.0 + entry.level() * 0.08) * mult;
                player.setHealth(Math.min(maxHealth(player), player.getHealth() + heal));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, true, true, true));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 10, .5, .4, .5, .02);
                player.sendMessage(type.color() + entry.customName() + ChatColor.GREEN + " dùng " + type.skillName()
                        + ", hồi " + String.format(Locale.US, "%.1f", heal) + " máu.");
            }
            case SPEED -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 1, true, true, true));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 16, .5, .2, .5, .03);
                player.sendMessage(type.color() + entry.customName() + ChatColor.GREEN + " dùng " + type.skillName() + ".");
            }
            case DEFENSE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 1, true, true, true));
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 12, .5, .5, .5, .02);
                player.sendMessage(type.color() + entry.customName() + ChatColor.GREEN + " kích hoạt " + type.skillName() + ".");
            }
            case ATTACK -> {
                double range = getConfig().getDouble("settings.skill-range", 8.0);
                int count = 1 + (entry.stage() - 1) + (type == PetType.PANDA || type == PetType.SNOW_GOLEM ? 2 : 0);
                List<LivingEntity> targets = nearestEnemies(player, range, count);
                if (targets.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "Không có quái vật trong phạm vi " + (int) range + " block.");
                    return;
                }
                double damage = skillDamage(entry);
                for (LivingEntity target : targets) strike(player, pet, target, entry, damage);
                player.sendMessage(type.color() + entry.customName() + ChatColor.GREEN + " dùng " + type.skillName()
                        + ", đánh " + targets.size() + " mục tiêu, mỗi đòn "
                        + String.format(Locale.US, "%.1f", damage) + " sát thương.");
            }
        }
        skillCooldowns.put(player.getUniqueId(), now + getConfig().getLong("settings.skill-cooldown-seconds", 8) * 1000L);
    }

    // ------------------------------------------------------------------ xp / level

    private void gainXp(Player player, PlayerProfile profile, PetEntry entry, int amount, String extra) {
        int beforeStage = entry.stage();
        int levels = entry.addXp(amount);
        boolean isActive = entry == profile.active();
        boolean summoned = isActive && activePetByOwner.containsKey(player.getUniqueId());
        if (levels > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendMessage(ChatColor.GOLD + "Pet " + entry.type().color() + entry.customName()
                    + ChatColor.GOLD + " lên cấp " + entry.level() + "!");
            if (entry.stage() > beforeStage) {
                player.sendTitle(ChatColor.GOLD + "TIẾN HÓA!",
                        entry.type().color() + entry.customName() + ChatColor.GRAY + " → " + ChatColor.YELLOW + entry.stageName(),
                        10, 60, 20);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
            if (summoned) {
                UUID petId = activePetByOwner.get(player.getUniqueId());
                Entity entity = petId == null ? null : Bukkit.getEntity(petId);
                if (entity != null) updateName(entity, entry);
                applyBuffs(player, entry);
            }
        }
        if (isActive) actionBar(player, entry, extra);
    }

    private void actionBar(Player player, PetEntry entry, String extra) {
        String progress;
        if (entry.level() >= PetEntry.MAX_LEVEL) {
            progress = ChatColor.GOLD + "MAX";
        } else {
            int filled = (int) Math.round(10.0 * entry.xp() / Math.max(1, entry.xpToNextLevel()));
            progress = ChatColor.GREEN + "|".repeat(Math.max(0, filled)) + ChatColor.DARK_GRAY + "|".repeat(Math.max(0, 10 - filled))
                    + ChatColor.GRAY + " " + entry.xp() + "/" + entry.xpToNextLevel();
        }
        String message = entry.type().color() + entry.customName() + ChatColor.GRAY + " Lv." + entry.level() + " " + progress
                + (extra == null || extra.isEmpty() ? "" : ChatColor.YELLOW + "  " + extra);
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    // ------------------------------------------------------------------ gacha

    private PetRarity rollRarity() {
        int total = 0;
        for (PetRarity rarity : PetRarity.values()) total += weight(rarity);
        int roll = random.nextInt(Math.max(1, total));
        for (PetRarity rarity : PetRarity.values()) {
            roll -= weight(rarity);
            if (roll < 0) return rarity;
        }
        return PetRarity.COMMON;
    }

    private int weight(PetRarity rarity) {
        return Math.max(0, getConfig().getInt("gacha.weights." + rarity.name().toLowerCase(Locale.ROOT), rarity.defaultWeight()));
    }

    private void gacha(Player player) {
        PlayerProfile profile = profile(player.getUniqueId());
        int cost = Math.max(1, getConfig().getInt("gacha.cost-souls", 100));
        if (!profile.takeSouls(cost)) {
            player.sendMessage(ChatColor.RED + "Không đủ Linh Hồn. Cần " + cost + ", bạn có " + profile.souls()
                    + ". Hãy diệt quái để kiếm thêm!");
            return;
        }
        int pityLimit = Math.max(1, getConfig().getInt("gacha.pity-epic", 30));
        PetRarity rarity;
        if (profile.pityCount() >= pityLimit - 1) {
            List<PetRarity> guaranteed = Arrays.stream(PetRarity.values())
                    .filter(r -> r.ordinal() >= PetRarity.EPIC.ordinal() && weight(r) > 0).toList();
            rarity = guaranteed.isEmpty() ? PetRarity.EPIC : guaranteed.get(random.nextInt(guaranteed.size()));
        } else {
            rarity = rollRarity();
        }
        if (rarity.ordinal() >= PetRarity.EPIC.ordinal()) profile.resetPity();
        else profile.incrementPity();
        List<PetType> pool = new ArrayList<>();
        for (PetType type : PetType.values()) if (type.rarity() == rarity) pool.add(type);
        PetType type = pool.get(random.nextInt(pool.size()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        if (profile.has(type)) {
            int dupXp = getConfig().getInt("gacha.duplicate-xp", 150);
            player.sendMessage(rarity.color() + "[" + rarity.display() + "] " + type.color() + type.display()
                    + ChatColor.GRAY + " (trùng) → +" + dupXp + " XP cho pet này.");
            gainXp(player, profile, profile.entry(type), dupXp, null);
        } else {
            profile.add(new PetEntry(type, 1, 0, null));
            player.sendTitle(rarity.color() + rarity.display(), type.color() + type.display(), 10, 50, 20);
            player.sendMessage(ChatColor.GREEN + "Bạn nhận được pet mới: " + rarity.color() + "[" + rarity.display() + "] "
                    + type.color() + type.display() + ChatColor.GREEN + "!");
            if (getConfig().getBoolean("gacha.broadcast-rare", true)
                    && (rarity.ordinal() >= PetRarity.EPIC.ordinal())) {
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " vừa quay được pet "
                        + rarity.color() + rarity.display() + " " + type.color() + type.display() + ChatColor.YELLOW + "!");
            }
        }
        storage.save(profile);
    }

    // ------------------------------------------------------------------ menus

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private void openMain(Player player) {
        PlayerProfile profile = profile(player.getUniqueId());
        PetEntry entry = profile.active();
        MenuHolder holder = new MenuHolder(MenuKind.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_AQUA + "Astrax Pet RPG");
        holder.inventory = inv;
        String current = entry == null ? "Không có" : entry.type().display() + " Lv." + entry.level();
        inv.setItem(10, item(Material.LEAD, ChatColor.GREEN + "Triệu hồi pet", List.of(ChatColor.GRAY + "Pet đang chọn: " + current)));
        inv.setItem(11, item(Material.BONE, ChatColor.YELLOW + "Thông tin pet", List.of(ChatColor.GRAY + "Xem cấp, XP và buff.")));
        inv.setItem(13, item(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Dùng kỹ năng", List.of(
                ChatColor.GRAY + (entry == null ? "-" : entry.type().skillName()),
                ChatColor.GRAY + (entry == null ? "" : entry.type().skillDescription()))));
        inv.setItem(15, item(Material.CHEST, ChatColor.AQUA + "Bộ sưu tập", List.of(
                ChatColor.GRAY + "Đã sở hữu: " + profile.pets().size() + "/" + PetType.values().length,
                ChatColor.GRAY + "Bấm để đổi pet.")));
        inv.setItem(16, item(Material.BARRIER, ChatColor.RED + "Thu hồi pet", List.of(ChatColor.GRAY + "Ẩn pet đang hoạt động.")));
        inv.setItem(20, item(Material.ENDER_CHEST, ChatColor.GOLD + "Quay pet (Gacha)", List.of(
                ChatColor.GRAY + "Giá: " + ChatColor.YELLOW + getConfig().getInt("gacha.cost-souls", 100) + " Linh Hồn",
                ChatColor.GRAY + "Bạn có: " + ChatColor.YELLOW + profile.souls() + " Linh Hồn")));
        inv.setItem(22, item(Material.SOUL_LANTERN, ChatColor.YELLOW + "Linh Hồn: " + profile.souls(),
                List.of(ChatColor.GRAY + "Diệt quái để nhận Linh Hồn.")));
        inv.setItem(24, item(Material.GOLD_INGOT, ChatColor.GOLD + "Bảng xếp hạng", List.of(ChatColor.GRAY + "Top pet cấp cao nhất.")));
        player.openInventory(inv);
    }

    private void openCollection(Player player) {
        PlayerProfile profile = profile(player.getUniqueId());
        MenuHolder holder = new MenuHolder(MenuKind.COLLECTION);
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_AQUA + "Bộ Sưu Tập Pet");
        holder.inventory = inv;
        PetType[] types = PetType.values();
        for (int i = 0; i < types.length; i++) {
            PetType type = types[i];
            PetRarity rarity = type.rarity();
            PetEntry entry = profile.entry(type);
            if (entry == null) {
                inv.setItem(i, item(Material.GRAY_DYE, ChatColor.GRAY + "??? (" + type.display() + ")", List.of(
                        rarity.color() + rarity.display(),
                        ChatColor.DARK_GRAY + "Chưa sở hữu - quay gacha để nhận.")));
            } else {
                List<String> lore = new ArrayList<>();
                lore.add(rarity.color() + rarity.display() + ChatColor.GRAY + " | " + type.role().display());
                lore.add(ChatColor.GRAY + "Cấp: " + ChatColor.WHITE + entry.level() + "/" + PetEntry.MAX_LEVEL
                        + ChatColor.GRAY + " (" + entry.stageName() + ")");
                lore.add(ChatColor.GRAY + "Kỹ năng: " + ChatColor.WHITE + type.skillName());
                lore.add(ChatColor.DARK_GRAY + type.skillDescription());
                lore.add(type == profile.selected() ? ChatColor.GREEN + "✔ Đang chọn" : ChatColor.YELLOW + "Bấm để chọn");
                inv.setItem(i, item(type.icon(), type.color() + entry.customName(), lore));
            }
        }
        inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "Quay lại", List.of(ChatColor.GRAY + "Về menu chính.")));
        inv.setItem(49, item(Material.BOOK, ChatColor.AQUA + "Đã sở hữu " + profile.pets().size() + "/" + types.length, List.of()));
        player.openInventory(inv);
    }

    @EventHandler public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        int slot = event.getRawSlot();
        if (holder.kind == MenuKind.MAIN) {
            switch (slot) {
                case 10 -> { player.closeInventory(); summon(player, false); }
                case 11 -> { player.closeInventory(); showInfo(player); }
                case 13 -> { player.closeInventory(); useSkill(player); }
                case 15 -> openCollection(player);
                case 16 -> { player.closeInventory(); dismiss(player, true); }
                case 20 -> { player.closeInventory(); gacha(player); }
                case 24 -> { player.closeInventory(); showTop(player); }
                default -> { }
            }
        } else {
            PetType[] types = PetType.values();
            if (slot == 45) { openMain(player); return; }
            if (slot < types.length) {
                selectPet(player, types[slot]);
                openCollection(player);
            }
        }
    }

    @EventHandler public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    // ------------------------------------------------------------------ info / top

    private void showInfo(Player player) {
        PlayerProfile profile = profile(player.getUniqueId());
        PetEntry entry = profile.active();
        if (entry == null) return;
        PetType type = entry.type();
        double[] buff = passiveValues(entry);
        player.sendMessage(ChatColor.GOLD + "=== Astrax Pet RPG ===");
        player.sendMessage(ChatColor.YELLOW + "Pet: " + type.color() + entry.customName() + ChatColor.GRAY + " (" + type.display() + ")");
        player.sendMessage(ChatColor.YELLOW + "Độ hiếm: " + type.rarity().color() + type.rarity().display()
                + ChatColor.GRAY + " | Vai trò: " + ChatColor.WHITE + type.role().display());
        player.sendMessage(ChatColor.YELLOW + "Cấp: " + ChatColor.WHITE + entry.level() + "/" + PetEntry.MAX_LEVEL
                + ChatColor.GRAY + " (" + entry.stageName() + ")");
        player.sendMessage(ChatColor.YELLOW + "XP: " + ChatColor.WHITE + entry.xp() + "/" + entry.xpToNextLevel());
        player.sendMessage(ChatColor.YELLOW + "Kỹ năng: " + ChatColor.WHITE + type.skillName() + ChatColor.GRAY + " — " + type.skillDescription());
        player.sendMessage(ChatColor.YELLOW + "Buff thụ động: " + ChatColor.GREEN + "+" + String.format(Locale.US, "%.1f", buff[0]) + " máu, +"
                + String.format(Locale.US, "%.1f", buff[1] * 100) + "% tốc độ, +" + String.format(Locale.US, "%.1f", buff[2]) + " sát thương");
        player.sendMessage(ChatColor.YELLOW + "Linh Hồn: " + ChatColor.WHITE + profile.souls()
                + ChatColor.GRAY + " | Bộ sưu tập: " + profile.pets().size() + "/" + PetType.values().length);
    }

    private void showTop(Player player) {
        List<PetStorage.TopEntry> top = storage.top(5);
        player.sendMessage(ChatColor.GOLD + "=== Top Pet cấp cao nhất ===");
        if (top.isEmpty()) { player.sendMessage(ChatColor.GRAY + "Chưa có dữ liệu."); return; }
        int rank = 1;
        for (PetStorage.TopEntry row : top) {
            String name = Bukkit.getOfflinePlayer(row.owner()).getName();
            player.sendMessage(ChatColor.YELLOW + "#" + rank++ + " " + ChatColor.WHITE + (name == null ? "?" : name)
                    + ChatColor.GRAY + " - " + row.type().color() + row.type().display() + ChatColor.GRAY + " Lv." + row.level());
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Lệnh Astrax Pet RPG ===");
        sender.sendMessage(ChatColor.YELLOW + "/pet" + ChatColor.GRAY + " - mở menu");
        sender.sendMessage(ChatColor.YELLOW + "/pet summon | dismiss" + ChatColor.GRAY + " - triệu hồi / thu hồi");
        sender.sendMessage(ChatColor.YELLOW + "/pet info | skill" + ChatColor.GRAY + " - thông tin / dùng kỹ năng");
        sender.sendMessage(ChatColor.YELLOW + "/pet select <loại>" + ChatColor.GRAY + " - chọn pet đã sở hữu");
        sender.sendMessage(ChatColor.YELLOW + "/pet collection" + ChatColor.GRAY + " - bộ sưu tập");
        sender.sendMessage(ChatColor.YELLOW + "/pet gacha" + ChatColor.GRAY + " - quay pet mới bằng Linh Hồn");
        sender.sendMessage(ChatColor.YELLOW + "/pet souls | top | rename <tên>");
    }

    // ------------------------------------------------------------------ commands

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("petadmin")) return handleAdmin(sender, args);

        if (!(sender instanceof Player player)) { sender.sendMessage("Lệnh này chỉ dành cho người chơi."); return true; }
        if (!player.hasPermission("soulpets.use")) { player.sendMessage(ChatColor.RED + "Bạn không có quyền."); return true; }
        if (args.length == 0) { openMain(player); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "menu" -> openMain(player);
            case "summon" -> summon(player, false);
            case "dismiss" -> dismiss(player, true);
            case "info" -> showInfo(player);
            case "skill" -> useSkill(player);
            case "collection", "list" -> openCollection(player);
            case "gacha", "roll", "egg", "hatch" -> gacha(player);
            case "stats" -> showInfo(player);
            case "evolve" -> {
                PetEntry current = profile(player.getUniqueId()).active();
                if (current == null) player.sendMessage(ChatColor.RED + "Bạn chưa chọn pet.");
                else player.sendMessage(ChatColor.LIGHT_PURPLE + current.type().display() + " — " + current.stageName()
                        + " (cấp " + current.level() + "). Tiến hóa tự động tại cấp 25/50/75; nhánh tiến hóa tùy chỉnh cần cấu hình thêm.");
            }
            case "equip" -> player.sendMessage(ChatColor.YELLOW + "Hệ trang bị đã được chuẩn bị trong cấu trúc mở rộng; cần bật module equipment để sử dụng.");
            case "petdex" -> openCollection(player);
            case "top" -> showTop(player);
            case "help" -> showHelp(player);
            case "souls" -> player.sendMessage(ChatColor.YELLOW + "Linh Hồn của bạn: " + ChatColor.WHITE + profile(player.getUniqueId()).souls());
            case "select" -> {
                if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + "Dùng: /pet select <loại pet>"); return true; }
                PetType type = PetType.parse(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                if (type == null) { player.sendMessage(ChatColor.RED + "Loại pet không hợp lệ."); return true; }
                selectPet(player, type);
            }
            case "rename" -> {
                if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + "Dùng: /pet rename <tên mới>"); return true; }
                String name = ChatColor.stripColor(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                if (name.length() > 24) { player.sendMessage(ChatColor.RED + "Tên tối đa 24 ký tự."); return true; }
                PlayerProfile profile = profile(player.getUniqueId());
                PetEntry entry = profile.active();
                if (entry == null) return true;
                entry.setCustomName(name);
                storage.save(profile);
                UUID id = activePetByOwner.get(player.getUniqueId());
                Entity entity = id == null ? null : Bukkit.getEntity(id);
                if (entity != null) updateName(entity, entry);
                player.sendMessage(ChatColor.GREEN + "Đã đổi tên pet thành " + name + ".");
            }
            default -> showHelp(player);
        }
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("soulpets.admin")) { sender.sendMessage(ChatColor.RED + "Bạn không có quyền."); return true; }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/petadmin give <player> <loại pet> | xp <player> <số> | souls <player> <số> | reload");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> {
                if (args.length != 3) { sender.sendMessage(ChatColor.YELLOW + "/petadmin give <player> <loại pet>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                PetType type = PetType.parse(args[2]);
                if (target == null || type == null) { sender.sendMessage(ChatColor.RED + "Player phải online và loại pet hợp lệ."); return true; }
                PlayerProfile profile = profile(target.getUniqueId());
                if (profile.has(type)) { sender.sendMessage(ChatColor.YELLOW + target.getName() + " đã sở hữu " + type.display() + "."); return true; }
                profile.add(new PetEntry(type, 1, 0, null));
                storage.save(profile);
                sender.sendMessage(ChatColor.GREEN + "Đã cấp " + type.display() + " cho " + target.getName() + ".");
                target.sendMessage(ChatColor.GREEN + "Bạn nhận được pet " + type.rarity().color() + type.display() + ChatColor.GREEN + "! Dùng /pet select.");
            }
            case "xp" -> {
                if (args.length != 3) { sender.sendMessage(ChatColor.YELLOW + "/petadmin xp <player> <số>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                int amount;
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage(ChatColor.RED + "XP phải là số nguyên."); return true; }
                if (target == null || amount < 1) { sender.sendMessage(ChatColor.RED + "Player phải online và XP phải > 0."); return true; }
                PlayerProfile profile = profile(target.getUniqueId());
                PetEntry entry = profile.active();
                if (entry == null) return true;
                gainXp(target, profile, entry, amount, null);
                storage.save(profile);
                sender.sendMessage(ChatColor.GREEN + "Đã cộng " + amount + " XP cho pet của " + target.getName() + ".");
            }
            case "souls" -> {
                if (args.length != 3) { sender.sendMessage(ChatColor.YELLOW + "/petadmin souls <player> <số>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                int amount;
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage(ChatColor.RED + "Số phải là số nguyên."); return true; }
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player phải online."); return true; }
                PlayerProfile profile = profile(target.getUniqueId());
                profile.addSouls(amount);
                storage.save(profile);
                sender.sendMessage(ChatColor.GREEN + "Linh Hồn của " + target.getName() + " hiện là " + profile.souls() + ".");
            }
            case "reload" -> { reloadConfig(); sender.sendMessage(ChatColor.GREEN + "Đã tải lại config.yml."); }
            default -> sender.sendMessage(ChatColor.YELLOW + "/petadmin give|xp|souls|reload ...");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> typeNames = Arrays.stream(PetType.values()).map(t -> t.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
        if (command.getName().equalsIgnoreCase("pet")) {
            if (args.length == 1) {
                return List.of("menu", "summon", "dismiss", "info", "skill", "select", "collection", "gacha", "souls", "top", "rename", "help");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("select") && sender instanceof Player player) {
                PlayerProfile profile = profile(player.getUniqueId());
                return profile.pets().keySet().stream().map(t -> t.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
            }
        } else if (command.getName().equalsIgnoreCase("petadmin")) {
            if (args.length == 1) return List.of("give", "xp", "souls", "reload");
            if (args.length == 2 && !args[0].equalsIgnoreCase("reload"))
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) return typeNames;
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------ events

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        removeBuffs(player);
        profile(player.getUniqueId());
        if (getConfig().getBoolean("settings.auto-summon-on-join", false)) {
            Bukkit.getScheduler().runTaskLater(this, () -> { if (player.isOnline()) summon(player, true); }, 20L);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dismiss(player, false);
        PlayerProfile profile = profiles.remove(player.getUniqueId());
        if (profile != null) storage.save(profile);
        skillCooldowns.remove(player.getUniqueId());
    }

    @EventHandler public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (isPet(entity) && !activePetByOwner.containsValue(entity.getUniqueId())) entity.remove();
        }
    }

    @EventHandler public void onInteractPet(PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();
        if (!isPet(target)) return;
        event.setCancelled(true);
        UUID owner = ownerOf(target);
        if (event.getHand() == EquipmentSlot.HAND && owner != null && owner.equals(event.getPlayer().getUniqueId())) {
            openMain(event.getPlayer());
        }
    }

    @EventHandler public void onPetTarget(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target == null) return;
        if (isPet(event.getEntity()) && target instanceof Player) event.setCancelled(true);
        if (isPet(target) && event.getEntity() instanceof Enemy) event.setCancelled(true);
    }

    @EventHandler public void onPetDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        if (isPet(victim) && (damager instanceof Player || isPet(damager))) event.setCancelled(true);
        if (isPet(damager) && (victim instanceof Player || isPet(victim))) event.setCancelled(true);
    }

    @EventHandler public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (isPet(dead)) {
            UUID owner = ownerOf(dead);
            if (owner != null) activePetByOwner.remove(owner);
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }
        if (!(dead instanceof Enemy)) return;
        Player killer = dead.getKiller();
        if (killer == null) return;
        PlayerProfile profile = profile(killer.getUniqueId());
        int souls = getConfig().getInt("settings.souls-per-kill", 5);
        profile.addSouls(souls);
        PetEntry entry = profile.active();
        String extra = "+" + souls + " Linh Hồn";
        if (entry != null) gainXp(killer, profile, entry, getConfig().getInt("settings.xp-per-monster-kill", 10), extra);
    }
}
