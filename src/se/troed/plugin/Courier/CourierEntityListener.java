package se.troed.plugin.Courier;

import java.util.logging.Level;

import org.bukkit.entity.CreatureType;
import org.bukkit.event.entity.*;


public class CourierEntityListener extends EntityListener {
    private final Courier plugin;

    public CourierEntityListener(Courier instance) {
        plugin = instance;
    }

    // don't allow postmen to attack players
    public void onEntityTarget(EntityTargetEvent e) {
        if(plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Cancel angry enderman");
            e.setCancelled(true);
        }
    }

    // don't allow players to attack postmen
    public void onEntityDamage(EntityDamageEvent e) {
        // don't care about cause, if it's a postman then drop mail and bail
        if(plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Postman taking damage");
            Postman postman = plugin.getPostman(e.getEntity().getUniqueId());
            if(!e.getEntity().isDead() && !postman.scheduledForQuickRemoval()) {
                postman.drop();
                postman.quickDespawn();
                plugin.getCConfig().clog(Level.FINE, "Drop and despawn");
            } // else already removed
            e.setCancelled(true);
        }
    }

    // postmen aren't block thieves
    public void onEndermanPickup(EndermanPickupEvent e) {
        if(plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman thief");
            e.setCancelled(true);
        }
    }
    
    public void onEndermanPlace(EndermanPlaceEvent e) {
        if(plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman maildrop");
            e.setCancelled(true);
        }
    }

    // currently not used - doesn't detect their teleports
    // todo: will have to fix that with a thread checking distance between enderman and their player and teleport them
    // back into line-of-sight
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getCreatureType() == CreatureType.ENDERMAN) {

//            plugin.getCConfig().clog(Level.FINE, "Enderman spawn");
//            Enderman ender = (Enderman) e.getEntity();

            // was this our spawned enderman near our player?
/*            List<Player> playerList = ender.getWorld().getPlayers();
            if(playerList != null) {
                for(Player p : playerList) {
                    if (p.isOnline()) { // is this needed?
                        Location ploc = p.getLocation();
                        Location sloc = ender.getLocation();
                        if (ploc.distance(sloc) < plugin.getCConfig().getDistance()) {
                            plugin.getCConfig().clog(Level.FINE, p.getDisplayName() + " near enderman");
                        }
                    }
                }
            }*/
        }
    }
}
