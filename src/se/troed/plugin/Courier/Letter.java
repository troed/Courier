package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

import java.util.*;
import java.util.logging.Level;

/**
 * A Letter is a cached database entry with text pre-formatted for Map rendering
 */
public class Letter {
    // while not specified with an API constant map width is hardcoded as 128 pixels
    @SuppressWarnings("FieldCanBeLocal")
    private final int CANVAS_WIDTH = 128;
    @SuppressWarnings("UnusedDeclaration")
    private final int CANVAS_HEIGHT = 128;
    static final String DATE_COLOR = "§"+(MapPalette.DARK_BROWN+2)+";";
    static final String HEADER_COLOR = "§"+(MapPalette.DARK_BROWN)+";";
    static final String HEADER_FROM_COLOR = "§"+(MapPalette.DARK_GREEN)+";";
    static final String MESSAGE_COLOR = "§"+(MapPalette.DARK_BROWN)+";";
    static final String MARKER_COLOR = "§"+(MapPalette.DARK_GREEN)+";";
    private final int MAP_HEIGHT_LINES = 12; // we get 12 full lines of text body into a map
    private final Courier plugin;
    private final String receiver;
    @SuppressWarnings("FieldCanBeLocal")
    private final String sender;
    private final int id;
    private List<String> message;
    private String topRow;
    private String header;
    private final int date;
    private String displayDate;
    private int displayDatePos;
    // note, this is JUST to avoid event spamming. Actual read status is saved in the database
    private boolean read;
    private int currentPage = 0;
    // to draw or not to draw
    private boolean dirty;

    public Letter(Courier plug, String s, String r, String m, int id, boolean rd, int date) {
        plugin = plug;
        sender = s;
        receiver = r;
        this.id = id;
        read = rd;
        this.date = date;
        // http://dev.bukkit.org/server-mods/courier/tickets/35-make-show-date-configurable/
        if(date > 0 && plugin.getCConfig().getShowDate()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setLenient(true);
            calendar.setTimeInMillis((long)(date) * 1000); // convert back from unix time

            String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            boolean valid;
            try {
                valid = MinecraftFont.Font.isValid(month);
            } catch (Exception e) {
                plugin.getCConfig().clog(Level.SEVERE, "Caught exception in MinecraftFont.Font.isValid(month)");
                valid = false;
            }
            if(!valid) {
                month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
            }
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            displayDate = (month != null ? month : "") + " " + day;
            try {
                displayDatePos = CANVAS_WIDTH - getWidth(displayDate);
            } catch (Exception e) {
                plugin.getCConfig().clog(Level.SEVERE, "Caught exception in MinecraftFont.Font.getWidth(displayDate)");
                displayDate = null;
                displayDatePos = 0; // whatever
            }
        } else {
            displayDate = null;
            displayDatePos = 0;
        }
        if(!r.equalsIgnoreCase(s)) { // r == s is an unposted Letter (same sender as receiver)
            header = plugin.getCConfig().getLetterFrom2(sender) + HEADER_COLOR + ":";
            try {
// See comment to getWidth on NPE
                if(getWidth(plugin.getCConfig().getLetterFrom(sender) + ":") > CANVAS_WIDTH) {
                    header = HEADER_FROM_COLOR + sender + HEADER_COLOR + ":";
                }
            } catch (Exception e) {
                plugin.getCConfig().clog(Level.SEVERE, "Caught exception in MinecraftFont.Font.getWidth(header)");
            }
        } else {
            header = null; // tested by LetterRenderer
        }
        // must be done after header, we use that knowledge for height calculation
        setMessage(m);
    }

    public void setDirty(boolean d) {
        dirty = d;
    }

    public boolean getDirty() {
        return dirty;
    }

    public int getId() {
        return id;
    }
    
    public String getReceiver() {
        return receiver;
    }
    
    public String getSender() {
        return sender;
    }
    
    public String getHeader() {
        return header;
    }

    // boolean OR intentional
    public boolean isAllowedToSee(Player p) {
        return receiver.equalsIgnoreCase(sender)        |    // Letters are public
               !plugin.getCConfig().getSealedEnvelope() |    // Config override
               p.hasPermission(Courier.PM_PRIVACYOVERRIDE) | // Permission to read all
               p.getName().equalsIgnoreCase(receiver);       // Player is receiver
    }

    // if we have more pages after format() than before, switch to the new page
    // hmm maybe should always switch to the _last_ page? extreme case
    // todo: http://dev.bukkit.org/server-mods/courier/tickets/55-letter-empty-after-creating-via-long-command-line/
    public void setMessage(String m) {
        int size;
        if(message != null) {
            size = message.size();
        } else {
            size = -1;
        }
        message = (m != null ? format(m) : null);
        if(message != null) {
            if(size != -1 && message.size() > size) {
                advancePage();
            }
        }
        setDirty(true);
    }
    
