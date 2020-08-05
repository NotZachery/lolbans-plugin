/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.common.utils;

import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.utils.ServerType;

// This class caused great pain and suffering, please enjoy.
public class CommandUtil {

    public static class Bukkit {

        // Honestly this is so fucking stupid, because md_5 decided that PluginCommand should be protected, I am forced
        // to use reflection to unfuck it, just to create and register new commands...
        // At least now I can make command classes that are registerable by both bukkit and bungeecord.
        public static void registerBukkitCommand(AsyncCommand command) {
            if (!(LolBans.getServer() == ServerType.BUKKIT || LolBans.getServer() == ServerType.PAPER))
                return;

            com.ristexsoftware.lolbans.bukkit.Main bukkitPlugin = com.ristexsoftware.lolbans.bukkit.Main
                    .getPlugin(com.ristexsoftware.lolbans.bukkit.Main.class);

            Constructor<?> c = ReflectionUtil.getProtectedConstructor(org.bukkit.command.PluginCommand.class, String.class, org.bukkit.plugin.Plugin.class);

            org.bukkit.command.PluginCommand bukkitCmd;
            try {
                bukkitCmd = (org.bukkit.command.PluginCommand) c.newInstance(command.getName(), bukkitPlugin);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                e.printStackTrace();
                return;
            }
            if (bukkitCmd == null)
                return;

            bukkitCmd.setExecutor(new org.bukkit.command.CommandExecutor() {
                @Override
                public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command cmd,
                        String label, String[] args) {
                    User user = sender instanceof org.bukkit.command.ConsoleCommandSender ? User.getConsoleUser()
                            : User.resolveUser(((org.bukkit.entity.Player) sender).getUniqueId().toString());

                    return command.execute(user, label, args);
                }
            });

            bukkitCmd.setTabCompleter(new org.bukkit.command.TabCompleter() {
                @Override
                public List<String> onTabComplete(org.bukkit.command.CommandSender sender,
                        org.bukkit.command.Command cmd, String alias, String[] args) {
                    User user = sender instanceof org.bukkit.command.ConsoleCommandSender ? User.getConsoleUser()
                            : User.resolveUser(((org.bukkit.entity.Player) sender).getUniqueId().toString());
                    return command.onTabComplete(user, args);
                }
            });
            bukkitCmd.setAliases(command.getAliases());
            bukkitCmd.setDescription(command.getDescription());
            if (LolBans.getPlugin().getConfig().getBoolean("general.hidden-commands"))
                bukkitCmd.setPermission(command.getPermission());

            // MD_5 and his knobbery continues. the CraftServer.java class has a
            // `getCommandMap()`
            // method and CommandMap is documented but there's no reasonable way to get the
            // command
            // map from within the server. Because MD_5 couldn't help but program like a 12
            // year old
            // we now have to use reflection to get a more reasonable way to register
            // commands.
            org.bukkit.command.CommandMap cmap = ReflectionUtil.getProtectedValue(org.bukkit.Bukkit.getServer(),
                    "commandMap");
            cmap.register(bukkitPlugin.getName().toLowerCase(), bukkitCmd);
        }
    }

    public static class BungeeCord {

        // Look how stupid fucking easy this is compared to bukkit...
        public static void registerBungeeCommand(AsyncCommand command) {
            if (LolBans.getServer() != ServerType.BUNGEECORD)
                return;
            
            com.ristexsoftware.lolbans.bungeecord.Main plugin = com.ristexsoftware.lolbans.bungeecord.Main.getPlugin();

            net.md_5.bungee.api.plugin.Command cmd = new net.md_5.bungee.api.plugin.Command(command.getName(), command.getPermission(), command.getAliases().toArray(new String[0])) {
        

                public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
                    User user = (!(sender instanceof net.md_5.bungee.api.connection.ProxiedPlayer))
                    ? User.getConsoleUser()
                    : User.resolveUser(((net.md_5.bungee.api.connection.ProxiedPlayer) sender).getUniqueId().toString());

                    command.execute(user, command.getName(), args);
                }
                
            };
            
            net.md_5.bungee.api.ProxyServer.getInstance().getPluginManager().registerCommand(plugin, cmd);
        }
    }
}