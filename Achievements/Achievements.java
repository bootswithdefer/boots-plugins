import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.minecraft.server.MinecraftServer;

public class Achievements extends Plugin
{
	private boolean versionCheck = true;
   private String name = "Achievements";
   private int version = 11;
   private boolean stopTimer = false;
   private String directory = "achievements";
   private String listLocation = "achievements.txt";
	private String color = MyColors.LightBlue;
   private String prefix = "ACHIEVEMENT: ";
   private int delay = 300;
   private HashMap<String, AchievementListData> achievementList = new HashMap<String, AchievementListData>();
   private HashMap<String, HashMap<String, PlayerAchievementData>> playerAchievements = new HashMap<String, HashMap<String, PlayerAchievementData>>();
	
   static final Logger log = Logger.getLogger("Minecraft");

   private void startTimer()
	{
      stopTimer = false;
      final Timer timer = new Timer();
      timer.schedule(
            new TimerTask() {
               @Override
               	public void run() {
                  if (stopTimer) {
                     timer.cancel();
                     return;
                  }
                  checkStats();
               }
            }, 3000, delay*1000);
   }

   private void stopTimer()
	{
      stopTimer = true;
   }
	
	public void checkNotifications(Player player)
	{
	}

	private void sendAchievementMessage(Player p, AchievementListData ach)
	{
		broadcast(MyColors.codeToColor(color) + prefix + p.getName() + " has been awarded " + ach.getName() + "!");
		p.sendMessage(MyColors.codeToColor(color) + "(" + ach.getDescription() + ")");
	}
	
	public boolean hasAchievement(Player p, AchievementListData ach)
	{
		if (ach == null)
			return false;
		if (playerAchievements.get(p.getName()).containsKey(ach.getName()))
			return true;
		return false;
	}

	public AchievementListData getAchievement(String name)
	{
		return achievementList.get(name);
	}

   private void checkStats()
   {
      for  (Player p: etc.getServer().getPlayerList())
      {
         if (!playerAchievements.containsKey(p.getName())) // add player to achievement list
            loadPlayerAchievements(p.getName());
      
         for (String name2: achievementList.keySet())
         {
            AchievementListData ach = achievementList.get(name2);
				if (!ach.isEnabled()) // disabled, skip
					continue;

				if (!ach.conditions.meets(p, this))
					continue;

				Object ret = etc.getLoader().callCustomHook("get stat", new Object[] {p.getName(), ach.getCategory(), ach.getKey()});
				int playerValue = (Integer)ret;
				
          	if (playerValue < ach.getValue()) // doesn't meet requirements, skip
           		continue;

				PlayerAchievementData pad = playerAchievements.get(p.getName()).get(ach.getName());

				//award achievement
				if (pad != null) {
					// already awarded
					if (pad.getCount() >= ach.getMaxawards())
						continue;

					if (pad.getCount() > 0 && playerValue < ((pad.getCount() + 1) * ach.getValue()))
						continue;
					
					pad.incrementCount();							
				} else {
					// not already found
					pad = new PlayerAchievementData(ach.getName(), 1);
          		playerAchievements.get(p.getName()).put(ach.getName(), pad);
				}

				sendAchievementMessage(p, ach);
				pad.notified();
            savePlayerAchievements(p.getName());

				ach.commands.run(p);
         }
      }
   }

	private void awardAchievement(String player, String achievement)
	{
		if (!playerAchievements.containsKey(player))
            loadPlayerAchievements(player);

		AchievementListData ach = achievementList.get(achievement);
		if (ach == null) {
			error("unable to award (not found): " + achievement + " for: " + player);
			return;
		}
		PlayerAchievementData pad;
		
		if (!playerAchievements.get(player).containsKey(achievement))
		{
			pad = new PlayerAchievementData(achievement, 1);
			playerAchievements.get(player).put(achievement, pad);
		}
		else
		{
			pad = playerAchievements.get(player).get(achievement);
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
			pad.notified();
		}

		savePlayerAchievements(player);
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

   private void savePlayerAchievements(String player)
   {
      String location = directory + "/" + player + ".txt";
      BufferedWriter writer = null;
      try {
         writer = new BufferedWriter(new	FileWriter(location));
         writer.write("# " + location);
         writer.newLine();
         for (String p: playerAchievements.get(player).keySet())
         {
				PlayerAchievementData pad = playerAchievements.get(player).get(p);
            writer.write(pad.toString());
            writer.newLine();
         }
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception while creating "	+ location,	e);
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

   private void loadPlayerAchievements(String player)
   {
      if (playerAchievements.containsKey(player))
         playerAchievements.remove(player);
      playerAchievements.put(player, new HashMap<String, PlayerAchievementData>());
   
      String location = directory + "/" + player + ".txt";
      if (!new File(location).exists())
         return;
      try {
         Scanner scanner =	new Scanner(new File(location));
         while	(scanner.hasNextLine())
         {
            String line =	scanner.nextLine();
            if (line.startsWith("#") || line.equals(""))
               continue;
            String[] split = line.split(":");
            if (split.length < 1) {
               log.log(Level.SEVERE, "Malformed line (" + line + ") in " + location);
               continue;
            }
            int count = 1;
            if (split.length >= 2)
               count = Integer.parseInt(split[1]);
            playerAchievements.get(player).put(split[0], new PlayerAchievementData(split[0], count));
         }
         scanner.close();
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception	while	reading " +	location, e);
         }
   }

   private void broadcast(String message)
   {
      for (Player	p : etc.getServer().getPlayerList()) {
         if	(p	!=	null) {
            p.sendMessage(message);
         }
      }
   }
	
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
			versionCheck = properties.getBoolean("boots-version-check", true);
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception while reading from server.properties", e);
         }
      etc.getInstance().addCommand("/achievements", " - Lists your achievements.");
      etc.getInstance().addCommand("/listachievements", " - Lists all achievements.");
      etc.getInstance().addCommand("/checkachievements", " - Checks achievements.");
      etc.getInstance().addCommand("/reloadachievements", " - Reloads achievements.");
      loadAchievementList();
      startTimer();
		log.info(name + " v" + version + " Plugin Enabled.");
   }

   public void disable()
	{
      stopTimer();
		log.info(name + " v" + version + " Plugin Disabled.");
   }
	
	public void initialize()
	{
		new VersionCheck(name, version, versionCheck);
		
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
	public class AchievementsListener extends PluginListener
	{
	   public void onLogin(Player player)
	   {
	      loadPlayerAchievements(player.getName());
			checkNotifications(player);
	   }
	
	   public void onDisconnect(Player player)
	   {
	   }
	
	   public boolean onCommand(Player player, String[] split)
	   {
	      if (split[0].equalsIgnoreCase("/achievements") || split[0].equalsIgnoreCase("/ach")) {
	         if (playerAchievements.get(player.getName()).isEmpty())
	         {
	            player.sendMessage(Colors.Rose + "You have no achievements.");
	            return true;
	         }
	         for (String p: playerAchievements.get(player.getName()).keySet())
	         {
					PlayerAchievementData pad = playerAchievements.get(player.getName()).get(p);
	         	AchievementListData ach = achievementList.get(pad.getName());
					if (ach == null) {
						player.sendMessage(MyColors.codeToColor(color) + pad.getName() + " (OLD)");
						continue;
					}
	         	if (pad.getCount() > 1)
	         		player.sendMessage(MyColors.codeToColor(color) + pad.getName() + " (" + pad.getCount() + "): " + ach.getDescription());
	         	else
	         		player.sendMessage(MyColors.codeToColor(color) + pad.getName() + ": " + ach.getDescription());
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
