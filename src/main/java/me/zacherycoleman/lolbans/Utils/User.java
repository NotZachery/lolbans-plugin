package me.zacherycoleman.lolbans.Utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Main;

public class User
{
    Main self = Main.getPlugin(Main.class);
    
    private Player pl;

    public User(Player pl)
    {
        this.pl = pl;
    }

    public Player getPlayer()
    {
        return this.pl;
    }

    public Location getLocation()
    {
        return this.pl.getLocation();
    }

    public String getName()
    {
        return this.pl.getName();
    }

    // All voids after this.

    public void sendMessage(String message)
    {
        this.pl.sendMessage(message);
    }

}