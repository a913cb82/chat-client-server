package local.fjava.tick5;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import local.fjava.messages.ChangeNickMessage;
import local.fjava.messages.ChatMessage;
import local.fjava.messages.Message;
import local.fjava.messages.RelayMessage;
import local.fjava.messages.StatusMessage;

public class ClientHandler {
	private Socket socket;
	private MultiQueue<Message> multiQueue;
	private String nickname;
	private MessageQueue<Message> clientMessages;
	private boolean clientActive = false;
	private Database database;

	public ClientHandler(Socket s, MultiQueue<Message> q, Database d) {
		// set client to active
		clientActive = true;

		// update socket, multiQueue and database
		socket = s;
		multiQueue = q;
		database = d;

		// update clientMessages
		clientMessages = new SafeMessageQueue<Message>();
		multiQueue.register(clientMessages);

		// increment database logins
		try {
			database.incrementLogins();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		// update nickname
		nickname = "Anonymous" + Integer.toString(new Random().nextInt(99999));

		// new thread to handle outgoing messages on SafeMessageQueue
		final Thread outgoing = new Thread() {
			@Override
			public void run() {
				// create stream to send message
				ObjectOutputStream outStream = null;
				try {
					outStream = new ObjectOutputStream(socket.getOutputStream());
				} catch (Exception e) {
					e.printStackTrace();
				}

				// send new client 10 most recent messages
				List<RelayMessage> recent = null;
				try {
					recent = database.getRecent();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				for (int i = recent.size() - 1; i > -1
						&& i > recent.size() - 11; i--) {
					try {
						outStream.writeObject((Object) recent.get(i));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				/*
				 * for (RelayMessage m : recent) { try {
				 * outStream.writeObject((Object) m); } catch (IOException e) {
				 * // TODO Auto-generated catch block e.printStackTrace(); } }
				 */

				while (clientActive) {
					Message m = clientMessages.take();
					// serialise and send this message
					try {
						outStream.writeObject((Object) m);
					} catch (IOException e) {
						return;
					}
				}
			}
		};
		outgoing.setDaemon(true);
		outgoing.start();

		// new thread to handle incoming serialised objects from client
		Thread incoming = new Thread() {
			@Override
			public void run() {
				// create stream to receive message
				ObjectInputStream inStream = null;
				try {
					inStream = new ObjectInputStream(socket.getInputStream());
				} catch (Exception e) {
					e.printStackTrace();
				}
				while (clientActive) {
					// get message type
					Message m = null;
					try {
						m = (Message) inStream.readObject();
					} catch (Exception e) {
						// client has gone
						System.out.println("Client Gone");
						// outgoing.stop();
						multiQueue.put(new StatusMessage(nickname
								+ " has disconnected."));
						return;
					}
					// if ChangeNickMessage, update nickname
					// and put StatusMessage
					if (m instanceof ChangeNickMessage) {
						String oldNick = nickname;
						nickname = ((ChangeNickMessage) m).name;
						multiQueue.put(new StatusMessage(oldNick
								+ " is now known as " + nickname + "."));

					}
					// if ChatMessage, put RelayMessage
					else if (m instanceof ChatMessage) {
						multiQueue.put(new RelayMessage(nickname,
								(ChatMessage) m));
						// add to database
						try {
							database.addMessage(new RelayMessage(nickname,
									(ChatMessage) m));
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// ignore any other type
				}
			}
		};
		incoming.setDaemon(true);
		incoming.start();

		// new status message to record new client connected
		InetAddress inet = socket.getInetAddress();
		String machineName = inet.getHostName();
		Message message = new StatusMessage(nickname + " connected from "
				+ machineName + ".");
		multiQueue.put(message);
	}
}