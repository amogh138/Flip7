import java.awt.image.BufferedImage;
public class Card {
    private BufferedImage image;
    private String type;
    private int value;
    public Card(BufferedImage image, String type, int value) {
        this.image = image;
        this.type = type;
        this.value = value;
    }
    public BufferedImage getImage() {
        return image;
    }
    public String getType() {
        return type;
    }
    public int getValue() {
        return value;
    }
    public boolean isNumberCard() {
        return "Num".equals(type);
    }
}