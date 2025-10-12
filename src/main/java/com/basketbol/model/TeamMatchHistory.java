package com.basketbol.model;

import java.util.ArrayList;
import java.util.List;

public class TeamMatchHistory {
	private String teamName;
	private String teamEv;
	private String teamDep;
	private String detailUrl;
	private Odds odds;
	private List<MatchResult> rekabetGecmisi;
	private List<MatchResult> sonMaclarHome;
	private List<MatchResult> sonMaclarAway;

	public TeamMatchHistory(String teamName, String teamEv, String teamDep, String detailUrl, Odds odds) {
		this.teamName = teamName;
		this.teamEv = teamEv;
		this.teamDep = teamDep;
		this.setDetailUrl(detailUrl);
		this.rekabetGecmisi = new ArrayList<>();
		this.sonMaclarHome = new ArrayList<>();
		this.sonMaclarAway = new ArrayList<>();
		this.odds = odds;
	}

	// Add methods
	public void addRekabetGecmisiMatch(MatchResult match) {
		rekabetGecmisi.add(match);
	}

	public void addSonMacMatch(MatchResult match, int homeOrAway) {
		if (homeOrAway == 1) {
			sonMaclarHome.add(match);
		} else if (homeOrAway == 2) {
			sonMaclarAway.add(match);
		}
	}

	// Utility methods
	public int getTotalMatches() {
		return rekabetGecmisi.size() + sonMaclarHome.size() + sonMaclarAway.size();
	}

	public List<MatchResult> getAllMatches() {
		List<MatchResult> allMatches = new ArrayList<>();
		allMatches.addAll(rekabetGecmisi);
		allMatches.addAll(sonMaclarHome);
		allMatches.addAll(sonMaclarAway);
		return allMatches;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public String getTeamEv() {
		return teamEv;
	}

	public void setTeamEv(String teamEv) {
		this.teamEv = teamEv;
	}

	public String getTeamDep() {
		return teamDep;
	}

	public void setTeamDep(String teamDep) {
		this.teamDep = teamDep;
	}

	public List<MatchResult> getRekabetGecmisi() {
		return rekabetGecmisi;
	}

	public void setRekabetGecmisi(List<MatchResult> rekabetGecmisi) {
		this.rekabetGecmisi = rekabetGecmisi;
	}

	public List<MatchResult> getSonMaclarHome() {
		return sonMaclarHome;
	}

	public void setSonMaclarHome(List<MatchResult> sonMaclarHome) {
		this.sonMaclarHome = sonMaclarHome;
	}

	public List<MatchResult> getSonMaclarAway() {
		return sonMaclarAway;
	}

	public void setSonMaclarAway(List<MatchResult> sonMaclarAway) {
		this.sonMaclarAway = sonMaclarAway;
	}

	public String getDetailUrl() {
		return detailUrl;
	}

	public void setDetailUrl(String detailUrl) {
		this.detailUrl = detailUrl;
	}

	public int getUst() {
		int ust = 0;

		for (int i = 0; i < rekabetGecmisi.size(); i++) {
			if ((rekabetGecmisi.get(i).getHomeScore() + rekabetGecmisi.get(i).getAwayScore()) > odds
					.gethOverUnderValue()) {
				ust++;
			}
		}

		for (int i = 0; i < sonMaclarHome.size(); i++) {
			if ((sonMaclarHome.get(i).getHomeScore() + sonMaclarHome.get(i).getAwayScore()) > odds
					.gethOverUnderValue()) {
				ust++;
			}
		}

		for (int i = 0; i < sonMaclarAway.size(); i++) {
			if ((sonMaclarAway.get(i).getHomeScore() + sonMaclarAway.get(i).getAwayScore()) > odds
					.gethOverUnderValue()) {
				ust++;
			}
		}
		return ust;
	}

	public int getAlt() {
		return getTotalMatches() - getUst();
	}

	public String toStringAsPercentage(int value, String type) {
		return type + " : %" + ((int) (((value * 1.0) / getTotalMatches()) * 100));
	}

	public Match createStats(MatchInfo pMatch) {
		Match currentMatch = new Match(teamEv, teamDep);
		currentMatch.setOdds(pMatch.getOdds());

		BasketballStats homeStats = new BasketballStats(getForAgainstAndTotal(sonMaclarHome, teamEv)[0],
				getForAgainstAndTotal(sonMaclarHome, teamEv)[1], getForAgainstAndTotal(sonMaclarHome, teamEv)[2]);

		BasketballStats awayStats = new BasketballStats(getForAgainstAndTotal(sonMaclarAway, teamDep)[0],
				getForAgainstAndTotal(sonMaclarAway, teamDep)[1], getForAgainstAndTotal(sonMaclarAway, teamDep)[2]);

		currentMatch.setHomeStats(homeStats);
		currentMatch.setAwayStats(awayStats);

		currentMatch.setAvgPointsForHome(getForAgainstAndTotal(rekabetGecmisi, teamEv)[0]);
		currentMatch.setAvgPointsForAway(getForAgainstAndTotal(rekabetGecmisi, teamEv)[1]);
		currentMatch.setH2hAvgTotalPoints(getForAgainstAndTotal(rekabetGecmisi, teamEv)[2]);

		return currentMatch;
	}

	public double[] getForAgainstAndTotal(List<MatchResult> macResult, String teamName) {
		double[] points = { 0.0, 0.0, 0.0 };

		int size = macResult.size();

		System.out.println(size);

		for (int i = 0; i < size; i++) {
			System.out.println(macResult.get(i).getHomeTeam());
			System.out.println(macResult.get(i).getAwayTeam());
			System.out.println(teamName);
			if (macResult.get(i).getHomeTeam().contains(teamName)) {
				points[0] += macResult.get(i).getHomeScore();
				points[1] += macResult.get(i).getAwayScore();
			} else if (macResult.get(i).getAwayTeam().contains(teamName)) {
				points[1] += macResult.get(i).getHomeScore();
				points[0] += macResult.get(i).getAwayScore();
			}
			points[2] = points[2] + macResult.get(i).getHomeScore() + macResult.get(i).getAwayScore();
		}
		

		if (size > 0) {
			points[0] /= size;
			points[1] /= size;
			points[2] /= size;
		}

		return points;
	}

	public String getStyle(int value, String type, String pick) {
		String colorH = "background-color: #deded1;";
		String color = "background-color: #c8facc;";

		if (value == -1) {
			return colorH;
		} else {
			if (pick.contains(type)) {
				return color;
			}
		}

		return "";
	}
}
