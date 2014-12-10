package netproj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;


/**
 * The main entry point to the server application.  This class serves as a simple
 * matchmaking service.  Clients connects and request single or multiplayer.  The
 * server then puts them into the relevant game and goes back to waiting for a new
 * client.
 *
 */
public class MatchServer extends FIARMsg{	
	
	public MatchServer(){
		
	}
	/**
	 * begins server matchmaking.  The server will listen indefinitely until
	 * shut down externally.
	 * 
	 * @throws IOException
	 */
	private void startServer() throws IOException{
		ChatServer.start();
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
	/**
	 * tests the given socket to see if there's still a connection.  this method
	 * creates a buffered reader, sends an ACK message and then waits for a response
	 * if the client does not ACK back or there's an issue, return false.
	 * 
	 * @param soc - socket to test
	 * @return whether or not we have a cooperative client
	 */
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
	}//end testSocket

	/**
	 * Launch point of server side application.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args){
		MatchServer ms = new MatchServer();
		try {
			ms.startServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//end Main	
	
}//end MatchServer
