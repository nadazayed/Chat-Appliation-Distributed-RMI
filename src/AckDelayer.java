import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AckDelayer implements Runnable
{
	private int port;
	private String serviceName;
	private Messages msg;
	
	public AckDelayer(int port, String serviceName, Messages msg) 
	{
		this.port = port;
		this.serviceName = serviceName;
		this.msg = msg;
	}
	
	@Override
	public void run() 
	{
		try 
		{
			Thread.sleep(7000); //7secs
			Registry reg = LocateRegistry.getRegistry(port);
			NodeI node = (NodeI) reg.lookup(serviceName);
			node.ack(msg);
		} 
		
		catch (RemoteException | NotBoundException | InterruptedException e1) 
		{
			e1.printStackTrace();
		}
	}
	
}