import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;

public class DisplayPanel extends JPanel implements ActionListener, KeyListener {
   private InfoPanel info;
   private EasySound ez;
   private AI ai;
   private Timer gameClock;
   private int delay = 120;
   private int numFrames = delay / 15;
   private int currentFrame = 0;
   private Entity player;
   private int numGhosts = 4;
   private Entity ghost1, ghost2, ghost3, ghost4;
   private Entity[] entities;
   private Color[] colors = new Color[] {Color.YELLOW, Color.GREEN, Color.CYAN.darker().darker(), Color.PINK, Color.RED};
   private int numRows = 30;
   private int numCols = 27;
   private int cellSize = 24;
   private int width = cellSize * numCols, height = cellSize * numRows;
   private boolean north = false, south = false, east = false, west = false; 
   private GameState gameState = GameState.IN_PROGRESS;
   private int ticksScared = 0;
   private int scaredTime = 64;
   private int pelletsRemaining = 236;
   private int[][] pelletLocations = new int[236][2];
   private int ghostsEaten = 0;
   private int score = 0;
   private boolean usingAI = false;
   private boolean paused = false;
   
   // Used to move the vulnerable ghosts once every two turns
   private boolean alternate = false;
   
   /* Board is accessed using [y][x] instead of [x][y]
      Meaning that the desired row = y, desired column = x
      The board is encoded using 0 = no pellet, 1 = pellet, 2 = player, 3 = ghost1, 
      4 = ghost2, 5 = ghost3, 6 = ghost4, wall = 7 power-pellet = 8 teleport = 9
   */
   private int[][] board;  
   
   public DisplayPanel(InfoPanel info) {
      this.info = info;
      
      setPreferredSize(new Dimension(width, height));
      setBackground(Color.BLACK);   
      
      // Spawn a new player at this position
      player = new Entity(this, 13, 23, Direction.NORTH, 0);
      createGhosts();
      entities = new Entity[] {player, ghost1, ghost2, ghost3, ghost4};
      board = parseBoard();
      
      Thread thread = new Thread(new EasySound("waka.wav"));
      thread.start();
      
      
      ai = new AI(pelletLocations);
      
      // Update the frame numFrames times within each move
      gameClock = new Timer(delay / numFrames, this);
      addKeyListener(this);
      
      setFocusable(true);
      requestFocusInWindow();
      gameClock.start();
   }
   
   public void createGhosts() {
      ghost1 = new Entity(this, 11, 14, Direction.EAST, 1);
      ghost2 = new Entity(this, 13, 14, Direction.NORTH, 2);
      ghost3 = new Entity(this, 15, 14, Direction.WEST, 3);
      ghost4 = new Entity(this, 13, 11, Direction.EAST, 4);
   }
   
   public void keyPressed(KeyEvent e) {
      if (e.getKeyChar() == 'w') {
         north = true;  
      }
      else if (e.getKeyChar() == 'a') {
         west = true;  
      }
      else if (e.getKeyChar() == 's') {
         south = true;  
      }
      else if (e.getKeyChar() == 'd') {
         east = true;  
      }     
      else if (e.getKeyChar() == ' ') {
         usingAI = !usingAI;
      }       
      else if (e.getKeyChar() == 'f') {
         paused = !paused;
      }
   }
   public void keyReleased(KeyEvent e) {
      if (e.getKeyChar() == 'w') {
         north = false;
      }
      else if (e.getKeyChar() == 'a') {
         west = false;
      }
      else if (e.getKeyChar() == 's') {
         south = false;
      }
      else if (e.getKeyChar() == 'd') {
         east = false;
      }           
   }
   public void keyTyped(KeyEvent e) {}
   
   public int getWidth() {
      return width;
   }
   
   // parses the board into an int[][] from a text file
   public int[][] parseBoard() {
      // change the board so that it can be accessed using [x][y]
      // Load all the people from a file
      String fileName = "board.txt";
      String line;
      
      board = new int[numCols][numRows];
      
      try {
         FileReader fileReader = new FileReader(fileName);
         BufferedReader bufferedReader = new BufferedReader(fileReader);
         
         // While there is something left to read
         int i = 0;
         for (int y = 0; y < numRows; y++) {
            line = bufferedReader.readLine();
            String[] parts = line.split(" ");
            for (int x = 0; x < numCols; x++) {
               board[x][y] = Integer.parseInt(parts[x]);
               if (board[x][y] == 1 || board[x][y] == 8) {
                  pelletLocations[i][0] = x;
                  pelletLocations[i][1] = y;
                  i++;
               }
            }
         }
      }
      catch(FileNotFoundException ex) {
            System.out.println("Unable to open file");                
      }
      catch(IOException ex) {
            System.out.println("Error reading file");
      }
      return board;
      
   }
   
