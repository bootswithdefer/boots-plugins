import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

public class Warn extends Plugin
{
	private String name = "Warn";
	private int version = 3;
	private int delay = 10;
	private boolean running = false;

	private HashMap<String, WarnData> warnings = new HashMap<String, WarnData>();

	static final Logger log = Logger.getLogger("Minecraft");

   private void startTimer() {
		running = true;
      final Timer timer = new Timer();
              timer.schedule(new TimerTask() {
                     @Override
                     public void run() {
               if (!running) {
                  timer.cancel();
                  return;
               }
               sendWarnings();
            }
         }, 3000, delay*1000);
   }

   private void stopTimer() {
		running = false;
   }

	private void sendWarnings()
	{
		for (String name: warnings.keySet())
		{
			Player p = etc.getServer().matchPlayer(name);
			if (p == null)
				continue;
			sendWarning(p, warnings.get(name));
		}
	}

	public void enable()
	{
		log.info(name + " Mod v" + version + " Enabled.");
		etc.getInstance().addCommand("/warn", " [player] [message] - warn a player about violating a rule.");
		etc.getInstance().addCommand("/unwarn", " [player] - removes a warning.");
		etc.getInstance().addCommand("/acknowledge", " - acknoledge a warning.");
		etc.getInstance().addCommand("/listwarnings", " - list warnings in effect.");
		startTimer();
	}

	public void disable()
	{
		warnings.clear();
		log.info(name + " Mod v" + version + " Disabled.");
		stopTimer();
	}

	public void initialize()
	{
		WarnListener listener = new WarnListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.HIGH);
	}
	
	private void sendWarning(Player p, WarnData warning)
	{
		if (warning == null)
			return;
		p.sendMessage(Colors.Rose + "Note: You cannot move until you /acknowledge this message:");
		p.sendMessage(Colors.Rose + warning.getMessage());
	}

	private void ackWarning(Player p)
	{
		WarnData w = warnings.get(p.getName());
		if (w == null) {
			p.sendMessage(Colors.Rose + "You are not being warned for anything.");
			return;
		}
		p.sendMessage(Colors.Rose + "You acknowledge the warning.");
		Player sender = etc.getServer().matchPlayer(w.getFrom());
		sender.sendMessage(Colors.Rose + p.getName() + " acknowledged your warning.");
		warnings.remove(p.getName());
	}

	public class WarnListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split)
		{
			if (split[0].equalsIgnoreCase("/acknowledge")) {
				ackWarning(player);
				return true;
			}

			if (!player.canUseCommand(split[0]))
				return false;
				
			if (split[0].equalsIgnoreCase("/warn")) {
				if (split.length < 3) {
					player.sendMessage(Colors.Rose + "Usage: /warn [player] [message]");
					return true;
				}
				Player target = etc.getServer().matchPlayer(split[1]);
				if (target == null) {
					player.sendMessage(Colors.Rose + "Player not found.");
					return true;
				}
				if (warnings.get(target.getName()) != null) {
					player.sendMessage(Colors.Rose + "That player is already being warned.");
					return true;
				}
				String message = split[2];
				for (int i = 3; i < split.length; i++)
					message = message + " " + split[i];
				
				WarnData warning = new WarnData(player.getName(), target.getName(), message);
				warnings.put(target.getName(), warning);
				sendWarning(target, warning);
				player.sendMessage("You warn " + target.getName() + ".");
				etc.getLoader().callCustomHook("set stat", new Object[] {player.getName(), "warn", "count", 1});
				return true;
			}
			if (split[0].equalsIgnoreCase("/unwarn")) {
				if (split.length < 2) {
					player.sendMessage(Colors.Rose + "Usage: /unwarn [player]");
					return true;
				}
				player.sendMessage(Colors.Rose + "Removed.");
				warnings.remove(split[1]);
				return true;
			}
			if (split[0].equalsIgnoreCase("/listwarnings")) {
				player.sendMessage(Colors.Rose + "Warnings:");
				for (String s: warnings.keySet()) {
					player.sendMessage(Colors.LightBlue + warnings.get(s).toString());
				}
				return true;
			}
			return false;
		}
		public void onPlayerMove(Player player, Location from, Location to)
		{
			if (warnings.get(player.getName()) != null) {
				player.teleportTo(from);
			}
		}
	}
}