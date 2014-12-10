package netproj;

import java.net.*;
import java.io.*;

/**
 * FIARServer works in tandem with FIAR to provide an implementation of the 
 * "Five in a Row" game.  FIAR holds the gameboard and FIARServer passes
 * messages to and from the players and the game over the network.  In the case
 * of single player, there is only one connected player FIARServer passes the
 * gameboard and player information to a FIAR-playing "robot" we made.
 * 
 * FIARServer also keeps track of whose turn it is.
 *
 */
public class FIARServer extends FIARMsg implements Runnable{

	//store resources in arrays of size 1 (single player) or 2 (multi player):
	private Socket[] socs;
	private BufferedWriter[] wtr;
	private BufferedReader[] rdr;
	
	/**if we're in single player mode*/
	private boolean singleMode;
	
	/**
	 * construct an instance of a single or multiplayer game.  
	 * 
	 * Once an instance is initialized, callers need to set the P1 socket and
	 * possibly the P2 socket, if in multiplayer.  The game thread will then
	 * need to be created, since this is an implementation of Runnable.
	 * @param singlePlayer
	 */
	public FIARServer(boolean singlePlayer){
		singleMode = singlePlayer;
		int i = (singlePlayer) ? 1 : 2;
		rdr = new BufferedReader[i];
		wtr = new BufferedWriter[i];
		socs = new Socket[i];
	}
	
	/**
	 * report whether or not we're in single player mode
	 * @return
	 */
	public boolean isSingle(){
		return singleMode;
	}
	
	
	/**
	 * set p1 or p2's sockets.  
	 * 
	 * ideally
	 * @param socket
	 */
	public void setSocket(boolean p1, Socket socket){
		if(p1)
			socs[0] = socket;
		else if(!singleMode)
			socs[1] = socket;
	}
	
	/**
	 * initialize the Buffered readers and writers
	 * 
	 * @param player: 0 for player 1, 1 for player 2
	 * @throws IOException 
	 */
	private void initComms(int player) throws IOException{
		rdr[player] = new BufferedReader(new InputStreamReader(socs[player].getInputStream()));		
		wtr[player] = new BufferedWriter(new OutputStreamWriter(socs[player].getOutputStream()));
		
	}
	
