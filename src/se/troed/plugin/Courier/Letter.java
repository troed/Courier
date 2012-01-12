package se.troed.plugin.Courier;

import org.bukkit.map.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 */
public class Letter {
    // while not specified with an API constant map width is hardcoded as 128 pixels
//    private final int CANVAS_WIDTH = 128; // I don't get the width calc correct .. or is getWidth buggy?
    @SuppressWarnings("FieldCanBeLocal")
    private final int CANVAS_WIDTH = 90; // 96 is a temp fix. Changed to 90 in 0.9.10. Need to deal with this properly.
    @SuppressWarnings("UnusedDeclaration")
    private final int CANVAS_HEIGHT = 128;
    private String receiver;
    @SuppressWarnings("FieldCanBeLocal")
    private String sender;
    private String message;
    private String header;
    private int id;
    // note, this is JUST to avoid event spamming. Actual read status is saved in CourierDB
    private boolean read;
    private Letter() {}
    
    private Letter(String s, String r, String m, int id) {
        sender = s;
        receiver = r;
        this.id = id;
        message = format(m);
        if(s != null && s.length() < 13) { // a nice version would do an actual check vs width, but [see issue with width]
            header = "§"+MapPalette.DARK_GRAY+";Letter from §"+MapPalette.DARK_GREEN+";" + sender + "§"+MapPalette.DARK_GRAY+";:";
        } else {
            header = "§"+MapPalette.DARK_GRAY+";From §"+MapPalette.DARK_GREEN+";" + sender + "§"+MapPalette.DARK_GRAY+";:";
        }
    }

    public Letter(String s, String r, String m, int id, boolean rd) {
        this(s, r, m, id);
        read = rd;
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
