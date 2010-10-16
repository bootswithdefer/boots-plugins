import java.util.HashMap;
import java.util.ArrayList;

public class StatMap
{
	private HashMap<String, Integer> stats = new HashMap<String, Integer>();
	
	public void put(String key, Integer val)
	{
		this.stats.put(key, val);
	}
	
	public int get(String key)
	{
		if (!this.stats.containsKey(key))
			return -1;
		return this.stats.get(key);
	}
	
	public ArrayList<String> toArray()
	{
		ArrayList<String> lines = new ArrayList<String>();
		for (String key: stats.keySet())
		{
			String line = key + ":" + stats.get(key);
			lines.add(line);
		}
		return lines;
	}
}
