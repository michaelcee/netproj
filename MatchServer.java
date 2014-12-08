package netproj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;


/**
 * 
 * @author MIKE
 *
 */
public class MatchServer extends FIARMsg{	
	
	public MatchServer(){
		
	}
	
	private void startServer() throws IOException{
		int port = 8484;
		ServerSocket se = new ServerSocket(port);
		boolean keepGoing = true;

		FIARServer fs = null;
		boolean waiting = false;
		Socket prevSoc = null;
		System.out.println("Server started.  Listening for clients");
		while(keepGoing){
			
			Socket incomingSoc = se.accept();
			System.out.print(">>new connection: ");
			//client connected.  clients will immediately announce whether or
			//not they're connecting for single or multi player:
			BufferedReader rdr = new BufferedReader(new InputStreamReader(
					incomingSoc.getInputStream()));

			boolean singlePlayer = Boolean.valueOf( getPayload(rdr.readLine()) );
			
			
			if(singlePlayer){
				//launch a new single player:
				System.out.println("new SinglePlayer game");
				fs = new FIARServer(true);
				fs.setSocket(true, incomingSoc);
				new Thread(fs).start();
			} else if(waiting) {
				//put the new player into spot 2 of waiting game and start it:
				System.out.print("P2 joined.");
				if(testSocket(prevSoc)){
					System.out.println("  Matched with P1.  Starting game");
					fs = new FIARServer(false);
					fs.setSocket(true, prevSoc);
					fs.setSocket(false, incomingSoc);
					new Thread(fs).start();
					waiting = false;
					
				} else {
					System.out.println("  But P1 has left.  P2 is now P1.  Waiting on new P2");
					prevSoc = incomingSoc;
				}
								
			} else {
				System.out.println("P1 joined");
				prevSoc = incomingSoc;
				waiting = true;
			}				
			
		}
		
		se.close();
		
	}
	
	private boolean testSocket(Socket soc){
		try{
			BufferedWriter wtr= new BufferedWriter(new OutputStreamWriter(
					soc.getOutputStream()));
			wtr.write(createMsg(Prefix.ACK, ""));
			wtr.newLine();
			wtr.flush();
			BufferedReader rdr = new BufferedReader(new InputStreamReader(
					soc.getInputStream()));
			Prefix p = getPrefix(rdr.readLine());
			if(p == Prefix.ACK)
				return true;
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	
	public static void main(String[] args) throws Exception{
		MatchServer ms = new MatchServer();
		ms.startServer();
	}//end Main	
	
}//end Caron_server
