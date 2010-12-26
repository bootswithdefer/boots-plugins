import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.minecraft.server.MinecraftServer;

public class Stats extends Plugin
{
	private boolean enabled = false;
	private String name = "Stats";
	private int version = 21;
	private HashMap<String, PlayerStat> stats = new HashMap<String, PlayerStat>();
	private String directory = "stats";
	private boolean useSQL = false;
	private boolean usehModDb = false;
	private int delay = 30;
	private String[] ignoredGroups = new String[] {""};
	private final String defaultCategory = "stats";
	private Block lastface = null;

	static final Logger log	= Logger.getLogger("Minecraft");

	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("server.properties");
		try {
			directory = properties.getString("stats-directory", "stats");
			String s = properties.getString("stats-ignored-groups", "default");
			ignoredGroups = s.split(",");
			delay = properties.getInt("stats-save-delay", 30);
			useSQL = properties.getBoolean("stats-use-sql", false);
			usehModDb = properties.getBoolean("stats-use-hmod-db", false);
			if (usehModDb)
				useSQL = true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception	while	reading from server.properties",	e);
		}
	 	try {
			new File(directory).mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while creating directory " + directory, e);
		}
		
      for (Player p: etc.getServer().getPlayerList())
			load(p);

		etc.getServer().addToServerQueue(new SaveAll(), delay*1000L);
		enabled = true;
		etc.getInstance().addCommand("/stats", " - Stats plugin command");
		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		saveAll();
		stats = new HashMap<String, PlayerStat>();
		enabled = false;
		etc.getInstance().removeCommand("/stats");
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		log.info(name + " initializing.");
		
		new VersionCheck(name, version);
	
