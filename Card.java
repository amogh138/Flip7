import java.awt.image.BufferedImage;
public class Card {
    private BufferedImage image; //the image u put in twin
    private String type; //possible type values: "Num", "SecondChance", "Freeze", "FlipThree", "Plus", "TimesTwo", or "Blank"
    private int value; //this is only for jumber card, putting 2 for a pkus two would lowk make it get added before thetimes 
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
    // easier check for looking at numbers since ion wanna do allat(yall get the ref?)
    public boolean isNumberCard() {
        return "Num".equals(type);
    }
}