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
	Socket soc;
	BufferedWriter wtr;
	BufferedReader rdr;
	
	public FIARClient(){
	
		bpg = new BoardPanelGrid("Five in a Row", this);
		bpg.launch();
	}
	
	private void processMsg(String msg){
		System.out.println(msg);
		switch(getPrefix(msg)){
		case INIT:
			bpg.setPlayer(Integer.valueOf(getPayload(msg)));
			break;
		case SINGLE_PLAYER: 
			bpg.singlePlayer(Boolean.valueOf(getPayload(msg)));
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
			break;
		case PROMPT:
			ready = true;
			System.out.println("PROMPTED BY SERVER");
			break;
		case SPOT_CHANGE:
			String[] payload = getPayload(msg).split(DELIM);
			int[] coords = getCoords(payload[1]);
			bpg.changeSpot(Integer.valueOf(payload[0]), coords[0], coords[1]);
			break;
		default:
			break;
			
		}
	}
	
	void initComms(int port, String host){
		try {
			soc = new Socket(host, port);
			wtr = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
			rdr = new BufferedReader(new InputStreamReader(soc.getInputStream()));
			System.out.println("CONNECTED TO SERVER");
			
			//just keep listening and process messages as they come
			while(keepGoing){
				processMsg(rdr.readLine());
				
			} 
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	boolean sendMove(int x, int y){
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
	
	void reqSinglePlayer(){
		try {
			wtr.write(createMsg(Prefix.SINGLE_PLAYER, "true"));
			wtr.newLine();
			wtr.flush();
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
		client.initComms(8484, "localhost");
		
	}
	

}
