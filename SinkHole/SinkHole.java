import java.util.concurrent.LinkedBlockingQueue;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.logging.*;

import net.minecraft.server.MinecraftServer;

public class SinkHole extends Plugin
{
	private static final String name = "SinkHole";
	private static final String command = "/shole";
	private static final int version = 1;
	private static final int startY = 52;
	static final Logger log = Logger.getLogger("Minecraft");

	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("server.properties");
		try {
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while reading from server.properties", ex);
		}		

		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		new VersionCheck(name, version);

		SHListener listener = new SHListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
	}

	public class SHListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split)
		{
			if (!split[0].equalsIgnoreCase(command) || !player.canUseCommand(command))
				return false;

			if (split.length < 2)
			{
				player.sendMessage(Colors.Rose + "Usage: " + command + " [blocks]");
				return true;
			}

			int num;
			try {
				num = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				player.sendMessage(Colors.Rose + "Usage: " + command + " [blocks]");
				return true;
			}

			Preparer pq = new Preparer();
			
			for (int x =  (int)(player.getX()) - num; x < (int)(player.getX()) + num; x++)
				for (int z = (int)(player.getZ()) - num; z < (int)(player.getZ()) + num; z++)
				{
					Block b = etc.getServer().getBlockAt(x, 0, z);
					pq.add(b);
				}

			player.sendMessage(Colors.Rose + "Sinking...");
			etc.getServer().addToServerQueue(pq, 100L);
			
			return true;
		}
	}

	private class Preparer implements Runnable
	{
		private LinkedBlockingQueue<Block> bqueue = new LinkedBlockingQueue<Block>();
		private Mover mover;

		Preparer()
		{
			mover = new Mover();
		}

		public void add(Block b)
		{
			bqueue.offer(b);
		}

		public void run()
		{
			if (bqueue.isEmpty())
			{
				etc.getServer().addToServerQueue(mover, 1000L);
				return;
			}
				
			int count = 0;
			int x, z, type;
			
			while (count < 5000)
			{
				Block b = bqueue.poll();
				if (b == null)
					break;
		
				x = b.getX();
				z = b.getZ();
				
				if (etc.getServer().getBlockIdAt(x, 5, z) == 7)
					continue;
	
				count++;
				
				mover.add(b);
				etc.getServer().setBlockAt(7, x, 0, z);
				for (int y = 1; y < startY; y++)
					etc.getServer().setBlockAt(1, x, y, z);
					
				for (int y = startY; y <= 128; y++)
					if (etc.getServer().getBlockIdAt(x, y, z) == 8 || etc.getServer().getBlockIdAt(x, y, z) == 9)
						etc.getServer().setBlockAt(7, x, y, z);
			}
			
			if (bqueue.isEmpty())
				etc.getServer().addToServerQueue(mover, 100L);
			else
				etc.getServer().addToServerQueue(this, 1000L);
		}
	}

	private class Mover implements Runnable
	{
		private LinkedBlockingQueue<Block> bqueue = new LinkedBlockingQueue<Block>();

		public void add(Block b)
		{
			bqueue.offer(b);
		}

		public void run()
		{
			if (bqueue.isEmpty())
				return;
				
			int count = 0;
			int x, z, type;
			
			while (count < 5000)
			{
				Block b = bqueue.poll();
				if (b == null)
					return;
		
				x = b.getX();
				z = b.getZ();
				
				count++;
				
				for (int y = startY+1; y <= 128; y++)
				{
					type = etc.getServer().getBlockIdAt(x, y, z);
					etc.getServer().setBlockAt(type, x, y-startY, z);
					etc.getServer().setBlockAt(0, x, y, z);
				}
				etc.getServer().setBlockAt(0, x, 1, z);
				etc.getServer().setBlockAt(0, x, 2, z);
				etc.getServer().setBlockAt(0, x, 3, z);
				etc.getServer().setBlockAt(0, x, 4, z);
				etc.getServer().setBlockAt(7, x, 5, z);
				
				for (int y = 6; y <= 128 - startY; y++)
					if (etc.getServer().getBlockIdAt(x, y, z) == 7)
						etc.getServer().setBlockAt(9, x, y, z);
			}
			
			if (bqueue.isEmpty())
				return;
			
			etc.getServer().addToServerQueue(this, 1000L);
		}
	}
}