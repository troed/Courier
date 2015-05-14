package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

@SuppressWarnings("UnusedDeclaration")
class CourierEventListener implements Listener {
    private final Courier plugin;
    private final Tracker tracker;

    public CourierEventListener(Courier instance) {
        plugin = instance;
        tracker = plugin.getTracker();
    }

    // todo: Bukkit 1.4.6 R0.1 - I don't seem to get WorldLoadEvents. Why?
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent e) {
        plugin.getCConfig().clog(Level.FINE, "World " + e.getWorld().getName() + " load");
        if(plugin.getServer().getWorlds().size() == 1) {
            // first world loaded - it's also the default in all cases I've seen [world(0)]
            plugin.postWorldLoad();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onCourierDeliveryEvent(CourierDeliveryEvent e) {
        if(e.getPlayer()!=null && e.getId()!=-1) {
            plugin.getCConfig().clog(Level.FINE, "Delivered letter to " + e.getPlayer().getName() + " with id " + e.getId());
            plugin.getCourierdb().setDelivered(e.getPlayer().getName(), e.getId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onCourierReadEvent(CourierReadEvent e) {
        if(e.getPlayer()!=null && e.getId()!=-1) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " has read the letter with id " + e.getId());
            plugin.getCourierdb().setRead(e.getPlayer().getName(), e.getId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if(plugin.courierMapType(item) == Courier.LETTER) {
 // todo: would it be awesome to allow page flipping in itemframes or do people want rotation instead?
 //       if(e.getMaterial() == Material.MAP && e.getItem().containsEnchantment(Enchantment.DURABILITY)) {
            Letter letter = tracker.getLetter(item);
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
        } else if(!e.isCancelled()) {
            // we need to track players who right click furnaces
            Block block = e.getClickedBlock();
            if(block != null) {
                BlockState blockState = block.getState();
                if(blockState != null) {
                    if(blockState.getType() == Material.FURNACE) {
                        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getName() + " is using a furnace");
                            tracker.setSmelter(e.getClickedBlock().getState().getBlock().getLocation(), e.getPlayer());
                        }
                    }
                } else {
                    plugin.getCConfig().clog(Level.FINE, "null BlockState in getPlayerInteract");
                }
            } else {
                plugin.getCConfig().clog(Level.FINE, "null Block in getPlayerInteract");
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItemEvent(PlayerDropItemEvent e) {
        Letter letter = tracker.getLetter(e.getItemDrop().getItemStack());
        if(!e.isCancelled() && letter != null && letter.isAllowedToSee(e.getPlayer())) {
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
            plugin.getCourierdb().deleteMessage((short)letter.getId());
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
            Player p = tracker.getSmelter(e.getBlock().getLocation());
            if(p != null && letter.isAllowedToSee(p)) {
                tracker.removeLetter(letter.getId());
                plugin.getCourierdb().deleteMessage((short)letter.getId());
                plugin.getCConfig().clog(Level.FINE, "Letter " + letter.getId() + " was burnt in a furnace by " + p.getName() + ", removed from database");
            }
        }
        // avoid NPE by manually faking the intended result and cancelling event
        // todo: https://bukkit.atlassian.net/browse/BUKKIT-745
        Furnace furnace = (Furnace) e.getBlock().getState();
        furnace.getInventory().clear(0); // 0 = ingredient slot
        // here would be some magic as to understanding whether to decrease the amount of fuel .. not trivial
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        ItemStack item = e.getPlayer().getItemInHand();

        // Did we put a map into an ItemFrame?
        if(e.getRightClicked().getType() == EntityType.ITEM_FRAME) {
            // Is it a Letter?
            int type = plugin.courierMapType(item);
            if(type != Courier.NONE) {
                Letter letter = tracker.getLetter(item);
                if(plugin.getCConfig().getLetterFrameable() &&
                            e.getPlayer().hasPermission(Courier.PM_USEITEMFRAMES) &&
                            (letter == null || letter.isAllowedToSee(e.getPlayer())) &&
                            type != Courier.PARCHMENT) {
                    if(type == Courier.LETTER) {
                        // Regular Courier Letter using our shared Map - convert to unique Map for ItemFrame use
                        plugin.getCConfig().clog(Level.FINE, "Courier Letter placed into ItemFrame");
                        MapView newMap = plugin.getServer().createMap(plugin.getServer().getWorlds().get(0));
                        newMap.setCenterX(Courier.MAGIC_NUMBER);
                        // the one and only rendering map uses 0, but we need the database key stored in pure Map data here
                        newMap.setCenterZ(item.getEnchantmentLevel(Enchantment.DURABILITY));
                        List<MapRenderer> renderers = newMap.getRenderers();
                        for(MapRenderer r : renderers) { // remove existing renderers
                            newMap.removeRenderer(r);
                        }
                        newMap.addRenderer(new FramedLetterRenderer(plugin));
                        ItemStack mapItem = new ItemStack(Material.MAP, 1, newMap.getId());
                        // Copy the same Enchantment level (our database lookup key) to the new ItemStack
                        mapItem.addUnsafeEnchantment(Enchantment.DURABILITY, item.getEnchantmentLevel(Enchantment.DURABILITY));

                        // use Lore to add info on current page
                        ItemMeta meta = mapItem.getItemMeta();
                        if(meta != null && letter != null) {
                            List<String> strings = meta.getLore();
                            if(strings == null) {
                                strings = new ArrayList<String>();
                            }
                            strings.add(letter.getCurPage() + "/" + letter.getPageCount());
                            meta.setLore(strings);
                            mapItem.setItemMeta(meta);
                        }
                        e.getPlayer().setItemInHand(mapItem); // Replace the Courier Letter the player had with the new unique Map
                        //noinspection ConstantConditions
                        letter.setDirty(true);
                    } else if(type == Courier.FRAMEDLETTER) {
                        // Probably never happens since we convert them back on pickup and heldchange into regular Courier Letters
                        plugin.getCConfig().clog(Level.FINE, "Courier Framed Letter placed into ItemFrame");
                    }
                } else {
                    // If we don't allow ItemFrames - block the interaction
                    // Also block placing parchments since we are not able to separate them from letters in LetterRenderer.render
                    //  when players hold a Letter and look at the ItemFrame
                    plugin.getCConfig().clog(Level.FINE, "Blocked Courier Letter into ItemFrame");
                    // todo: feedback string to player
                    e.setCancelled(true);
                }
            }
        }

        // Did we right click a Postman?
        Postman postman = tracker.getPostman(e.getRightClicked().getUniqueId());
        if(!e.isCancelled() && !e.getRightClicked().isDead() && postman != null && !postman.scheduledForQuickRemoval()) {
            plugin.getCConfig().clog(Level.FINE, e.getPlayer().getDisplayName() + " receiving mail");
            ItemStack letter = postman.getLetterItem();

            if(item != null && item.getAmount() > 0) {
                plugin.getCConfig().clog(Level.FINE, "Player hands not empty");
                HashMap<Integer, ItemStack> items = e.getPlayer().getInventory().addItem(letter);
                if(items.isEmpty()) {
                    plugin.getCConfig().clog(Level.FINE, "Letter added to inventory");
                    // send an explaining string to the player
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

                // todo: quick render
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

            // Only cancelling the event for Villagers, less possible legacy issues
            // There might be a good reason for cancelling it for all entities though
            if(e.getRightClicked() instanceof Villager) {
                plugin.getCConfig().clog(Level.FINE, "Cancel Villager trading screen");
                e.setCancelled(true);
            }

            postman.quickDespawn();
        }
    }

    // helper method for converting legacy Courier Letters from MapID to Enchantment Level
    // also used when converting Maps that have been placed in ItemFrames back to regular Courier Maps
    ItemStack convertMap(int id) {
        if(id == 0) {
            // special case. MapID 0 was a valid Courier Letter, Enchantment Level 0 is not!
            int newId = plugin.getCourierdb().generateUID();
            if(newId == -1) {
                plugin.getCConfig().clog(Level.SEVERE, "Out of unique message IDs! Notify your admin!");
                return null;
            }
            plugin.getCConfig().clog(Level.FINE, "Converting unique Courier Letter 0 to " + newId);
            plugin.getCourierdb().changeId(id, newId);
            id = newId;
        } else {
            plugin.getCConfig().clog(Level.FINE, "Converting unique Courier Letter id " + id);
        }
        // convert old Courier Letter into new
        ItemStack letterItem = new ItemStack(Material.MAP, 1, plugin.getCourierdb().getCourierMapId());
        // I can trust this id to stay the same thanks to how we handle it in CourierDB
        letterItem.addUnsafeEnchantment(Enchantment.DURABILITY, id);
        return letterItem;
    }

    ItemStack convertLegacyMap(int id, MapView map) {
        ItemStack converted = convertMap(id);
        // store the date in the db
        plugin.getCourierdb().storeDate(id, map.getCenterZ());
        return converted;
    }

    // helper method
    // also see similar code in CourierCommands
    private ItemStack updateLore(ItemStack item, Letter letter, Player player) {
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(plugin.getCConfig().getLetterDisplayName());
            List<String> strings = new ArrayList<String>();
            if(letter.isAllowedToSee(player)) {
                strings.add(plugin.getCConfig().getLetterFrom(letter.getSender()));
                strings.add(letter.getTopRow());
            } else {
                strings.add(plugin.getCConfig().getLetterTo(letter.getReceiver()));
            }
            meta.setLore(strings);
            item.setItemMeta(meta);
        } else {
            // ???
        }
        return item;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemHeldChange(PlayerItemHeldEvent e) {
        // http://dev.bukkit.org/server-mods/courier/tickets/36-severe-could-not-pass-event-player-item-held-event/
        ItemStack item = e.getPlayer().getInventory().getItem(e.getNewSlot());
        if(item != null && item.getType() == Material.MAP) {
            // convert legacy and ItemFrame maps
            MapView map = plugin.getServer().getMap(item.getDurability());
            if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
                ItemStack letterItem = null;
                if(item.containsEnchantment(Enchantment.DURABILITY)) {
                    // unique map item having been freed from its ItemFrame
                    int id = item.getEnchantmentLevel(Enchantment.DURABILITY);
                    plugin.getCConfig().clog(Level.FINE, "Converting unique Courier Letter id " + id);
                    letterItem = convertMap(id);
                } else {
                    // legacy Courier map from pre v1.0.0 days
                    int id = item.getDurability();
                    plugin.getCConfig().clog(Level.FINE, "Converting legacy Courier Letter id " + id);
                    letterItem = convertLegacyMap(id, map);
                }
                // replacing under the hood
                if(letterItem != null) {
                    item = letterItem;
                }
            }
            // conversion end
            Letter letter = tracker.getLetter(item);
            if(letter != null) {
                e.getPlayer().getInventory().setItem(e.getNewSlot(), updateLore(item, letter, e.getPlayer()));

                plugin.getCConfig().clog(Level.FINE, "Switched to Letter id " + letter.getId());

                // quick render
                letter.setDirty(true);
            } else {
                // or regular map
                // or parchment?
                // or MapItem we have pointing to a Letter that has been deleted. If enchanted (== proof) we could
                //    just decided to delete it here
                plugin.getCConfig().clog(Level.FINE, "Switched to blank parchment");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        if(!e.isCancelled() && e.getItem().getItemStack().getType() == Material.MAP) {
            // convert legacy and ItemFrame maps
            ItemStack item = e.getItem().getItemStack();
            MapView map = plugin.getServer().getMap(item.getDurability());
            if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
                ItemStack letterItem = null;
                if(item.containsEnchantment(Enchantment.DURABILITY)) {
                    // unique map item having been freed from its ItemFrame
                    int id = item.getEnchantmentLevel(Enchantment.DURABILITY);
                    letterItem = convertMap(id);
                } else {
                    // legacy Courier map from pre v1.0.0 days
                    int id = item.getDurability();
                    letterItem = convertLegacyMap(id, map);
                }
                // replacing under the hood
                if(letterItem != null) {
                    item = letterItem;
                }
            }
            // conversion end
            Letter letter = tracker.getAndRemoveDrop(e.getItem().getUniqueId());
            if(letter != null) {
                // if someone picked up a drop we were tracking, remove it from here
                plugin.getCConfig().clog(Level.FINE, "Letter id " + letter.getId() + " was dropped and picked up again");
            }
            letter = tracker.getLetter(item);
            if(letter != null) {
                e.getItem().setItemStack(updateLore(item, letter, e.getPlayer()));
                // todo: need to updateInventory for lore to be reflected correctly on picked up Framed Letters?

                plugin.getCConfig().clog(Level.FINE, "Letter " + letter.getId() + " picked up.");

                // delivered
                CourierDeliveryEvent event = new CourierDeliveryEvent(e.getPlayer(), letter.getId());
                plugin.getServer().getPluginManager().callEvent(event);

                // if itemheldhand was empty, we should render the letter immediately
                ItemStack heldItem = e.getPlayer().getItemInHand();
                if(heldItem != null && heldItem.getAmount() == 0) {
                    letter.setDirty(true);
                }
            } else {
                // or regular map
                // or parchment?
                // or MapItem we have pointing to a Letter that has been deleted. If enchanted (== proof) we could
                //    just decided to delete it here
                plugin.getCConfig().clog(Level.FINE, "Picked up blank parchment");
            }
        }        
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e) {
        // immediately return if we don't support ItemFrames or if it's a newly generated chunk
        if(!plugin.getCConfig().getLetterFrameable() || e.isNewChunk()) {
            return;
        }
        Entity[] entities = e.getChunk().getEntities();
        int found = 0;
        for(Entity entity : entities) {
            if(entity instanceof ItemFrame) {
                ItemStack item = ((ItemFrame)entity).getItem();
                if(plugin.courierMapType(item) == Courier.FRAMEDLETTER) {
                    MapView map = plugin.getServer().getMap(item.getDurability());
                    if(item.hasItemMeta() && item.getItemMeta().hasLore()) {
                        // Lore
                        ItemMeta meta = item.getItemMeta();
                        int page = 0;
                        ListIterator iter = meta.getLore().listIterator(meta.getLore().size());
                        while(iter.hasPrevious()) {
                            String[] strings = ((String)iter.previous()).split("/");
                            if(strings.length > 1) {
                                // starting from the back the first '/' delimeter we find is for our our page info
                                page = Integer.valueOf(strings[0]);
                                plugin.getCConfig().clog(Level.FINE, "Extracted page info: " + page);
                                break;
                            }
                        }
                        if(page > 0) {
                            Letter letter = tracker.getLetter(map.getCenterZ());
                            if(letter!=null) {
                                letter.setCurPage(page);
                            }
                        }
                    }
                    List<MapRenderer> renderers = map.getRenderers();
                    for(MapRenderer r : renderers) { // remove existing renderers
                        map.removeRenderer(r);
                    }
                    map.addRenderer(new FramedLetterRenderer(plugin));
                    found++;
                }
            }
        }
        if(found > 0) {
            plugin.getCConfig().clog(Level.FINE, "Found " + found + " Courier Maps in ItemFrames, re-added renderers");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if(plugin.getServer().getOnlinePlayers().toArray().length <= 1) { // ==
            // last player left
            plugin.pauseDeliveries();
        }
        plugin.getCConfig().clog(Level.FINE, event.getPlayer().getDisplayName() + " has left the building");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(plugin.getServer().getOnlinePlayers().toArray().length == 1) {
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
        if(e.getEntity().getType() == plugin.getCConfig().getType()) {
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
