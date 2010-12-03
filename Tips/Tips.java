import java.util.HashMap;
import java.util.ArrayList;
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
import java.util.Random;

import net.minecraft.server.MinecraftServer;

public class Tips extends Plugin {
   private String name = "Tips";
	private int version = 9;
   private String location = "tips.txt";
   private String  color = MyColors.LightBlue;
   private String  prefix = "TIP: ";
   private boolean stopTimer = false;
   private int delay = 120;
   private ArrayList<TipGroup> tips = new ArrayList<TipGroup>();
	private Random generator = null;
	private int currentGroup = 0;
	private static final String splitChar = "|";

   static final Logger log = Logger.getLogger("Minecraft");

	private TipGroup getTipGroup(String group)
	{
		for (int i=0; i < tips.size(); i++)
			if (tips.get(i).group.equals(group))
				return tips.get(i);
		return null;
	}

   private void startTimer() {
      stopTimer = false;
      final Timer timer = new Timer();
              timer.schedule(new TimerTask() {
                     @Override
                     public void run() {
               if (stopTimer) {
                  timer.cancel();
                  return;
               }
               broadcastTip();
            }
         }, 3000, delay*1000);
   }

   private void stopTimer() {
      stopTimer = true;
   }

   private String splittoline(String[] split, int start) {
      String line = "";
      for (int i=start; i<split.length; i++) {
          line = line + split[i];
          if (i < split.length-1)
         line = line + " ";
      }
      return line;
   }

	public void displayTip(Player player, String tip)
	{
		for (String t : tip.split("@"))
		{
			String message = MyColors.codeToColor(color) + prefix + t;
			player.sendMessage(message);
		}
	}

	public void displayTip(Player player, String group, int num, String tip)
	{
		for (String t : tip.split("@"))
		{
			String message = MyColors.codeToColor(color) + group + " " + num + ": " + prefix + t;
			player.sendMessage(message);
		}
	}

	public void broadcastTip(TipGroup tg)
	{
		for (Player p : etc.getServer().getPlayerList())
			if (tg.group.equals("all") || p.isInGroup(tg.group))
				displayTip(p, tg.nextTip());
	}

	public void broadcastTip(TipGroup tg, int num)
	{
		for (Player p : etc.getServer().getPlayerList())
			if (tg.group.equals("all") || p.isInGroup(tg.group))
				displayTip(p, tg.tips.get(num));
	}

   public void broadcastTip()
	{
		if (tips.isEmpty())
			return;

		broadcastTip(tips.get(currentGroup));
		
		currentGroup++;
		if (currentGroup >= tips.size())
			currentGroup = 0;
   }

   private void saveTips()
   {
      BufferedWriter writer = null;
      try {
         writer = new BufferedWriter(new  FileWriter(location));
         writer.write("# One tip per line, group 'all' will go to everybody");
         writer.newLine();
         writer.write("# Format: ");
         writer.newLine();
         writer.write("# group" + splitChar + "tip ");
         writer.newLine();
         for (int i=0; i < tips.size(); i++)
				tips.get(i).write(writer);
      } catch (Exception e) {
          log.log(Level.SEVERE, "Exception while creating " + location, e);
      } finally {
          try {
              if (writer != null) {
                  writer.close();
              }
          } catch (IOException e) {
          }
       }
   }

