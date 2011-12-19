package se.troed.plugin.Courier;

//import org.bukkit.Server;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Send mail by typing /courier Playername message
 *
 * Spawn postmen delivering maps which render the mails
 * - good way to run into the map limit on a server!! this could very well be an issue
 * - crap. look into map id reusing. Must keep track of all IDs generated as _mail_ and then ...
 *         ... reuse them somehow.
 *
 * Overkill to spawn a postman picking up the rendered mail-to-send as well?
 *
 * Very online/offline playerlist - else just "player not found" message back
 * - include possibility to list. what to do on servers with a gazillion players?
 *
 * Need a List of Letters (list, really?) that persists - use MapDataStorage idea!?
 * Also handle a List of Postman somehow
 *
 * todo: in open spaces the endermen teleport away INSTANTLY - kind of ruining the whole thing. How to fix??
 * still can't see there being a stop-enderman-teleporting-event
 * shall I try to edit Bukkit/CraftBukkit and make a pull request?
 *
 * Idea for recycling maps: Use x,y,z or center etc to create a unique identifier for courier maps
 * could even put date in there and then recycle all maps older than a certain date
 * Q: how to remove them from previous owners? getId() gives me the map but I don't seem to be able to
 *    go from there to ItemStack to actually move the item
 *    Duh, obviously not. There can be any number of ItemStacks all pointing to the same mapId
 *
 * clever: MapDataStorage - encode receiver's name as data in top pixels, listen for the event and recreate renderer
 *
 // idea: extract receiver from our MapDataStorage, then new Letter()
 // and initialize this MapView with that LetterRenderer
 // the map will have gotten the graphics with sender and actual text on creation
 // -- UH NO. After a server restart (our renderers deallocated) these are regular
 // -- maps that will render from our X,Z!! ALL info will need to be save in the actual
 // -- map, including sender and message.

 // only other option is to flatfile store all info in a plugin file and look it up
 // through mapId. Added benefit: We know all the mapId's and we need that anyway
 // to know whether the mail has been delivered or not.
 // -- maybe that's the best first version ...

 // but we need the receiver to verify in onRender
 // - but, won't that change the map making it unreadable for the real recipient? problem
 // - that will REQUIRE us to save sender and message in MapDataStorage as well!

 // map palette per pixel = 56, should be able to store ~0.5 characters per pixel

 // if date == "too old", burn the map and remove the item (is it even possible - do we ever get the item?)
 // - maybe in render, we "know" the user is then actively holding the map in her hands

 // todo: 	for (Entity e : location.getWorld().getEntities())
 // https://vserver.miykeal.com/~svn/ShowCaseStandalone/src/com/miykeal/showCaseStandalone/Shop.java
 //
 
    How to deal with players who NEVER read their mail. We'll spawn an immense number of postmen
    and Items over time! Currently we actually do not track delivered, maybe I need both delivered 
    as well as read? Done.

 http://www.minecraftwiki.net/wiki/Map_Item_Format
 */
public class Courier extends JavaPlugin {
    // these must match plugin.yml
    public static final String CMD_POSTMAN = "postman";
    public static final String CMD_COURIER = "courier";
    public static final String PM_POSTMAN = "courier.postman";
    public static final String PM_SEND = "courier.send";
    public static final int MAGIC_NUMBER = Integer.MAX_VALUE - 395743; // used to id our maps

    private final CourierEntityListener entityListener = new CourierEntityListener(this);
    private final CourierPlayerListener playerListener = new CourierPlayerListener(this);
    private final CourierServerListener serverListener = new CourierServerListener(this);
    private final CourierDeliveryListener deliveryListener = new CourierDeliveryListener(this);
    private final CourierCommands courierCommands = new CourierCommands(this);
    private final CourierDB courierdb = new CourierDB(this);
    private CourierConfig config;

    private Runnable deliveryThread;
    private int deliveryId = -1;
//    private final Map<UUID, Runnable> despawners = new HashMap<UUID, Runnable>();
//    private int taskId = -1;
    private final Map<UUID, Postman> postmen = new HashMap<UUID, Postman>();
    private final Map<Integer, Letter> letters = new HashMap<Integer, Letter>();

    // postmen should never live long, will always despawn
    public void addPostman(Postman p) {
        postmen.put(p.getUUID(), p);
        schedulePostmanDespawn(p.getUUID(), getCConfig().getDespawnTime());
    }

    // returns null if it's not one of ours
    public Postman getPostman(UUID uuid) {
        return postmen.get(uuid);
    }
    
    public void addLetter(short id, Letter l) {
        letters.put(new Integer(id),l);
    }

