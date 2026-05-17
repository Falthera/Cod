package com.midknightgarden.commands;

import com.midknightgarden.services.ServiceManager;
import com.midknightgarden.quest.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Admin verification command to validate and complete quests.
 */
public class VerifyCommand implements CommandExecutor {
    private final ServiceManager services;

    public VerifyCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("midknight.verify")) {
            sender.sendMessage("You lack permission.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Usage: /midknight-verify <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("Player not online.");
            return true;
        }

        // Find a signed book in player's inventory
        ItemStack found = null;
        for (ItemStack is : target.getInventory().getContents()) {
            if (is == null) continue;
            if (is.getType() == Material.WRITTEN_BOOK && is.getItemMeta() instanceof BookMeta) {
                found = is; break;
            }
        }
        if (found == null) {
            sender.sendMessage("No quest book found in player's inventory.");
            return true;
        }

        final ItemStack questBook = found;
        java.util.Optional<java.util.UUID> maybeId = services.bookSigner.extractQuestId(questBook);
        if (maybeId.isEmpty()) {
            sender.sendMessage("Book appears invalid or unsigned.");
            return true;
        }
        java.util.UUID questId = maybeId.get();

        // fetch quest from storage and verify
        services.storage.getQuestByIdAsync(questId, maybeQuest -> {
            if (maybeQuest.isEmpty()) {
                sender.sendMessage("No matching quest found for that book.");
                return;
            }
            Quest quest = maybeQuest.get();
            // verify signature
            boolean ok = services.bookSigner.verifyBook(questBook, quest);
            if (!ok) {
                sender.sendMessage("Book signature invalid. Possible forgery.");
                return;
            }

            // verify ownership
            if (!quest.owner().equals(target.getUniqueId())) {
                sender.sendMessage("This book is not owned by the player.");
                return;
            }

            // verify required items exist in inventory
            boolean hasAll = true;
            for (org.bukkit.Material m : quest.items()) {
                if (target.getInventory().contains(m, 1)) continue;
                hasAll = false; break;
            }
            if (!hasAll) {
                sender.sendMessage("Player does not possess all required items.");
                return;
            }

            // remove items and mark quest complete atomically on main thread
            Bukkit.getScheduler().runTask(services.getPlugin(), () -> {
                for (org.bukkit.Material m : quest.items()) target.getInventory().removeItem(new ItemStack(m, 1));
                services.storage.markQuestComplete(quest.questId());
                services.bountyService.startEvent(quest, target);
                sender.sendMessage("Quest verified and bounty event started for " + target.getName());
            });
        });
        return true;
    }
}
