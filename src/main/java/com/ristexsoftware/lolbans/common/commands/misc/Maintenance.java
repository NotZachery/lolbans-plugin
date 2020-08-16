package com.ristexsoftware.lolbans.common.commands.misc;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.MaintenanceLevel;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;

public class Maintenance extends AsyncCommand {

    public Maintenance(LolBans plugin) {
        super("maintenace", plugin);
        setDescription("set the maintenance mode of the server");
        setPermission("lolbans.maintenance");
    }
    // -k flag = kick players
    // /maintenance [-k] [level]
    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(Messages.invalidSyntax);
        try {
            sender.sendMessage(
                    Messages.translate("syntax.maintenance", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        if (args.length < 2)
            return Arrays.asList("lowest", "low", "high", "highest");
        return Arrays.asList();
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) throws Exception {
        
        Arguments a = new Arguments(args);
        a.optionalString("level");
        a.optionalFlag("kickPlayers", "-k");
        a.optionalFlag("silent", "-s");
        
        if (!a.valid())
            return false;

        String level = a.get("level");
        if (level == null) {
            if (!sender.hasPermission("lolbans.maintenance.toggle"))
                return sender.permissionDenied("lolbans.maintenance.toggle");

            getPlugin().setMaintenanceModeEnabled(!getPlugin().getMaintenanceModeEnabled());
            getPlugin().broadcastEvent(Messages.translate("maintenance.toggled", new TreeMap<String, String>(){{
                put("maintenancelevel", getPlugin().getMaintenanceLevel().displayName());
                put("player", sender.getName());
                put("enabled", String.valueOf(getPlugin().getMaintenanceModeEnabled()));
            }}), a.getFlag("silent"));
            return true;
        }
        
        if (!sender.hasPermission("lolbans.maintenance.setlevel"))
            return sender.permissionDenied("lolbans.maintenance.setlevel");

        MaintenanceLevel exactLevel = MaintenanceLevel.valueOf(level.toUpperCase()); // level is null here.
        if (exactLevel == null)
            return false;

        getPlugin().setMaintenanceLevel(exactLevel);
        getPlugin().broadcastEvent(Messages.translate("maintenance.toggled", new TreeMap<String, String>(){{
            put("maintenancelevel", getPlugin().getMaintenanceLevel().displayName());
            put("player", sender.getName());
            put("enabled", String.valueOf(getPlugin().getMaintenanceModeEnabled()));
        }}), a.getFlag("silent"));
        if (a.exists("kickPlayers"))
            kickAppropriatePlayers();

        return true;
    }

    /**
     * On change of the maintenance mode state
     */
    public void kickAppropriatePlayers() {
        if (!getPlugin().getMaintenanceModeEnabled())
            return;

        int level = getPlugin().getMaintenanceLevel().ordinal();

        for (User user : getPlugin().getOnlineUsers()) {
            boolean hasPermission = false;
            for (int i = MaintenanceLevel.HIGHEST.ordinal(); i >= level; i--) {
                if (user.hasPermission("lolbans.maintenance." + MaintenanceLevel.fromOrdinal(i).displayName())) {
                    hasPermission = true;
                    break;
                }
            }

            if (!hasPermission)
                try {
                    user.disconnect(Messages.translate("maintenance.kick-message",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("maintenancelevel", MaintenanceLevel.fromOrdinal(level).displayName());
                                }
                            }));
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                    user.disconnect(Messages.serverError);
                }
        }
    }
}