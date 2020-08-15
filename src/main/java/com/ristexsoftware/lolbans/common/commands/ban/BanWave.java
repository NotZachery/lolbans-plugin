package com.ristexsoftware.lolbans.common.commands.ban;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.google.common.collect.ImmutableList;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.runnables.BanwaveRunnable;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.common.utils.Debug;

public class BanWave extends AsyncCommand {

    public BanWave(LolBans plugin) {
        super("banwave", plugin);
        setDescription("Manages ");
        setPermission("lolbans.banwave");
        setAliases(Arrays.asList(new String[] { "eban", "tempban" }));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(Messages.invalidSyntax);
        try {
            sender.sendMessage(
                    Messages.translate("syntax.ban-wave", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        
        if (args.length < 2) {
            return Arrays.asList("add", "remove", "execute");
        }

        if (args.length < 3) {

            switch (args[0]) {
                case "add":
                case "remove":
                    ArrayList<String> usernames = new ArrayList<>();
                    if(!args[0].equals("")) {
                        for(User user : LolBans.getPlugin().getUserCache().getAll()) {
                            if(user.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                                usernames.add(user.getName());
                            }
                        }
                    } else {
                        // Instead of creating a stupid for loop here, let's just stream 
                        usernames = (ArrayList<String>) LolBans.getPlugin().getUserCache().getAll().stream()
                        .map(user -> user.getName())
                        .collect(Collectors.toList());
                    }
    
                    return usernames;
                default:
                    return Arrays.asList();
            }
        }

        if (args.length < 4) {
            switch (args[0]) {
                case "add":
                case "remove":
                    return ImmutableList.of("1m", "15m", "1h", "3h", "12h", "1d", "1w", "1mo", "1y");
                default:
                    return Arrays.asList();
                }
        }

        return Arrays.asList();
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        if (!sender.hasPermission("lolbans.banwave"))
            return sender.permissionDenied("lolbans.banwave");
        
        Arguments a = new Arguments(args);
        a.optionalFlag("silent", "-s");
        a.requiredString("subCommand");

        if (!a.valid())
            return false;

        boolean silent = a.getBoolean("silent");
        String subCommand = a.get("subCommand").toLowerCase();
        String[] subArgs = Arrays.copyOfRange(a.getUnparsedArgs().toArray(new String[a.getUnparsedArgs().size()]), 1, a.getUnparsedArgs().size());

        switch (subCommand) {
            case "add":
                return add(sender, subArgs, silent);
            case "remove":
                return remove(sender, subArgs, silent);
            case "execute":
                return execute(sender, subArgs, silent);
            default:
                return false;
        }
    }

    /**
     * Add a user to the next banwave.
     */
    public boolean add(User sender, String[] args, boolean silent) {
        Debug debug = new Debug(getClass());

        if (!sender.hasPermission("lolbans.banwave.add"))
            return sender.permissionDenied("lolbans.banwave.add");

        try {
            Arguments a = new Arguments(args);
            a.requiredString("target");
            a.optionalTimestamp("expiry");
            a.requiredSentence("reason");

            if (!a.valid())
                return false;

            User target = User.resolveUser(a.get("target"));
            if (target == null) 
                return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("target"), true);
            
            if (Punishment.findPunishment(PunishmentType.BANWAVE, target.getUniqueId().toString(), false) != null) 
                return sender.sendReferencedLocalizedMessage("ban-wave.player-is-in-wave", target.getName(), true);
        
            if (target.isPunished(PunishmentType.BAN))
                return sender.sendReferencedLocalizedMessage("ban.player-is-banned", target.getName(), true);
                
            Punishment punishment = new Punishment(PunishmentType.BANWAVE, sender, target, a.get("reason"),
                    a.getTimestamp("expiry"), silent, false);
            
            LolBans.getPlugin().getPunishmentCache().put(punishment);
            punishment.commit(sender);
            punishment.broadcast();

            debug.print("Command execution complete");

        } catch(Exception e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
        return true;
    }

    /**
     * Remove a user from the next banwave.
     */
    public boolean remove(User sender, String[] args, boolean silent) {
        Debug debug = new Debug(getClass());

        if (!sender.hasPermission("lolbans.banwave.remove"))
            return sender.permissionDenied("lolbans.banwave.remove");

        try {
            Arguments a = new Arguments(args);
            a.requiredString("target");
            a.requiredSentence("reason");

            if (!a.valid())
                return false;

            User target = User.resolveUser(a.get("target"));
            if (target == null) {
                return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("target"), true);
            }
                    
            
            Punishment punishment = Punishment.findPunishment(PunishmentType.BANWAVE, target.getUniqueId().toString(), false);

            if (punishment == null) {
                return sender.sendReferencedLocalizedMessage("ban-wave.player-not-in-wave", target.getName(), true);
            }

            punishment.appeal(sender, a.get("reason"), silent);
            punishment.broadcast();

            debug.print("Command execution complete");

        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
        
        return true;
    }
    
    /**
     * Execute the next banwave.
     */
    public boolean execute(User sender, String[] args, boolean silent) {
        if (!sender.hasPermission("lolbans.banwave.execute"))
            return sender.permissionDenied("lolbans.banwave.execute");

        try {
            PreparedStatement banwaveQuery = Database.getConnection().prepareStatement("SELECT * FROM lolbans_punishments WHERE type = 6 AND appealed = false LIMIT 1");
            ResultSet res = banwaveQuery.executeQuery();

            if (!res.isBeforeFirst()) {
                return sender.sendReferencedLocalizedMessage("ban-wave.empty-wave", sender.getName(), true);
            }

            LolBans.getPlugin().getPool().submit(new BanwaveRunnable());
            sender.sendReferencedLocalizedMessage("ban-wave.wave-start", sender.getName(), true);
        } catch(Exception e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }

        return true;
    }
}