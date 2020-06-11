package me.prostedeni.goodcraft.learnspeedbridge;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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

import static org.bukkit.ChatColor.*;

public class LearnSpeedBridge extends JavaPlugin implements Listener
{

    HashMap<Player, Location> ProtectedBlocks = new HashMap<>();

    private HashMap<String, Location> SavedProtectedBlocks = new HashMap<>();

    HashMap<String, Location> old;

    HashMap<Location, String> blocksToRemove = new HashMap<>();

    ArrayList<Location> bTRcheck = new ArrayList<>();

    ArrayList<String> sPlayers = new ArrayList<>();

    //kinda a lot of HashMaps and ArrayLists with mostly duplicate
    //content, but i couldn't find better way to do this :(

    static boolean blockDecayBoolean;
    static boolean enableProtection;
    static boolean cantBreakUnder;
    static int blockDecayTimerInt;
    static int fallheightInt;

    public void fetchConfig(){
        blockDecayBoolean = getConfig().getBoolean("blockDecay");
        blockDecayTimerInt = getConfig().getInt("blockDecayTimer");
        fallheightInt = getConfig().getInt("fallheight");
        enableProtection = getConfig().getBoolean("blockProtection");
        cantBreakUnder = getConfig().getBoolean("cantBreakUnder");

        // gets values from config on request because it's faster to get static values than access the file every time
    }

