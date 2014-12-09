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
	protected Socket[] socs;
	BufferedWriter[] wtr;
	BufferedReader[] rdr;
	Thread singleWaiter;
	private boolean single;
	public FIARServer(boolean singlePlayer){
		single = singlePlayer;
		int i = (singlePlayer) ? 1 : 2;
		rdr = new BufferedReader[i];
		wtr = new BufferedWriter[i];
		socs = new Socket[i];
	}
	
	public boolean isSingle(){
		return single;
	}
	
	
	/**
	 * set p1 or p2's sockets 
	 * @param socket
	 */
	public void setSocket(boolean p1, Socket socket){
		if(p1)
			socs[0] = socket;
		else
			socs[1] = socket;
	}
	
	/**
	 * @param player: 0 for player 1, 1 for player 2
	 * @throws IOException 
	 */
	private void initComms(int player) throws IOException{
		rdr[player] = new BufferedReader(new InputStreamReader(socs[player].getInputStream()));		
		wtr[player] = new BufferedWriter(new OutputStreamWriter(socs[player].getOutputStream()));
		
	}
	
	/**
	 * 
	 */
	private void launchMultiGame(){
		try{
			initComms(0);
			initComms(1);
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
	
	private void launchSingle(){
		try{
			FIAR fiar = new FIAR();
			
			initComms(0);
			
			//tell the clients which player num they are:
			write(0, createMsg(Prefix.INIT, ""+0));			
			
			boolean p1 = true;//if it's p1's turn
			boolean newGame = true;
			int[] prevMove;
			while(newGame){
				int curPlayer = (p1) ? 0 : 1;
				//send out new game notification
					write(0, createMsg(Prefix.NEW_GAME, curPlayer + ""));
					fiar.newGame( p1 );

				boolean keepGoing = true;
				while(keepGoing){
					//throw out whatever may have been sent
					//whatever it is, we probably don't want to care about it
					while(rdr[0].ready())
						rdr[0].readLine();
					write(0, createMsg(Prefix.PROMPT, ""));
					String m;
					if(curPlayer == 0){
						m = rdr[0].readLine();
					} else {
						int[] xy = FIARRobot.getMove(fiar.getBoard());
						m = createMsg(Prefix.SPOT_CHANGE, makeCoords(
						 		xy[0], xy[1]));
					}
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
								
								curPlayer = (curPlayer + 1) % 2;
								
								break;
							case WINNING_MOVE:
								keepGoing = false;

								write(0, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								write(0, createMsg(Prefix.GAME_OVER, fiar.getWinningRun()));

								if(!newSingleGame()){
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

								write(0, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								write(0, createMsg(Prefix.GAME_OVER, ""));

								if(!newSingleGame()){
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
			rdr[0].close();
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
	
	private boolean newSingleGame(){
		try{
			if( (getPrefix(rdr[0].readLine()) == Prefix.NEW_GAME) ){
				return true;
			} else
				return false;
		
		} catch (IOException e){
			return false;
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
		if(single){
			launchSingle();
		} else {
			launchMultiGame();	
		}
	}

}//end FIARserver
