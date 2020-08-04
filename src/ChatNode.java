import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ChatNode extends UnicastRemoteObject implements NodeI
{
	private static final long serialVersionUID = 1L;
	public static String PATH;
	final static int num = hostnames.length;
	
	int myLocalClock, myPort;
	String myHost, myService;
	
	PriorityQueue<Messages> msgsQ;
	HashMap<String, Integer> msgsAcks;
	
	PriorityQueue<Messages> backupQ;
	
	Scanner scan;
	
	boolean available = false;
	
	protected ChatNode (int nodeId) throws RemoteException
	{
		myHost = hostnames[nodeId];
		myPort = ports[nodeId];
		myService = services[nodeId];
		
		myLocalClock = 0;
		
		msgsQ = new PriorityQueue<Messages>(); // Nada:msg
		msgsAcks = new HashMap<String, Integer>(); // Nada:msg  --  1 ack
		
		backupQ = new PriorityQueue<Messages>();
		
		scan = new Scanner(System.in);
		
		URL url = ChatNode.class.getResource("LocalStorage/"+myService+"/");
        PATH = url.getPath();
        
        System.out.println("Node:"+myService+" host:"+myHost+" port:"+myPort+" local clock: "+myLocalClock+" local storage:"+PATH);
        
        retrieve();
        
        ping(0);
	}
	
	public void ping(int flag)
	{
		Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() //Run this thread every 100sec
        {
            @Override
            public void run() 
            {
            	try
        		{
            		Registry reg = LocateRegistry.getRegistry(BackupI.PrimaryServerPort);
        			BackupI server = (BackupI) reg.lookup(BackupI.PrimaryServerServiceName);
        			available = true;
    				System.out.println("Primary server is alive");
    				
    				if (flag == 1)
    					upload();
        		}
        		
        		catch (RemoteException | NotBoundException e) 
        		{
        			System.out.println("Primary server is dead");
        			available = false;
        		}
            }
        }, 0, 20000);
	}
	
	public void retrieve() //get history from local storage
	{
		try 
		{
			URL url = ChatNode.class.getResource("LocalStorage/"+myService+"/");
	        PATH = url.getPath();
	        
			File f = new File(PATH + "/"+myService); 
			if (f.exists())
			{
			System.out.println("\nLoading previous messages from local storage..");
			BufferedReader br = new BufferedReader(new FileReader(f));
		 
		 	String st; 
			while ((st = br.readLine()) != null) 
			System.out.println(st);
			}
			
			else
			{
				System.out.println("\nNo previous messages in local storage..");
				
				//call backup server
				download();
			}
			
		} 
		
		catch (IOException e) { e.printStackTrace(); } 
	}
	
	public void download() //Download history from server
	{
		
		try
		{
			Registry reg;
			BackupI server;
			if (available)
			{
				reg = LocateRegistry.getRegistry(BackupI.PrimaryServerPort);
				server = (BackupI) reg.lookup(BackupI.PrimaryServerServiceName);
			}
			else //call secondary
			{
				reg = LocateRegistry.getRegistry(BackupI.SecondaryServerPort);
				server = (BackupI) reg.lookup(BackupI.SecondaryServerServiceName);
			}
			
			
			SerialilzableFile fs = server.getBackup(myService);
			
			if (fs == null)
			{
				System.out.println("\nNo previous messages in server storage..");
			}
			else
			{
				System.out.println("\nLoading previous messages from server storage..");
				
				URL url = ChatNode.class.getResource("LocalStorage/"+myService+"/");
		        PATH = url.getPath();
		        
		        System.out.println(myService+" Saving "+fs.getName()+" file in "+PATH);
		        
		        File localFile = new File(PATH+"/" +fs.getName());
//		        if(!localFile.exists()) { localFile.getParentFile().mkdir(); }

		        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(localFile,true));
	            out.write(fs.getData(), 0, fs.getData().length);
	            out.flush();
	            out.close();
	            
	            retrieve();
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
		catch (NotBoundException e) {e.printStackTrace();} 
	}
	
	public void save () throws RemoteException //Save history to local storage
    {
		if (!backupQ.isEmpty())
		{
			try 
			{
			  URL url = ChatNode.class.getResource("LocalStorage/"+myService+"/");
	          PATH = url.getPath();
		        
			  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(PATH + myService, true)));
//		      System.out.println(backupQ.size());
			  while(backupQ.size()!=0)
		      {
		    	    out.println(backupQ.peek().sender);
		    	    out.println(backupQ.peek().msg);
		    	    
//		    	    System.out.println(backupQ.peek().msg);
		    	    backupQ.remove();
		      }
		      
		      out.close();
		      System.out.println("Loacl storage backup history updated.");
		      
		      upload();
		    	  
		    } 
			
			catch (IOException e) {e.printStackTrace();}
		}
		
    }
	
	public void upload() //Upload history to server
	{
		SerialilzableFile fs = new SerialilzableFile();
		
		URL url = ChatNode.class.getResource("LocalStorage/"+myService+"/");
        PATH = url.getPath();
        
		File f = new File(PATH + "/"+myService); 
		int fileSize = (int) f.length();
	    byte[] buffer = new byte[fileSize];
 
		try 
		{
			Registry reg;
			BackupI server;
			
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            in.read(buffer, 0, buffer.length);
            in.close();
            
            fs.setData(buffer);
            fs.setName(myService);
            fs.setLastModifiedDate(new Date(f.lastModified()));
			
            if (available)
			{
				reg = LocateRegistry.getRegistry(BackupI.PrimaryServerPort);
				server = (BackupI) reg.lookup(BackupI.PrimaryServerServiceName);
			}
			else //call secondary
			{
				reg = LocateRegistry.getRegistry(BackupI.SecondaryServerPort);
				server = (BackupI) reg.lookup(BackupI.SecondaryServerServiceName);
			}
			
			 boolean res = server.setBackup(myService,fs);

		        if(res)
		        	 System.out.println("Server backup history updated.");
		        else
		            System.out.println("An error occurred. Try again later");
		        
		        System.exit(1);
		} 
		
		catch (RemoteException e) {e.printStackTrace();} 
		catch (NotBoundException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
		
	}
	
	public void multicastMessages(Messages msg) throws RemoteException, NotBoundException 
	{
//		System.out.println("Multicasting message ...");
		boolean delay = false;
		for(int i=0; i<num; i++)
		{
//			if(delay && msg.sender.equals("Nada")) 
//			{ // Delay Sending msg from Nada to Ghaith
//				if(i == 2) 
//				{
//					Delayer delayer = new Delayer(ports[i], services[i], msg);
//					new Thread(delayer).start();
//					continue;
//				}
//			}
			
			Registry reg = LocateRegistry.getRegistry(ports[i]);
			NodeI node = (NodeI) reg.lookup(services[i]);
			node.sendMsg(msg);
		}
//		displayMessages();
		
//		System.out.println("---multicastMessages");
//		displayAcks();
//		System.out.println("Multicasting done successfully");
	}
	
	@Override
	public void sendMsg(Messages msg) throws RemoteException, NotBoundException 
	{
//		System.out.println("Sending message ...");
		msgsQ.add(msg); //Adding to priority Q
		
		if(!msg.sender.equalsIgnoreCase(myService)) //only A-> B  &&  A->C
		{
			myLocalClock = Math.max(myLocalClock, msg.clock) + 1; //A:Clock = 1   B:Clock = 2
		}
		
//		displayMessages();
//		System.out.println("---sendMsg");
//		displayAcks();
		multicastAck(msg);
//		System.out.println("Sending done successfully");
	}
	
	private void multicastAck(Messages msg) throws RemoteException, NotBoundException 
	{
//		System.out.println("MultiAcking messages ...");
		boolean delay = false;
		for(int i=0; i<num; i++)
		{
//			if(delay && myService.equals("Nada")) 
//			{ // Delay Sending Ack from Nada to Ghaith
//				if(i == 2) 
//				{
//					System.out.println("|||||||||||||||||||||||||||||||||||||||||");
//					AckDelayer ackDelayer = new AckDelayer(ports[i], services[i], msg);
//					new Thread(ackDelayer).start();
//					continue;
//				}
//			}
			
			Registry reg = LocateRegistry.getRegistry(ports[i]);
			NodeI node = (NodeI) reg.lookup(services[i]);
			node.ack(msg);
		}
//		displayMessages();
//		System.out.println("---multicastAcks");
//		displayAcks();
//		System.out.println("Multiacking done successfully");
	}
	
	@Override
	public void ack(Messages msg) throws RemoteException 
	{
		if(msgsAcks.containsKey(msg.msgId))
		{
			msgsAcks.put(msg.msgId, msgsAcks.get(msg.msgId) + 1);
		}
		
		else 
			msgsAcks.put(msg.msgId, 1);
		System.out.println("---------------Acks--------------");
		System.out.println("Node:"+myService+" Acked the incoming message");
	}
	
	public void fetchNewMessages() throws RemoteException 
	{
		if(msgsQ.size() > 0 && msgsAcks.containsKey(msgsQ.peek().msgId) 
				&& msgsAcks.get(msgsQ.peek().msgId) == num)
		{
			System.out.println("Fetching New Message ...");
			
			backupQ.add(msgsQ.peek());
			displayMessages();
//			displayAcks();
			
			Messages msg = msgsQ.poll();
			msgsAcks.remove(msg.msgId);
			// msgsQ.remove(msg.msgId);
			
//			System.out.println("After Fetch New Msgs:");
//			displayAcks();
		}
	}
	
	private void displayMessages() 
	{
		System.out.println("---------------Messages--------------");
		for(Messages msg: msgsQ) 
		{
			System.out.println("Reciever: "+myService+"   Local Clock : "+myLocalClock);
			System.out.println("Msg Clock: "+msg.clock+"   Message ID : "+msg.msgId);
			System.out.println("Sender: "+msg.sender+"\nMessage: "+msg.msg);
			System.out.println("\n");
				
//			displayAcks();
		}
//		System.out.println("\n");
	}
	
	private void displayAcks()
	{
		System.out.println("---------------Acks--------------");
		Set<String> keys = msgsAcks.keySet();
		for(String k: keys)
		{
			System.out.println("Message ID: "+k+"  #Acks: "+msgsAcks.get(k));
		}
		System.out.println("\n");
	}
	
}
