import java.util.HashMap;
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

public class CategoryMap
{
	private HashMap<String, StatMap> stats = new HashMap<String, StatMap>();
	private boolean modified = false;
	static final Logger log	= Logger.getLogger("Minecraft");

	public void put(String category, String key, Integer val)
	{
		if (!this.stats.containsKey(category))
		{
			StatMap bstats = new StatMap();
			this.stats.put(category, bstats);
		}
		this.stats.get(category).put(key, val);
		modified = true;
	}
	
	public int get(String category, String key)
	{
		if (!this.stats.containsKey(category))
			return -1;
		return this.stats.get(category).get(key);
	}

	public void save(String location)
	{
		BufferedWriter writer = null;
		try {
			if (!modified)
				return;
			modified = false;
//			log.info("Saving " + location);
			writer = new BufferedWriter(new	FileWriter(location));
			writer.write("# " + location);
			writer.newLine();
			for (String category: stats.keySet())
			{
				StatMap istat = stats.get(category);
				for (String val: istat.toArray())
				{
					String line = category + ":" + val;
					writer.write(line);
					writer.newLine();
				}
			}
		} catch (Exception e) {
			 log.log(Level.SEVERE, "Exception while creating "	+ location,	e);
		} finally {
			 try {
				  if (writer != null) {
						writer.close();
				  }
			 }	catch	(IOException e) {
			 }
		}
	}
	
	public void load(String location)
	{
		if (!new File(location).exists())
			return;
		try {
			Scanner scanner =	new Scanner(new File(location));
			while	(scanner.hasNextLine())
			{
				String line =	scanner.nextLine();
				if (line.startsWith("#") || line.equals(""))
					continue;
				String[] split = line.split(":");
				if (split.length != 3) {
					log.log(Level.SEVERE, "Malformed line (" + line + ") in " + location);
				 	continue;
				}
				String category = split[0];
				String key;
				if (category.equals("blockcreate") || category.equals("blockdestroy"))
					key = split[1];
				else
					key = split[1];
				int val = Integer.parseInt(split[2]);
				this.put(category, key, val);
			}
			scanner.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception	while	reading " +	location, e);
		}
	}
}
