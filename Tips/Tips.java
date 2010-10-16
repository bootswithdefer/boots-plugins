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
	private int version = 3;
   private String location = "tips.txt";
   private String  color = MyColors.LightBlue;
   private String  prefix = "TIP";
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

   private String splittoline(String[] split) {
      String line = "";
      for (int i=1; i<split.length; i++) {
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

   public void broadcastTip() {
		if (tips.isEmpty())
			return;
      if (currentTip >= tips.size())
          currentTip = 0;
      String message = MyColors.codeToColor(color) + prefix + ": " + tips.get(currentTip);
      for (Player p : etc.getServer().getPlayerList()) {
         if (p != null /*&& !p.canUseCommand("/notips")*/) {
            p.sendMessage(message);
         }
      }
      currentTip++;
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
         prefix = properties.getString("tip-prefix",   "TIP");
         color = properties.getString("tip-color", MyColors.LightBlue);
      } catch (Exception e) {
         log.log(Level.SEVERE, "Exception while reading from server.properties", e);
      }
      etc.getInstance().addCommand("/tip", " - display a random tip.");
      etc.getInstance().addCommand("/addtip", " [tip] - Add a tip.");
      etc.getInstance().addCommand("/deltip", " [tipnum] - Delete a tip.");
      etc.getInstance().addCommand("/findtip", " - Delete a tip.");
      etc.getInstance().addCommand("/reloadtips", " - Reload tips from file.");
      loadTips();
      startTimer();
      log.info(name + " v" + version + " Mod Enabled.");
   }

   public void disable()
	{
      stopTimer();
   }
	
	public void initialize()
	{
		TipsListener listener = new TipsListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
	}
	
	public class TipsListener extends PluginListener
	{
	   public boolean onCommand(Player player, String[] split)
		{
	      if (split[0].equalsIgnoreCase("/addtip")) {
	         if (split.length < 2) {
	            player.sendMessage(Colors.Rose + "Usage: /addtip [tip]");
	            return true;
	         }
	         String line = splittoline(split);
	         tips.add(line);
	         saveTips();
	         player.sendMessage(Colors.Rose + "Added.");
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/deltip")) {
	         int tip = -1;
	         if (split.length < 2) {
	            player.sendMessage(Colors.Rose + "Usage: /deltip [tip number]");
	            return true;
	         }
	         try {
	            tip = Integer.parseInt(split[1]);
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
	      else if (split[0].equalsIgnoreCase("/findtip")) {
	         if (split.length < 2) {
	            player.sendMessage(Colors.Rose + "Usage: /findtip [text to search]");
	            return true;
	         }
	         String line = splittoline(split);
	         int tip = findtip(line);
	         if (tip == -1) {
	            player.sendMessage(Colors.Rose + "Not tip found containing '" + line + "'.");
	            return true;
	         }
	         player.sendMessage(MyColors.codeToColor(color) + prefix + " " + tip + ": " + tips.get(tip));
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/reloadtips")) {
	         loadTips();
	         player.sendMessage(Colors.Rose + "Tips reloaded.");
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/tip")) {
				if (generator == null)
					generator = new Random();
				int tipNum = generator.nextInt(tips.size());
	         player.sendMessage(MyColors.codeToColor(color) + prefix + ": " + tips.get(tipNum));
	         return true;
	      }
	      return false;
	   }
	}
}
