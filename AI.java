import java.util.*;

public class AI {
   private Random rand;
   private int maxDepth = 10;
   private int count = 0;
   private int[][] pelletLocations;
   private final int INITIAL_ALPHA = -1000000, INITIAL_BETA = 1000000;
   
   public AI(int[][] pelletLocations) {
      rand = new Random();
      this.pelletLocations = pelletLocations;
   }
   
   public void increaseMaxDepth() {
      maxDepth--;
   }
   
   public Direction getMove(ArrayList<Direction> legalMoves, State initialState) {
      // If ticksScared is 0, then godMode is false
      
      ArrayList<Direction> bestMoves = new ArrayList<Direction>();
      
      int[][] testBoard = initialState.getBoard()
      ;
      int maxScore = -100000;
      int score = maxScore;
      
      //System.out.println("---------------");
      for (int i = 0; i < legalMoves.size(); i++) {
         State currentState = generateState(legalMoves.get(i), 0, initialState);
         score = minimax(currentState, 1, maxDepth, INITIAL_ALPHA, INITIAL_BETA);
         //System.out.println("Direction: " + legalMoves.get(i) + " Score: " + score + " Count: " + count);
         count = 0;
         
         if (score > maxScore) {
            maxScore = score;
            bestMoves.clear();
            bestMoves.add(legalMoves.get(i));
         }
         else if (score == maxScore) {
            bestMoves.add(legalMoves.get(i));
         }
      }
      
      return bestMoves.get(rand.nextInt(bestMoves.size()));
   }
   
   public int[][] copyBoard(int[][] matrix) {
      int[][] myInt = new int[matrix.length][];
      for(int i = 0; i < matrix.length; i++) {
         myInt[i] = matrix[i].clone();
      }
      return myInt;
   }
   
   // Takes a state and an action, and generates a new state
   public State generateState(Direction d, int entityID, State s) {
      Entity[] entities = s.getEntities().clone();
      int ticksScared = s.getTicksScared();
      
      // If a ghost is scared it moves every other turn
      if (entities[entityID].isVulnerable() && ticksScared % 2 == 0)
         return s;
      
      int[][] board = s.getBoard();
      int score = s.getScore();
      int pelletsRemaining = s.getPelletsRemaining();
      
      for (int i = 0; i < entities.length; i++) {
         try {
            entities[i] = (Entity) ((Entity) entities[i]).clone();
         }
         catch (CloneNotSupportedException c) {
            System.out.println("Clone not supported");
         }
      }
      
      Entity currentEntity = entities[entityID];
      
      currentEntity.setDirection(d);
      currentEntity.moveForward();
      
      if (entityID == 0) {
         score -= 1;
         
         // If we are in god mode, increase the ticksScared
         if (entities[0].isGodMode()) {
            ticksScared++;
         }
         if (ticksScared > 64) {
            ticksScared = 0;
            entities[0].setGodMode(false);
            for (int i = 1; i < 5; i++) {
               entities[i].setVulnerable(false);
            }
         }
          
         if (board[currentEntity.getX()][currentEntity.getY()] == 1) {
            pelletsRemaining--;
         }
         else if (board[currentEntity.getX()][currentEntity.getY()] == 8) {
            ticksScared = 0;
            pelletsRemaining--;
            // Don't eat a power pellet while in god mode
            if (entities[0].isGodMode() || moreThanOneInSpawn(entities)) {
               score = -25000;
            }
            else 
               entities[0].setGodMode(true);
            for (int i = 1; i < 5; i++) {
               entities[i].setVulnerable(true);
            }
         }
      }
      score += checkCollisions(entities, s);
      
      
      return new State(board, entities, score, ticksScared, pelletsRemaining);
   }
   
   public Entity cloneEntity(Entity e) {
      try {
         e = (Entity) ((Entity) e).clone();
      }
      catch (CloneNotSupportedException c) {
         System.out.println("Clone not supported");
      }
      return e;
   }
   
   public Entity[] cloneEntities(Entity[] entities) {
      for (int i = 0; i < entities.length; i++) {
         entities[i] = cloneEntity(entities[i]);
      }
      return entities;
   }
    
