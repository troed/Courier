package se.troed.plugin.Courier;

import java.util.logging.Level;

import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.*;


class CourierEntityListener extends EntityListener {
    private final Courier plugin;

    public CourierEntityListener(Courier instance) {
        plugin = instance;
    }

    // don't allow postmen to attack players
    public void onEntityTarget(EntityTargetEvent e) {
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Cancel angry enderman");
            e.setCancelled(true);
        }
    }

    // don't allow players to attack postmen
    public void onEntityDamage(EntityDamageEvent e) {
        // don't care about cause, if it's a postman then drop mail and bail
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
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
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman thief");
            e.setCancelled(true);
        }
    }
    
    public void onEndermanPlace(EndermanPlaceEvent e) {
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman maildrop");
            e.setCancelled(true);
        }
    }

    // in theory we could add another listener at Monitor priority for announce() ..
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if(e.getCreatureType() == CreatureType.ENDERMAN) {
            plugin.getCConfig().clog(Level.FINE, "onCreatureSpawn Enderman with uuid:" + e.getEntity().getUniqueId());
            // we end up here before we've had a chance to log and store our Postman uuids!
            // this means we cannot reliably override spawn deniers with perfect identification.
            // We match on Location instead but it's not pretty. Might be the only solution though.
            Postman postman = plugin.getAndRemoveSpawner(e.getLocation());
            if(postman != null) {
                plugin.getCConfig().clog(Level.FINE, "onCreatureSpawn is a Postman");
                if(e.isCancelled()) {
                    if(plugin.getCConfig().getBreakSpawnProtection()) {
                        plugin.getCConfig().clog(Level.FINE, "onCreatureSpawn Postman override");
                        e.setCancelled(false);
                        postman.announce(e.getLocation());
                    } else {
                        postman.cannotDeliver();
                    }
                } else {
                    postman.announce(e.getLocation());
                }
            }
        }
    }
}
