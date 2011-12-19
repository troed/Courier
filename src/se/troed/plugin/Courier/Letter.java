package se.troed.plugin.Courier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

/**
 */
public class Letter extends MapRenderer {
    // while not specified with an API constant it seems map width is hardcoded as 128 pixels
    // "Each map is 128x128 pixels in size" - minecraftwiki
//    private final int CANVAS_WIDTH = 128; // I don't get the width calc correct .. or is getWidth buggy?
    private final int CANVAS_WIDTH = 96;
    private final int CANVAS_HEIGHT = 128;
    private final int HEADER_POS = 2; // 2*getHeight()
    private final int BODY_POS = 4; // 4*getHeight()
    private String receiver;
    private String sender;
    private String message;
    // note, this is JUST to avoid event spamming. Actual read status is saved in CourierDB
    private boolean read;
    private Letter() {}
    
    public Letter(String s, String r, String m) {
        super(true); // all our messages are contextual (i.e different for different players)
        sender = s;
        receiver = r;
        message = m;
    }

    public Letter(String s, String r, String m, boolean rd) {
        this(s, r, m);
        read = rd;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(player.getName().equals(receiver)) {
            String temp = "§"+MapPalette.DARK_GRAY+";Letter from §"+MapPalette.DARK_GREEN+";" + sender + "§"+MapPalette.DARK_GRAY+";:";
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, temp);
            canvas.drawText(0, MinecraftFont.Font.getHeight()*BODY_POS, MinecraftFont.Font, "§"+MapPalette.DARK_GRAY+";"+ format(message));

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

        if(player.getItemInHand().getType() == Material.MAP) {
//            System.out.println("Courier: User is holding a map");
        }
    }

    // splits and newlines a String to fit MapCanvas width
    // todo: what to do about height? I could scroll the text ... :)

    private String format(String s) {
        String[] words = s.split("\\s+");
        StringBuffer buffer = new StringBuffer();
        int i = 0;
        while(i < words.length) {
            int width = 0;
            int x = 0;
            while(i < words.length && (x+width) < CANVAS_WIDTH) {
                // seems to NPE in MapFont.java:52 if we include the color codes ("§12;" etc) - most likely a bug
                width = MinecraftFont.Font.getWidth(words[i]); // NPE warning!
                if(width >= CANVAS_WIDTH) {
// split
                    i++; // just skip long words for now
                }
                if((x+width) < CANVAS_WIDTH) {
                    buffer.append(words[i]);
                    buffer.append(" ");
                    x+=width;
                    i++;
                }
            }
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
