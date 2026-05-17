package com.midknightgarden.services;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.bounty.BountyService;
import com.midknightgarden.commands.VerifyCommand;
import com.midknightgarden.npc.VillagerService;
import com.midknightgarden.quest.QuestGenerator;
import com.midknightgarden.security.BookSigner;
import com.midknightgarden.storage.StorageService;
import com.midknightgarden.storage.sql.SQLiteStorage;
import org.bukkit.plugin.PluginLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized service registry and lifecycle manager.
 */
public class ServiceManager {
    private final MidknightGardenPlugin plugin;
    private final ExecutorService ioExecutor;

    public final BookSigner bookSigner;
    public final QuestGenerator questGenerator;
    public final VillagerService villagerService;
    public final BountyService bountyService;
    public final StorageService storage;

    public ServiceManager(MidknightGardenPlugin plugin) {
        this.plugin = plugin;
        this.ioExecutor = Executors.newFixedThreadPool(2);

        // Initialize components with DI-friendly constructors
        this.bookSigner = new BookSigner(plugin);
        this.questGenerator = new QuestGenerator(plugin);
        this.storage = new SQLiteStorage(plugin, ioExecutor);
        this.villagerService = new VillagerService(plugin, bookSigner, questGenerator);
        this.bountyService = new BountyService(plugin, storage);
    }

    public void initialize() {
        storage.initialize();
        villagerService.initialize();
        bountyService.initialize();
    }

    public void shutdown() {
        try {
            villagerService.shutdown();
            bountyService.shutdown();
            storage.shutdown();
            ioExecutor.shutdownNow();
        } catch (Exception ex) {
            plugin.getLogger().severe("Error shutting down services: " + ex.getMessage());
        }
    }

    public MidknightGardenPlugin getPlugin() { return plugin; }
}
