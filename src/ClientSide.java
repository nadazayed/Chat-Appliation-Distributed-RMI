import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class ClientSide 
{
	public static void main(String[] args) 
	{
		int nodeId = 1;
		
		try 
		{
//			System.out.println("Node: " + NodeI.services[nodeId]);
			
			ChatNode node = new ChatNode(nodeId);
			initServer(node);
			initClient(node, nodeId);
			
		} 
		
		catch (RemoteException e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		} 
		catch (NotBoundException e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	//SERVER
		public static void initServer(ChatNode node) throws RemoteException
		{
			Registry reg = LocateRegistry.createRegistry(node.myPort);
			reg.rebind(node.myService, node);
		}
		
	//CLIENT
		private static void initClient(ChatNode node, int nodeId) throws RemoteException, NotBoundException 
		{
			Timer timer = new Timer();

	        timer.scheduleAtFixedRate(new TimerTask() //Run this thread every 100sec
	        {
	            @Override
	            public void run() 
	            {
	                try 
	                {
	                    node.fetchNewMessages();
	                } 
	                
	                catch (RemoteException ex) 
	                {
	                    System.out.println(ex.getMessage());
	                }
	            }
	        }, 0, 100);
			
	        System.out.println("To end chat type 'exit'");
	        
			while (true)
			{
				System.out.println("\nEnter your msg here ..");
				String myMsg = node.scan.nextLine();
				
				if(myMsg.isEmpty())	continue;

				if (myMsg.matches("exit"))
				{
					node.save();
				}
				
				// Initialize message ID
				String msgId = UUID.randomUUID().toString();
				String sender = node.myService;
				
				// Update local logical clock before sending a message
				node.myLocalClock++; //for the specified node
				
				// Create a message object to be sent (time-stamped with lClock value)
				Messages msg = null;
				
				msg = new Messages(msgId, nodeId, sender, node.myLocalClock, myMsg);
				
				node.multicastMessages(msg);
			}
			
		}

}
