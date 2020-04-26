package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.Optional;
import java.util.TreeMap;

public class RegexUnbanCommand extends RistExCommand
{
    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.RegexUnban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.regexunban"))
            return true;

        if (args.length < 2)
            return false;
        
        // Syntax: /unban [-s] <PlayerName|PunishID> <Reason>
        try 
        {
            boolean silent = args.length > 3 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            OfflinePlayer target = User.FindPlayerByAny(args[0]);
            
            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("RegexBan.RegexIsNotBanned", sender, target.getName(), true);

            // Preapre a statement
            // We need to get the latest banid first.
            Optional<Punishment> op = Punishment.FindPunishment(PunishmentType.PUNISH_BAN, target, false);
            if (!op.isPresent())
            {
                sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
            }

            Punishment punish = op.get();
            punish.SetAppealReason(reason);
            punish.SetAppealed(true);
            punish.SetAppealStaff(sender);
            punish.Commit(sender);

            // Prepare our announce message
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                //put("regex", bnyeh.pattern());
                //put("arbiter", sender.getName());
                //put("AFFECTEDPLAYERS", String.valueOf(fuckingfinal));
                //put("TOTALPLAYERS", String.valueOf(TotalOnline));
                //put("INSANEPERCENT", String.valueOf(percentage));
                //put("INSANETHRESHOLD", String.valueOf(sanepercent));
            }};
            
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("RegexBan.UnbanAnnouncment", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}