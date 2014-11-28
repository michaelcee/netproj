package netproj;

import java.net.*;
import java.io.*;

/**
 * an instance of this class represents a single instance of a game containing
 * two player sockets
 *
 */
public class FIARServer extends FIARMsg implements Runnable{
	/**connection to p1*/
	protected Socket socket1;
	/**connection to p2*/
	protected Socket socket2;
	BufferedWriter[] wtr;
	BufferedReader[] rdr;
	public FIARServer(){
	
	}
	/**
	 * set p1 or p2's sockets 
	 * @param socket
	 */
	public void setSocket(boolean p1, Socket socket){
		if(p1)
			socket1 = socket;
		else
			socket2 = socket;
	}
	
	/**
	 * 
	 */
	public void launchGame(){
		try{
			rdr = new BufferedReader[2];
			rdr[0] = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
			rdr[1] = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
			
			wtr = new BufferedWriter[2];
			wtr[0] = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
			wtr[1] = new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream()));			
			
			FIAR fiar = new FIAR();
			
			//tell the clients which player num they are:
			write(0, createMsg(Prefix.INIT, ""+0));
			write(1, createMsg(Prefix.INIT, ""+1));
			
			
			boolean p1 = true;//if it's p1's turn
			boolean newGame = true;
			while(newGame){
				int curPlayer = (p1) ? 0 : 1;
				//send out new game notifications
				for(int i = 0; i < 2; i++){
					write(i, createMsg(Prefix.NEW_GAME, curPlayer + ""));
					fiar.newGame( p1 );
				}
				
				boolean keepGoing = true;
				while(keepGoing){
					//throw out whatever may have been sent
					//whatever it is, we probably don't want to care about it
					while(rdr[curPlayer].ready())
						rdr[curPlayer].readLine();
					write(curPlayer, createMsg(Prefix.PROMPT, ""));
					String m = rdr[curPlayer].readLine();
					switch(getPrefix(m)){
						case GAME_OVER:
							break;
						case NEW_GAME:
							break;
						case PROMPT:
							break;
						case SPOT_CHANGE:
							int[] coords = getCoords(getPayload(m));
							switch( fiar.makePlay(curPlayer, coords[0], coords[1]) ){
							case VALID:
								//update each player then switch active player
								write(0, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								write(1, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								
								curPlayer = (curPlayer + 1) % 2;
								
								break;
							case WINNING_MOVE:
								keepGoing = false;
								for(int i = 0; i < 2; i++){
									write(i, createMsg(Prefix.SPOT_CHANGE, 
											curPlayer + DELIM + makeCoords(coords[0], coords[1])));
									write(i, createMsg(Prefix.GAME_OVER, fiar.getWinningRun()));
								}
								if(!newGame()){
									newGame = false;
									
								}else{
									p1 = !p1;
								}
								break;
							case INVALID:
								break;
							case ERROR:
								break;
							case DRAW_MOVE:
								keepGoing = false;
								for(int i = 0; i < 2; i++){
									write(i, createMsg(Prefix.SPOT_CHANGE, 
											curPlayer + DELIM + makeCoords(coords[0], coords[1])));
									write(i, createMsg(Prefix.GAME_OVER, ""));
								}
								if(!newGame()){
									newGame = false;
									
								}else{
									p1 = !p1;
								}
								break;
							default:
								break;
							}
							
							
							break;
						default:
							break;
						
					}//end of handle player messages
					
				}//end of keepGoing  (current game loop)
				
				
				
			}//end of new game loop
			
			//close resources:
			wtr[0].close();
			wtr[1].close();
			rdr[0].close();
			rdr[1].close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	private void write(int player, String msg){
		try {
			wtr[player].write(msg);
			wtr[player].newLine();
			wtr[player].flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private boolean newGame(){
		try{
			if( (getPrefix(rdr[0].readLine()) == Prefix.NEW_GAME) && 
				(getPrefix(rdr[1].readLine()) == Prefix.NEW_GAME)){
				return true;
			} else
				return false;
		
		} catch (IOException e){
			return false;
		}
	}
	
	
	@Override
	public void run() {
		launchGame();		
	}

}//end FIARserver
