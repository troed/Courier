package se.troed.plugin.Courier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Structures and methods used to track things from one point in time to another in Courier
 * 
 * Some of these could in a perfect world be persisted between restarts, but it's an edge case
 */
public class Tracker {
    Courier plugin;

    // tracks postmen from spawn to despawn
    private final Map<UUID, Postman> postmen = new HashMap<UUID, Postman>();
    // letter cache to minimize database access
    private final Map<Integer, Letter> letters = new HashMap<Integer, Letter>();
    // used temporarily in breaking spawn protections as well as making sure we only announce when spawned
    private final Map<Location, Postman> spawners = new HashMap<Location, Postman>();
    // tracks dropped Letters until despawn
    private final Map<UUID, Letter> drops = new HashMap<UUID, Letter>();
    // tracks the last player to right-click a furnace [forever]
    private final Map<Location, String> smelters = new HashMap<Location, String>();

    public Tracker(Courier p) {
        plugin = p;
    }

    public void setSmelter(Location loc, String pn) {
        smelters.put(loc, pn);
    }
    
    public String getSmelter(Location loc) {
        return smelters.get(loc);
    }
    
    // postmen should never live long, will always despawn
    public void addPostman(Postman p) {
        postmen.put(p.getUUID(), p);
        schedulePostmanDespawn(p.getUUID(), plugin.getCConfig().getDespawnTime());
    }

    public Postman getPostman(UUID uuid) {
        return postmen.get(uuid);
    }

    public void clearPostmen() {
        for (Map.Entry<UUID, Postman> uuidPostmanEntry : postmen.entrySet()) {
            Postman postman = (Postman) ((Map.Entry) uuidPostmanEntry).getValue();
            if (postman != null) {
                postman.remove();
            }
        }
    }

    public void addDrop(UUID uuid, Letter l) {
        // if someone were to cancel all item despawns, this would grow forever
        drops.put(uuid, l);
        plugin.getCConfig().clog(Level.FINE, drops.size() + " drops in queue");
    }

    public Letter getAndRemoveDrop(UUID uuid) {
        Letter letter = drops.get(uuid);
        if(letter != null) {
            drops.remove(uuid);
        }
        return letter;
    }

    public void addSpawner(Location l, Postman p) {
        // if this just keeps on growing we could detect and warn the admin that something is blocking
        // even our detection of Postman spawn events. Regular cleanup thread?
        spawners.put(l, p);
        plugin.getCConfig().clog(Level.FINE, spawners.size() + " spawners in queue");
    }

    public Postman getAndRemoveSpawner(Location l) {
        Postman p = spawners.get(l);
        if(p != null) {
            spawners.remove(l);
        }
        return p;
    }

    public void clearSpawners() {
        spawners.clear();
    }

    public void removeLetter(int id) {
        letters.remove(id);
    }

    private void addLetter(int id, Letter l) {
        letters.put(id, l);
    }

    // finds the Letter associated with a specific item
    // recreates structure from db after each restart as needed
    public Letter getLetter(ItemStack letterItem) {
        if(letterItem == null || letterItem.getType() != Material.MAP || !letterItem.containsEnchantment(Enchantment.DURABILITY)) {
            return null;
        }
        int id = letterItem.getEnchantmentLevel(Enchantment.DURABILITY);
        Letter letter = letters.get(id);
        if(letter == null) {
            // server has lost the ItemStack<->Letter associations, re-populate
            if(plugin.getDb().isValid(id)) {
                letter = new Letter(plugin, id);
                addLetter(id, letter);
                plugin.getCConfig().clog(Level.FINE, "Letter " + id + " recreated from db for " + plugin.getDb().getPlayer(id));
            } else {
                // we've found an item pointing to a Courier letter that does not exist in the db anylonger
                // ripe for re-use!
                // todo: visual effect and then removing the item?
                plugin.getCConfig().clog(Level.FINE, "BAD: " + id + " not found in messages database");
            }
        }
        return letter;
    }

    private void despawnPostman(UUID uuid) {
        plugin.getCConfig().clog(Level.FINE, "Despawning postman " + uuid);
        Postman postman = postmen.get(uuid);
        if(postman != null) {
            postman.remove();
            postmen.remove(uuid);
        } // else, shouldn't happen
    }

    public void schedulePostmanDespawn(final UUID uuid, int time) {
        // if there's an existing (long) timeout on a postman and a quick comes in, cancel the first and start the new
        // I don't know if it's long ... but I could add that info to Postman
        Runnable runnable = postmen.get(uuid).getRunnable();
        if(runnable != null) {
            plugin.getCConfig().clog(Level.FINE, "Cancel existing despawn on Postman " + uuid);
            plugin.getServer().getScheduler().cancelTask(postmen.get(uuid).getTaskId());
        }
        runnable = new Runnable() {
            public void run() {
                despawnPostman(uuid);
            }
        };
        postmen.get(uuid).setRunnable(runnable);
        // in ticks. one tick = 50ms
        plugin.getCConfig().clog(Level.FINE, "Scheduled " + time + " second despawn for Postman " + uuid);
        int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable, time*20);
        if(taskId >= 0) {
            postmen.get(uuid).setTaskId(taskId);
        } else {
            plugin.getCConfig().clog(Level.WARNING, "Despawning task scheduling failed");
        }
    }
}