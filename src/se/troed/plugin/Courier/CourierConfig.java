package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapPalette;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginDescriptionFile;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CourierConfig {
    private static final boolean debug = true;

    private final Logger log;
    private final Configuration config;

    private static final String LOGPREFIX = "[Courier] ";
    private static final String USEFEES = "Courier.UseFees";
    // any config file _older_ than this is invalid - compatibility break
    private static final String VERSIONBREAK = "0.9.0";
    private static final String UPDATEINTERVAL = "Courier.UpdateInterval";
    private static final String FEE_SEND = "Courier.Fee.Send";
    private static final String FEE_INFOFEE = "Courier.Fee.InfoFee";
    private static final String FEE_INFONOFEE = "Courier.Fee.InfoNoFee";
    private static final String FEE_BANKACCOUNT = "Courier.Fee.BankAccount";
    private static final String PRIVACY_SEALED = "Courier.Privacy.SealedEnvelope";
    private static final String PRIVACY_LOCKED = "Courier.Privacy.Locked";
    private static final String POSTMAN_QUICK_DESPAWN = "Courier.Postman.QuickDespawn";
    private static final String POSTMAN_DESPAWN = "Courier.Postman.Despawn";
    private static final String ROUTE_INITIALWAIT = "Courier.Route.InitialWait";
    private static final String ROUTE_NEXTROUTE = "Courier.Route.NextRoute";
    private static final String ROUTE_WALKTOPLAYER = "Courier.Route.WalkToPlayer";
    private static final String POSTMAN_TYPE = "Courier.Postman.Type";
    private static final String POSTMAN_SPAWNDISTANCE = "Courier.Postman.SpawnDistance";
    private static final String POSTMAN_BREAKSPAWNPROTECTION = "Courier.Postman.BreakSpawnProtection";
    private static final String POSTMAN_CREATIVEDELIVERY = "Courier.Postman.CreativeDelivery";
    private static final String POSTMAN_GREETING = "Courier.Postman.Greeting";
    private static final String POSTMAN_MAILDROP = "Courier.Postman.MailDrop";
    private static final String POSTMAN_INVENTORY = "Courier.Postman.Inventory";
    private static final String POSTMAN_CANNOTDELIVER = "Courier.Postman.CannotDeliver";
    private static final String POSTMAN_EXTRADELIVERIES = "Courier.Postman.ExtraDeliveries";
    private static final String POSTMAN_NOUNREADMAIL = "Courier.Postman.NoUnreadMail";
    private static final String LETTER_FREE = "Courier.Letter.FreeLetter";
    private static final String LETTER_RESOURCES = "Courier.Letter.Resources";
    private static final String LETTER_REQUIRESCRAFTING = "Courier.Letter.RequiresCrafting";
    private static final String LETTER_DROP = "Courier.Letter.Drop";
    private static final String LETTER_INVENTORY = "Courier.Letter.Inventory";
    private static final String LETTER_SHOWDATE = "Courier.Letter.ShowDate";
    private static final String LETTER_FRAMEABLE = "Courier.Letter.Frameable";
    private static final String LETTER_NOTEXT = "Courier.Letter.NoText";
    private static final String LETTER_SKIPPEDTEXT = "Courier.Letter.SkippedText";
    private static final String LETTER_CREATEFAILED = "Courier.Letter.CreateFailed";
    private static final String LETTER_NOMOREUIDS = "Courier.Letter.NoMoreUIDs";
    private static final String LETTER_INFOCOST = "Courier.Letter.InfoCost";
    private static final String LETTER_INFOFREE = "Courier.Letter.InfoFree";
    private static final String LETTER_LACKINGRESOURCES = "Courier.Letter.LackingResources";
    private static final String LETTER_NOCRAFTEDFOUND = "Courier.Letter.NoCraftedFound";
    private static final String LETTER_DISPLAYNAME = "Courier.Letter.DisplayName";
    private static final String LETTER_FROM = "Courier.Letter.From";
    private static final String LETTER_TO = "Courier.Letter.To";
    private static final String PARCHMENT_DISPLAYNAME = "Courier.Parchment.DisplayName";
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
    private final int updateInterval;
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
    private final boolean freeLetter;
    private final List<ItemStack> letterStacks = new ArrayList<ItemStack>();
    private EntityType type = null;

    private final String version;
    
    public CourierConfig(Courier plug) {

        log = plug.getServer().getLogger();

        config = plug.getConfig();
        PluginDescriptionFile pdfFile = plug.getDescription();
        this.version = pdfFile.getVersion(); // actual plugin version
        
        // verify config compatibility with config file version (could be different from plugin)
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
        updateInterval = config.getInt(UPDATEINTERVAL, 18000); // added in v1.1.0
        clog(Level.FINE, UPDATEINTERVAL + ": " + updateInterval);
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
        type = EntityType.fromName(stype);
        if(type == null) {
            type = EntityType.ENDERMAN;
            clog(Level.WARNING, "Postman.Type: " + stype + " is not a valid Creature, using default.");
        }
        clog(Level.FINE, POSTMAN_TYPE + ": " + type.getName());

        spawnDistance = config.getInt(POSTMAN_SPAWNDISTANCE);
        clog(Level.FINE, POSTMAN_SPAWNDISTANCE + ": " + spawnDistance);
        breakSpawnProtection = config.getBoolean(POSTMAN_BREAKSPAWNPROTECTION, true); // added in 0.9.6
        clog(Level.FINE, POSTMAN_BREAKSPAWNPROTECTION + ": " + breakSpawnProtection);
        showDate = config.getBoolean(LETTER_SHOWDATE, true); // added in 1.1.0
        clog(Level.FINE, LETTER_SHOWDATE + ": " + showDate);

        freeLetter = config.getBoolean(LETTER_FREE, true); // added in 1.1.5
        clog(Level.FINE, LETTER_FREE + ": " + freeLetter);

        List<String> letterResources = config.getStringList(LETTER_RESOURCES); // added in 1.1.5
        if(letterResources != null) {
            for(String resource : letterResources) {
                Material material = Material.matchMaterial(resource);
                if(material != null) {
                    boolean added = false;
                    for(ItemStack is : letterStacks) {
                        if(is.getType() == material) {
                            // add to this existing stack
                            is.setAmount(is.getAmount() + 1);
                            added = true;
                            break;
                        }
                    }
                    if(!added) {
                        // create new stack
                        letterStacks.add(new MaterialData(material).toItemStack(1));
                    }
                } else {
                    clog(Level.WARNING, "Cannot parse \'" + resource + "\' into a valid Minecraft resource! Skipped.");
                }
            }
            clog(Level.FINE, LETTER_RESOURCES + ": " + letterStacks.toString());
        }

        sealedEnvelope = config.getBoolean(PRIVACY_SEALED, true); // added in 0.9.11
        clog(Level.FINE, PRIVACY_SEALED + ": " + sealedEnvelope);
    }
    
    public String getVersion() {
        return version;
    }
    
    public int getUpdateInterval() {
        return updateInterval;
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

    public EntityType getType() {
        return type;
    }

    public int getSpawnDistance() {
        return spawnDistance;
    }

    public boolean getShowDate() {
        return showDate;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getFreeLetter() {
        return freeLetter;
    }

    public List<ItemStack> getLetterResources() {
        return letterStacks;
    }

    public boolean getRequiresCrafting() {
        return config.getBoolean(LETTER_REQUIRESCRAFTING, false);
    }

    public boolean getBreakSpawnProtection() {
        return breakSpawnProtection;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getSealedEnvelope() {
        return sealedEnvelope;
    }

    public boolean getLetterFrameable() {
        return config.getBoolean(LETTER_FRAMEABLE, true);
    }

    public boolean getCreativeDelivery() {
        return config.getBoolean(POSTMAN_CREATIVEDELIVERY, false);
    }

    public String getBankAccount() {
        return config.getString(FEE_BANKACCOUNT, "");
    }

    public Double getFeeSend() {
        return feeSend;
    }

    // translatable strings

    public String getGreeting() {
        return colorize(config.getString(POSTMAN_GREETING, "")); // added in 0.9.1
    }

    public String getMailDrop() {
        return colorize(config.getString(POSTMAN_MAILDROP, "")); // added in 0.9.1
    }

    public String getInventory() {
        return colorize(config.getString(POSTMAN_INVENTORY, "")); // added in 0.9.5
    }

    public String getCannotDeliver() {
        return colorize(config.getString(POSTMAN_CANNOTDELIVER, "")); // added in 0.9.6
    }

    public String getLetterDrop() {
        return colorize(config.getString(LETTER_DROP, "")); // added in 0.9.10
    }

    public String getLetterInventory() {
        return colorize(config.getString(LETTER_INVENTORY, "")); // added in 0.9.10
    }

    public String getInfoFee(String fee) {
        return String.format(colorize(config.getString(FEE_INFOFEE, "")), fee); // 1.1.0
    }

    public String getInfoNoFee() {
        return colorize(config.getString(FEE_INFONOFEE, "")); // 1.1.0
    }

    // LetterRender colorization
    public String getPrivacyLocked(String player) {
        return String.format(colorize2(config.getString(PRIVACY_LOCKED)), player); // 1.1.8
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

    public String getLetterInfoCost(String resources) {
        return String.format(colorize(config.getString(LETTER_INFOCOST, "")), resources); // 1.1.5
    }

    public String getLetterInfoFree() {
        return colorize(config.getString(LETTER_INFOFREE, "")); // 1.1.5
    }

    public String getLetterLackingResources() {
        return colorize(config.getString(LETTER_LACKINGRESOURCES, "")); // 1.1.5
    }

    public String getLetterNoCraftedFound() {
        return colorize(config.getString(LETTER_NOCRAFTEDFOUND, "")); // 1.1.8
    }

    // Lore should not be colorized
    public String getLetterDisplayName() {
        return strip(config.getString(LETTER_DISPLAYNAME, "")); // 1.1.8
    }

    // Lore should not be colorized
    public String getLetterFrom(String player) {
        return String.format(strip(config.getString(LETTER_FROM)), player); // 1.1.8
    }

    // LetterRenderer version
    public String getLetterFrom2(String player) {
        return String.format(colorize2(config.getString(LETTER_FROM)), player); // 1.1.8
    }

    // Lore should not be colorized
    public String getLetterTo(String player) {
        return String.format(strip(config.getString(LETTER_TO)), player); // 1.1.8
    }

    // Lore should not be colorized
    public String getParchmentDisplayName() {
        return strip(config.getString(PARCHMENT_DISPLAYNAME, "")); // 1.1.8
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

    // Converts from Chat to Map color space
    String colorize2(String s) {
        String result = s.replace("\\n","\n");
        Pattern p = Pattern.compile("&(\\p{XDigit})");
        Matcher m = p.matcher(s);
        while (m.find()) {
            result = result.replaceAll(m.group(), "§" + convertToMapColor(Byte.parseByte(m.group().substring(1), 16)) + ";");
        }
        return result;
    }

    // credits: theguynextdoor - http://forums.bukkit.org/threads/adding-color-support.52980/#post-890244
    // see ChatColor.java for value validity
    String colorize(String s) {
        return s.replaceAll("(&(\\p{XDigit}))", "\u00A7$2");
    }

    String strip(String s) {
//        return s.replaceAll("(§(\\p{Digit}+);)", "");
        return s.replaceAll("(&(\\p{XDigit}))", "");
    }

    // code lifted from ScrollingMenuSign by desht (CC-BY-NC)
    // https://github.com/desht/ScrollingMenuSign/
    private byte[] colorCache = new byte[16];
    private boolean[] colorInited = new boolean[16];

    byte convertToMapColor(byte color) {
        if(color < 0 || color > 15) {
            clog(Level.FINE, "Out of bounds color value passed to convertToMapColor");
            return MapPalette.DARK_BROWN;
        }

        if (colorInited[color]) {
            return colorCache[color];
        }

        byte result = MapPalette.DARK_GRAY;

        switch (color) {
            case 0:
                result = MapPalette.matchColor(0, 0, 0); break;
            case 1:
                result = MapPalette.matchColor(0, 0, 128); break;
            case 2:
                result = MapPalette.matchColor(0, 128, 0); break;
            case 3:
                result = MapPalette.matchColor(0, 128, 128); break;
            case 4:
                result = MapPalette.matchColor(128, 0, 0); break;
            case 5:
                result = MapPalette.matchColor(128, 0, 128); break;
            case 6:
                result = MapPalette.matchColor(128, 128, 0); break;
            case 7:
                result = MapPalette.matchColor(128, 128, 128); break;
            case 8:
                result = MapPalette.matchColor(64, 64, 64); break;
            case 9:
                result = MapPalette.matchColor(0, 0, 255); break;
            case 10:
                result = MapPalette.matchColor(0, 255, 0); break;
            case 11:
                result = MapPalette.matchColor(0, 255, 255); break;
            case 12:
                result = MapPalette.matchColor(255, 0, 0); break;
            case 13:
                result = MapPalette.matchColor(255, 0, 255); break;
            case 14:
                result = MapPalette.matchColor(255, 255, 0); break;
            case 15:
                result = MapPalette.matchColor(255, 255, 255); break;
        }

        colorInited[color] = true;
        colorCache[color] = result;

        return result;
    }
}
