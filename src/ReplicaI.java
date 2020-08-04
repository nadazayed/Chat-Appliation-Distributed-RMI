import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReplicaI extends Remote
{
	String[] hostnames = {"127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1"};
	Integer[] ports = {6000 ,7000, 8000, 9000};
	String[] services = {"ReplicaA", "ReplicaB", "ReplicaC", "ReplicaD"};
	
	boolean uploadFile(SerialilzableFile file) throws RemoteException;
	SerialilzableFile downloadFile(String chatNode) throws RemoteException;
}
