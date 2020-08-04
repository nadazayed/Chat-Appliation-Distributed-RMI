import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class PrimaryBackupServer  extends UnicastRemoteObject implements BackupI
{
	private static final long serialVersionUID = 1L;
	static HashMap <String, String> backup; //saeed : replicaA, replicaB, replicaC
	static HashMap <String, String> outdated ; //saeed : replicB
	
	int cnt = 0;
	Timer timer;
	
	protected PrimaryBackupServer() throws RemoteException 
	{
		System.out.println("Primary server intialized..\n");
		
		check();
	}
	
	public static void main(String[] args) throws RemoteException
	{
		backup = new HashMap();
		outdated = new HashMap();
		
		PrimaryBackupServer primary = new PrimaryBackupServer();
        Registry registry = LocateRegistry.createRegistry(BackupI.PrimaryServerPort);
        registry.rebind(BackupI.PrimaryServerServiceName, primary);
	}
	
	public void check()
	{
		timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() //Run this thread every 9000
        {
            @Override
            public void run() 
            {
                if (!outdated.isEmpty())
                {
                	processUpdate();
                }
            }
        }, 0, 9000);
	}
	
	public void processUpdate() 
	{
//		timer.cancel();
    	for(String k:  outdated.keySet()) // k(nada) : 0   outdated(nada) > 0
    	{
    		System.out.println( outdated.keySet().size()+" outdated nodes, now trying to update "+k);
    		
			if (k.matches("Secondary Server")) //primary server missing updates
			{
				System.out.println("Now serving Secondary server");
				try
				{
					Registry registry = LocateRegistry.getRegistry(BackupI.SecondaryServerPort);
    				BackupI secondary = (BackupI) registry.lookup(BackupI.SecondaryServerServiceName);
    				
    				System.out.println("Secondary server is back alive");
    				
    				System.out.println(k+" is removed from outdated map");
    				outdated.remove(k);
    				
    				secondary.getUpdate(backup, outdated); // nada:0|1|2
    				System.out.println(outdated.size()+" left to be updated");
    				
    				
//    				keys = outdated.keySet(); //......
				}
				
				catch (RemoteException | NotBoundException e) 
	    		{
					System.out.println("Primary server is still dead");
	    		}
				
			}
			
			if (!k.matches("Secondary Server")) //Replica x missing updates  ....
			{
				System.out.println("Now serving "+k);
				try
				{
					Registry reg = LocateRegistry.getRegistry(ReplicaI.ports[Integer.parseInt(outdated.get(k))]); // replica 1
    				ReplicaI replica = (ReplicaI) reg.lookup(ReplicaI.services[Integer.parseInt(outdated.get(k))]);
    				
    				System.out.println(ReplicaI.services[Integer.parseInt(outdated.get(k))]+" is back alive");
    				
    				//get file from another replica and put it here
    				String reps = backup.get(k); // 0 | 2
    				String[] split = reps.split("|");
    				System.out.println("Getting backup from "+ReplicaI.services[Integer.parseInt(split[0])]);
    				
    				Registry reg2 = LocateRegistry.getRegistry(ReplicaI.ports[Integer.parseInt(split[0])]); // replica 0
    				ReplicaI replica2 = (ReplicaI) reg2.lookup(ReplicaI.services[Integer.parseInt(split[0])]);
    				
    				SerialilzableFile fs = replica2.downloadFile(k);
    				replica.uploadFile(fs);
    				
    				backup.put(k, backup.get(k)+outdated.get(k)+"|"); //nada: A|B >> nada:A|B|C
    				
    				setUpdate(k, backup.get(k));
    				
    				System.out.println(ReplicaI.services[Integer.parseInt(outdated.get(k))]+" is now updated");
    				
    				System.out.println(k+" is removed from outdated map");
    				outdated.remove(k);
    				System.out.println(outdated.size()+" left to be updated");
//    				keys = outdated.keySet();
				}
				
				catch (RemoteException | NotBoundException e) 
	    		{
					System.out.println(ReplicaI.services[Integer.parseInt(outdated.get(k))]+" is still dead");
	    				
	    		}
				
			}
    		
    	}
    	
    	System.out.println("\n");
//    	check();
	}

	@Override
	public boolean setBackup(String chatNode, SerialilzableFile file)  //saeed, history file
    {
		boolean flag = true;
		String temp = "";
		
		System.out.println("\nfile recieved successfully from "+chatNode);
		
		for (int i=0;i<3;i++)
		{
			System.out.println("backup going to replica:"+ReplicaI.services[cnt]);
			
			try 
			{
				Registry reg = LocateRegistry.getRegistry(ReplicaI.ports[cnt]);
				ReplicaI replica = (ReplicaI) reg.lookup(ReplicaI.services[cnt]);
				
				if (replica.uploadFile(file))
				{
					System.out.println("History stored in replica node successfully\n");
					temp+=cnt + "|"; // Saeed : 0 | 1 | 2 >> replicA ...
				}
					
				else
					flag = false;
			} 
			
			catch (RemoteException | NotBoundException e) 
			{
				System.out.println("Error occured while trying to connect to "+ReplicaI.services[cnt]);
				
				outdated.put(chatNode, cnt+""); //saeed : 1
				System.out.println(ReplicaI.services[cnt]+" is now outdated\n");
//				e.printStackTrace();
			}
			
			cnt=(cnt+1)%4;
		}
		
		backup.put(chatNode, temp); // saeed : 0 | 2
		
		try 
		{
			setUpdate(chatNode, temp);
		} 
		
		catch (RemoteException | NotBoundException e) 
		{
//			e.printStackTrace();
			System.out.println("Error occured while trying to connect to Primary server");
			
			outdated.put("Secondary Server", 4+"");
			System.out.println("outdated server is added");
		}
		return flag;
    }

	@Override
	public void setUpdate(String ChatNode, String ReplicaNode) throws RemoteException, NotBoundException // btb3t el update
	{
		Registry registry = LocateRegistry.getRegistry(BackupI.SecondaryServerPort);
		BackupI secondary = (BackupI) registry.lookup(BackupI.SecondaryServerServiceName);
		
		secondary.getUpdate(backup, outdated);
	}
	
	@Override
	public void getUpdate (HashMap newBackup, HashMap newOutdated) throws RemoteException, NotBoundException //bt-save el update
	{
		backup = newBackup;
		outdated = newOutdated;
		
		System.out.println("Primary backup server is updated");
	}

	@Override
	public SerialilzableFile getBackup(String chatNode) throws RemoteException, NotBoundException 
	{
		
		if (!backup.isEmpty() || backup.containsKey(chatNode))
		{
			String reps = backup.get(chatNode); // backup.get(saeed) > 0|1|2
			
			String temp[] = reps.split("|");
			
			for (int i=0;i<temp.length;i++)
			{
				try
				{
					System.out.println("Searching in "+ReplicaI.services[Integer.parseInt(temp[i])]+" for file "+chatNode);
					
					Registry reg = LocateRegistry.getRegistry(ReplicaI.ports[Integer.parseInt(temp[i])]);
					ReplicaI replica = (ReplicaI) reg.lookup(ReplicaI.services[Integer.parseInt(temp[i])]);
					
					return replica.downloadFile(chatNode);
				}
				
				catch (RemoteException | NotBoundException e) 
				{
					System.out.println(ReplicaI.services[Integer.parseInt(temp[i])]+" is down trying to retrieve history from another replica..");
				}
			}
		}
		
		return null;
		
	}

}
