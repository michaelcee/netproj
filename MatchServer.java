package netproj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
		System.out.println("Server started.  Listening for clients");
		while(keepGoing){
			
			Socket incomingSoc = se.accept();
			System.out.print(">>new connection: ");
			//client connected.  clients will immediately announce whether or
			//not they're connecting for single or multi player:
			BufferedReader rdr = new BufferedReader(new 
					InputStreamReader(incomingSoc.getInputStream()));
			boolean singlePlayer = Boolean.valueOf( getPayload(rdr.readLine()) );
			
			if(singlePlayer){
				//launch a new single player:
				System.out.println("new SP");
				fs = new FIARServer(true);
				fs.setSocket(true, incomingSoc);
				new Thread(fs).start();
			} else if(waiting) {
				//put the new player into spot 2 of waiting game and start it:
				System.out.println("P2 joined");
				fs.setSocket(false, incomingSoc);
				new Thread(fs).start();
				waiting = false;
			} else {
				//start a new game and put the new player in spot 1 and wait:
				System.out.println("P1 joined");
				fs = new FIARServer(false);
				fs.setSocket(true, incomingSoc);
				waiting = true;
			}				
			
			//must close this, otherwise we'd have a leak for each connection
			//rdr.close();
			
		}
		
		se.close();
		
	}

	
	public static void main(String[] args) throws Exception{
		MatchServer ms = new MatchServer();
		ms.startServer();
	}//end Main	
	
}//end Caron_server
