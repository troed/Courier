package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class LetterRenderer extends MapRenderer {

    @SuppressWarnings("FieldCanBeLocal")
    private final int HEADER_POS = 2; // 2*getHeight()
    @SuppressWarnings("FieldCanBeLocal")
    private final int BODY_POS = 4; // 4*getHeight()
    private final Courier plugin;
    @SuppressWarnings("FieldCanBeLocal")
    private final int CANVAS_WIDTH = 128;
    @SuppressWarnings("FieldCanBeLocal")
    private final int CANVAS_HEIGHT = 128;
    private final byte[] clearImage;
    BufferedImage laminated = null;
    private int lastId = -1;
    private boolean clear = false;

    public LetterRenderer(Courier p) {
        super(true); // all our messages are contextual (i.e different for different players)
        plugin = p;
        try {
            // todo: issues at /reload here
            InputStream is = plugin.getClass().getResourceAsStream("/laminated.png");
            laminated = ImageIO.read(is);
            is.close();
        } catch (IOException e) {
            plugin.getCConfig().clog(Level.WARNING, "Unable to find laminated.png in .jar");
            e.printStackTrace();
        }
        if(laminated != null) {
            plugin.getCConfig().clog(Level.FINE, "Laminated image found");
            clearImage = imageToBytes(laminated);
        } else {
            clearImage = new byte[128*128]; // todo: manually in code "shiny" background image for laminated letters? nah, make this null
        }
    }

    // what? I'm getting _constant_ calls to this renderer method, 20tps, no matter if I'm holding a map or not!
    // it starts as soon as I have a map (to check: any map or Courier) in the inventory, but whether it's
    // in my hands or not isn't relevant
    // this is also in old code with non-enchanted maps. bug reported.
    // https://bukkit.atlassian.net/browse/BUKKIT-476
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
//        System.out.print("render(); ");
        // thanks to the above bug we end up here even if we're not holding a Map specifically
        Letter letter = plugin.getTracker().getLetter(player.getItemInHand());
        if(clear || (letter != null && lastId != letter.getId())) {
            if(letter != null && letter.isLaminated()) {
//            canvas.drawImage(0, 0, laminated);
                for(int j = 0; j < CANVAS_HEIGHT; j++) {
                    for(int i = 0; i < CANVAS_WIDTH; i++) {
                        canvas.setPixel(i, j, clearImage[j * CANVAS_WIDTH + i]);
                    }
                }
            } else {
                for(int j = 0; j < CANVAS_HEIGHT; j++) {
                    for(int i = 0; i < CANVAS_WIDTH; i++) {
                        canvas.setPixel(i, j, MapPalette.TRANSPARENT);
                    }
                }
            }
            if(letter != null) {
                lastId = letter.getId();
            }
            clear = false;
        }

        if(letter != null && letter.isAllowedToSee(player.getName())) {
            int drawPos = HEADER_POS;
            if(letter.getHeader() != null) {
                canvas.drawText(0, MinecraftFont.Font.getHeight() * drawPos, MinecraftFont.Font, letter.getHeader());
                drawPos = BODY_POS;
            }

            canvas.drawText(letter.getLeftMarkerPos(), MinecraftFont.Font.getHeight(), MinecraftFont.Font, letter.getLeftMarker());
            canvas.drawText(letter.getRightMarkerPos(), MinecraftFont.Font.getHeight(), MinecraftFont.Font, letter.getRightMarker());

            canvas.drawText(0,
                            MinecraftFont.Font.getHeight() * drawPos,
                            MinecraftFont.Font, Letter.MESSAGE_COLOR + letter.getMessage());

            if(letter.getDisplayDate() != null) {
                canvas.drawText(letter.getDisplayDatePos(),
                                0,
                                MinecraftFont.Font, Letter.DATE_COLOR + letter.getDisplayDate());
            }

            // this is the actual time we can be sure a letter has been read
            // post an event to make sure we don't block the rendering pipeline
            if(!letter.getRead()) {
                CourierReadEvent event = new CourierReadEvent(player, letter.getId());
                plugin.getServer().getPluginManager().callEvent(event);
                letter.setRead(true);
            }
        } else if(letter != null) {
            String temp = Letter.HEADER_COLOR + "Sorry, only " + Letter.HEADER_FROM_COLOR +
                          letter.getReceiver() + "\n" + Letter.HEADER_COLOR + "can read this letter";
            canvas.drawText(0, MinecraftFont.Font.getHeight()*HEADER_POS, MinecraftFont.Font, temp);
        }
    }
    
    // called by CourierCommands commandLetter. Not terribly pretty architectured.
    public void forceClear() {
        clear = true;
    }

    // MapPalette.imageToBytes fails to create Colors with alpha - corrected below
    // https://bukkit.atlassian.net/browse/BUKKIT-852
    public static byte[] imageToBytes(Image image) {
        BufferedImage temp = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = temp.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        int[] pixels = new int[temp.getWidth() * temp.getHeight()];
        temp.getRGB(0, 0, temp.getWidth(), temp.getHeight(), pixels, 0, temp.getWidth());

        byte[] result = new byte[temp.getWidth() * temp.getHeight()];
        for (int i = 0; i < pixels.length; i++) {
            result[i] = MapPalette.matchColor(new Color(pixels[i], true)); // correction: new Color(int, true)
        }
        return result;
    }    
}
