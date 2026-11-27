package be.kuleuven.gt.javabean;

public class ScoreComparison {
    private String gameName;
    private int myScore;
    private int friendScore;
    private String gameType; // 新增字段

    public ScoreComparison(String gameName, int myScore, int friendScore, String gameType) {
        this.gameName = gameName;
        this.myScore = myScore;
        this.friendScore = friendScore;
        this.gameType = gameType;
    }

    // getters
    public String getGameType() { return gameType; }
    public String getGameName() { return gameName; }
    public int getYourScore() { return myScore; }
    public int getFriendScore() { return friendScore; }
}
