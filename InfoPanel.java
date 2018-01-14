import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class InfoPanel extends JPanel {
   private int score = 0;
   private int ghostsEaten = 0;
   private int stage = 1;
   private Font font;
   
   public InfoPanel() {
      setPreferredSize(new Dimension(648, 60));
      setBackground(Color.DARK_GRAY.darker());  
      
      font = new Font("", Font.PLAIN, 50);
   }
   
   public void increaseScore(int amt) {
      score += amt;
      
      repaint();
   }
   
   public void increaseGhostsEaten() {
      ghostsEaten++;
      
      repaint();
   }
   
   public void increaseStage() {
      stage++;
      
      System.out.println("Increase Stage");
      repaint();
   }
   
   public void paint(Graphics g) {
      super.paint(g);
       
      g.setFont(font);
      g.setColor(Color.WHITE);
      g.drawString("" + score, 5, 50);
      
      int stringWidth = g.getFontMetrics().stringWidth("" + ghostsEaten);
      g.drawString("" + ghostsEaten, getWidth() - stringWidth - 5, 50);
      
      stringWidth = g.getFontMetrics().stringWidth("" + stage);
      g.drawString("" + stage, getWidth() / 2 - stringWidth / 2, 50);
   }
}