package distributed.singhal;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for a distributed system node using the Singhal mutual exclusion algorithm.
 */
public interface SinInterface extends Remote {

	/**
	 * Method for processing an incoming request for CS access.
	 * @param process The id of the process that wants access
	 * @param requestId Each request should be unique and incrementing per process
	 */
	public void requestToken(int process, int requestId) throws RemoteException;

	/**
	 * Method for receiving the token.
	 * It gives the callee permission to enter the CS.
	 */
	public void receiveToken(Token token) throws RemoteException;

}
