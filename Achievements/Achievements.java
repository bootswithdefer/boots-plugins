import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.minecraft.server.MinecraftServer;

public class Achievements extends Plugin
{
	private boolean enabled = false;
	private boolean useSQL = false;
	private boolean usehModDb = false;
	private boolean tweet = true;
   private String name = "Achievements";
   private int version = 16;
   private String directory = "achievements";
   private String listLocation = "achievements.txt";
	private String color = MyColors.LightBlue;
   private String prefix = "ACHIEVEMENT: ";
   private int delay = 300;
   private HashMap<String, AchievementListData> achievementList = new HashMap<String, AchievementListData>();
   private HashMap<String, PlayerAchievement> playerAchievements = new HashMap<String, PlayerAchievement>();
	
   static final Logger log = Logger.getLogger("Minecraft");

   public void enable()
   {
	   Plugin statsPlugin = etc.getLoader().getPlugin("Stats");
      if (statsPlugin == null)
      {
         log.log(Level.SEVERE, "Stats plugin not found, aborting load of " + name);
         return;
      }
		log.info(name + ": Found required plugin: " + statsPlugin.getName());

      PropertiesFile properties = new PropertiesFile("server.properties");
      try {
         directory = properties.getString("achievements-directory", "achievements");
         listLocation = properties.getString("achievements-list", "achievements.txt");
         delay = properties.getInt("achievements-delay", 300);
			prefix = properties.getString("achievements-prefix", "ACHIEVEMENT: ");
         color = properties.getString("achievements-color", MyColors.LightBlue);
			useSQL = properties.getBoolean("achievements-use-sql", false);
			usehModDb = properties.getBoolean("achievements-use-hmod-db", false);
			if (usehModDb)
				useSQL = true;
			tweet = properties.getBoolean("achievements-tweet", true);
      } 
      catch (Exception e) {
         log.log(Level.SEVERE, "Exception while reading from server.properties", e);
      }
	 	try {
			new File(directory).mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while creating directory " + directory, e);
		}
		
      etc.getInstance().addCommand("/achievements", " - Lists your achievements.");
      etc.getInstance().addCommand("/listachievements", " - Lists all achievements.");
      etc.getInstance().addCommand("/checkachievements", " - Checks achievements.");
      etc.getInstance().addCommand("/reloadachievements", " - Reloads achievements.");
      loadAchievementList();
		etc.getServer().addToServerQueue(new Checker(), delay*1000L);
		enabled = true;
		log.info(name + " v" + version + " Plugin Enabled.");
   }

   public void disable()
	{
		enabled = false;
		log.info(name + " v" + version + " Plugin Disabled.");
   }
	
	public void initialize()
	{
		new VersionCheck(name, version);
		
		AchievementsListener listener = new AchievementsListener();
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		
		etc.getLoader().addCustomListener(new AchievementAward());
	}

	private void info(String msg)
	{
		log.info(name + ": " + msg);
	}
	
	private void error(String msg)
	{
		log.log(Level.SEVERE, name + ": " + msg);
	}

	private void sendAchievementMessage(Player p, AchievementListData ach)
	{
		broadcast(MyColors.codeToColor(color) + prefix + p.getName() + " has been awarded " + ach.getName() + "!");
		p.sendMessage(MyColors.codeToColor(color) + "(" + ach.getDescription() + ")");
	}

	private void tweet(String player, AchievementListData ach)
	{
		if (!tweet)
			return;
		String msg = prefix + player + " has been awarded " + ach.getName() + "!";
		etc.getLoader().callCustomHook("tweet", new Object[] {msg});
	}

	protected AchievementListData getAchievement(String name)
	{
		return achievementList.get(name);
	}

