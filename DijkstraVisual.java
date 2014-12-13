package dijvis;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.Border;


public class DijkstraVisual extends JFrame {
	
	//initialize some UI components:
    final static int maxGap = 20;    
    public final static Color COLOR_PATH = new Color(100,100,255);
    public final static Color COLOR_NEIGHBOR = new Color(255,100,100);
    public final static Color COLOR_REJECT = new Color(150, 160, 150);
    private static String dim = "20";
    private static String cost = "10";
    private static String chance = ".4";
    private int boardSize;//the board will always be sized via a single number
    private JButton resizeButton = new JButton("Redim Board");
    private JButton rerollButton = new JButton("Reroll");
    private JTextField costField = new JTextField(cost);
    private JTextField chanceField = new JTextField(chance);
    private JTextField sizeField = new JTextField(dim);
    private Label msgLabel = new Label("Left click to set source.  Right click to show neighbors.");
    private JPanel msgPanel;
    private JPanel controls;
    private JPanel boardGrid;

    /** [0] = default border, [1] = universal hover border, [2] = blinking border*/
    private Border[] borArr;
    /** a temp var for restoring color after mouse exit on hoverings */
    private Color hoverTemp;
    /**the board itself is composed of 100 NodeLabels*/
    private NodeLabel[][] nodes;// = new NodeLabel[20][20];
    /**for the fancy mouse-fade effect*/
    private LinkedList<String> msgQ = new LinkedList<String>();
    /**if there's a message waiting in the msgQ*/
    private boolean msgWaiting;
    private NodeLabel sourceNode;//which node we've clicked on
    private NodeLabel prevNode;
    private final DijkstraVisual instance;
    private boolean mouseDown;
    
    
    
