package se.troed.plugin.Courier;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Flatfile now, database and/or map storage later
 * I'm quite sure I could get rid of messageids by using other primitives 
 * "delivered" and "read" are slightly tricky. Delivered mail sets newmail to false, even when not read.
 * (and of course delivered=false and read=true is an invalid combination should it arise)
 *
 * couriermap: mapid
 * receiver1:
 *   newmail: true/false          <-- makes some things faster but others slow
 *   messageids: 42,73,65         <-- get/setIntegerList, although it currently doesn't look this pretty in the yml
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
 *
 * New version:
 *
 * receiver1:
 *   newmail: true/false
 *   letteritemids:
 *     - 3234894uuid // non-mc uuid
 *       - fh484hf   // is this even possible
 *       - fij43f4
 *       - fjkjf8t
 *     - 8773498uuid
 *     - 2373843uuid
 *   3234894uuid:
 *     sender:
 *     message:
 *     delivered:
 *     read:
 *   ...
 */
public class CourierDB {
    private static final String FILENAME = "messages.yml";
    private final Courier plugin;
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

    // retrieves what we think is our specially allocated Map
    public short getCourierMapId() {
        if(mdb == null) {
            return -1;
        }
        return (short)mdb.getInt("couriermap", -1);
    }
    
    public void setCourierMapId(short mapId) {
        if(mdb == null) {
            return;
        }
        mdb.set("couriermap", (int)mapId);
    }

    public boolean storeMessage(int id, String r, String s, String m, int d) {
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
        messageids.add(id);
        mdb.set(r + ".messageids", messageids);

        mdb.set(r + "." + String.valueOf(id) + ".sender", s);
        mdb.set(r + "." + String.valueOf(id) + ".message", m);
        mdb.set(r + "." + String.valueOf(id) + ".date", d);
        // new messages can't have been delivered
        mdb.set(r + "." + String.valueOf(id) + ".delivered", false);
        // new messages can't have been read
        mdb.set(r + "." + String.valueOf(id) + ".read", false);

        this.save(); // save after each sent message currently

        return true;
    }
    
    // used for legacy Letter conversion only
    public boolean storeDate(int id, int d) {
        if(mdb == null) {
            return false;
        }
        
        String player = getPlayer(id);
        if(player == null) {
            return false; // this would be bad
        }
        mdb.set(player + "." + String.valueOf(id) + ".date", d);

        return true;
    }
    
    // currently used for legacy Letter conversion only, but it is generalized
    public void changeId(int oldid, int newid) {
        if(mdb == null) {
            return;
        }
        
        String r = getPlayer(oldid);
        String s = getSender(r, oldid);
        String m = getMessage(r, oldid);
        int date = getDate(r, oldid);
        boolean delivered = getDelivered(r, oldid);
        boolean read = getRead(r, oldid);
        
        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids == null) { // safety, should not happen in this case
            messageids = new ArrayList<Integer>();
        }
        messageids.add(newid);
        // "atomic" add
        mdb.set(r + ".messageids", messageids);
        mdb.set(r + "." + String.valueOf(newid) + ".sender", s);
        mdb.set(r + "." + String.valueOf(newid) + ".message", m);
        mdb.set(r + "." + String.valueOf(newid) + ".date", date);
        mdb.set(r + "." + String.valueOf(newid) + ".delivered", delivered);
        mdb.set(r + "." + String.valueOf(newid) + ".read", read);

        // "atomic" remove
        messageids.remove(Integer.valueOf(oldid)); // caught out by ArrayList.remove(Object o) vs remove(int i) ...
        mdb.set(r + ".messageids", messageids);
        mdb.set(r + "." + String.valueOf(oldid), null);
    }

// figure out letter decay, re-use of ids etc
//    public boolean removeMessage(short id, String r) {
//    }

    public boolean undeliveredMail(String r) {
        //noinspection SimplifiableIfStatement
        if(mdb == null || r == null) {
            return false;
        }
        return mdb.getBoolean(r + ".newmail");
    }
    
    // runs through messageids, finds a message not read and returns the corresponding id
    // returns -1 on failure
    public int unreadMessageId(String r) {
        if(mdb == null || r == null) {
            return -1;
        }

        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids != null) {
            for(Integer id : messageids) {
                boolean read = mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
                if(!read) {
                    return id;
                }
            }
        }
        return -1;
    }

    // runs through messageids, finds a message not delivered and returns the corresponding id
    // returns -1 on failure
    public int undeliveredMessageId(String r) {
        if(mdb == null || r == null) {
            return -1;
        }

        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids != null) {
            for(Integer id : messageids) {
                boolean delivered = mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
                if(!delivered) {
                    return id;
                }
            }
        }

        // if we end up here, for any reason, it means there are no undelivered messages
        mdb.set(r + ".newmail", false);
        return -1;
    }

    // finds a specific messageid and returns associated player
    // needed since the server drops our Letter associations with mapId on restart
    public String getPlayer(int id) {
        if(id == -1 || mdb == null) {
            return null;
        }
        
        Set<String> strings = mdb.getKeys(false);
        for (String key : strings) {
            List<Integer> messageids = mdb.getIntegerList(key + ".messageids");
            if (messageids != null && messageids.contains(id)) {
                return key;
            }
        }
        return null;
    }
    
    public String getSender(String r, int id) {
        if(mdb == null || r == null) {
            return null;
        }

        return mdb.getString(r + "." + String.valueOf(id) + ".sender");
    }
    
    public String getMessage(String r, int id) {
        if(mdb == null || r == null) {
            return null;
        }

        return mdb.getString(r + "." + String.valueOf(id) + ".message");
    }

    public boolean getDelivered(String r, int id) {
        //noinspection SimplifiableIfStatement
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        return mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
    }

    // unexpected side effect, we end up here if player1 takes a message intended for player2
    // exploit or remove logging of it?
    public boolean setDelivered(String r, int id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        mdb.set(r + "." + String.valueOf(id) + ".delivered", true);
        undeliveredMessageId(r); // DIRTY way of making sure "newmail" is cleared
        return true;
    }

    public int getDate(String r, int id) {
        if(mdb == null || r == null) {
            return -1;
        }
        return mdb.getInt(r + "." + String.valueOf(id) + ".date");
    }

    public boolean getRead(String r, int id) {
        //noinspection SimplifiableIfStatement
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        return mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
    }

    public boolean setRead(String r, int id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }
        mdb.set(r + "." + String.valueOf(id) + ".read", true);
        return true;
    }

    // returns the first available id
    // expected to be called seldom (at letter creation) and is allowed to be slow
    // obvious caching/persisting of TreeSet possible
    public int generateUID() {
        if(mdb == null) {
            return -1;
        }
        TreeSet<Integer> sortedSet = new TreeSet<Integer>();
        Set<String> players = mdb.getKeys(false);
        for (String player : players) {
            List<Integer> messageids = mdb.getIntegerList(player + ".messageids");
            if (messageids != null) {
                // add all messageids found for this player to our ordered set
                sortedSet.addAll(messageids);
            }
        }
        // make sure we don't enter negative number territory
        for(int i=Courier.MIN_ID; i<Courier.MAX_ID; i++) {
            if(sortedSet.add(i)) {
                // i wasn't in the set
                return i;
            }
        }
        // todo: no more unique ids available - force admin to cleanup I guess
        return -1;
    }
}