   private void checkStats()
   {
      for  (Player p: etc.getServer().getPlayerList())
      {
         if (!playerAchievements.containsKey(p.getName())) // add player to achievement list
            loadPlayerAchievements(p.getName());
      
			PlayerAchievement pa = playerAchievements.get(p.getName());
		
         for (String name2: achievementList.keySet())
         {
            AchievementListData ach = achievementList.get(name2);
				if (!ach.isEnabled()) // disabled, skip
					continue;

				if (!ach.conditions.meets(this, pa))
					continue;

				Object ret = etc.getLoader().callCustomHook("get stat", new Object[] {p.getName(), ach.getCategory(), ach.getKey()});
				int playerValue = (Integer)ret;
				
          	if (playerValue < ach.getValue()) // doesn't meet requirements, skip
           		continue;

				//award achievement
				if (pa.hasAchievement(ach))
				{
					Achievement pad = pa.get(ach.getName());

					// already awarded
					if (pad.getCount() >= ach.getMaxawards())
						continue;

					if (pad.getCount() > 0 && playerValue < ((pad.getCount() + 1) * ach.getValue()))
						continue;
					
					pad.incrementCount();
				}
				else
				{
					// not already found
					pa.award(ach.getName());
				}

				sendAchievementMessage(p, ach);
				tweet(p.getName(), ach);
				pa.save();

				ach.commands.run(p);
         }
      }
   }

	private void awardAchievement(String player, String achievement)
	{
		if (!playerAchievements.containsKey(player))
            loadPlayerAchievements(player);

		PlayerAchievement pa = playerAchievements.get(player);

		AchievementListData ach = achievementList.get(achievement);
		if (ach == null) {
			error("unable to award (not found): " + achievement + " for: " + player);
			return;
		}
		if (!pa.hasAchievement(ach))
		{
			pa.award(achievement);
		}
		else
		{
			Achievement pad = pa.get(achievement);
			if (pad.getCount() < ach.getMaxawards())
				pad.incrementCount();
			else
				return;
		}
		
		Player p = etc.getServer().matchPlayer(player);
		if (p == null)
		{
			info("awarded: " + ach.getName() + " to offline player: " + player);
		}
		else
		{
			sendAchievementMessage(p, ach);
			ach.commands.run(p);
		}
		tweet(player, ach);
		
		pa.save();
	}

	private void loadPlayerAchievements(String player)
	{
		if (playerAchievements.containsKey(player))
		{
			log.log(Level.SEVERE, name + " attempting to load already loaded player: " + player);
			return;
		}
		PlayerAchievement pa;
		if (useSQL)
		{
			String location = directory + "/" + player + ".txt";
			File fold = new File(location);
			pa = new PlayerAchievementSQL(player, usehModDb);
			if (fold.exists())
			{
				PlayerAchievement paold = new PlayerAchievementFile(directory, player);
				paold.load();
				File fnew = new File(location + ".old");
				fold.renameTo(fnew);
				pa.copy(paold);
				pa.save();
			}
		}
		else
			pa = new PlayerAchievementFile(directory, player);
		pa.load();
		playerAchievements.put(player, pa);
	}

	private void unloadPlayerAchievements(String player)
	{
		if (!playerAchievements.containsKey(player))
		{
			log.log(Level.SEVERE, name + " attempting to unload a player who is not loaded: " + player);
			return;
		}
		PlayerAchievement pa = playerAchievements.get(player);
		pa.save();
		playerAchievements.remove(player);
	}

   private void saveAchievementList()
   {
      BufferedWriter writer = null;
      try {
         log.info("Saving " + listLocation);
         writer = new BufferedWriter(new	FileWriter(listLocation));
         writer.write("# Achievements");
         writer.newLine();
         writer.write("# Format is: enabled:name:maxawards:category:key:value:description[:commands[:conditions]]");
         writer.newLine();
         writer.write("# commands are optional, separated by semicolons (;), available commands: item group");
         writer.newLine();
         writer.write("# Example: 1:ClownPuncher:1:stats:armswing:1000:Awarded for punching the air 1000 times.[:item goldblock 1]");
         writer.newLine();
         for (String name: achievementList.keySet())
         {
            writer.write(achievementList.get(name).toString());
            writer.newLine();
         }
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception while creating "	+ listLocation,	e);
         } 
      finally {
         try {
            if (writer != null) {
               writer.close();
            }
         }	
            catch	(IOException e) {
            }
      }
   }

