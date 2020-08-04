import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ReplicaNode extends UnicastRemoteObject implements ReplicaI
{
	int myPort;
	String myHost, myService;
	
	public static String PATH;
	static int replicaId = 3;
	
	public static void main(String[] args) throws RemoteException
	{
		ReplicaNode replica = new ReplicaNode(replicaId); 
		 
		 Registry registry = LocateRegistry.createRegistry(ReplicaI.ports[replicaId]);
	     registry.rebind(ReplicaI.services[replicaId], replica);
	}
	
	public ReplicaNode(int nodeId) throws RemoteException 
	{
		myHost = hostnames[nodeId];
		myPort = ports[nodeId];
		myService = services[nodeId];
		
		URL url = ChatNode.class.getResource("ServerStorage/"+myService+"/");
        PATH = url.getPath();
        
        System.out.println("id:"+nodeId+" "+myService+" host:"+myHost+" port:"+myPort+" local storage:"+PATH+"\n");
	}
	
	
	@Override
	public boolean uploadFile(SerialilzableFile fs) 
    {
		URL url = ChatNode.class.getResource("ServerStorage/"+myService+"/");
        PATH = url.getPath();
        
        System.out.println(myService+" Saving "+fs.getName()+" file in "+PATH);
        
        File localFile = new File(PATH+"/" +fs.getName());
//        if(!localFile.exists()) { localFile.getParentFile().mkdir(); }

        try
        {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(localFile));
            out.write(fs.getData(), 0, fs.getData().length);
            out.flush();
            out.close();
            return true;
        }

        catch (FileNotFoundException e) { System.out.println("File not found"); }
        catch (IOException e) { System.out.println("IO error"); }

        return false;
    }
	
	@Override
	public SerialilzableFile downloadFile(String chatNode)
	{
		System.out.println("Getting "+chatNode+" File ...");
		
		URL url = ChatNode.class.getResource("ServerStorage/"+myService+"/");
        PATH = url.getPath();
		try
        {
            File f = new File(PATH+"/" +chatNode);
            SerialilzableFile fs = new SerialilzableFile();

            int fileSize = (int) f.length();
            byte[] buffer = new byte[fileSize];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            in.read(buffer, 0, buffer.length);
            in.close();

            fs.setData(buffer);
            fs.setName(chatNode);

            return fs;
        }

		 catch (FileNotFoundException e) { System.out.println("File not found"); }
        catch (IOException e) { System.out.println("IO error"); }

        return null;
	}


}
