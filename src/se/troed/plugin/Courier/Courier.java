package se.troed.plugin.Courier;

/*
 *   Copyright (C) 2011 Troed Sangberg <courier@troed.se>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/*
 *
 * Courier - a Minecraft player to player realistic mail plugin
 *
 * Courier letter maps are uniquely defined by their x-value being 2147087904 (INT_MAX - 395743)
 * - I find it unlikely anyone will ever seriously craft a map at that location, it will have to do.
 * - Other plugin developers can use this fact to skip Courier Letters when they traverse maps
 *
 * How to deal with players who NEVER accept delivery? We'll spawn an immense number of postmen
 * and Items over time! I do not track how many times a single mail has been delivered, maybe I should?
 *
 */
public class Courier extends JavaPlugin {
    // these must match plugin.yml
    public static final String CMD_POSTMAN = "postman";
    public static final String CMD_COURIER = "courier";
    public static final String CMD_POST = "post";
    public static final String CMD_LETTER = "letter";
    public static final String PM_POSTMAN = "courier.postman";
    public static final String PM_SEND = "courier.send";
    public static final String PM_WRITE = "courier.write";
    public static final String PM_LIST = "courier.list";
    public static final String PM_INFO = "courier.info";
    public static final String PM_THEONEPERCENT = "courier.theonepercent";
    public static final String PM_PRIVACYOVERRIDE = "courier.privacyoverride";
    public static final String PM_USEITEMFRAMES = "courier.useitemframes";

    // Courier Map types internal enum
    public static final int NONE = 0;
    public static final int LETTER = 1;
    public static final int FRAMEDLETTER = 2;
    public static final int PARCHMENT = 3;

    public static final int MAGIC_NUMBER = Integer.MAX_VALUE - 395743; // used to id our map
    public static final int MAX_ID = Short.MAX_VALUE; // really, we don't do negative numbers well atm
    public static final int MIN_ID = 1; // since unenchanted items are level 0
    private static final int DBVERSION = 1; // used since 1.0.0
    private static final String RSS_URL = "http://dev.bukkit.org/server-mods/courier/files.rss";

    private static Vault vault = null;
    private static Economy economy = null;

    private final Tracker tracker = new Tracker(this); // must be done before CourierEventListener
    private boolean abort = false; // used to abort plugin load on error
    private boolean initDone = false; // failsafe for postWorldInit()

    private final CourierEventListener eventListener = new CourierEventListener(this);
    private final CourierCommands courierCommands = new CourierCommands(this);
    private final CourierDB courierdb = new CourierDB(this);
    private CourierConfig config;
    private LetterRenderer letterRenderer = null;

    private BukkitTask updateTask;
    private Runnable deliveryThread;
    private int deliveryId = -1;

    // Helper method to quickly identify a Courier Map ItemStack
    // Should be used in many more places than it is currently
    // Valid for Enchanted Courier Letters, Blank Courier Parchments && Framed Letters
    int courierMapType(ItemStack item) {
        if(item != null && item.getType() == Material.MAP) {
            MapView map = getServer().getMap(item.getDurability());
            if(map != null && map.getCenterX() == Courier.MAGIC_NUMBER) {
                if(map.getId() == getCourierdb().getCourierMapId()) {
                    if(item.containsEnchantment(Enchantment.DURABILITY)) { // && level > 0
                        return Courier.LETTER;
                    } else {
                        return Courier.PARCHMENT;
                    }
                } else {
                    return Courier.FRAMEDLETTER;
                }
            }
        }
        return Courier.NONE;
    }

    public Economy getEconomy() {
        return economy;
    }

    public Tracker getTracker() {
        return tracker;
    }

    public CourierDB getCourierdb() {
        return courierdb;
    }

