package info.avatartoolbox.kaba.TieDyeSheep;

//import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class TieDyeSheep extends JavaPlugin{
	
	private final TieDyeSheep_EntityListener entityListener = new TieDyeSheep_EntityListener(this);

	
    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        System.out.println( this.getDescription().getName() + " is now disabled." );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Normal, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
        
        readConfig();
    }
    
    private void generateDefaultConfig()
    {
    	System.out.println( this.getDescription().getName() + " is generating a default config file.");
    	
    	PluginDescriptionFile pdfFile = this.getDescription();
    	Configuration config = this.getConfiguration();
    	
    	Integer i = 1;
    	config.setProperty("TieDyeSheep", pdfFile.getVersion());
    	config.setProperty("color.WHITE", i);
    	config.setProperty("color.ORANGE", i);
    	config.setProperty("color.MAGENTA", i);
    	config.setProperty("color.LIGHT_BLUE", i);
    	config.setProperty("color.YELLOW", i);
    	config.setProperty("color.LIME", i);
    	config.setProperty("color.PINK", i);
    	config.setProperty("color.GRAY", i);
    	config.setProperty("color.SILVER", i);
    	config.setProperty("color.CYAN", i);
    	config.setProperty("color.PURPLE", i);
    	config.setProperty("color.BLUE", i);
    	config.setProperty("color.BROWN", i);
    	config.setProperty("color.GREEN", i);
    	config.setProperty("color.RED", i);
    	config.setProperty("color.BLACK", i);
    	
    	config.save();
    }
    
    private void readConfig()
    {
        Configuration config = this.getConfiguration();
        config.load();
        if (config.getString("TieDyeSheep") == null)
        {
        	generateDefaultConfig();
        }
        entityListener.updatedConfig();
        
    }
}