	/**
	 * initialize the readers and writers and begin the main game loop.  multiple
	 * games can be played in succession from within this method. 
	 * 
	 */
	private void launchMultiGame(){
		try{
			//set up communications:
			initComms(0);
			initComms(1);
			
			//initialize the gameboard
			FIAR fiar = new FIAR();
			
			//tell the clients which player num they are:
			write(0, createMsg(Prefix.INIT, ""+0));
			write(1, createMsg(Prefix.INIT, ""+1));			
			/**if p1 went first this game; p1 always goes first on the first game*/
			boolean p1 = true;
			//if a new game is wanted
			boolean newGame = true;
			while(newGame){
				
				/**who the current player is; first move alternates by game*/
				int curPlayer = (p1) ? 0 : 1;
				
				//send out new game notifications:
				for(int i = 0; i < 2; i++){
					write(i, createMsg(Prefix.NEW_GAME, curPlayer + ""));
					fiar.newGame();
				}
				
				/**whether the game is still going or if it has ended*/
				boolean keepGoing = true;
				while(keepGoing){
					//throw out whatever may have been sent
					//whatever it is, we probably don't want to care about it
					while(rdr[curPlayer].ready())
						rdr[curPlayer].readLine();
					
					//prompt the current player to make their move:
					write(curPlayer, createMsg(Prefix.PROMPT, ""));
					
					//read response
					String m = rdr[curPlayer].readLine();
					
					//every message has a prefix; use it to determine what to do
					switch(getPrefix(m)){
						case GAME_OVER:		//unused
							break;
						case NEW_GAME:		//unused
							break;
						case PROMPT:		//unused
							break;
						case SPOT_CHANGE:	//player has made a move
							int[] coords = getCoords(getPayload(m));
							//give move to game and store result for switch:
							MoveResult mr = fiar.makePlay(curPlayer, coords[0], coords[1]);
							switch( mr ){
							case VALID:
								//update each player then switch active player
								//game continues
								write(0, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								write(1, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								
								curPlayer = (curPlayer + 1) % 2;
								
								break;
							case WINNING_MOVE:
								//game over, update each player
								keepGoing = false;
								for(int i = 0; i < 2; i++){
									write(i, createMsg(Prefix.SPOT_CHANGE, 
											curPlayer + DELIM + makeCoords(coords[0], coords[1])));
									write(i, createMsg(Prefix.GAME_OVER, fiar.getWinningRun()));
								}
								
								//prompt players for new game
								if(!newGame()){
									newGame = false;
									
								}else{
									//if we want a new game, switch starting player
									p1 = !p1;
								}
								break;
							case INVALID:
								break;
							case ERROR:
								break;
							case DRAW_MOVE:
								keepGoing = false;
								//game over; update players:
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
	
	/**
	 * much like launchMultiGame, but uses a FIARRobot to come up with moves
	 * for the computer.  there is only one reader and writer used for 
	 * communications, since it's single player.  
	 * 
	 * in the future, we would have liked to refactored this and LaunchSinglePlayer 
	 * into several methods, since there's a lot of duplicated code.
	 * 
	 */
	private void launchSingleGame(){
		try{
			FIAR fiar = new FIAR();
			
			//just single player
			initComms(0);
			
			//init the lone player, always p1:
			write(0, createMsg(Prefix.INIT, ""+0));			
			
			boolean p1 = true;
			boolean newGame = true;
			int[] prevMove = null;
			while(newGame){
				//toggle who makes first move
				int curPlayer = (p1) ? 0 : 1;
				//send out new game notification
				write(0, createMsg(Prefix.NEW_GAME, curPlayer + ""));
				fiar.newGame();

				boolean keepGoing = true;
				while(keepGoing){
					//throw out whatever may have been sent
					//whatever it is, we probably don't want to care about it
					while(rdr[0].ready())
						rdr[0].readLine();
					String m;
					if(curPlayer == 0){
						write(0, createMsg(Prefix.PROMPT, ""));
						m = rdr[0].readLine();
					} else {
						//it's the robot's turn.  hand it a copy of the gameboard
						//and the player's previous move so it can make its choice
						int[] xy = FIARRobot.getMove(fiar.getBoard(), prevMove[0], prevMove[1]);
						
						//turn it into a regular FIARMsg and process as usual:
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
							prevMove = coords;
							MoveResult mr = fiar.makePlay(curPlayer, coords[0], coords[1]);
							switch( mr ){
							case VALID:
								//update the player
								write(0, createMsg(Prefix.SPOT_CHANGE, 
										curPlayer + DELIM + makeCoords(coords[0], coords[1])));
								
								//switch player
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
	
	/**
	 * convenience method to write to the relevant BufferedWriter.  
	 * 
	 * future expansion would be to implement "throws InvalidArgumentException" 
	 * for this method.  as of now, defensive coding is relied on
	 * @param player
	 * @param msg
	 */
	private void write(int player, String msg){
		
		//if it's single player, we can only be writing to player 1
		if(singleMode && player != 0)
			return;
		//we can only write to 0 and 1, anything greater is an error
		else if( player > 1)
			return;
		try {
			wtr[player].write(msg);
			wtr[player].newLine();
			wtr[player].flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * called after a game has ended.  poll only player 1 to see if they want
	 * to keep going.  this method blocks until an answer is received.  it is
	 * expected that upon receiving a "game over" message, clients will respond
	 * with whether or not they want to player another
	 * 
	 * @return whether or not client requested a new game
	 */
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
	/**
	 * just like above but poll both players.  
	 * 
	 * in the future, this and newSingleGame should be refactored into a single
	 * class and the "singleMode" flag used to determine whether or not to poll
	 * p1 and p2 or just p1
	 * 
	 * @return whether or not client requested a new game
	 */ 
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
	
	/**
	 * implements runnable so this object can run in its own thread and the 
	 * server can go back to listening for connections
	 */
	@Override
	public void run() {
		if(singleMode){
			launchSingleGame();
		} else {
			launchMultiGame();	
		}
	}

}//end FIARserver
