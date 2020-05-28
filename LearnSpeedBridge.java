package me.prostedeni.goodcraft.learnspeedbridge;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;

import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import java.util.HashMap;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class LearnSpeedBridge extends JavaPlugin implements Listener
{
    HashMap<Player, Location> old;

    ArrayList<String> sPlayers = new ArrayList<>();

    public LearnSpeedBridge() {
        this.old = new HashMap<Player, Location>();
    }

    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, (Runnable)new Runnable() {
            @Override
            public void run() {
                for (final Player p : Bukkit.getOnlinePlayers()) {
                    if (new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() - 1.0, p.getLocation().getZ()).getBlock().getType() != Material.AIR) {
                        LearnSpeedBridge.this.old.put(p, new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() + 1.0, p.getLocation().getZ(), p.getLocation().getYaw(), p.getLocation().getPitch()));
                    }
                }
            }
        }, 2L, 2L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                if (getConfig().getBoolean("onoff") || !(getConfig().getBoolean("onoff"))){
                    getConfig().set("onoff", null);
                }
            }
        }, 20L);
    }

    public void onDisable() {
        saveConfig();
    }

    @EventHandler
    public void a(final PlayerMoveEvent e) {
        int fallheight = getConfig().getInt("fallheight");
        if (this.old.get(e.getPlayer()).getWorld() != e.getTo().getWorld()) {
            return;
        }
        e.getPlayer().setFallDistance(0.0f);
        if (e.getTo().getBlockY() < this.old.get(e.getPlayer()).getBlockY() - (fallheight + 1)) {
            if (sPlayers.size() != 0){
                if (sPlayers.contains(e.getPlayer().getName())) {
                    if (e.getPlayer().hasPermission("speedbridge.use")) {
                        if (!e.getPlayer().isFlying()) {
                            if (e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                                e.getPlayer().teleport((Location) this.old.get(e.getPlayer()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equals("speedbridge")) {
            if (sender.hasPermission("speedbridge.cmd")) {
                if (args.length == 0) {
                    if (sPlayers.contains(sender.getName())) {
                        sPlayers.remove(sender.getName());
                        sender.sendMessage(ChatColor.DARK_RED + "SpeedBridge has been turned off");
                    } else if (!(sPlayers.contains(sender.getName()))){
                        sPlayers.add(sender.getName());
                        sender.sendMessage(ChatColor.DARK_GREEN + "SpeedBridge has been turned on");
                    }
                }
                if (args.length == 1) {
                    if (args[0].equals("reload")) {
                        reloadConfig();
                        saveConfig();
                        sender.sendMessage(ChatColor.DARK_GREEN + "Config reloaded");
                        System.out.println("Config reloaded");
                    } else {
                        int fallheight = getConfig().getInt("fallheight");
                        int setFallHeight = Integer.parseInt(args[0]);
                        if (fallheight == setFallHeight){
                            sender.sendMessage(ChatColor.DARK_RED + "Fall height already is set to " + fallheight);
                        } else {
                            if (setFallHeight < 1) {
                                sender.sendMessage(ChatColor.DARK_RED + "Fall height cannot be below 1");
                            }
                            if (setFallHeight >= 1 && setFallHeight <= 20) {
                                sender.sendMessage(ChatColor.DARK_GREEN + "Fall height set to " + setFallHeight);
                                getConfig().set("fallheight", setFallHeight);
                                saveConfig();
                                reloadConfig();
                            }
                            if (setFallHeight > 20) {
                                sender.sendMessage(ChatColor.DARK_RED + "Fall height cannot be over 20");
                            }
                        }
                    }
                }
                if (args.length > 1) {
                    sender.sendMessage(ChatColor.DARK_RED + "Invalid number of arguments");
                }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "You don't have the required permission");
            }
        }
        return false;
    }
}
