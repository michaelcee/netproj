package netproj;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.concurrent.SynchronousQueue;

import javax.swing.*;
import javax.swing.border.Border;
 
public class BoardPanelGrid extends JFrame {
    final static int maxGap = 20;
    
    private JButton connectButton = new JButton("Connect");
    private JButton newGameButton = new JButton("New Game");
    private JTextField hostBox = new JTextField("localhost");
    private JCheckBox checkBox = new JCheckBox("Single Player");
    private Label msgLabel = new Label("Connect to a server to play");
    private Color[] colArr = {new Color(150, 160, 150), new Color(255,100,100), new Color(100,100,255)};
    private Color[] highArr = {new Color(150, 100, 100), new Color(100,100,150)};
    private Border[] borArr; //0 no high, 1 = high, 2 = winning run
    private Color hoverTemp;//a temp var for restoring color after mouse exit on hoverings
    private boolean[][] boolBoard = new boolean[10][10];
    private boolean keepBlinking;
    private boolean connected;
    private SpotLabel[][] spots = new SpotLabel[10][10];
    private FlairManager flair = new FlairManager(255, 20);
    
    private LinkedList<String> msgQ = new LinkedList<String>();
    private boolean msgWaiting;
    
    private int player = 0;//default, just in case there's trouble...
    
    private final FIARClient client;
     
    public BoardPanelGrid(String name, FIARClient client) {
    	super(name);    	
    	borArr = new Border[3];
    	borArr[0] = BorderFactory.createLineBorder(new Color(0, 0, 0));
    	borArr[1] = BorderFactory.createLineBorder(new Color(255, 255, 255));
    	borArr[2] = BorderFactory.createLineBorder(new Color(255, 255, 0));
        setResizable(false);
        this.client = client;
    }
    
