package distributed.schiperegglisandoz;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
	private static final long serialVersionUID = 4130400422360754730L;

	private String message;
	private int senderId;
	private int[] timestamp;
	private int[][] buffer;

	public Message(String message, int senderId, int[][] buffer, int[] timestamp) {
		this.message = message;
		this.senderId = senderId;
		this.timestamp = timestamp;
		this.buffer = buffer;
	}

	public String getMessage() {
		return message;
	}

	public int getSenderId() {
		return senderId;
	}

	public int[] getTimestamp() {
		return timestamp;
	}

	public int[] getBuffer(int i) {
		return buffer[i];
	}

	@Override
	public String toString() {
		return "Message(\""+message+"\", sender = "+senderId+", timestamp = "+Arrays.toString(timestamp)+", buffer = "+Arrays.deepToString(buffer)+")";
	}

}
