package se.troed.plugin.Courier;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Flatfile now, database later
 * I'm quite sure I could get rid of messageids by using other primitives 
 * "delivered" and "read" are slightly tricky. Delivered mail sets newmail to false, even when not read.
 * (and of course delivered=false and read=true is an invalid combination should it arise)
 *
 * courierclaimedmap: mapid       # 17 chars = cannot clash with any playername
 * receiver1:
 *   newmail: true/false          <-- makes some things faster but others slow
 *   messageids: 42,73,65         <-- get/setIntegerList, although it currently doesn't look this pretty in the yml
 *   mapid42:
 *     sender:
 *     message:
 *     date:
 *     delivered:
 *     read:
 *   mapid73:
 *     sender:
 *     message:
 *     date:
 *     delivered:
 *     read:
 *   mapid65:
 *     sender:
 *     message:
 *     date:
 *     delivered:
 *     read:
 * receiver2:
 *   ...
 *
 */
public class CourierDB {
    private static final String FILENAME = "messages.yml";
    private final Courier plugin;
    private YamlConfiguration mdb;
    
    public CourierDB(Courier p) {
        plugin = p;    
    }

    // reading the whole message db into memory, is that a real problem?
    // returns true if there already was a db
    public boolean load() throws IOException {
        File db = new File(plugin.getDataFolder(), FILENAME);
        mdb = new YamlConfiguration();
        if(db.exists()) {
            try {
                mdb.load(db);
            } catch (Exception e) {
                mdb = null;
                e.printStackTrace();
                throw new IOException("Could not read Courier database!");
            }
            return true;
        }
        return false;
    }