   private void loadAchievementList()
   {
      achievementList = new HashMap<String, AchievementListData>();
      if (!new File(listLocation).exists())
      {
         saveAchievementList();
         return;
      }
		String line = "";
      try {
         Scanner scanner =	new Scanner(new File(listLocation));
         while	(scanner.hasNextLine())
         {
            line = scanner.nextLine();
            if (line.startsWith("#") || line.equals(""))
               continue;
            String[] split = line.split(":");
            if (split.length < 7) {
               log.log(Level.SEVERE, "Malformed line, not enough fields (" + line + ") in " + listLocation);
               continue;
            }
				int enabled = Integer.parseInt(split[0]);
            int maxawards = Integer.parseInt(split[2]);
            int value = Integer.parseInt(split[5]);
				String commands = null;
				if (split.length > 7)
					commands = split[7];
				String conditions = null;
				if (split.length > 8)
					conditions = split[8];
            achievementList.put(split[1], new AchievementListData(enabled, split[1], maxawards, split[3], split[4], value, split[6], commands, conditions));
         }
         scanner.close();
      } 
      catch (Exception e) {
      	log.log(Level.SEVERE, "Exception while reading " + listLocation + " (" + line + ")", e);
      }
      if (achievementList.isEmpty())
         disable();
   }

   private void broadcast(String message)
   {
      for (Player	p : etc.getServer().getPlayerList()) {
         if	(p	!=	null) {
            p.sendMessage(message);
         }
      }
   }

	// task for server's DelayQueue
	private class Checker implements Runnable
	{
		public void run()
		{
			if (!enabled)
				return;
			checkStats();
			etc.getServer().addToServerQueue(this, delay*1000L);
		}
	}
	
	// custom plugin listener
	public class AchievementAward implements PluginInterface
	{
		public String getName() { return "award achievement"; }
		public int getNumParameters() { return 2; }

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
				
			return null;
		}

		public Object run(Object[] parameters)
		{
			String playername = (String)parameters[0];
			String achievement = (String)parameters[1];
			
			awardAchievement(playername, achievement);			
			return false;
		}
	}
	
	// hey0 command listener
	private class AchievementsListener extends PluginListener
	{
	   public void onLogin(Player player)
	   {
	      loadPlayerAchievements(player.getName());
	   }
	
	   public void onDisconnect(Player player)
	   {
			unloadPlayerAchievements(player.getName());
	   }
	
	   public boolean onCommand(Player player, String[] split)
	   {
	      if (split[0].equalsIgnoreCase("/achievements") || split[0].equalsIgnoreCase("/ach")) {
	         if (playerAchievements.get(player.getName()).isEmpty())
	         {
	            player.sendMessage(Colors.Rose + "You have no achievements.");
	            return true;
	         }
				PlayerAchievement pa = playerAchievements.get(player.getName());
				Iterator<String> iter = pa.iterator();
				while (iter.hasNext())
	         {
					String achievement = iter.next();
					Achievement pad = pa.get(achievement);
	         	AchievementListData ach = achievementList.get(achievement);
					if (ach == null) {
						player.sendMessage(MyColors.codeToColor(color) + achievement + " (OLD)");
						continue;
					}
	         	if (pad.getCount() > 1)
	         		player.sendMessage(MyColors.codeToColor(color) + achievement + " (" + pad.getCount() + "): " + ach.getDescription());
	         	else
	         		player.sendMessage(MyColors.codeToColor(color) + achievement + ": " + ach.getDescription());
	         }
	         return true;
	      }
			
			if (!player.canUseCommand(split[0]))
				return false;

			if (split[0].equalsIgnoreCase("/listachievements")) {
				player.sendMessage(Colors.Rose + "Enabled Name Maxawards Category Key Value");
				for (String name: achievementList.keySet()) {
					AchievementListData ach = achievementList.get(name);
					player.sendMessage(MyColors.codeToColor(color) + ach.toString());
				}
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/checkachievements")) {
	         checkStats();
	         player.sendMessage(Colors.Rose + "Achievements updated.");
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/reloadachievements")) {
	         loadAchievementList();
	         player.sendMessage(Colors.Rose + "Achievements reloaded.");
	         return true;
	      }
	      return false;
	   }
	}
}
