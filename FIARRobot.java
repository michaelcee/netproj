package netproj;
import java.util.Random;

public class FIARRobot {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static int[] getMove(int[][] board, int x, int y){
		
	Random rnd = new Random();
	int [] nextMove = new int [2];
	int robotMoveX = 0; 
	int robotMoveY = 0; 
	boolean invalid = true;
	while(invalid)
	{	
	  robotMoveX = rnd.nextInt(3)-1+x;
	  robotMoveY = rnd.nextInt(3)-1+y;
	  if( ((robotMoveX <=9 && robotMoveX>=0) 
			  &&(robotMoveY <=9 && robotMoveY>=0)) 
              && (board [robotMoveY][robotMoveX]==0))
		  invalid=false;
	}
	
	nextMove[0] = robotMoveX;
	nextMove[1] = robotMoveY;
	
	
		return nextMove;
	}
		
}
	
	
	
	

