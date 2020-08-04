import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface BackupI extends Remote
{
	int PrimaryServerPort = 4000;
	int SecondaryServerPort = 5000;
	
	String PrimaryServerHostName = "127.0.0.1";
	String SecondaryServerHostName = "127.0.0.1";
	
	String PrimaryServerServiceName = "PrimaryServer";
	String SecondaryServerServiceName = "SecondaryServer";
	
	//from client to server >> send backup to replicas
	boolean setBackup(String chatNode,SerialilzableFile file) throws RemoteException, NotBoundException;
	
	//from client to server >> get backup from replicas
	SerialilzableFile getBackup(String chatNode) throws RemoteException, NotBoundException;
	
	void  setUpdate (String ChatNode, String ReplicaNode) throws RemoteException, NotBoundException;
	void getUpdate (HashMap backup, HashMap outdated) throws RemoteException, NotBoundException;
}