    /**
     * Picks a spot suitably in front of the player's eyes and checks to see if there's room 
     * for a postman to spawn in line-of-sight
     * 
     * Currently this can fail badly not checking whether we're on the same Y ..
     *
     * Also: Should be extended to check at least a few blocks to the sides and not JUST direct line of sight
     *
     * todo: Move this method to Postman
     */
    @SuppressWarnings("JavaDoc")
    Location findSpawnLocation(Player p) {
        Location sLoc = null;

        List<Block> blocks;
        // http://dev.bukkit.org/server-mods/courier/tickets/67-task-of-courier-generated-an-exception/
        // "@param maxDistance This is the maximum distance in blocks for the trace. Setting this value above 140 may lead to problems with unloaded chunks. A value of 0 indicates no limit"
        try {
            // o,o,o,o,o,o,x
            blocks = p.getLineOfSight((Set)null, getCConfig().getSpawnDistance());
        } catch (IllegalStateException e) {
            blocks = null;
            getCConfig().clog(Level.WARNING, "caught IllegalStateException in getLineOfSight");
        }
        if(blocks != null && !blocks.isEmpty()) {
            Block block = blocks.get(blocks.size()-1); // get last block
            getCConfig().clog(Level.FINE, "findSpawnLocation got lineOfSight");
            if(!block.isEmpty() && blocks.size()>1) {
                getCConfig().clog(Level.FINE, "findSpawnLocation got non-air last block");
                block = blocks.get(blocks.size()-2); // this SHOULD be an air block, then
            }
            if(block.isEmpty()) {
                // find bottom
                // http://dev.bukkit.org/server-mods/courier/tickets/62-first-letter-sent-and-received-crash/
                getCConfig().clog(Level.FINE, "findSpawnLocation air block");
                while(block.getY() > 0 && block.getRelative(BlockFace.DOWN, 1).isEmpty()) {
                    getCConfig().clog(Level.FINE, "findSpawnLocation going down ...");
                    block = block.getRelative(BlockFace.DOWN, 1);
                }
                // verify this is something we can stand on and that we fit
                if(block.getY() > 0 && !block.getRelative(BlockFace.DOWN, 1).isLiquid()) {
                    if(Postman.getHeight(this) > 2 && (!block.getRelative(BlockFace.UP, 1).isEmpty() || !block.getRelative(BlockFace.UP, 2).isEmpty())) {
                        // Enderpostmen don't fit
                    } else if(Postman.getHeight(this) > 1 && !block.getRelative(BlockFace.UP, 1).isEmpty()) {
                        // "normal" height Creatures don't fit
                    } else {
                        Location tLoc = block.getLocation();
                        getCConfig().clog(Level.FINE, "findSpawnLocation got location! [" + tLoc.getBlockX() + "," + tLoc.getBlockY() + "," + tLoc.getBlockZ() + "]");

                        // make sure we spawn in the middle of the blocks, not at the corner
                        sLoc = new Location(tLoc.getWorld(), tLoc.getBlockX()+0.5, tLoc.getBlockY(), tLoc.getBlockZ()+0.5);
                    }
                }
            }
        }

        // make a feeble attempt at not betraying vanished players
        // the box will likely need to be a lot bigger
        // just loop through all online players instead? ~300 checks max
        // but that would mean vanished players can never receive mail
        if(sLoc != null) {
            int length = getCConfig().getVanishDistance();
            if(length > 0) {
                List<Entity> entities = p.getNearbyEntities(length, 64, length);
                for(Entity e : entities) {
                    if(e instanceof Player) {
                        Player player = (Player) e;
                        if(!player.canSee(p)) {
                            sLoc = null; // it's enough that one Player nearby isn't supposed to see us
                            getCConfig().clog(Level.FINE, "Vanished player " + p.getName() + "'s postman could be seen by " + player.getName());
                            break;
                        }
                    }
                }
            }
        }

        if(sLoc == null) {
            getCConfig().clog(Level.FINE, "Didn't find room to spawn Postman");
            // fail
        }

        return sLoc;
    }

