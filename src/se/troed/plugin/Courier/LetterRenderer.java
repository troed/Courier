package se.troed.plugin.Courier;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.*;

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
//    private final byte[] clearImage = new byte[128*128];  // nice letter background image todo
    private int lastId = -1;
    private boolean clear = false;

    public LetterRenderer(Courier p, boolean contextual) {
        super(contextual); // all our messages are contextual (i.e different for different players)
        plugin = p;
    }

    // This method gets called at 20tps whenever a map is in a players inventory. Bail out as quickly as possible if we
    // shouldn't do anything with it.
    // https://bukkit.atlassian.net/browse/BUKKIT-476
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        Letter letter = null;
        ItemStack item = player.getItemInHand();
        // todo: Trying to figure out if we can render Letters in ItemFrames
        if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
            // it's a Courier map, and we get called even when it's in an ItemFrame in a loaded chunk. Player doesn't
            // even need to be near it. Performance issues galore ...
            letter = plugin.getLetter(map.getCenterZ());
//            plugin.getCConfig().clog(Level.FINE, "Rendering a Courier ItemFrame map");
        }
        if(letter != null || (item != null && item.getType() == Material.MAP)) {
            if(letter == null) {
                letter = plugin.getLetter(item);
            }
            if(clear || (letter != null && lastId != letter.getId())) {
                for(int j = 0; j < CANVAS_HEIGHT; j++) {
                    for(int i = 0; i < CANVAS_WIDTH; i++) {
                        //                    canvas.setPixel(i, j, clearImage[j*128+i]);
                        canvas.setPixel(i, j, MapPalette.TRANSPARENT);
                    }
                }
                if(letter != null) {
                    lastId = letter.getId();
                }
                clear = false;
            }
            // todo: idea for pvp war servers: "your mail has fallen into enemy hands". "they've read it!")
            if(letter != null && letter.isAllowedToSee(player)) {
                int drawPos = HEADER_POS;
//                if(!letter.getReceiver().equalsIgnoreCase(letter.getSender())) {
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
    }
    
    // called by CourierCommands commandLetter. Not terribly pretty architectured.
    public void forceClear() {
        clear = true;
    }
}
