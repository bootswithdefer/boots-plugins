import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.*;

public class PlayerStatSQL extends PlayerStat
{
	private boolean enabled;
	private boolean usehModDb;
	private String dbDriver;
	private String dbUrl;
	private String dbUsername;
	private String dbPassword;
	static final Logger log	= Logger.getLogger("Minecraft");

	PlayerStatSQL(String name, boolean usehModDb)
	{
		super(name);
		this.usehModDb = usehModDb;

		if (!usehModDb)
		{		
			PropertiesFile properties	= new	PropertiesFile("mysql.properties");
			try {
				dbDriver = properties.getString("driver", "com.mysql.jdbc.Driver");
				dbUrl = properties.getString("db", "jdbc:mysql://localhost:3306/minecraft");
				dbUsername = properties.getString("user", "user");
				dbPassword = properties.getString("pass", "pass");
			} catch (Exception ex) {
				log.log(Level.SEVERE, this.getClass().getName() + ": exception while reading from mysql.properties", ex);
				return;
			}
		}

		enabled = checkSchema();
	}

	private Connection getConnection()
	{
		try {
			if (usehModDb)
				return etc.getSQLConnection();
			return DriverManager.getConnection(dbUrl + "?autoReconnect=true&user=" + dbUsername + "&password=" + dbPassword);
		} catch (SQLException ex) {
			log.log(Level.SEVERE, this.getClass().getName() + ": exception while connection to database", ex);
		}
		enabled = false;
		return null;
	}

	private boolean checkSchema()
	{
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, "stats", null);
			if (!rs.next())
			{
				ps = conn.prepareStatement(
					"CREATE TABLE `stats` (" +
					"`player` varchar(32) NOT NULL DEFAULT '-'," +
					"`category` varchar(32) NOT NULL DEFAULT 'stats'," +
					"`stat` varchar(32) NOT NULL DEFAULT '-'," +
					"`value` int(11) NOT NULL DEFAULT '0'," +
					"PRIMARY KEY (`player`,`category`,`stat`));"
				);
				ps.executeUpdate();
				log.info(this.getClass().getName() + " created table 'stats'.");
			}
			return true;
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
		return false;
	}	

	protected void save()
	{
		if (!enabled)
			return;

		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);

			Iterator<String> catIter = categories.keySet().iterator();
			while (catIter.hasNext())
			{
				String catName = catIter.next();
				Category cat = categories.get(catName);
				if (!cat.modified)
					continue;

				Iterator<String> statIter = cat.iterator();
				while (statIter.hasNext())
				{
					String statName = statIter.next();
					int value = cat.get(statName);

					ps = conn.prepareStatement("INSERT INTO stats (player,category,stat,value) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE value=?", Statement.RETURN_GENERATED_KEYS);
					ps.setString(1, name);
					ps.setString(2, catName);
					ps.setString(3, statName);
					ps.setInt(4, value);
					ps.setInt(5, value);
					ps.executeUpdate();
				}
			}
			conn.commit();
		} catch (SQLException ex) {
			log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
			}
		}
	}
	
	protected void load()
	{
		if (!enabled)
			return;

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			conn = getConnection();
			ps = conn.prepareStatement("SELECT * from stats where player = ?", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			rs = ps.executeQuery();
			while (rs.next())
			{
				put(rs.getString("category"), rs.getString("stat"), rs.getInt("value"));
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
}