    // finds the Letter associated with a specific Map
    // making this hashmap persistent might save us a lot of list-searching, then only using this
    // as fallback
    public Letter getLetter(MapView map) {
        if(map == null) { // safety first
            return null;
        }
        Letter letter = null;
        if(!letters.containsKey(new Integer(map.getId()))) {
            // server has lost the MapView<->Letter associations, re-populate
            short id = map.getId();
            String to = getCourierdb().getPlayer(id);
            if(to != null) {
                String from = getCourierdb().getSender(to, id);
                String message = getCourierdb().getMessage(to, id);
                letter = new Letter(from, to, message, getCourierdb().getRead(to, id));
                letter.initialize(map); // does this make a difference at all?
                List<MapRenderer> renderers = map.getRenderers();
                for(MapRenderer r : renderers) { // remove existing renderers
                    map.removeRenderer(r);
                }
                map.addRenderer(letter);
                addLetter(id, letter);
            } else {
                // we've found an item pointing to a Courier letter that does not exist anylonger
                // ripe for re-use!
                getCConfig().clog(Level.FINE, "BAD: " + id + " not found in messages database");
            }
        } else {
            letter = letters.get(new Integer(map.getId()));
        }
        return letter;
    }

    /**
     * Picks a spot suitably in front of the player's eyes and checks to see if there's room 
     * for a postman (Enderman) to spawn in line-of-sight
     * 
     * Currently this can fail badly not checking whether we're on the same Y ..
     *
     * Also: Should be extended to check at least a few blocks to the sides and not JUST direct line of sight
     * seems this can fail and spawn an enderman "half" into the block to the side, thus taking damage
     * seems to always happen? need to check a 3x3x3 area instead of 1x1x3?
     *
     * todo: don't spawn postmen outdoors when it's raining!
     */
    Location findSpawnLocation(Player p) {
        Location sLoc = null;

        // o,o,o,o,o,o,x 
        List<Block> blocks = p.getLineOfSight(null, getCConfig().getSpawnDistance());
        if(blocks != null && !blocks.isEmpty()) {
            Block block = blocks.get(blocks.size()-1); // get last block
            getCConfig().clog(Level.FINE, "findSpawnLocation got lineOfSight");
            if(!block.isEmpty() && blocks.size()>1) {
                getCConfig().clog(Level.FINE, "findSpawnLocation got non-air last block");
                block = blocks.get(blocks.size()-2); // this SHOULD be an air block, then
            }
            if(block.isEmpty()) {
                // find bottom
                getCConfig().clog(Level.FINE, "findSpawnLocation air block");
                while(block.getRelative(BlockFace.DOWN, 1).isEmpty()) {
                    getCConfig().clog(Level.FINE, "findSpawnLocation going down ...");
                    block = block.getRelative(BlockFace.DOWN, 1);
                }
                // verify this is something we can stand on and that we fit
                if(!block.getRelative(BlockFace.DOWN, 1).isLiquid() && block.getRelative(BlockFace.UP, 1).isEmpty() && block.getRelative(BlockFace.UP, 2).isEmpty()) {
                    getCConfig().clog(Level.FINE, "findSpawnLocation got location!");
                    Location tLoc = block.getLocation();
                    // could it be the fractions that gives us a position that looks ok but then spawns the enderman half inside
                    // a block anyway? Floor it.
                    // nope, made no difference whatsoever

                    // is there a rule as to the spawn point being "center" and I get "corner"?
                    // this is a pure guess .. but seems to be correct at least for Z. Check sign for X as well.
                    sLoc = new Location(tLoc.getWorld(), tLoc.getBlockX()+0.5, tLoc.getBlockY(), tLoc.getBlockZ()+0.5);
                }
            }
        }
            
        if(sLoc == null) {
            getCConfig().clog(Level.FINE, "Didn't find room to spawn Postman");
            // fail
        }

        return sLoc;
    }

    public CourierDB getCourierdb() {
        return courierdb;
    }

