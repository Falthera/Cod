package com.midknightgarden.quest;

import org.bukkit.Material;

import java.util.List;
import java.util.UUID;

/**
 * Immutable quest model.
 */
public class Quest {
    private final UUID questId;
    private final UUID owner;
    private final List<Material> items;
    private final long createdAt;

    public Quest(UUID questId, UUID owner, List<Material> items, long createdAt) {
        this.questId = questId;
        this.owner = owner;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
    }

    public UUID questId() { return questId; }
    public UUID owner() { return owner; }
    public List<Material> items() { return items; }
    public long createdAt() { return createdAt; }
}