    public LearnSpeedBridge() {
        this.old = new HashMap<String, Location>();
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent e){
        if (blockDecayBoolean) {
            if (sPlayers.contains(e.getPlayer().getName())) {
                Player p = e.getPlayer();

                blocksToRemove.put(e.getBlockPlaced().getLocation(), p.getName());
                bTRcheck.add(e.getBlockPlaced().getLocation());
                ProtectedBlocks.put(p, e.getBlockPlaced().getLocation());

                if (new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() - 1.0, p.getLocation().getZ()).getBlock().getType() != Material.AIR) {
                    LearnSpeedBridge.this.old.put(p.getName(), new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() + 1.0, p.getLocation().getZ(), p.getLocation().getYaw(), p.getLocation().getPitch()));
                }
                // position of player before the fall gets put in here (into HashMap old)

            }
        }
    }

    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        fetchConfig();

        if (blockDecayBoolean) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (bTRcheck.size() != 0) {
                        for (int i = bTRcheck.size(); i > 0; i--) {
                            Location blockLoc = bTRcheck.get(i - 1);
                            if (!(SavedProtectedBlocks.containsValue(blockLoc))) {
                                try {
                                    Player player = getServer().getPlayer(blocksToRemove.get(blockLoc));
                                    if (blockLoc.distance((player.getLocation())) > 5) {
                                        if (blockLoc.getBlock().getType() != Material.AIR) {
                                            blockLoc.getBlock().setType(Material.AIR);
                                            blocksToRemove.remove(blockLoc);
                                            bTRcheck.remove(blockLoc);
                                        } else if (blockLoc.getBlock().getType() == Material.AIR) {
                                            blocksToRemove.remove(blockLoc);
                                            bTRcheck.remove(blockLoc);
                                        }
                                    }
                                } catch (java.lang.NullPointerException | java.lang.IllegalArgumentException e) {
                                    if (blockLoc.getBlock().getType() != Material.AIR) {
                                        blockLoc.getBlock().setType(Material.AIR);
                                        blocksToRemove.remove(blockLoc);
                                        bTRcheck.remove(blockLoc);
                                    } else if (blockLoc.getBlock().getType() == Material.AIR) {
                                        blocksToRemove.remove(blockLoc);
                                        bTRcheck.remove(blockLoc);
                                    }
                                }
                                //using errors to accomplish a goal is so meta XD

                            }
                            if (bTRcheck.size() == 0) {
                                break;
                            }
                        }
                    }
                }
            }.runTaskTimer(this, 2, (blockDecayTimerInt));
        }
        //this part here handles removing the blocks

    }

    public void onDisable() {
        saveConfig();
        //lots of code here /s
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent e) {
        if (old.containsKey(e.getPlayer().getName())){
            World wold = old.get(e.getPlayer().getName()).getWorld();
            World nold = e.getPlayer().getWorld();
            if (wold != nold) {
                return;
            }

            e.getPlayer().setFallDistance(0.0f);
            //this prevents fall damage of speedbridging player

            if (e.getPlayer().getLocation().getBlockY() < old.get(e.getPlayer().getName()).getBlockY() - (fallheightInt + 1)) {
                if (sPlayers.size() != 0) {
                    if (sPlayers.contains(e.getPlayer().getName())) {
                        if (e.getPlayer().hasPermission("speedbridge.use")) {
                            if (!e.getPlayer().isFlying()) {
                                if (e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                                    e.getPlayer().teleport((Location) this.old.get(e.getPlayer().getName()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    //this bit handles putting player back on the bridge

    @EventHandler
    public void onLeave(PlayerQuitEvent q) {
        if (sPlayers != null) {
            if (sPlayers.contains(q.getPlayer().getName())) {
                if (enableProtection) {
                    Player p = q.getPlayer();
                    if (ProtectedBlocks.containsKey(p)){
                        SavedProtectedBlocks.put(p.getName(), ProtectedBlocks.get(p));
                        ProtectedBlocks.remove(p);
                    }
                }
                sPlayers.remove(q.getPlayer().getName());
            }
        }
        if (old != null) {
            if (old.containsKey(q.getPlayer().getName())) {
                old.remove(q.getPlayer().getName());
            }
        }
    }
    //hopefully prevents mem leaks by removing every bit of data associated with given player

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        try {
            if (enableProtection) {
                SavedProtectedBlocks.remove(e.getPlayer().getName());
            }
        } catch (NullPointerException e1){
            //just catch the error to avoid unnecesary check
        }
    }
    //removes block from where the player stood from protected blocks

    @EventHandler
    public void onBlockDestroy(BlockBreakEvent e){
        String player = e.getPlayer().getName();
        Location blockLoc = e.getBlock().getLocation();

        if (enableProtection){
            if (SavedProtectedBlocks.containsValue(blockLoc)){
                if (SavedProtectedBlocks.get(player) != blockLoc){
                    e.setCancelled(true);
                }
                if (SavedProtectedBlocks.get(player) == blockLoc){
                    SavedProtectedBlocks.remove(player);
                }
            }
        }
        //this protects block where speedbridging player stood before logging off from destruction

        if (cantBreakUnder){
            for (Player p : Bukkit.getOnlinePlayers()){
                if (sPlayers != null) {
                    if (sPlayers.contains(p.getName())) {
                        if (p.getLocation().distance(e.getPlayer().getLocation()) < 7) {
                            Location loc = new Location(p.getWorld(), p.getLocation().getBlockX(), p.getLocation().getBlockY() - 1, p.getLocation().getBlockZ());
                            if ((e.getBlock().getLocation()).equals(loc)) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
            //this protects block which speedbridging player is standing on


            Block b = e.getBlock();
            Location loc = new Location(b.getWorld(), b.getX(), (b.getY() - 2), b.getZ());
            if (old.containsValue(loc)) {
                if (!(old.get(e.getPlayer().getName()).equals(loc))) {
                    e.setCancelled(true);
                }
            }
            //this protects block which the player will be returned to if he falls
            //*(probably)

        }



    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equals("speedbridge")) {
            if (sender.hasPermission("speedbridge.use") || sender.hasPermission("speedbridge.cmd")) {
                if (args.length == 0) {
                    if (sPlayers.contains(sender.getName())) {
                        sPlayers.remove(sender.getName());
                        sender.sendMessage(DARK_RED + "SpeedBridge has been turned off");
                        if (old != null) {
                            if (old.containsKey(sender.getName())) {
                                old.remove(sender.getName());
                            }
                        }
                    } else if (!(sPlayers.contains(sender.getName()))) {
                        sPlayers.add(sender.getName());
                        sender.sendMessage(DARK_GREEN + "SpeedBridge has been turned on");
                    }
                }
            } else {
                sender.sendMessage(DARK_RED + "You don't have the required permission");
            }

            if (sender.hasPermission("speedbridge.cmd")) {
                if (args.length == 1) {
                    switch ((args[0]).toUpperCase()) {
                        case "CANTBREAKUNDER":
                            if (cantBreakUnder) {
                                getConfig().set("cantBreakUnder", false);
                                saveConfig();
                                reloadConfig();
                                fetchConfig();
                                sender.sendMessage(DARK_RED + "cantBreakUnder has been turned off");
                                break;
                            } else if (!(cantBreakUnder)) {
                                getConfig().set("cantBreakUnder", true);
                                saveConfig();
                                reloadConfig();
                                fetchConfig();
                                sender.sendMessage(DARK_GREEN + "cantBreakUnder has been turned on");
                                break;
                            }
                        case "BLOCKPROTECTION":
                            if (enableProtection) {
                                getConfig().set("blockProtection", false);
                                saveConfig();
                                reloadConfig();
                                fetchConfig();
                                sender.sendMessage(DARK_RED + "blockProtection has been turned off");
                                break;
                            } else if (!(enableProtection)) {
                                getConfig().set("blockProtection", true);
                                saveConfig();
                                reloadConfig();
                                fetchConfig();
                                sender.sendMessage(DARK_GREEN + "blockProtection has been turned on");
                                break;
                            }
                        case "BLOCKDECAY":
                            if (blockDecayBoolean) {
                                getConfig().set("blockDecay", false);
                                saveConfig();
                                reloadConfig();
                                fetchConfig();
                                sender.sendMessage(DARK_RED + "blockDecay has been turned off");
                                break;
                            } else if (!(blockDecayBoolean)) {
                                getConfig().set("blockDecay", true);
                                saveConfig();
                                reloadConfig();
                                fetchConfig();
                                sender.sendMessage(DARK_GREEN + "blockDecay has been turned on");
                                break;
                            }
                        case "RELOAD":
                            reloadConfig();
                            saveConfig();
                            fetchConfig();
                            sender.sendMessage(DARK_GREEN + "Config reloaded");
                            System.out.println("Config reloaded");
                            break;
                        default:
                            try {
                                int setFallHeight = Integer.parseInt(args[0]);
                                if (fallheightInt == setFallHeight) {
                                    sender.sendMessage(DARK_RED + "Fall height already is set to " + fallheightInt);
                                    break;
                                } else {
                                    if (setFallHeight < 1) {
                                        sender.sendMessage(DARK_RED + "Fall height cannot be below 1");
                                        break;
                                    }
                                    if (setFallHeight >= 1 && setFallHeight <= 20) {
                                        sender.sendMessage(DARK_GREEN + "Fall height set to " + setFallHeight);
                                        getConfig().set("fallheight", setFallHeight);
                                        reloadConfig();
                                        saveConfig();
                                        fetchConfig();
                                        break;
                                    }
                                    if (setFallHeight > 20) {
                                        sender.sendMessage(DARK_RED + "Fall height cannot be over 20");
                                        break;
                                    }
                                }
                            } catch (java.lang.NumberFormatException e) {
                                sender.sendMessage(DARK_RED + "Invalid arguments");
                                break;
                            }

                            //used switch statement here (first time in my life), this should help optimize the command (teeny tiny bit)
                    }
                }

                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("blockdecay")) {
                        try {
                            if (args[1] != null) {
                                if (blockDecayTimerInt == Integer.parseInt(args[1])) {
                                    sender.sendMessage(DARK_RED + "blockDecayTimer already is set to " + blockDecayTimerInt);
                                } else {
                                    if (Integer.parseInt(args[1]) <= 0) {
                                        sender.sendMessage(DARK_RED + "blockDecayTimer cannot be 0 or below");
                                    } else if ((Integer.parseInt(args[1])) > 0 && (Integer.parseInt(args[1]) <= 200)) {
                                        getConfig().set("blockDecayTimer", args[1]);
                                        saveConfig();
                                        reloadConfig();
                                        fetchConfig();
                                        sender.sendMessage(DARK_GREEN + "blockDecayTimer was set to " + args[1]);
                                    } else if (Integer.parseInt(args[1]) > 200) {
                                        sender.sendMessage(DARK_RED + "blockDecayTimer cannot be over 200");
                                    }
                                }
                            }
                        } catch (java.lang.NumberFormatException e){
                            sender.sendMessage(DARK_RED + "Invalid arguments");
                        }
                        //using errors to accomplish a goal is so meta XD

                    }
                }

                if (args.length > 2) {
                    sender.sendMessage(DARK_RED + "Invalid number of arguments");
                }
            } else {
                sender.sendMessage(DARK_RED + "You don't have the required permission");
            }
        }
        return false;
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
                    commands.add("blockProtection");
                    commands.add("<fallheight number>");
                    commands.add("cantBreakUnder");

                    if (args[0].startsWith("r")) {
                        final ArrayList<String> r = new ArrayList<>();
                        r.add("reload");
                        StringUtil.copyPartialMatches(args[0], r, l);
                        return l;
                    } else if (args[0].startsWith("b")) {
                        final ArrayList<String> b = new ArrayList<>();
                        final ArrayList<String> u = new ArrayList<>();
                        b.add("blockDecay");
                        b.add("blockProtection");
                        StringUtil.copyPartialMatches(args[0], b, u);
                        return u;
                    } else if (args[0].startsWith("c")){
                        final ArrayList<String> c = new ArrayList<>();
                        c.add("cantBreakUnder");
                        StringUtil.copyPartialMatches(args[0], c, l);
                        return l;
                    } else {
                        if (!(args[0].equals(""))) {
                            try {
                                if (Integer.parseInt(args[0]) > 0 && Integer.parseInt(args[0]) <= 20) {
                                    final ArrayList<String> d = new ArrayList<>();
                                    d.add("<fallheight number>");
                                    return d;
                                }
                            } catch (java.lang.NumberFormatException e){
                                return null;
                            }
                            //using errors to accomplish a goal is so meta XD

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

                        /*
                        final ArrayList<String> nums = new ArrayList<>();
                        for (int i = 1; i > 0 && i <= 200; i++) {
                            nums.add(String.valueOf(i));
                        }


                        if (nums.contains(args[1])) {
                            final ArrayList<String> d = new ArrayList<>();
                            d.add("<blockDecay number>");
                            return d;
                        }
                        */

                        //thought of much better way to go about this, adding 200 numbers every time
                        //autocomplete happens might've been resource taxing
                        //did the same thing on lines 435-447

                        //if there are no issues reported, will remove this comment in next version (assuming there is one)

                        if (!(args[1].equals(""))) {
                            try {
                                if (Integer.parseInt(args[1]) > 0 && Integer.parseInt(args[1]) <= 200) {
                                    final ArrayList<String> d = new ArrayList<>();
                                    d.add("<blockDecay number>");
                                    return d;
                                }
                            } catch (java.lang.NumberFormatException e){
                                return null;
                            }
                            //using errors to accomplish a goal is so meta XD

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
