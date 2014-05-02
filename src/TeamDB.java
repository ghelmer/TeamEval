import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TeamDB {
	private Connection conn;
	
	/**
	 * Construct the new TeamDB database connection.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public TeamDB() throws ClassNotFoundException, IOException
	{
		SimpleDataSource.init("database.properties");
	}
	
	/**
	 * Construct the new TeamDB database connection with the
	 * specified database connection parameter file.
	 * @param propertyFn
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public TeamDB(String propertyFn) throws ClassNotFoundException, IOException
	{
		SimpleDataSource.init(propertyFn);
	}
	public Connection getConnection() throws SQLException
	{
		if (conn == null)
		{
			conn = SimpleDataSource.getConnection();
		}
		return conn;
	}
	
	/**
	 * Close the database connection.
	 * @throws SQLException
	 */
	public void close() throws SQLException
	{
		if (conn != null)
		{
			conn.close();
			conn = null;
		}
	}
}
