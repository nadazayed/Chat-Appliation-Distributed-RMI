import java.io.Serializable;

public class Messages implements Comparable<Messages>, Serializable
{
	private static final long serialVersionUID = 1L;
	
	String msgId, sender, msg;
	int nodeId, clock;
	
	public Messages(String msgId, int nodeId, String sender, int clock, String msg) 
	{
		this.msgId = msgId;
		this.nodeId = nodeId;
		this.sender = sender;
		this.clock = clock;
		this.msg = msg;
	}
	
	@Override
	public String toString() 
	{
		return "Sender: "+sender + " "+
				"Message: " + msg + " "+
				"Logical Clock = " + clock;
	}
	
	
		@Override
		public int compareTo(Messages msg) // Total Order using logical clocks
		{
			// Tie Breaker
			if(this.clock == msg.clock)
				return this.nodeId - msg.nodeId;
			return this.clock - msg.clock;
		}
}
