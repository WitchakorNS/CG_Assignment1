package CG_Assignment1.Merge;

import CG_Assignment1.GameEnter_Scene;
import CG_Assignment1.Game_Scene;
import CG_Assignment1.AdultChild_Scene;
import CG_Assignment1.MyMemoryAnimation;

import javax.swing.*;
import java.awt.*;

/**
 * mergeAll — Unified 600x600 px Player for all scenes:
 * Scene 1 (GameEnter_Scene) -> Scene 2 (Game_Scene) -> Scene 3 (AdultChild_Scene)
 * and the integrated MyMemoryAnimation.
 */
public class mergeAll extends JPanel {
    private static final int W = 600, H = 600;
    private final CardLayout cardLayout;
    private final JPanel container;

    public mergeAll() {
        setPreferredSize(new Dimension(W, H));
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);

        // Add full integrated animation and all 3 individual scenes
        container.add(new MyMemoryAnimation(), "FULL_ANIMATION");
        container.add(new GameEnter_Scene(W, H), "SCENE_1");
        container.add(new Game_Scene(W, H), "SCENE_2");
        container.add(new AdultChild_Scene(W, H), "SCENE_3");

        add(container, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Angry Birds - Memory Animation (600x600)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new mergeAll());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}
