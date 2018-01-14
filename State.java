public class State {
   private Entity[] entities;
   private int[][] board;
   private int score, ticksScared, pelletsRemaining;
   
   public State(int[][] board, Entity[] entities, int score, int ticksScared, int pelletsRemaining) {
      this.board = board;
      this.entities = entities;
      this.score = score;
      this.ticksScared = ticksScared;
      this.pelletsRemaining = pelletsRemaining;
   }
   
   public int[][] getBoard() {
      return board;
   }
   
   public int getTicksScared() {
      return ticksScared;
   }
   
   public Entity[] getEntities() {
      return entities;
   }
   
   public int getScore() {
      return score;
   }
   
   public int getPelletsRemaining() {
      return pelletsRemaining;
   }
}