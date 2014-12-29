package local.fjava.tick5;

import java.util.HashSet;
import java.util.Set;

public class MultiQueue<T> {
	private Set<MessageQueue<T>> outputs = new HashSet<MessageQueue<T>>();

	public void register(MessageQueue<T> q) {
		// add "q" to "outputs";
		synchronized (this) {
			outputs.add(q);
		}
	}

	public void deregister(MessageQueue<T> q) {
		// remove "q" from "outputs"
		synchronized (this) {
			outputs.remove(q);
		}
	}

	public void put(T message) {
		// copy "message" to all elements in "outputs"
		synchronized (this) {
			for (MessageQueue<T> q : outputs)
				q.put(message);
		}
	}
}