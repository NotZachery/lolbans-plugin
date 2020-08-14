package com.ristexsoftware.lolbans.common.commands.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;

public class Kick extends AsyncCommand {

    public Kick(LolBans plugin) {
        super("kick", plugin);
        setDescription("kick a player");
        setPermission("lolbans.kick");
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(Messages.invalidSyntax);
        try {
            sender.sendMessage(
                    Messages.translate("syntax.kick", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        if (args.length < 2) {
            ArrayList<String> usernames = new ArrayList<>();
            for (User user : LolBans.getPlugin().getUserCache().getAll()) {
                usernames.add(user.getName());
            }
            return usernames;
        }

        return Arrays.asList();
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) throws Exception {
        if (!sender.hasPermission("lolbans.kick"))
            return sender.permissionDenied("lolbans.kick");
        
        Arguments a = new Arguments(args);
        a.optionalFlag("silent", "-s");
        a.requiredString("username");
        a.optionalSentence("reason"); 

        if (!a.valid())
            return false;

        User target = User.resolveUser(a.get("username"));
        if (target == null)
            return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("username"), true);


        Punishment punishment = new Punishment(PunishmentType.KICK, sender, target, a.get("reason"), a.getTimestamp("expiry"), a.getFlag("silent"), false);
        punishment.commit(sender);
        punishment.broadcast();

        if (target.isOnline())
            target.disconnect(punishment);
        
        return true;
    }
}