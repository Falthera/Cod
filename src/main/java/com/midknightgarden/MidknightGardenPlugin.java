package com.midknightgarden;

import com.midknightgarden.commands.MidknightCommand;
import com.midknightgarden.commands.VerifyCommand;
import com.midknightgarden.services.ServiceManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MidknightGarden main plugin class.
 * Initializes core services and registers commands/listeners.
 */
public final class MidknightGardenPlugin extends JavaPlugin {

    private ServiceManager services;

    @Override
    public void onEnable() {
        // Initialize services (dependency injection style)
        saveDefaultConfig();
        this.services = new ServiceManager(this);
        this.services.initialize();

        // Register commands
        MidknightCommand midknightCommand = new MidknightCommand(services);
        this.getCommand("midknight").setExecutor(midknightCommand);
        this.getCommand("midknight").setTabCompleter(midknightCommand);
        this.getCommand("midknight-verify").setExecutor(new VerifyCommand(services));

        getLogger().info("MidknightGarden enabled");
    }

    @Override
    public void onDisable() {
        if (services != null) services.shutdown();
        getLogger().info("MidknightGarden disabled");
    }

    public ServiceManager services() {
        return services;
    }
}
