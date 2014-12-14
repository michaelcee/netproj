package dijvis;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

import javax.swing.*;
import javax.swing.border.Border;



/**
 * DijkstraVisualizer
 * contains both Dijkstra and GUI code.  This should be refactored to have the
 * GUI components distinct from the Dijkstra's components but I don't feel it now
 * 
 * @author MIKE
 *
 */
@SuppressWarnings("serial")
public class DijkstraVisual extends JFrame {
	
	  
    public final static Color COLOR_PATH = new Color(100,100,255);
    public final static Color COLOR_NEIGHBOR = new Color(255,100,100);
    public final static Color COLOR_REJECT = new Color(150, 160, 150);
    
    private static String dim = "20";
    private static String cost = "10";
    private static String chance = ".3";
    
    private int boardSizeI;//the board will always be sized via a single number
    private int boardSizeJ;//...but we save the second dimension for convenience
    
    //initialize UI components
    private JCheckBox checkBox = new JCheckBox("Double width");
    private JButton resizeButton = new JButton("Redim Board");
    private JButton rerollButton = new JButton("Reroll");
    private JTextField costField = new JTextField(cost);
    private JTextField chanceField = new JTextField(chance);
    private JTextField sizeField = new JTextField(dim);
    private Label msgLabel = new Label("Left click to set source.  Right click to show neighbors.");
    private JPanel msgPanel;
    private JPanel controls;
    private JPanel boardGrid;

    /** [0] = default border, [1] = hover border, [2] = source node border*/
    private Border[] borArr;
    /** a temp var for restoring color after mouse exit on hoverings */
    private Color hoverTemp;
    /**all NodeLabels.  access using nodes[y][x]*/
    private NodeLabel[][] nodes;
    private NodeLabel sourceNode;//which node we've clicked on
    private NodeLabel prevNode;
    private final DijkstraVisual instance;
    private boolean mouseDown; //if the mouse is down