    private void startDeliveryThread() {
        if(deliveryId >= 0) {
            config.clog(Level.WARNING, "Multiple calls to startDeliveryThread()!");
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

    private void startUpdateThread() {
        if(getCConfig().getUpdateInterval() == 0) { // == disabled
            return;
        }
        if(updateTask != null) {
            config.clog(Level.WARNING, "Multiple calls to startUpdateThread()!");
        } else {
            updateTask = new BukkitRunnable() {
                public void run() {
                    String version = config.getVersion();
                    String checkVersion = null;
                    // updateCheck returns "Courier v1.2.3" - extract "1.2.3"
                    String[] parsed = updateCheck(version).split(" v");
                    if(parsed != null && parsed.length > 0) {
                        checkVersion = parsed[1];
                    }
                    if(checkVersion == null) {
                        config.clog(Level.WARNING, "Error in Courier automatic update check. Notify plugin author.");
                    } else {
                        config.clog(Level.FINE, "version: " + version + " vs updateCheck: " + checkVersion);
                        if(config.versionCompare(version, checkVersion) < 0) {
                            config.clog(Level.WARNING, "There's a new version of Courier available: v" + checkVersion + " (you have v" + version + ")");
                            config.clog(Level.WARNING, "Please visit the Courier home: http://dev.bukkit.org/server-mods/courier/");
                        }
                    }
                }
            }.runTaskTimer(this, 400, getCConfig().getUpdateInterval() * 20);
        }
        if(updateTask == null) {
            config.clog(Level.WARNING, "UpdateCheck task scheduling failed");
        }
    }

    private void stopUpdateThread() {
        if(updateTask != null) {
            updateTask.cancel();
        }
        updateTask = null;
    }

    private void deliverMail() {
        // find first online player with undelivered mail
        // spawn new thread to deliver the mail
        Player[] playerArray = new Player[100];
        Iterator players = getServer().getOnlinePlayers().iterator();
        
        while(players.hasNext()) {
        	
        	Player player = (Player) players.next();
            if (courierdb.undeliveredMail(player.getName())) {
                // Do not deliver mail to players in Creative mode
                // http://dev.bukkit.org/server-mods/courier/tickets/49-pagination-stops-working-after-changing-slot-creative/
                if(player.getGameMode() == GameMode.CREATIVE && !config.getCreativeDelivery()) {
                    // todo: this might well turn out to be too spammy ... and the message is about "place" not "mode"
                    // Also, could warn when detecting PlayerGameModeChangeEvent
                    config.clog(Level.FINE, "Didn't deliver mail to " + player.getDisplayName() + " - player is in Creative mode");
                    display(player, getCConfig().getCannotDeliver());
                    continue;
                }
                // if already delivery out for this player do something?
                int undeliveredMessageId = getCourierdb().undeliveredMessageId(player.getName());
                config.clog(Level.FINE, "Undelivered messageid: " + undeliveredMessageId);
                if (undeliveredMessageId != -1) {
                    Location spawnLoc = findSpawnLocation(player);
                    if(spawnLoc != null && player.getWorld().hasStorm() && config.getType() == EntityType.ENDERMAN) {
                        // hey. so rails on a block cause my findSpawnLocation to choose the block above
                        // I guess there are additional checks I should add. emptiness?
                        // todo: that also means we try to spawn an enderpostman on top of rails even in rain
                        // todo: and glass blocks _don't_ seem to be included in "getHighest..." which I feel is wrong ("non-air")
                        // see https://bukkit.atlassian.net/browse/BUKKIT-445
                        // Minecraftwiki:
                        // "Rain occurs in all biomes except Tundra, Taiga, and Desert."
                        // "Snow will only fall in the Tundra and Taiga biomes"
                        // .. but on my test server there's rain in Taiga. What gives?
                        // .. and snow in ICE_PLAINS of course.
                        // .. let's go with DESERT being safe and that's it. (Endermen are hurt by snow as well)
                        // .. maybe add BEACH later?
                        Biome biome = player.getWorld().getBiome((int) spawnLoc.getX(), (int) spawnLoc.getZ());
                        config.clog(Level.FINE, "SpawnLoc is in biome: " + biome);
                        if(biome != Biome.DESERT) {
                            config.clog(Level.FINE, "Top sky facing block at Y: " + player.getWorld().getHighestBlockYAt(spawnLoc));
                            if(player.getWorld().getHighestBlockYAt(spawnLoc) == spawnLoc.getBlockY()) {
                                spawnLoc = null;
                            }
                        }
                    }
                    if (spawnLoc != null) {
//                        Postman postman = new CreaturePostman(this, player, undeliveredMessageId);
                        Postman postman = Postman.create(this, player, undeliveredMessageId);
                        // separate instantiation from spawning, save spawnLoc in instantiation
                        // and create a new method to lookup unspawned locations. Use loc matching
                        // in onCreatureSpawn as mob-denier override variable.
                        tracker.addSpawner(spawnLoc, postman);
                        postman.spawn(spawnLoc);
                        // since we COULD be wrong when using location, re-check later if it indeed
                        // was a Postman we allowed through and despawn if not? Extra credit surely.
                        // Let's see if it's ever needed first
                        tracker.addPostman(postman);
                    }
                } else {
                    config.clog(Level.SEVERE, "undeliveredMail and undeliveredMessageId not in sync: " + undeliveredMessageId);
                }
            }
        }
    }

    public void startDeliveries() {
        startDeliveryThread();
        config.clog(Level.FINE, "Deliveries have started");
    }
    
    public void pauseDeliveries() {
        if(deliveryId != -1) {
            getServer().getScheduler().cancelTask(deliveryId);
            deliveryId = -1;
        }
        tracker.clearPostmen();
        courierdb.save(null);
        config.clog(Level.FINE, "Deliveries are now paused");
    }
    
    public void onDisable() {
        pauseDeliveries();
        tracker.clearSpawners();
        stopUpdateThread();
        getServer().getScheduler().cancelTasks(this); // failsafe
        config.clog(Level.INFO, this.getDescription().getName() + " is now disabled.");
    }

    public void onEnable() {
        this.loadConfig();

        abort = false;
        initDone = false;

        try {
            this.saveResource("translations/readme.txt", true);
            this.saveResource("translations/config_french.yml", true);
            this.saveResource("translations/config_swedish.yml", true);
            this.saveResource("translations/config_dutch.yml", true);
            this.saveResource("translations/config_german.yml", true);
            this.saveResource("translations/config_portuguese.yml", true);
        } catch (Exception e) {
            config.clog(Level.WARNING, "Unable to copy translations from .jar to plugin folder");
        }

        boolean dbExist = false;
        try {
            dbExist = courierdb.load();
        } catch (Exception e) {
            config.clog(Level.SEVERE, "Fatal error when trying to read Courier database! Make a backup of messages.yml and contact plugin author.");
            abort = true;
        }

        // detect if we have a v0.9.x -> v1.0.0 upgrade needed
        if(!abort && dbExist && courierdb.getDatabaseVersion() == -1) {
            // fixes http://dev.bukkit.org/server-mods/courier/tickets/32-player-never-gets-mail-uppercase-lowercase-username/
            config.clog(Level.WARNING, "Case sensitive database found, rewriting ...");
            try {
                courierdb.keysToLower();
                courierdb.setDatabaseVersion(Courier.DBVERSION);
                config.clog(Level.WARNING, "Case sensitive database found, rewriting ... done");
            } catch (Exception e) {
                config.clog(Level.SEVERE, "Case sensitive database rewriting failed! Visit plugin support forum");
                config.clog(Level.SEVERE, "Your old pre-1.0.0 Courier database has been backed up");
                abort = true;
            }
        }

        if(!abort) {
            // Register our events
            getServer().getPluginManager().registerEvents(eventListener, this);

            // and our commands
            getCommand(CMD_POSTMAN).setExecutor(courierCommands);
            getCommand(CMD_COURIER).setExecutor(courierCommands);
            getCommand(CMD_POST).setExecutor(courierCommands);
            getCommand(CMD_LETTER).setExecutor(courierCommands);
        }

        // wait until default world has been loaded for the rest of our startup code
        if(!getServer().getWorlds().isEmpty()) {
            // We've been reloaded, continue post-World loading now
            postWorldLoad();
        } else {
            // hacky workaround since we're not getting WorldLoadEvent
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    postWorldLoad();
                }
            }, 0);
        }
    }

    // called from onWorldLoad event
    public void postWorldLoad() {
        if(initDone) {
            getCConfig().clog(Level.WARNING, "postWorldLoad() called more than once! Contact plugin developer.");
        }

        short mapId = 0;
        if(!abort) {
            // Prepare the magic Courier Map we use for all rendering
            // and more importantly, the one all ItemStacks will point to
            mapId = courierdb.getCourierMapId();
            getCConfig().clog(Level.INFO, "Rendering map as found in config: " + mapId);

            // check if the server admin has used Courier and then deleted the world
            if(mapId != -1 && getServer().getMap(mapId) == null) {
                getCConfig().clog(Level.SEVERE, "The Courier claimed map id " + mapId + " wasn't found in the world folder! Reclaiming.");
                getCConfig().clog(Level.SEVERE, "If deleting the world (or maps) wasn't intended you should look into why this happened.");
                mapId = -1;
            }
            // verify that everything is as it should
            if(mapId != -1) {
                MapView mv = getServer().getMap(mapId);
                if(mv.getCenterX() != Courier.MAGIC_NUMBER) {
                    getCConfig().clog(Level.SEVERE, "The map claimed to be the Courier Map in config isn't! Trying to fix.");
                    mapId = -1; // deal with this as if we had none allocated, see below
                }
            }
            if(mapId == -1) {
                // we don't have an allocated map stored, see if there is one we've forgotten about
                MapView mv = null;
                for(short i=0; i<Short.MAX_VALUE; i++) {
                    mv = getServer().getMap(i);
                    if(mv != null && mv.getCenterX() == Courier.MAGIC_NUMBER && mv.getCenterZ() == 0 ) {
                        // there we go, a nice Courier Letter map to render with
                        mapId = i;
                        courierdb.setCourierMapId(mapId);
                        getCConfig().clog(Level.INFO, "Found existing Courier map with id " + mv.getId());
                        break;
                    // else if getCenterX == MAGIC_NUMBER it's a legacy Letter and will be handled by PlayerListener
                    }
                }
                if(mapId == -1) {
                    // no Courier Map found and we've gone through them all
                    // (in reality this might be triggered if the admin has deleted some maps, nothing I can do)
                    // Maps are saved in the world-folders, use default world(0) trick
                    short existingMapId = courierdb.getCourierMapId();
                    if(existingMapId != -1) {
                        // if we really believe this is our map then go for it - fix it up.
                        mv = getServer().getMap(existingMapId);
                    }
                    if(existingMapId == -1 || mv == null) {
                        // allocate a new map
                        mv = getServer().createMap(getServer().getWorlds().get(0));
                    }
                    mv.setCenterX(Courier.MAGIC_NUMBER);
                    mv.setCenterZ(0); // legacy Courier Letters have a unix timestamp here instead
                    mapId = mv.getId();
                    getCConfig().clog(Level.INFO, "Rendering map claimed with the id " + mv.getId());
                    courierdb.setCourierMapId(mapId);
                }
            }
            if(mapId == -1) {
                getCConfig().clog(Level.SEVERE, "Could not allocate a Map. This is a fatal error.");
                abort = true;
            }
        }

        if(!abort) {
            MapView mv = getServer().getMap(mapId);
            if(letterRenderer == null) {
                letterRenderer = new LetterRenderer(this);
                getCConfig().clog(Level.FINE, "New LetterRenderer allocated");
            }
            letterRenderer.initialize(mv); // does this make a difference at all?
            List<MapRenderer> renderers = mv.getRenderers();
            for(MapRenderer r : renderers) { // remove existing renderers
                mv.removeRenderer(r);
            }
            mv.addRenderer(letterRenderer);
            getCConfig().clog(Level.FINE, "LetterRenderer attached to Map " + mv.getId());
        }
        
        if(!abort && !getServer().getOnlinePlayers().isEmpty()) {
            // players already on, we've been reloaded
            startDeliveries();
        }

        // if config says we should use economy, require vault + economy support
        if(!abort && config.getUseFees()) {
            Plugin x = getServer().getPluginManager().getPlugin("Vault");
            if(x != null && x instanceof Vault) {
                vault = (Vault) x;

                if(setupEconomy()) {
                    config.clog(Level.INFO, "Courier has linked to " + economy.getName() + " through Vault");
                } else {
                    config.clog(Level.SEVERE, "Vault could not find an Economy plugin installed!");
                    abort = true;
                }
            } else {
                config.clog(Level.SEVERE, "Courier relies on Vault for economy support and Vault isn't installed!");
                config.clog(Level.INFO, "See http://dev.bukkit.org/server-mods/vault/");
                config.clog(Level.INFO, "If you don't want economy support, set UseFees to false in Courier config.");
                abort = true;
            }
        }

        // Warn about "facepalm" moments:
        // http://dev.bukkit.org/server-mods/courier/tickets/81-entity-not-appearing-heads-up-comment/
        World defWorld = getServer().getWorlds().get(0);
// It seems Courier can spawn animals and monsters even though the server setting is for them to be off, except for Monsters on Peaceful
//        if(!defWorld.getAllowAnimals() || !defWorld.getAllowMonsters() || defWorld.getDifficulty() == Difficulty.PEACEFUL) {
        if(defWorld.getDifficulty() == Difficulty.PEACEFUL) {
            config.clog(Level.WARNING, "With difficulty set to Peaceful Monsters cannot spawn. Verify that the Postman type you've configured Courier to use isn't a Monster.");
        }

        // todo: configurable
        // Make burning of Letters possible
        // oops, not allowed to have Material.AIR as the result .. (NPE) .. working around this in the event listener
        // https://bukkit.atlassian.net/browse/BUKKIT-745
        if(!abort) {
            FurnaceRecipe rec = new FurnaceRecipe(new ItemStack(Material.MAP), Material.MAP);
            getServer().addRecipe(rec);
        }

        if(!abort && config.getRequiresCrafting()) {
            config.clog(Level.FINE, "Crafting recipe required");
            ItemStack item = new ItemStack(Material.MAP, 1, getCourierdb().getCourierMapId());
            ItemMeta meta = item.getItemMeta();
            if(meta != null) {
                meta.setDisplayName(config.getParchmentDisplayName());
                item.setItemMeta(meta);
            }
            ShapelessRecipe recipe = new ShapelessRecipe(item);
            for(ItemStack resource : config.getLetterResources()) {
                config.clog(Level.FINE, "Crafting recipe adding: " + resource.getAmount() + " x " + resource.getType());
                recipe.addIngredient(resource.getAmount(), resource.getType());
            }
            getServer().addRecipe(recipe);
        }

        PluginDescriptionFile pdfFile = this.getDescription();
        if(!abort) {
            config.clog(Level.INFO, pdfFile.getName() + " version v" + pdfFile.getVersion() + " is enabled!");

            // launch our background thread checking for Courier updates
            startUpdateThread();
        } else {
            config.clog(Level.INFO, pdfFile.getName() + " version v" + pdfFile.getVersion() + " has been disabled!");
            setEnabled(false);
        }

        initDone = true;
    }

    // in preparation for plugin config dynamic reloading
    void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        config = new CourierConfig(this);
    }

    // avoid empty lines being output if we silence messages in the config
    static void display(CommandSender s, String m) {
        if((m == null) || m.isEmpty()) {
            return;
        }
        s.sendMessage(m);
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

    // Thanks to Sleaker & vault for the hint and code on how to use BukkitDev RSS feed for this
    // http://dev.bukkit.org/profiles/Sleaker/
    public String updateCheck(String currentVersion) {
        try {
            URL url = new URL(RSS_URL);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openConnection().getInputStream());
            doc.getDocumentElement().normalize();
            NodeList nodes = doc.getElementsByTagName("item");
            Node firstNode = nodes.item(0);
            if (firstNode.getNodeType() == 1) {
                Element firstElement = (Element)firstNode;
                NodeList firstElementTagName = firstElement.getElementsByTagName("title");
                Element firstNameElement = (Element) firstElementTagName.item(0);
                NodeList firstNodes = firstNameElement.getChildNodes();
                return firstNodes.item(0).getNodeValue();
            }
        }
        catch (Exception e) {
            config.clog(Level.WARNING, "Caught an exception in updateCheck()");
            return currentVersion;
        }
        return currentVersion;
    }
}
