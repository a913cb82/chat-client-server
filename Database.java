package local.fjava.tick5;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import local.fjava.messages.ChatMessage;
import local.fjava.messages.RelayMessage;

public class Database {

	private Connection connection;

	public Database(String databasePath) throws SQLException,
			ClassNotFoundException {
		// where to store database on disk
		Class.forName("org.hsqldb.jdbcDriver");
		connection = DriverManager.getConnection("jdbc:hsqldb:file:"
				+ databasePath + "/chat-database", "SA", "");
		Statement delayStmt = connection.createStatement();
		try {
			delayStmt.execute("SET WRITE_DELAY FALSE");
		} // Always update data on disk
		finally {
			delayStmt.close();
		}

		// we will manually control when to commit transaction
		connection.setAutoCommit(false);

		// create messages table
		Statement sqlStmt = connection.createStatement();
		try {
			sqlStmt.execute("CREATE TABLE messages(nick VARCHAR(255) NOT NULL,"
					+ "message VARCHAR(4096) NOT NULL,timeposted BIGINT NOT NULL)");
		} catch (SQLException e) {
			System.out
					.println("Warning: Database table \"messages\" already exists.");
		} finally {
			sqlStmt.close();
		}

		// create statistics table
		sqlStmt = connection.createStatement();
		try {
			sqlStmt.execute("CREATE TABLE statistics(key VARCHAR(255),value INT)");
		} catch (SQLException e) {
			System.out
					.println("Warning: Database table \"statistics\" already exists.");
		} finally {
			sqlStmt.close();
		}

		// add rows to statistics table
		String stmt = "INSERT INTO statistics(key,value) VALUES ('Total messages',0)";
		PreparedStatement insertMessages = connection.prepareStatement(stmt);
		insertMessages.executeUpdate();
		insertMessages.close();
		stmt = "INSERT INTO statistics(key,value) VALUES ('Total logins',0)";
		PreparedStatement insertLogins = connection.prepareStatement(stmt);
		insertLogins.executeUpdate();
		insertLogins.close();

		// commit
		connection.commit();
	}

	public void close() throws SQLException {
		connection.close();
	}

	public void incrementLogins() throws SQLException {
		// increment count whenever user logs into server
		String stmt = "UPDATE statistics SET value = value+1 WHERE key='Total logins'";
		PreparedStatement incCount = connection.prepareStatement(stmt);
		incCount.executeUpdate();
		incCount.close();

		connection.commit();
	}

	public void addMessage(RelayMessage m) throws SQLException {
		// add a row to message table
		String stmt = "INSERT INTO MESSAGES(nick,message,timeposted) VALUES (?,?,?)";
		PreparedStatement insertMessage = connection.prepareStatement(stmt);
		try {
			insertMessage.setString(1, m.getFrom());
			insertMessage.setString(2, m.getMessage());
			insertMessage.setLong(3, System.currentTimeMillis());
			insertMessage.executeUpdate();
		} finally {
			insertMessage.close();
		}
		connection.commit();
	}

	public List<RelayMessage> getRecent() throws SQLException {
		List<RelayMessage> recent = new ArrayList<RelayMessage>();

		String stmt = "SELECT nick,message,timeposted FROM messages "
				+ "ORDER BY timeposted DESC LIMIT 10";
		PreparedStatement recentMessages = connection.prepareStatement(stmt);
		try {
			ResultSet rs = recentMessages.executeQuery();
			try {
				while (rs.next()) {
					recent.add(new RelayMessage(rs.getString(1),
							new ChatMessage(rs.getString(2))));
				}
			} finally {
				rs.close();
			}
		} finally {
			recentMessages.close();
		}
		return recent;
	}

	public static void main(String[] args) throws SQLException,
			ClassNotFoundException {

		if (args.length == 0) {
			System.out
					.println("Usage: java local.fjava.tick5.Database <database name>");
			return;
		}

		// where to store database on disk
		Class.forName("org.hsqldb.jdbcDriver");
		Connection connection2 = DriverManager.getConnection(
				"jdbc:hsqldb:file:" + args[0] + "/chat-database", "SA", "");
		Statement delayStmt = connection2.createStatement();
		try {
			delayStmt.execute("SET WRITE_DELAY FALSE");
		} // Always update data on disk
		finally {
			delayStmt.close();
		}

		// we will manually control when to commit transaction
		connection2.setAutoCommit(false);

		// create messages table
		Statement sqlStmt = connection2.createStatement();
		try {
			sqlStmt.execute("CREATE TABLE messages(nick VARCHAR(255) NOT NULL,"
					+ "message VARCHAR(4096) NOT NULL,timeposted BIGINT NOT NULL)");
		} catch (SQLException e) {
			System.out
					.println("Warning: Database table \"messages\" already exists.");
		} finally {
			sqlStmt.close();
		}

		// add a row to message table
		String stmt = "INSERT INTO MESSAGES(nick,message,timeposted) VALUES (?,?,?)";
		PreparedStatement insertMessage = connection2.prepareStatement(stmt);
		try {
			insertMessage.setString(1, "Alastair"); // set value of first "?" to
													// "Alastair"
			insertMessage.setString(2, "Hello, Andy");
			insertMessage.setLong(3, System.currentTimeMillis());
			insertMessage.executeUpdate();
		} finally { // Notice use of finally clause here to finish statement
			insertMessage.close();
		}

		// commit transaction
		connection2.commit();

		// query database and print out answer
		stmt = "SELECT nick,message,timeposted FROM messages "
				+ "ORDER BY timeposted DESC LIMIT 10";
		PreparedStatement recentMessages = connection2.prepareStatement(stmt);
		try {
			ResultSet rs = recentMessages.executeQuery();
			try {
				while (rs.next())
					System.out.println(rs.getString(1) + ": " + rs.getString(2)
							+ " [" + rs.getLong(3) + "]");
			} finally {
				rs.close();
			}
		} finally {
			recentMessages.close();
		}

		connection2.close();
	}
}