    private void despawnPostman(UUID uuid) {
        config.clog(Level.FINE, "Despawning postman " + uuid);
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
            config.clog(Level.FINE, "Cancel existing despawn on Postman " + uuid);
            getServer().getScheduler().cancelTask(postmen.get(uuid).getTaskId());    
        }
        runnable = new Runnable() {
            public void run() {
                despawnPostman(uuid);
            }
        };
        postmen.get(uuid).setRunnable(runnable);
        // in ticks. one tick = 50ms
        config.clog(Level.FINE, "Scheduled " + time + " second despawn for Postman " + uuid);
        int taskId = getServer().getScheduler().scheduleSyncDelayedTask(this, runnable, time*20);
        if(taskId >= 0) {
            postmen.get(uuid).setTaskId(taskId);
        } else {
            config.clog(Level.WARNING, "Despawning task scheduling failed");
        }
    }
    
    private void startDeliveryThread() {
        if(deliveryId >= 0) {
            config.clog(Level.WARNING, "Multiple calls to startDelivery()!");
        }
        if(deliveryThread == null) {
            deliveryThread = new Runnable() {
                public void run() {
                    deliverMail(); 
                }
            };
        }
        deliveryId = getServer().getScheduler().scheduleSyncRepeatingTask(this, deliveryThread, getCConfig().getInitialWait()*20, getCConfig().getNextRoute()*20);
        if(deliveryId < 0) {
            config.clog(Level.WARNING, "Delivery task scheduling failed");
        }
    }
    
    private void stopDeliveryThread() {
        if(deliveryId != -1) {
            getServer().getScheduler().cancelTask(deliveryId);
            deliveryId = -1;
        }
    }
    
    private void deliverMail() {
        // find first online player with undelivered mail
        // spawn new thread to deliver the mail
        Player[] players = getServer().getOnlinePlayers();
        for(int i=0; i<players.length; i++) {
            // I really need to remember which players have had a postman sent out even if they
            // haven't read their mail. Time to separate delivered and read ... maybe even picked up as well?
            // currently picked up count as delivered, maybe that's what it should as well :)

            // hmm I made all these changes and nothing uses read atm. Weird.
            if(courierdb.undeliveredMail(players[i].getName())) {
                // is this lookup slow? it saves us in the extreme case new deliveries are scheduled faster than despawns
                if(!postmen.containsValue(players[i])) {
                    short undeliveredMessageId = getCourierdb().undeliveredMessageId(players[i].getName());
                    if(undeliveredMessageId != -1) {
                        Location spawnLoc = findSpawnLocation(players[i]);
                        if(spawnLoc != null) {
                            Postman postman = new Postman(this, players[i], spawnLoc, undeliveredMessageId);
                            this.addPostman(postman);
                        }
                    } else {
                        config.clog(Level.SEVERE, "undeliveredMail and undeliveredMessageId not in sync: " + undeliveredMessageId);
                    }
                }
            }
        }
    }
           
/*    private void stopTask() {
        if(taskId < 0) {
            return;
        }
        config.clog(Level.FINE, "Task stop");
        getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    //        runnable = null;
    }*/

    public void onDisable() {
//        stopTask();
        // stopDeliveryThread();
        getServer().getScheduler().cancelTasks(this);
        Iterator iter = postmen.entrySet().iterator();
        while(iter.hasNext()) {
            Postman postman = (Postman)((Map.Entry)iter.next()).getValue();
            if(postman != null) {
                postman.remove();
            }
        }
        courierdb.save();
        config.clog(Level.FINE, this.getDescription().getName() + " is now disabled.");
    }

    public void onEnable() {
        this.loadConfig();
        saveConfig();
        courierdb.load();

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENDERMAN_PICKUP, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENDERMAN_PLACE, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.MAP_INITIALIZE, serverListener, Priority.Normal, this);
//        pm.registerEvent(Event.Type.SERVER_COMMAND, courierCommands, Priority.Normal, this);
        pm.registerEvent(Event.Type.CUSTOM_EVENT, deliveryListener, Priority.Normal, this);

        getCommand(CMD_POSTMAN).setExecutor(courierCommands);
        getCommand(CMD_COURIER).setExecutor(courierCommands);

        // I kind of think I need a few task here
        // 1: Every now and then, go through online players and check if there's a letter waiting
        //    for anyone of them. Or: 
        // 1: OnPlayerJoin, OnPlayerLeavingBed, start task with random(5) waiting time before checking
        //    their mail. What's the best user experience?
        //
        // 2: For each instantiated postman, start a despawn task to fire after a config amount of 
        //    seconds. 10 as default?
        // 3: QuickDespawn cancels the task above and adds a new one for config (3) instead.

        startDeliveryThread();
        
        PluginDescriptionFile pdfFile = this.getDescription();
        config.clog(Level.INFO, pdfFile.getName() + " version v" + pdfFile.getVersion() + " is enabled!");
      }

    // in preparation for plugin config dynamic reloading
    public void loadConfig() {
        getConfig().options().copyDefaults(true);

        config = new CourierConfig(this);
    }

    public CourierConfig getCConfig() {
        return config;
    }

}
