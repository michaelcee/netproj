package netproj;

import java.io.IOException;
import java.net.*;


/**
 * 
 * @author MIKE
 *
 */
public class MatchServer {	
	
	public MatchServer(){
		
	}
	
	private void startServer() throws IOException{
		int port = 8484;
		ServerSocket se = new ServerSocket(port);
		boolean keepGoing = true;
		boolean loopCheat = false;
		FIARServer fs = new FIARServer();
		while(keepGoing){
			if(!loopCheat){
				System.out.println("player 1 joined");
				fs = new FIARServer();
				Socket soc = se.accept();
				fs.setSocket(true, soc);
				fs.promptSingle(true);
			} else {
				System.out.println("loop cheat");
				Socket soc = se.accept();
				fs.setSocket(false, soc);
				new Thread(fs).start();
				loopCheat = false;
			}
			
			Socket soc2 = se.accept();
			if(fs.isSingle()){
				System.out.println("player 2 denied, starting new FS");
				fs = new FIARServer();
				fs.setSocket(true, soc2);
				loopCheat = true;
			} else {
				System.out.println("player 2 joined");
				fs.setSocket(false, soc2);
				new Thread(fs).start();
			}
						
			
		}
		
		se.close();
		
	}
	
	public static void main(String[] args) throws Exception{
		MatchServer ms = new MatchServer();
		ms.startServer();
	}//end Main	
	
}//end Caron_server
