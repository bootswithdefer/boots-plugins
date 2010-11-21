import java.util.HashMap;
import java.util.logging.Logger;

public class PlayerMap
{
	private HashMap<String, CategoryMap> stats = new HashMap<String, CategoryMap>();
	static final Logger log	= Logger.getLogger("Minecraft");

	public int size()
	{
		return this.stats.size();
	}

	public void put(String player, String category, String key, Integer val)
	{
		if (!this.stats.containsKey(player))
		{
			CategoryMap cstats = new CategoryMap();
			this.stats.put(player, cstats);
		}
		this.stats.get(player).put(category, key, val);
	}
	
	public int get(String player, String category, String key)
	{
		if (!this.stats.containsKey(player))
			return -1;
		return this.stats.get(player).get(category, key);
	}
	
	public void save(String directory, String player)
	{
		if (directory.isEmpty() || player.isEmpty())
			return;
		if (!this.stats.containsKey(player))
			return;

		String location = directory + "/" + player + ".txt";
		this.stats.get(player).save(location);
	}
	
	private void XsaveAll(String directory)
	{
		String location;
		if (directory.isEmpty())
			return;
		if (this.stats.size() == 0)
			return;
//		log.info("Saving " + this.stats.size() + " stat files...");
		for (String player: this.stats.keySet())
			save(directory, player);
	}
	
	public void load(String directory, String player)
	{
		if (directory.isEmpty() || player.isEmpty())
			return;
			
		if (this.stats.containsKey(player))
			return;
		
		String location = directory + "/" + player + ".txt";
//		log.info("Loading " + location);
		
		CategoryMap cstats = new CategoryMap();
		cstats.load(location);
		
		this.stats.put(player, cstats);
	}
	
	public void unload(String directory, String player)
	{
//		log.info("Unloading " + player);
		save(directory, player);
		this.stats.remove(player);
	}
}
