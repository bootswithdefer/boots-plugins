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
	private int version = 8;
   private String location = "tips.txt";
   private String  color = MyColors.LightBlue;
   private String  prefix = "TIP: ";
   private boolean stopTimer = false;
   private int delay = 120;
   private ArrayList<String> tips;
   private int currentTip = 0;
	private Random generator = null;

   static final Logger log = Logger.getLogger("Minecraft");

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

   private int findtip(String search) {
      for (int i=0; i<tips.size(); i++) {
         if (tips.get(i).contains(search))
            return i;
      }
      return -1;
   }

   public void broadcastTip()
	{
		broadcastTip(currentTip);
      if (currentTip >= tips.size())
          currentTip = 0;
		else
      	currentTip++;
   }


   public void broadcastTip(int tipnum)
	{
		if (tips.isEmpty())
			return;
		if (tipnum < 0 || tipnum >= tips.size())
			return;
		for (String tip : tips.get(tipnum).split("@"))
		{
	      String message = MyColors.codeToColor(color) + prefix + tip;
	      for (Player p : etc.getServer().getPlayerList()) {
	         if (p != null /*&& !p.canUseCommand("/notips")*/) {
	            p.sendMessage(message);
	         }
	      }
		}
   }

   private void saveTips()
   {
      BufferedWriter writer = null;
      try {
         writer = new BufferedWriter(new  FileWriter(location));
         writer.write("# One tip per line");
         writer.newLine();
         for (String s: tips) {
            writer.write(s);
            writer.newLine();
         }
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

   private void loadTips() {
      tips = new ArrayList<String>();
      if (!new File(location).exists()) {
          saveTips();
          return;
      }
           try {
               Scanner scanner = new Scanner(new File(location));
               while (scanner.hasNextLine()) {
                   String line = scanner.nextLine();
                   if (line.startsWith("#") || line.equals(""))
                       continue;
					    tips.add(line);
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

			if (split.length == 1 || !player.canUseCommand("/tipadmin")) {
				if (tips.size() == 0) {
					player.sendMessage(MyColors.codeToColor(color) + "No tips.");
					return true;
				}
				if (generator == null)
					generator = new Random();
				int tipNum = generator.nextInt(tips.size());
				for (String tip: tips.get(tipNum).split("@"))
		         player.sendMessage(MyColors.codeToColor(color) + prefix + tip);
	         return true;
	      }

	      if (split[1].equalsIgnoreCase("add")) {
	         if (split.length < 3) {
	            player.sendMessage(Colors.Rose + "Usage: /tip add [tip]");
	            return true;
	         }
	         String line = splittoline(split, 2);
	         tips.add(line);
	         saveTips();
	         player.sendMessage(Colors.Rose + "Added.");
	         return true;
			}
			if (split[1].equalsIgnoreCase("del")) {
	         int tip = -1;
	         if (split.length < 3) {
	            player.sendMessage(Colors.Rose + "Usage: /tip del [tip number]");
	            return true;
	         }
	         try {
	            tip = Integer.parseInt(split[2]);
	         } catch (NumberFormatException ex) {
	            tip = -1;
	         }
	         if (tip < 0 || tip >= tips.size()) {
	            player.sendMessage(Colors.Rose + "Invalid tip number.");
	            return true;
	         }
	         tips.remove(tip);
	         saveTips();
	         player.sendMessage(Colors.Rose + "Deleted.");
	         return true;
	      }
	      if (split[1].equalsIgnoreCase("find")) {
	         if (split.length < 3) {
	            player.sendMessage(Colors.Rose + "Usage: /tip find [text to search]");
	            return true;
	         }
	         String line = splittoline(split, 2);
	         int tipNum = findtip(line);
	         if (tipNum == -1) {
	            player.sendMessage(Colors.Rose + "Not tip found containing '" + line + "'.");
	            return true;
	         }
				for (String tip: tips.get(tipNum).split("@"))
		         player.sendMessage(MyColors.codeToColor(color) + prefix + tipNum + ": " + tip);
	         return true;
	      }
	      if (split[1].equalsIgnoreCase("b")) {
	         int tip = -1;
	         if (split.length < 3) {
	            player.sendMessage(Colors.Rose + "Usage: /tip b [tip number]");
	            return true;
	         }
	         try {
	            tip = Integer.parseInt(split[2]);
	         } catch (NumberFormatException ex) {
	            tip = -1;
	         }
	         if (tip < 0 || tip >= tips.size()) {
	            player.sendMessage(Colors.Rose + "Invalid tip number.");
	            return true;
	         }
				broadcastTip(tip);
	         return true;
	      }
	      if (split[1].equalsIgnoreCase("reload")) {
	         loadTips();
	         player.sendMessage(Colors.Rose + "Tips reloaded.");
	         return true;
	      }
			
			player.sendMessage(Colors.Rose + "Usage: /tip add [text]");
			player.sendMessage(Colors.Rose + "Usage: /tip del [tip num]");
			player.sendMessage(Colors.Rose + "Usage: /tip find [text]");
			player.sendMessage(Colors.Rose + "Usage: /tip b [tip num]");
			player.sendMessage(Colors.Rose + "Usage: /tip reload");
	      return true;
	   }
	}
}
