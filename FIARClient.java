package netproj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class FIARClient extends FIARMsg {
	/** a lock on transmitting messages from player clicks */
	private boolean ready;
	private final BoardPanelGrid bpg;
	private boolean keepGoing = true;
	private int playerNum;
	Socket soc;
	BufferedWriter wtr;
	BufferedReader rdr;
	
	public FIARClient(){
	
		bpg = new BoardPanelGrid("Five in a Row", this);
		bpg.launch();
	}
	
	private void processMsg(String msg){
		switch(getPrefix(msg)){
		case INIT:
			playerNum = Integer.valueOf(getPayload(msg));
			bpg.setPlayer(playerNum);
			bpg.setMsg("Game session started.  You are Player " + (playerNum + 1));
			break;
		case GAME_OVER:{
			String[] winningCoords = getPayload(msg).split(DELIM);
			if(winningCoords.length != 1){
				int[][] coords = new int[winningCoords.length][2];
				for(int i = 0; i < winningCoords.length;i++){
					int[] pair = getCoords(winningCoords[i]);
					coords[i][0] = pair[0];
					coords[i][1] = pair[1];
				}
				bpg.endGame(coords);
			} else {
				bpg.endGameDraw();
			}
		}//using brackets to control var scope
			break;
		case NEW_GAME:
			bpg.newGame();
			bpg.setMsg("New game starting");
			break;
		case PROMPT:
			bpg.setMsg("Your move");
			ready = true;
			break;
		case SPOT_CHANGE:
			String tickerMsg;
			String[] payload = getPayload(msg).split(DELIM);
			int player = Integer.valueOf(payload[0]);
			if(player == playerNum)
				tickerMsg = "You ";
			else
				tickerMsg = "Your opponent ";
			tickerMsg += "took spot: " + payload[1];
			
			int[] coords = getCoords(payload[1]);
			bpg.changeSpot(player, coords[0], coords[1]);
			bpg.setMsg(tickerMsg);
			break;
		case ACK:
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
	 * 
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
	
	String getMsg(){
		try {
			return rdr.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
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
	

	public static void main(String[] args) {
		
		FIARClient client = new FIARClient();
		
	}
	

}
