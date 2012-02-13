package se.troed.plugin.Courier;

import javax.persistence.*;
import java.util.*;
import java.util.logging.Level;

/**
 * courierclaimedmap and courierdatabaseversion are kept in messages.yml
 *
 * player table
 *  name pk
 *  newmail
 *  messages list
 *
 * message table
 *  id pk
 *  receiver fk
 *  (parcel)
 *  sender
 *  message
 *  date
 *  delivered
 *  read
 *  (protected) - only admins can delete, not part of age cleanup?
 *
 *
 * or maybe parcels can just point out messages, no point in reciprocality?
 * (
 * parcel table
 *   id pk
 *   message - onetoone mapping with message table
 * )
 */

/**
 * I still see no solution to the very slow finding of unique IDs. Also, for privacy purposes I shouldn't
 * reuse those that were _just_ deleted.
 *
 * Various doc:
 * http://www.avaje.org/doc/ebean-userguide.pdf
 * http://en.wikibooks.org/wiki/Java_Persistence/Ebean/Example_Model/Order
 * http://en.wikibooks.org/wiki/Java_Persistence/Ebean/Example_Model/Order_Detail
 * http://en.wikibooks.org/wiki/Java_Persistence/Ebean/Quick_Start_-_examples
 * http://www.jarvana.com/jarvana/view/org/avaje/ebean/2.7.4/ebean-2.7.4-javadoc.jar!/overview-summary.html
 * http://docs.redhat.com/docs/en-US/JBoss_Enterprise_Web_Platform/5/html/Hibernate_Annotations_Reference_Guide/entity-mapping-association.html
 * http://docs.redhat.com/docs/en-US/JBoss_Enterprise_Web_Platform/5/html/Hibernate_Annotations_Reference_Guide/entity-mapping-association-collection-onetomany.html
 * https://github.com/axefan/DatabaseErrorDemo/blob/master/src/us/axefan/demo/DatabaseErrorDemo.java
 * http://forums.bukkit.org/threads/trying-to-understand-persistence.12235/
 * http://docs.jboss.org/hibernate/annotations/3.5/reference/en/html/entity.html
 *
 * //@Table(name="o_receiver", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
 */
public class CourierDatabase extends MyDatabase {
    private final Courier plugin;

