package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

import java.util.logging.Level;

/**
 */
public class Letter extends MapRenderer {
    // while not specified with an API constant it seems map width is hardcoded as 128 pixels
    // guessing height as well
    private final int CANVAS_WIDTH = 128;
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
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, format(temp));
            canvas.drawText(0, MinecraftFont.Font.getHeight()*BODY_POS, MinecraftFont.Font, "§"+MapPalette.DARK_GRAY+";"+ format(message));
        } else {
            String temp = "§"+MapPalette.DARK_GRAY+";Sorry, only §"+MapPalette.DARK_GREEN+";" + receiver + "§"+MapPalette.DARK_GRAY+";can read this letter";
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, format(temp));
        }
//        canvas.drawText(0, MinecraftFont.Font.getHeight()*5, MinecraftFont.Font, String.valueOf(map.getId()));
    }

    // splits and newlines a String according to MapCanvas width
    // todo: what to do about height? I could scroll the text ... :)
    private String format(String s) {
        String[] words = s.split("\\s+");
        StringBuffer buffer = new StringBuffer();
        int i=0;
        while(i<words.length) {
            for(int x=0; x+MinecraftFont.Font.getWidth(words[i]) < CANVAS_WIDTH && i<words.length; i++, x+=MinecraftFont.Font.getWidth(words[i])) {
                buffer.append(words[i]);
                buffer.append(" ");
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
