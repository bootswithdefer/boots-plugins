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
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import net.minecraft.server.MinecraftServer;

public class Stats extends Plugin
{
	private String name = "Stats";
	private int version = 8;
	private PlayerMap playerStats = new PlayerMap();
	private boolean stopTimer = false;
	private String directory = "stats";
	private int savedelay = 30;
	private String[] ignoredGroups = new String[] {""};

	static final Logger log	= Logger.getLogger("Minecraft");

	private void startTimer() {
		stopTimer =	false;
		final	Timer	timer	= new	Timer();
				  timer.schedule(new	TimerTask()	{
							@Override
							public void	run()	{
					if	(stopTimer)	{
						timer.cancel();
						return;
					}
					save();
				}
			},	3000,	savedelay*1000);
	}

	private void stopTimer() {
		stopTimer =	true;
	}

	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("server.properties");
		try {
			directory = properties.getString("stats-directory", "stats");
			String s = properties.getString("stats-ignored-groups", "default");
			ignoredGroups = s.split(",");
			savedelay = properties.getInt("stats-save-delay", 30);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception	while	reading from server.properties",	e);
		}
		startTimer();
	 	try {
			new File(directory).mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while creating directory " + directory, e);
		}
		log.info(name + " v" + version + " Mod Enabled.");
	}

	public void disable()
	{
		stopTimer();
		save();
		playerStats = new PlayerMap();
	}

	public void initialize()
	{
		log.info(name + " initializing.");
	
		StatsListener listener = new StatsListener();
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.LOW);
		
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
		updateStat(player.getName(), "stats", statType, num);
	}
	
	private void updateStat(Player player, String statType, Block block)
	{
		updateStat(player, statType, block, 1);
	}
		
	private void updateStat(Player player, String statType, Block block, int num)
	{
		String blockName = etc.getDataSource().getItem(block.getType());
		updateStat(player.getName(), statType, blockName, num);
	}
	
	private void updateStat(String player, String category, String key, int val)
	{
		int oldval = playerStats.get(player, category, key);
		if (oldval == -1)
			oldval = 0;
		playerStats.put(player, category, key, oldval + val);
	}

	public int get(String player, String category, String key)
	{
		return playerStats.get(player, category, key);
	}

	private void load(Player player)
	{
		if (inIgnoredGroup(player))
			return;
		playerStats.load(directory, player.getName());
	}

	private void unload(Player player)
	{
		if (inIgnoredGroup(player))
			return;
		playerStats.unload(directory, player.getName());
	}
	
	private void save()
	{
		playerStats.saveAll(directory);
	}
	
	private boolean inIgnoredGroup(Player player)
	{
		for (String g: ignoredGroups) {
			if (player.isInGroup(g))
				return true;
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
			
			int data = get(name, category, key);
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
		}
	
		public void onDisconnect(Player player)
		{
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

		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
		{
			Block before = new Block(etc.getServer().getBlockIdAt(blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ()), blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ());
			
			if (before.getType() == blockPlaced.getType())
				return false;
				
			updateStat(player, "blockcreate", blockPlaced);
			updateStat(player, "blockdestroy", before);
			return false;
		}
		
		public boolean onBlockDestroy(Player player, Block blockAt)
		{
			Block after = new Block(etc.getServer().getBlockIdAt(blockAt.getX(), blockAt.getY(), blockAt.getZ()), blockAt.getX(), blockAt.getY(), blockAt.getZ());
			
			if (after.getType() == blockAt.getType())
				return false;
				
			updateStat(player, "blockdestroy", blockAt);
			updateStat(player, "blockcreate", after);
			return false;
		}
		
		public void onArmSwing(Player player)
		{
			updateStat(player, "armswing");
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
}
