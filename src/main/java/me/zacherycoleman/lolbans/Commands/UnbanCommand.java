package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;

import java.sql.*;
import java.util.Arrays;

public class UnbanCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.unban") || ((Player)sender).isOp())));
        
        if (command.getName().equalsIgnoreCase("unban"))
        {
            if (SenderHasPerms)
            {
                try 
                {
                    // just incase someone, magically has a 1 char name........
                    if (!(args.length < 2 || args == null))
                    {
                        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                        reason = reason.replace(",", "");
                        OfflinePlayer target = self.FindPlayerByBanID(args[0]);

                        if (target == null)
                        {
                            sender.sendMessage(String.format("Player/ID \"%s\" does not exist!", target));
                            return true;
                        }

                        if (!self.IsPlayerBanned(target))
                        {
                            sender.sendMessage(String.format(ChatColor.RED + "Player/ID \"%s\" is not banned", target));
                            return true;
                        }
                    
                        // Prepare our reason for unbanning
                        boolean silent = reason.contains("-s");
                        reason = reason.replace("-s", "").trim();

                        // Preapre a statement
                        // We need to get the latest banid first.
                        PreparedStatement pst2 = self.connection.prepareStatement("UPDATE BannedHistory INNER JOIN (SELECT BanID AS LatestBanID, UUID as bUUID FROM BannedPlayers WHERE UUID = ?) tm SET UnbanReason = ?, UnbanExecutioner = ? WHERE UUID = tm.bUUID AND BanID = tm.LatestBanID");
                        pst2.setString(1, target.getUniqueId().toString());
                        pst2.setString(2, reason);
                        pst2.setString(3, sender.getName());
                        pst2.executeUpdate();

                        // Preapre a statement
                        PreparedStatement pst = self.connection.prepareStatement("DELETE FROM BannedPlayers WHERE UUID = ?");
                        pst.setString(1, target.getUniqueId().toString());
                        pst.executeUpdate();

                        // Log to console.
                        Bukkit.getConsoleSender().sendMessage(String.format("\u00A7c%s \u00A77has unbanned \u00A7c%s\u00A77: \u00A7c%s\u00A77%s\u00A7r", 
                        sender.getName(), target.getName(), reason, (silent ? " [silent]" : "")));

                        // Send to Discord.
                        DiscordUtil.Send(":hammer: **%s** unbanned **%s** for: **%s**%s", sender.getName(), target.getName(), reason, (silent ? " [silent]" : ""));

                        // Post that to the database.
                        for (Player p : Bukkit.getOnlinePlayers())
                        {
                            if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                                continue;

                            p.sendMessage(String.format("\u00A7c%s \u00A77has unbanned \u00A7c%s\u00A77: \u00A7c%s\u00A77%s\u00A7r", 
                                                        sender.getName(), target.getName(), reason, (silent ? " [silent]" : "")));
                        }
                        return true;
                    }
                    else
                    {
                        sender.sendMessage("\u00A7CInvalid Syntax!");
                        return false; // Show syntax.
                    }
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                    sender.sendMessage("\u00A7CThe server encountered an error, please try again later.");
                    return true;
                }
            }
            // They're denied perms, just return.
            return true;
        }
        // Invalid command.
        return false;
    }
}