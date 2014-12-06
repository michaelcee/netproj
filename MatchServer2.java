package netproj;

import java.io.IOException;
import java.net.*;


/**
 * 
 * @author MIKE
 *
 */
public class MatchServer2 {	
	
	public MatchServer2(){
		
	}
	
	private void startServer() throws IOException{
		int port = 8484;
		ServerSocket se = new ServerSocket(port);
		boolean keepGoing = true;
		
		
		/**
		 * main matchmaking loop.  
		 * 
		 * inverted is true when
		 */
		boolean inverted = false;
		FIARServer fs = new FIARServer();
		while(keepGoing){
			/* player [a] joins inside 1st IF statement
			 * player [b] joins after 
			 * 
			 * if we're not inverted, it means there is no game waiting and 
			 * player a will join spot 1.  
			 * 
			 * otherwise, it means that on the previous loop around, player [a] 
			 * or [b] wanted to play single player and was put into a new game 
			 * spot 1.  player [a] in this case will go in spot 2.  this means 
			 * that there is a state where player [a] is in both spots
			 */
			if(!inverted){ 						
				fs = new FIARServer();
				Socket soc = se.accept();
				System.out.println("player [A] in spot 1");
				fs.setSocket(true, soc);
				fs.promptSingle(true);
			} else {
				Socket soc = se.accept();
				/*player [b] in spot 1 from the previous loop could want to play
				 * single player.  in which case, we initialize that game and
				 * then put player [a] into spot 1 of a new game
				 */
				if(fs.isSingle()){
					System.out.println("player [b] in spot 1 wants single player");
					fs.promptSingle(false);
					new Thread(fs).start();
					
					fs = new FIARServer();
					fs.setSocket(true, soc);
					fs.promptSingle(true);
					System.out.println("player [a] in spot 1 inverted");
				} else {
					fs.setSocket(false, soc);
					fs.promptSingle(false);
					new Thread(fs).start();		
					System.out.println("player [a] in spot 2 game init");
				}
				inverted = false;
			}
			
			Socket soc2 = se.accept();
			if(fs.isSingle()){
				System.out.println("player [A] single player");
				fs.promptSingle(false);
				new Thread(fs).start();
				fs = new FIARServer();
				fs.setSocket(true, soc2);
				fs.promptSingle(true);
				System.out.println("player [B] in spot 1; inverted");
				inverted = true;
			} else {
				System.out.println("player [B] in spot 2 game init");
				fs.promptSingle(false);
				fs.setSocket(false, soc2);
				new Thread(fs).start();
			}
						
			
		}
		
		se.close();
		
	}
	
	public static void main(String[] args) throws Exception{
		MatchServer2 ms = new MatchServer2();
		ms.startServer();
	}//end Main	
	
}//end Caron_server
