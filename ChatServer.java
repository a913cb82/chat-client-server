package local.fjava.tick5;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

import local.fjava.messages.Message;

public class ChatServer {
	public static void main(String args[]) throws ClassNotFoundException,
			SQLException {
		if (args.length < 2) {
			System.out
					.println("Usage: java ChatServer <port> <database path prefix>");
			return;
		}
		int port = Integer.parseInt(args[0]);
		Database d = new Database(args[1]);

		ServerSocket ss;
		try {
			ss = new ServerSocket(port);
		} catch (Exception e) {
			System.out.println("Cannot use port number " + port);
			return;
		}

		MultiQueue<Message> q = new MultiQueue<Message>();
		Socket s = null;

		while (true) {
			// call accept on ServerSocket
			try {
				s = ss.accept();
				System.out.println("Accepted");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// create new ClientHandler, passing MultiQueue reference
			// with Socket returned by ServerSocket
			new ClientHandler(s, q, d);
		}
	}
}