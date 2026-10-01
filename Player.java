import java.util.ArrayList;
public class Player {
    public static final int PLAY = 0; // still alive
    public static final int WAIT = 1; // pressed STAY
    public static final int FROZEN = 2; // the card froze them
    public static final int WIN = 100; // round win condition
    public static final int LOSE = 115; // el variant ending(lichess ref)
    private ArrayList<Card> currentList; // what they have in their hand
    private int status;
    private String name;
    private int totalScore; //whole game total
    private int lastRoundScore; // the score fro, the most recent round
    public Player(String name) {
        this.name = name;
        currentList = new ArrayList<Card>();
        status = PLAY;
        totalScore = 0;
        lastRoundScore = 0;
    }
    public String getName() {
        return name;
    }
    public ArrayList<Card> getCurrentList() {
        return currentList;
    }
    public void addCard(Card c) {
        currentList.add(c);
    }
    //true if pl;ayer already has one, for duplicate checking
    public boolean hasCard(Card c) {
        if (!c.isNumberCard()) { //cant be a dupe if it not a number right
            return false;
        }
        for (int k = 0; k < currentList.size(); k++) {
            Card existing = currentList.get(k);
            if (existing.isNumberCard() && existing.getValue() == c.getValue()) {
                return true;
            }
        }
        return false;
    }
    //self-explanatory
    private int indexOfType(String type) {
        for (int k = 0; k < currentList.size(); k++) {
            if (type.equals(currentList.get(k).getType())) {
                return k;
            }
        }
        return -1;
    }
    //used for reviving bro
    public boolean hasSecondChance() {
        return indexOfType("SecondChance") != -1;
    }
    //if they get revived take their stuff away
    public void removeSecondChance() {
        int index = indexOfType("SecondChance");
        if (index != -1) {
            currentList.remove(index);
        }
    }
    //like the name implies
    public int getUniqueNumberCount() {
        int count = 0;
        for (int k = 0; k < currentList.size(); k++) {
            if (currentList.get(k).isNumberCard()) {
                count++;
            }
        }
        return count;
    }
    //read the rule book like idk how u passed the first quiz if u didnt
    public int getHandScore() {
        int numSum = 0;
        int plusSum = 0;
        boolean timesTwo = false;
        for (int k = 0; k < currentList.size(); k++) {
            Card c = currentList.get(k);
            if (c.isNumberCard()) {
                numSum += c.getValue();
            } else if ("Plus".equals(c.getType())) {
                plusSum += c.getValue();
            } else if ("TimesTwo".equals(c.getType())) {
                timesTwo = true;
            }
        }
        int score = numSum + plusSum;
        if (timesTwo) {
            score *= 2;
        }
        return score;
    }
    public int getScore() {
        return totalScore;
    }
    public void setScore(int score) {
        totalScore = score;
    }
    public int getLastRoundScore() {
        return lastRoundScore;
    }
    public void setLastRoundScore(int score) {
        lastRoundScore = score;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    //once they have a abnormal status they are donzo and arent in the hit rotation and aren't eligible for action cards
    public boolean isDone() {
        return status != PLAY;
    }
}