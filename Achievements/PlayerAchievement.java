import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class PlayerAchievement
{
	protected String name;
	protected HashMap<String, Achievement> achievements;
	static final Logger log	= Logger.getLogger("Minecraft");

	PlayerAchievement(String name)
	{
		this.name = name;
		this.achievements = new HashMap<String, Achievement>();
	}

	protected boolean isEmpty()
	{
		return achievements.isEmpty();
	}

	protected Achievement get(String name)
	{
		return achievements.get(name);
	}

	protected Achievement newAchievement(String name)
	{
		Achievement achievement = new Achievement();
		achievements.put(name, achievement);
		return achievement;
	}

	protected void award(String achievement)
	{
		Achievement ach;
		if (!achievements.containsKey(achievement))
			ach = newAchievement(achievement);
		else
			ach = achievements.get(achievement);
			
		ach.incrementCount();
	}

	protected void put(String achievement, int count)
	{
		Achievement ach;
		if (!achievements.containsKey(achievement))
			ach = newAchievement(achievement);
		else
			ach = achievements.get(achievement);
			
		ach.put(count);
	}

	protected boolean hasAchievement(AchievementListData ach)
	{
		if (achievements.containsKey(ach.getName()))
			return true;
		return false;
	}

	protected void copy(PlayerAchievement from)
	{
		this.name = from.name;
		this.achievements = new HashMap<String, Achievement>(from.achievements);
	}


	Iterator<String> iterator()
	{
		return achievements.keySet().iterator();
	}

	protected abstract void save();
	protected abstract void load();
}