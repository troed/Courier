package se.troed.plugin.Courier;

import com.google.common.io.Files;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.persistence.PersistenceException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;

/**
 * messages.yml
 *
 * courierclaimedmap: mapid
 * courierdatabaseversion: version
 */
public class CourierDB {
    private static final String FILENAME = "messages.yml";
    private final Courier plugin;
    private YamlConfiguration mdb;
    
    public CourierDB(Courier p) {
        plugin = p;    
    }

    // returns true if there already was a db
    public boolean load() throws IOException {
        File db = new File(plugin.getDataFolder(), FILENAME);
        mdb = new YamlConfiguration();
        if(db.exists()) {
            try {
                mdb.load(db);
            } catch (InvalidConfigurationException e) {
                // this might be a MacRoman (or other) encoded file and we're running under a UTF-8 default JVM
                mdb = loadNonUTFConfig(db);
                if(mdb == null) {
                    throw new IOException("Could not read Courier database!");
                }
            } catch (Exception e) {
                mdb = null;
//                e.printStackTrace();
                throw new IOException("Could not read Courier database!");
            }
            return true;
        }
        return false;
    }

    // see http://forums.bukkit.org/threads/friends-dont-let-friends-use-yamlconfiguration-loadconfiguration.57693/
    // manually load as MacRoman if possible, we'll force saving in UTF-8 later
    // Testing shows Apple Java 6 with default-encoding utf8 finds a "MacRoman" charset
    // OpenJDK7 on Mac finds a "x-MacRoman" charset.
    //
    // "Every implementation of the Java platform is required to support the following standard charsets. Consult the release documentation for your implementation to see if any other charsets are supported. The behavior of such optional charsets may differ between implementations.
    // US-ASCII, ISO-8859-1, UTF-8 [...]"
    //
    // http://www.alanwood.net/demos/charsetdiffs.html - compares ansi, iso and macroman
    // NOTE: This method isn't pretty and should - really - be recoded.
    YamlConfiguration loadNonUTFConfig(File db) {
        InputStreamReader reader; 
        try {
            Charset cs;
            try {
                // This issue SHOULD be most common on Mac, I think, assume MacRoman default
                cs = Charset.forName("MacRoman");
            } catch (Exception e) {
                cs = null;
            }
            if(cs == null) {
                try {
                    // if no MacRoman can be found in the JVM, assume ISO-8859-1 is the closest match
                    cs = Charset.forName("ISO-8859-1");
                } catch (Exception e) {
                    return null;
                }
            }
            plugin.getCConfig().clog(Level.WARNING, "Trying to convert message database from " + cs.displayName() + " to UTF-8");
            reader = new InputStreamReader(new FileInputStream(db), cs);
        } catch (Exception e) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        BufferedReader input = new BufferedReader(reader);

        try {
            String line;

            while ((line = input.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        } catch (Exception e) {
            return null;
        } finally {
            try {
                input.close();
            } catch (Exception e) {
                // return null;
            }
        }
        mdb = new YamlConfiguration();
        try {
            mdb.loadFromString(builder.toString());
        } catch (Exception e) {
            mdb = null;
            e.printStackTrace();
        }
        return mdb;
    }

    // if filename == null, uses default
    // (this makes making backups really easy)
    public boolean save(String filename) {
        boolean ret = false;
        if(mdb != null) {
            File db = new File(plugin.getDataFolder(), filename != null ? filename : FILENAME);
            try {
//                saveUTFConfig(db, mdb);
                mdb.save(db);
                ret = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    // even if we're run under a JVM with non-utf8 default encoding, force it
    // at least that was the idea, but on Mac it's still read back using MacRoman. No automatic switching to UTF-8
    void saveUTFConfig(File file, YamlConfiguration yaml) throws IOException {
        if(yaml != null) {
            Charset cs;
            try {
                cs = Charset.forName("UTF-8");
            } catch (Exception e) {
                throw new IOException("UTF-8 not a supported charset");
            }

            Files.createParentDirs(file);
            String data = yaml.saveToString();
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), cs);

            try {
                writer.write(data);
            } finally {
                writer.close();
            }
        }
    }

    // retrieves the version of our database format, -1 if it doesn't exist
    public int getDatabaseVersion() {
        if(mdb == null) {
            return -1;
        }
        return mdb.getInt("courierdatabaseversion", -1);
    }

    public void setDatabaseVersion(int v) {
        if(mdb == null) {
            return;
        }
        mdb.set("courierdatabaseversion", v);
        this.save(null);
    }

    // retrieves what we think is our specially allocated Map
    public short getCourierMapId() {
        if(mdb == null) {
            return -1;
        }
        return (short)mdb.getInt("courierclaimedmap", -1);
    }
    
    public void setCourierMapId(short mapId) {
        if(mdb == null) {
            return;
        }
        mdb.set("courierclaimedmap", (int)mapId);
        this.save(null);
    }

    // this method is called when we detect a database version using Yaml
    // it makes a backup, then copies all message content to sqlite and removes from Yaml
    public void yamlToSql(CourierDatabase db) {
        if(mdb == null || db == null) {
            throw new IllegalArgumentException("mdb or db was null");
        }

        // just for safety, back up db first, and don't allow the backup to be overwritten if it exists
        // (if this method throws exceptions most admins will just likely try a few times .. )
        String backup = FILENAME + ".120.backup";
        File fdb = new File(plugin.getDataFolder(), backup);
        if(!fdb.exists()) {
            this.save(backup);
        }

        Set<String> players = mdb.getKeys(false);
        for (String r : players) {
            // skip our two protected words, and I've seen "null" (empty yaml blocks) appear as well
            if(r.equalsIgnoreCase("courierclaimedmap") || r.equalsIgnoreCase("courierdatabaseversion") || r.equalsIgnoreCase("null")) {
                continue;
            }
            Receiver receiver = new Receiver();
            receiver.setName(r.toLowerCase());
            receiver.setNewmail(mdb.getBoolean(r + ".newmail"));

            List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
            if(messageids != null) { // safety, should not happen in this case
                for(Integer id : messageids) {
                    Message message = new Message();
                    
                    // fetch yaml message
                    String s = mdb.getString(r + "." + String.valueOf(id) + ".sender");
                    String m = mdb.getString(r + "." + String.valueOf(id) + ".message");
                    int date = mdb.getInt(r + "." + String.valueOf(id) + ".date");
                    boolean delivered = mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
                    boolean read = mdb.getBoolean(r + "." + String.valueOf(id) + ".read");

                    // sanity check, saw this in my own chaotic test environment but it shouldn't exist in the wild
                    if(s != null && !s.isEmpty()) {
                        message.setId(id);
                        message.setSender(s);
                        message.setMessage(m);
                        message.setMdate(date);
                        message.setDelivered(delivered);
                        message.setRead(read);

                        message.setReceiver(receiver);
                        receiver.getMessages().add(message);
                    }
                }
            }

            try {
                db.getDatabase().save(receiver);
                mdb.set(r, null); // delete the old entry
            } catch (PersistenceException e) {
                plugin.getCConfig().clog(Level.SEVERE, "Unable to persist data for player: " + r);
            }
        }
        this.save(null);
    }
}
