package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.logging.Level;

class CourierEventListener implements Listener {
    private final Courier plugin;

    public CourierEventListener(Courier instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onCourierDeliveryEvent(CourierDeliveryEvent e) {
        if(e.getPlayer()!=null && e.getId()!=-1) {
            if(e.getEventName().equals(CourierDeliveryEvent.COURIER_DELIVERED)) {
                plugin.getCConfig().clog(Level.FINE, "Delivered letter to " + e.getPlayer().getName() + " with id " + e.getId());
                plugin.getCourierdb().setDelivered(e.getPlayer().getName(), e.getId());
            } else if(e.getEventName().equals(CourierDeliveryEvent.COURIER_READ)) {
                plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " has read the letter with id " + e.getId());
                plugin.getCourierdb().setRead(e.getPlayer().getName(), e.getId());
            } else {
                // dude, what?
                plugin.getCConfig().clog(Level.WARNING, "Unknown Courier event " + e.getEventName() + " received!");
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(e.getMaterial() == Material.MAP && e.getItem().containsEnchantment(Enchantment.DURABILITY)) {
            Letter letter = plugin.getLetter(e.getItem());
            if(letter != null) {
                plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " navigating letter");
                Action act = e.getAction();
                if(act == Action.LEFT_CLICK_BLOCK || act == Action.LEFT_CLICK_AIR) {
                    letter.backPage();
                    e.setCancelled(true);
                } else if(act == Action.RIGHT_CLICK_BLOCK || act == Action.RIGHT_CLICK_AIR) {
                    letter.advancePage();
                    e.setCancelled(true);
                }
            }
        }
    }
    
/*    public void onPlayerAnimation(PlayerAnimationEvent e) {
        plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " animating");
        ItemStack item = e.getPlayer().getItemInHand();
        if(item.getType() == Material.MAP && item.containsEnchantment(Enchantment.DURABILITY)) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " animating letter");
            e.setCancelled(true);
        }
    }*/

    @EventHandler(priority = EventPriority.MONITOR)
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
                    // send an explaining string to the player
                    String inventory = plugin.getCConfig().getInventory();
                    if(inventory != null && !inventory.isEmpty()) {
                        e.getPlayer().sendMessage(inventory);
                    }
                    if(e.getRightClicked() instanceof Enderman) {
                        ((Enderman)e.getRightClicked()).setCarriedMaterial(new MaterialData(Material.AIR));
                    } else if(e.getRightClicked() instanceof Villager) {
                        ((Villager) e.getRightClicked()).setTarget(null);
                    }

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

                if(e.getRightClicked() instanceof Enderman) {
                    ((Enderman)e.getRightClicked()).setCarriedMaterial(new MaterialData(Material.AIR));
                } else if(e.getRightClicked() instanceof Villager) {
                    ((Villager) e.getRightClicked()).setTarget(null);
                }

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_DELIVERED, e.getPlayer(), letter.getEnchantmentLevel(Enchantment.DURABILITY));
                plugin.getServer().getPluginManager().callEvent(event);
            }

            postman.quickDespawn();
        }
    }

    // helper method for converting legacy Courier Letters from MapID to Enchantment Level
    ItemStack legacyConversion(int id, MapView map) {
        if(id == 0) {
            // special case. MapID 0 was a valid Courier Letter, Enchantment Level 0 is not!
            // (actually I was probably wrong about that, but let's go with it anyway)
            int newId = plugin.getCourierdb().generateUID();
            if(newId == -1) {
                plugin.getCConfig().clog(Level.SEVERE, "Out of unique message IDs! Notify your admin!");
                return null;
            }
            plugin.getCConfig().clog(Level.FINE, "Converting legacy Courier Letter 0 to " + newId);
            plugin.getCourierdb().changeId(id, newId);
            id = newId;
        } else {
            plugin.getCConfig().clog(Level.FINE, "Converting legacy Courier Letter id " + id);
        }
        // convert old Courier Letter into new
        ItemStack letterItem = new ItemStack(Material.MAP, 1, plugin.getCourierdb().getCourierMapId());
        // I can trust this id to stay the same thanks to how we handle it in CourierDB
        letterItem.addUnsafeEnchantment(Enchantment.DURABILITY, id);
        // store the date in the db
        plugin.getCourierdb().storeDate(id, map.getCenterZ());
        return letterItem;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent e) {
        if(e.getPlayer().getInventory().getItem(e.getNewSlot()).getType() == Material.MAP) {
            // legacy Courier support
            MapView map = plugin.getServer().getMap(e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability());
            if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
                int id = e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability();
                ItemStack letterItem = legacyConversion(id, map);
                // replacing under the hood
                if(letterItem != null) {
                    e.getPlayer().getInventory().setItem(e.getNewSlot(), letterItem);
                }
            }
            // legacy end
            Letter letter = plugin.getLetter(e.getPlayer().getInventory().getItem(e.getNewSlot()));
            if(letter != null) {
                plugin.getCConfig().clog(Level.FINE, "Switched to Letter id " + letter.getId());

                // quick render
                e.getPlayer().sendMap(plugin.getServer().getMap(plugin.getCourierdb().getCourierMapId()));
            } else { // not needed?
                plugin.getCConfig().clog(Level.FINE, "Id " + e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability() + " is not a Letter");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        if(!e.isCancelled() && e.getItem().getItemStack().getType() == Material.MAP) {
            // legacy Courier support
            MapView map = plugin.getServer().getMap(e.getItem().getItemStack().getDurability());
            if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
                int id = e.getItem().getItemStack().getDurability();
                ItemStack letterItem = legacyConversion(id, map);
                // replacing under the hood
                if(letterItem != null) {
                    e.getItem().setItemStack(letterItem);
                }
            }
            // legacy end
            plugin.getCConfig().clog(Level.FINE, "Letter id " + e.getItem().getItemStack().getEnchantmentLevel(Enchantment.DURABILITY));
            Letter letter = plugin.getLetter(e.getItem().getItemStack());
            if(letter != null) {
                plugin.getCConfig().clog(Level.FINE, "Letter " + letter.getId() + " picked up.");

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if(plugin.getServer().getOnlinePlayers().length <= 1) { // ==
            // last player left
            plugin.pauseDeliveries();
        }
        plugin.getCConfig().clog(Level.FINE, event.getPlayer().getDisplayName() + " has left the building");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(plugin.getServer().getOnlinePlayers().length == 1) {
            // first player joined
            // note: if this ever jumps from 0 to 2 in one go we'll never start deliveries. Implement failsafe?
            plugin.startDeliveries();
        }
        plugin.getCConfig().clog(Level.FINE, event.getPlayer().getDisplayName() + " has joined");
    }

    // if it's a Monster, don't target - it'll attack the player
    // (at least true for Enderman, but maybe not PigZombie?)
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent e) {
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            if(e.getEntity() instanceof Monster) {
                plugin.getCConfig().clog(Level.FINE, "Cancel angry postman");
                e.setCancelled(true);
            }
        }
    }

    // don't allow players to attack postmen
    @EventHandler(priority = EventPriority.HIGH)
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

    // enderpostmen aren't block thieves
    @EventHandler(priority = EventPriority.HIGH)
    public void onEndermanPickup(EndermanPickupEvent e) {
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman thief");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEndermanPlace(EndermanPlaceEvent e) {
        if(!e.isCancelled() && plugin.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman maildrop");
            e.setCancelled(true);
       }
    }

    // Highest since we might need to override spawn deniers
    // in theory we could add another listener at Monitor priority for announce() ..
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if(e.getCreatureType() == plugin.getCConfig().getType()) {
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