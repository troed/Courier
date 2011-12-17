package se.troed.plugin.Courier;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Flatfile now, database and/or map storage later
 *
 * receiver1:
 *   newmail: true/false          <-- makes some things faster but others slow
 *   messageids: 42,73,65         <-- get/setIntegerList, although it doesn't look this pretty in the yml
 *   mapid42:
 *     sender:
 *     message:
 *     delivered:
 *   mapid73:
 *     sender:
 *     message:
 *     delivered:
 *   mapid65:
 *     sender:
 *     message:
 *     delivered:
 * receiver2:
 *   ...
 */
public class CourierDB {
    private static final String FILENAME = "messages.yml";
    private Courier plugin;
    private YamlConfiguration mdb;
    
    public CourierDB(Courier p) {
        plugin = p;    
    }

    // reading the whole message db into memory, is that a real problem?
    public boolean load() {
        File db = new File(plugin.getDataFolder(), FILENAME);
        mdb = YamlConfiguration.loadConfiguration(db);
        return true;
    }

    // only saving when plugin quits might lose a lot of messages
    public boolean save() {
        boolean ret = false;
        if(mdb != null) {
            File db = new File(plugin.getDataFolder(), FILENAME);
            try {
                mdb.save(db);
                ret = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public boolean storeMessage(short id, String r, String s, String m) {
        if(mdb == null || r == null || s == null || m == null) {
            return false;
        }

        mdb.set(r + "." + String.valueOf(id) + ".sender", s);
        mdb.set(r + "." + String.valueOf(id) + ".message", m);
        // new messages can't have been delivered
        mdb.set(r + "." + String.valueOf(id) + ".delivered", false);
        // since there's at least one new message, set newmail to true
        mdb.set(r + ".newmail", true);
        
        // update messageids
        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids == null) {
            System.out.println("Courier: messageids was null");
            messageids = new ArrayList<Integer>();
        }
        messageids.add((int)id); // safe cast
        mdb.set(r + ".messageids", messageids);
        
        this.save(); // save after each sent message currently

        return true;
    }
    
    public boolean gotMessage(String r) {
        if(mdb == null || r == null) {
            return false;
        }
        if(mdb.contains(r + ".newmail") == false) {
            return false;
        }
        
        return mdb.getBoolean(r + ".newmail");
    }
    
    // runs through messageids, finds a message not delivered and returns the corresponding id
    // returns -1 on failure
    public short unreadMessageId(String r) {
        if(mdb == null || r == null) {
            return -1;
        }

        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids != null) {
            for(Integer id: messageids) {
                boolean delivered = mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
                if(!delivered) {
                    return id.shortValue();
                }
            }
        }

        // if we end up here, for any reason, it means there are no undelivered messages
        mdb.set(r + ".newmail", false);
        return -1;
    }

    // finds a specific messageid and returns associated player
    // needed since the server drops our Letter associations with mapId on restart
    // why don't I just give up and persist such a List? :)
    public String getPlayer(short id) {
        if(id == -1 || mdb == null) {
            return null;
        }
        
        Set<String> strings = mdb.getKeys(false);
        Iterator iter = strings.iterator();
        while(iter.hasNext()) {
            String key = (String)iter.next(); // slightly dangerous cast?
            System.out.println("Courier getPlayer, key: " + key);

            Integer temp = new Integer(id);
            List<Integer> messageids = mdb.getIntegerList(key + ".messageids");
            if(messageids != null && messageids.contains(temp)) {
                return key;
            }
        }
        return null;
    }
    
    public String getSender(String r, short id) {
        if(mdb == null || r == null) {
            return null;
        }

        return mdb.getString(r + "." + String.valueOf(id) + ".sender");
    }
    
    public String getMessage(String r, short id) {
        if(mdb == null || r == null) {
            return null;
        }

        return mdb.getString(r + "." + String.valueOf(id) + ".message");
    }

    public boolean delivered(String r, short id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        mdb.set(r + "." + String.valueOf(id) + ".delivered", true);
        unreadMessageId(r); // DIRTY way of making sure "newmail" is cleared
        return true;
    }

    // remove delivered - that means just deleting the entry and removing the messageid from the list?
    // still no list of all mapids ever used atm
}
