package com.midknightgarden.commands;

import com.midknightgarden.quest.Quest;
import com.midknightgarden.services.ServiceManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class MidknightCommand implements CommandExecutor, TabCompleter {
    private final ServiceManager services;

    public MidknightCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("midknight.use")) {
            sender.sendMessage("You lack permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("MidknightGarden: use /midknight help");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sender.sendMessage("/midknight spawnvillager - spawn NPC (players only)");
                sender.sendMessage("/midknight reload - reload config");
                sender.sendMessage("/midknight givebook <player> - generate and give a signed quest book");
                sender.sendMessage("/midknight-verify <player> - verify quest book (admin)");
                return true;
            case "spawnvillager":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can spawn the villager.");
                    return true;
                }
                Player p = (Player) sender;
                Location loc = p.getLocation();
                services.villagerService.spawnVillager(loc, true, false, true, "Midknight Villager");
                p.sendMessage("Spawned Midknight Villager.");
                return true;
            case "reload":
                services.getPlugin().reloadConfig();
                services.storage.initialize();
                sender.sendMessage("MidknightGarden configs reloaded.");
                return true;
            case "givebook":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /midknight givebook <player>");
                    return true;
                }
                Player target = services.getPlugin().getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not online.");
                    return true;
                }

                Quest quest = services.questGenerator.generateQuest(target.getUniqueId());
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
                if (!(book.getItemMeta() instanceof BookMeta)) {
                    sender.sendMessage("Failed to create quest book.");
                    return true;
                }
                BookMeta meta = (BookMeta) book.getItemMeta();
                meta.setTitle("Midknight Request");
                meta.setAuthor("Villager");
                meta.addPage(
                    "A whisper from the End...\\n\\n" +
                    "Bring me:\\n" +
                    "- " + quest.items().get(0).name() + "\\n" +
                    "- " + quest.items().get(1).name() + "\\n" +
                    "- " + quest.items().get(2).name() + "\\n\\n" +
                    "Ask The Monkey"
                );
                book.setItemMeta(meta);

                try {
                    services.bookSigner.signBook(book, quest);
                    services.storage.saveQuest(quest);
                } catch (Exception ex) {
                    sender.sendMessage("Failed to sign quest book.");
                    return true;
                }

                target.getInventory().addItem(book);
                sender.sendMessage("Signed quest book generated for " + target.getName() + ".");
                target.sendMessage("You received a Midknight request book.");
                return true;
            default:
                sender.sendMessage("Unknown subcommand. Use /midknight help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("help");
            suggestions.add("spawnvillager");
            suggestions.add("reload");
            suggestions.add("givebook");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("givebook")) {
            for (Player p : services.getPlugin().getServer().getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
        }
        return suggestions;
    }
}
