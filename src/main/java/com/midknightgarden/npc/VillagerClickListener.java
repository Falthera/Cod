package com.midknightgarden.npc;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.quest.Quest;
import com.midknightgarden.quest.QuestGenerator;
import com.midknightgarden.security.BookSigner;
import com.midknightgarden.storage.StorageService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.stream.Collectors;

public class VillagerClickListener implements Listener {
    private final MidknightGardenPlugin plugin;
    private final BookSigner bookSigner;
    private final QuestGenerator generator;
    private final StorageService storage;

    public VillagerClickListener(MidknightGardenPlugin plugin, BookSigner bookSigner, QuestGenerator generator, StorageService storage) {
        this.plugin = plugin;
        this.bookSigner = bookSigner;
        this.generator = generator;
        this.storage = storage;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager)) return;
        Villager v = (Villager) clicked;

        NamespacedKey key = new NamespacedKey(plugin, "midknight_villager");
        if (!v.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Prevent normal interaction (trading GUI)
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Generate a fresh quest for this player
        Quest quest = generator.generateQuest(player.getUniqueId());

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        if (!(book.getItemMeta() instanceof BookMeta)) {
            player.sendMessage("Failed to create quest book.");
            return;
        }
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Midknight Request");
        meta.setAuthor("Midknight");

        String page = buildQuestPage(quest);
        meta.addPage(page);
        book.setItemMeta(meta);

        try {
            bookSigner.signBook(book, quest);
            storage.saveQuest(quest);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to sign or save quest: " + ex.getMessage());
            player.sendMessage("An error occurred creating your quest. Try again later.");
            return;
        }

        player.getInventory().addItem(book);
        player.sendMessage("You received a Midknight request book.");
    }

    private String buildQuestPage(Quest quest) {
        String items = quest.items().stream()
                .map(Material::name)
                .map(n -> n.replace('_', ' ').toLowerCase())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .map(s -> "- " + s)
                .collect(Collectors.joining("\n"));

        return "A whisper from the End...\n\n" +
                "Bring me:\n" +
                items +
                "\n\nAsk the Monkey.";
    }
}
