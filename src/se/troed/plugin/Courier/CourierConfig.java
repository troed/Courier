package se.troed.plugin.Courier;

import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;


public class CourierConfig {
    private static final boolean debug = true;

    private final Logger log;
    
    private static final String LOGPREFIX = "[Courier] ";
    private static final String USEFEES = "Courier.UseFees";
    // any config file _older_ than this is invalid - compatibility break
    private static final String VERSIONBREAK = "0.9.0";
    private static final String FEE_SEND = "Courier.Fee.Send";
    private static final String POSTMAN_QUICK_DESPAWN = "Courier.Postman.QuickDespawn";
    private static final String POSTMAN_DESPAWN = "Courier.Postman.Despawn";
    private static final String ROUTE_INITIALWAIT = "Courier.Route.InitialWait";
    private static final String ROUTE_NEXTROUTE = "Courier.Route.NextRoute";
    private static final String POSTMAN_SPAWNDISTANCE = "Courier.Postman.SpawnDistance";
    private static final String POSTMAN_BREAKSPAWNPROTECTION = "Courier.Postman.BreakSpawnProtection";
    private static final String POSTMAN_GREETING = "Courier.Postman.Greeting";
    private static final String POSTMAN_MAILDROP = "Courier.Postman.MailDrop";
    private static final String POSTMAN_INVENTORY = "Courier.Postman.Inventory";
    private static final String POSTMAN_CANNOTDELIVER = "Courier.Postman.CannotDeliver";
    private static final String LETTER_DROP = "Courier.Letter.Drop";
    private static final String LETTER_INVENTORY = "Courier.Letter.Inventory";
    
    private final boolean useFees;
    private final double feeSend;
    private final int quickDespawnTime;
    private final int despawnTime;
    private final int initialWait;
    private final int nextRoute;
    private final int spawnDistance;
    private final boolean breakSpawnProtection;
    private String greeting = null;
    private String maildrop = null;
    private String inventory = null;
    private String cannotDeliver = null;
    private String letterDrop = null;
    private String letterInventory = null;
    
