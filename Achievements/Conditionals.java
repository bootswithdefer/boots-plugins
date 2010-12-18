import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Conditionals
{
	private boolean empty;
	private ArrayList<String> condList = new ArrayList<String>();

   static final Logger log = Logger.getLogger("Minecraft");

	Conditionals(String conditions)
	{
		this.empty = true;
		if (conditions == null)
			return;

		String[] split = conditions.split(";");
		for (String c: split)
		{
			if (c.length() <= 1)
				continue;
//			log.info(c);
			this.condList.add(c);
			this.empty = false;
		}
	}

	public boolean isEmpty()
	{
		return this.empty;
	}

	public boolean meets(Achievements plugin, PlayerAchievement pa)
	{
		if (isEmpty())
			return true;

		boolean meets = false;
					
		for (String c: condList)
		{
			String cond;
			boolean not = false;
			if (c.startsWith("!"))
			{
				not = true;
				cond = c.substring(1);
			}
			else
			{
				cond = c;
			}

			AchievementListData ach = plugin.getAchievement(cond);
			if (ach == null)
				continue;
				
			if (!not && pa.hasAchievement(ach))
			{
				meets = true;
				continue;
			}
			if (not && !pa.hasAchievement(ach))
			{
				meets = true;
				continue;
			}

			return false;
		}

		return meets;
	}

	public String toString()
	{
		boolean second = false;
		
		if (isEmpty())
			return "";
	
		String ret = "";
		for (String s: condList)
		{
			if (second)
				ret = ret + ";";
			ret = ret + s;
			second = true;
		}
		return ret;
	}
}