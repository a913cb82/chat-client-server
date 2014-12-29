package local.fjava.tick5;

public class SafeMessageQueue<T> implements MessageQueue<T> {
	private static class Link<L> {
		L val;
		Link<L> next;

		Link(L val) {
			this.val = val;
			this.next = null;
		}
	}

	private Link<T> first = null;
	private Link<T> last = null;

	public synchronized void put(T val) {
		// given a new "val", create a new Link<T>
		// element to contain it and update "first" and
		// "last" as appropriate
		Link<T> l = new Link<T>(val);
		//l.next = first;
		//if (last == null)
		//	last = l;
		//first = l;
		if (last != null)
			last.next = l;
		last = l;
		if (first == null)
			first = l;
		this.notify();
	}

	public synchronized T take() {
		while (first == null)
			// use a loop to block thread until data is available
			try {
				this.wait();
			} catch (InterruptedException ie) {
			}
		// retrieve "val" from "first", update "first" to refer
		// to next element in list (if any). Return "val"
		T val = first.val;
		first = first.next;
		if (first == null)
			last = null;
		return val;
	}
}