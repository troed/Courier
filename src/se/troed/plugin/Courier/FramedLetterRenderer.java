package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

import java.util.logging.Level;

public class FramedLetterRenderer extends MapRenderer {

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

    public FramedLetterRenderer(Courier p) {
        super(false); // Framed Letters are not contextual (i.e same for all players)
        plugin = p;
    }

    // This method gets called at 20tps whenever a map is in a players inventory. Bail out as quickly as possible if we
    // shouldn't do anything with it.
    // https://bukkit.atlassian.net/browse/BUKKIT-476
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(map.getCenterX() == Courier.MAGIC_NUMBER && map.getId() != plugin.getCourierdb().getCourierMapId()) {
            // it's a Courier map in an ItemFrame. We get called when it's in a loaded chunk. Player doesn't
            // even need to be near it. Performance issues galore ...
            Letter letter = plugin.getTracker().getLetter(map.getCenterZ());
            if(letter != null && letter.getDirty()) {
                plugin.getCConfig().clog(Level.FINE, "Rendering a Courier ItemFrame Letter (" + letter.getId() + ") on Map (" + map.getId() + ")");
                for(int j = 0; j < CANVAS_HEIGHT; j++) {
                    for(int i = 0; i < CANVAS_WIDTH; i++) {
                        //                    canvas.setPixel(i, j, clearImage[j*128+i]);
                        canvas.setPixel(i, j, MapPalette.TRANSPARENT);
                    }
                }

                int drawPos = HEADER_POS;

                if(letter.getHeader() != null) {
                    canvas.drawText(0, MinecraftFont.Font.getHeight() * drawPos, MinecraftFont.Font, letter.getHeader());
                    LetterRenderer.drawLine(canvas, 10, MinecraftFont.Font.getHeight() * (drawPos+1) +
                            (int)(MinecraftFont.Font.getHeight() * 0.4), CANVAS_WIDTH-11, MapPalette.DARK_BROWN);
                    drawPos = BODY_POS;
                }

                canvas.drawText(letter.getLeftMarkerPos(), MinecraftFont.Font.getHeight(), MinecraftFont.Font, letter.getLeftMarker());
                canvas.drawText(letter.getRightMarkerPos(), MinecraftFont.Font.getHeight(), MinecraftFont.Font, letter.getRightMarker());

                if(letter.getMessage() != null) {
                    canvas.drawText(0,
                                    MinecraftFont.Font.getHeight() * drawPos,
                                    MinecraftFont.Font, Letter.MESSAGE_COLOR + letter.getMessage());
                }

                if(letter.getDisplayDate() != null) {
                    canvas.drawText(letter.getDisplayDatePos(),
                                    0,
                                    MinecraftFont.Font, Letter.DATE_COLOR + letter.getDisplayDate());
                }
                letter.setDirty(false);
                player.sendMap(map);
            }
        }
    }
}
