import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Delayer implements Runnable 
{
	private int port;
	private String serviceName;
	private Messages msg;
	
	public Delayer(int port, String serviceName, Messages msg) 
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
			Thread.sleep(5000); //5secs
			Registry reg = LocateRegistry.getRegistry(port);
			NodeI node = (NodeI) reg.lookup(serviceName);
			node.sendMsg(msg);
		} 
		
		catch (RemoteException | NotBoundException | InterruptedException e1) 
		{
			e1.printStackTrace();
		}
	}

}
