import java.util.*;

public class Entity implements Cloneable {
   // Which number this ghost is represented by on the board
   private int entityID;
   private int xPos, yPos, lastX, lastY;
   private Direction direction;
   private DisplayPanel display;
   private Random rand;
   private boolean vulnerable = false;
   private boolean teleporting = false;
   private boolean godMode = false;
   
   public Entity(DisplayPanel display, int x, int y, Direction d, int entityID) {
      xPos = lastX = x;
      yPos = lastY = y;
      this.display = display;
      this.entityID = entityID;
      direction = d;
      rand = new Random();
   }

   public boolean canTurn(Direction direction) {
      int[][] board = display.getBoard();
      
      int x = xPos;
      int y = yPos;
      
      if (direction == Direction.NORTH) 
         y--;
      else if (direction == Direction.EAST)
         x++;
      else if (direction == Direction.SOUTH) 
         y++;
      else
         x--;
      
      boolean outOfBounds = x < 0 || x >= display.getNumCols() || y < 0 || y >= display.getNumRows();
      // return true if we are going to teleport (use some short-circuit evaluation)
      return outOfBounds || (board[x][y] != 7 && (board[x][y] != 2 || direction == Direction.NORTH));
   }
  
   public boolean isVulnerable() {
      return vulnerable;
   }
   
   public void setVulnerable(boolean b) {
      vulnerable = b;
   }
  
   // Test if the front is clear without actually moving pacman (cuz multithreads)
   public boolean frontIsClear() {
      return display.frontIsClear(direction, xPos, yPos);
   }   
   
   public void setDirection(Direction direction) {
      this.direction = direction;
   }
   
   public Direction getDirection() {
      return direction;
   }
   
   public int getDirectionAsInt() {
      if (direction == Direction.EAST) 
         return 0;
      else if (direction == Direction.NORTH)
         return 1;
      else if (direction == Direction.WEST)
         return 2;
      else
         return 3;
   }
   
   public int getX() {
      return xPos;
   }
   
   public int getY() {
      return yPos;
   }
   
   public int getLastX() {
      return lastX;
   }
   
   public int getLastY() {
      return lastY;
   }
   
   public void setPosition(int x, int y) {
      xPos = x;
      yPos = y;
   }
   
   public void setLastPosition() {
      lastX = xPos;
      lastY = yPos;
   }
   
   public int getEntityID() {
      return entityID;
   }
   
   public boolean isGodMode() {
      return godMode;
   }
   
   public void setGodMode(boolean g) {
      godMode = g;
   }  
   
   public void moveX(int dx) {
      xPos += dx;
   }
   
   public void moveY(int dy) {
      yPos += dy;
   }
   
   public void move() {
      if (direction == Direction.NORTH) 
         yPos--;
      else if (direction == Direction.EAST)
         xPos++;
      else if (direction == Direction.SOUTH) 
         yPos++;
      else
         xPos--;
   }
   
   public void moveForward() {
      int[][] board = display.getBoard();
      
      teleporting = board[xPos][yPos] == 9;
      
      move();
      
      if (teleporting) {
         if (xPos == -1) {
            xPos = display.getNumCols() - 1;
            lastX = display.getNumCols();
         }
         else if (xPos == display.getNumCols()) {
            xPos = 0;
            lastX = -1;
         }
         else if (yPos == -1) {
            xPos = 13;
            yPos = display.getNumRows() - 1;
            lastX = 13;
            lastY = display.getNumRows();
         }
         else if (yPos == display.getNumRows()) {
            if (Math.random() < .5) 
               lastX = xPos = 12;
            else 
               lastX = xPos = 14;
            yPos = 0;
            lastY = -1;
         } 
      }
   }
   
   public ArrayList<Direction> getLegalMoves() {
      ArrayList<Direction> legalTurns = new ArrayList<Direction>();
      Direction[] allDirections = Direction.values();
      for (int i = 0; i < allDirections.length; i++) {
         Direction pickedDirection = allDirections[i];
         if (canTurn(pickedDirection) && (!isOppositeDirection(pickedDirection) || entityID == 0)) {
            legalTurns.add(pickedDirection);
         }
      }
      return legalTurns;
   }
   
   public void turn() {
      ArrayList<Direction> legalTurns = getLegalMoves();      
      // If we are in the spawn box
      if (xPos <= 16 && xPos >= 10 && yPos >= 12 && yPos <= 16) {
         randomTurn(legalTurns);
         return;
      }
      
      if(vulnerable) {
         findWorstTurn(legalTurns); 
      } 
      else {
         // Red ghost
         if (entityID == 4)
            findBestTurn(legalTurns);
         // Cyan Ghost   
         else if (entityID == 3)
            findWorstTurn(legalTurns);
         else
            randomTurn(legalTurns);  
      }       
   }
   
   public void randomTurn(ArrayList<Direction> legalTurns) {
      direction = legalTurns.get(rand.nextInt(legalTurns.size()));
   }
   
   public int[] positionInDirection(int x, int y, Direction d) {
      if (d == Direction.NORTH) 
         y--;
      else if (d == Direction.EAST)
         x++;
      else if (d == Direction.SOUTH) 
         y++;
      else
         x--;
      
      return new int[] {x, y};
   }
   
   public void findBestTurn(ArrayList<Direction> legalTurns) {
      Entity player = display.getPlayer();
      int playerX = player.getX();
      int playerY = player.getY();
      int bestIndex = 0;
      double bestDist = 10000;
      for (int i = 0; i < legalTurns.size(); i++) {
         Direction d = legalTurns.get(i);
         int[] newPos = positionInDirection(xPos, yPos, d);
         double dist = findDistance(newPos[0], newPos[1], playerX, playerY);
         if (dist < bestDist) {
            bestIndex = i;
            bestDist = dist;
         }
      }
      direction = legalTurns.get(bestIndex);
   }
   
   public void findWorstTurn(ArrayList<Direction> legalTurns) {
      Entity player = display.getPlayer();
      int playerX = player.getX();
      int playerY = player.getY();
      int bestIndex = 0;
      double bestDist = -10000;
      for (int i = 0; i < legalTurns.size(); i++) {
         Direction d = legalTurns.get(i);
         int[] newPos = positionInDirection(xPos, yPos, d);
         double dist = findDistance(newPos[0], newPos[1], playerX, playerY);
         if (dist > bestDist) {
            bestIndex = i;
            bestDist = dist;
         }
      }
      direction = legalTurns.get(bestIndex);
   }
   
   public double findDistance(int x1, int y1, int x2, int y2) {
      return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
   }
   
   private boolean isOppositeDirection(Direction pickedDirection) {
      if (direction == Direction.NORTH) {
         return pickedDirection == Direction.SOUTH;
      }
      else if (direction == Direction.EAST) {
         return pickedDirection == Direction.WEST;
      }
      else if (direction == Direction.SOUTH) {
         return pickedDirection == Direction.NORTH;
      }
      return pickedDirection == Direction.EAST;  
   }
   
   // Clone this object's data members
   public Object clone() throws CloneNotSupportedException {
      return super.clone();
  }
}