package se.troed.plugin.Courier;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

/**
 * todo: render differently for different players
 * "This is a letter to xxxxxx"
 * set contextual to true and verify letter recipient in render()
 */
public class LetterRenderer extends MapRenderer {

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        canvas.drawText(1, 1, MinecraftFont.Font, "You've got mail!\n\n Dear " + player.getDisplayName());
    }
}