    public CourierDatabase(Courier p) {
        super(p);
        plugin = p;    
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Receiver.class);
        list.add(Message.class);
        return list;
    }

    public boolean sendMessage(int id, String r, String s) {
        if(r == null || s == null) {
            return false;
        }

        // nothing to say the player who wants to send a picked up Letter is the one with it in her storage
        // but if player2 steals a letter written by player1 and immediately sends to player3, player1
        // should not be listed as sender. 

        Message message = getDatabase().find(Message.class, id);
        if(message == null) {
            plugin.getCConfig().clog(Level.FINE, "Courier: Message " + id + " not found in database");
            return false;
        }
        message.setSender(s);
        message.setDelivered(false);
        message.setRead(false);

//        Receiver receiver = getDatabase().find(Receiver.class).where().ieq("name", r).findUnique();
        Receiver receiver = getDatabase().find(Receiver.class, r.toLowerCase());
        if(receiver == null) {
            plugin.getCConfig().clog(Level.FINE, "Courier: " + r + " not found in database, creating new column");
            receiver = new Receiver();
            receiver.setName(r.toLowerCase());
        }
        receiver.getMessages().add(message); // really?
        receiver.setNewmail(true);
        message.setReceiver(receiver);

        try {
            getDatabase().save(receiver); // will cascade
//          getDatabase().save(message);
        } catch (PersistenceException e) {
            if(CourierConfig.debug) {
                e.printStackTrace();
            } else {
                plugin.getCConfig().clog(Level.SEVERE, e.toString());
            }
            return false;
        }

        return true;
    }

    // storeMessage saves an unposted Letter, which requires receiver and sender to be one and the same
    public boolean storeMessage(int id, String s, String m, int d) {
        if(s == null || m == null) {
            return false;
        }

        Message message = getDatabase().find(Message.class, id);
        if(message == null) {
            plugin.getCConfig().clog(Level.FINE, "Courier: Message " + id + " not found in database, creating new column");
            message = new Message();
            message.setId(id);
        }

//        Receiver receiver = getDatabase().find(Receiver.class).where().ieq("name", s).findUnique();
        Receiver receiver = getDatabase().find(Receiver.class, s.toLowerCase());
        if(receiver == null) {
            plugin.getCConfig().clog(Level.FINE, "Courier: " + s + " not found in database, creating new column");
            receiver = new Receiver();
            receiver.setName(s.toLowerCase());
            receiver.setNewmail(false);
        }
        receiver.getMessages().add(message); // really?
        message.setReceiver(receiver);
        message.setSender(s);
        message.setMessage(m);
        message.setMdate(d);
        message.setDelivered(true);
        message.setRead(true);

        try {
// If I do save(message) then save(receiver) will fail with primary key not null since it's getting persisted twice
//            getDatabase().save(message);
            getDatabase().save(receiver);
        } catch (PersistenceException e) {
            if(CourierConfig.debug) {
                e.printStackTrace();
            } else {
                plugin.getCConfig().clog(Level.SEVERE, e.toString());
            }
            return false;
        }
        return true;
    }
    
    // used for legacy Letter conversion only
    public boolean storeDate(int id, int date) {
        Message message = getDatabase().find(Message.class, id);
        if(message != null) {
            message.setMdate(date);
            try {
                getDatabase().save(message);
            } catch (PersistenceException e) {
                if(CourierConfig.debug) {
                    e.printStackTrace();
                } else {
                    plugin.getCConfig().clog(Level.SEVERE, e.toString());
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    // currently used for legacy Letter conversion only
    public void changeId(int oldid, int newid) {
        Message message = getDatabase().find(Message.class, oldid);
        if(message == null) {
            return;
        }
        message.setId(newid);
        try {
            getDatabase().save(message);
        } catch (PersistenceException e) {
            if(CourierConfig.debug) {
                e.printStackTrace();
            } else {
                plugin.getCConfig().clog(Level.SEVERE, e.toString());
            }
        }
    }

    public boolean undeliveredMail(String r) {
        if(r == null) {
            return false;
        }

//        Receiver rec = getDatabase().find(Receiver.class).where().ieq("name", r).findUnique();
        Receiver rec = getDatabase().find(Receiver.class, r.toLowerCase());
        if(rec != null) {
            return rec.isNewmail();
        } 
        
        return false;
    }

    // runs through messageids, sets all unread messages to undelivered
    // returns false when there are no unread messages
    public boolean deliverUnreadMessages(String r) {
        if(r == null) {
            return false;
        }

        List<Message> messages = getDatabase().find(Message.class)
                .where().ieq("receiver.name", r)
                .where().eq("read", false)
                .findList();

        if(messages != null && !messages.isEmpty()) {
            Receiver rec = messages.get(0).getReceiver();
            for(Message message : messages) {
                message.setDelivered(false);
                try {
                    getDatabase().save(message);
                } catch (PersistenceException e) {
                    if(CourierConfig.debug) {
                        e.printStackTrace();
                    } else {
                        plugin.getCConfig().clog(Level.SEVERE, e.toString());
                    }
                }
            }
            rec.setNewmail(true);
            try {
                getDatabase().save(rec); // does not cascade to the messages
            } catch (PersistenceException e) {
                if(CourierConfig.debug) {
                    e.printStackTrace();
                } else {
                    plugin.getCConfig().clog(Level.SEVERE, e.toString());
                    return false;
                }
            }
        } else {
            return false;        
        }
       
        return true;
    }

    // runs through messageids, finds a message not read and returns the corresponding id
    // returns -1 on failure
    public int unreadMessageId(String r) {
        if(r == null) {
            return -1;
        }

        List<Message> messages = getDatabase().find(Message.class)
                .where().ieq("receiver.name", r)
                .where().eq("read", false)
                .findList();

        if(messages != null && !messages.isEmpty()) {
            return messages.get(0).getId();
        }

        return -1;
    }

    // runs through messageids, finds a message not delivered and returns the corresponding id
    // returns -1 if there were none, or failure
    public int undeliveredMessageId(String r) {
        if(r == null) {
            return -1;
        }

        List<Message> messages = getDatabase().find(Message.class)
                .where().ieq("receiver.name", r)
                .where().eq("delivered", false)
                .findList();

        if(messages != null && !messages.isEmpty()) {
            return messages.get(0).getId();
        }

        // if we end up here, for any reason, it means there are no undelivered messages
//        Receiver rec = getDatabase().find(Receiver.class).where().ieq("name", r).findUnique();
        Receiver rec = getDatabase().find(Receiver.class, r.toLowerCase());
        if(rec != null) {
            rec.setNewmail(false);
            try {
                getDatabase().save(rec);
            } catch (PersistenceException e) {
                if(CourierConfig.debug) {
                    e.printStackTrace();
                } else {
                    plugin.getCConfig().clog(Level.SEVERE, e.toString());
                    return -1;
                }
            }
        }
        return -1;
    }

    // removes a single Letter from the database
    public boolean deleteMessage(short id) {
        Message m = getDatabase().find(Message.class, id);

        if(m == null) {
            return false;
        }

        try {
            getDatabase().delete(m);
        } catch (PersistenceException e) {
            if(CourierConfig.debug) {
                e.printStackTrace();
            } else {
                plugin.getCConfig().clog(Level.SEVERE, e.toString());
                return false;
            }
        }

        return true;
    }
    
    // removes all Letters to a player from the database
    public boolean deleteMessages(String r) {
        if(r == null) {
            return false;
        }
        Receiver rec = getDatabase().find(Receiver.class, r.toLowerCase());
//        List<Message> messages = rec.getMessages();
//        getDatabase().delete(messages.iterator());
        try {
            getDatabase().delete(rec);
        } catch (PersistenceException e) {
            if(CourierConfig.debug) {
                e.printStackTrace();
            } else {
                plugin.getCConfig().clog(Level.SEVERE, e.toString());
                return false;
            }
        }
        return true;
    }

    // todo: not deleting unread/undelivered? implement "protected" messages?
    public int recycleMessages(int utime) {
        int count = 0;
        List<Message> messages = getDatabase().find(Message.class)
                .where().lt(("mdate"), utime)
                .findList();
        count = messages.size();
        plugin.getCConfig().clog(Level.FINE, "Number of Letters for recycling: " + count);
        return count;
    }
    
    // does this id exist in the database
    public boolean isValid(int id) {
        Message m = getDatabase().find(Message.class, id);

        if(m != null) {
            return m.getId() == id;
        }

        return false;
    }

    // finds a specific messageid and returns associated player
    public String getPlayer(int id) {
        Message m = getDatabase().find(Message.class, id);
        
        if(m != null) {
            Receiver r = m.getReceiver();
            if(r != null) {
                return r.getName();
            }
        }

        return null;
    }
    
    public String getSender(int id) {
        Message m = getDatabase().find(Message.class, id);
        
        if(m != null) {
            return m.getSender();
        }

        return null;
    }
    
    public String getMessage(int id) {
        Message m = getDatabase().find(Message.class, id);

        if(m != null) {
            return m.getMessage();
        }
        
        return null;
    }

/*    public boolean getDelivered(int id) {
        Message m = getDatabase().find(Message.class, id);

        if(m != null) {
            return m.isDelivered();
        }

        return false;
    }*/

    // unexpected side effect, we end up here if player1 takes a message intended for player2
    // could possibly be used for something
    public boolean setDelivered(String r, int id) {
        if(r == null || id==-1) {
            return false;
        }

        Message m = getDatabase().find(Message.class, id);
        if(m != null) {
            Receiver rec = m.getReceiver();
            if(rec != null && r.equalsIgnoreCase(rec.getName())) {
                m.setDelivered(true);
                try {
                    getDatabase().save(m);
                } catch (PersistenceException e) {
                    if(CourierConfig.debug) {
                        e.printStackTrace();
                    } else {
                        plugin.getCConfig().clog(Level.SEVERE, e.toString());
                        return false;
                    }
                }
            } else {
                // picked up someone elses mail
            }
        }
        undeliveredMessageId(r); // DIRTY way of making sure "newmail" is cleared
        return true;
    }

/*    public int getDate(int id) {
        Message m = getDatabase().find(Message.class, id);
        if(m != null) {
            return m.getMdate();
        }
        return -1;
    }

    public boolean getRead(int id) {
        Message m = getDatabase().find(Message.class, id);
        if(m != null) {
            return m.isRead();
        }
        return false;
    }*/

    public boolean setRead(int id) {
        Message m = getDatabase().find(Message.class, id);
        if(m != null) {
            m.setRead(true);
            try {
                getDatabase().save(m);
            } catch (PersistenceException e) {
                if(CourierConfig.debug) {
                    e.printStackTrace();
                } else {
                    plugin.getCConfig().clog(Level.SEVERE, e.toString());
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // returns the first available id, or -1 when we're fatally out of them (or db error .. hmm)
    // expected to be called seldom (at letter creation) and is allowed to be slow
    // obvious caching/persisting of TreeSet possible
    // immediately reusing the same id might be a privacy problem
    // or use database id increment instead of this?
    // no, legacy (and fuzziness) prevents us from using .nextId(Message.class) ..
    public int generateUID() {
        TreeSet<Integer> sortedSet = new TreeSet<Integer>();

        Iterator messageit = getDatabase().find(Message.class).findIds().iterator();
        while(messageit.hasNext()) {
            final Integer i = (Integer)messageit.next();
            sortedSet.add(i);
        }
        plugin.getCConfig().clog(Level.FINE, "Id: " + sortedSet.toString()); // todo: remove for release

/*        List<Message> messages = getDatabase().find(Message.class).findList();
        if (messages != null && !messages.isEmpty()) {
            for(Message message : messages) {
                sortedSet.add(message.getId());
            }
        }*/

        // make sure we don't enter negative number territory
        // todo: introduce "fuzziness" making nextId less predictable
        for(int i=Courier.MIN_ID; i<Courier.MAX_ID; i++) {
            if(sortedSet.add(i)) {
                // i wasn't in the set
                return i;
            }
        }
        return -1;
    }

    // returns the total number of Letters (rows) in the database
    public int totalLetters() {
        int count = getDatabase().find(Message.class).findList().size();
        plugin.getCConfig().clog(Level.FINE, "Number of Letters in database: " + count);
        return count;
    }
}
