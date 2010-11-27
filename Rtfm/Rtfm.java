import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

public class Rtfm extends Plugin
{
	private String name = "Rtfm";
	private int version = 3;
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	public void enable()
	{
		log.info(name + " v" + version + " Plugin Enabled.");
		etc.getInstance().addCommand("/rtfm", " - Read the F***ing Manual.");
	}

	public void disable()
	{
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		new VersionCheck(name, version);

		RtfmListener listener = new RtfmListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
	}
	
	public class RtfmListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split) {
			if (split[0].equalsIgnoreCase("/rtfm")) {
	 			if (!player.isInGroup("builder")) {
					boolean newUser = false;
					String playername = player.getName();
					// update player's group
					player.addGroup("builder");	
	
			                if (!etc.getDataSource().doesPlayerExist(player.getName())) {
	       			             newUser = true;
			                }
	
			                if (newUser) {
	       			             etc.getDataSource().addPlayer(player);
			                } else {
	       			             etc.getDataSource().modifyPlayer(player);
			                }
	
					for  (Player p : etc.getServer().getPlayerList() ) {
						if (player == p)
							continue;
						p.sendMessage(Colors.Yellow + "Congratulations to "+playername+" for reading the manual!");
						if (!p.isInGroup("builder"))
							p.sendMessage(Colors.Yellow + "Maybe you should try reading /motd too!");
					}
				}
				
				etc.getLoader().callCustomHook("award achievement", new Object[] {player.getName(), "RTFM"});
	
				player.sendMessage(Colors.Rose + "You can now build except with 500 blocks of spawn.");
				player.sendMessage(Colors.Yellow + "Certain other areas are protected from building.");
				player.sendMessage(Colors.Rose + "Tell people to read /motd, do NOT tell them to /rtfm");
				player.sendMessage(Colors.Yellow + "Visit the web site in /motd for rules and pictures.");
				player.sendMessage(Colors.Rose + "Helpful commands: /help, /spawn, /kit, /listwarps, /who");
				player.sendMessage(Colors.Yellow + "To get to the free build spawn: /warp FreeBuild");
				
				return true;
			}
			return false;
		}
	}
}