    public CourierConfig(Courier plug) {

        log = plug.getServer().getLogger();

        FileConfiguration config = plug.getConfig();
        PluginDescriptionFile pdfFile = plug.getDescription();

        // verify config compatibility
        String version = config.getString(pdfFile.getName() + ".Version");
        if(version!=null) {
            int major = 0;
            int minor = 0;
            int revision = 0;

            String[] parts = version.split("\\.");
            if(parts.length > 0 && parts[0] != null) {
                major = Integer.decode(parts[0]);
            }
            if(parts.length > 1 && parts[1] != null) {
                minor = Integer.decode(parts[1]);
            }
            if(parts.length > 2 && parts[2] != null) {
                revision = Integer.decode(parts[2]);
            }
            clog(Level.FINE, "Config version: Major: " + major + " Minor: " + minor + " Revision: " + revision);

            int existingVersion = major*1000000+minor*1000+revision;

            parts = VERSIONBREAK.split("\\.");
            if(parts.length > 0 && parts[0] != null) {
                major = Integer.decode(parts[0]);
            }
            if(parts.length > 1 && parts[1] != null) {
                minor = Integer.decode(parts[1]);
            }
            if(parts.length > 2 && parts[2] != null) {
                revision = Integer.decode(parts[2]);
            }
            clog(Level.FINE, "Comp break: Major: " + major + " Minor: " + minor + " Revision: " + revision);

            int breakVersion = major*1000000+minor*1000+revision;

            if(existingVersion < breakVersion) {
                // config file not valid - abort plugin load
                clog(Level.SEVERE, "Config file version too old - unexpected behaviour might occur!");
            }
        }
        
        useFees = config.getBoolean(USEFEES, false); // added in 0.9.5
        clog(Level.FINE, USEFEES + ": " + useFees);
        feeSend = config.getDouble(FEE_SEND, 0); // added in 0.9.5
        clog(Level.FINE, FEE_SEND + ": " + feeSend);
        quickDespawnTime = config.getInt(POSTMAN_QUICK_DESPAWN);
        clog(Level.FINE, POSTMAN_QUICK_DESPAWN + ": " + quickDespawnTime);
        despawnTime = config.getInt(POSTMAN_DESPAWN);
        clog(Level.FINE, POSTMAN_DESPAWN + ": " + despawnTime);
        initialWait = config.getInt(ROUTE_INITIALWAIT);
        clog(Level.FINE, ROUTE_INITIALWAIT + ": " + initialWait);
        nextRoute = config.getInt(ROUTE_NEXTROUTE);
        clog(Level.FINE, ROUTE_NEXTROUTE + ": " + nextRoute);
        spawnDistance = config.getInt(POSTMAN_SPAWNDISTANCE);
        clog(Level.FINE, POSTMAN_SPAWNDISTANCE + ": " + spawnDistance);
        breakSpawnProtection = config.getBoolean(POSTMAN_BREAKSPAWNPROTECTION, true); // added in 0.9.6
        clog(Level.FINE, POSTMAN_BREAKSPAWNPROTECTION + ": " + breakSpawnProtection);
        greeting = colorize(config.getString(POSTMAN_GREETING, "")); // added in 0.9.1
        clog(Level.FINE, POSTMAN_GREETING + ": " + greeting);
        maildrop = colorize(config.getString(POSTMAN_MAILDROP, "")); // added in 0.9.1
        clog(Level.FINE, POSTMAN_MAILDROP + ": " + maildrop);
        inventory = colorize(config.getString(POSTMAN_INVENTORY, "")); // added in 0.9.5
        clog(Level.FINE, POSTMAN_INVENTORY + ": " + inventory);
        cannotDeliver = colorize(config.getString(POSTMAN_CANNOTDELIVER, "")); // added in 0.9.6
        clog(Level.FINE, POSTMAN_CANNOTDELIVER + ": " + cannotDeliver);
        letterDrop = colorize(config.getString(LETTER_DROP, "")); // added in 0.9.10
        clog(Level.FINE, LETTER_DROP + ": " + letterDrop);
        letterInventory = colorize(config.getString(LETTER_INVENTORY, "")); // added in 0.9.10
        clog(Level.FINE, LETTER_INVENTORY + ": " + letterInventory);
    }

    public boolean getUseFees() {
        return useFees;
    }

    public int getQuickDespawnTime() {
        return quickDespawnTime;
    }

    public int getDespawnTime() {
        return despawnTime;
    }

    public int getInitialWait() {
        return initialWait;
    }

    public int getNextRoute() {
        return nextRoute;
    }

    public int getSpawnDistance() {
        return spawnDistance;
    }
    
    public boolean getBreakSpawnProtection() {
        return breakSpawnProtection;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getMailDrop() {
        return maildrop;
    }

    public String getInventory() {
        return inventory;
    }
    
    public String getCannotDeliver() {
        return cannotDeliver;
    }

    public Double getFeeSend() {
        return feeSend;
    }

    public String getLetterDrop() {
        return letterDrop;
    }
    
    public String getLetterInventory() {
        return letterInventory;
    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    void clog(Level level, String message) {
        if(!debug && (level != Level.SEVERE && level != Level.WARNING && level != Level.INFO)) {
            return;
        }
        // Bukkit doesn't log CONFIG, FINE etc at least with the defaults
        if(level == Level.FINE) {
            level = Level.INFO;
        }
        log.log(level, LOGPREFIX + message);
    }

    // credits: theguynextdoor - http://forums.bukkit.org/threads/adding-color-support.52980/#post-890244
    // see ChatColor.java for value validity
    String colorize(String s) {
        return s.replaceAll("(&(\\p{XDigit}))", "\u00A7$2");
    }
}
