package se.troed.plugin.Courier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

import java.util.logging.Level;

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
    
    private Letter() {}
    
    public Letter(String s, String r, String m) {
        super(true); // all our messages are contextual (i.e different for different players)
        sender = s;
        receiver = r;
        message = m;
    }
    
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(player.getName().equals(receiver)) {
            String temp = "§"+MapPalette.DARK_GRAY+";Letter from §"+MapPalette.DARK_GREEN+";" + sender + "§"+MapPalette.DARK_GRAY+";:";
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, temp);
            canvas.drawText(0, MinecraftFont.Font.getHeight()*BODY_POS, MinecraftFont.Font, "§"+MapPalette.DARK_GRAY+";"+ format(message));

            // this is the actual time we can be sure a letter has been delivered
            // post an event to make sure we don't block the rendering pipeline
            CourierDeliveryEvent event = new CourierDeliveryEvent("COURIER_DELIVERY", player, map.getId());
            Bukkit.getServer().getPluginManager().callEvent(event);
        } else {
            String temp = "§"+MapPalette.DARK_GRAY+";Sorry, only §"+MapPalette.DARK_GREEN+";" + receiver + "§"+MapPalette.DARK_GRAY+";can read this letter";
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, temp);
        }
//        canvas.drawText(0, MinecraftFont.Font.getHeight()*5, MinecraftFont.Font, String.valueOf(map.getId()));
        
        if(player.getItemInHand().getType() == Material.MAP) {
//            System.out.println("Courier: User is holding a map");
        }
    }

    // splits and newlines a String to fit MapCanvas width
    // todo: what to do about height? I could scroll the text ... :)
    private String format(String s) {
        String[] words = s.split("\\s+");
        StringBuffer buffer = new StringBuffer();
        int i=0;
        while(i<words.length) {
            int width = 0;
            for(int x=0; i<words.length && (x+width) < CANVAS_WIDTH; i++) {
                // seems to NPE in MapFont.java:52 if we include the color codes ("§12;" etc) - most likely a bug
                width = MinecraftFont.Font.getWidth(words[i]); // NPE warning!
                if((x+width) < CANVAS_WIDTH) {
                    buffer.append(words[i]);
                    buffer.append(" ");
                    x+=width;
                }
            }
            buffer.append("\n");
        }
        return buffer.toString();
    }
    
    public void setReceiver(String r) {
        receiver = r;
    }
    
    public void setMessage(String m) {
        message = m;
    }
}
