package se.troed.plugin.Courier;

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Send mail by typing /courier playername message
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
 * Currently implemented 1
 *
 * 1: Every now and then, go through online players and check if there's a letter waiting
 *    for anyone of them. And/Or:
 * 2: OnPlayerJoin, OnPlayerLeavingBed, start task with random(5) waiting time before checking
 *    their mail. What's the best user experience?
 *
 *
 * todo: in open spaces the endermen teleport away INSTANTLY - kind of ruining the whole thing.
 * see: https://bukkit.atlassian.net/browse/BUKKIT-366
 *
 * I'll wait for some responses before deciding whether I'll make a fork for our server.
 *
 * Courier letter maps are uniquely defined by their x-value being 2147087904 (INT_MAX - 395743)
 * - I find it unlikely anyone will ever seriously craft a map at that location, it will have to do.
 * - Other plugin developers can use this fact to skip Courier Letters when they traverse maps
 * - I'll also likely need to use it when I finally solve map recycling.
 *
 * Additionally, Courier letter z-value is the unix timestamp when they were created.
 *
 *
 *
 * How to deal with players who NEVER read their mail. We'll spawn an immense number of postmen
 * and Items over time! I do not track how many times a single mail has been delivered, maybe I should?
 *
 * For recycling purposes, good info:
 * - http://www.minecraftwiki.net/wiki/Map_Item_Format
 *
 *
 * Known Issues:
 * - User does not know how long a message can be
 * -- Additionally console does not accept as long messages as can be viewed with the map item
 * - CRITICAL: Running out of MapIds!
 * - MINOR: Database is only saved when storing new messages. File does not update just because people read mail.
 * - INTERESTING: People who receive (but they still cannot read of course) other's mails are logged as such.
 * -- Remove or do something useful with?
 * - Postmen teleport away if out in open areas
 * -- Please vote for https://bukkit.atlassian.net/browse/BUKKIT-366 :)
 * - Postmen are spawned outside even if it's raining
 *
 * Suggested future development (not set in stone):
 * - Allow items to be attached to mails
 * - /courier list [what if someone's named "list"? :) Maybe /post should be the only send-alias
 * - Take "photos" of what the sender is looking at. (Either just that or as a "background" to a message)
 *
 */
public class Courier extends JavaPlugin {
    // these must match plugin.yml
    public static final String CMD_POSTMAN = "postman";
    public static final String CMD_COURIER = "courier";
    public static final String CMD_POST = "post";
    public static final String PM_POSTMAN = "courier.postman";
    public static final String PM_SEND = "courier.send";
    public static final String PM_LIST = "courier.list";
    public static final String PM_INFO = "courier.info";

    public static final int MAGIC_NUMBER = Integer.MAX_VALUE - 395743; // used to id our maps

    private static Vault vault = null;
    private static Economy economy = null;
    
    private final CourierEntityListener entityListener = new CourierEntityListener(this);
    private final CourierPlayerListener playerListener = new CourierPlayerListener(this);
    private final CourierServerListener serverListener = new CourierServerListener(this);
    private final CourierDeliveryListener deliveryListener = new CourierDeliveryListener(this);
    private final CourierCommands courierCommands = new CourierCommands(this);
    private final CourierDB courierdb = new CourierDB(this);
    private CourierConfig config;

    private Runnable deliveryThread;
    private int deliveryId = -1;
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
    
    void addLetter(short id, Letter l) {
        letters.put((int) id,l);
    }

    // finds the Letter associated with a specific Map
    // making this hashmap persistent might save us a lot of list-searching, then only using this
    // as fallback
    public Letter getLetter(MapView map) {
        if(map == null) { // safety first
            return null;
        }
        Letter letter = null;
        if(!letters.containsKey((int) map.getId())) {
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
            letter = letters.get((int) map.getId());
        }
        return letter;
    }
    
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Picks a spot suitably in front of the player's eyes and checks to see if there's room 
     * for a postman (Enderman) to spawn in line-of-sight
     * 
     * Currently this can fail badly not checking whether we're on the same Y ..
     *
     * Also: Should be extended to check at least a few blocks to the sides and not JUST direct line of sight
     *
     */
    @SuppressWarnings("JavaDoc")
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
                    Location tLoc = block.getLocation();
                    getCConfig().clog(Level.FINE, "findSpawnLocation got location! [" + tLoc.getBlockX() + "," + tLoc.getBlockY() + "," + tLoc.getBlockZ() + "]");

                    // make sure we spawn in the middle of the blocks, not at the corner
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
    
/*    private void stopDeliveryThread() {
        if(deliveryId != -1) {
            getServer().getScheduler().cancelTask(deliveryId);
            deliveryId = -1;
        }
    }*/

    private void deliverMail() {
        // find first online player with undelivered mail
        // spawn new thread to deliver the mail
        Player[] players = getServer().getOnlinePlayers();
        for (Player player : players) {
            // I really need to remember which players have had a postman sent out even if they
            // haven't read their mail. Time to separate delivered and read ... maybe even picked up as well?
            // currently picked up count as delivered, maybe that's what it should as well :)

            // hmm I made all these changes and nothing uses read atm. Weird.
            if (courierdb.undeliveredMail(player.getName())) {
// is this lookup slow? it saves us in the extreme case new deliveries are scheduled faster than despawns
// oh my this was crappy coding. why did I add this, esp. considering it could never have worked?
//                if (!postmen.containsValue(player)) {
                    short undeliveredMessageId = getCourierdb().undeliveredMessageId(player.getName());
                    if (undeliveredMessageId != -1) {
                        Location spawnLoc = findSpawnLocation(player);
                        if(spawnLoc != null && player.getWorld().hasStorm()) {
                            // I think I consider this to be a temporary solution to
                            // http://dev.bukkit.org/server-mods/courier/tickets/4-postmen-are-spawned-outside-even-if-its-raining/
                            // Also, do endermen get hurt by snowfall? (and damage events for endermen in rain are lacking in Bukkit, right?)
                            //
                            // hey. so rails on a block cause my findSpawnLocation to choose the block above
                            // I guess there are additional checks I should add. emptiness?
                            // todo: and glass blocks _don't_ seem to be included in "getHighest..." which I feel is wrong ("non-air")
                            config.clog(Level.FINE, "Top sky facing block at Y: " + player.getWorld().getHighestBlockYAt(spawnLoc));
                            if(player.getWorld().getHighestBlockYAt(spawnLoc) == spawnLoc.getBlockY()) {
                                spawnLoc = null;
                            }
                        }
                        if (spawnLoc != null) {
                            Postman postman = new Postman(this, player, spawnLoc, undeliveredMessageId);
                            this.addPostman(postman);
                        }
                    } else {
                        config.clog(Level.SEVERE, "undeliveredMail and undeliveredMessageId not in sync: " + undeliveredMessageId);
                    }
//                }
            }
        }
    }

    public void startDeliveries() {
        startDeliveryThread();
        config.clog(Level.FINE, "Deliveries have started");
    }
    
    public void pauseDeliveries() {
        getServer().getScheduler().cancelTasks(this);
        for (Map.Entry<UUID, Postman> uuidPostmanEntry : postmen.entrySet()) {
            Postman postman = (Postman) ((Map.Entry) uuidPostmanEntry).getValue();
            if (postman != null) {
                postman.remove();
            }
        }
        courierdb.save();
        deliveryId = -1;
        config.clog(Level.FINE, "Deliveries are now paused");
    }
    
    public void onDisable() {
        pauseDeliveries();
        config.clog(Level.INFO, this.getDescription().getName() + " is now disabled.");
    }

    public void onEnable() {
        this.loadConfig();
        courierdb.load();

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        // I register as High on some events since I know I only modify for Endermen I've spawned
        pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Priority.High, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.High, this);
        pm.registerEvent(Event.Type.ENDERMAN_PICKUP, entityListener, Priority.High, this);
        pm.registerEvent(Event.Type.ENDERMAN_PLACE, entityListener, Priority.High, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Monitor, this);
//        pm.registerEvent(Event.Type.MAP_INITIALIZE, serverListener, Priority.Normal, this);
//        pm.registerEvent(Event.Type.SERVER_COMMAND, courierCommands, Priority.Normal, this);
        pm.registerEvent(Event.Type.CUSTOM_EVENT, deliveryListener, Priority.Normal, this);

        getCommand(CMD_POSTMAN).setExecutor(courierCommands);
        getCommand(CMD_COURIER).setExecutor(courierCommands);
        getCommand(CMD_POST).setExecutor(courierCommands);

        if(getServer().getOnlinePlayers().length > 0) {
            // players already on, we've been reloaded
            startDeliveries();
        }

        // if config says we should use economy, require vault + economy support
        if(config.getUseFees()) {
            Plugin x = getServer().getPluginManager().getPlugin("Vault");
            if(x != null && x instanceof Vault) {
                vault = (Vault) x;
            }
            if(vault != null) {
                if(setupEconomy()) {
                    config.clog(Level.INFO, "Courier has linked to " + economy.getName() + " through Vault");
                } else {
                    config.clog(Level.SEVERE, "Courier could not find an Economy plugin installed!");
                    setEnabled(false); // verify if this is the right way of doing it
                }
            } else {
                config.clog(Level.SEVERE, "Courier relies on Vault for economy support and Vault isn't installed!");
                config.clog(Level.INFO, "See http://dev.bukkit.org/server-mods/vault/");
                config.clog(Level.INFO, "If you don't want economy support, set UseFees to false in Courier config.");
                setEnabled(false); // verify if this is the right way of doing it
            }
        }

        if(this.isEnabled()) {
            PluginDescriptionFile pdfFile = this.getDescription();
            config.clog(Level.INFO, pdfFile.getName() + " version v" + pdfFile.getVersion() + " is enabled!");
        }
    }

    // in preparation for plugin config dynamic reloading
    void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        config = new CourierConfig(this);
    }

    public CourierConfig getCConfig() {
        return config;
    }

    private Boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}