    /**
     * no args from command line, just launch window with default settings
     * @param args
     */
    public static void main(String[] args){

    	DijkstraVisual bpg = new DijkstraVisual("Dijkstra Visualizer", 20, false);
    	bpg.launch();
    }
    
     
    /**
     * creates some resources, initializes the board and calls the super 
     * constructor but does not make anything visible
     * @param name
     * @param client
     */
    public DijkstraVisual(String name, int size, boolean doubleWide) {
    	super(name);    
        instance = this;    	
    	borArr = new Border[3];
    	//due to the nature of borders, they have to init here
    	
    	borArr[0] = BorderFactory.createLineBorder(new Color(0, 0, 0));
    	borArr[1] = BorderFactory.createLineBorder(new Color(255, 255, 255));
    	borArr[2] = BorderFactory.createLineBorder(Color.RED);
        setResizable(true);
        initBoard(size, doubleWide);
        if(doubleWide)
        	checkBox.setSelected(true);
        
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
     * start the process of showing the GUI.  we have to launch on new thread 
     * otherwise callers would get locked up and callers involve other swing
     * components
     */
    private void launch() {

         
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();

            }
        });
        
        
    }
    
    /**
     * initialize the board of nodes with the given parameters.  after all
     * objects are created, the board is randomized using the values of the
     * controls
     * 
     * @param size
     * @param doubleWide if we want height = size; width = size * 2
     */
    private void initBoard(int size, boolean doubleWide){
    	boardSizeI = size;
    	boardSizeJ = doubleWide ? size * 2: size;
    	
    	nodes = new NodeLabel[boardSizeJ][boardSizeI];
    	for(int i = 0; i < boardSizeI; i++){
        	for(int j = 0; j < boardSizeJ; j++){
		        NodeLabel spot = new NodeLabel(j, i);
		        nodes[j][i] = spot;
		        spot.addMouseListener(new MouseAdapter() {
		            public void mouseEntered(MouseEvent evt) {
		                nodeMouseEntered(evt);
		            }
		            public void mouseExited(MouseEvent evt) {
		                nodeMouseExited(evt);
		            }
		            public void mouseClicked(MouseEvent evt) {
		                nodeMouseClicked(evt);
		            }
		            
		            public void mousePressed(MouseEvent evt) {
		                nodeMouseDown(evt);
		            }
		            
		            public void mouseReleased(MouseEvent evt){
		            	nodeMouseUp(evt);
		            }
		        });
		        
        	}
        }
    	
    	
    	//
        boardGrid = new JPanel();
        GridLayout boardLayout = new GridLayout(boardSizeI,boardSizeJ);
        boardGrid.setLayout(boardLayout);
        boardLayout.setHgap(1);
        boardLayout.setVgap(1);
        boardGrid.setBackground(new Color(100, 100, 100));
        int pSize = doubleWide ? 1040 : 520;
        boardGrid.setPreferredSize(new Dimension(pSize, 520));

        randomizeBoard(Integer.valueOf(costField.getText()), 
        		Double.valueOf(chanceField.getText()));

        for(int i = 0; i < boardSizeI; i++){
        	for(int j = 0; j < boardSizeJ; j++){
		        boardGrid.add(nodes[j][i]);
        	}
        }
      
    }
      
    /**
     * add and layout of all our components for display
     * @param pane
     */
    private void addComponentsToPane(final Container pane) {
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
        JLabel connectLabel = new JLabel("Chance:");
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
        checkBox.setHorizontalAlignment(JLabel.RIGHT);
        checkBox.setBackground(Color.WHITE);
        checkBox.setOpaque(true);
        controls.add(checkBox);
        
        //r2,c3
        JPanel sizePanel = new JPanel();
        sizePanel.setBackground(Color.WHITE);
        JLabel sizeLabel = new JLabel("Board size:");
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
                		DijkstraVisual bpg = new DijkstraVisual("Dijkstra Visualizer", Integer.valueOf(dim), checkBox.isSelected());
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
            	for(int i = 0; i < boardSizeI; i++){for(int j = 0; j < boardSizeJ; j++){
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
    		if(!rejectNodes.contains(prevNode)){
    	    	if(prevNode != sourceNode ){
    	    		prevNode.setBackground(hoverTemp);//default background color
    	    		prevNode.setBorder(borArr[0]);
    	    		
    	    	}
    	    	lightParents(prevNode, false);
        	}
    	}
    	
    	
    	NodeLabel node = (NodeLabel) evt.getSource(); 
    	prevNode = node;
    	if(!rejectNodes.contains(node)){
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
    		node.setBackground(COLOR_PATH);
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
     * 
     * @param maxCost maximum cost of a edge
     * @param chance the chance that any two neighboring nodes are connected
     */
	private void randomizeBoard(int maxCost, double chance) {
		Random r = new Random();

		//iterate through the board and determine connections between adjacent
		//nodes.  also, I'm seeing if I like this for loop style
		for(int i = 0; i < boardSizeI; i++){for(int j = 0; j < boardSizeJ; j++){
			//iterate through the up to 8 adjacent nodes.  
			//this is going to get confusing..
			for(int k = -1;k < 2; k++){
				for(int m = -1; m < 2; m++)
				
        		if( (m|k) != 0 && chance > r.nextDouble() && //if they're not both zero
        				i + k >=0 && i + k < boardSizeI && //not out of bounds
        				j + m >=0 && j +m < boardSizeJ){

        			int cost = r.nextInt(maxCost) + Math.abs(k) + Math.abs(m);
        			nodes[j+m][i+k].addNeighbor(nodes[j][i], cost);
        			
        		}
			}
        	
        }}
	}
	
	private PriorityQueue<NodeLabel> tempNodes = new PriorityQueue<NodeLabel>();
	private HashSet<NodeLabel> finalizedNodes = new HashSet<NodeLabel>();
	private HashSet<NodeLabel> rejectNodes = new HashSet<NodeLabel>();
	
	private void resetDijkstra(){
		finalizedNodes = new HashSet<NodeLabel>(boardSizeJ*boardSizeI);
		rejectNodes = new HashSet<NodeLabel>();
		tempNodes = new PriorityQueue<NodeLabel>();
		
    	for(int i = 0; i < boardSizeI; i++){
        	for(int j = 0; j < boardSizeJ; j++){
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
	private void doDijkstra(final NodeLabel source){
		source.parent = null;
		source.finalCost = 0;
		resetDijkstra();

		dijk(source);
    	for(int i = 0; i < boardSizeI; i++){
        	for(int j = 0; j < boardSizeJ; j++){
        		if(!finalizedNodes.contains(nodes[j][i])){
        			nodes[j][i].setBackground(DijkstraVisual.COLOR_REJECT);
        			rejectNodes.add(nodes[j][i]);
        		}
        	}
        }

	}
	
	/**
	 * a recursive step of Dijkstra's algorithm
	 * @param node
	 */
	private void dijk(NodeLabel node){
		finalizedNodes.add(node);
		HashMap<NodeLabel, Integer> neighborhood = node.neighborhood();
		for(NodeLabel n : neighborhood.keySet()){
			if(!finalizedNodes.contains(n)){
				int tCost = node.finalCost + neighborhood.get(n);
				if(n.tempCost > tCost){
					n.tempCost = tCost;
					n.parent = node;
				}
				if(!tempNodes.contains(n))
					tempNodes.add(n);
			}
		}

		NodeLabel n = tempNodes.poll();
		if(null == n)
			return;
		n.finalCost = n.tempCost;

		dijk(n);
		
	}
	
	/**
	 * for highlighting or unhighlighting the shortest path.  this method works 
	 * backwards, from the destination node and recursively highlights parent 
	 * nodes until the source node is reached.
	 * @param node
	 * @param on
	 */
	private void lightParents(NodeLabel node, boolean on){
		if(null == node)
			return;    
		node.flip(on);
		lightParents(node.parent, on);
			
	}


	
}
/**
 * this class extends JLabel to provide the functionality of a node on the board
 * 
 * each node has a set of x,y coordinates, a temporary cost, a final cost and a
 * parent node.  Nodes also keep track their neighbors and the related cost.
 * 
 * utility methods are provided to highlight neighbors and the node itself
 * 
 * nodes also implement comparable so they can be used in a priority queue by
 * Dijkstra's algorithm.  
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

    	for(NodeLabel node : neighbors.keySet()){
    			flip(on, node);
    	}

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
