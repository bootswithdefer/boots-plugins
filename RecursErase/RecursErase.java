import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

import net.minecraft.server.MinecraftServer;

public class RecursErase extends Plugin
{
	private static final String name = "RecursErase";
	private static final String command = "/recurserase";
	private static final int version = 1;
	static final Logger log = Logger.getLogger("Minecraft");
	
	private int toolID = 18;
	private int biteSize = 50;
	private int gapSize = 1;
	private LinkedBlockingQueue<Block> bqueue = new LinkedBlockingQueue<Block>();
	String eraser = null;
	
	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("server.properties");
		try {
			biteSize = properties.getInt("recurserase-bite-size", 50);
			toolID = properties.getInt("recurserase-tool-id", 18);
			gapSize = properties.getInt("recurserase-gap-size", 1);
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

		REListener listener = new REListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
	}
	
	public void start(Block block)
	{
		Block adj = block;
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
			for (int y = b.getY(); y <= b.getY()+gapSize; y++)
				for (int z = b.getZ()-gapSize; z <= b.getZ()+gapSize; z++)
				{
					if (x == b.getX() && y == b.getY() && z == b.getZ())
						continue;
					Block adj = etc.getServer().getBlockAt(x, y, z);
					if (adj.getType() == 0)
						continue;
					bqueue.offer(adj);
					adj.setType(0);
					etc.getServer().setBlock(adj);
					count++;
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

			stop();
			player.sendMessage(Colors.Rose + "Erasing stopped.");
			return true;
		}

		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
		{
			if (itemInHand == toolID && player.canUseCommand(command))
			{
				player.sendMessage(Colors.Rose + "Erasing started.");
				start(blockPlaced);
				eraser = player.getName();
				return true;
			}
			return false;
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
		}
	}
}