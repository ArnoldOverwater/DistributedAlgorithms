package distributed.singhal;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SinInterface extends Remote {

	public void requestToken(int process, int requestId) throws RemoteException;

	public void receiveToken(Token token) throws RemoteException;

}
