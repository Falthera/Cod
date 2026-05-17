package com.midknightgarden.bounty;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.quest.Quest;
import com.midknightgarden.storage.StorageService;
// using simple string broadcasts to avoid tight Adventure coupling here
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages bounty survival events: broadcasts, timer, bossbar, and victory/failure.
 */
public class BountyService implements Listener {
    private final MidknightGardenPlugin plugin;
    private final StorageService storage;
    private BossBar bossBar;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private UUID activeTarget;
    private String activeTargetName;
    private BukkitTask coordTask;
    private BukkitTask graceFailTask;

    public BountyService(MidknightGardenPlugin plugin, StorageService storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        if (bossBar != null) bossBar.removeAll();
        if (coordTask != null) coordTask.cancel();
        if (graceFailTask != null) graceFailTask.cancel();
    }

    public synchronized void startEvent(Quest quest, Player winner) {
        if (!active.compareAndSet(false, true)) return; // only one event at a time
        storage.markQuestComplete(quest.questId());
        activeTarget = winner.getUniqueId();
        activeTargetName = winner.getName();

        Bukkit.broadcastMessage("[MidknightGarden] Bounty started! " + winner.getName() + " must survive!");

        bossBar = Bukkit.createBossBar("Survive the bounty", BarColor.RED, BarStyle.SOLID);
        bossBar.addPlayer(winner);

        long durationSecs = plugin.getConfig().getLong("bounty.duration_seconds", 600);
        long interval = plugin.getConfig().getLong("bounty.broadcast_interval_seconds", 60);

        // periodic coordinate broadcast
        coordTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player target = Bukkit.getPlayer(activeTarget);
            if (target == null || !target.isOnline()) return;
            Bukkit.broadcastMessage("Bounty target: " + target.getName() + " at " + formatLoc(target));
        }, 0L, interval * 20L);

        // schedule success after duration
        new BukkitRunnable() {
            long left = durationSecs;
            @Override
            public void run() {
                Player target = Bukkit.getPlayer(activeTarget);
                if (target != null && target.isOnline()) {
                    bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, (double) left / (double) durationSecs)));
                }
                if (left <= 0) {
                    Player online = Bukkit.getPlayer(activeTarget);
                    if (online != null && online.isOnline()) {
                        succeedEvent(online);
                    } else {
                        failEvent(activeTargetName);
                    }
                    if (coordTask != null) coordTask.cancel();
                    cancel();
                    return;
                }
                left -= 1;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!active.get() || activeTarget == null) return;
        if (!event.getEntity().getUniqueId().equals(activeTarget)) return;
        failEvent(event.getEntity().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!active.get() || activeTarget == null) return;
        if (!event.getPlayer().getUniqueId().equals(activeTarget)) return;

        long graceSeconds = plugin.getConfig().getLong("bounty.reconnect_grace_seconds", 30L);
        Bukkit.broadcastMessage("[MidknightGarden] " + activeTargetName + " disconnected. Reconnect within " + graceSeconds + "s or fail.");
        if (graceFailTask != null) graceFailTask.cancel();
        graceFailTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(activeTarget);
            if (p == null || !p.isOnline()) failEvent(activeTargetName);
        }, graceSeconds * 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!active.get() || activeTarget == null) return;
        if (!event.getPlayer().getUniqueId().equals(activeTarget)) return;
        if (graceFailTask != null) {
            graceFailTask.cancel();
            graceFailTask = null;
        }
        if (bossBar != null) bossBar.addPlayer(event.getPlayer());
        Bukkit.broadcastMessage("[MidknightGarden] " + activeTargetName + " reconnected. Bounty continues.");
    }

    private void succeedEvent(Player winner) {
        Bukkit.broadcastMessage("[MidknightGarden] " + winner.getName() + " survived the bounty and wins!");
        if (bossBar != null) bossBar.removeAll();
        if (coordTask != null) coordTask.cancel();
        if (graceFailTask != null) graceFailTask.cancel();
        activeTarget = null;
        activeTargetName = null;
        active.set(false);
        // reward handling could be added here: run configured commands
    }

    private void failEvent(String winnerName) {
        Bukkit.broadcastMessage("[MidknightGarden] " + winnerName + " failed the bounty.");
        if (bossBar != null) bossBar.removeAll();
        if (coordTask != null) coordTask.cancel();
        if (graceFailTask != null) graceFailTask.cancel();
        activeTarget = null;
        activeTargetName = null;
        active.set(false);
    }

    private String formatLoc(Player p) {
        return p.getLocation().getWorld().getName() + " " + p.getLocation().getBlockX() + "," + p.getLocation().getBlockY() + "," + p.getLocation().getBlockZ();
    }
}
