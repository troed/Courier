package se.troed.plugin.Courier;

import org.bukkit.map.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

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
    private final String receiver;
    @SuppressWarnings("FieldCanBeLocal")
    private final String sender;
    private final int id;
    private final String message;
    private final String header;
    private final int date;
    private final String displayDate;
    private final int displayDatePos;
    // note, this is JUST to avoid event spamming. Actual read status is saved in CourierDB
    private boolean read;

    public Letter(String s, String r, String m, int id, boolean rd, int date) {
        sender = s;
        receiver = r;
        this.id = id;
        message = (m != null ? format(m) : m);
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
    
    public String getMessage() {
        return message;
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
    // what to do about height? I could scroll the text ... :)

    private String format(String s) {
//        String[] splitwords = s.split("\\s+");
        ArrayList<String> words = new ArrayList<String>();
//        Collections.addAll(words, splitwords);
        Collections.addAll(words, s.split("\\s+"));
        StringBuilder buffer = new StringBuilder();
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
//            System.out.println("newline");
        }
        return buffer.toString();
    }
}
