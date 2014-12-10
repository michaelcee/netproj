// Author: Elena
// purpose: This class is responsible for generation of randomly placed moves during the single mode game.
// Date: 12/10/14
package netproj;
import java.util.Random;

public class FIARRobot {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static int[] getMove(int[][] board, int x, int y){
		
	Random rnd = new Random();
	
	//we create a 2 element array that will pass a values of the next move coordinates
	int [] nextMove = new int [2];
	int robotMoveX = 0; 
	int robotMoveY = 0; 
	boolean invalid = true;
	// check for validity of created coordinates
	while(invalid)
	{	
	  // uses the player`s one last move to generate 2 random numbers. Those numbers will be placed close to the last move that P1 made	
	  robotMoveX = rnd.nextInt(3)-1+x;
	  robotMoveY = rnd.nextInt(3)-1+y;
	  // cet up limits for the RobotMove (making sure they are within board )
	  if( ((robotMoveX <=9 && robotMoveX>=0) 
			  &&(robotMoveY <=9 && robotMoveY>=0)) 
              && (board [robotMoveY][robotMoveX]==0))
		  invalid=false;
	}
	
	nextMove[0] = robotMoveX;
	nextMove[1] = robotMoveY;
	
		// returning coordinates for the move that Robot chose.
		return nextMove;
	}
		
}
	
	
	
	

