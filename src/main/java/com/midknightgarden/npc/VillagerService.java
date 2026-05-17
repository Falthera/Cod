package com.midknightgarden.npc;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.quest.QuestGenerator;
import com.midknightgarden.security.BookSigner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Manages the custom villager NPC used to distribute quests.
 */
public class VillagerService {
    private final MidknightGardenPlugin plugin;
    private final BookSigner bookSigner;
    private final QuestGenerator generator;

    public VillagerService(MidknightGardenPlugin plugin, BookSigner bookSigner, QuestGenerator generator) {
        this.plugin = plugin;
        this.bookSigner = bookSigner;
        this.generator = generator;
    }

    public void initialize() {
        // Load persistent villagers from storage in future iterations
    }

    public void shutdown() {
        // cleanup if necessary
    }

    public Villager spawnVillager(Location loc, boolean invulnerable, boolean silent, boolean noAI, String displayName) {
        Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        v.setInvulnerable(invulnerable);
        v.setAI(!noAI);
        v.setSilent(silent);
        v.setCustomName(displayName);
        v.setCustomNameVisible(true);

        // mark as Midknight villager for identification
        NamespacedKey key = new NamespacedKey(plugin, "midknight_villager");
        v.getPersistentDataContainer().set(key, PersistentDataType.STRING, UUID.randomUUID().toString());

        // prevent trading
        v.setProfession(org.bukkit.entity.Villager.Profession.NITWIT);

        // schedule a gentle particle/sound emitter for immersion
        new BukkitRunnable() {
            @Override
            public void run() {
                if (v.isDead() || !v.isValid()) cancel();
                v.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, v.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.01);
            }
        }.runTaskTimer(plugin, 20L, 60L);

        return v;
    }
}
