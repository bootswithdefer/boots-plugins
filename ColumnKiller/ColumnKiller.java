import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

public class ColumnKiller extends Plugin
{
	private String name = "ColumnKiller";
	private int version = 1;
	private int history = 6;
	
	private HashMap<String, ArrayList<Block>> blocks = new HashMap<String, ArrayList<Block>>();
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	public void enable()
	{
		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		new VersionCheck(name, version);

		CKListener listener = new CKListener();
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
	}
	
	private boolean blockInColumn(Block a, Block b)
	{
		if (a.getX() != b.getX())
			return false;
			
		if (a.getZ() != b.getZ())
			return false;

		if (a.getY() < 64 || b.getY() < 64)
			return false;

		if (b.getY() >  a.getY() && b.getY() < (a.getY() + 5))
			return true;
			
		return false;
	}
	
	private boolean hasAdjacentBlocks(Block b)
	{
		int x = b.getX();
		int y = b.getY();
		int z = b.getZ();
		
		if (etc.getServer().getBlockIdAt(x,y,z-1) != 0)
			return true;
		if (etc.getServer().getBlockIdAt(x,y,z+1) != 0)
			return true;
		if (etc.getServer().getBlockIdAt(x-1,y,z) != 0)
			return true;
		if (etc.getServer().getBlockIdAt(x+1,y,z) != 0)
			return true;
		
		return false;
	}
	
	public class CKListener extends PluginListener
	{
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
		{
			if (hasAdjacentBlocks(blockPlaced))
				return false;

			if (!blocks.containsKey(player.getName()))
				blocks.put(player.getName(), new ArrayList<Block>());
				
			ArrayList<Block> myblocks = blocks.get(player.getName());
			
			if (myblocks.size() > 0)
			{
				int count = 0;
				
				for (Block b: myblocks)
					if (blockInColumn(b, blockPlaced))
						if (!hasAdjacentBlocks(b))
							count++;

				if (count > 4)
				{
					log.info(name + " blocked " + player.getName() + " x=" + blockPlaced.getX() + " z=" + blockPlaced.getZ() + " y=" + blockPlaced.getY());
					player.sendMessage(Colors.Rose + "Don't build 1x1 towers!");
					return true;
				}
			}
			
			myblocks.add(blockPlaced);
			
			if (myblocks.size() > history)
				myblocks.remove(0);
	
			return false;
		}
	}
}
