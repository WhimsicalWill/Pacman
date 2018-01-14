import javax.swing.*;
import java.awt.*;

public class Pacman extends JFrame {
   private DisplayPanel display;
   private InfoPanel info;
   public Pacman() { 
      super("PacMan");
      setLayout(new BorderLayout());
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setResizable(false);
      
      info = new InfoPanel();
      display = new DisplayPanel(info);
      
      add(info, BorderLayout.SOUTH);
      add(display, BorderLayout.CENTER);
      
      pack();
      
      // Centers the window and sets it to be visible
      setLocationRelativeTo(null);
      setVisible(true);
   }
   
   public static void main(String[] args) {
      Pacman pac = new Pacman();
      // Human Record = 4441
      // AI Record = 4851 (15)
      // Improve distance metric
      // Better teleport evaluation
      // Add reward for eating pellets at an earlier tick (reward for speed)
      // Pink ghost should always use max 
   }
}      