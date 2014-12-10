package netproj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * This class provides communications services to the UI.  It also the main
 * entry point into the client-side application.  The UI is launched and this
 * class waits for connection initialization using the fields from the UI.  Once
 * a request to connect has been made, a new thread is started to listen for
 * messages and they are processed accordingly.  
 *
 */
public class FIARClient extends FIARMsg {
	/** a lock on transmitting messages from player clicks unless we're ready */
	private boolean ready;
	/** an instance of our UI */
	private final BoardPanelGrid bpg;
	//more or less a constant:
	private boolean keepGoing = true;
	/**which player num, given by the server, this client is */
	private int playerNum;
	//resources for communications:
	Socket soc;
	BufferedWriter wtr;
	BufferedReader rdr;
	
	public FIARClient(){
		//create and launch GUI
		bpg = new BoardPanelGrid("Five in a Row", this);
		bpg.launch();
	}
	
	/**
	 * reads messages from the server and handles them as necessary.  like the
	 * server counterpart, the FIARMsg Prefix is used as a switch statement to
	 * determine what should be done with the information.  in almost all cases,
	 * the UI is updated to notify the player.
	 * @param msg
	 */
	private void processMsg(String msg){
		switch(getPrefix(msg)){
		case INIT:
			//initializes the game and lets the player know which number they are
			playerNum = Integer.valueOf(getPayload(msg));
			bpg.setPlayer(playerNum);
			bpg.setMsg("Game session started.  You are Player " + (playerNum + 1));
			break;
		case GAME_OVER:{
			//game is over.  if there is no payload to the message, it means the
			//game is a draw.  
			
			//the payload looks like 4,5-4,6-4,7-4,8-4,9  ('-' is the DELIM)
			//so split it to {"4,5", "4,6", "4,7", "4,8", "4,9"}
			String[] winningCoords = getPayload(msg).split(DELIM);
			if(winningCoords.length != 1){
				//create a list of ordered pairs to store the winning run of spots
				int[][] coords = new int[winningCoords.length][2];
				
				//fill the list:
				for(int i = 0; i < winningCoords.length;i++){
					int[] pair = getCoords(winningCoords[i]);
					coords[i][0] = pair[0];
					coords[i][1] = pair[1];
				}
				//send the list of winning spots to the UI:
				bpg.endGame(coords);
			} else {
				//it's a draw, update ui
				bpg.endGameDraw();
				bpg.setMsg("Game over - no moves left.  No winner.");
			}
		}//using brackets to control var scope
			break;
		case NEW_GAME:
			bpg.newGame();
			bpg.setMsg("New game starting");
			break;
		case PROMPT:
			//if clients don't receive a PROMPT message, no clicks will be 
			//transmitted to server.  ready must = true to send a move request
			bpg.setMsg("Your move");
			ready = true;
			break;
		case SPOT_CHANGE:
			//a spot has changed its owner.  update the UI
			String tickerMsg;	//message to shown in the UI ticker
			
			//the payload of SPOT_CHANGE is <player num>-<spot x,y>, so we need
			//to split it one more time:
			String[] payload = getPayload(msg).split(DELIM);
			
			//payload[0] IDs the player
			int player = Integer.valueOf(payload[0]);
			if(player == playerNum)
				tickerMsg = "You ";
			else
				tickerMsg = "Opponent ";
			//payload[1] is the "x,y" ordered pair
			tickerMsg += "took spot: " + payload[1];
			
			int[] coords = getCoords(payload[1]);
			
			//update UI:
			bpg.changeSpot(player, coords[0], coords[1]);
			bpg.setMsg(tickerMsg);
			break;
		case ACK:
			//server heartbeat-style message.  respond with an ACK back
			System.out.println(">ACK from server");
			respAck();
		default:
			break;
			
		}
	}
	/**
	 * start a new thread to listen for communications from the server.  
	 * 
	 * this is intended to be called from the UI so a new thread was necessary 
	 * otherwise the UI itself would completely freeze waiting for messages and
	 * stuff.    
	 * @param port - this should be hardwired to 8484 for now
	 * @param host - can be IP address or server's name
	 */
	void initComms(final int port, final String host){

		try {
			soc = new Socket(host, port);
			rdr = new BufferedReader(new InputStreamReader(soc.getInputStream()));
			wtr = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		bpg.setMsg("connected to server");
    	new Thread( new Runnable(){
    		public void run(){
				//just keep listening and process messages as they come
				while(keepGoing){
					try {
						String msg = rdr.readLine();
						if(null != msg){
							System.out.println(">received: " + msg);
							processMsg(msg);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
    		}

    	}).start();
	}
	
	
	/**
	 * send a move to the server and flip the "ready" flag.  to send another move,
	 * a "PROMPT" message will have to be received from the server
	 * @param x
	 * @param y
	 * @return
	 */
	boolean sendMove(int x, int y){
		System.out.println("sending move for spot: " + makeCoords(x,y));
		try {
			wtr.write(createMsg(Prefix.SPOT_CHANGE, makeCoords(x,y)));
			wtr.newLine();
			wtr.flush();
			ready = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
			
		}
		
		return true;
	}
	/**
	 * convenience method to respond to an ACK request back to the server
	 */
	private void respAck(){
		try{
			wtr.write(createMsg(Prefix.ACK, ""));
			wtr.newLine();
			wtr.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}
	
	
	/**
	 * send a message requesting single player mode.  this or reqMultiPlayer 
	 * is intended to be called immediately after establishing connection.
	 * 
	 * future work would be to refactor this an reqMultiPlayer into a single
	 * method which takes a boolean argument
	 */
	void reqSinglePlayer(){
		try {
			wtr.write(createMsg(Prefix.SINGLE_PLAYER, "true"));
			wtr.newLine();
			wtr.flush();
			bpg.setMsg("Starting single player");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * send a message requesting mutli player mode.  this or reqSinglePlayer 
	 * is intended to be called immediately after establishing connection.
	 * 
	 * future work would be to refactor this an reqSinglePlayer into a single
	 * method which takes a boolean argument
	 */
	void reqMultiPlayer(){
		try {
			wtr.write(createMsg(Prefix.SINGLE_PLAYER, "false"));
			wtr.newLine();
			wtr.flush();
			bpg.setMsg("Waiting for oponent");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * inform the server whether or not we want to play another game
	 * @param yes
	 */
	void requestNew(boolean yes){
		try {
			if(yes)
				wtr.write(createMsg(Prefix.NEW_GAME, ""));
			else
				wtr.write(createMsg(Prefix.GAME_OVER, ""));
			wtr.newLine();
			wtr.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * lets us know whether or not we're ready to listen to what the
	 * player has to say.  this is set by a "PROMPT" message from the server.
	 * 
	 * the "ready" flag prevents clients from flooding the server with invalid
	 * requests.  IE, if the client could click a spot multiple times, and each
	 * time a move request was sent to the server, we'd be wasting resources.  
	 * the server is aware of whose turn it is so there's no way to dupe the
	 * server into making a move out of turn.  
	 * @return
	 */
	public boolean ready(){
		return ready;
	}
	
	/**
	 * main entry point.  Port num is hardwired and the host is specified in the UI
	 * so there's no need for arguments here.  however, future work could entail
	 * dynamic port settings but that's not the case at the moment
	 * @param args
	 */
	public static void main(String[] args) {
		
		FIARClient client = new FIARClient();
		
	}
	

}
