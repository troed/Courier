package se.troed.plugin.Courier;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.CreatureType;
import org.bukkit.plugin.PluginDescriptionFile;


import java.util.logging.Level;
import java.util.logging.Logger;


public class CourierConfig {
    private static final boolean debug = true;

    private final Logger log;
    private final Configuration config;

    private static final String LOGPREFIX = "[Courier] ";
    private static final String USEFEES = "Courier.UseFees";
    // any config file _older_ than this is invalid - compatibility break
    private static final String VERSIONBREAK = "0.9.0";
    private static final String FEE_SEND = "Courier.Fee.Send";
    private static final String FEE_INFOFEE = "Courier.Fee.InfoFee";
    private static final String FEE_INFONOFEE = "Courier.Fee.InfoNoFee";
    private static final String POSTMAN_QUICK_DESPAWN = "Courier.Postman.QuickDespawn";
    private static final String POSTMAN_DESPAWN = "Courier.Postman.Despawn";
    private static final String ROUTE_INITIALWAIT = "Courier.Route.InitialWait";
    private static final String ROUTE_NEXTROUTE = "Courier.Route.NextRoute";
    private static final String ROUTE_WALKTOPLAYER = "Courier.Route.WalkToPlayer";
    private static final String POSTMAN_TYPE = "Courier.Postman.Type";
    private static final String POSTMAN_SPAWNDISTANCE = "Courier.Postman.SpawnDistance";
    private static final String POSTMAN_BREAKSPAWNPROTECTION = "Courier.Postman.BreakSpawnProtection";
    private static final String POSTMAN_GREETING = "Courier.Postman.Greeting";
    private static final String POSTMAN_MAILDROP = "Courier.Postman.MailDrop";
    private static final String POSTMAN_INVENTORY = "Courier.Postman.Inventory";
    private static final String POSTMAN_CANNOTDELIVER = "Courier.Postman.CannotDeliver";
    private static final String POSTMAN_EXTRADELIVERIES = "Courier.Postman.ExtraDeliveries";
    private static final String POSTMAN_NOUNREADMAIL = "Courier.Postman.NoUnreadMail";
    private static final String LETTER_DROP = "Courier.Letter.Drop";
    private static final String LETTER_INVENTORY = "Courier.Letter.Inventory";
    private static final String LETTER_SHOWDATE = "Courier.Letter.ShowDate";
    private static final String LETTER_NOTEXT = "Courier.Letter.NoText";
    private static final String LETTER_SKIPPEDTEXT = "Courier.Letter.SkippedText";
    private static final String LETTER_CREATEFAILED = "Courier.Letter.CreateFailed";
    private static final String LETTER_NOMOREUIDS = "Courier.Letter.NoMoreUIDs";
    private static final String PRIVACY_SEALED = "Courier.Privacy.SealedEnvelope";
    private static final String POST_NOCREDIT = "Courier.Post.NoCredit";
    private static final String POST_NORECIPIENT = "Courier.Post.NoRecipient";
    private static final String POST_DIDYOUMEAN = "Courier.Post.DidYouMean";
    private static final String POST_DIDYOUMEANLIST = "Courier.Post.DidYouMeanList";
    private static final String POST_DIDYOUMEANLIST2 = "Courier.Post.DidYouMeanList2";
    private static final String POST_NOSUCHPLAYER = "Courier.Post.NoSuchPlayer";
    private static final String POST_LETTERSENT = "Courier.Post.LetterSent";
    private static final String POST_LETTERSENTFEE = "Courier.Post.LetterSentFee";
    private static final String POST_FUNDPROBLEM = "Courier.Post.FundProblem";
    private static final String POST_NOLETTER = "Courier.Post.NoLetter";
    private static final String INFO_LINE1 = "Courier.Info.Line1";
    private static final String INFO_LINE2 = "Courier.Info.Line2";
    private static final String INFO_LINE3 = "Courier.Info.Line3";
    private static final String INFO_LINE4 = "Courier.Info.Line4";

    private final boolean useFees;
    private final double feeSend;
    private final int quickDespawnTime;
    private final int despawnTime;
    private final int initialWait;
    private final int nextRoute;
    private final int spawnDistance;
    private final boolean breakSpawnProtection;
    private final boolean sealedEnvelope;
    private final boolean showDate;
    private final boolean walkToPlayer;
    private CreatureType type = null;
    private String greeting = null;
    private String maildrop = null;
    private String inventory = null;
    private String cannotDeliver = null;
    private String letterDrop = null;
    private String letterInventory = null;
    
