
public class AchievementListData
{
	private boolean enabled;
	private String name;
	private String category;
	private String key;
	private int value;
	private String description;
	private int maxawards;
	
	AchievementListData(int enabled, String name, int maxawards, String category, String key, int value, String description)
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
		int en = 0;
	   if (isEnabled())
			en = 1;
		return en + ":" + name + ":" + maxawards + ":" + category + ":" + key + ":" + value + ":" + description;
	}
}