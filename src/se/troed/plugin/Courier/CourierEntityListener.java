package se.troed.plugin.Courier;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.material.MaterialData;


public class CourierEntityListener extends EntityListener {
    private final Courier plugin;

    public CourierEntityListener(Courier instance) {
        plugin = instance;
    }

    // don't allow postmen to attack players
    public void onEntityTarget(EntityTargetEvent e) {
        if(plugin.getPostman() != null) {
            if(e.getEntity().getUniqueId() == plugin.getPostman().getUUID()) {
                plugin.getCConfig().clog(Level.FINE, "Cancel angry enderman");
                e.setCancelled(true);
            }
        }
    }

    // don't allow players to attack postmen
    public void onEntityDamage(EntityDamageEvent e) {
        // don't care about cause, if it's a postman then drop mail and bail
        if(plugin.getPostman() != null) {
            if(e.getEntity().getUniqueId() == plugin.getPostman().getUUID()) {
                plugin.getCConfig().clog(Level.FINE, "Postman taking damage");
                if(!e.getEntity().isDead() && !plugin.getPostman().scheduledForRemoval()) {
                    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), plugin.getPostman().getLetter());
                    ((Enderman)e.getEntity()).setCarriedMaterial(new MaterialData(Material.AIR)); // null is not valid
                    plugin.getPostman().quickDespawn();
                    plugin.getCConfig().clog(Level.FINE, "Drop and despawn");
                } // else already removed
                e.setCancelled(true);
            }
        }
    }

    // postmen aren't block thieves
    public void onEndermanPickup(EndermanPickupEvent e) {
        if(plugin.getPostman() != null) {
            if(e.getEntity().getUniqueId() == plugin.getPostman().getUUID()) {
                plugin.getCConfig().clog(Level.FINE, "Prevented postman thief");
                e.setCancelled(true);
            }
        }
    }
    
    public void onEndermanPlace(EndermanPlaceEvent e) {
        if(plugin.getPostman() != null) {
            if(e.getEntity().getUniqueId() == plugin.getPostman().getUUID()) {
                plugin.getCConfig().clog(Level.FINE, "Prevented postman maildrop");
                e.setCancelled(true);
            }
        }
    }

    // currently not used - doesn't detect their teleports
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

    /**
     * Called when an Enderman places a block
     *
     * @see org.bukkit.event.entity.EndermanPlaceEvent
     */
// this is cool if they're placing the map nearby
//    ENDERMAN_PLACE (Category.LIVING_ENTITY),