    public CourierConfig(Courier plug) {

        log = plug.getServer().getLogger();

        config = plug.getConfig();
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
        walkToPlayer = config.getBoolean(ROUTE_WALKTOPLAYER, true); // added in 1.1.0
        clog(Level.FINE, ROUTE_WALKTOPLAYER + ": " + walkToPlayer);

        String stype = config.getString(POSTMAN_TYPE, "Enderman"); // added in 1.1.0
        type = CreatureType.fromName(stype);
        if(type == null) {
            type = CreatureType.ENDERMAN;
            clog(Level.WARNING, "Postman.Type: " + stype + " is not a valid Creature, using default.");
        }
        clog(Level.FINE, POSTMAN_TYPE + ": " + type.getName());

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
        showDate = config.getBoolean(LETTER_SHOWDATE, true); // added in 1.1.0
        clog(Level.FINE, LETTER_SHOWDATE + ": " + showDate);
        letterDrop = colorize(config.getString(LETTER_DROP, "")); // added in 0.9.10
        clog(Level.FINE, LETTER_DROP + ": " + letterDrop);
        letterInventory = colorize(config.getString(LETTER_INVENTORY, "")); // added in 0.9.10
        clog(Level.FINE, LETTER_INVENTORY + ": " + letterInventory);
        sealedEnvelope = config.getBoolean(PRIVACY_SEALED, true); // added in 0.9.11
        clog(Level.FINE, PRIVACY_SEALED + ": " + sealedEnvelope);
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

    public boolean getWalkToPlayer() {
        return walkToPlayer;
    }

    public CreatureType getType() {
        return type;
    }

    public int getSpawnDistance() {
        return spawnDistance;
    }

    public boolean getShowDate() {
        return showDate;
    }

    public boolean getBreakSpawnProtection() {
        return breakSpawnProtection;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getSealedEnvelope() {
        return sealedEnvelope;
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

    // translatable strings
    public String getInfoFee(String fee) {
        return String.format(colorize(config.getString(FEE_INFOFEE, "")), fee); // 1.1.0
    }

    public String getInfoNoFee() {
        return colorize(config.getString(FEE_INFONOFEE, "")); // 1.1.0
    }

    public String getPostmanExtraDeliveries() {
        return colorize(config.getString(POSTMAN_EXTRADELIVERIES, "")); // 1.1.0
    }

    public String getPostmanNoUnreadMail() {
        return colorize(config.getString(POSTMAN_NOUNREADMAIL, "")); // 1.1.0
    }

    public String getPostNoCredit(String fee) {
        return String.format(colorize(config.getString(POST_NOCREDIT, "")), fee); // 1.1.0
    }

    public String getPostNoRecipient() {
        return colorize(config.getString(POST_NORECIPIENT, "")); // 1.1.0
    }

    public String getPostDidYouMean(String input, String match) {
        return String.format(colorize(config.getString(POST_DIDYOUMEAN, "")), input, match); // 1.1.0
    }

    public String getPostDidYouMeanList(String input) {
        return String.format(colorize(config.getString(POST_DIDYOUMEANLIST, "")), input); // 1.1.0
    }

    public String getPostDidYouMeanList2(String list) {
        return String.format(colorize(config.getString(POST_DIDYOUMEANLIST2, "")), list); // 1.1.0
    }

    public String getPostNoSuchPlayer(String input) {
        return String.format(colorize(config.getString(POST_NOSUCHPLAYER, "")), input); // 1.1.0
    }

    public String getPostLetterSent(String recipient) {
        return String.format(colorize(config.getString(POST_LETTERSENT, "")), recipient); // 1.1.0
    }

    public String getPostLetterSentFee(String recipient, String fee) {
        return String.format(colorize(config.getString(POST_LETTERSENTFEE, "")), recipient, fee); // 1.1.0
    }

    public String getPostFundProblem() {
        return colorize(config.getString(POST_FUNDPROBLEM, "")); // 1.1.0
    }

    public String getPostNoLetter() {
        return colorize(config.getString(POST_NOLETTER, "")); // 1.1.0
    }

    public String getLetterNoText() {
        return colorize(config.getString(LETTER_NOTEXT, "")); // 1.1.0
    }

    public String getLetterSkippedText() {
        return colorize(config.getString(LETTER_SKIPPEDTEXT, "")); // 1.1.0
    }

    public String getLetterCreateFailed() {
        return colorize(config.getString(LETTER_CREATEFAILED, "")); // 1.1.0
    }

    public String getLetterNoMoreUIDs() {
        return colorize(config.getString(LETTER_NOMOREUIDS, "")); // 1.1.0
    }

    public String getInfoLine1() {
        return colorize(config.getString(INFO_LINE1, "")); // 1.1.0
    }

    public String getInfoLine2() {
        return colorize(config.getString(INFO_LINE2, "")); // 1.1.0
    }

    public String getInfoLine3() {
        return colorize(config.getString(INFO_LINE3, "")); // 1.1.0
    }

    public String getInfoLine4() {
        return colorize(config.getString(INFO_LINE4, "")); // 1.1.0
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
