package se.troed.plugin.Courier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 */
public class Letter extends MapRenderer {
    // while not specified with an API constant map width is hardcoded as 128 pixels
//    private final int CANVAS_WIDTH = 128; // I don't get the width calc correct .. or is getWidth buggy?
    @SuppressWarnings("FieldCanBeLocal")
    private final int CANVAS_WIDTH = 96; // 96 is a temp fix
    @SuppressWarnings("UnusedDeclaration")
    private final int CANVAS_HEIGHT = 128;
    @SuppressWarnings("FieldCanBeLocal")
    private final int HEADER_POS = 2; // 2*getHeight()
    @SuppressWarnings("FieldCanBeLocal")
    private final int BODY_POS = 4; // 4*getHeight()
    private String receiver;
    @SuppressWarnings("FieldCanBeLocal")
    private String sender;
    private String message;
    private String header;
    // note, this is JUST to avoid event spamming. Actual read status is saved in CourierDB
    private boolean read;
    private Letter() {}
    
    private Letter(String s, String r, String m) {
        super(true); // all our messages are contextual (i.e different for different players)
        sender = s;
        receiver = r;
        message = format(m);
        if(s != null && s.length() < 13) { // a nice version would do an actual check vs width, but [see issue with width]
            header = "§"+MapPalette.DARK_GRAY+";Letter from §"+MapPalette.DARK_GREEN+";" + sender + "§"+MapPalette.DARK_GRAY+";:";
        } else {
            header = "§"+MapPalette.DARK_GRAY+";From §"+MapPalette.DARK_GREEN+";" + sender + "§"+MapPalette.DARK_GRAY+";:";
        }
    }

    public Letter(String s, String r, String m, boolean rd) {
        this(s, r, m);
        read = rd;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(player.getName().equals(receiver)) {
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, header);
            canvas.drawText(0, MinecraftFont.Font.getHeight()*BODY_POS, MinecraftFont.Font, "§"+MapPalette.DARK_GRAY+";"+ message);

            // todo: add date

            // this is the actual time we can be sure a letter has been read
            // post an event to make sure we don't block the rendering pipeline
            if(!read) {
                CourierDeliveryEvent event = new CourierDeliveryEvent(CourierDeliveryEvent.COURIER_READ, player, map.getId());
                Bukkit.getServer().getPluginManager().callEvent(event);
                read = true;
            }
        } else {
            String temp = "§"+MapPalette.DARK_GRAY+";Sorry, only §"+MapPalette.DARK_GREEN+";" + receiver + "\n§"+MapPalette.DARK_GRAY+";can read this letter";
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, temp);
        }
    }

    // splits and newlines a String to fit MapCanvas width
    // what to do about height? I could scroll the text ... :)

    private String format(String s) {
        String[] splitwords = s.split("\\s+");
        ArrayList<String> words = new ArrayList<String>();
        Collections.addAll(words, splitwords);
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while(i < words.size()) {
            int width = 0;
            int x = 0;
            while(i < words.size() && (x+width) <= CANVAS_WIDTH) {
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