   public void resetGame() {
      board = parseBoard();
      gameState = GameState.IN_PROGRESS;
      
      player = new Entity(this, 13, 23, Direction.NORTH, 0);
      ghost1 = new Entity(this, 11, 14, Direction.EAST, 1);
      ghost2 = new Entity(this, 13, 14, Direction.NORTH, 2);
      ghost3 = new Entity(this, 15, 14, Direction.WEST, 3);
      ghost4 = new Entity(this, 13, 11, Direction.EAST, 4);
      
      pelletsRemaining = 236;
      currentFrame = 0;
      delay -= 15;
      numFrames = delay / 15;
      info.increaseStage();
      if (delay == 105)
         ai.increaseMaxDepth();
      entities = new Entity[] {player, ghost1, ghost2, ghost3, ghost4};
   }
   
   // Called automatically when the timer "fires"
   public void actionPerformed(ActionEvent e) {
      if (paused)
         return;
      if (currentFrame == numFrames) {
         if (gameState == GameState.WIN) {
            resetGame();
            return;
         }
         else if (gameState == GameState.LOSS) {
            return;
         }
         currentFrame = 0;
         setLastPositions();
         alternate = !alternate; 
         updateGame();
      }
      repaint();
      currentFrame++;
   }
   
   public void updateGame() { 
      info.increaseScore(-1);
      score -=1;
      if (player.isGodMode()) {
         ticksScared++;
         if (ticksScared > scaredTime)
            setGodMode(false); 
      }     
      
      updatePlayer();
      checkCollisions();
      // Game over can occur and if so dont move the ghosts
      if (gameState != GameState.IN_PROGRESS) {
         return;
      }
      updateGhosts();
      checkCollisions();
   }
   
   public void setLastPositions() {
      for (int i = 0; i < numGhosts + 1; i++) {
         entities[i].setLastPosition();
      }
   }  
   
   public void updatePlayer() {
      if (usingAI) {
         State state = new State(board.clone(), entities, 0, ticksScared, pelletsRemaining);
         player.setDirection(ai.getMove(player.getLegalMoves(), state));
         
      }
      else {
         if (north && player.canTurn(Direction.NORTH)) {
            if (player.canTurn(Direction.NORTH))
               player.setDirection(Direction.NORTH);
         }
         else if (west && player.canTurn(Direction.WEST)) {
            if (player.canTurn(Direction.WEST))
               player.setDirection(Direction.WEST);
         }
         else if (south && player.canTurn(Direction.SOUTH)) {
            if (player.canTurn(Direction.SOUTH))
               player.setDirection(Direction.SOUTH);
         }
         else if (east && player.canTurn(Direction.EAST)) {
            if (player.canTurn(Direction.EAST))
               player.setDirection(Direction.EAST);
         }
      } 
      if (board[player.getX()][player.getY()] == 1) {
         board[player.getX()][player.getY()] = 0;
         pelletsRemaining--;
         info.increaseScore(10);
         score += 10;
      }
      // Win condition
      if (pelletsRemaining == 0 && !player.isGodMode()) {
         gameState = GameState.WIN;
         return;
      }   
      // Loss condition
      checkCollisions();
      if (gameState == GameState.LOSS)
         return;
      if (player.frontIsClear()) {
         player.moveForward();
      }  
      if (board[player.getX()][player.getY()] == 8) {
         pelletsRemaining--;
         board[player.getX()][player.getY()] = 0;
         info.increaseScore(20);
         score += 20;
         setGodMode(true);
      }
   }
   
   public void setGodMode(boolean b) {
      if (b) { 
         for (int i = 1; i < numGhosts + 1; i++) {
            entities[i].setVulnerable(true);
         }
         ticksScared = 0;
      }   
      else {
         for (int i = 1; i < numGhosts + 1; i++) {
            entities[i].setVulnerable(false);
         }
      }
      player.setGodMode(b);
   }
   
   public void updateGhosts() {
      // Check once before changing positions
      for (int i = 1; i < numGhosts + 1; i++) {
         Entity currentGhost = entities[i];
         if (currentGhost.isVulnerable() && alternate)
            continue;
         // Doesn't turn if it is not allowed to
         currentGhost.turn();
         currentGhost.moveForward();
      }
   }
   