    public void setPlayer(int playerNum){
    		player = playerNum;    	
    		
    }
     
    
    public void addComponentsToPane(final Container pane) {
    	final JPanel msgPanel = new JPanel();
    	msgPanel.setBackground(new Color(255,255,255));
        msgLabel.setBackground(new Color(255,255,255));
        msgLabel.setForeground(new Color(0, 0, 200));
        msgLabel.setAlignment(Label.CENTER);
        msgLabel.setFont(new Font("Arial", 0, 16));
        
        msgPanel.add(msgLabel);
    	
        final JPanel boardGrid = new JPanel();
        GridLayout boardLayout = new GridLayout(10,10);
        boardGrid.setLayout(boardLayout);
        boardLayout.setHgap(5);
        boardLayout.setVgap(5);
        boardGrid.setBackground(new Color(100, 100, 100));
        boardGrid.setPreferredSize(new Dimension(412, 412));
        JPanel controls = new JPanel();
        GridLayout controlsLayout = new GridLayout(2,3);
        controlsLayout.setHgap(5);
        controls.setLayout(controlsLayout);

        boardGrid.setPreferredSize(new Dimension((int)( (30+maxGap) * 10 + maxGap),
        		(int)( (30+maxGap) * 10 + maxGap)));
        
        for(int i = 0; i < 10; i++){
        	for(int j = 0; j < 10; j++){
		        SpotLabel spot = new SpotLabel(j, i);
		        spots[j][i] = spot;
		        spot.setBackground(new Color(0,0,0)); //spots are black until opponent connects
		        spot.setBorder(borArr[0]);
		        spot.setFocusable(false);
		        spot.setInheritsPopupMenu(false);
		        spot.setMaximumSize(new Dimension(30, 30));
		        //spot.setMinimumSize(new Dimension(30, 30));
		        spot.setOpaque(true);
		        spot.setPreferredSize(new Dimension(30, 30));
		        spot.setRequestFocusEnabled(false);
		        spot.addMouseListener(new MouseAdapter() {
		            public void mouseEntered(MouseEvent evt) {
		                spotMouseEntered(evt);
		            }
		            public void mouseExited(MouseEvent evt) {
		                spotMouseExited(evt);
		            }
		            public void mouseClicked(MouseEvent evt) {
		                spotMouseClicked(evt);
		            }
		        });
		        
		        boardGrid.add(spot);
        	}
        }
        //controls.add(msgLabel);
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
    
         
        //Process the Apply gaps button press
        connectButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	connect();
            }
        });
        
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
/*
        pane.add(boardGrid, BorderLayout.NORTH);
        pane.add(new JSeparator(), BorderLayout.CENTER);
        pane.add(controls, BorderLayout.SOUTH);
*/
    }
    
    
    
    private void spotMouseEntered(MouseEvent evt) {//GEN-FIRST:event_jLabel100MouseEntered
    	
    	SpotLabel spot = (SpotLabel) evt.getSource(); 
    	if(!boolBoard[spot.x][spot.y]){
    		flair.killFlair(spot);
    		spot.setBorder(borArr[1]);
    		hoverTemp = spot.getBackground();
	        spot.setBackground(highArr[player]);
	        //System.out.println(spot.x + ", " + spot.y);
    	}
    }

    private void spotMouseExited(MouseEvent evt) {//GEN-FIRST:event_jLabel100MouseExited
    	SpotLabel spot = (SpotLabel) evt.getSource();
    	if(!boolBoard[spot.x][spot.y]){
	    	spot.setBackground(hoverTemp);//default background color
	    	spot.setBorder(borArr[0]);
	    	flair.startFlair(spot);
	    	//System.out.println(spot.x + ", " + spot.y);
    	}
    }

    private void spotMouseClicked(MouseEvent evt) {//GEN-FIRST:event_jLabel100MouseReleased
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
    	
    
    private void connect(){
    	if(!connected){
	    	client.initComms(8484, hostBox.getText());
	    	
	    	if(checkBox.isSelected()){
	    		client.reqSinglePlayer();
	    	} else {
	    		client.reqMultiPlayer();
	    	}
	    	
	    	toggleControls(true);
	    	
	    	connected = true;
    	} else {
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
    
    private void writeMsg(final String msg){
    	new Thread(new Runnable() {
    		
            public void run() {
            	char[] msgArr = msg.toCharArray();
            	final StringBuilder sb = new StringBuilder();
            	
            	for(int i = 0; i < msgArr.length; i+=5){
            		for(int j = i; j < i+5; j++){
            			if(j < msgArr.length)
            				sb.append(msgArr[j]);
            		}
            		
            		SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                        	msgLabel.setText(sb.toString());
                        	pack();
                        }
                    });
	        		try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}//end large for-loop

            }//end run

    	}).start();
    }
    
    private void printQueue(){
    	
    	msgWaiting = true;
    	new Thread(new Runnable() {
    		
            public void run() {
            	boolean keepGoing = true;
             	while(keepGoing){
            		synchronized(msgQ){
            			writeMsg(msgQ.remove());            			
            		}
                	try {
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
     * set the text of the message box at the top.  this should be called by
     * FIARClient
     * 
     * @param msg - what to display in the box
     */
    public void setMsg(String msg){
    		synchronized(msgQ){
    			msgQ.add(msg);
    			if(!msgWaiting){
    				printQueue();
    			}
    			
    		}
    }
    
    /**
     * start a new game.  this method is to be invoked externally by the attached
     * FIARClient, by the socket-listening thread.
     * 
     * sets the background color of all spots to neutral
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
     * 
     * @param winningRun
     */
    public void endGame(final int[][] winningRun){
    	keepBlinking = true;
    	new Thread( new Runnable(){
    	
    		public void run(){
    			//temp var to alternate colors for blinking
    			int c = 2;//start at highlight
    			while(keepBlinking){
    				//blink border
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
    	endGame();
    	
    }
    
    public void endGame() {
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	newGameButton.setEnabled(true);            	
            }
        });
		
	}
    
    public void endGameDraw(){
    	//do stuff to let player know its a draw
    	
    	
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
     */
    private void createAndShowGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Set up the content pane.
        addComponentsToPane(getContentPane());
        //Display the window.
        pack();
        setVisible(true);
    }
     
    public void launch() {//this used to be MAIN
        /* Use an appropriate Look and Feel */
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
         
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
    public void setConnectButton(final boolean on){
    	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	connectButton.setEnabled(on);
            }
        });
    	
    }
    
    public static void main(String[] args){
    	BoardPanelGrid bpg = new BoardPanelGrid("Five in a Row", null);
    	bpg.launch();
    }

	
}

@SuppressWarnings("serial")
class SpotLabel extends JLabel {
	
	public final int x;
	public final int y;
	private int owner; //the player number of the occupant, if any
	
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
 * @author MIKE
 *
 */
class FlairManager{
	boolean[][] board = new boolean[10][10];
	Thread[][] threads = new Thread[10][10];
	private Border[] colors;
	
	
	/**
	 * val = 255
	 * frames = 20
	 * @param whiteVal
	 * @param frames
	 */
	public FlairManager(int whiteVal, int frames){
		int velocity = whiteVal / frames;
		colors = new Border[frames];
		
		for(int i = 0; i < colors.length; i++){
			colors[i] = BorderFactory.createLineBorder(new Color(whiteVal, whiteVal, whiteVal));
			whiteVal -= velocity;
			
			
		}
		
	}
	
	public void killFlair(final SpotLabel spot){
		try {
			if(board[spot.x][spot.y]){
				threads[spot.x][spot.y].join();
				threads[spot.x][spot.y].interrupt();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startFlair(final SpotLabel spot){

		//killFlair(spot);
		
		board[spot.x][spot.y] = true;
		threads[spot.x][spot.y] = new Thread(new Runnable(){
			
			public void run(){
				for(Border b : colors){
		        	flairDown(b, spot);
		        	try {
						Thread.sleep(1000/60);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        }
				board[spot.x][spot.y] = false;
			}
		});
		threads[spot.x][spot.y].start();
		
		
	}
	
	private void flairDown(final Border b, final SpotLabel spot){
		//SwingUtilities.invokeLater(new Runnable() {
            //public void run() {
                spot.setBorder(b);
            //}
        //});
	}
	
}
