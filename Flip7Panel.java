import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.swing.*;
public class Flip7Panel extends JPanel implements MouseListener {
    //tracking game status
    private static final int NormalState = 0;
    private static final int FreezeState = 1;
    private static final int GambleState = 2;
    private static final int TARGET_SCORE = 200;
    private BufferedImage back, blank; // card-back image and a blank placeholder image for a semi-slot system(used in drawingFlip7 fully)
    private Card empty; //made this for testing
    private ArrayList<Card> deck;           
    private ArrayList<Card> roundDiscardPile = new ArrayList<>(); // what the bottom line said
    private ArrayList<Card> mainDiscardPile = new ArrayList<Card>(); // read the game rules stupid
    private ArrayList<int[]> roundHistory = new ArrayList<>(); //self-explanatory
    private ArrayList<Player> players;
    private int currentPlayerIndex = 0; // player turn
    private int dealerIndex = 0; // dealer for said round
    private int roundCount = 1;
    private int gameState = NormalState;
    private boolean roundOver = false;
    private boolean matchOver = false;
    private Player matchWinner = null;
    //Cheecks to see if someone has a pendinga ction card to use so eithe rflip three or frozen
    private Player pendingActingPlayer;
    private Card pendingActionCard;
    private boolean gameStarted = false;
    //used for deal (5 clicks = noi more dealing)
    private int openingDealCount = 0;
    //builds and shuffules the deck, creates 5 players, and starts el gamo
    public Flip7Panel() {
        try {
            buildDeck();
        } catch (Exception e) {
            System.out.println("Check Spelling");
            return;
        }
        Collections.shuffle(deck);
        players = new ArrayList<Player>();
        for (int p = 1; p <= 5; p++) {
            players.add(new Player("Player " + p));
        }
        dealerIndex = 0;
        currentPlayerIndex = dealerIndex;
        addMouseListener(this);
        startRoundSequence(true);
    }
    //disallows clicking in various places based on states and booleans
    public void mouseClicked(MouseEvent m) {
        int x = m.getX();
        int y = m.getY();
        // if teh entire thing ended, it only listens for a new mtach input sicne ion want tampering
        if (matchOver) {
            int[] deal = getNewMatchButtonBounds();
            if (contains(x, y, deal[0], deal[1], deal[2], deal[3])) {
                resetMatch();
                repaint();
            }
            return;
        }
        //round just ended so only next round is clickable. again no tampering
        if (roundOver) {
            int boardH = 100 + players.size() * 28 + roundHistory.size() * 20;
            int boardY = getHeight() / 2 - boardH / 2;
            int nextW = 140;
            int nextH = 35;
            int nextX = getWidth() / 2 - nextW / 2;
            int nextY = boardY + boardH + 10;
            if (contains(x, y, nextX, nextY, nextW, nextH)) {
                startNextRound();
            }
            return;
        }
        // Waiting for a resolve, more on that under
        if (gameState == FreezeState || gameState == GambleState) {
            for (int seat = 0; seat < players.size(); seat++) {
                int pIndex = (dealerIndex + seat) % players.size();
                int[] bounds = getPlayerSlotBounds(seat);
                if (bounds != null && contains(x, y, bounds[0], bounds[1], bounds[2], bounds[3])) {
                    if (gameState == FreezeState) {
                        resolveFreezeTarget(players.get(pIndex));
                    } else {
                        resolveFlip3Target(players.get(pIndex));
                    }
                    return;
                }
            }
            return;
        }
        //as per dictator of stroudonia's prefs i made a dealer thing that appears for every round of a round of a match
        if (!gameStarted) {
            int[] deal = getHitButtonBounds();
            if (contains(x, y, deal[0], deal[1], deal[2], deal[3])) {
                gameStarted = true;
                hitCurrentPlayer();
            }
            return;
        }
        //clicking hit = get card, stay = stop playing
        int cardH = getHeight() / 7;
        int cardW = cardH * 67 / 99;
        int deckX = getWidth() / 2 - cardW / 2;
        int deckY = 45;
        int[] hitB = getHitButtonBounds();
        int[] stayB = getStayButtonBounds();
        if (contains(x, y, deckX, deckY, cardW, cardH) || contains(x, y, hitB[0], hitB[1], hitB[2], hitB[3])) {
            hitCurrentPlayer();
        } else if (contains(x, y, stayB[0], stayB[1], stayB[2], stayB[3])) {
            stayCurrentPlayer();
        }
    }
    public void mousePressed(MouseEvent m) {}
    public void mouseReleased(MouseEvent m) {}
    public void mouseEntered(MouseEvent m) {}
    public void mouseExited(MouseEvent m) {}
    // makes a new round
    private void startRoundSequence(boolean isFirstRound) {
        if (!isFirstRound) {
            dealerIndex = (dealerIndex + 1) % players.size();
        }
        currentPlayerIndex = (dealerIndex + 1) % players.size(); //left of dealer starts always
        gameState = NormalState;
        roundOver = false;
        for (Player p : players) {//this now clears the hands
            p.getCurrentList().clear();
            p.setStatus(Player.PLAY);
        }
        gameStarted = false;
        openingDealCount = 0;
        repaint();
    }
    //one card if u press hit
    private void hitCurrentPlayer() {
        if (!gameStarted || roundOver || gameState != NormalState) return;
        Player p = players.get(currentPlayerIndex);
        if (p.getStatus() != Player.PLAY) {
            return;
        }
        Card card = drawCard();
        if (card == null) {
            finishRound();
            repaint();
            return;
        }
        // allows me to switch teh button to hit instead after 5 clicks every round (yes i hard coded 5 ppl cuz im a bum)
        if (openingDealCount < 5) {
            openingDealCount++;
        }
        boolean busted = processDraw(p, card);
        if (p.getStatus() == Player.WIN) {
            finishRound();
            repaint();
            return;
        }
        if (!busted) {
            if ("Freeze".equals(card.getType())) {
                pendingActingPlayer = p;
                pendingActionCard = card;
                gameState = FreezeState;
                repaint();
                return;
            }
            if ("FlipThree".equals(card.getType())) {
                pendingActingPlayer = p;
                pendingActionCard = card;
                gameState = GambleState;
                repaint();
                return;
            }
        }
        // cant have inf hits(prior issue)
        advanceTurn();
        repaint();
    }
    //idk bro just stay
    private void stayCurrentPlayer() {
        if (!gameStarted || roundOver || gameState != NormalState) return;
        Player p = players.get(currentPlayerIndex);
        if (p.getStatus() == Player.PLAY) {
            p.setStatus(Player.WAIT);
        }
        if (allPlayersDone()) {
            finishRound();
        } else {
            advanceTurn();
        }
        repaint();
    }
    //again, prior issue
    private void advanceTurn() {
        if (allPlayersDone()) {
            finishRound();
            return;
        }
        for (int step = 1; step <= players.size(); step++) {
            int index = (currentPlayerIndex + step) % players.size();
            if (players.get(index).getStatus() == Player.PLAY) {
                currentPlayerIndex = index;
                return;
            }
        }
        finishRound();
    }
    //checks evry eprsons status to make sure it doesnt rob u of a turn
    private boolean allPlayersDone() {
        for (Player p : players) {
            if (p.getStatus() == Player.PLAY) {
                return false;
            }
        }
        return true;
    }
    //resolving action cards = cant do jack till u give them attention
    private void resolveFreezeTarget(Player target) {
        if (target.getStatus() != Player.PLAY) return;
        target.setStatus(Player.FROZEN);
        finishTargetResolution();
    }
    private void resolveFlip3Target(Player target) {
        if (target.getStatus() != Player.PLAY) return;
        boolean gotAnotherOne = false;
        for (int n = 0; n < 3 && target.getStatus() == Player.PLAY; n++) {
            Card c = drawCard();
            if (c == null) break;
            processDraw(target, c);
            if ("Freeze".equals(c.getType())) {
                target.setStatus(Player.FROZEN);
            }
            if ("FlipThree".equals(c.getType())) {
                gotAnotherOne = true;
            }
            if (target.getStatus() == Player.WIN) {
                cleanupPendingCard();
                finishRound();
                repaint();
                return;
            }
        }
        if (gotAnotherOne && target.getStatus() == Player.PLAY) {
            gameState = GambleState;
            resolveFlip3Target(target);
            return;
        }
        finishTargetResolution();
    }
    //makes sure it doesnt look like u keep ur freeze/flip 7
    private void cleanupPendingCard() {
        if (pendingActionCard != null) {
            roundDiscardPile.add(pendingActionCard);
            if (pendingActingPlayer != null) {
                pendingActingPlayer.getCurrentList().remove(pendingActionCard);
            }
        }
        gameState = NormalState;
        pendingActionCard = null;
        pendingActingPlayer = null;
    }
    // remove the barriers disallowing u to excersize free will
    private void finishTargetResolution() {
        cleanupPendingCard();
        if (allPlayersDone()) {
            finishRound();
        } else {
            advanceTurn();
        }
        repaint();
    }
    //just math vro
    private int calculateRoundScore(Player p) {
        if (p.getStatus() == Player.LOSE) return 0;
        int numSum = 0;
        int plusSum = 0;
        boolean timesTwo = false;
        for (Card c : p.getCurrentList()) {
            if (c.isNumberCard()) {
                numSum += c.getValue();
            } else if ("Plus".equals(c.getType())) {
                plusSum += c.getValue();
            } else if ("TimesTwo".equals(c.getType())) {
                timesTwo = true;
            }
        }
        int score = numSum;
        if (timesTwo) score *= 2;
        if (p.getStatus() == Player.WIN || p.getUniqueNumberCount() >= 7) {
            score += 15;
        }
        score+=plusSum;
        return score;
    }
    /*this is teh cleanup and in a sense wipes the entire board for everyhging 
    except what is necessary, but not hands cuz i wnat u to be able to see them when you see score so u know what happened */
    private void finishRound() {
        if (roundOver) return;
        roundOver = true;
        int[] scoresThisRound = new int[players.size()];
        int highestScore = -1;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int rScore = calculateRoundScore(p);
            scoresThisRound[i] = rScore;
            p.setScore(p.getScore() + rScore);
            p.setLastRoundScore(rScore);
            if (p.getScore() > highestScore) {
                highestScore = p.getScore();
            }
            roundDiscardPile.addAll(p.getCurrentList());
        }
        roundHistory.add(scoresThisRound);
        if (highestScore >= TARGET_SCORE) {
            int leaderCount = 0;
            Player soleWinner = null;
            for (Player p : players) {
                if (p.getScore() == highestScore) {
                    leaderCount++;
                    soleWinner = p;
                }
            }
            if (leaderCount == 1) {
                matchOver = true;
                matchWinner = soleWinner;
            } else {
                // TS is never happenion sonion
                matchOver = false;
            }
        }
    }
    private void startNextRound() {
        if (matchOver) return;
        for(int i = 0; i<5; i++) {
            Player p = players.get(i);
            p.getCurrentList().clear();
        }
        mainDiscardPile.addAll(roundDiscardPile);
        roundDiscardPile.clear();
        roundCount++;
        startRoundSequence(false);
        repaint();
    }
    private void resetMatch() {
        try {
            deck.clear();
            buildDeck();
        } catch (Exception e) {
            return;
        }
        Collections.shuffle(deck);
        roundDiscardPile.clear();
        mainDiscardPile.clear();
        roundHistory.clear();
        for (Player p : players) {
            p.setScore(0);
            p.setLastRoundScore(0);
            p.getCurrentList().clear();
            p.setStatus(Player.PLAY);
        }
        dealerIndex = 0;
        roundCount = 1;
        matchOver = false;
        matchWinner = null;
        startRoundSequence(true);
    }
    //i wnated to be different so i js made this instead of try/catch sue me
    private void buildDeck() throws Exception {
        deck = new ArrayList<Card>();
        BufferedImage zeroImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Zero.png"));
        BufferedImage oneImg = ImageIO.read(Flip7Panel.class.getResource("/Image/One.png"));
        BufferedImage twoImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Two.png"));
        BufferedImage threeImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Three.png"));
        BufferedImage fourImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Four.png"));
        BufferedImage fiveImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Five.png"));
        BufferedImage sixImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Six.png"));
        BufferedImage sevenImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Seven.png"));
        BufferedImage eightImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Eight.png"));
        BufferedImage nineImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Nine.png"));
        BufferedImage tenImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Ten.png"));
        BufferedImage elevenImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Eleven.png"));
        BufferedImage twelveImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Twelve.png"));
        BufferedImage scImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Second_Chance.png"));
        BufferedImage freezeImg = ImageIO.read(Flip7Panel.class.getResource("/Image/Freeze.png"));
        BufferedImage f3Img = ImageIO.read(Flip7Panel.class.getResource("/Image/Flip_Three.png"));
        BufferedImage t2 = ImageIO.read(Flip7Panel.class.getResource("/Image/Times_Two.png"));
        BufferedImage p2 = ImageIO.read(Flip7Panel.class.getResource("/Image/Plus_Two.png"));
        BufferedImage p4 = ImageIO.read(Flip7Panel.class.getResource("/Image/Plus_Four.png"));
        BufferedImage p6 = ImageIO.read(Flip7Panel.class.getResource("/Image/Plus_Six.png"));
        BufferedImage p8 = ImageIO.read(Flip7Panel.class.getResource("/Image/Plus_Eight.png"));
        BufferedImage p10 = ImageIO.read(Flip7Panel.class.getResource("/Image/Plus_Ten.png"));
        back = ImageIO.read(Flip7Panel.class.getResource("/Image/Back.png"));
        blank = ImageIO.read(Flip7Panel.class.getResource("/Image/Blank.png"));
        empty = new Card(blank, "Blank", 0);
        //TS aint even common ball
        deck.add(new Card(zeroImg, "Num", 0));
        deck.add(new Card(oneImg, "Num", 1));
        for (int k = 0; k < 2; k++) deck.add(new Card(twoImg, "Num", 2));
        for (int k = 0; k < 3; k++) deck.add(new Card(threeImg, "Num", 3));
        for (int k = 0; k < 4; k++) deck.add(new Card(fourImg, "Num", 4));
        for (int k = 0; k < 5; k++) deck.add(new Card(fiveImg, "Num", 5));
        for (int k = 0; k < 6; k++) deck.add(new Card(sixImg, "Num", 6));
        for (int k = 0; k < 7; k++) deck.add(new Card(sevenImg, "Num", 7));
        for (int k = 0; k < 8; k++) deck.add(new Card(eightImg, "Num", 8));
        for (int k = 0; k < 9; k++) deck.add(new Card(nineImg, "Num", 9));
        for (int k = 0; k < 10; k++) deck.add(new Card(tenImg, "Num", 10));
        for (int k = 0; k < 11; k++) deck.add(new Card(elevenImg, "Num", 11));
        for (int k = 0; k < 12; k++) deck.add(new Card(twelveImg, "Num", 12));
        for (int k = 0; k < 3; k++) {
            deck.add(new Card(scImg, "SecondChance", 0));
            deck.add(new Card(freezeImg, "Freeze", 0));
            deck.add(new Card(f3Img, "FlipThree", 0));
        }
        deck.add(new Card(t2, "TimesTwo", 0));
        deck.add(new Card(p2, "Plus", 2));
        deck.add(new Card(p4, "Plus", 4));
        deck.add(new Card(p6, "Plus", 6));
        deck.add(new Card(p8, "Plus", 8));
        deck.add(new Card(p10, "Plus", 10));
    }
    //draws and reshuffles, if main deck is empty steals from discard not round discard
    private Card drawCard() {
        if (deck.isEmpty()) {
            if (!mainDiscardPile.isEmpty()) {
                deck.addAll(mainDiscardPile);
                mainDiscardPile.clear();
                Collections.shuffle(deck);
            } else {
                return null;
            }
        }
        return deck.remove(deck.size() - 1);
    }
    //this is basically revival code and normal stuff or processes (ts is cuz of abhay's presentation)
    private boolean processDraw(Player p, Card card) {
        if (card.isNumberCard()) {
            if (p.hasCard(card)) {
                if (p.hasSecondChance()) {
                    Card scCard = null;
                    for (Card c : p.getCurrentList()) {
                        if ("SecondChance".equals(c.getType())) {
                            scCard = c;
                            break;
                        }
                    }
                    if (scCard != null) {
                        p.getCurrentList().remove(scCard);
                        roundDiscardPile.add(scCard);
                    }
                    roundDiscardPile.add(card);
                    return false;
                }
                p.addCard(card);
                p.setStatus(Player.LOSE);
                return true;
            }
            p.addCard(card);
            if (p.getUniqueNumberCount() >= 7) {
                p.setStatus(Player.WIN);
                return true;
            }
            return false;
        }
        if ("SecondChance".equals(card.getType())) {
            if (p.hasSecondChance()) {
                roundDiscardPile.add(card);
                return false;
            }
            p.addCard(card);
            return false;
        }
        p.addCard(card);
        return false;
    }
    /* anything taht ends with "Bounds" is because I was having syncing issues with listeners
    and rects, i made these, not necessary if u are willing to scroll a lot */
    private int[] getPlayerSlotBounds(int seat) {
        int w = getWidth();
        int h = getHeight();
        int slotW = 390;
        int slotH = 230;
        //hardcoding player positions
        if (seat == 0) {
            return new int[]{w / 2 - slotW / 2, h - 230, slotW, slotH};
        } else if (seat == 1) {
            return new int[]{20, h - 230, slotW, slotH};
        } else if (seat == 2) {
            return new int[]{20, 230, slotW, slotH};
        } else if (seat == 3) {
            return new int[]{w - slotW - 20, 230, slotW, slotH};
        } else if (seat == 4) {
            return new int[]{w - slotW - 20, h - 230, slotW, slotH};
        } else {
            return null;
        }
    }
    private int[] getNewMatchButtonBounds() {
        int w = 120;
        int h = 46;
        int x = 25;
        int y = getHeight() / 2 - h / 2;
        return new int[]{x, y, w, h};
    }
    private int[] getHitButtonBounds() {
        int btnWidth = 130;
        int btnHeight = 42;
        int btnY = getHeight() / 2 - 42;
        int hitX = getWidth() / 2 - 145;
        return new int[]{hitX, btnY, btnWidth, btnHeight};
    }
    private int[] getStayButtonBounds() {
        int btnWidth = 130;
        int btnHeight = 42;
        int btnY = getHeight() / 2 - 42;
        int stayX = getWidth() / 2 + 15;
        return new int[]{stayX, btnY, btnWidth, btnHeight};
    }
    //ion wanna do allat if else jazz so i made a helper method
    private boolean contains(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    //text in the top left of each player seat
    private String statusText(Player p) {
        switch (p.getStatus()) {
            case Player.LOSE: return "Busted";
            case Player.WAIT: return "Stayed";
            case Player.FROZEN: return "Frozen";
            case Player.WIN: return "Flip 7!";
            default: return "Playing";
        }
    }
    //if non active player explain why
    private String statusSuffix(Player p) {
        switch (p.getStatus()) {
            case Player.LOSE: return " [BUSTED]";
            case Player.WAIT: return " [STAYED]";
            case Player.FROZEN: return " [FROZEN]";
            case Player.WIN: return " [FLIP 7!]";
            default: return "";
        }
    }
    //drawing everything
    public void paint(Graphics g) {
        super.paint(g);
        //casino green
        g.setColor(new Color(40, 90, 60));
        g.fillRect(0, 0, getWidth(), getHeight());
        //the top left text
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), 35);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        Player activeP = players.get(currentPlayerIndex);
        Player dealerP = players.get(dealerIndex);
        String topText = "Round " + roundCount + "  |  Active Turn: " + activeP.getName() + " (" + statusText(activeP) + ")  |  Dealer: " + dealerP.getName();
        g.drawString(topText, 20, 27);
        //card piles
        int cardH = getHeight() / 7;
        int cardW = cardH * 67 / 99; //this is js cuz it was my cropping, do whatever your heart desires
        int deckX = getWidth() / 2 - cardW / 2;
        int deckY = 55;
        int discardX = deckX - cardW - 70;
        int roundDiscardX = deckX + cardW + 70;
        drawDeckBox(g, discardX, deckY, cardW, cardH, !mainDiscardPile.isEmpty(),
                "Discard Pile (" + mainDiscardPile.size() + ")");
        drawDeckBox(g, deckX, deckY, cardW, cardH, !deck.isEmpty(),
                "Deck (" + deck.size() + ")");
        drawDeckBox(g, roundDiscardX, deckY, cardW, cardH, !roundDiscardPile.isEmpty(),
                "Round Discard Pile (" + roundDiscardPile.size() + ")");
        int thumbW = cardW / 2;
        int thumbH = cardH / 2;
        for (int seat = 0; seat < players.size(); seat++) {
            int pIndex = (dealerIndex + seat) % players.size();
            Player p = players.get(pIndex);
            int[] rect = getPlayerSlotBounds(seat);
            if (rect == null) continue;
            int rx = rect[0], ry = rect[1], rw = rect[2], rh = rect[3];
            boolean isCurrent = (pIndex == currentPlayerIndex) && !roundOver && gameState == NormalState;
            boolean isTargetable = (gameState == FreezeState || gameState == GambleState)
                    && p.getStatus() == Player.PLAY;
            // Casino Red
            g.setColor(new Color(210,41,40));
            g.fillRect(rx, ry, rw, rh);
            // Seat border: yellow if it's their turn, cyan if they're a valid target for actions, gray otherwise
            if (isCurrent) {
                g.setColor(Color.YELLOW);
                g.drawRect(rx, ry, rw, rh);
                g.drawRect(rx + 1, ry + 1, rw - 2, rh - 2);
                g.drawRect(rx + 2, ry + 2, rw - 4, rh - 4);
            } else if (isTargetable) {
                g.setColor(Color.CYAN);
                g.drawRect(rx, ry, rw, rh);
                g.drawRect(rx + 1, ry + 1, rw - 2, rh - 2);
                g.drawRect(rx + 2, ry + 2, rw - 4, rh - 4);
            } else {
                g.setColor(Color.GRAY);
                g.drawRect(rx, ry, rw, rh);
            }
            // All the seat stuff
            if (isCurrent) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.setFont(new Font("Arial", Font.BOLD, 15));
            String seatLabel;
            if (isCurrent) {
                seatLabel = " [ACTIVE]";
            } else {
                seatLabel = "";
            }
            String dealerBadge;
            if (pIndex == dealerIndex) {
                dealerBadge = " (Dealer)";
            } else {
                dealerBadge = "";
            }
            String statusBadge = statusSuffix(p);
            String header = p.getName() + dealerBadge + seatLabel + statusBadge;
            g.drawString(header, rx + 10, ry + 20);
            // the active score of hand and total duringa  round in the seat
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            g.drawString("Total: " + p.getScore() + "   Hand: " + p.getHandScore(), rx + 10, ry + 38);
            //resizing thumbnails so u actually know whos active player
            ArrayList<Card> hand = p.getCurrentList();
            int curW;
            int curH;
            if (isCurrent) {
                curW = thumbW;
                curH = thumbH;
            } else {
                curW = cardW / 3;
                curH = cardH / 3;
            }
            //rotated stay card code
            int slotW = curH;
            int slotH = curW;
            int slotX = rx + rw - slotW - 12;
            int slotY = ry + 45;
            g.setColor(new Color(150, 150, 150));
            g.drawRect(slotX, slotY, slotW, slotH);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString("STAY", slotX + 2, slotY - 3);
            boolean isStayed = (p.getStatus() == Player.WAIT || p.getStatus() == Player.FROZEN) && !hand.isEmpty();
            int normalCardsCount;
            if (isStayed) {//basically sicne i rotate it sideways take it out from normal hand viewing
                normalCardsCount = hand.size() - 1;
            } else {
                normalCardsCount = hand.size();
            }
            int cax = rx + 8;
            int cay = ry + 45;
            int caw = Math.max(curW, rw - slotW - 28);
            int cah = rh - 50;
            g.setClip(cax, cay, caw, cah);//reorder the box based on whats in it, for testing
            int perRow = 6;
            int cx = cax;
            int cy = cay;
            int col = 1;
            for (int c = 0; c < normalCardsCount; c++) {
                g.drawImage(hand.get(c).getImage(), cx, cy, curW, curH, null);
                col++;
                cx += curW + 3;
                if (col >= perRow) {
                    col = 0;
                    cx = cax;
                    cy += curH + 3;
                }
            }
            g.setClip(0, 0, getWidth(), getHeight());
            //actual rotate code in motion
            if (isStayed) {
                Card stayedCard = hand.get(hand.size() - 1);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(slotX + slotW / 2, slotY + slotH / 2);
                g2.rotate(Math.toRadians(90));
                g2.drawImage(stayedCard.getImage(), -curW / 2, -curH / 2, curW, curH, null);
                g2.dispose();
            }
            g.setColor(Color.DARK_GRAY);
            g.drawRect(rx, ry, rw, rh);
        }
        //just drawing buttons
        int[] hitB = getHitButtonBounds();
        int[] stayB = getStayButtonBounds();
        int hitX = hitB[0], btnY = hitB[1], btnWidth = hitB[2], btnHeight = hitB[3];
        int stayX = stayB[0];
        boolean canAct = !roundOver && gameState == NormalState && gameStarted;
        boolean showDealButton = openingDealCount < 5;
        if (showDealButton) {
            //cuz like idk no hitting first round
            g.setColor(new Color(50, 100, 180));
            g.fillRect(hitX, btnY, btnWidth, btnHeight);
            g.setColor(Color.BLACK);
            g.drawRect(hitX, btnY, btnWidth, btnHeight);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            g.drawString("DEAL", hitX + 40, btnY + 27);
        } else {
            if (canAct) {
                g.setColor(new Color(40, 160, 60));
            } else {
                g.setColor(new Color(150, 190, 155));
            }
            g.fillRect(hitX, btnY, btnWidth, btnHeight);
            g.setColor(Color.BLACK);
            g.drawRect(hitX, btnY, btnWidth, btnHeight);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            g.drawString("HIT", hitX + 48, btnY + 27);
        }
        if (canAct) {
            g.setColor(new Color(180, 50, 50));
        } else {
            g.setColor(new Color(205, 160, 160));
        }
        g.fillRect(stayX, btnY, btnWidth, btnHeight);
        g.setColor(Color.BLACK);
        g.drawRect(stayX, btnY, btnWidth, btnHeight);
        g.setColor(Color.WHITE);
        g.drawString("STAY", stayX + 40, btnY + 27);
        //top right words
        if (gameState == FreezeState) {
            drawStatusBox(g, "Freeze drawn - click a player's box to freeze them.");
        } else if (gameState == GambleState) {
            drawStatusBox(g, "Flip Three drawn - click a player's box to make them draw 3.");
        }
        if (matchOver) {
            drawnewMatchButton(g, "New Match");
        }
        if (roundOver) {
            drawScoreboard(g);
        }
    }
    //draws  apile or an empty outline
    private void drawDeckBox(Graphics g, int x, int y, int w, int h, boolean hasCards, String label) {
        if (hasCards) {
            g.drawImage(back, x, y, w, h, null);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);
        } else {
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(x, y, w, h);
        }
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.drawString(label, x, y - 8);
    }
    // makes the new match button
    private void drawnewMatchButton(Graphics g, String label) {
        int[] b = getNewMatchButtonBounds();
        int bx = b[0], by = b[1], bw = b[2], bh = b[3];
        g.setColor(new Color(50, 100, 180));
        g.fillRect(bx, by, bw, bh);
        g.setColor(Color.BLACK);
        g.drawRect(bx, by, bw, bh);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        int textW = g.getFontMetrics().stringWidth(label);
        g.drawString(label, bx + (bw - textW) / 2, by + 28);
    }
    //actually draw top left words
    private void drawStatusBox(Graphics g, String text) {
        int w = 340;
        int h = 28;
        int x = getWidth() - w - 20;
        int y = 4;
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(text, x + 8, y + 19);
    }
    //actually draws the scoreboard based on other data
    private void drawScoreboard(Graphics g) {
        int boardW = 480;
        int boardH = 100 + players.size() * 28 + roundHistory.size() * 20;
        int boardX = getWidth() / 2 - boardW / 2;
        int boardY = getHeight() / 2 - boardH / 2;
        g.setColor(new Color(10, 10, 10, 235));
        g.fillRect(boardX, boardY, boardW, boardH);
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        if (matchOver) {
            g.drawString("MATCH OVER - WINNER!", boardX + 20, boardY + 30);
            g.setColor(Color.CYAN);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Winner: " + matchWinner.getName(), boardX + 20, boardY + 52);
        } else {
            g.drawString("ROUND OVER - STANDINGS", boardX + 20, boardY + 35);
        }
        ArrayList<Player> ranked = new ArrayList<Player>(players);
        ranked.sort((a, b) -> b.getScore() - a.getScore()); //basic comparable, again skimping out on code, also, vs code told me to
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        int y;
        if (matchOver) {
            y = boardY + 75;
        } else {
            y = boardY + 65;
        }
        for (int k = 0; k < ranked.size(); k++) {
            Player p = ranked.get(k);
            if (k == 0) {
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString((k + 1) + ". " + p.getName() + " - Total Score: " + p.getScore()
                    + " (+ " + p.getLastRoundScore() + " this rd)", boardX + 20, y);
            y += 26;
        }
        //save ur scores for each round
        y += 16;
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString("Score History", boardX + 20, y);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        for (int r = 0; r < roundHistory.size(); r++) {
            y += 20;
            int[] scores = roundHistory.get(r);
            String line = "Round " + (r + 1) + ":  ";
            for (int i = 0; i < players.size(); i++) {
                line += players.get(i).getName() + " " + scores[i];
                if (i != players.size() - 1) line += "   ";
            }
            g.drawString(line, boardX + 20, y);
        }
        //draws the enxt round button omg i had so many problems with ts
        if (matchOver) return;
        int nextW = 140;
        int nextH = 35;
        int nextX = getWidth() / 2 - nextW / 2;
        int nextY = boardY + boardH + 10;
        g.setColor(new Color(50, 120, 200));
        g.fillRect(nextX, nextY, nextW, nextH);
        g.setColor(Color.BLACK);
        g.drawRect(nextX, nextY, nextW, nextH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Next Round", nextX + 24, nextY + 23);
    }
}