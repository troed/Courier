package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;
import sun.text.normalizer.Replaceable;

import java.util.HashMap;
import java.util.logging.Level;

public class CourierPlayerListener extends PlayerListener {
    private final Courier plugin;

    public CourierPlayerListener(Courier instance) {
        plugin = instance;
    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if(plugin.getPostman() != null) {
            Entity ent = (Entity) e.getRightClicked();
            if(ent.getUniqueId() == plugin.getPostman().getUUID()) {
                plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " receiving mail");
                // todo: is it _ours_ ?
                ItemStack letter = plugin.getPostman().getLetter();

                boolean replace = false;
                ItemStack item = e.getPlayer().getItemInHand();
                if(item != null) {
                    plugin.getCConfig().clog(Level.FINE, "Item not null");
                    int slot = e.getPlayer().getInventory().getHeldItemSlot();
//                    HashMap<Integer, ItemStack> items = e.getPlayer().getInventory().addItem(item.clone());
//                    if(items.isEmpty()) {
                        plugin.getCConfig().clog(Level.FINE, "Held item added to inventory");
                        replace = true;
//                    } // else add didn't work, we'll drop and the clone will disappear again. Right?
                } else {
                    replace = true;
                }
                if(replace) {
                    plugin.getCConfig().clog(Level.FINE, "Set item in hand");
                    e.getPlayer().setItemInHand(letter); // REALLY replaces what's there
                } else {
                    plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " inventory full");
                    ent.getWorld().dropItemNaturally(ent.getLocation(), letter);
                }

                ((Enderman)ent).setCarriedMaterial(new MaterialData(Material.AIR)); // null is not valid
                plugin.getPostman().quickDespawn();

    //   Player  public void sendMap(MapView map);
            }
        }
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCConfig().clog(Level.FINE, event.getPlayer().getDisplayName() + " has left the building");
    }
}

// need this to make sure only the right reciepient can read their letters?
  /**
 * Called when a player picks an item up off the ground
 *
 * @see org.bukkit.event.player.PlayerPickupItemEvent
 */
  //      PLAYER_PICKUP_ITEM (Category.PLAYER),
