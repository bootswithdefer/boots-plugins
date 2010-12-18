import java.util.ArrayList;
import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;


public class NoTp extends Plugin
{
	private String name = "NoTp";
	private int version = 1;
	private ArrayList<String> notps = new ArrayList<String>();	
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	public void enable()
	{
		log.info(name + " v" + version + " Plugin Enabled.");
		etc.getInstance().addCommand("/notp", " - Prevent someone from teleporting.");
	}

	public void disable()
	{
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		new VersionCheck(name, version);

		NoTpListener listener = new NoTpListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.HIGH);
	}
	
	public class NoTpListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split)
		{
			if (!player.canUseCommand("/notp"))
				return false;
			if (split[0].equalsIgnoreCase("/notp"))
			{
				if (split.length < 2)
				{
					player.sendMessage(Colors.Rose + "Usage: /notp [player]");
					return true;
				}

				Player target = etc.getServer().matchPlayer(split[1]);
				
				if (!player.isAdmin() && !player.hasControlOver(target))
				{
					player.sendMessage(Colors.Rose + "You can't do that to them.");
					target.sendMessage(Colors.Rose + player.getName() + " just tried to /notp you.");
					return true;
				}
				
				if (notps.contains(target.getName()))
				{
					player.sendMessage(Colors.Rose + "Removed.");
					notps.remove(target.getName());
					return true;
				}

				notps.add(target.getName());
				player.sendMessage(Colors.Rose + "Added.");
				return true;
			}
			return false;
		}
		
		public boolean onTeleport(Player player, Location from, Location to)
		{
			if (player.isAdmin())
				return false;
			if (notps.contains(player.getName()))
				return true;
			return false;
		}
	}
}