		StatsListener listener = new StatsListener();
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_BROKEN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_DROP, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_PICK_UP, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_USE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.HEALTH_CHANGE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DAMAGE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.IGNITE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.VEHICLE_ENTERED, listener, this, PluginListener.Priority.MEDIUM);
		
		try {
			etc.getLoader().addCustomListener(new StatsGet());
			etc.getLoader().addCustomListener(new StatsSet());
		} catch (NoClassDefFoundError ex) {
			log.info(name + "no class def");
		}
	}

	private void updateStat(Player player, String statType)
	{
		updateStat(player, statType, 1);
	}

	private void updateStat(Player player, String statType, int num)
	{
		updateStat(player.getName(), defaultCategory, statType, num);
	}
	
	private void updateStat(Player player, String statType, Block block)
	{
		updateStat(player, statType, block, 1);
	}
		
	private void updateStat(Player player, String statType, Block block, int num)
	{
	   if (block.getType() <= 0 || (block.getX() ==0 && block.getY() == 0 && block.getZ() == 0))
			return;
		String blockName = etc.getDataSource().getItem(block.getType());
		updateStat(player.getName(), statType, blockName, num);
	}

	private void updateStat(Player player, String statType, Item item)
	{
		String itemName = etc.getDataSource().getItem(item.getItemId());
		int amount = item.getAmount();
		if (amount == 0)
			amount = 1;
		updateStat(player.getName(), statType, itemName, amount);
	}

	private void updateStat(Player player, String category, String key, int val)
	{
		updateStat(player.getName(), category, key, val);
	}

	// primary updateStat
	private void updateStat(String player, String category, String key, int val)
	{
		if (player == null || player.length()<1)
		{
			log.log(Level.SEVERE, "updateStat got empty player for [" + category + "] [" + key + "] [" + val + "]");
			return;
		}
	
		PlayerStat ps = stats.get(player);
		if (ps == null)
			return;
		Category cat = ps.get(category);
		if (cat == null)
			cat = ps.newCategory(category);
		cat.add(key, val);
	}

	private void logout(String player)
	{
		int now = (int)(System.currentTimeMillis()/1000L);
		setStat(player, defaultCategory, "lastlogout", now);

		int loginTime = get(player, defaultCategory, "lastlogin");
		if (loginTime > 0 && now > loginTime)
			updateStat(player, defaultCategory, "playedfor", now - loginTime);
	}

	private void setStat(String player, String category, String key, int val)
	{
		PlayerStat ps = stats.get(player);
		if (ps == null)
			return;
		ps.put(category, key, val);
	}

	public int get(String player, String category, String key)
	{
		PlayerStat ps = stats.get(player);
		if (ps == null)
			return 0;
		Category cat = ps.get(category);
		if (cat == null)
			return 0;
		return cat.get(key);
	}

	private void load(Player player)
	{
		if (inIgnoredGroup(player))
			return;
		if (stats.containsKey(player.getName()))
		{
			log.log(Level.SEVERE, name + " attempting to load already loaded player: " + player.getName());
			return;
		}
		PlayerStat ps;
		if (useSQL)
		{
			ps = new PlayerStatSQL(player.getName(), usehModDb);

			String location = directory + "/" + player.getName() + ".txt";
			if (new File(location).exists())
				ps.convertFlatFile(directory);
		}
		else
			ps = new PlayerStatFile(player.getName(), directory);
		ps.load();
		stats.put(player.getName(), ps);
	}

	private void unload(Player player)
	{
		if (inIgnoredGroup(player))
			return;
		if (!stats.containsKey(player.getName()))
		{
			log.log(Level.SEVERE, name + " attempting to unload an player that's not loaded: " + player.getName());
			return;
		}
		PlayerStat ps = stats.get(player.getName());
		ps.save();
		stats.remove(player.getName());
	}

	private void saveAll()
	{
		int count = 0;

      for (Player p: etc.getServer().getPlayerList())
			if (inIgnoredGroup(p) && stats.containsKey(p.getName()))
				stats.remove(p.getName());				

		Iterator<String> iter = stats.keySet().iterator();
		ArrayList<String> remove = new ArrayList<String>();

		while (iter.hasNext())
		{
			String name = iter.next();
			PlayerStat stat = stats.get(name);
			stat.save();
			count++;
			if (etc.getServer().matchPlayer(name) == null)
				remove.add(name);
		}
		
		for (String name: remove)
		{
			log.log(Level.SEVERE, name + " onDisconnect did not happen, unloading " + name + " now");
			logout(name);
			stats.get(name).save();
			stats.remove(name);
		}
		
//		log.info("Saved " + count + "/" + stats.size() + " stat files...");
	}
	
	private boolean inIgnoredGroup(Player player)
	{
		for (String ignored: ignoredGroups) {
			for (String group: player.getGroups()) {
				if (ignored.equalsIgnoreCase(group))
					return true;
			}
		}
		return false;
	}
	
	// custom listener class
	public class StatsGet implements PluginInterface
	{
		public String getName() { return "get stat"; }
		public int getNumParameters() { return 3; }

		public String checkParameters(Object[] parameters)
		{
			boolean good;

			if (parameters.length != getNumParameters())
				return getName() + ": incorrect number of parameters, got: " + parameters.length + " expected: " + getNumParameters();
			
			good = false;
			if (parameters[0] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 0 should be String";
			good = false;
			if (parameters[1] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 1 should be String";
			good = false;
			if (parameters[2] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 2 should be String";
				
			return null;
		}

		public Object run(Object[] parameters)
		{
			String name = (String)parameters[0];
			String category = (String)parameters[1];
			String key = (String)parameters[2];
			
			Integer data = get(name, category, key);
			
			return data;
		}
	}
	public class StatsSet implements PluginInterface
	{
		public String getName() { return "set stat"; }
		public int getNumParameters() { return 4; }

		public String checkParameters(Object[] parameters)
		{
			boolean good;

			if (parameters.length != getNumParameters())
				return getName() + ": incorrect number of parameters, got: " + parameters.length + " expected: " + getNumParameters();
			
			good = false;
			if (parameters[0] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 0 should be String";
			good = false;
			if (parameters[1] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 1 should be String";
			good = false;
			if (parameters[2] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 2 should be String";
			good = false;
			if (parameters[3] instanceof Integer)
				good = true;
			if (!good)
				return getName() + ": parameter 3 should be Integer";
				
			return null;
		}

		public Object run(Object[] parameters)
		{
			String name = (String)parameters[0];
			String category = (String)parameters[1];
			String key = (String)parameters[2];
			Integer value = (Integer)parameters[3];
			
			updateStat(name, category, key, value);
			return true;
		}
	}
	// Listener Class
	public class StatsListener extends PluginListener
	{
	   public void onPlayerMove(Player player, Location from, Location to)
		{
			updateStat(player, "move");
		}
	
		public void onLogin(Player player)
		{
			load(player);
			// TODO: rate limit to prevent abuse
			updateStat(player, "login");
			setStat(player.getName(), defaultCategory, "lastlogin", (int)(System.currentTimeMillis()/1000L));
		}
	
		public void onDisconnect(Player player)
		{
			logout(player.getName());
			unload(player);
		}
	
		public boolean onChat(Player player, String message)
		{
			updateStat(player, "chat");
			updateStat(player, "chatletters", message.length());
			return false;
		}
		
		public boolean onCommand(Player player, String[] split)
		{
			if (player.canUseCommand("/stats") && split[0].equalsIgnoreCase("/stats"))
			{
				if (split.length == 2 && split[1].equalsIgnoreCase("convert"))
				{
					if (!useSQL)
					{
						player.sendMessage(Colors.Rose + "You are not running with SQL enabled.");
						return true;
					}
					
					File dir = new File(directory);
					FilenameFilter filter = new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".txt");
						}
					}; 
					String[] files = dir.list(filter);
					if (files == null)
					{
						player.sendMessage(Colors.Rose + "No files to convert.");
						return true;
					}
					
					int count = 0;
					PlayerStat ps;
					for (int i=0; i < files.length; i++)
					{
						String location = directory + "/" + files[i];
						File fold = new File(location);
						if (!fold.exists())
							continue;

						String basename = files[i].substring(0, files[i].lastIndexOf("."));
//						log.info(name + " convert: " + basename);
						ps = new PlayerStatSQL(basename, usehModDb);
						ps.convertFlatFile(directory);
						count++;
					}
					
					player.sendMessage(Colors.Rose + "Converted " + count + " stat files.");
					return true;
				}
				player.sendMessage(Colors.Rose + "Player Stats in memory: " + stats.size());
				return true;
			}
		
			updateStat(player, "command");
			return false;
		}
	
		public void onBan(Player player, String reason)
		{
			updateStat(player, "ban");
		}
	
		public void onIpBan(Player player, String reason)
		{
			updateStat(player, "ipban");
		}
		
		public void onKick(Player player, String reason)
		{
			updateStat(player, "kick");
		}

		public void onBlockRightClicked(Player player, Block blockClicked, Item item)
		{
			lastface = blockClicked.getFace(blockClicked.getFaceClicked());
		}

		public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand)
		{
			updateStat(player, "blockcreate", blockPlaced);
			updateStat(player, "totalblockcreate");
			if (lastface != null)
			{
				updateStat(player, "totalblockdestroy");
				updateStat(player, "blockdestroy", lastface);
			}
			return false;
		}
		
		public boolean onBlockBreak(Player player, Block block)
		{
			updateStat(player, "blockdestroy", block);
			updateStat(player, "totalblockdestroy");
			return false;
		}
		
		public void onArmSwing(Player player)
		{
			updateStat(player, "armswing");
		}

		public boolean onTeleport(Player player, Location from, Location to)
		{
			updateStat(player, "teleport");
			return false;
		}
		
		public boolean onItemDrop(Player player, Item item)
		{
			updateStat(player, "itemdrop", item);
			return false;
		}

		public boolean onItemPickUp(Player player, Item item)
		{
			updateStat(player, "itempickup", item);
			return false;
		}

		public boolean onHealthChange(Player player, int oldValue, int newValue)
		{
			int change = newValue - oldValue;
			if (change > 0)
				updateStat(player, "damagehealed", change);
			return false;
		}
		
		public boolean onDamage(PluginLoader.DamageType type, BaseEntity attacker, BaseEntity defender, int amount)
		{
			Player d = null, a = null;
			String typeName = null;
			Boolean died = false;
			
			if (defender != null && defender.isPlayer())
				d = defender.getPlayer();
			if (attacker != null && attacker.isPlayer())
				a = attacker.getPlayer();

			// one or other must be player
			if (d == null && a == null)
				return false;

			switch (type)
			{
				case ENTITY:
					if (a != null && d != null)
						typeName = "player";
					else if (defender instanceof LivingEntity)
					{
						LivingEntity le = (LivingEntity)defender;
						if (le instanceof Mob)
							typeName = ((Mob)le).getName();
						else
							typeName = "livingdefender";
					}
					else if (attacker instanceof LivingEntity)
					{
						LivingEntity le = (LivingEntity)attacker;
						if (le instanceof Mob)
							typeName = ((Mob)le).getName();
						else
							typeName = "livingattacker";
					}
					else if (defender instanceof Boat || attacker instanceof Boat)
						typeName = "boat";
					else if (defender instanceof Minecart || attacker instanceof Minecart)
						typeName = "minecart";
					else
						typeName = "entity";
					break;
				case CREEPER_EXPLOSION: typeName = "creeperexplosion"; break;
				case EXPLOSION: typeName = "explosion"; break;
				case FALL: typeName = "fall"; break;
				case FIRE:
				case FIRE_TICK: typeName = "fire"; break;
				case LAVA: typeName = "lava"; break;
				case WATER: typeName = "water"; break;
				case CACTUS: typeName = "cactus"; break;
			}
			
			if (typeName == null)
			{
				log.info(name + " got unknown damage type: " + type);
				return false;
			}

			if (defender instanceof LivingEntity)
			{
				LivingEntity le = (LivingEntity)defender;
				if ((le.getHealth() - amount) <= 0)
					died = true;
			}

			// attacker is player
			if (a != null)
			{
				updateStat(a, "damagedealt", "total", amount);
				updateStat(a, "damagedealt", typeName, amount);
				
				if (died)
				{
					updateStat(a, "kills", "total", 1);
					updateStat(a, "kills", typeName, 1);
				}
			}

			// defender is player
			if (d != null)
			{
				updateStat(d, "damagetaken", "total", amount);
				updateStat(d, "damagetaken", typeName, amount);
				
				if (died)
				{
					updateStat(d, "deaths", "total", 1);
					updateStat(d, "deaths", typeName, 1);
				}
			}
				
			return false;
		}
		
		public boolean onItemUse(Player player, Item item)
		{
			updateStat(player, "itemuse", item);
			return false;
		}
		
		public boolean onIgnite(Block block, Player player)
		{
			if (player == null)
				return false;
			if (block.getStatus() != 2)
				return false;
			updateStat(player, "lighter");
			return false;
		}

		public void onVehicleEnter(BaseVehicle vehicle, HumanEntity player)
		{
			Player p = vehicle.getPassenger();
			if (p == null)
				return;

			String vehicleName = etc.getDataSource().getItem(vehicle.getId());

			updateStat(p, "vehicleenter", vehicleName, 1);
		}
	}
	// End Listener Class
	
	private class AgeBlock {
		public Block block;
		public long time;
		
		AgeBlock(Block block) {
			this.block = block;
			this.time = System.currentTimeMillis();
		}
		
		public boolean isOld(long now) {
			long delta = 1000 * 60; // 1 minute
			if (now > time + delta)
				return true;
			return false;
		}
	}
	
	// task for server's DelayQueue
	private class SaveAll implements Runnable
	{
		public void run()
		{
			if (!enabled)
				return;
			saveAll();
			etc.getServer().addToServerQueue(this, delay*1000L);
		}
	}
}
