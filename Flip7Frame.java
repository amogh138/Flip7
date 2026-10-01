import javax.swing.*;
public class Flip7Frame extends JFrame {
    public static final int WIDTH = 1600;
    public static final int HEIGHT = 960;
    public Flip7Frame(String framename) {
        super(framename);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        setSize(WIDTH, HEIGHT);
        add(new Flip7Panel());
        setVisible(true);
    }
}