package netproj;

import java.util.ArrayList;

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
 * instances are reused for successive games
 * @author MIKE
 *
 */

public class FIAR extends FIARMsg {
	
	private int[][] board;
	private int moves;
	String winningRun; //format: "x0,y0-x1,y1" 	
	public FIAR(){
		
	}
	
	/**
	 * init a new game
	 * @param p1 - is player 1 making the first move?
	 */
	public void newGame(boolean p1){
		board = new int[10][10];	
		moves = 0;
		winningRun = null;
	}
	
	public int[][] getBoard(){
		return board;
	}
	
	//debugging use ONLY
	@SuppressWarnings("unused")
	private void setBoard(int[][] board){
		this.board = board;
	}
	/**
	 * returns the winningRun string. 
	 * the string is formated as x0,y0[DELIM]x1,y1
	 * @return
	 */
	public String getWinningRun(){
		return winningRun;
	}
	
	/**
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
		} else if (moves == 100) {
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
	 * @return
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


