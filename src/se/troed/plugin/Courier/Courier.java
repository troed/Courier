package se.troed.plugin.Courier;

//import org.bukkit.Server;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Runnable runnable = null;
    private int taskId = -1;
    private Postman postman; // temp, to be hashmap
    private final Map<Integer, Letter> letters = new HashMap<Integer, Letter>();

    // postmen needs to become a hashmap and to have proper appearance and despawning
    public void addPostman(Postman p) {
        // currently setPostman ..
        postman = p;
    }

    public Postman getPostman() {
        return postman;
    }
    
    public void addLetter(short id, Letter l) {
        letters.put(new Integer(id),l);
    }

    // making this hashmap persistent might save us a lot of list-searching, then only using this
    // as fallback
    public Letter getLetter(MapView map) {
        Letter letter = null;
        if(!letters.containsKey(new Integer(map.getId()))) {
            // server has lost the MapView<->Letter associations, re-populate
            short id = map.getId();
            String to = getCourierdb().getPlayer(id);
            if(to != null) {
                String from = getCourierdb().getSender(to, id);
                String message = getCourierdb().getMessage(to, id);
                letter = new Letter(from, to, message);
                letter.initialize(map); // does this make a difference at all?
                List<MapRenderer> renderers = map.getRenderers();
                for(MapRenderer r : renderers) { // remove existing renderers
                    map.removeRenderer(r);
                }
                map.addRenderer(letter);
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

    public void scheduleDespawnPostman() {
        startTask();
    }

    private void despawnPostman() {
        config.clog(Level.FINE, "Removing postman");
        if(postman != null) {
            postman.remove();
        }
        postman = null;
        taskId = -1;
    }

    public CourierDB getCourierdb() {
        return courierdb;
    }

    // ok to be called multiple times
    private void startTask() {
        if(taskId >= 0) {
            config.clog(Level.WARNING, "Task existed");
            return;
        }
        if(runnable == null) {
            runnable = new Runnable() {
                    public void run() {
                        despawnPostman();
                    }
                };
        }
        config.clog(Level.FINE, "Task start");
        // in ticks. one tick = 50ms
        taskId = getServer().getScheduler().scheduleSyncDelayedTask(this, runnable, 60);
        if(taskId < 0) {
            config.clog(Level.WARNING, "Task scheduling failed");
        }
    }

    private void stopTask() {
        if(taskId < 0) {
            return;
        }
        config.clog(Level.FINE, "Task stop");
        getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    //        runnable = null;
    }

    public void onDisable() {
        stopTask();
        despawnPostman();
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
