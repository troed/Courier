package se.troed.plugin.Courier;

import java.util.logging.Level;

import org.bukkit.entity.CreatureType;
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
}
