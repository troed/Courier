package se.troed.plugin.LoveSheep;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

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
            World world = sheep.getWorld();

            // see if there's an online player nearby in the same world as this sheep
            List<Player> playerList = world.getPlayers();
            if(playerList != null) {
                Iterator<Player> iterator = playerList.iterator();
                while (iterator.hasNext()) {
                    Player p = iterator.next();
                    if (p.isOnline()) {
                        // close enough?
                        Location ploc = p.getLocation();
                        Location sloc = sheep.getLocation();
                        // Double dist = ploc.distance(sloc);
                        // System.out.println("Dist: " + dist.toString());
                        if (ploc.distance(sloc) < plugin.getConfig().getDistance()) {
                            boolean sheepUp = true;
                            Integer owned = plugin.ownership(p);
                            // check if we have loads of sheep already
                            if(owned < plugin.getConfig().getMaxLove()) {
                                if(owned > 0) {
                                    // roll the bigamy dice
                                    // 0.5^1 = 0.5, 0.5^2 = 0.25 etc
                                    Double bigamy = Math.pow(plugin.getConfig().getBigamyChance(), owned);
                                    if(Math.random() > bigamy) {
                                        sheepUp = false;
                                        System.out.println(p.getDisplayName() + " is not in Utah");
                                    }
                                }
                                if(sheepUp) {
                                    System.out.println("Sheep in love with " + p.getDisplayName() + "!");
                                    plugin.fallInLove(sheep, p);
                                }
                            } else {
                                System.out.println(p.getDisplayName() + " has enough sheep");
                            }
                        }
                    }
                }
            }
        }
    }
}