   public boolean moreThanOneInSpawn(Entity[] entities) {
      for (int i = 1, total = 0; i < 5; i++) {
         if (insideSpawn(entities[i].getX(), entities[i].getY()))
            total++;
         if (total == 2) {
            return true;
         }
      }
      return false;
   }
   
   // Returns a score increase based on collisions
   public int checkCollisions(Entity[] entities, State s) {
      int score = 0;
      Entity player = entities[0];
      for (int i = 1; i < 5; i++) {
         Entity gh = entities[i];
         if (player.getX() == gh.getX() && player.getY() == gh.getY()) {
            if (gh.isVulnerable()) {
               score += 2000 - s.getTicksScared() * 10;
               gh.setPosition(13, 14);
               gh.setVulnerable(false);
            }
            else {
               score -= 100000;
            }
         }
      }
      return score;
   }
   
   public int getPelletDistance(int[][] board, int xPos, int yPos) {
      int minDist = 10000;
      int x, y;
      
      for (int i = 0; i < pelletLocations.length; i++) {
         x = pelletLocations[i][0];
         y = pelletLocations[i][1];
         if (board[x][y] == 0)
            continue;
         minDist = Math.min(minDist, manhattanDistance(x, xPos, y, yPos));
      }
      return minDist;
   }
   
   public boolean insideSpawn(int x, int y) {
      return x <= 16 && x >= 10 && y >= 13 && y <= 16;
   }
   
   public int getGhostDistance(Entity[] entities, int xPos, int yPos, int ticksScared) {
      int minDist = 100;
      int x, y, tempDist;
      
      for (int i = 0; i < 4; i++) {
         x = entities[i].getX();
         y = entities[i].getY();
         if (!entities[i].isVulnerable() || insideSpawn(x, y)) 
            continue;
         tempDist = manhattanDistance(x, xPos, y, yPos);
         // Ghosts move 1 block for every 2 blocks that pacman moves
         // Dont chase if it's out of reach
//          if (tempDist * 1.5 > 64 - ticksScared)
//             continue;
         minDist = Math.min(minDist, tempDist);
      }
      return minDist;
   }
   
   public int manhattanDistance(int x1, int x2, int y1, int y2) {
      return Math.abs(x2 - x1) + Math.abs(y2 - y1);
   }
   
   public boolean outOfRange(Entity ghost, Entity player, int depth) {
      int x = ghost.getX();
      int y = ghost.getY();
      int playerX = player.getX();
      int playerY = player.getY();
      
      int dist = manhattanDistance(x, playerX, y, playerY);
      
      if (dist <= depth * 2)
         return false;
         
      // Tp1 left, Tp2 right, Tp3 top, Tp4 bottom
      int tp1Dist, tp2Dist;
      
      // Find the distance from ghost to tp1, and from tp2 to pacman
      tp1Dist = manhattanDistance(x, 0, y, 14);
      tp2Dist = manhattanDistance(playerX, 26, playerY, 14);
      // Add the distances together to get the total distance from pacman
      dist = Math.min(dist, tp1Dist + tp2Dist);
      
      if (dist <= depth * 2)
         return false;
      
      tp1Dist = manhattanDistance(playerX, 0, playerY, 14);
      tp2Dist = manhattanDistance(x, 26, y, 14);
      // Add the distances together to get the total distance from pacman
      dist = Math.min(dist, tp1Dist + tp2Dist);
      
      if (dist <= depth * 2)
         return false;
      
      // Get dist to middle of 2 tps and subtract 1
      tp1Dist = manhattanDistance(x, 13, y, 0) - 1;
      tp2Dist = manhattanDistance(playerX, 13, playerY, 29);
      // Add the distances together to get the total distance from pacman
      dist = Math.min(dist, tp1Dist + tp2Dist);
      
      if (dist <= depth * 2)
         return false;
      
      tp1Dist = manhattanDistance(playerX, 13, playerY, 0) - 1;
      tp2Dist = manhattanDistance(x, 13, y, 29);
      dist = Math.min(dist, tp1Dist + tp2Dist);
      
//       if (dist > depth * 2 && ghost.getEntityID() == 4 && depth == maxDepth)
//          System.out.println("Out of range at depth " + depth);
         
      return dist > depth * 2;
   }
   
