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
	private int version = 5;
	private PlayerMap playerStats = new PlayerMap();
	private boolean stopTimer = false;
	private String directory = "stats";
	private int savedelay = 30;
   private HashMap<String, AgeBlock> tBlocks = new HashMap<String, AgeBlock>();

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
					clean();
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
			savedelay = properties.getInt("stats-save-delay", 30);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception	while	reading from server.properties",	e);
		}
		startTimer();
		log.info(name + " v" + version + " Mod Enabled.");
	}

	public void disable()
	{
		stopTimer();
	}

	public void initialize()
	{
		StatsListener listener = new StatsListener();
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PRE_MODIFY, listener, this, PluginListener.Priority.CRITICAL);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_POST_MODIFY, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.LOW);
		
		etc.getLoader().addCustomListener(new StatsGet());
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
		playerStats.load(directory, player.getName());
	}
	
	private void unload(Player player)
	{
		playerStats.unload(directory, player.getName());
	}
	
	private void save()
	{
		playerStats.saveAll(directory);
	}
	
	private void clean()
	{
		long now = System.currentTimeMillis();
		int count = 0;

		for (Iterator<Map.Entry<String, AgeBlock>> it = tBlocks.entrySet().iterator(); it.hasNext();)
		{
    		Map.Entry<String, AgeBlock> entry = it.next();
    		if(entry.getValue().isOld(now)) {
         	it.remove();
				count++;
			}
		}

		if (count > 0)
			log.info(name + " old blocks cleaned: " + count);
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

		public void onBlockPreModify(Player player, Block blockAt, int itemInHand)
		{
			String bKey = player.getName() + " " + blockAt.getX() + " " + blockAt.getY() + " " + blockAt.getZ();
			tBlocks.put(bKey, new AgeBlock(blockAt));
		}
		
		public void onBlockPostModify(Player player, Block blockAt, int itemInHand)
		{
//			Block b1 = blockAt;
//			log.info("oBPM: post " + etc.getDataSource().getItem(b1.getType()) + "(" + b1.getX() + "," + b1.getY() + "," + b1.getZ() + ")");
			
			// get block from previous onBlockPreModify
			String bKey = player.getName() + " " + blockAt.getX() + " " + blockAt.getY() + " " + blockAt.getZ();
			AgeBlock aBlock = tBlocks.get(bKey);
			if (aBlock == null)
				return;
			tBlocks.remove(bKey);

			// if blocks are the same do nothing
			if (blockAt.getType() == aBlock.block.getType())
				return;

			updateStat(player, "blockcreate", blockAt);
			updateStat(player, "blockdestroy", aBlock.block);
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
