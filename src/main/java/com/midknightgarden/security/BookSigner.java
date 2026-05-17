package com.midknightgarden.security;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.quest.Quest;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Signs and verifies quest books using HMAC-SHA256 stored in the book PDC.
 * Uses plugin secret configured in config.yml.
 */
public class BookSigner {
    private final MidknightGardenPlugin plugin;
    private final NamespacedKey sigKey;
    private final NamespacedKey questKey;

    public BookSigner(MidknightGardenPlugin plugin) {
        this.plugin = plugin;
        this.sigKey = new NamespacedKey(plugin, "mg_sig");
        this.questKey = new NamespacedKey(plugin, "mg_quest_id");
    }

    private String secret() {
        return plugin.getConfig().getString("security.secret", "replace_this_with_secret");
    }

    public void signBook(ItemStack book, Quest quest) throws Exception {
        if (!(book.getItemMeta() instanceof BookMeta)) return;
        BookMeta meta = (BookMeta) book.getItemMeta();

        String payload = buildPayload(quest);
        String signature = hmacSha256Base64(payload, secret());

        meta.getPersistentDataContainer().set(questKey, PersistentDataType.STRING, quest.questId().toString());
        meta.getPersistentDataContainer().set(sigKey, PersistentDataType.STRING, signature);
        book.setItemMeta(meta);
    }

    public boolean verifyBook(ItemStack book, Quest expected) {
        if (!(book.getItemMeta() instanceof BookMeta)) return false;
        BookMeta meta = (BookMeta) book.getItemMeta();
        String storedId = meta.getPersistentDataContainer().get(questKey, PersistentDataType.STRING);
        String storedSig = meta.getPersistentDataContainer().get(sigKey, PersistentDataType.STRING);
        if (storedId == null || storedSig == null) return false;
        if (!storedId.equals(expected.questId().toString())) return false;
        String payload = buildPayload(expected);
        try {
            return storedSig.equals(hmacSha256Base64(payload, secret()));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to verify book signature: " + e.getMessage());
            return false;
        }
    }

    public java.util.Optional<java.util.UUID> extractQuestId(ItemStack book) {
        if (!(book.getItemMeta() instanceof BookMeta)) return java.util.Optional.empty();
        BookMeta meta = (BookMeta) book.getItemMeta();
        String storedId = meta.getPersistentDataContainer().get(questKey, PersistentDataType.STRING);
        if (storedId == null) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(java.util.UUID.fromString(storedId));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private String buildPayload(Quest q) {
        StringBuilder sb = new StringBuilder();
        sb.append(q.questId().toString()).append("|").append(q.owner().toString()).append("|").append(q.createdAt());
        q.items().forEach(i -> sb.append('|').append(i.name()));
        return sb.toString();
    }

    private String hmacSha256Base64(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }
}