   public int evaluationFunction(State s) {
      int score = s.getScore();
      Entity[] entities = s.getEntities();
      int xPos = entities[0].getX();
      int yPos = entities[0].getY();
      
      // Give 1000 points per ghost eaten
      // Reward for eating at the earliest ticksScared value to encourage quick eating
      // This has to override the instinct to not eat due to the punishment from ghost distance
      
      // If the ghosts are scared
      if (entities[0].isGodMode()) {
         // score -= 10 * s.getTicksScared();
         // Chase ghosts if you are in god mode
         int distPenalty = getGhostDistance(entities, xPos, yPos, s.getTicksScared());
         
         // If no ghosts are available
         if (distPenalty == 100) {
            score += 100 * (236 - s.getPelletsRemaining());
            score -= getPelletDistance(s.getBoard(), xPos, yPos);
         }
         else {
            score -= distPenalty; 
            // Give a small reward for taking the route that gathers most pellets
            // If there are 6 pellets on a route that takes 1 more block, take that route
            //score += (int) (0.2 * (236 - s.getPelletsRemaining()));
         }
         
         //System.out.println("Score: " + score + " TicksScared: " + s.getTicksScared() + " ghostDist: " + getGhostDistance(entities, xPos, yPos));
      }
      else {
         // Decrease the score by 100 for every pellet present
         // There are 232 total pellets
         score -= 100 * s.getPelletsRemaining();
         
         // Penalize for being far from a pellet to encourage pellet seeking
         score -= getPelletDistance(s.getBoard(), xPos, yPos);
         //System.out.println(getPelletDistance(s.getBoard(), xPos, yPos));
      }
      
      return score;
   }
   
   public int minimax(State state, int entityID, int depth, int alpha, int beta) {
      count++;
      
      Entity[] entities = state.getEntities();
      if (depth == 0) {
         return evaluationFunction(state);
      }  
      // Loss
      else if (state.getScore() <= -50000) {
         return -50000 - depth;
      }
      // Win
      else if (state.getPelletsRemaining() == 0 && !entities[0].isGodMode()) {
         return 50000 + depth;
      }
         
      entityID = entityID % 5;
      
      ArrayList<Direction> legalMoves = entities[entityID].getLegalMoves();
      State possibleState;
      
      
      // If the ghost is inside of the spawn box, skip it
      if (entityID != 0 && (insideSpawn(entities[entityID].getX(), entities[entityID].getY()) || outOfRange(entities[entityID], entities[0], depth))) {
         if (entityID == 4)
            depth--;
         return minimax(state, entityID + 1, depth, alpha, beta);
      }
      // Max node
      // Pink Ghost and Pacman
      // Will always try to avoid pacman
      // Can only kill pacman if it has 1 move going towards him
      // If there is more than one move, maximize pacman's score
      // If pink ghost is scared, he minimizes pacman's score
      
      // If there is just one possible move, use the average node
      if (entityID == 0 || (entityID == 3 && legalMoves.size() > 1 && !entities[entityID].isVulnerable())) {
         int score = INITIAL_ALPHA;
         
         for (int i = 0; i < legalMoves.size(); i++) {
            possibleState = generateState(legalMoves.get(i), entityID, state);
            
            score = Math.max(minimax(possibleState, entityID + 1, depth, alpha, beta), score);
            
            alpha = Math.max(alpha, score);
            
            if (beta <= alpha)
               break;
         }
         return score;
      }
      // Red Ghost
      else if (entityID == 4) {
         depth--;
      }
      // Min node
      int score = INITIAL_BETA;
      
      for (int i = 0; i < legalMoves.size(); i++) {
         possibleState = generateState(legalMoves.get(i), entityID, state);
         
         score = Math.min(minimax(possibleState, entityID + 1, depth, alpha, beta), score);
         
         beta = Math.min(beta, score);
         
         if (beta <= alpha)
            break;
      }
      return score;
   }
}