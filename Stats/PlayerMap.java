import java.util.HashMap;

public class PlayerMap
{
	private HashMap<String, CategoryMap> stats = new HashMap<String, CategoryMap>();
	
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
		String location = directory + "/" + player + ".txt";
		if (stats.containsKey(player))
			stats.get(player).save(location);
	}
	
	public void saveAll(String directory)
	{
		String location;
		if (directory.isEmpty())
			return;
		
		for (String player: stats.keySet())
		{
			location = directory + "/" + player + ".txt";
			stats.get(player).save(location);
		}
	}
	
	public void load(String directory, String player)
	{
		if (directory.isEmpty() || player.isEmpty())
			return;
			
		if (stats.containsKey(player))
			return;
			
		String location = directory + "/" + player + ".txt";
		
		CategoryMap cstats = new CategoryMap();
		this.stats.put(player, cstats);
		
		stats.get(player).load(location);
	}
	
	public void unload(String directory, String player)
	{
		save(directory, player);
		stats.remove(player);
	}
}
