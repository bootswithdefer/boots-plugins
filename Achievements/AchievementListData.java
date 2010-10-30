
public class AchievementListData
{
	private boolean enabled;
	private String name;
	private String category;
	private String key;
	private int value;
	private String description;
	private int maxawards;
	public CommandHandler commands;
	public Conditionals conditions;
	
	AchievementListData(int enabled, String name, int maxawards, String category, String key, int value, String description, String commands, String conditionals)
	{
		if (enabled > 0)
			this.enabled = true;
		else
			this.enabled = false;
		this.name = name;
		this.category = category;
		this.key = key;
		this.value = value;
		this.description = description;
		this.maxawards = maxawards;
		this.commands = new CommandHandler(commands);
		this.conditions = new Conditionals(conditionals);
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String getCategory()
	{
		return this.category;
	}
	
	public String getKey()
	{
		return this.key;
	}
	
	public int getValue()
	{
		return this.value;
	}
	
	public String getDescription()
	{
		return this.description;
	}
	
	public int getMaxawards()
	{
		return this.maxawards;
	}
	
	public boolean isEnabled()
	{
		return this.enabled;
	}
	
	public String toString()
	{
		String ret;
		int en = 0;
	   if (isEnabled())
			en = 1;
		ret = en + ":" + name + ":" + maxawards + ":" + category + ":" + key + ":" + value + ":" + description;
		ret = ret + ":" + commands.toString() + ":" + conditions.toString();
		return ret;
	}
}