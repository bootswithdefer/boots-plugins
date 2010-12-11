import java.util.HashSet;
import java.util.HashMap;

import java.util.logging.*;
import java.sql.*;

public class AreaStats implements Runnable
{
	static final Logger log = Logger.getLogger("Minecraft");
	private Player player;
	private int size;
	private Connection conn = null;
	
	AreaStats(Connection conn, Player player, int size)
	{
		this.player = player;
		this.size = size;
		this.conn = conn;
	}
	public void run()
	{
		HashSet<String> players = new HashSet<String>();
		HashMap<String, Integer> created = new HashMap<String, Integer>();
		HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT player, count(player) as num from blocks where type > 0 and y > ? and y < ? and x > ? and x < ? and z > ? and z < ? group by player order by count(player) desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, (int)player.getY()-size);
			ps.setInt(2, (int)player.getY()+size);
			ps.setInt(3, (int)player.getX()-size);
			ps.setInt(4, (int)player.getX()+size);
			ps.setInt(5, (int)player.getZ()-size);
			ps.setInt(6, (int)player.getZ()+size);
			rs = ps.executeQuery();
			while (rs.next())
			{
				players.add(rs.getString("player"));
				created.put(rs.getString("player"), rs.getInt("num"));
			}
			rs.close();
			ps.close();
			
			ps = conn.prepareStatement("SELECT player, count(player) as num from blocks where replaced > 0 and y > ? and y < ? and x > ? and x < ? and z > ? and z < ? group by player order by count(player) desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, (int)player.getY()-size);
			ps.setInt(2, (int)player.getY()+size);
			ps.setInt(3, (int)player.getX()-size);
			ps.setInt(4, (int)player.getX()+size);
			ps.setInt(5, (int)player.getZ()-size);
			ps.setInt(6, (int)player.getZ()+size);
			rs = ps.executeQuery();
			while (rs.next())
			{
				players.add(rs.getString("player"));
				destroyed.put(rs.getString("player"), rs.getInt("num"));
			}
			
		} catch (SQLException ex) {
			log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
			}
		}

		player.sendMessage(Colors.Blue + "Within " + size + " blocks of you: ");
		if (players.size() == 0)
		{
			player.sendMessage(Colors.Blue + "No results found.");
			return;
		}
		
		player.sendMessage(Colors.Gold + String.format("%-6s %-6s %s", "Creat", "Destr", "Player"));
		for (String p: players)
		{
			Integer c = created.get(p);
			Integer d = destroyed.get(p);
			if (c == null)
				c = 0;
			if (d == null)
				d = 0;
			player.sendMessage(Colors.Gold + String.format("%-6d %-6d %s", c, d, p));
		}
	}
}
