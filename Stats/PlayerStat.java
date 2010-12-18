import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class PlayerStat
{
	protected String name;
	protected HashMap<String, Category> categories;
	static final Logger log	= Logger.getLogger("Minecraft");

	PlayerStat(String name)
	{
		this.name = name;
		this.categories = new HashMap<String, Category>();
	}

	protected Category get(String name)
	{
		return categories.get(name);
	}

	protected Category newCategory(String name)
	{
		Category category = new Category();
		categories.put(name, category);
		return category;
	}

	protected void put(String category, String key, int val)
	{
		Category cat;
		if (!categories.containsKey(category))
			cat = newCategory(category);
		else
			cat = categories.get(category);
			
		cat.put(key, val);
	}

	protected void copy(PlayerStat from)
	{
		this.name = from.name;
		this.categories = new HashMap<String, Category>(from.categories);
	}

	public void convertFlatFile(String directory)
	{
		PlayerStat psold = new PlayerStatFile(name, directory);
		psold.load();
		copy(psold);
		save();

		String location = directory + "/" + name + ".txt";
		File fold = new File(location);
		File fnew = new File(location + ".old");
		fold.renameTo(fnew);
	}

	protected abstract void save();
	protected abstract void load();
}