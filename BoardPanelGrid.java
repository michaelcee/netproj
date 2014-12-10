package netproj;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * this is the UI class which provides a window to the player.  
 * 
 * public game-related methods are available for things such as "end game" and "change spot"
 * 
 * this class makes extensive use of the 
 * SwingUtilities.invokeLater(new Runnable() {
 *           public void run() {
 *           	doStuff();
 *           }
 *  });
 *  
 * pattern whenver the UI needs to be updated.  this is necessary to prevent 
 * various problems that arise when trying to schedule multiple updates to the 
 * interface from various threads (such as the client's socket-listening thread)  
 * 
 * NOTE:
 * this class was adapted from the Oracle tutorial found here:
 * https://docs.oracle.com/javase/tutorial/uiswing/examples/layout/
 * 				GridLayoutDemoProject/src/layout/GridLayoutDemo.java
 * 
 *
 */
public class BoardPanelGrid extends JFrame {
	
	//initialize some UI components:
    final static int maxGap = 20;    
    private JButton connectButton = new JButton("Connect");
    private JButton newGameButton = new JButton("New Game");
    private JTextField hostBox = new JTextField("localhost");
    private JCheckBox checkBox = new JCheckBox("Single Player");
    private Label msgLabel = new Label("Connect to a server to play");
    
    //arrays are used for player/situation-specific coloring and borders:
    /** [0] = default spot color, [1] = p1 color, [2] = p2 color */
    private Color[] colArr = {new Color(150, 160, 150), new Color(255,100,100), new Color(100,100,255)};
    /** [0] = p1's hover color, [1] = p2's hover color */
    private Color[] highArr = {new Color(150, 100, 100), new Color(100,100,150)};
    /** [0] = default border, [1] = universal hover border, [2] = blinking border*/
    private Border[] borArr;
    /** a temp var for restoring color after mouse exit on hoverings */
    private Color hoverTemp;
    /**if a spot on the boolboard is true, it means the spot is taken. 
     * this is for preventing clients from sending pointless spot requests*/
    private boolean[][] boolBoard = new boolean[10][10];
    private boolean keepBlinking;
    private boolean connected;
    /**the board itself is composed of 100 SpotLabels*/
    private SpotLabel[][] spots = new SpotLabel[10][10];
    /**for the fancy mouse-fade effect*/
    private FlairManager flair = new FlairManager(255, 20);    
    /**the message ticker uses a LinkedList Queue to display messages*/
    private LinkedList<String> msgQ = new LinkedList<String>();
    /**if there's a message waiting in the msgQ*/
    private boolean msgWaiting;
    /**what this client's player number is*/
    private int player = 0;//default, just in case there's trouble...
    /**the client which launched this GUI.  used for comms*/
    private final FIARClient client;
     
    /**
     * this constructor takes the Window frame's name and a reference to the
     * FIARClient that should be making the call.  
     * @param name
     * @param client
     */
    public BoardPanelGrid(String name, FIARClient client) {
    	super(name);    	
    	borArr = new Border[3];
    	//due to the nature of borders, they have to init here
    	borArr[0] = BorderFactory.createLineBorder(new Color(0, 0, 0));
    	borArr[1] = BorderFactory.createLineBorder(new Color(255, 255, 255));
    	borArr[2] = BorderFactory.createLineBorder(new Color(255, 255, 0));
        setResizable(false);
        this.client = client;
    }
    
    /**
     * clients need to know which player they are so they can color the spots
     * on the board appropriately
     * @param playerNum
     */
    public void setPlayer(int playerNum){
    		player = playerNum;    	
    		
    }
     
    /**
     * add and layout of all our components for display
     * @param pane
     */
    public void addComponentsToPane(final Container pane) {
    	//create the ticker at the top:
    	final JPanel msgPanel = new JPanel();
    	msgPanel.setBackground(new Color(255,255,255));
        msgLabel.setBackground(new Color(255,255,255));
        msgLabel.setForeground(new Color(0, 0, 200));
        msgLabel.setAlignment(Label.CENTER);
        msgLabel.setFont(new Font("Arial", 0, 16));
        msgPanel.add(msgLabel);
    	
        //create the game board:
        final JPanel boardGrid = new JPanel();
        GridLayout boardLayout = new GridLayout(10,10);
        boardGrid.setLayout(boardLayout);
        boardLayout.setHgap(5);
        boardLayout.setVgap(5);
        boardGrid.setBackground(new Color(100, 100, 100));
        boardGrid.setPreferredSize(new Dimension(412, 412));
        boardGrid.setPreferredSize(new Dimension((int)( (30+maxGap) * 10 + maxGap),
        		(int)( (30+maxGap) * 10 + maxGap)));
        //create each of the 100 spots:
        for(int i = 0; i < 10; i++){
        	for(int j = 0; j < 10; j++){
		        SpotLabel spot = new SpotLabel(j, i);
		        spots[j][i] = spot;
		        spot.setBackground(new Color(0,0,0)); //spots are black until opponent connects
		        spot.setBorder(borArr[0]);
		        spot.setFocusable(false); //don't "focus" on it, in the window sense
		        spot.setInheritsPopupMenu(false);
		        spot.setMaximumSize(new Dimension(30, 30));
		        //spot.setMinimumSize(new Dimension(30, 30));
		        spot.setOpaque(true);
		        spot.setPreferredSize(new Dimension(30, 30));
		        spot.setRequestFocusEnabled(false); //we don't want to tab to it
		        //the spots have actions for 3 events:
		        spot.addMouseListener(new MouseAdapter() {
		            public void mouseEntered(MouseEvent evt) {
		            	//so we can highlight the spot
		                spotMouseEntered(evt);
		            }
		            public void mouseExited(MouseEvent evt) {
		            	//so we can start the fancy fade effect
		                spotMouseExited(evt);
		            }
		            public void mouseClicked(MouseEvent evt) {
		            	//so we can send the spot change request
		                spotMouseClicked(evt);
		            }
		        });
		        
		        boardGrid.add(spot);
        	}
        }
        
        //create the controls at the bottom of the window
        JPanel controls = new JPanel();
        GridLayout controlsLayout = new GridLayout(2,3);
        controlsLayout.setHgap(5);
        controls.setLayout(controlsLayout);
        controls.add(new Label("[reserved space]"));
        controls.add(checkBox);
        newGameButton.setEnabled(false);
        controls.add(newGameButton);
        Label hostLabel = new Label("Host Name:");
        hostLabel.setAlignment(Label.RIGHT);
        controls.add(hostLabel);
        controls.add(hostBox);
        controls.add(connectButton);
        connectButton.setEnabled(true);
    
        //wire up our buttons:
        connectButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	connect();
            }
        });
        
        //newGame doesn't become enabled unless a "GAME_OVER" message is received
        //as a result, its pressing is used to determine whether or not we should
        //keep flashing the winning run. then requests a new game and disables itself
        newGameButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	keepBlinking = false;
            	client.requestNew(true);
            	newGameButton.setEnabled(false);
            }
        });

        pane.add(msgPanel, BorderLayout.NORTH);
        pane.add(boardGrid, BorderLayout.CENTER);
        pane.add(controls, BorderLayout.SOUTH);

    }
    
    
    /**
     * if a spot isn't taken, we need to highlight it when we mouse over it
     * @param evt
     */
    private void spotMouseEntered(MouseEvent evt) {
    	
    	SpotLabel spot = (SpotLabel) evt.getSource(); 
    	if(!boolBoard[spot.x][spot.y]){
    		//kill any fading that may already be taking place:
    		flair.killFlair(spot);
    		spot.setBorder(borArr[1]);
    		//remember the previous color of the spot:
    		hoverTemp = spot.getBackground();
    		//set the background to the player's hover color
	        spot.setBackground(highArr[player]);
	        //System.out.println(spot.x + ", " + spot.y);
    	}
    }
    
    /**
     * begin fancy fade effect and change the border and fill colors back.  
     * 
     * @param evt
     */
    private void spotMouseExited(MouseEvent evt) {
    	SpotLabel spot = (SpotLabel) evt.getSource();
    	//we have to double check boolBoard otherwise we'd change the spot
    	//back after a player clicked it!
    	if(!boolBoard[spot.x][spot.y]){
	    	spot.setBackground(hoverTemp);//default background color
	    	spot.setBorder(borArr[0]);
	    	flair.startFlair(spot);
	    	//System.out.println(spot.x + ", " + spot.y);
    	}
    }

    /**
     * if the player has clicked a spot that is not yet taken and it's the 
     * player's turn, we send the move to the server
     * @param evt
     */
    private void spotMouseClicked(MouseEvent evt) {
    	SpotLabel spot = (SpotLabel) evt.getSource();
    	if(!boolBoard[spot.x][spot.y]){
    		if(client.ready()){
    			if(client.sendMove(spot.x, spot.y)){
    				//success			    	
    			} else {
    				//not success
    			}
    		}
    	}
    }
    
    /**
     * disable relevant options after the server has been connected to.  
     * @param connected
     */
    private void toggleControls(final boolean connected){
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	checkBox.setEnabled(!connected);
        		hostBox.setEnabled(!connected);
            	if(connected){
            		connectButton.setText("Disconnect");
            	} else {
            		connectButton.setText("Connect");
            	}
            }
        });
    }
    	
    /**
     * connect if we're not connected already.  the port is hardwired for now
     * but this could be changed in the future if necessary.  
     */
    private void connect(){
    	if(!connected){
    		//works with IP address and hostnames
	    	client.initComms(8484, hostBox.getText());
	    	
	    	//if we want to play single player or not
	    	if(checkBox.isSelected()){
	    		client.reqSinglePlayer();
	    	} else {
	    		client.reqMultiPlayer();
	    	}
	    	
	    	//turn off the connection-related controls.  once we're connected to
	    	//the server, there's no need to to keep the "connect" button or the
	    	//"single player" check box enabled
	    	toggleControls(true);
	    	
	    	connected = true;
    	} else {
    		//clients will need to close the window to disconnect from the server
    		//for now
    		setMsg("this doesn't do anything yet..");
    	}
    }
    /**
     * change a spot on the board.  this method is to be invoked externally by the attached
     * FIARClient, on its socket-listening thread.  
     * @param player
     * @param x
     * @param y
     */
    public void changeSpot(final int player, final int x, final int y){
    	
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	SpotLabel spot = spots[x][y];
            	spot.setOwner(player, colArr[player + 1]);
            	boolBoard[spot.x][spot.y] = true;
            }
        });
    	
    }
    
    /**
     * set the text of the message box at the top.  this should be called by
     * FIARClient.
     * 
     * a synchronized queue is used to display messages.  each message is drawn
     * 5 characters at a time, 20ms apart.  the message is then held in the
     * text box at the top for 1.5 seconds.  messages that arrive during this
     * draw and hold process are placed in the queue 
     * 
     * @param msg - what to display in the box
     */
    public void setMsg(String msg){
    	//we must synchronize access to msgQ to be sure we don't modify the list
    	//while trying to read it
		synchronized(msgQ){
			msgQ.add(msg);
			//if there are no messages waiting, we need to initialize printing
			if(!msgWaiting){
				printQueue();
			}
			
		}
    }
    
    /**
     * prints the contents of the msgQ.  if a message is added to the queue during
     * the display period (takes approximately 1.75 seconds per message), print
     * queue will print it automatically and separate invocation of printQueue
     * is NOT NEEDED 
     */
    private void printQueue(){
    	
    	//we've begun printing so we're busy:
    	msgWaiting = true;
    	new Thread(new Runnable() {
    		
    		//this thread will run until there are no more messages to print
            public void run() {
            	boolean keepGoing = true;
             	while(keepGoing){
             		//must synchronize on msgQ to prevent concurrency problems
            		synchronized(msgQ){
            			writeMsg(msgQ.remove());            			
            		}
                	try {
                		//wait a second and a half before continuing
        				Thread.sleep(1500);
        			} catch (InterruptedException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}

                	synchronized(msgQ){
                		if(msgQ.isEmpty())
                			keepGoing = false;
                	}
                	
            	}
             	msgWaiting = false;
            }//end run

    	}).start();
    	
    }
    
    /**
     * writes a message to the ticker in 5 character spurts, 20ms apart.  
     * @param msg
     */
    private void writeMsg(final String msg){
    	new Thread(new Runnable() {
    		
            public void run() {
            	
            	char[] msgArr = msg.toCharArray();
            	final StringBuilder sb = new StringBuilder();
            	
            	//go up the message, 5 characters at a time and add them to the
            	//string to be displayed.  
            	for(int i = 0; i < msgArr.length; i+=5){
            		for(int j = i; j < i+5; j++){
            			if(j < msgArr.length)//only if j isn't out of bounds
            				sb.append(msgArr[j]);
            		}
            		
            		SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                        	msgLabel.setText(sb.toString());
                        	//we need to call pack in order to resize msgLabel
                        	//to the length of the text:
                        	pack();
                        }
                    });
	        		try {
	        			//do it 20ms at a time
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}//end large for-loop

            }//end run

    	}).start();
    }
    
    
    

    
    /**
     * start a new game.  this method is to be invoked externally by the attached
     * FIARClient, by the socket-listening thread.
     * 
     * sets the owner, background color and borders of all spots to neutral.
     * also clears the boolBoard
     */
    public void newGame(){
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	for(int i = 0; i < 10; i++){
                	for(int j = 0; j < 10; j++){
        		        spots[j][i].setBackground(colArr[0]);
        		        spots[j][i].setBorder(borArr[0]);
        		        spots[j][i].resetOwner();
                	}
            	}
            	
            	boolBoard = new boolean[10][10];
            }
        });
    }
    /**
     * end the game with a winning run of spots.  endGameDraw handles games
     * when there is no winner.  the winning run will blink to show the winner 
     * and that is handled here as well. 
     * 
     * @param winningRun and int[][] of size: [number of spots][2]; 
     * a list of ordered pairs holding the winning run of spots
     */
    public void endGame(final int[][] winningRun){
    	keepBlinking = true;
    	new Thread( new Runnable(){
    	
    		public void run(){
    			//temp var to alternate colors for blinking:
    			int c = 2;//2 = highlight color
    			while(keepBlinking){
    				//blink border
    				//blink the winning run with the given color index:
    				doBlink(c, winningRun);
    				
    				try {
    					//effective blink rate
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				//alternate highlight and default border colors
    				c = (c == 2) ? 0 : 2;
    			}
    		}
    	}).start();
    	
    	//enable the newGame button:
    	endGame();
    	
    }
    
    
    /**
     * convenience method to turn the newGame button back on
     */
    public void endGame() {
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	newGameButton.setEnabled(true);            	
            }
        });
		
	}
    
    /**
     * no need for celebration or anything like that.  just update the player ticker 
     * (done by FIARClient) and enable the newGame button
     */
    public void endGameDraw(){
    	endGame();
    }
    /**
     * change the border of a run of spots.  
     * borArr[0] = regular background
     * borArr[1] = hover
     * borArr[2] = highlight
     * @param color
     * @param winningRun
     */
    private void doBlink(final int color, final int[][] winningRun){
    	SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
        		//do each spot in the array:
            	for(int i = 0; i < winningRun.length; i++){
            		int x = winningRun[i][0];
            		int y = winningRun[i][1];
            		spots[x][y].setBorder(borArr[color]);
            	}
	            	
            }
    	});
    }
     
    /**
     * Create the GUI and show it.  For thread safety,
     * this method is invoked from the
     * event dispatch thread.
     * 
     * 
     */
    private void createAndShowGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Set up the content pane.
        addComponentsToPane(getContentPane());
        //Display the window.
        pack();
        setVisible(true);
    }
     
    
    /**
     * start the process of showing the GUI.  the UI manager bit was adapted
     * from the Oracle tutorial.  
     */
    public void launch() {
        try {
        	for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
         
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
    /**
     * turn the connect button on or off
     * @param on
     */
    public void setConnectButton(final boolean on){
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	connectButton.setEnabled(on);
            }
        });
    	
    }
    
    /** for testing
     * 
     * @param args
     */
    public static void main(String[] args){
    	BoardPanelGrid bpg = new BoardPanelGrid("Five in a Row", null);
    	bpg.launch();
    }

	
}
/**
 * this class extends JLabel to provide the functionality of a spot on the board.
 * 
 * each spot has fields x,y that are permanently associated with the given spot
 * 
 * each spot also has an owner, stored in an int:
 * 	[0] = no owner
 * 	[1] = player 1
 * 	[2] = player 2
 * 
 * the code is otherwise quite self-documenting
 *
 */
