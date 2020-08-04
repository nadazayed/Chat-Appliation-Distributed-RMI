import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.PriorityQueue;

public interface NodeI extends Remote
{
	String[] hostnames = {"127.0.0.1", "127.0.0.1", "127.0.0.1"};
	Integer[] ports = {1000, 2000, 3000};
	String[] services = {"Nada", "Saeed", "Ghaith"};
	
	void sendMsg(Messages msg) throws RemoteException, NotBoundException;
	void ack(Messages msg) throws RemoteException;
}
