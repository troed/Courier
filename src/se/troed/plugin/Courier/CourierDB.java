package se.troed.plugin.Courier;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Flatfile now, database and/or map storage later
 * I'm quite sure I could get rid of messageids by using other primitives 
 * "delivered" and "read" are slightly tricky. Delivered mail sets newmail to false, even when not read!
 * (and of course delivered=false and read=true is an invalid combination should it arise)
 *
 * receiver1:
 *   newmail: true/false          <-- makes some things faster but others slow
 *   messageids: 42,73,65         <-- get/setIntegerList, although it doesn't look this pretty in the yml
 *   mapid42:
 *     sender:
 *     message:
 *     delivered:
 *     read:
 *   mapid73:
 *     sender:
 *     message:
 *     delivered:
 *     read:
 *   mapid65:
 *     sender:
 *     message:
 *     delivered:
 *     read:
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

        // since there's at least one new message, set newmail to true
        mdb.set(r + ".newmail", true);

        // update messageids
        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids == null) {
            messageids = new ArrayList<Integer>();
        }
        messageids.add((int)id); // safe cast
        mdb.set(r + ".messageids", messageids);

        mdb.set(r + "." + String.valueOf(id) + ".sender", s);
        mdb.set(r + "." + String.valueOf(id) + ".message", m);
        // new messages can't have been delivered
        mdb.set(r + "." + String.valueOf(id) + ".delivered", false);
        // new messages can't have been read
        mdb.set(r + "." + String.valueOf(id) + ".read", false);

        this.save(); // save after each sent message currently

        return true;
    }

    // figure out letter decay, re-use of mapids etc
//    public boolean removeMessage(short id, String r) {
//    }

    public boolean undeliveredMail(String r) {
        if(mdb == null || r == null) {
            return false;
        }
        return mdb.getBoolean(r + ".newmail");
    }
    
    // runs through messageids, finds a message not read and returns the corresponding id
    // returns -1 on failure
    public short unreadMessageId(String r) {
        if(mdb == null || r == null) {
            return -1;
        }

        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids != null) {
            for(Integer id: messageids) {
                boolean read = mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
                if(!read) {
                    return id.shortValue();
                }
            }
        }
        return -1;
    }

    // runs through messageids, finds a message not delivered and returns the corresponding id
    // returns -1 on failure
    public short undeliveredMessageId(String r) {
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

    public boolean getDelivered(String r, short id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        return mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
    }
    
    public boolean setDelivered(String r, short id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        mdb.set(r + "." + String.valueOf(id) + ".delivered", true);
        undeliveredMessageId(r); // DIRTY way of making sure "newmail" is cleared
        return true;
    }

    public boolean getRead(String r, short id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        return mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
    }

    public boolean setRead(String r, short id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        mdb.set(r + "." + String.valueOf(id) + ".read", true);
        return true;
    }
    // remove delivered - that means just deleting the entry and removing the messageid from the list?
    // still no list of all mapids ever used atm
}
