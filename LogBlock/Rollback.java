import java.util.concurrent.LinkedBlockingQueue;

import java.util.logging.*;
import java.sql.*;

public class Rollback implements Runnable
{
	static final Logger log = Logger.getLogger("Minecraft");
	private LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();

	Rollback(Connection conn, String name, int minutes)
	{
		String query = "select type, replaced, x, y, z from blocks where player = ? and date > date_sub(now(), interval ? minute) order by date desc";
		PreparedStatement ps = null;
		ResultSet rs = null;
		edits.clear();
		
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, minutes);
			rs = ps.executeQuery();

			while (rs.next())
			{
				Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
				edits.offer(e);
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
		
	}
	
	public int count()
	{
		return edits.size();
	}
	
	public void run()
	{
		Edit e = edits.poll();

		while (e != null)
		{
			e.perform();
			e.log();
			e = edits.poll();
		}
	}
	
	private class Edit
	{
		int type, replaced;
		int x, y, z;
		
		Edit(int type, int replaced, int x, int y, int z)
		{
			this.type = type;
			this.replaced = replaced;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void perform()
		{
			if (etc.getServer().getBlockIdAt(x, y, z) == type)
			{
				if (etc.getServer().setBlockAt(replaced, x, y, z))
					log.info("R (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
				else
					log.info("r (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
			}
		}
		
		public void log()
		{
			int current = etc.getServer().getBlockIdAt(x, y, z);
			if (current == type)
				log.info("+ (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
			else
				log.info("- (" + x + ", " + y + ", " + z + ") " + replaced + " " + type);
		}
	}
}