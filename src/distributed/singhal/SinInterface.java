package distributed.singhal;

import java.rmi.Remote;

public interface SinInterface extends Remote {

	public void requestToken(int process, int requestId);

	public void receiveToken(Token token);

}
