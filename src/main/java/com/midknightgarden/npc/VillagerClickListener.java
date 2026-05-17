package com.midknightgarden.npc;

import com.midknightgarden.MidknightGardenPlugin;
import com.midknightgarden.bounty.BountyService;
import com.midknightgarden.quest.Quest;
import com.midknightgarden.quest.QuestGenerator;
import com.midknightgarden.security.BookSigner;
import com.midknightgarden.storage.StorageService;
import org.bukkit.Bukkit;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class VillagerClickListener implements Listener {
    private final MidknightGardenPlugin plugin;
    private final BookSigner bookSigner;
    private final QuestGenerator generator;
    private final StorageService storage;
    private final BountyService bountyService;

    public VillagerClickListener(MidknightGardenPlugin plugin, BookSigner bookSigner, QuestGenerator generator, StorageService storage, BountyService bountyService) {
        this.plugin = plugin;
        this.bookSigner = bookSigner;
        this.generator = generator;
        this.storage = storage;
        this.bountyService = bountyService;
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

        ItemStack questBook = findQuestBook(player);
        if (questBook != null) {
            handleQuestTurnIn(player, questBook);
            return;
        }

        giveQuestBook(player);
    }

    private void handleQuestTurnIn(Player player, ItemStack questBook) {
        Optional<java.util.UUID> maybeId = bookSigner.extractQuestId(questBook);
        if (maybeId.isEmpty()) {
            player.sendMessage("That quest book looks invalid.");
            return;
        }

        java.util.UUID questId = maybeId.get();
        storage.getQuestByIdAsync(questId, maybeQuest -> {
            if (maybeQuest.isEmpty()) {
                player.sendMessage("No matching quest was found for that book.");
                return;
            }

            Quest quest = maybeQuest.get();
            if (!bookSigner.verifyBook(questBook, quest)) {
                player.sendMessage("That book has been forged or altered.");
                return;
            }

            if (!quest.owner().equals(player.getUniqueId())) {
                player.sendMessage("This quest book is not yours.");
                return;
            }

            if (!hasAllItems(player, quest)) {
                player.sendMessage("You do not have all of the required items yet.");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                removeQuestItems(player, quest);
                removeQuestBook(player, questBook);
                storage.markQuestComplete(quest.questId());
                bountyService.startEvent(quest, player);
                player.sendMessage("The villager accepts your offering.");
            });
        });
    }

    private void giveQuestBook(Player player) {
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

    private ItemStack findQuestBook(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && item.getType() == Material.WRITTEN_BOOK && item.getItemMeta() instanceof BookMeta)
                .filter(item -> bookSigner.extractQuestId(item).isPresent())
                .findFirst()
                .orElse(null);
    }

    private boolean hasAllItems(Player player, Quest quest) {
        return quest.items().stream().allMatch(material -> player.getInventory().contains(material, 1));
    }

    private void removeQuestItems(Player player, Quest quest) {
        for (Material material : quest.items()) {
            player.getInventory().removeItem(new ItemStack(material, 1));
        }
    }

    private void removeQuestBook(Player player, ItemStack questBook) {
        player.getInventory().removeItem(questBook);
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
