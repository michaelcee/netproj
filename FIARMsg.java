package netproj;

/**
 * helper class for creating messages.  both clients and servers
 * should extend FIARMsg.  this includes the FIAR class itself as well.
 * 
 * this allows easy access to these methods and also protects the 
 * communication scheme from whatever it is we're supposed to protect stuff from
 *	
 * messages are constructed as a PREFIX_PAYLOAD
 */
public abstract class FIARMsg {
	
	/**
	 * all messages have a prefix.  future prefixes can be added and the switch
	 * statements expanded if necessary.
	 *
	 */
	protected enum Prefix{
		ACK,			//utility prefix
						//used for heartbeat-style checks
		
		INIT,			//the session has been initiated
						//from server only
						//payload: player number (0 or 1)
		
		PROMPT, 		//notify client of their turn
						//from server only
						//payload: none
		
		SPOT_CHANGE,	//update a spot on the board
						//both directions
						//payload: <player>-x,y
		
		GAME_OVER,		//game is over
						//from server only
						//payload: <player>-winning run in x0,y0-x1,y1 form
		
		SINGLE_PLAYER,	//request a single player game
						//both directions
						//no payload.  prompt from server and response from player
						//for optional single player mode
		
		NEW_GAME		//new game starting
						//both directions
						//payload: none
	}
	
	/**
	 * the result of a move.  this is used server-side exclusively for now 
	 *
	 */
	protected enum MoveResult{
		VALID,
		WINNING_MOVE,
		DRAW_MOVE,
		INVALID,
		ERROR
	}
	
	/**universal delimiter*/
	protected static final String DELIM = "-";
	
	/**
	 * gets the prefix of the given message
	 * @param msg
	 * @return
	 */
	protected static Prefix getPrefix(String msg){
		//split at the delimiter and return the first element from the array:
		return Prefix.valueOf(msg.split(DELIM, 2)[0]);
	}
	
	/**
	 * gets the payload of the given message
	 * @param msg
	 * @return
	 */
	protected static String getPayload(String msg){
		//split at the delimiter and return the second element from the array:
		return msg.split(DELIM, 2)[1];
	}
	
	
	/**
	 * create a message from the given Prefix and String
	 * @param prefix
	 * @param payload
	 * @return
	 */
	protected static String createMsg(Prefix prefix, String payload){
		return prefix + DELIM + payload;
	}
	/**
	 * given an ordered pair in the form "x,y" returns the int values in an
	 * array of size 2
	 * @param payload string in the form "x,y"
	 * @return a 2 element array where 0 = x, 1 = y
	 */
	protected static int[] getCoords(String payload){
		int[] retVal = new int[2];
		String[] vals = payload.split(",");
		retVal[0] = Integer.valueOf(vals[0]);
		retVal[1] = Integer.valueOf(vals[1]);
		
		return retVal;
		
	}
	/**
	 * creates an ordered pair from the supplied ints
	 * @param x
	 * @param y
	 * @return a string formatted as "x,y"
	 */
	protected static String makeCoords(int x, int y){
		return String.valueOf(x) + "," + String.valueOf(y);
	}
	
}
