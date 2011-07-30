package hydra.ajm;

public class ByteFIFO {

	private byte[] buffer = null;
	private int buffer_size;
	private int head;
	private int tail;

	public ByteFIFO(int n) {
		buffer_size = (int) Math.pow(2, n);
		buffer = new byte[buffer_size];
		head = tail = 0;
	}

	public synchronized int size() {
		return (tail + buffer_size - head) & (buffer_size - 1);
	}

	public synchronized boolean full() {
		int new_tail = (tail + 1) & (buffer_size - 1);
		return (new_tail == head);
	}

	public synchronized boolean empty() {
		return (size() == 0);
	}

	public synchronized int free() {
		return buffer_size - size();
	}

	public synchronized void add(byte what) {
		int new_tail = (tail + 1) & (buffer_size - 1);
		if(new_tail != head){
			buffer[tail] = what;
			tail = new_tail;
		}
	}

	public synchronized byte remove() {
		byte d = buffer[head];
		head = (head + 1) & (buffer_size - 1);
		return d;
	}

	public synchronized void clear() {
		head = tail = 0;
	}
}
