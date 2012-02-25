package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.logging.Level;

class CourierEventListener implements Listener {
    private final Courier plugin;
    private final Tracker tracker;

    public CourierEventListener(Courier p) {
        plugin = p;
        tracker = p.getTracker();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onCourierDeliveryEvent(CourierDeliveryEvent e) {
        if(e.getPlayer()!=null && e.getId()!=-1) {
            plugin.getCConfig().clog(Level.FINE, "Delivered letter to " + e.getPlayer().getName() + " with id " + e.getId());
            plugin.getDb().setDelivered(e.getPlayer().getName(), e.getId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onCourierReadEvent(CourierReadEvent e) {
        if(e.getPlayer()!=null && e.getId()!=-1) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " has read the letter with id " + e.getId());
            plugin.getDb().setRead(e.getId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Letter letter = tracker.getLetter(e.getItem());
        if(letter != null) {
            Action act = e.getAction();
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " navigating letter with action: " + act.name());
            if(act == Action.LEFT_CLICK_BLOCK || act == Action.LEFT_CLICK_AIR) {
                letter.backPage();
                e.setCancelled(true);
            } else if(act == Action.RIGHT_CLICK_BLOCK || act == Action.RIGHT_CLICK_AIR) {
                letter.advancePage();
                e.setCancelled(true);
            }
        } else if(!e.isCancelled()) {
            // we need to track players who right click furnaces
            if(e.getClickedBlock().getState().getType() == Material.FURNACE) {
                if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " is using a furnace");
                    tracker.setSmelter(e.getClickedBlock().getState().getBlock().getLocation(), e.getPlayer().getName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItemEvent(PlayerDropItemEvent e) {
        Letter letter = tracker.getLetter(e.getItemDrop().getItemStack());
        if(!e.isCancelled() && letter != null && letter.isAllowedToSee(e.getPlayer().getName())) {
            tracker.addDrop(e.getItemDrop().getUniqueId(), letter);
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " dropped Letter " + letter.getId());
        }
    }

    // Letters can be deleted by letting them despawn
    // However, if the chunk is unloaded, will we ever get this event?
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawnEvent(ItemDespawnEvent e) {
        if(e.isCancelled()) {
            return;
        }
        Letter letter = tracker.getAndRemoveDrop(e.getEntity().getUniqueId());
        if(letter != null) {
            tracker.removeLetter(letter.getId());
            plugin.getLetterRenderer().forceClear();
            plugin.getDb().deleteMessage((short)letter.getId());
            plugin.getCConfig().clog(Level.FINE, "Dropped Letter " + letter.getId() + " despawned, was removed from database");
        }
    }

    // Letters (and yes, normal Maps as well) can be deleted by burning them in furnaces
    // Priority Highest until the NPE is fixed. We added the recipe - we must cancel the event
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnaceSmeltEvent(FurnaceSmeltEvent e) {
        if(e.isCancelled() || e.getSource().getType() != Material.MAP) {
            return;            
        }
        Letter letter = tracker.getLetter(e.getSource());
        if(letter != null) {
            // verify that the player who last right-clicked this furnace "owns" the letter and can delete it
            String pn = tracker.getSmelter(e.getFurnace().getLocation());
            if(pn != null && letter.isAllowedToSee(pn)) {
                tracker.removeLetter(letter.getId());
                plugin.getLetterRenderer().forceClear();
                plugin.getDb().deleteMessage((short)letter.getId());
                plugin.getCConfig().clog(Level.FINE, "Letter " + letter.getId() + " was burnt in a furnace by " + pn + ", removed from database");
            }
        }

        // avoid NPE by manually faking the intended result and cancelling event
        // https://bukkit.atlassian.net/browse/BUKKIT-745
        Furnace furnace = (Furnace) e.getFurnace().getState();
        furnace.getInventory().clear(0); // 0 = ingredient slot
        // here would be some magic as to understanding whether to decrease the amount of fuel .. not trivial
        e.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Postman postman = tracker.getPostman(e.getRightClicked().getUniqueId());
        if(!e.isCancelled() && !e.getRightClicked().isDead() && postman != null && !postman.scheduledForQuickRemoval()) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " receiving mail");
            ItemStack letter = postman.getLetterItem();

            ItemStack item = e.getPlayer().getItemInHand();
            if(item != null && item.getAmount() > 0) {
                plugin.getCConfig().clog(Level.FINE, "Player hands not empty");
                HashMap<Integer, ItemStack> items = e.getPlayer().getInventory().addItem(letter);
                if(items.isEmpty()) {
                    plugin.getCConfig().clog(Level.FINE, "Letter added to inventory");
                    Courier.display(e.getPlayer(), plugin.getCConfig().getInventory());
                    if(e.getRightClicked() instanceof Enderman) {
                        ((Enderman)e.getRightClicked()).setCarriedMaterial(new MaterialData(Material.AIR));
                    } else {
                        ((Creature) e.getRightClicked()).setTarget(null);
                    }

                    // delivered
                    CourierDeliveryEvent event = new CourierDeliveryEvent(e.getPlayer(), letter.getEnchantmentLevel(Enchantment.DURABILITY));
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
                } else {
                    ((Creature) e.getRightClicked()).setTarget(null);
                }

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(e.getPlayer(), letter.getEnchantmentLevel(Enchantment.DURABILITY));
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
            int newId;
            try {
                newId = plugin.getDb().generateUID();
            } catch (InternalError e) {
                plugin.getCConfig().clog(Level.SEVERE, "Out of unique message IDs! Notify your admin!");
                return null;
            }
            plugin.getCConfig().clog(Level.FINE, "Converting legacy Courier Letter 0 to " + newId);
            plugin.getDb().changeId(id, newId);
            id = newId;
        } else {
            plugin.getCConfig().clog(Level.FINE, "Converting legacy Courier Letter id " + id);
        }
        // convert old Courier Letter into new
        ItemStack letterItem = new ItemStack(Material.MAP, 1, plugin.getCourierdb().getCourierMapId());
        // I can trust this id to stay the same thanks to how we handle it in CourierDB
        letterItem.addUnsafeEnchantment(Enchantment.DURABILITY, id);
        // store the date in the db
        plugin.getDb().storeDate(id, map.getCenterZ());
        return letterItem;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent e) {
        if(e.getPlayer().getInventory().getItem(e.getNewSlot()).getType() == Material.MAP) {
            // legacy Courier support
            MapView map = plugin.getServer().getMap(e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability());
            // todo: actually, if it's enchanted we should just switch to the new map_id
            if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
                int id = e.getPlayer().getInventory().getItem(e.getNewSlot()).getDurability();
                ItemStack letterItem = legacyConversion(id, map);
                // replacing under the hood
                if(letterItem != null) {
                    e.getPlayer().getInventory().setItem(e.getNewSlot(), letterItem);
                } else {
                    Courier.display(e.getPlayer(), plugin.getCConfig().getLetterNoMoreUIDs());
                }
            }
            // legacy end
            Letter letter = tracker.getLetter(e.getPlayer().getInventory().getItem(e.getNewSlot()));
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
            // todo: actually, if it's enchanted we should just switch to the new map_id
            if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
                int id = e.getItem().getItemStack().getDurability();
                ItemStack letterItem = legacyConversion(id, map);
                // replacing under the hood
                if(letterItem != null) {
                    e.getItem().setItemStack(letterItem);
                } else {
                    Courier.display(e.getPlayer(), plugin.getCConfig().getLetterNoMoreUIDs());
                }
            }
            // legacy end
            Letter letter = tracker.getAndRemoveDrop(e.getItem().getUniqueId());
            if(letter != null) {
                // if someone picked up a drop we were tracking, remove it from here
                plugin.getCConfig().clog(Level.FINE, "Letter id " + letter.getId() + " was dropped and picked up again");
            }
            letter = tracker.getLetter(e.getItem().getItemStack());
            if(letter != null) {
                plugin.getCConfig().clog(Level.FINE, "Letter " + letter.getId() + " picked up.");

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(e.getPlayer(), letter.getId());
                plugin.getServer().getPluginManager().callEvent(event);

                // if itemheldhand was empty, we should render the letter immediately
                ItemStack item = e.getPlayer().getItemInHand();
                if(item != null && item.getAmount() == 0) {
                    e.getPlayer().sendMap(plugin.getServer().getMap(plugin.getCourierdb().getCourierMapId()));
                }
            }
        }        
    }

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
        if(!e.isCancelled() && tracker.getPostman(e.getEntity().getUniqueId()) != null) {
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
        if(!e.isCancelled() && tracker.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Postman taking damage");
            Postman postman = tracker.getPostman(e.getEntity().getUniqueId());
            if(!e.getEntity().isDead() && !postman.scheduledForQuickRemoval()) {
                postman.drop();
                postman.quickDespawn();
                plugin.getCConfig().clog(Level.FINE, "Drop and despawn");
            } // else already removed
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if(!e.isCancelled() && tracker.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman blockchange");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTeleport(EntityTeleportEvent e) {
        if(!e.isCancelled() && tracker.getPostman(e.getEntity().getUniqueId()) != null) {
            plugin.getCConfig().clog(Level.FINE, "Prevented postman teleport");
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
            Postman postman = tracker.getAndRemoveSpawner(e.getLocation());
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