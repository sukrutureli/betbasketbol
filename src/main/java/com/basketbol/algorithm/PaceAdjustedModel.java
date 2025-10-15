package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

public class PaceAdjustedModel implements BettingAlgorithm {
	@Override
	public String name() {
		return "PaceAdjustedModel";
	}

	@Override
	public double weight() {
		return 0.2;
	}

	@Override
	public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
		BasketballStats h = match.getHomeStats();
		BasketballStats a = match.getAwayStats();
		if (h == null || a == null || h.isEmpty() || a.isEmpty())
			return neutral(match);

		double barem = -1.0;
		Odds o;
		if (!oddsOpt.isEmpty()) {
			o = oddsOpt.get();
			if (o.gethOverUnderValue() > 0)
				barem = o.gethOverUnderValue();
		}

		double paceFactor = (h.getAvgPossessions() + a.getAvgPossessions()) / 2.0;
		double effOff = (h.getAvgOffensiveRating() + a.getAvgOffensiveRating()) / 2.0;
		double effDef = (h.getAvgDefensiveRating() + a.getAvgDefensiveRating()) / 2.0;

		double expectedTotal = (paceFactor / 100.0) * ((effOff + (200 - effDef)) / 2.0);

		double pOver = sigmoid((expectedTotal - barem) / 10.0);
		if (barem < 0) {
			pOver = 0.5;
		}
		double pHome = sigmoid((h.getRating100() - a.getRating100()) / 30.0);

		String pick = pHome > 0.55 ? "MS1" : (pHome < 0.45 ? "MS2" : "Yakın");
		String ouPick = pOver > 0.55 ? "Üst" : (pOver < 0.45 ? "Alt" : "Sınırda");
		if (barem < 0) {
			ouPick = "-";
		}

		double conf = Math.max(pHome, Math.max(1 - pHome, Math.abs(pOver - 0.5))) * 0.8;

		String finalPick = pick + " | " + ouPick;

		return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), pHome, 0, 1 - pHome, pOver, 0,
				finalPick, conf, "-");
	}

	private double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private PredictionResult neutral(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.5, 0, 0.5, 0.5, 0, "-", 0.3, "-");
	}
}

