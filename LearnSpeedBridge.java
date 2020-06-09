package me.prostedeni.goodcraft.learnspeedbridge;

import com.google.common.collect.Maps;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;

import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.List;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import static org.bukkit.ChatColor.DARK_GREEN;
import static org.bukkit.ChatColor.DARK_RED;

public class LearnSpeedBridge extends JavaPlugin implements Listener {

    static boolean blockDecayBoolean;
    static int blockDecayTimerInt;
    static int fallheightInt;

    private static HashMap<Player, userdata> data = Maps.newHashMap();

    ArrayList<Location> blocksToRemove = new ArrayList<>();

    //List<Location> prot = Lists.newArray
    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent e){
        if (blockDecayBoolean) {
            if (data.containsKey(e.getPlayer())) {
                runRemoveTask(e.getBlockPlaced(), getUser(e.getPlayer()));
            }
        }
    }

    private void runRemoveTask(Block block, userdata player){

        player.addBlock(block.getLocation());

        new BukkitRunnable(){
            public void run(){
                if(!player.isOnline()){
                    return;
                }

                if (player.getOld().distance(block.getLocation()) > 5) {

                    player.remove(block.getLocation());
                    blocksToRemove.add(block.getLocation());
                    return;
                }
            }
        }.runTaskTimerAsynchronously(this, 0, ((blockDecayTimerInt*5)));
    }

    private static userdata getUser(Player p){
        if(!data.containsKey(p))data.put(p, new userdata(p));
        return data.get(p);
    }

    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        new BukkitRunnable(){
            public void run(){
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() - 1.0, p.getLocation().getZ()).getBlock().getType() != Material.AIR)
                       if (data.containsKey(p))
                    getUser(p).setOld(new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() + 1.0, p.getLocation().getZ(), p.getLocation().getYaw(), p.getLocation().getPitch()));
                }
            }
        }.runTaskTimerAsynchronously(this,0,3);
        new BukkitRunnable(){
            public void run(){
                if (getConfig().getBoolean("onoff") || !(getConfig().getBoolean("onoff"))){
                    getConfig().set("onoff", null);
                }
            }
        }.runTaskLaterAsynchronously(this,20);

        fetchConfig();

        if (blockDecayBoolean){
            new BukkitRunnable(){
                public void run(){
                    if (blocksToRemove.size() != 0){
                        for (Location blockLoc : blocksToRemove){
                            if (blockLoc.getBlock().getType() != Material.AIR){
                                blockLoc.getBlock().setType(Material.AIR);
                            } else if (blockLoc.getBlock().getType() == Material.AIR){
                                blocksToRemove.remove(blockLoc);
                            }
                            if (blocksToRemove.size() == 0){
                                break;
                            }
                        }
                    }
                }
            }.runTaskTimer(this, 0, ((blockDecayTimerInt*10)));
        }
    }

    public void fetchConfig(){
        blockDecayBoolean = getConfig().getBoolean("blockDecay");
        blockDecayTimerInt = getConfig().getInt("blockDecayTimer");
        fallheightInt = getConfig().getInt("fallheight");
    }

    public void onDisable() {
        saveConfig();
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent e) {
        if (getUser(e.getPlayer()).getOld() != null) {
            if (getUser(e.getPlayer()).getOld().getWorld() != e.getPlayer().getWorld()) {
                return;
            }
        }

        if (data.containsKey(e.getPlayer())) {
            e.getPlayer().setFallDistance(0.0f);
        }

        if (data.containsKey(e.getPlayer())) {
            if (data.get(e.getPlayer()).getOld() != null) {
                if (e.getPlayer().getLocation().getBlockY() < data.get(e.getPlayer()).getOld().getBlockY() - (fallheightInt + 1)) {
                    if (e.getPlayer().hasPermission("speedbridge.use")) {
                        if (!e.getPlayer().isFlying()) {
                            if (e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                                e.getPlayer().teleport(getUser(e.getPlayer()).getOld());
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent q){
        if(data.containsKey(q.getPlayer())){
            for(Location a : data.get(q.getPlayer()).getBlocks())
                a.getBlock().setType(Material.AIR);
            data.remove(q.getPlayer());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) throws NumberFormatException {
        if(command.getName().equals("speedbridge")) {
            if (sender instanceof Player){
            if (sender.hasPermission("speedbridge.cmd")) {
                if (args.length == 0) {
                    if (data.containsKey(((Player) sender).getPlayer())) {
                        data.remove(((Player) sender).getPlayer());
                        msg( "&4SpeedBridge has been turned off", sender);
                    } else if (!(data.containsKey(sender))){
                        getUser((Player)sender);
                        msg( "&2SpeedBridge has been turned on", sender);
                    }
                }

                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("blockdecay")) {
                        if (blockDecayBoolean) {
                            getConfig().set("blockDecay", false);
                            saveConfig();
                            reloadConfig();
                            fetchConfig();
                            msg("&4blockDecay has been turned off", sender);
                        } else if (!(blockDecayBoolean)){
                            getConfig().set("blockDecay", true);
                            saveConfig();
                            reloadConfig();
                            fetchConfig();
                            msg( "&2blockDecay has been turned on", sender);
                        }
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        reloadConfig();
                        saveConfig();
                        fetchConfig();
                        msg("&2Config reloaded", sender);
                        System.out.println("Config reloaded");
                    } else {
                        try {
                            int setFallHeight = Integer.parseInt(args[0]);
                            if (fallheightInt == setFallHeight){
                                msg("&4Fall height already is set to " + fallheightInt, sender);
                            } else {
                                if (setFallHeight < 1) {
                                    msg( "&4Fall height cannot be below 1",sender);
                                }
                                if (setFallHeight >= 1 && setFallHeight <= 20) {
                                    msg("&2Fall height set to " + setFallHeight, sender);
                                    getConfig().set("fallheight", setFallHeight);
                                    saveConfig();
                                    reloadConfig();
                                    fetchConfig();
                                }
                                if (setFallHeight > 20) {
                                    msg("&4Fall height cannot be over 20", sender);
                                }
                            }
                        }
                        catch (java.lang.NumberFormatException e){
                            msg("&4Invalid arguments", sender);
                        }
                    }
                }

                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("blockdecay")) {
                        try {
                            if (args[1] != null) {
                                if (Integer.parseInt(args[1]) <= 0) {
                                    msg("&4blockDecayTimer cannot be 0 or below", sender);
                                } else if ((Integer.parseInt(args[1])) > 0 && (Integer.parseInt(args[1]) <= 20)) {
                                    getConfig().set("blockDecayTimer", args[1]);
                                    saveConfig();
                                    reloadConfig();
                                    fetchConfig();
                                    msg("&2blockDecayTimer was set to "+args[1], sender);
                                } else if (Integer.parseInt(args[1]) > 20) {
                                    msg("&4blockDecayTimer cannot be over 20", sender);
                                }
                            }
                        }
                        catch (java.lang.NumberFormatException e){
                            msg("&4Invalid arguments",sender);
                        }
                    }
                }

                if (args.length > 2) {
                    msg("&4Invalid number of arguments",sender);
                }
            } else {
                msg("&4You don't have the required permission",sender);
            }
        }}
        return false;
    }

    public void msg(String text, CommandSender s){
        if(text!=null && s!=null)
        s.sendMessage(ChatColor.translateAlternateColorCodes('&',text));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (command.getName().equalsIgnoreCase("speedbridge")) {
            if (args.length == 1) {
                if (sender.hasPermission("speedbridge.cmd")) {
                    final ArrayList<String> l = new ArrayList<>();

                    final ArrayList<String> commands = new ArrayList<>();
                    commands.add("reload");
                    commands.add("blockDecay");
                    commands.add("<fallheight number>");

                    if (args[0].contains("r")) {
                        final ArrayList<String> r = new ArrayList<>();
                        r.add("reload");
                        return r;
                    } else if (args[0].contains("b")) {
                        final ArrayList<String> b = new ArrayList<>();
                        b.add("blockDecay");
                        return b;
                    } else {
                        final ArrayList<String> nums = new ArrayList<>();
                        for (int i = 1; i > 0 && i <= 20; i++) {
                            nums.add(String.valueOf(i));
                        }
                        if (nums.contains(args[0])) {
                            final ArrayList<String> d = new ArrayList<>();
                            d.add("<fallheight number>");
                            return d;
                        }
                    }

                    StringUtil.copyPartialMatches(args[0], commands, l);
                    return l;
                }
            } else if (args.length == 2){
                if (args[0].equalsIgnoreCase("blockDecay")){

                    if (sender.hasPermission("speedbridge.cmd")) {
                        final ArrayList<String> l = new ArrayList<>();

                        final ArrayList<String> commands = new ArrayList<>();
                        commands.add("<blockDecay number>");

                        final ArrayList<String> nums = new ArrayList<>();
                        for (int i = 1; i > 0 && i <= 20; i++) {
                            nums.add(String.valueOf(i));
                        }
                        if (nums.contains(args[1])) {
                            final ArrayList<String> d = new ArrayList<>();
                            d.add("<fallheight number>");
                            return d;
                        }

                        StringUtil.copyPartialMatches(args[1], commands, l);
                        return l;

                    }
                }
            }
        }
        return null;
    }
}