   private void loadTips()
	{
      tips = new ArrayList<TipGroup>();
      if (!new File(location).exists())
		{
          saveTips();
          return;
      }
      try {
			Scanner scanner = new Scanner(new File(location));
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				if (line.startsWith("#") || line.equals(""))
					continue;
				String[] split = line.split("\\" + splitChar);
				String group;
				String tip;
				if (split.length == 1)
				{
					group = "all";
					tip = split[0];
				}
				else if (split.length == 2)
				{
					group = split[0];
					tip = split[1];
				}
				else
				{
					log.log(Level.SEVERE, "Bad tip: " + line + " (" + split.length + ")");
					continue;
				}
				TipGroup tg = getTipGroup(group);
				if (tg == null)
				{
					tg = new TipGroup(group);
					tips.add(tg);
				}
				tg.add(tip);
			}
			scanner.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while reading " + location, e);
			stopTimer();
		}
   }

   public void enable()
	{
      PropertiesFile properties  = new PropertiesFile("server.properties");
      try {
         location = properties.getString("tip-location", "tips.txt");
         delay = properties.getInt("tip-delay", 120);
         prefix = properties.getString("tip-prefix",   "TIP: ");
         color = properties.getString("tip-color", MyColors.LightBlue);
      } catch (Exception e) {
         log.log(Level.SEVERE, "Exception while reading from server.properties", e);
      }
		if (prefix == null || prefix.length() == 0)
			prefix = "";
		else if (prefix.charAt(prefix.length()-1) != ' ')
			prefix = prefix + " ";
		if (color == null || color.length() == 0)
			color = MyColors.LightBlue;
      etc.getInstance().addCommand("/tip", " - display a random tip.");
      loadTips();
      startTimer();
		log.info(name + " v" + version + " Plugin Enabled.");
   }

   public void disable()
	{
      stopTimer();
      etc.getInstance().removeCommand("/tip");
		log.info(name + " v" + version + " Plugin Disabled.");
   }
	
	public void initialize()
	{
		new VersionCheck(name, version);

		TipsListener listener = new TipsListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.SERVERCOMMAND, listener, this, PluginListener.Priority.LOW);
	}
	
	public class TipsListener extends PluginListener
	{
		public boolean onConsoleCommand(String[] split)
		{
		   if (split[0].equalsIgnoreCase("reloadtips")) {
	         loadTips();
				log.info("Tips reloaded.");
	         return true;
	      }
			return false;
		}
	
	   public boolean onCommand(Player player, String[] split)
		{
			if (!split[0].equalsIgnoreCase("/tip"))
				return false;

			if (split.length == 1 || !player.canUseCommand("/tipadmin"))
			{
				String tip = "No tips.";
				for (int i=0; i < tips.size(); i++)
					if (tips.get(i).group.equals("all") || player.isInGroup(tips.get(i).group))
					{
						tip = tips.get(i).randTip();
						break;
					}
				if (tip != null)
				{
					displayTip(player, tip);
		         return true;
				}
				for (int i=0; i < tips.size(); i++)
					if (tips.get(i).group.equals("all"))
					{
						tip = tips.get(i).randTip();
						break;
					}
				displayTip(player, tip);
	         return true;
	      }

	      if (split[1].equalsIgnoreCase("add"))
			{
	         if (split.length < 4) {
	            player.sendMessage(Colors.Rose + "Usage: /tip add [group] [tip]");
	            return true;
	         }
				TipGroup tg = getTipGroup(split[2]);
				if (tg == null)
				{
					tg = new TipGroup(split[2]);
					tips.add(tg);
		         player.sendMessage(Colors.Rose + "Tip Group '" + split[2] + "' Added.");
				}
	         String line = splittoline(split, 3);
	         tg.tips.add(line);
	         saveTips();
	         player.sendMessage(Colors.Rose + "Tip Added.");
	         return true;
			}
			if (split[1].equalsIgnoreCase("del"))
			{
	         int tip = -1;
	         if (split.length < 4) {
	            player.sendMessage(Colors.Rose + "Usage: /tip del [group] [tip number]");
	            return true;
	         }
				TipGroup tg = getTipGroup(split[2]);
				if (tg == null)
				{
		         player.sendMessage(Colors.Rose + "Can't find Tip Group.");
					return true;
				}
	         try {
	            tip = Integer.parseInt(split[3]);
	         } catch (NumberFormatException ex) {
	            tip = -1;
	         }
	         if (tip < 0 || tip >= tg.tips.size()) {
	            player.sendMessage(Colors.Rose + "Invalid tip number.");
	            return true;
	         }
	         tg.tips.remove(tip);
	         player.sendMessage(Colors.Rose + "Tip Deleted.");
				if (tg.tips.isEmpty())
				{
		         player.sendMessage(Colors.Rose + "Tip Group '" + tg.group + "' Deleted.");
					tips.remove(tg);
				}
	         saveTips();
	         return true;
	      }
	      if (split[1].equalsIgnoreCase("groups"))
			{
	         player.sendMessage(Colors.Rose + "Tip Groups:");
				for (int i=0; i < tips.size(); i++)
					player.sendMessage(MyColors.codeToColor(color) + tips.get(i).group + " " + tips.get(i).tips.size());
				return true;
			}
	      if (split[1].equalsIgnoreCase("find"))
			{
	         if (split.length < 3) {
	            player.sendMessage(Colors.Rose + "Usage: /tip find [text to search]");
	            return true;
	         }
	         String line = splittoline(split, 2);
				for (int i=0; i < tips.size(); i++)
				{
		         int tipNum = tips.get(i).findTip(line);
		         if (tipNum == -1)
						continue;
					displayTip(player, tips.get(i).group, tipNum, tips.get(i).tips.get(tipNum));
					return true;
				}
				player.sendMessage(Colors.Rose  + "Not found.");
	         return true;
	      }
	      if (split[1].equalsIgnoreCase("b")) {
	         if (split.length < 4) {
	            player.sendMessage(Colors.Rose + "Usage: /tip b [group] [tip number]");
	            return true;
	         }
				TipGroup tg = getTipGroup(split[2]);
				if (tg == null)
				{
	            player.sendMessage(Colors.Rose + "Invalid tip group.");
	            return true;
				}
	         int tipNum = -1;
	         try {
	            tipNum = Integer.parseInt(split[3]);
	         } catch (NumberFormatException ex) {
	            tipNum = -1;
	         }
	         if (tipNum < 0 || tipNum >= tg.tips.size()) {
	            player.sendMessage(Colors.Rose + "Invalid tip number.");
	            return true;
	         }
            player.sendMessage(Colors.Rose + "Tip Broadcast.");
				broadcastTip(tg, tipNum);
	         return true;
	      }
	      if (split[1].equalsIgnoreCase("reload")) {
	         loadTips();
	         player.sendMessage(Colors.Rose + "Tips reloaded.");
	         return true;
	      }
			
			player.sendMessage(Colors.Rose + "Usage: /tip add [group] [text]");
			player.sendMessage(Colors.Rose + "Usage: /tip del [group] [tip num]");
			player.sendMessage(Colors.Rose + "Usage: /tip groups");
			player.sendMessage(Colors.Rose + "Usage: /tip find [text]");
			player.sendMessage(Colors.Rose + "Usage: /tip b [group] [tip num]");
			player.sendMessage(Colors.Rose + "Usage: /tip reload");
	      return true;
	   }
	}

	private class TipGroup
	{
		public String group;
		public ArrayList<String> tips;
		public int currentTip;
		
		TipGroup()
		{
			this.tips = new ArrayList<String>();
			this.group = "all";
			this.currentTip = 0;
		}
		
		TipGroup(String group)
		{
			this.tips = new ArrayList<String>();
			this.group = group;
			this.currentTip = 0;
		}
		
		public int findTip(String search)
		{
			for (int i = 0; i < this.tips.size(); i++)
				if (this.tips.get(i).contains(search))
					return i;
			return -1;
		}
		
		public String nextTip()
		{
			if (this.tips.isEmpty())
				return null;
			String t = this.tips.get(currentTip);
			currentTip++;
			if (currentTip >= tips.size())
				currentTip = 0;
			return t;
		}

		public String randTip()
		{
			if (generator == null)
				generator = new Random();
			int tipNum = generator.nextInt(this.tips.size());
			return this.tips.get(tipNum);
		}
		
		public void write(BufferedWriter writer) throws IOException
		{
			for (int i=0; i < tips.size(); i++)
			{
				writer.write(this.group + splitChar + this.tips.get(i));
				writer.newLine();
			}
		}
		
		public void add(String tip)
		{
			this.tips.add(tip);
		}
	}
}