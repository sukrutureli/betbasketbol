package com.basketbol;

import java.util.ArrayList;
import java.util.List;

import com.basketbol.model.LastPrediction;
import com.basketbol.model.MatchInfo;
import com.basketbol.model.PredictionResult;
import com.basketbol.model.TeamMatchHistory;

public class LastPredictionManager {
	private List<LastPrediction> lastPrediction;
	private MatchHistoryManager historyManager;
	private List<PredictionResult> predictionResults;
	private List<MatchInfo> matchInfo;

	public LastPredictionManager(MatchHistoryManager historyManager, List<PredictionResult> predictionResults,
			List<MatchInfo> matchInfo) {
		this.lastPrediction = new ArrayList<LastPrediction>();
		this.historyManager = historyManager;
		this.predictionResults = predictionResults;
		this.matchInfo = matchInfo;
	}

	public void fillPredictions() {
		for (int i = 0; i < historyManager.getTeamHistories().size(); i++) {
			TeamMatchHistory th = historyManager.getTeamHistories().get(i);
			
			LastPrediction tempLastPrediction = new LastPrediction(matchInfo.get(i).getName(), matchInfo.get(i).getTime());
			String[] tahminListesi = { "MS1", "MS2", "Üst", "Alt" };
			
			tempLastPrediction.setScore(predictionResults.get(i).getScoreline());

			for (String s : tahminListesi) {
				if (calculatePrediction(th, predictionResults.get(i),
						matchInfo.get(i), s) != null) {
					String withOdd = s + " (" + getOdds(s, matchInfo.get(i))  + ")";
					tempLastPrediction.getPredictions().add(withOdd);
				}
			}

			if (!tempLastPrediction.getPredictions().isEmpty()) {
				lastPrediction.add(tempLastPrediction);
			}
		}
	}

	private String calculatePrediction(TeamMatchHistory h, PredictionResult pr, MatchInfo matchInfo, String tahmin) {
		double percentageH = 0;
		double percentagePR = 0;

		if (!h.isInfoEnough()) {
			return null;
		}
		if ((pr.getConfidence() * 100) < 70) {
			return null;
		}
		
		if (tahmin.equals("MS1")) {
			percentageH = h.getMs1() * 100;
			percentagePR = pr.getpHome() * 100;
			if (matchInfo.getOdds().getMs1() > 0.0 && percentageH > 60 && percentagePR > 60
					&& isScoreOk(pr.getScoreline(), "MS1", matchInfo)) {
				return "MS1";
			}
		} else if (tahmin.equals("MS2")) {
			percentageH = h.getMs2() * 100;
			percentagePR = pr.getpAway() * 100;
			if (matchInfo.getOdds().getMs2() > 0.0 && percentageH > 60 && percentagePR > 60
					&& isScoreOk(pr.getScoreline(), "MS2", matchInfo)) {
				return "MS2";
			}
		} else if (tahmin.equals("Üst")) {
			percentageH = h.getUst() * 100;
			percentagePR = pr.getpOver25() * 100;
			if (matchInfo.getOdds().getOver() > 0.0) {
				if (percentageH > 60 && percentagePR > 60 && isScoreOk(pr.getScoreline(), "Üst", matchInfo)) {
					return "Üst";
				}
				if (percentagePR > 70 && isScoreOk(pr.getScoreline(), "Üst", matchInfo)) {
					return "Üst";
				}
			}
		} else if (tahmin.equals("Alt")) {
			percentageH = h.getAlt() * 100;
			percentagePR = (1 - pr.getpOver25()) * 100;
			if (matchInfo.getOdds().getUnder() > 0.0) {
				if (percentageH > 60 && percentagePR > 60 && isScoreOk(pr.getScoreline(), "Alt", matchInfo)) {
					return "Alt";
				}
				if (percentagePR > 70 && isScoreOk(pr.getScoreline(), "Alt", matchInfo)) {
					return "Alt";
				}
			}
		}

		return null;
	}

	private boolean isScoreOk(String score, String tahmin, MatchInfo match) {
		String[] splitScore = score.split("-");
		int home = Integer.valueOf(splitScore[0].trim());
		int away = Integer.valueOf(splitScore[1].trim());

		if (tahmin.equals("MS1") && home > away) {
			return true;
		} else if (tahmin.equals("MS2") && home < away) {
			return true;
		} else if (tahmin.equals("Alt") && (home + away) < match.getOdds().gethOverUnderValue()) {
			return true;
		} else if (tahmin.equals("Üst") && (home + away) > match.getOdds().gethOverUnderValue()) {
			return true;
		} else {
			return false;
		}
	}
	
	private String getOdds(String tahmin, MatchInfo match) {
		if (tahmin.equals("MS1")) {
			return String.valueOf(match.getOdds().getMs1());
		} else if (tahmin.equals("MS2")) {
			return String.valueOf(match.getOdds().getMs2());
		} else if (tahmin.equals("Alt")) {
			return String.valueOf(match.getOdds().getUnder());
		} else if (tahmin.equals("Üst")) {
			return String.valueOf(match.getOdds().getOver());
		}
		return null;
	}

	public List<LastPrediction> getLastPrediction() {
		return lastPrediction;
	}

}

