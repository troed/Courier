package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;

import java.util.Date;
import java.util.logging.Level;

class CourierPlayerListener extends PlayerListener {
    private final Courier plugin;

    public CourierPlayerListener(Courier instance) {
        plugin = instance;
    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Entity ent = e.getRightClicked();
        if(plugin.getPostman(ent.getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " receiving mail");
            ItemStack letter = plugin.getPostman(ent.getUniqueId()).getLetter();

            boolean replace = false;
            ItemStack item = e.getPlayer().getItemInHand();
            if(item != null && item.getAmount() > 0) {
                plugin.getCConfig().clog(Level.FINE, "Player held item in hand");
//                int slot = e.getPlayer().getInventory().getHeldItemSlot();
// todo: fix whatever went wrong here, or disallow right-clicking altogether
//                    HashMap<Integer, ItemStack> items = e.getPlayer().getInventory().addItem(item.clone());
//                    if(items.isEmpty()) {
//                    plugin.getCConfig().clog(Level.FINE, "Held item added to inventory");
//                    replace = true;
//                    } // else add didn't work, we'll drop and the clone will disappear again. Right?
            } else {
                replace = true;
            }
            if(replace) {
                plugin.getCConfig().clog(Level.FINE, "Set item in hand");
                e.getPlayer().setItemInHand(letter); // REALLY replaces what's there

                // checks if we still have an attached renderer and fixes it if not
                MapView map = plugin.getServer().getMap(letter.getDurability());
                plugin.getLetter(map);
                // quick render
                e.getPlayer().sendMap(map);
                ((Enderman)ent).setCarriedMaterial(new MaterialData(Material.AIR));

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_DELIVERED, e.getPlayer(), letter.getDurability());
                plugin.getServer().getPluginManager().callEvent(event);
            } else {
                plugin.getPostman(ent.getUniqueId()).drop();
            }

            plugin.getPostman(ent.getUniqueId()).quickDespawn();
        }
    }
    
    public void onItemHeldChange(PlayerItemHeldEvent e) {
        if(e.getPlayer().getInventory().getItem(e.getNewSlot()).getType() == Material.MAP) {
            // durability = the map id
            MapView map = plugin.getServer().getMap(e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability());
            if(map != null) {
                plugin.getCConfig().clog(Level.FINE, "Map " + map.getId() + " held. X=" + map.getCenterX() + " Z=" + map.getCenterZ());
                if(map.getCenterX() == Courier.MAGIC_NUMBER) {
                    Date date = new Date((long)(map.getCenterZ()) * 1000); // convert back to milliseconds
                    plugin.getCConfig().clog(Level.FINE, "Map " + map.getId() + " is a Courier letter!");
                    plugin.getCConfig().clog(Level.FINE, "Created: " + date.toString());

                    // really checks if we still have an attached renderer and fixes it if not
                    plugin.getLetter(map);

                    // fixing stuffs and immediate rendering?
                    e.getPlayer().sendMap(map);
                }
            } else { // not needed?
                plugin.getCConfig().clog(Level.FINE, "Id " + e.getPlayer().getItemInHand().getDurability() + " is not a map");
            }
        }
    }
    
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        if(e.getItem().getItemStack().getType() == Material.MAP) {
            MapView map = plugin.getServer().getMap(e.getItem().getItemStack().getDurability());
            if(map != null) {
                plugin.getCConfig().clog(Level.FINE, "Map " + map.getId() + " picked up. X=" + map.getCenterX() + " Z=" + map.getCenterZ());
                if(map.getCenterX() == Courier.MAGIC_NUMBER) {
                    Date date = new Date((long)(map.getCenterZ()) * 1000); // convert back to milliseconds
                    plugin.getCConfig().clog(Level.FINE, "Map " + map.getId() + " is a Courier letter!");
                    plugin.getCConfig().clog(Level.FINE, "Created: " + date.toString());

                    // really checks if we still have an attached renderer and fixes it if not
                    plugin.getLetter(map);

                    // delivered
                    CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_DELIVERED, e.getPlayer(), map.getId());
                    plugin.getServer().getPluginManager().callEvent(event);

                    // if itemheldhand was empty, we should render the letter immediately
                    ItemStack item = e.getPlayer().getItemInHand();
                    if(item != null && item.getAmount() == 0) {
                        e.getPlayer().sendMap(map);
                    }
               }
            }
        }        
    }

    // onPlayerDropItem for recycling? or something more active? (furnace? :D)

    public void onPlayerQuit(PlayerQuitEvent event) {
        if(plugin.getServer().getOnlinePlayers().length <= 1) { // ==
            // last player left
            plugin.pauseDeliveries();
        }
        plugin.getCConfig().clog(Level.FINE, event.getPlayer().getDisplayName() + " has left the building");
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        if(plugin.getServer().getOnlinePlayers().length == 1) {
            // first player joined
            // note: if this ever jumps from 0 to 2 in one go we'll never start deliveries. Implement failsafe?
            plugin.startDeliveries();
        }
        plugin.getCConfig().clog(Level.FINE, event.getPlayer().getDisplayName() + " has joined");
    }
}