    /**
     * 
     * @param args
     */
    public static void main(String[] args){

    	DijkstraVisual bpg = new DijkstraVisual("Dijkstra Visualizer", 20);
    	bpg.launch();
    }
    
     
    /**
     * 
     * @param name
     * @param client
     */
    public DijkstraVisual(String name, int size) {
    	super(name);    
        instance = this;    	
    	borArr = new Border[3];
    	//due to the nature of borders, they have to init here
    	borArr[0] = BorderFactory.createLineBorder(new Color(0, 0, 0));
    	borArr[1] = BorderFactory.createLineBorder(new Color(255, 255, 255));
    	borArr[2] = BorderFactory.createLineBorder(Color.RED);
        setResizable(true);
        initBoard(size);
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
        pack();
        setVisible(true);

    }
     
    
    /**
     * start the process of showing the GUI.  the UI manager bit was adapted
     * from the Oracle tutorial.  
     */
    public void launch() {

         
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();

            }
        });
        
        
    }
    
    private void initBoard(int size){
    	boardSize = size;
    	nodes = new NodeLabel[boardSize][boardSize];
    	for(int i = 0; i < boardSize; i++){
        	for(int j = 0; j < boardSize; j++){
		        NodeLabel spot = new NodeLabel(j, i);
		        nodes[j][i] = spot;
		        spot.addMouseListener(new MouseAdapter() {
		            public void mouseEntered(MouseEvent evt) {
		            	//so we can highlight the spot
		                nodeMouseEntered(evt);
		            }
		            public void mouseExited(MouseEvent evt) {
		            	//so we can start the fancy fade effect
		                nodeMouseExited(evt);
		            }
		            public void mouseClicked(MouseEvent evt) {
		            	//so we can send the spot change request
		                nodeMouseClicked(evt);
		            }
		            
		            public void mousePressed(MouseEvent evt) {
		            	//so we can send the spot change request
		                nodeMouseDown(evt);
		            }
		            
		            public void mouseReleased(MouseEvent evt){
		            	nodeMouseUp(evt);
		            }
		        });
		        
        	}
        }
    	
    	
        //create the game board:
        boardGrid = new JPanel();
        GridLayout boardLayout = new GridLayout(boardSize,boardSize);
        boardGrid.setLayout(boardLayout);
        boardLayout.setHgap(1);
        boardLayout.setVgap(1);
        boardGrid.setBackground(new Color(100, 100, 100));
        boardGrid.setPreferredSize(new Dimension(412, 412));
        boardGrid.setPreferredSize(new Dimension((int)( (30+maxGap) * 10 + maxGap),
        		(int)( (30+maxGap) * 10 + maxGap)));
        randomizeBoard(Integer.valueOf(costField.getText()), 
        		Double.valueOf(chanceField.getText()));

        for(int i = 0; i < boardSize; i++){
        	for(int j = 0; j < boardSize; j++){
		        boardGrid.add(nodes[j][i]);
        	}
        }
      
    }
      
    /**
     * add and layout of all our components for display
     * @param pane
     */
    public void addComponentsToPane(final Container pane) {
    	//create the ticker at the top:
    	msgPanel = new JPanel();
    	msgPanel.setBackground(new Color(255,255,255));
        msgLabel.setBackground(new Color(255,255,255));
        msgLabel.setForeground(new Color(0, 0, 200));
        msgLabel.setAlignment(Label.CENTER);
        msgLabel.setFont(new Font("Arial", 0, 16));
        msgPanel.add(msgLabel);
        
        //create the controls at the bottom of the window
        controls = new JPanel();
        controls.setBackground(Color.WHITE);
        GridLayout controlsLayout = new GridLayout(2,4);
        controlsLayout.setHgap(10);
        controls.setLayout(controlsLayout);
        
        //r1,c1
        JLabel neighLabel = new JLabel("   ");
        neighLabel.setHorizontalAlignment(JLabel.RIGHT);
        neighLabel.setBackground(Color.WHITE);
        neighLabel.setOpaque(true);
        controls.add(neighLabel);
        
        //r1,c2
        JPanel costPanel = new JPanel();
        costPanel.setBackground(Color.WHITE);
        JLabel costLabel = new JLabel("Max cost:");
        costLabel.setHorizontalAlignment(JLabel.RIGHT);
        costPanel.add(costLabel);
        costField.setPreferredSize(new Dimension(40, 20));
        costPanel.add(costField);

        costPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controls.add(costPanel);
        
        
        
        //r1,c3
        JPanel chancePanel = new JPanel();
        chancePanel.setBackground(Color.WHITE);
        JLabel connectLabel = new JLabel("chance:");
        connectLabel.setHorizontalAlignment(JLabel.RIGHT);
        chancePanel.add(connectLabel);
        chanceField.setPreferredSize(new Dimension(40, 20));
        chancePanel.add(chanceField);
        chancePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controls.add(chancePanel);
        
        //r1,c4
        controls.add(rerollButton);
        
        //r2,c1
        JLabel res1 = new JLabel("   ");
        res1.setHorizontalAlignment(JLabel.RIGHT);
        res1.setBackground(Color.WHITE);
        res1.setOpaque(true);
        controls.add(res1);
        
        //r2,c2
        JLabel res2 = new JLabel("   ");
        res2.setHorizontalAlignment(JLabel.RIGHT);
        res2.setBackground(Color.WHITE);
        res2.setOpaque(true);
        controls.add(res2);
        
        //r2,c3
        JPanel sizePanel = new JPanel();
        sizePanel.setBackground(Color.WHITE);
        JLabel sizeLabel = new JLabel("board size:");
        sizeLabel.setHorizontalAlignment(JLabel.RIGHT);
        sizePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        sizePanel.add(sizeLabel);
        sizeField.setPreferredSize(new Dimension(30, 20));
        sizePanel.add(sizeField);

        controls.add(sizePanel);
        
        //r2,c4
        controls.add(resizeButton);
        //wire up our buttons:
        resizeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                		instance.dispose();
                		dim = sizeField.getText();
                		cost = costField.getText();
                		chance = chanceField.getText();
                		DijkstraVisual bpg = new DijkstraVisual("Dijkstra Visualizer", Integer.valueOf(dim));
                    	bpg.launch();
                	}
                });	
            }
        });
        
        rerollButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	if(null != sourceNode)
            		toggleSourceNode(false);
            	sourceNode = null;
            	for(int i = 0; i < boardSize; i++){for(int j = 0; j < boardSize; j++){
        			nodes[j][i].resetNeighborhood();
        			nodes[j][i].finalCost = 0;
            	}}
            	resetDijkstra();
            	randomizeBoard(Integer.valueOf(costField.getText()), 
                		Double.valueOf(chanceField.getText()));
                
            }
        });

        pane.add(msgPanel, BorderLayout.NORTH);
        pane.add(boardGrid, BorderLayout.CENTER);
        pane.add(controls, BorderLayout.SOUTH);
    }
    

    private void nodeMouseEntered(MouseEvent evt) { 
    	if(null != prevNode){
    		if(!rejects.contains(prevNode)){
    	    	if(prevNode != sourceNode ){
    	    		prevNode.setBackground(hoverTemp);//default background color
    	    		prevNode.setBorder(borArr[0]);
    	    		
    	    	}
    	    	lightParents(prevNode, false);
        	}
    	}
    	
    	
    	NodeLabel node = (NodeLabel) evt.getSource(); 
    	prevNode = node;
    	if(!rejects.contains(node)){
    		//kill any fading that may already be taking place:
    		node.setBorder(borArr[1]);
    		//remember the previous color of the spot:
    		hoverTemp = node.getBackground();
    		//set the background to the player's hover color
	        node.setBackground(COLOR_REJECT);
	        //spot.flipNeighbors(true);
	        lightParents(node, true);
    	}
    	
    	if(mouseDown){
    		node.flipNeighbors(true);
    	}
	        
	        //System.out.println(spot.x + ", " + spot.y);
    	
    }
    
    /**
     * we don't use this because otherwise the path would flicker when the
     * mouse hovered over the borders
     * 
     * @param evt
     */
    private void nodeMouseExited(MouseEvent evt) {
    	NodeLabel node = (NodeLabel) evt.getSource();
    	if(mouseDown){
    		 node.flipNeighbors(false);
    	}
    }

    /**
     * if the player has clicked a spot that is not yet taken and it's the 
     * player's turn, we send the move to the server
     * @param evt
     */
    private void nodeMouseClicked(MouseEvent evt) {
    	if(SwingUtilities.isLeftMouseButton(evt)){
	    	if(null != sourceNode)
	    		toggleSourceNode(false);
	    	NodeLabel node = (NodeLabel) evt.getSource();
	    	lightParents(node, false);

	    	sourceNode = node;
	    	toggleSourceNode(true);
	    	doDijkstra(node);
	    	node.setBackground(DijkstraVisual.COLOR_PATH);
	    	node.setText("0");
    	}
    }
    private void nodeMouseDown(MouseEvent evt) {
    	if(SwingUtilities.isRightMouseButton(evt)){
    		mouseDown = true;
	    	NodeLabel node = (NodeLabel) evt.getSource();
	    	node.flipNeighbors(true);
    	}
    }
    
    private void nodeMouseUp(MouseEvent evt) {
    	if(SwingUtilities.isRightMouseButton(evt)){
    		mouseDown = false;
	    	prevNode.flipNeighbors(false);
    	}
    }
    
    private void toggleSourceNode(boolean on){
    	if(on)
    		sourceNode.setBorder(borArr[2]);
    	else
    		sourceNode.setBorder(borArr[0]);
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
        				Thread.sleep(500);
        			} catch (InterruptedException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}

                	synchronized(msgQ){
                		if(msgQ.isEmpty()){
                			keepGoing = false;
                     		msgWaiting = false;
                		}
                	}
            	}//end of keepgoing

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
     * 
     * @param maxCost maximum cost of a edge
     * @param chance the chance that any two neighboring nodes are connected
     */
	private void randomizeBoard(int maxCost, double chance) {
		Random r = new Random();

		//iterate through the board and determine connections between adjacent
		//nodes.  also, I'm seeing if I like this for loop style
		for(int i = 0; i < boardSize; i++){for(int j = 0; j < boardSize; j++){
			//iterate through the up to 8 adjacent nodes.  
			//this is going to get confusing..
			for(int k = -1;k < 2; k++){
				for(int m = -1; m < 2; m++)
				
        		if( (m|k) != 0 && chance > r.nextDouble() && //if they're not both zero
        				i + k >=0 && i + k < boardSize && //not out of bounds
        				j + m >=0 && j +m < boardSize){

        			int cost = r.nextInt(maxCost) + Math.abs(k) + Math.abs(m);
        			nodes[i+k][j+m].addNeighbor(nodes[i][j], cost);
        			
        		}
			}
        	
        }}
	}
	
	private PriorityQueue<NodeLabel> tNodes = new PriorityQueue<NodeLabel>();
	private HashSet<NodeLabel> finalized = new HashSet<NodeLabel>();
	private HashSet<NodeLabel> rejects = new HashSet<NodeLabel>();
	
	private void resetDijkstra(){
		finalized = new HashSet<NodeLabel>(boardSize*boardSize);
		rejects = new HashSet<NodeLabel>();
		tNodes = new PriorityQueue<NodeLabel>();
		
    	for(int i = 0; i < boardSize; i++){
        	for(int j = 0; j < boardSize; j++){
        		nodes[j][i].flip(false);
		        nodes[j][i].tempCost = Integer.MAX_VALUE;
		        nodes[j][i].parent = null;
		        

        	}
    	}
	}
	
	/**
	 * Reset the board and begins a new round of path-finding
	 * @param source
	 */
	private void doDijkstra(NodeLabel source){
		source.parent = null;
		source.finalCost = 0;
		resetDijkstra();

		dijk(source);
    	for(int i = 0; i < boardSize; i++){
        	for(int j = 0; j < boardSize; j++){
        		if(!finalized.contains(nodes[j][i])){
        			nodes[j][i].setBackground(DijkstraVisual.COLOR_REJECT);
        			rejects.add(nodes[j][i]);
        		}
        	}
        }

	}
	
	private void dijk(NodeLabel node){
		finalized.add(node);
		HashMap<NodeLabel, Integer> neighborhood = node.neighborhood();
		for(NodeLabel n : neighborhood.keySet()){
			if(!finalized.contains(n)){
				if(n.tempCost > node.finalCost + neighborhood.get(n)){
					n.tempCost = node.finalCost + neighborhood.get(n);
					n.parent = node;
				}
				if(!tNodes.contains(n))
					tNodes.add(n);
			}
		}

		NodeLabel n = tNodes.poll();
		if(null == n)
			return;
		//n.parent = node;
		n.finalCost = n.tempCost;
		if(n.neighborhood().isEmpty()){
			
		}
		dijk(n);
		
	}
	
	private void lightParents(final NodeLabel node, final boolean on){
		if(null == node)
			return;    
		node.flip(on);
		lightParents(node.parent, on);
			
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
class NodeLabel extends JLabel implements Comparable<NodeLabel>{
	
	public final int x;	//x position
	public final int y;	//y position
	public int tempCost;
	public int finalCost;
	
	public NodeLabel parent;
	private HashMap<NodeLabel, Integer> neighbors;
	private boolean isLit;
	
	public NodeLabel(int x, int y){
		
		super();
		tempCost = Integer.MAX_VALUE;
		this.x = x;
		this.y = y;
		neighbors = new HashMap<NodeLabel, Integer>();
		this.setText("   ");
		this.setHorizontalAlignment(CENTER);
		this.setForeground(Color.WHITE);
        this.setBackground(new Color(0,0,0)); //spots are black until opponent connects
        this.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
        this.setFocusable(false); //don't "focus" on it, in the window sense
        this.setInheritsPopupMenu(false);
        this.setMaximumSize(new Dimension(15, 15));
        //this.setMinimumSize(new Dimension(30, 30));
        this.setOpaque(true);
        this.setPreferredSize(new Dimension(15, 15));
        this.setRequestFocusEnabled(false); //we don't want to tab to it

		
		
	}
	
	public void resetNeighborhood() {
		neighbors = new HashMap<NodeLabel, Integer>();
		
	}

	public void addNeighbor(NodeLabel neighbor, int cost){
		neighbors.put(neighbor, cost);
	}
	
	public HashMap<NodeLabel, Integer> neighborhood(){
		return neighbors;
	}
	
	public void flipNeighbors(final boolean on){
//when done "properly", it's slow and misses some flips (maybe unflips too..)
 //   	SwingUtilities.invokeLater(new Runnable() {
 //           public void run() {
            	for(NodeLabel node : neighbors.keySet()){
            		
            			flip(on, node);
            	}
//            }
//        });
	}
	
	public void flip(final boolean on){
		isLit = on;
		if(on){
			setBackground(DijkstraVisual.COLOR_PATH);
			setText(String.valueOf(finalCost));
			
		} else { 
			setBackground(Color.BLACK);
			setText("   ");
		}
	
	}
	
	public String flipTemp;
	public Color colorTemp;
	private void flip(final boolean on, final NodeLabel node){
		//when done "properly", it's slow and misses some flips (maybe unflips too..)
//    	SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
				if(on){
					node.flipTemp = node.getText();
					node.colorTemp = node.getBackground();
					node.setBackground(DijkstraVisual.COLOR_NEIGHBOR);
					node.setText(String.valueOf(neighbors.get(node)));
					
				} else{ 
					if(!node.isLit){
						node.setBackground(node.colorTemp);
						node.setText("   ");
					} else {
						node.setBackground(DijkstraVisual.COLOR_PATH);
						node.setText(String.valueOf(node.flipTemp));
					}
				}
//			}
//		});
	}
	
	@Override
	public int compareTo(NodeLabel n1) {
		
		if(n1.tempCost < this.tempCost)
			return 1;
		else if(n1.tempCost > this.tempCost)
			return -1;
		return 0;
	}


}
