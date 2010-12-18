import java.util.List;
import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

public class EntityClean extends Plugin
{
	private String name = "EntityClean";
	private int version = 1;
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	public void enable()
	{
		log.info(name + " v" + version + " Plugin Enabled.");
		etc.getInstance().addCommand("/eclean", " - Clean Entities.");
	}

	public void disable()
	{
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		new VersionCheck(name, version);

		ECListener listener = new ECListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
	}
	
	public class ECListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split)
		{
			if (!player.canUseCommand("/eclean"))
				return false;
				
			if (split[0].equalsIgnoreCase("/eclean"))
			{
				try {
					List<Mob> ml = etc.getServer().getMobList();
					for (Mob m: ml)
						m.setHealth(-1);
					ml = etc.getServer().getAnimalList();
					for (Mob m: ml)
						m.setHealth(-1);
					player.sendMessage(Colors.Rose + "Done.");
				} catch (NoSuchMethodError ex) {
				}
			
				return true;
			}

			if (split[0].equalsIgnoreCase("/etpvh"))
			{
				List<BaseVehicle> vehl = etc.getServer().getVehicleEntityList();
				for (BaseVehicle v: vehl)
				{
					v.teleportTo(player);
				}
				player.sendMessage("Done.");
				return true;
			}

			if (split[0].equalsIgnoreCase("/etpmob"))
			{
				List<Mob> ml = etc.getServer().getMobList();
				for (Mob m: ml)
					m.teleportTo(player);
				player.sendMessage("Done.");
				return true;
			}
			
			if (split[0].equalsIgnoreCase("/etpani"))
			{
				List<Mob> ml = etc.getServer().getAnimalList();
				for (Mob m: ml)
					m.teleportTo(player);
				player.sendMessage("Done.");
				return true;
			}
			
			if (split[0].equalsIgnoreCase("/ecount"))
			{
				player.sendMessage("Mob spawn rate: " + etc.getInstance().getMobSpawnRate());
			
				try {
					List<Player> pl = etc.getServer().getPlayerList();
					player.sendMessage("Player count: " + pl.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<Mob> ml = etc.getServer().getMobList();
					player.sendMessage("Mob count: " + ml.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<Mob> al = etc.getServer().getAnimalList();
					player.sendMessage("Animal count: " + al.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<Minecart> mcl = etc.getServer().getMinecartList();
					player.sendMessage("Minecart count: " + mcl.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<Boat> bl = etc.getServer().getBoatList();
					player.sendMessage("Boat count: " + bl.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<BaseEntity> basel = etc.getServer().getEntityList();
					player.sendMessage("Base count: " + basel.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<LivingEntity> livingl = etc.getServer().getLivingEntityList();
					player.sendMessage("Living count: " + livingl.size());
				} catch (NoSuchMethodError ex) {
				}

				try {
					List<BaseVehicle> vehl = etc.getServer().getVehicleEntityList();
					player.sendMessage("Vehicle count: " + vehl.size());
				} catch (NoSuchMethodError ex) {
				}
				
				return true;
			}
			
			return false;
		}
	}
}
