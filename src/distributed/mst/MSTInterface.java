package distributed.mst;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MSTInterface extends Remote {

	public void initiate(int fromId, int level, long fragment, State state) throws RemoteException;

	public void test(int fromId, int level, long fragment) throws RemoteException;

	public void reject(int fromId) throws RemoteException;

	public void accept(int fromId) throws RemoteException;

	public void report(int fromId, long weight) throws RemoteException;

	public void changeRoot() throws RemoteException;

	public void connect(int fromId, int level) throws RemoteException;

	public void halt(int fromId) throws RemoteException;

}
