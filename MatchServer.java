package netproj;

import java.net.*;


/**
 * 
 * @author MIKE
 *
 */
public class MatchServer {	
	
	public static void main(String[] args) throws Exception{
		int port = 8484;
		ServerSocket se = new ServerSocket(port);
		boolean keepGoing = true;
		while(keepGoing){
			Socket soc1 = se.accept();
			Socket soc2 = se.accept();
			FIARServer fs = new FIARServer();
			fs.setSocket(true, soc1);			
			fs.setSocket(false, soc2);
			new Thread(fs).start();
		}
		
		se.close();
		
	}//end Main	
	
}//end Caron_server