    public String getMessage() {
        return message.get(currentPage);
    }

    // helper method used when adding Lore to Letter ItemStacks
    public String getTopRow() {
        return topRow != null ? topRow : "";
    }

    public void advancePage() {
        if(currentPage < message.size()-1) {
            currentPage++;
//            plugin.getLetterRenderer().forceClear();
            setDirty(true);
        }
    }

    public void backPage() {
        if(currentPage > 0) {
            currentPage--;
//            plugin.getLetterRenderer().forceClear();
            setDirty(true);
        }
    }

    public int getPageCount() {
        return message.size();
    }

    // 0 internally is page 1 externally
    public int getCurPage() {
        return currentPage + 1;
    }

    // page 1 externally is 0 internally
    public void setCurPage(int p) {
        if(p > 0 && p <= message.size()) {
            currentPage = p - 1;
//            if(plugin.getLetterRenderer() != null) {
//                plugin.getLetterRenderer().forceClear();
                setDirty(true);
//            }
        }
    }
    
    public int getLeftMarkerPos() {
        return 48;
    }

    public String getLeftMarker() {
        return currentPage > 0 ? MARKER_COLOR + "<<" : "";
    }

    public int getRightMarkerPos() {
        return 64;
    }
    
    public String getRightMarker() {
        return currentPage < message.size()-1 ? MARKER_COLOR + ">>" : "";
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getRead() {
        return read;
    }
    
    public void setRead(boolean r) {
        read = r;
    }
    
    public String getDisplayDate() {
        return displayDate;
    }
    
    public int getDisplayDatePos() {
        return displayDatePos;
    }

    // splits and newlines a String to fit MapCanvas width
    // returns a list of pages
    private List<String> format(String s) {
        ArrayList<String> words = new ArrayList<String>();
        Collections.addAll(words, s.split("\\s+"));
        ArrayList<String> pages = new ArrayList<String>();
        StringBuilder buffer = new StringBuilder();
        int height = 0;
        int page = 0; // our current page
        int i = 0;
        while(i < words.size()) {
            int width = 0;
            int x = 0;
            while(i < words.size() && (x+width) <= CANVAS_WIDTH) {
                if(words.get(i).equals("&nl") || words.get(i).equals("\\n")) {
                    i++;
                    break; // inner loop break, will cause a newline
                }
                try {
                    width = getWidth(words.get(i)); // NPE warning!
                } catch (Exception e) {
                    i++; // obviously needs skipping
                    plugin.getCConfig().clog(Level.SEVERE, "Caught Exception in MinecraftFont.Font.getWidth()");
                    continue; // was break;
                }
                if(width > CANVAS_WIDTH) {
                    // always splits words in half, if they're still too long it wraps around and splits again ..
                    String orig = words.get(i);
                    String s1 = orig.substring(0, orig.length() / 2) + "-";
                    String s2 = orig.substring(s1.length() - 1); // -1 since we added "-" above
                    words.add(i, s1);
                    words.set(i+1, s2);
                    try {
                        width = getWidth(words.get(i)); // NPE warning!
                    } catch (Exception e) {
                        plugin.getCConfig().clog(Level.SEVERE, "Caught Exception in MinecraftFont.Font.getWidth()");
                    }
                }
                if((x+width) <= CANVAS_WIDTH) {
                    buffer.append(words.get(i));
                    buffer.append(" ");
                    x += (width + getWidth(" ")); // space cannot NPE
                    i++;
                }
            }
            // store first line of text on the first page for convenience
            if(height == 0 && page == 0) {
                topRow = buffer.toString();
            }
            // if there's more to come, newline and check if we need a new page
            if(i < words.size()) {
                buffer.append("\n");
                height++;
                if(height == MAP_HEIGHT_LINES || (header != null && page == 0 && height == MAP_HEIGHT_LINES-2)) {
                    height = 0;
                    pages.add(buffer.toString());
                    buffer.setLength(0); // clear();
                    page++;
                }
            }
        }
        if(pages.size() == page) {
            pages.add(buffer.toString());
        }
        return pages;
    }

    // getWidth() seems to NPE in MapFont.java:55 on '§'
    // Unicode Character 'SECTION SIGN' (U+00A7)
    // isValid() passes over '\u00a7' and '\n' - but getWidth() doesn't
    // https://bukkit.atlassian.net/browse/BUKKIT-685
    int getWidth(String s) throws NullPointerException {
        if((s == null) || s.isEmpty()) {
            return 0;
        }
        int width = MinecraftFont.Font.getWidth(s);
        width += s.length(); // getWidth currently does not include the space between characters (1px) in its calculation
        return width;
    }
}
