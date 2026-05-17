package com.midknightgarden.storage.sql;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.quest.Quest;
import com.midknightgarden.storage.StorageService;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Lightweight SQLite storage using a background executor.
 */
public class SQLiteStorage implements StorageService {
    private final MidknightGardenPlugin plugin;
    private final ExecutorService ioExecutor;
    private Connection conn;

    public SQLiteStorage(MidknightGardenPlugin plugin, ExecutorService ioExecutor) {
        this.plugin = plugin;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public void initialize() {
        ioExecutor.execute(() -> {
            try {
                conn = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/midknight.db");
                try (Statement s = conn.createStatement()) {
                    s.execute("CREATE TABLE IF NOT EXISTS quests(quest_id TEXT PRIMARY KEY, owner TEXT, items TEXT, created_at INTEGER, completed INTEGER DEFAULT 0);");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize SQLite: " + e.getMessage());
            }
        });
    }

    @Override
    public void shutdown() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }

    @Override
    public void saveQuest(Quest quest) {
        ioExecutor.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO quests(quest_id, owner, items, created_at, completed) VALUES(?,?,?,?,0);")) {
                ps.setString(1, quest.questId().toString());
                ps.setString(2, quest.owner().toString());
                ps.setString(3, String.join(",", quest.items().stream().map(Material::name).toList()));
                ps.setLong(4, quest.createdAt());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save quest: " + e.getMessage());
            }
        });
    }

    @Override
    public void getQuestByIdAsync(UUID id, Consumer<Optional<Quest>> callback) {
        ioExecutor.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT owner, items, created_at, completed FROM quests WHERE quest_id = ? LIMIT 1;")) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { callback.accept(Optional.empty()); return; }
                    UUID owner = UUID.fromString(rs.getString("owner"));
                    String itemsCsv = rs.getString("items");
                    long created = rs.getLong("created_at");
                    List<Material> items = new ArrayList<>();
                    if (itemsCsv != null && !itemsCsv.isEmpty()) {
                        for (String s : itemsCsv.split(",")) {
                            try { items.add(Material.valueOf(s)); } catch (Exception ignored) {}
                        }
                    }
                    Quest q = new Quest(id, owner, items, created);
                    callback.accept(Optional.of(q));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to fetch quest: " + e.getMessage());
                callback.accept(Optional.empty());
            }
        });
    }

    @Override
    public void markQuestComplete(UUID id) {
        ioExecutor.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE quests SET completed = 1 WHERE quest_id = ?;")) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to mark quest complete: " + e.getMessage());
            }
        });
    }
}
