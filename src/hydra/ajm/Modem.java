package hydra.ajm;

public class Modem {

	public void close() {
	}

	public int available() {
		return 0;
	}

	public int read() {
		return 0;
	}

	public byte[] readBytes() {
		byte[] bytes = null;
		int size = available();
		if (size > 0) {
			bytes = new byte[size];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte) read();
			}
		}
		return bytes;
	}

	public void write(byte value) {
	}

	public void write(byte bytes[]) {
		for (int i = 0; i < bytes.length; i++) {
			write(bytes[i]);
		}
	}

	public void write(String what) {
		write(what.getBytes());
	}
}
