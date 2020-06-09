package me.prostedeni.goodcraft.learnspeedbridge;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class userdata {
    private Player s;
    private String name;
    private Location old;
    private List<Location> placed = Lists.newArrayList();
    public userdata(Player player){
        s=player;
        name=player.getName();
        //new userdata(e.getPlayer());
    }

    public void addBlock(Location a){
        placed.add(a);
    }

    public Location getOld(){
        return old;
    }

    public void setOld(Location loc){
        old=loc;
    }

    public Location getLast(){
        return placed.get(placed.size()-1);
    }

    public boolean hasNext(){ //je block v list
        return placed.isEmpty()==false;
    }

    public Player getPlayer(){
        return s;
    }

    public List<Location> getBlocks(){
        return placed;
    }

    public void remove(Location location) {
        placed.remove(location);
    }

    public boolean isOnline() {
        return Bukkit.getPlayer(name)!=null && Bukkit.getPlayer(name).getName().equals(name);
    }
}
