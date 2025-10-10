package com.basketbol.model;

public class MatchInfo {
    private String homeTeam;
    private String awayTeam;
    private int homeScore;
    private int awayScore;
    private String prediction;

    public MatchInfo(String homeTeam, String awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    // Getter - Setter
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public String getPrediction() { return prediction; }

    public void setHomeScore(int score) { this.homeScore = score; }
    public void setAwayScore(int score) { this.awayScore = score; }
    public void setPrediction(String prediction) { this.prediction = prediction; }
}
