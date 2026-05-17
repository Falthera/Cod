package com.midknightgarden.quest;

import com.midknightgarden.MidknightGardenPlugin;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Simple, configurable quest generator.
 * In production this would read full configuration for weights/blacklists.
 */
public class QuestGenerator {
    private final MidknightGardenPlugin plugin;
    private final Random random = new Random();

    private final List<Material> pool = new ArrayList<>();

    public QuestGenerator(MidknightGardenPlugin plugin) {
        this.plugin = plugin;
        // curated default pool of obtainable items (safe across many versions)
        pool.add(Material.IRON_INGOT);
        pool.add(Material.GOLD_INGOT);
        pool.add(Material.DIAMOND);
        pool.add(Material.ENDER_PEARL);
        pool.add(Material.BLAZE_ROD);
        pool.add(Material.OBSIDIAN);
        pool.add(Material.ARROW);
        pool.add(Material.COOKED_BEEF);
        pool.add(Material.OAK_LOG);
    }

    public Quest generateQuest(UUID playerUuid) {
        List<Material> chosen = new ArrayList<>();
        while (chosen.size() < 3) {
            Material m = pool.get(random.nextInt(pool.size()));
            if (!chosen.contains(m)) chosen.add(m);
        }
        return new Quest(UUID.randomUUID(), playerUuid, chosen, System.currentTimeMillis());
    }
}