   public void checkCollisions() {
      for (int i = 1; i < numGhosts + 1; i++) {
         Entity gh = entities[i];
         if (player.getX() == gh.getX() && player.getY() == gh.getY()) {
            if (gh.isVulnerable()) {
               ghostsEaten++;
               info.increaseScore(200);
               info.increaseGhostsEaten();
               score += 200;
               gh.setPosition(13, 14);
               gh.setVulnerable(false);
            }
            else {
               gameState = GameState.LOSS;
            }
         }
      }
   }
   
   public Entity getPlayer() {
      return player;
   }
   
   public int getNumCols() {
      return numCols;
   }  
   
   public int getNumRows() {
      return numRows;
   }
   
   public void paint(Graphics g) {
      super.paint(g);
      
      drawBoard(g);
      drawEntities(g);
   }
   
   public int[][] getBoard() {
      return board;
   }
   
   public void setCell(int x, int y, int v) {
      board[x][y] = v;
   }
   
   // Test if the front is clear without actually moving pacman (cuz multithreads)
   public boolean frontIsClear(Direction d, int xPos, int yPos) {
      int x = xPos;
      int y = yPos;
      
      if (d == Direction.NORTH) 
         y--;
      else if (d == Direction.EAST)
         x++;
      else if (d == Direction.SOUTH) 
         y++;
      else
         x--;
         
      boolean outOfBounds = x < 0 || x >= getNumCols() || y < 0 || y >= getNumRows();
      // return true if we are going to teleport (use some short-circuit evaluation)
      return outOfBounds || (board[x][y] != 7 && (board[x][y] != 2 || d == Direction.NORTH));
   }
   
   public void drawEntities(Graphics g) {
      //System.out.println(player.getLastX());
      for (int i = 0; i < numGhosts + 1; i++) {
         int dx = entities[i].getX() - entities[i].getLastX();
         int dy = entities[i].getY() - entities[i].getLastY();
         double startX = entities[i].getLastX() + currentFrame * 1.0 / numFrames * dx;
         double startY = entities[i].getLastY() + currentFrame * 1.0 / numFrames * dy;
         // Blink if there are 2 seconds left on the scaredTimer
         if (entities[i].isVulnerable() && (ticksScared < scaredTime * 3 / 4 || alternate)) 
            g.setColor(Color.WHITE);
         else 
            g.setColor(colors[i]);
         if (i == 0 && frontIsClear(player.getDirection(), player.getLastX(), player.getLastY()) && gameState == GameState.IN_PROGRESS) {
            int openingDegrees, degrees, startDegrees;
            if (alternate) {
               openingDegrees = 40;
            }
            else
               openingDegrees = 100;
            startDegrees = openingDegrees / 2 + 90 * entities[i].getDirectionAsInt();
            degrees = 360 - openingDegrees;
            
            //g.fillOval((int) (startX * cellSize), (int) (startY * cellSize), cellSize, cellSize);
            g.fillArc((int) (startX * cellSize), (int) (startY * cellSize), cellSize, cellSize, startDegrees, degrees);
         }
         else {
            g.fillOval((int) (startX * cellSize), (int) (startY * cellSize), cellSize, cellSize);
         }
      }
   }
   
   public void drawBoard(Graphics g) {
      for (int y = 0; y < numRows; y++) {
         for (int x = 0; x < numCols; x++) {
            // Draw different things
            int startX = x * cellSize;
            int startY = y * cellSize;
            
            switch(board[x][y]) {
               case 1:
                  g.setColor(Color.YELLOW);
                  g.fillRect(startX + cellSize / 3, startY + cellSize / 3, cellSize / 3, cellSize / 3);
                  break;
               case 2:
                  g.setColor(Color.WHITE);
                  g.fillRect(startX, startY, cellSize, cellSize / 6);
                  break;
               case 7:
                  g.setColor(Color.BLUE);
                  g.fillRect(startX, startY, cellSize, cellSize);
                  break;
               case 8:
                  if (alternate)
                     g.setColor(Color.BLACK);
                  else
                     g.setColor(Color.WHITE);
                  g.fillRect(startX + cellSize / 4, startY + cellSize / 4, cellSize / 2, cellSize / 2);
                  break;
               default:
                  break;
            }
         }
      }
   }
}