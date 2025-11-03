package com.basketbol;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.basketbol.model.LastPrediction;
import com.basketbol.model.MatchInfo;
import com.basketbol.model.PredictionResult;
import com.basketbol.model.TeamMatchHistory;
import com.basketbol.model.PredictionData;

public class LastPredictionManager {
	private List<LastPrediction> lastPrediction;
	private MatchHistoryManager historyManager;
	private List<PredictionResult> predictionResults;
	private List<MatchInfo> matchInfo;
	private List<PredictionData> predictionData;

	public LastPredictionManager(MatchHistoryManager historyManager, List<PredictionResult> predictionResults,
			List<MatchInfo> matchInfo) {
		this.lastPrediction = new ArrayList<LastPrediction>();
		this.historyManager = historyManager;
		this.predictionResults = predictionResults;
		this.matchInfo = matchInfo;
		this.predictionData = new ArrayList<PredictionData>();
	}

	public void fillPredictions() {
		for (int i = 0; i < historyManager.getTeamHistories().size(); i++) {
			TeamMatchHistory th = historyManager.getTeamHistories().get(i);

			LastPrediction tempLastPrediction = new LastPrediction(matchInfo.get(i).getName(),
					matchInfo.get(i).getTime());
			String[] tahminListesi = { "MS1", "MS2", "Üst", "Alt" };

			tempLastPrediction.setScore(predictionResults.get(i).getScoreline());

			for (String s : tahminListesi) {
				if (calculatePrediction(th, predictionResults.get(i), matchInfo.get(i), s) != null) {
					String t = s;
					if (!s.startsWith("MS")) {
						t = matchInfo.get(i).getOdds().gethOverUnderValue() + " " + s;
					}
					String withOdd = t + getOddsAndPercentage(s, matchInfo.get(i), predictionResults.get(i));
					tempLastPrediction.getPredictions().add(withOdd);
				}
			}

			if (!tempLastPrediction.getPredictions().isEmpty()) {
				lastPrediction.add(tempLastPrediction);

				String homeTeam = tempLastPrediction.getName().split("-")[0].trim();
				String awayTeam = tempLastPrediction.getName().split("-")[1].trim();
				PredictionData tempPredictionData = new PredictionData(homeTeam, awayTeam,
						tempLastPrediction.getPredictions());
				predictionData.add(tempPredictionData);
			}
		}
	}

	private String calculatePrediction(TeamMatchHistory h, PredictionResult pr, MatchInfo matchInfo, String tahmin) {
		double percentageH = 0;
		double percentagePR = 0;

		if (!h.isInfoEnough() && !h.isInfoEnoughWithoutRekabet()) {
			return null;
		}

		if (tahmin.equals("MS1")) {
			percentageH = h.getMs1() * 100;
			percentagePR = pr.getpHome() * 100;
			if (matchInfo.getOdds().getMs1() > 0.0 && isPercentageOK(percentageH, percentagePR)
					&& isScoreOk(pr.getScoreline(), "MS1", matchInfo)) {
				return "MS1";
			}
		} else if (tahmin.equals("MS2")) {
			percentageH = h.getMs2() * 100;
			percentagePR = pr.getpAway() * 100;
			if (matchInfo.getOdds().getMs2() > 0.0 && isPercentageOK(percentageH, percentagePR)
					&& isScoreOk(pr.getScoreline(), "MS2", matchInfo)) {
				return "MS2";
			}
		} else if (tahmin.equals("Üst")) {
			percentageH = h.getUst() * 100;
			percentagePR = pr.getpOver25() * 100;
			if (matchInfo.getOdds().getOver() > 0.0) {
				if (isPercentageOK(percentageH, percentagePR) && isScoreOk(pr.getScoreline(), "Üst", matchInfo)) {
					return "Üst";
				}
			}
		} else if (tahmin.equals("Alt")) {
			percentageH = h.getAlt() * 100;
			percentagePR = (1 - pr.getpOver25()) * 100;
			if (matchInfo.getOdds().getUnder() > 0.0) {
				if (isPercentageOK(percentageH, percentagePR) && isScoreOk(pr.getScoreline(), "Alt", matchInfo)) {
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

	private String getOddsAndPercentage(String tahmin, MatchInfo match, PredictionResult pr) {
		if (tahmin.equals("MS1")) {
			return " (" + String.valueOf(match.getOdds().getMs1()) + " - %" + ((int) (pr.getpHome() * 100)) + ")";
		} else if (tahmin.equals("MS2")) {
			return " (" + String.valueOf(match.getOdds().getMs2()) + " - %" + ((int) (pr.getpAway() * 100)) + ")";
		} else if (tahmin.equals("Alt")) {
			return " (" + String.valueOf(match.getOdds().getUnder()) + " - %" + ((int) ((1 - pr.getpOver25()) * 100))
					+ ")";
		} else if (tahmin.equals("Üst")) {
			return " (" + String.valueOf(match.getOdds().getOver()) + " - %" + ((int) (pr.getpOver25() * 100)) + ")";
		}
		return null;
	}

	public List<LastPrediction> getLastPrediction() {
		return lastPrediction;
	}

	public List<PredictionData> getPredictionData() {
		return predictionData;
	}

	public boolean isPercentageOK(double h, double pr) {
		boolean result = false;
		double hWd = 55;
		double hWe = 60;
		double prWd = 60;
		double prWe = 65;

		LocalDate now = LocalDate.now(ZoneId.of("Europe/Istanbul"));
		boolean isWeekEnd = now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY;

		if (isWeekEnd) {
			if (h >= hWe && pr >= prWe) {
				result = true;
			}
		} else {
			if (h >= hWd && pr >= prWd) {
				result = true;
			}
		}

		return result;
	}
}
