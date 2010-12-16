import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PlayerStatFile extends PlayerStat
{
	private String directory;
	static final Logger log	= Logger.getLogger("Minecraft");

	PlayerStatFile(String name, String directory)
	{
		super(name);
		this.directory = directory;
	}

	protected void save()
	{
		String location = directory + "/" + name + ".txt";
		BufferedWriter writer = null;
		try {
//			log.info("Saving " + location);
			writer = new BufferedWriter(new FileWriter(location));
			writer.write("# " + location);
			writer.newLine();
			
//			log.info("categories size " + categories.size());
			
			Iterator<String> catIter = categories.keySet().iterator();
			while (catIter.hasNext())
			{
				String catName = catIter.next();
//				log.info(catName);
				Category cat = categories.get(catName);
				Iterator<String> statIter = cat.iterator();
				while (statIter.hasNext())
				{
					String statName = statIter.next();
					String line = catName + ":" + statName + ":" + cat.get(statName);
					writer.write(line);
					writer.newLine();
				}
			}
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Exception while creating " + location, ex);
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException ex) {
				log.log(Level.SEVERE, "Exception while closing " + location, ex);
			}
		}
	}
	
	protected void load()
	{
		String location = directory + "/" + name + ".txt";

		if (!new File(location).exists())
		{
			log.log(Level.SEVERE, "File does not exist " + location);
			return;
		}
		
//		log.info("Loading " + location);
		
		try
		{
			Scanner scanner = new Scanner(new File(location));
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				if (line.startsWith("#") || line.equals(""))
					continue;
				String[] split = line.split(":");
				if (split.length != 3) {
					log.log(Level.SEVERE, "Malformed line (" + line + ") in " + location);
					continue;
				}
				String category = split[0];
				String key = split[1];
				Integer val = Integer.parseInt(split[2]);
				
				put(category, key, val);
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while reading " + location, ex);
		}
	}
}