@SuppressWarnings("serial")
class SpotLabel extends JLabel {
	
	public final int x;	//x position
	public final int y;	//y position
	private int owner; 	//the player number of the occupant, if any
	
	public SpotLabel(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public boolean setOwner(int newOwner){
		if(owner == 0){
			owner = newOwner;
			return true;
		}
		
		return false;
	}
	
	public boolean setOwner(int newOwner, Color ownerColor){
		if( setOwner(newOwner) ){
			this.setBackground(ownerColor);
			return true;
		}
		
		return false;
	}
	
	public void resetOwner(){
		owner = 0;
	}
}

/**
 * 
 * a class to provide functionality for the fancy fade-out feature.  
 * 
 * each spot that is faded is done so on its own thread.  perhaps in the future
 * a more efficient way of doing this can be used.
 *
 */
class FlairManager{
	/**used to tell whether or not we're currently fading this spot*/
	boolean[][] board = new boolean[10][10];
	/**each spot is faded on its own thread*/
	Thread[][] threads = new Thread[10][10];
	/**the colors are stored in borders and applied one at a time.  they start at
	 * an initial value and then decrease at a rate that depends on how many frames we 
	 * want the effect to last for
	 */
	private Border[] colors;
	
	
	/**
	 * set the parameters of this FlairManager.  FlairManager changes the border
	 * color of a spot from white (or whatever whitevalue is specified) to black
	 * over the specified number of frames of animation
	 * 
	 * recommended defaults: 
	 * val = 255
	 * frames = 20
	 * @param whiteVal
	 * @param frames
	 */
	FlairManager(int whiteVal, int frames){
		
		int velocity = whiteVal / frames;
		colors = new Border[frames];
		
		for(int i = 0; i < colors.length; i++){
			colors[i] = BorderFactory.createLineBorder(new Color(whiteVal, whiteVal, whiteVal));
			whiteVal -= velocity;
			
			
		}
		
	}
	/**
	 * if a player has highlighted a spot, moved the mouse to an adjacent spot
	 * and then quickly moved back, we need to cancel the flairing process because
	 *  the spot needs to be highlighted again
	 * @param spot
	 */
	public void killFlair(final SpotLabel spot){
		//kill the thread at the given spot:
		if(board[spot.x][spot.y]){
			threads[spot.x][spot.y].interrupt();
		}
	}
	
	/**
	 * start changing the background color of the given spot from the initial
	 * value to the default non-highlighted color
	 * @param spot
	 */
	public void startFlair(final SpotLabel spot){
		board[spot.x][spot.y] = true;
		//start the thread at the given spot
		threads[spot.x][spot.y] = new Thread(new Runnable(){
			
			public void run(){
				for(Border b : colors){
		        	flairDown(b, spot);
		        	try {
						Thread.sleep(1000/60); //60 frames a second
					} catch (InterruptedException e) {
						// it's ok, it means the player has re-highlighted
						//this spot while we were still flairng down
					}
		        }
				board[spot.x][spot.y] = false;
			}
		});
		threads[spot.x][spot.y].start();
		
		
	}
	
	/**
	 * do the actual work of changing the spot's color on the UI thread
	 * @param b
	 * @param spot
	 */
	private void flairDown(final Border b, final SpotLabel spot){
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                spot.setBorder(b);
            }
        });
	}
	
}
