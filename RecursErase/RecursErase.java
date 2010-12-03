import java.util.concurrent.LinkedBlockingQueue;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.logging.*;

import net.minecraft.server.MinecraftServer;

public class RecursErase extends Plugin
{
	private static final String name = "RecursErase";
	private static final String command = "/reer";
	private static final int version = 3;
	static final Logger log = Logger.getLogger("Minecraft");

	private ArrayList<Integer> naturalBlocks = new ArrayList<Integer>(Arrays.asList(new Integer[] {1, 2, 3, 12, 13, 17, 18}));
	
	private int toolID = 83;
	private int biteSize = 50;
	private int gapSize = 1;
	private LinkedBlockingQueue<Block> bqueue = new LinkedBlockingQueue<Block>();
	String eraser = null;
	private boolean nonat = false;
	
	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("server.properties");
		try {
			biteSize = properties.getInt("recurserase-bite-size", 50);
			toolID = properties.getInt("recurserase-tool-id", 83);
			gapSize = properties.getInt("recurserase-gap-size", 1);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while reading from server.properties", ex);
		}		

		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		stop();
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		new VersionCheck(name, version);

		REListener listener = new REListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, PluginListener.Priority.MEDIUM);
	}
	
	public void start(Block block)
	{
		bqueue.offer(block);
	}
	
	public void stop()
	{
		bqueue.clear();
		eraser = null;
	}
	
	public void run(int count)
	{
		if (bqueue.isEmpty())
			return;
			
		Block b = bqueue.poll();
		if (b == null)
			return;
		
		for (int x = b.getX()-gapSize; x <= b.getX()+gapSize; x++)
		{
			for (int y = b.getY(); y <= b.getY()+gapSize; y++)
			{
				for (int z = b.getZ()-gapSize; z <= b.getZ()+gapSize; z++)
				{
					if (x == b.getX() && y == b.getY() && z == b.getZ())
						continue;
					Block adj = etc.getServer().getBlockAt(x, y, z);
					if (adj.getType() == 0 || (nonat && naturalBlocks.contains(adj.getType())))
						continue;
					bqueue.offer(adj);
					adj.setType(0);
					etc.getServer().setBlock(adj);
					count++;
				}
			}
		}
		b.setType(0);
		etc.getServer().setBlock(b);
		if (count < biteSize)
			run(count);
	}

	public class REListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split)
		{
			if (!split[0].equalsIgnoreCase(command) || !player.canUseCommand(command))
				return false;

			if (split.length < 2)
			{
				stop();
				player.sendMessage(Colors.Rose + "Erasing stopped.");
				return true;
			}
			
			if (split[1].equalsIgnoreCase("nonat"))
			{
				if (!nonat)
				{
					nonat = true;
					player.sendMessage(Colors.Rose + "Set to ignore 'natural blocks'.");
				}
				else
				{
					nonat = false;
					player.sendMessage(Colors.Rose + "Set to include 'natural blocks'.");
				}
				return true;
			}
			
			player.sendMessage(Colors.Rose + "Usage: " + command);
			return true;
		}

		public void onBlockRightClicked(Player player, Block blockClicked, Item item)
		{
			if (item.getItemId() != toolID)
				return;
			if (!player.canUseCommand(command))
				return;
			if (eraser != null && !player.getName().equals(eraser))
			{
				player.sendMessage(Colors.Rose + eraser + " is erasing something, wait until they're done.");
				return;
			}
			player.sendMessage(Colors.Rose + "Erasing started.");
			start(blockClicked);
			eraser = player.getName();
		}

		public void onArmSwing(Player player)
		{
			if (eraser == null)
				return;
			if (!player.getName().equals(eraser))
				return;
			if (bqueue.isEmpty())
				return;
			player.sendMessage(Colors.Rose + "Erasing (" + bqueue.size() + " left)...");
			run(0);
			if (bqueue.isEmpty())
			{
				player.sendMessage(Colors.Rose + "Done.");
				eraser = null;
			}
		}
	}
}