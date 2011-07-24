package se.troed.plugin.LoveSheep;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.DyeColor;
import org.bukkit.World;

public class InfatuatedSheep {

    private Sheep sheep;
//    private long myStartTime;
    private World world;

/*    public static final DyeColor dyeColors[] = {
            DyeColor.WHITE,
            DyeColor.ORANGE,
            DyeColor.MAGENTA,
            DyeColor.LIGHT_BLUE,
            DyeColor.YELLOW,
            DyeColor.LIME,
            DyeColor.PINK,
            DyeColor.GRAY,
            DyeColor.SILVER,
            DyeColor.CYAN,
            DyeColor.PURPLE,
            DyeColor.BLUE,
            DyeColor.BROWN,
            DyeColor.GREEN,
            DyeColor.RED,
            DyeColor.BLACK
    };*/

    public InfatuatedSheep(Sheep s, World w) {
        sheep = s;
//        myStartTime = w.getTime();
        world = w;
    }

/*    public void Dye(SheepColorPercentages p) {
        Random r = new Random();
        //iSheep.setColor(dyeColors[r.nextInt(dyeColors.length)]);
        //System.out.println("found the sheep");

        iSheep.setColor(p.PickColor(r.nextFloat()));
    }*/


    public boolean fellInLove() {
        boolean ret = false;
// want to do something with this. need our own list of LovePlayers? Max nr of sheep per player?
//        Player o = (Player)sheep.getTarget();
        if (!world.getLivingEntities().contains(sheep)) {
            // sheep not found in this world anymore
//            System.out.println("Sheep gone from this world");
        } else {
            LivingEntity e = sheep.getTarget();
            Player o = null;
            if(e instanceof Player) {
                o = (Player)e;
            }
            if(o != null) {
                // this sheep is already in love
                // is it successful? time to drop it? do something here
                // caveat: we only get here on sheep.spawn - that is, seldom
                // should maybe move stuff from here to EntityMove or PlayerMove
            } else {
                // see if there's an online player nearby in the same world as this sheep
                List<Player> playerList = world.getPlayers();
                if(playerList != null) {
                    try {
                        Iterator<Player> iterator = playerList.iterator();
                        while (iterator.hasNext()) {
                            Player p = iterator.next();
                            if (p.isOnline()) {
                                Location ploc = p.getLocation();
                                Location sloc = sheep.getLocation();
                                //                            Double dist = ploc.distance(sloc);
                                //                            System.out.println("Dist: " + dist.toString());
                                if (ploc.distance(sloc) < 60) { // ? what's a good value
                                    System.out.println("Sheep in love with " + p.getDisplayName() + "!");
                                    sheep.setTarget(p);
                                    sheep.setColor(DyeColor.PINK);
                                    ret = true;
                                }
                            }
                        }
                    } catch (Exception ex) {
    //                    System.out.println("Exception caught");
                    }
                } else {
    //                System.out.println("PlayerList null");
                }
            }
        }

        return ret;
    }
}