package com.lincoln.diary.commands;

import com.lincoln.diary.DiaryPlugin;
import com.lincoln.diary.item.DiaryItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiaryCommand implements CommandExecutor {

    private final DiaryPlugin plugin;

    public DiaryCommand(DiaryPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("diary.admin")) {
            s.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            s.sendMessage("§e/diary reload");
            s.sendMessage("§e/diary issue <player> §7- mint only if never issued");
            s.sendMessage("§e/diary status <player> §7- show issued state & diary id");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.configManager().reload();
                plugin.diaryStore().save();
                s.sendMessage(plugin.configManager().msg("reload-complete"));
                return true;
            }

            case "issue" -> {
                if (args.length < 2) {
                    s.sendMessage("§cUsage: /diary issue <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    s.sendMessage("§cPlayer must be online to issue.");
                    return true;
                }
                UUID id = target.getUniqueId();

                if (plugin.diaryStore().hasIssued(id) && plugin.diaryStore().getId(id) != null) {
                    s.sendMessage("§eThey have already been issued a diary. (One-time only)");
                    return true;
                }

                var minted = DiaryItem.mintFor(target);
                var leftovers = target.getInventory().addItem(minted);
                if (!leftovers.isEmpty()) {
                    plugin.deliveryService().enqueue(id, minted);
                }
                plugin.diaryStore().markIssued(id);
                s.sendMessage("§aIssued a diary to §e" + target.getName() + "§a.");
                return true;
            }

            case "status" -> {
                if (args.length < 2) {
                    s.sendMessage("§cUsage: /diary status <player>");
                    return true;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                UUID puid = op.getUniqueId();
                boolean issued = plugin.diaryStore().hasIssued(puid);
                String diaryId = plugin.diaryStore().getId(puid);
                s.sendMessage("§ePlayer: §f" + (op.getName() != null ? op.getName() : puid));
                s.sendMessage("§eIssued: §f" + issued);
                s.sendMessage("§eDiary ID: §f" + (diaryId != null ? diaryId : "none"));
                return true;
            }

            default -> {
                s.sendMessage("§cUnknown subcommand. Try /diary");
                return true;
            }
        }
    }
}