    // only saving when plugin quits might lose a lot of messages
    // if filename == null, uses default
    public boolean save(String filename) {
        boolean ret = false;
        if(mdb != null) {
            File db = new File(plugin.getDataFolder(), filename != null ? filename : FILENAME);
            try {
                mdb.save(db);
                ret = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    // retrieves the version of our database format, -1 if it doesn't exist
    // 1 = v1.0.0
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
    }

    public boolean sendMessage(int id, String r, String s) {
        boolean ret = false;
        if(mdb == null || r == null || s == null) {
            return false;
        }

        r = r.toLowerCase();
        
        // nothing to say the player who wants to send a picked up Letter is the one with it in her storage
        // but if player2 steals a letter written by player1 and immediately sends to player3, player1
        // should not be listed as sender. See outcommented getSender() below

        String origin = getPlayer(id);

        if(origin != null) {
            // alright, sign over to a specific receiver
//            String s = getSender(origin, id);
            String m = getMessage(origin, id);
            int date = getDate(origin, id);

            List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
            if(messageids == null) {
                messageids = new ArrayList<Integer>();
            }
            if(!messageids.contains(id)) { // I should move to a non-duplicate storage type .. 
                messageids.add(id);
            }
            mdb.set(r + ".messageids", messageids);
            mdb.set(r + "." + String.valueOf(id) + ".sender", s);
            mdb.set(r + "." + String.valueOf(id) + ".message", m);
            mdb.set(r + "." + String.valueOf(id) + ".date", date);
            // new messages can't have been delivered
            mdb.set(r + "." + String.valueOf(id) + ".delivered", false);
            // new messages can't have been read
            mdb.set(r + "." + String.valueOf(id) + ".read", false);

            // since there's at least one new message, set newmail to true
            mdb.set(r + ".newmail", true);

            // if we send to ourselves, don't delete what we just added
            if(!r.equalsIgnoreCase(origin)) {
                // "atomic" remove
                messageids = mdb.getIntegerList(origin + ".messageids");
                if(messageids != null) { // safety check
                    messageids.remove(Integer.valueOf(id));
                }
                mdb.set(origin + ".messageids", messageids);
                mdb.set(origin + "." + String.valueOf(id), null);
            }

            this.save(null);
            ret = true;
        }
        return ret;    
    }

    public boolean storeMessage(int id, String s, String m, int d) {
        if(mdb == null || s == null || m == null) {
            return false;
        }

        String skey = s.toLowerCase();
        String origin = getPlayer(id);

        // update messageids
        List<Integer> messageids = mdb.getIntegerList(skey + ".messageids");
        if(messageids == null) {
            messageids = new ArrayList<Integer>();
        }
        if(!messageids.contains(id)) { // I should move to a non-duplicate storage type .. 
            messageids.add(id);
        }
        mdb.set(skey + ".messageids", messageids);

        mdb.set(skey + "." + String.valueOf(id) + ".sender", s);
        mdb.set(skey + "." + String.valueOf(id) + ".message", m);
        mdb.set(skey + "." + String.valueOf(id) + ".date", d);
        mdb.set(skey + "." + String.valueOf(id) + ".delivered", true);
        mdb.set(skey + "." + String.valueOf(id) + ".read", true);
        // we do not change .newmail when storing in our own storage, of course

        if(origin != null && !s.equalsIgnoreCase(origin)) {
            // the current writer of this letter was not the same as the last, make sure it's moved
            messageids = mdb.getIntegerList(origin + ".messageids");
            if(messageids != null) { // safety check
                messageids.remove(Integer.valueOf(id));
            }
            mdb.set(origin + ".messageids", messageids);
            mdb.set(origin + "." + String.valueOf(id), null);            
        }
        
        this.save(null); // save after each stored message currently

        return true;
    }

    // this method is called when we detect a database version with case sensitive keys
    // it simply lowercases all Player name keys
    public void keysToLower() {
        if(mdb == null) {
            return;
        }

        // just for safety, back up db first, and don't allow the backup to be overwritten if it exists
        // (if this method throws exceptions most admins will just likely try a few times .. )
        String backup = FILENAME + ".100.backup";
        File db = new File(plugin.getDataFolder(), backup);
        if(!db.exists()) {
            this.save(backup);
        }
        
        Set<String> players = mdb.getKeys(false);
        for (String r : players) {
            String rlower = r.toLowerCase();

            if(!r.equals(rlower)) {
                // this receiver needs full rewriting
                boolean newmail = mdb.getBoolean(r + ".newmail");
                List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
                if(messageids == null) { // safety, should not happen in this case
                    messageids = new ArrayList<Integer>();
                }
                List<Integer> newMessageids = mdb.getIntegerList(rlower + ".messageids");
                if(newMessageids == null) { // most likely, but who knows?
                    newMessageids = new ArrayList<Integer>();
                }
                for(Integer id : messageids) {
                    // fetch a message
                    String s = mdb.getString(r + "." + String.valueOf(id) + ".sender");
                    String m = mdb.getString(r + "." + String.valueOf(id) + ".message");
                    int date = mdb.getInt(r + "." + String.valueOf(id) + ".date");
                    boolean delivered = mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
                    boolean read = mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
                    
                    mdb.set(rlower + "." + String.valueOf(id) + ".sender", s);
                    mdb.set(rlower + "." + String.valueOf(id) + ".message", m);
                    mdb.set(rlower + "." + String.valueOf(id) + ".date", date);
                    mdb.set(rlower + "." + String.valueOf(id) + ".delivered", delivered);
                    mdb.set(rlower + "." + String.valueOf(id) + ".read", read);

                    newMessageids.add(id);

                    mdb.set(r + "." + String.valueOf(id), null); // delete old message
                }
                mdb.set(rlower + ".messageids", newMessageids);
                mdb.set(rlower + ".newmail", newmail);

                mdb.set(r, null); // delete the old entry
            }
        }
        this.save(null);
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
        
        r = r.toLowerCase();

        return mdb.getBoolean(r + ".newmail");
    }

    // runs through messageids, sets all unread messages to undelivered
    // returns false when there are no unread messages
    public boolean deliverUnreadMessages(String r) {
        if(mdb == null || r == null) {
            return false;
        }

        r = r.toLowerCase();

        boolean newmail = false;
        List<Integer> messageids = mdb.getIntegerList(r + ".messageids");
        if(messageids != null) {
            for(Integer id : messageids) {
                boolean read = mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
                if(!read) {
                    mdb.set(r + "." + String.valueOf(id) + ".delivered", false);
                    newmail = true;
                }
            }
        }
        if(newmail) {
            mdb.set(r + ".newmail", newmail);
        }
        return newmail;
    }

    // runs through messageids, finds a message not read and returns the corresponding id
    // returns -1 on failure
    public int unreadMessageId(String r) {
        if(mdb == null || r == null) {
            return -1;
        }

        r = r.toLowerCase();

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

        r = r.toLowerCase();

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

        r = r.toLowerCase();

        return mdb.getString(r + "." + String.valueOf(id) + ".sender");
    }
    
    public String getMessage(String r, int id) {
        if(mdb == null || r == null) {
            return null;
        }

        r = r.toLowerCase();

        return mdb.getString(r + "." + String.valueOf(id) + ".message");
    }

    public boolean getDelivered(String r, int id) {
        //noinspection SimplifiableIfStatement
        if(mdb == null || r == null || id==-1) {
            return false;
        }

        r = r.toLowerCase();

        return mdb.getBoolean(r + "." + String.valueOf(id) + ".delivered");
    }

    // unexpected side effect, we end up here if player1 takes a message intended for player2
    // exploit or remove logging of it?
    public boolean setDelivered(String r, int id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }

        r = r.toLowerCase();

        mdb.set(r + "." + String.valueOf(id) + ".delivered", true);
        undeliveredMessageId(r); // DIRTY way of making sure "newmail" is cleared
        return true;
    }

    public int getDate(String r, int id) {
        if(mdb == null || r == null) {
            return -1;
        }

        r = r.toLowerCase();

        return mdb.getInt(r + "." + String.valueOf(id) + ".date");
    }

    public boolean getRead(String r, int id) {
        //noinspection SimplifiableIfStatement
        if(mdb == null || r == null || id==-1) {
            return false;
        }

        r = r.toLowerCase();

        return mdb.getBoolean(r + "." + String.valueOf(id) + ".read");
    }

    public boolean setRead(String r, int id) {
        if(mdb == null || r == null || id==-1) {
            return false;
        }

        r = r.toLowerCase();

        mdb.set(r + "." + String.valueOf(id) + ".read", true);
        return true;
    }

    // returns the first available id, or -1 when we're fatally out of them (or db error .. hmm)
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
        return -1;
    }
}
