package se.troed.plugin.Courier;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.List;
import java.util.logging.Level;

/**
 * Naughty: Implementing ServerCommands and onCommand in the same class
 * Nice: Implementing ServerCommands and onCommand in the same class
 */
class CourierCommands /*extends ServerListener*/ implements CommandExecutor {
    private final Courier plugin;

    public CourierCommands(Courier instance) {
        plugin = instance;
    }
    
    // Player is null for console
    private boolean allowed(Player p, String c) {
        boolean a = false;
        if (p != null) {
            if(c.equals(Courier.CMD_POSTMAN) && p.hasPermission(Courier.PM_POSTMAN)) {
                a = true;
            } else if(c.equals(Courier.CMD_COURIER) && p.hasPermission(Courier.PM_SEND)) {
                a = true;
            } else if(c.equals(Courier.CMD_POST) && p.hasPermission(Courier.PM_SEND)) {
                a = true;
            }
            plugin.getCConfig().clog(Level.FINE, "Player command event");
        } else {
            // console operator is op, no player and no location
            plugin.getCConfig().clog(Level.FINE, "Server command event");
        }
        plugin.getCConfig().clog(Level.FINE, "Permission: " + a);
        return a;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean ret = false;

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        // sender is always safe to sendMessage to - console as well as player
        // player can from now on be null!

        String cmd = command.getName().toLowerCase();
        if((cmd.equals(Courier.CMD_COURIER) || cmd.equals(Courier.CMD_POST)) && allowed(player, cmd)) {
            // not allowed to be run from the console, uses player
            if(args == null || args.length < 1) {
                sender.sendMessage("Courier: Error, no recipient for message!");
            } else if(args.length < 2) {
                sender.sendMessage("Courier: Error, message cannot be empty!");
            } else {
                OfflinePlayer[] offPlayers = plugin.getServer().getOfflinePlayers();
                OfflinePlayer p = null;
                for(OfflinePlayer o : offPlayers) {
                    if(o.getName().equalsIgnoreCase(args[0])) {
                       p = o;
                       plugin.getCConfig().clog(Level.FINE, "Found " + p.getName() + " in OfflinePlayers");
                       break;
                    }
                }
                if(p == null) {
                    // See https://bukkit.atlassian.net/browse/BUKKIT-404 by GICodeWarrior
                    // https://github.com/troed/Courier/issues/2
                    // We could end up here if this is to a player who's on the server for the first time
                    p = plugin.getServer().getPlayerExact(args[0]);
                    if(p != null) {
                        plugin.getCConfig().clog(Level.FINE, "Found " + p.getName() + " in getPlayerExact");
                    }
                }
                if(p == null) {
                    // still not found, try lazy matching and display suggestions
                    // (searches online players only)
                    List<Player> players = plugin.getServer().matchPlayer(args[0]);
                    if(players != null && players.size() == 1) {
                        // we got one exact match
                        // p = players.get(0); // don't, could be embarrassing if wrong
                        sender.sendMessage("Courier: Couldn't find " + args[0] + ". Did you mean " + players.get(0).getName() + "?");
                    } else if (players != null && players.size() > 1) {
                        // more than one possible match found
                        StringBuilder suggestList = new StringBuilder();
                        int width = 0;
                        for(Player pl : players) {
                            suggestList.append(pl.getName());
                            suggestList.append(" ");
                            width += pl.getName().length()+1;
                            if(width >= 40) { // todo: how many chars can the console show?
                                suggestList.append("\n");
                                width = 0;
                            }
                        }
                        sender.sendMessage("Courier: Couldn't find " + args[0] + ". Did you mean anyone of these players?");
                        sender.sendMessage("Courier: " + suggestList.toString());
                    } else {
                        // time to give up
                        sender.sendMessage("Courier: There's no player on this server with the name " + args[0]);
                    }
                }
                if(p != null) {
                    // Q: Maps are saved in the world-folders, "null" isn't valid as world then?
                    MapView map = plugin.getServer().createMap(player.getWorld());
                    plugin.getCConfig().clog(Level.FINE, "Map ID = " + map.getId());
                    // make it ours
                    map.setCenterX(Courier.MAGIC_NUMBER);
                    map.setCenterZ((int)(System.currentTimeMillis() / 1000L)); // oh noes unix y2k issues!!!11
                    List<MapRenderer> renderers = map.getRenderers();
                    for(MapRenderer r : renderers) { // remove existing renderers
                        map.removeRenderer(r);
                    }

                    sender.sendMessage("Courier: Message to " + p.getName() + " sent!");
                    // todo: figure out max length and show if a cutoff was made
                    // Minecraftfont isValid(message)

                    StringBuilder message = new StringBuilder();
                    for(int i=1; i<args.length; i++) {
                        message.append(args[i]);
                        message.append(" ");
                    }

                    plugin.getCourierdb().storeMessage(map.getId(), p.getName(), sender.getName(),  message.toString());
                }
                ret = true;
            }
        } else if(cmd.equals(Courier.CMD_POSTMAN) && allowed(player, cmd)){
            // not allowed to be run from the console, uses player
            if(plugin.getCourierdb().undeliveredMail(player.getName())) {
                short undeliveredMessageId = plugin.getCourierdb().undeliveredMessageId(player.getName());
                if(undeliveredMessageId != -1) {
                    sender.sendMessage("You've got mail!");

                    // Is it the FIRST map viewed on server start that gets the wrong id when rendering?
                    // how can that be? if it's my code I don't see where ...

                    plugin.getCConfig().clog(Level.FINE, "MessageId: " + undeliveredMessageId);
                    String from = plugin.getCourierdb().getSender(player.getName(), undeliveredMessageId);
                    String message = plugin.getCourierdb().getMessage(player.getName(), undeliveredMessageId);
                    plugin.getCConfig().clog(Level.FINE, "Sender: " + from + " Message: " + message);
                    if(from != null && message != null) {
                        Location spawnLoc = plugin.findSpawnLocation(player);
                        if(spawnLoc != null) {
                            Postman postman = new Postman(plugin, player, spawnLoc, undeliveredMessageId);
                            plugin.addPostman(postman);
                        }

                    } else {
                        plugin.getCConfig().clog(Level.SEVERE, "Gotmail but no sender or message found! mapId=" + undeliveredMessageId);
                    }
                } else {
                    plugin.getCConfig().clog(Level.WARNING, "Gotmail but no mailid!");
                }
            }
            ret = true;
        }
        return ret;
    }

/*    public void onServerCommand(ServerCommandEvent event) {
        plugin.getCConfig().clog(Level.FINE, "Server command event");
    }*/
}


