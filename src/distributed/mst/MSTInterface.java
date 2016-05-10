package distributed.mst;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MSTInterface extends Remote {

	public void initiate(long order, int fromId, int level, long fragment, State state) throws RemoteException;

	public void test(int fromId, int level, long fragment) throws RemoteException;

	public void reject(int fromId) throws RemoteException;

	public void accept(int fromId) throws RemoteException;

	public void report(long order, int fromId, long weight) throws RemoteException;

	public void changeRoot(long order, int fromId) throws RemoteException;

	public void connect(long order, int fromId, int level) throws RemoteException;

	public void halt(long order, int fromId) throws RemoteException;

}
