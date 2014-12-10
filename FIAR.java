package netproj;

/**
 * this class represents an instance of the game 5 in a row.  the game board,
 * by default, is 10 by 10 spaces and is stored in a matrix of ints.
 * if a spot is 0, that means unoccupied.  otherwise, it will store 1 or 2
 * representing player 1 or player 2 
 * 
 * the game board is accessed using X and Y values, with the origin being the 
 * top left corner.  meaning, the top left has coordinates (0, 0) and the bottom
 * right has coordinates (9, 9).  However, the matrix itself is counterintuitively 
 * accessed using board[y][x] due to the nature of 2d arrays.
 * 
 * instances are reused for successive games but no data is preserved.  this 
 * class can be thought of as an intelligent real life game board that 
 * evaluates player moves but does not enforce the rules of the game; that is 
 * done by FIARServer, the other half of the implementation of "Five in a Row"
 *
 */

//extend FIARMsg for access to communication protocols
public class FIAR extends FIARMsg {
	
	private int[][] board; 	//2D array of the game board itself
	private int moves;		//if moves >= 100, the board is full and it's a draw
	String winningRun; 		//format: "x0,y0-x1,y1" 	

	/**
	 * init a new game, reset vars
	 */
	public void newGame(){
		board = new int[10][10];	
		moves = 0;
		winningRun = null;
	}
	
	/**
	 * creates a deep copy of the game board int[][] and returns it.  If not
	 * for a deep copy, then callers of this method could directly modify the 
	 * board object to horrible effect!
	 * 
	 * @return - a deep copy of the game board.  
	 */
	public int[][] getBoard(){
		int[][] retVal = new int[10][10];
		for(int i = 0; i < 10; i++){
			for(int j = 0; j < 10; j++){
				retVal[i][j] = board[i][j];
			}
		}
		return retVal;
	}
	
	//debugging use ONLY
	@SuppressWarnings("unused")
	private void setBoard(int[][] board){
		this.board = board;
	}
	/**
	 * returns the winningRun string. 
	 * the string is formated as x0,y0[DELIM]x1,y1
	 * 
	 * DELIM is defined in FIARMsg
	 * @return
	 */
	public String getWinningRun(){
		return winningRun;
	}
	
	/**
	 * the act of making a play on the game board.  the choice is evaluated and
	 * a FIARMsg.MoveResult enum is returned.
	 * 
	 * moves can be: 
	 * 	ERROR - specified spot is off the 10x10 board, such as (-4, 15)
	 * 	INVALID - spot is already taken
	 * 	WINNING_MOVE - this move is a five-in-a-row formation
	 * 	DRAW_MOVE - this is the 100th move and there's no winner.  ergo, a draw
	 * 	VALID - the move is valid and the game continues
	 * 
	 * @param player 0 for player 1, 1 for player 2
	 * @param x pos
	 * @param y pos
	 * @return MoveResult enum value
	 */
	public MoveResult makePlay(int player, int x, int y){

		if(x > 9 || x < 0 || y > 9 || 0 > y){
			return MoveResult.ERROR;
		}
		
		if(board[y][x] != 0){
			return MoveResult.INVALID;
		}
		
		//it's a valid move, so change board and update move count:
		board[y][x] = player + 1;
		moves++;
		
		//check if it's a winning move:
		if(evalSpot(x, y)){
			return MoveResult.WINNING_MOVE;
			
		//check if we've run out of spots:	
		} else if (moves >= 100) {
			return MoveResult.DRAW_MOVE;
			
		//otherwise, it's valid and game continues:	
		} else {
			return MoveResult.VALID;
		}
		
	}
	
	
	/**
	 * checks a position on the board for any 5-in-a-row formations involving
	 * this position.  
	 * 
	 * this method is intended to be called after a player has a made a move and
	 * the player's move is supplied to be checked.  otherwise, this would make a
	 * lot of duplicate work if we marched down each spot in the board and checked
	 * it for 5 in a row.  
	 * @param x
	 * @param y
	 * @return - whether or not this spot is part of a five-in-a-row
	 */
	private boolean evalSpot(int x, int y){
	/**
	 * 	need to check 4 directions of runs: 
	 * 	up/down, left/right, upLeft/downRight, upRight,downLeft
	 * 
	 * 	works like this:
	 * 
	 * start: count = 1 (the piece that we're checking)
	 * check each direction pair:
	 * 	choose direction (up, upRight etc)
	 * 	check for consecutive chips in that direction
	 *		count++ for each find
	 *	check for consecutive chips in opposite direction
	 *		count++ for each find
	 *	if count >= 5 return true
	 *	else check the next direction
	 *		if all directions checked, return false
	 */
		//dunno why you'd check an empty spot, but yeah, it's empty thus no winner
		if( board[y][x] == 0)
			return false;
		if(		checkDir(x, y, 1, 1)  	//downRight/upLeft
			|| 	checkDir(x, y, 1, -1)	//upRight/downLeft
			|| 	checkDir(x, y, 1, 0) 	//right/left
			|| 	checkDir(x, y, 0, 1)	//down/up
		) return true;
		
		return false;
	}
	
	/**
	 * checks a single direction from a given spot on the board for a 5 in a row
	 * match.  
	 * 
	 * if found, it updates the winningRun string
	 * 
	 * @param xPos 	x coordinate
	 * @param yPos 	y coordinate
	 * @param ySp	1 for down, -1 for up
	 * @param xSp	1 right, -1 for left
	 * @return
	 */
	private boolean checkDir(int xPos, int yPos, int ySp, int xSp){
		StringBuilder sb = new StringBuilder(xPos + "," + yPos);
		int p = board[yPos][xPos];
		int totalCount = 1;
		
		short doItTwice = 2;
		while(doItTwice-- > 0){
			int count = 0;
			//vars for inner looping
			int yPosT = yPos; 
			int xPosT = xPos;
			boolean keepGoing = true;
			while(keepGoing){
				yPosT += ySp;
				xPosT += xSp;
				/*
				if we've gone into the negatives at all, 
				we can't go any further in this direction
				*/
				if(yPosT > 9 || yPosT < 0 || xPosT > 9 || xPosT < 0){
					keepGoing = false;
				} else if(board[yPosT][xPosT] != p) {
					keepGoing = false;
				} else{
					sb.append(DELIM+xPosT+","+yPosT);
					count++;
				}
						
			}
			
			//change to complement
			xSp *= -1;
			ySp *= -1;
			totalCount += count;
				
		}//end of doItTwice
		
		if(totalCount >= 5){
			winningRun = sb.toString();
			return true;
		}else
			return false;
	}
	
	/**
	 * test test test
	 * @param args
	 */
	public static void main(String[] args) {
		
		//test board::
		int[][] board = { {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},	
						  
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 	0, 0, 0, 0, 0}
						};
				
		FIAR f = new FIAR();
		//f.setBoard(board);
		System.out.println(
				f.makePlay(1, 0, 0)
		);
	
	}//end of main
	
}//end of class


