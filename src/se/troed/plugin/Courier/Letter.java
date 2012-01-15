package se.troed.plugin.Courier;

import org.bukkit.map.*;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * A Letter is a cached database entry with text pre-formatted for Map rendering
 */
public class Letter {
    // while not specified with an API constant map width is hardcoded as 128 pixels
//    private final int CANVAS_WIDTH = 128; // I don't get the width calc correct .. or is getWidth buggy?
    @SuppressWarnings("FieldCanBeLocal")
    private final int CANVAS_WIDTH = 90; // 96 is a temp fix. Changed to 90 in 0.9.10. Need to deal with this properly.
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
//    private final String message;
    private List<String> message;
    private final String header;
    private final int date;
    private final String displayDate;
    private final int displayDatePos;
    // note, this is JUST to avoid event spamming. Actual read status is saved in CourierDB
    private boolean read;
    private int currentPage = 0;

    public Letter(Courier plug, String s, String r, String m, int id, boolean rd, int date) {
        plugin = plug;
        sender = s;
        receiver = r;
        this.id = id;
        read = rd;
        this.date = date;
        if(date > 0) {
            // Date date = new Date((long)(letter.getDate()) * 1000); // convert back from unix time
            Calendar calendar = Calendar.getInstance();
            calendar.setLenient(true);
            calendar.setTimeInMillis((long)(date) * 1000); // convert back from unix time
            String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            displayDate = (month != null ? month : "") + " " + day;
            displayDatePos = 112 - MinecraftFont.Font.getWidth(displayDate); // getWidth() must be so off
        } else {
            displayDate = null;
            displayDatePos = 0;
        }
        if(!r.equalsIgnoreCase(s)) { // r == s is an unposted Letter (same sender as receiver)
            if(s.length() < 13) { // a nice version would do an actual check vs width, but [see issue with width]
                header = HEADER_COLOR + "Letter from " + HEADER_FROM_COLOR + sender + HEADER_COLOR + ":";
            } else {
                header = HEADER_COLOR + "From " + HEADER_FROM_COLOR + sender + HEADER_COLOR + ":";
            }
        } else {
            header = null; // tested by LetterRenderer
        }
        // must be done after header, we use that knowledge for height calculation
        setMessage(m);
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
    }
    
    public String getMessage() {
        return message.get(currentPage);
    }

    public void advancePage() {
        if(currentPage < message.size()-1) {
            currentPage++;
            plugin.getLetterRenderer().forceClear();
        }
    }

    public void backPage() {
        if(currentPage > 0) {
            currentPage--;
            plugin.getLetterRenderer().forceClear();
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
                // seems to NPE in MapFont.java:52 if we include the color codes ("§12;" etc) - most likely a bug
                // doesn't seem to be possible to generate those characters from the in-game console though
                try {
                    width = MinecraftFont.Font.getWidth(words.get(i)); // NPE warning!
                } catch (NullPointerException e) {
                    i++; // obviously needs skipping
                    System.out.println("[COURIER]: Severe! Caught NullPointerException in MinecraftFont.Font.getWidth()");
                }
                if(width > CANVAS_WIDTH) {
                    // always splits words in half, if they're still too long it wraps around and splits again ..
                    String orig = words.get(i);
                    String s1 = orig.substring(0, orig.length() / 2) + "-";
                    String s2 = orig.substring(s1.length() - 1); // -1 since we added "-" above
                    words.add(i, s1);
                    words.set(i+1, s2);
                    width = MinecraftFont.Font.getWidth(words.get(i)); // NPE warning!
//                    System.out.println("Split " + orig + " into " + s1 + " and " + s2);
                }
                if((x+width) <= CANVAS_WIDTH) {
                    buffer.append(words.get(i));
                    buffer.append(" ");
                    x+=width;
//                    System.out.println("Appended " + words.get(i));
                    i++;
                }
            }
            buffer.append("\n");
            height++;
            if(height == MAP_HEIGHT_LINES || (header != null && page == 0 && height == MAP_HEIGHT_LINES-2)) {
                height = 0;
                pages.add(buffer.toString());
                buffer.setLength(0); // clear();
                page++;
            }
//            System.out.println("newline");
        }
        if(pages.size() == page) {
            pages.add(buffer.toString());
        }
        return pages;
    }
}
