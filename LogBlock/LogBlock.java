import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

import java.sql.*;

import net.minecraft.server.MinecraftServer;

public class LogBlock extends Plugin
{
	private String name = "LogBlock";
	private int version = 5;
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbUrl = "";
	private String dbUsername = "";
	private String dbPassword = "";
	private int delay = 10;
	private int toolID = 270; // 270 is wood pick
	private Consumer consumer = null;
	
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("logblock.properties");
		try {
			dbDriver = properties.getString("driver", "com.mysql.jdbc.Driver");
			dbUrl = properties.getString("url", "jdbc:mysql://localhost:3306/db");
			dbUsername = properties.getString("username", "user");
			dbPassword = properties.getString("password", "pass");
			delay = properties.getInt("delay", 10);
			toolID = properties.getInt("tool-id", 270);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception	while	reading from logblock.properties",	ex);
		}
		
		try {
			new JDCConnectionDriver(dbDriver, dbUrl, dbUsername, dbPassword);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while creation database connection pool", ex);
			return;
		}

		consumer = new Consumer();
		new Thread(consumer).start();
		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		consumer.stop();
		consumer = null;
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		LBListener listener = new LBListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.LOW);
	}
	
	private Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:jdc:jdcpool");
	}
	
	private void queueBlock(Player player, String type, Block b)
	{
		if (b.getType() == 0)
			return;
		BlockRow row = new BlockRow(player.getName(), type, b.getType(), b.getX(), b.getY(), b.getZ());
		boolean result = bqueue.offer(row);
		if (!result)
			log.info(name + " failed to queue block for " + player.getName());
	}
	
	private void showBlockHistory(Player player, Block b)
	{
		player.sendMessage(Colors.Blue + "Block history (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + "): ");
		boolean hist = false;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * from blocks where x = ? and y = ? and z = ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, b.getX());
			ps.setInt(2, b.getY());
			ps.setInt(3, b.getZ());
			rs = ps.executeQuery();
			while (rs.next())
			{
				String msg = rs.getString("date") + " " + rs.getString("player") + " " + rs.getString("action") + " " + etc.getDataSource().getItem(rs.getInt("type"));
				player.sendMessage(Colors.Blue + msg);
				hist = true;
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, name + " SQL exception", ex);
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception on close", ex);
			}
		}
		if (!hist)
			player.sendMessage(Colors.Blue + "None.");
	}
	
	public class LBListener extends PluginListener // start
	{
	   public boolean onCommand(Player player, String[] split)
	   {
			if (!player.canUseCommand(split[0]))
				return false;
				
	      if (split[0].equalsIgnoreCase("/lb")) {
			
				return true;
			}
			return false;
		}
	
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
		{
			if (itemInHand == toolID && player.canUseCommand("/blockhistory"))
			{
				showBlockHistory(player, blockClicked);
				return true;
			}
		
			Block before = new Block(etc.getServer().getBlockIdAt(blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ()), blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ());
			
			if (before.getType() == blockPlaced.getType())
				return false;
				
			queueBlock(player, "create", blockPlaced);
			queueBlock(player, "destroy", before);
			return false;
		}
		
		public boolean onBlockDestroy(Player player, Block blockAt)
		{
			Block after = new Block(etc.getServer().getBlockIdAt(blockAt.getX(), blockAt.getY(), blockAt.getZ()), blockAt.getX(), blockAt.getY(), blockAt.getZ());
			
			if (after.getType() == blockAt.getType())
				return false;
			
			queueBlock(player, "destroy", blockAt);
			queueBlock(player, "create", after);
			return false;
		}
	} // end LBListener

	private class Consumer implements Runnable // start
	{
		private boolean stop = false;
		Consumer() { stop = false; }
		public void stop() { stop = true; }
		public void run()
		{
			PreparedStatement ps = null;
			Connection conn = null;
			BlockRow b;
			
			while (!stop)
			{
			   long start = System.currentTimeMillis();
				int count = 0;
				
				if (bqueue.size() > 100)
					log.info(name + " queue size " + bqueue.size());
				
				try {
					conn = getConnection();
					conn.setAutoCommit(false);
					ps = conn.prepareStatement("INSERT INTO blocks (date, player, action, type, x, y, z) VALUES (now(),?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

					while (count < 100 && start+delay > (System.currentTimeMillis()/1000F))
					{
						b = bqueue.poll(1L, TimeUnit.SECONDS);
						if (b == null)
							continue;
						//b.log();
						ps.setString(1, b.name);
						ps.setString(2, b.action);
						ps.setInt(3, b.type);
						ps.setInt(4, b.x);
						ps.setInt(5, b.y);
						ps.setInt(6, b.z);
						ps.executeUpdate();
						count++;
					}
					
					conn.commit();
				} catch (InterruptedException ex) {
					log.log(Level.SEVERE, name + " interrupted exception", ex);
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception", ex);
				} finally {
					try {
						ps.close();
						conn.close();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, name + " SQL exception on close", ex);
					}
				}
			}
		}
	} // end LBDB
	
	private class BlockRow // start
	{
		public String name;
		public String action;
		public int type;
		public int x, y, z;
		
		BlockRow(String name, String action, int type, int x, int y, int z)
		{
			this.name = name;
			this.action = action;
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void log()
		{
			log.info("name: " + name + " action: " + action + " type: " + type + " x: " + x + " y: " + y + " z: " + z);
		}
	} // end MyRow
} // end LogBlock
