package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Enderman;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapView;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

class CourierPlayerListener extends PlayerListener {
    private final Courier plugin;

    public CourierPlayerListener(Courier instance) {
        plugin = instance;
    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Postman postman = plugin.getPostman(e.getRightClicked().getUniqueId());
        if(!e.isCancelled() && !e.getRightClicked().isDead() && postman != null && !postman.scheduledForQuickRemoval()) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " receiving mail");
            ItemStack letter = postman.getLetterItem();

            ItemStack item = e.getPlayer().getItemInHand();
            if(item != null && item.getAmount() > 0) {
                plugin.getCConfig().clog(Level.FINE, "Player hands not empty");
                HashMap<Integer, ItemStack> items = e.getPlayer().getInventory().addItem(letter);
                if(items.isEmpty()) {
                    plugin.getCConfig().clog(Level.FINE, "Letter added to inventory");

                    String inventory = plugin.getCConfig().getInventory();
                    if(inventory != null && !inventory.isEmpty()) {
                        e.getPlayer().sendMessage(inventory);
                    }
                    ((Enderman)e.getRightClicked()).setCarriedMaterial(new MaterialData(Material.AIR));
                    // delivered
                    CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_DELIVERED, e.getPlayer(), letter.getEnchantmentLevel(Enchantment.DURABILITY));
                    plugin.getServer().getPluginManager().callEvent(event);
                } else {
                    plugin.getCConfig().clog(Level.FINE, "Inventory full, letter dropped");
                    postman.drop();
                    // delivered on pickup
                }
            } else {
                plugin.getCConfig().clog(Level.FINE, "Letter delivered into player's hands");
                e.getPlayer().setItemInHand(letter); // REALLY replaces what's there

                // quick render
                e.getPlayer().sendMap(plugin.getServer().getMap(plugin.getCourierdb().getCourierMapId()));
                ((Enderman)e.getRightClicked()).setCarriedMaterial(new MaterialData(Material.AIR));

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_DELIVERED, e.getPlayer(), letter.getEnchantmentLevel(Enchantment.DURABILITY));
                plugin.getServer().getPluginManager().callEvent(event);
            }

            postman.quickDespawn();
        }
    }
    
    public void onItemHeldChange(PlayerItemHeldEvent e) {
        if(e.getPlayer().getInventory().getItem(e.getNewSlot()).getType() == Material.MAP) {
//            MapView map = plugin.getServer().getMap(e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability());
            Letter letter = plugin.getLetter(e.getPlayer().getInventory().getItem(e.getNewSlot()));
            if(letter != null) {
                plugin.getCConfig().clog(Level.FINE, "Switched to Letter id " + letter.getId());

                // quick render
                e.getPlayer().sendMap(plugin.getServer().getMap(plugin.getCourierdb().getCourierMapId()));
            } else { // not needed?
                plugin.getCConfig().clog(Level.FINE, "Id " + e.getPlayer().getItemInHand().getDurability() + " is not a Letter");
            }
        }
    }
    
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        if(!e.isCancelled() && e.getItem().getItemStack().getType() == Material.MAP) {
            //MapView map = plugin.getServer().getMap(e.getItem().getItemStack().getDurability());
            plugin.getCConfig().clog(Level.FINE, "Map id " + e.getItem().getItemStack().getEnchantmentLevel(Enchantment.DURABILITY));
            Letter letter = plugin.getLetter(e.getItem().getItemStack());
            if(letter != null) {
                plugin.getCConfig().clog(Level.FINE, "Map " + letter.getId() + " picked up.");

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_DELIVERED, e.getPlayer(), letter.getId());
                plugin.getServer().getPluginManager().callEvent(event);

                // if itemheldhand was empty, we should render the letter immediately
                ItemStack item = e.getPlayer().getItemInHand();
                if(item != null && item.getAmount() == 0) {
                    e.getPlayer().sendMap(plugin.getServer().getMap(plugin.getCourierdb().getCourierMapId()));
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