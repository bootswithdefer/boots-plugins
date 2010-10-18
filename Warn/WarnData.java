

public class WarnData
{
	private String from;
	private String to;
	private String message;
	private boolean acknowledged;
	
	WarnData(String from, String to, String message)
	{
		this.from = from;
		this.to = to;
		this.message = message;
		this.acknowledged = false;
	}
	
	public String getFrom()
	{
		return this.from;
	}
	
	public String getMessage()
	{
		return this.message;
	}
	
	public String toString()
	{
		return from + ":" + to + ":" + message + ":" + acknowledged;
	}
}