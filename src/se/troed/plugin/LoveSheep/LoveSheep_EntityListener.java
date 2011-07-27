package se.troed.plugin.LoveSheep;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import com.sun.javaws.jnl.LaunchSelection;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerListener;
import sun.security.krb5.Config;


public class LoveSheep_EntityListener extends EntityListener {
    private final LoveSheep plugin;

    public LoveSheep_EntityListener(LoveSheep instance) {
        plugin = instance;
    }

    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getCreatureType() == CreatureType.SHEEP) {

            Sheep sheep = (Sheep) e.getEntity();

// see     public List<org.bukkit.entity.Entity> getNearbyEntities(double x, double y, double z);

// see     plugin.getServer().getOnlinePlayers();

            // see if there's an online player nearby in the same world as this sheep
            List<Player> playerList = sheep.getWorld().getPlayers();
            if(playerList != null) {
                for(Player p : playerList) {
//                Iterator<Player> iterator = playerList.iterator();
//                while ((iterator != null) && iterator.hasNext()) {
//                    Player p = iterator.next();
                    if (p.isOnline()) { // is this needed?
                        Location ploc = p.getLocation();
                        Location sloc = sheep.getLocation();
                        if (ploc.distance(sloc) < plugin.getConfig().getDistance()) {
                            boolean sheepUp = true;
                            Integer love = plugin.loverCount(p);
                            // check if we have enough sheep already
                            if(love < plugin.getConfig().getMaxLove()) {
                                if(love > 0) {
                                    // roll the bigamy dice
                                    // 0.5^1 = 0.5, 0.5^2 = 0.25 etc
                                    Double bigamy = Math.pow(plugin.getConfig().getBigamyChance(), love);
                                    if(Math.random() > bigamy) {
                                        sheepUp = false;
                                    } else {
                                        plugin.getConfig().lslog(Level.FINE, p.getDisplayName() + " lives in Utah!");
                                    }
                                }
                                if(sheepUp) {
                                    plugin.getConfig().lslog(Level.FINE, "Sheep in love with " + p.getDisplayName() + "!");
                                    plugin.fallInLove(sheep, p);
                                }
                            } else {
                                plugin.getConfig().lslog(Level.FINE, p.getDisplayName() + " has enough sheep");
                            }
                        }
                    }
                }
            }
        }
    }
}
