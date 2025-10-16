package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

public class FormMomentumModel implements BettingAlgorithm {
	@Override
	public String name() {
		return "FormMomentumModel";
	}

	@Override
	public double weight() {
		return 0.5;
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

		double formMomentum = h.getPpg() - a.getPpg();
		double diff = (h.getAvgPointsFor() - a.getAvgPointsFor()) + formMomentum * 2.0;
		double total = 0.5 * (h.getAvgPointsFor() + a.getAvgPointsFor())
				+ 0.5 * (h.getAvgPointsAgainst() + a.getAvgPointsAgainst());

		String pick = diff > 3 ? "MS1" : (diff < -3 ? "MS2" : "Yakın");
		String ouPick = total > barem ? "Üst" : "Alt";
		if (barem < 0) {
			ouPick = "-";
		}

		double pHome = clamp(0.5 + diff / 20.0, 0, 1);
		double pOver = clamp(0.5 + (total - 160) / 40.0, 0, 1);
		if (barem < 0) {
			pOver = 0.5;
		}

		double conf = Math.min(1.0, 0.5 + Math.abs(diff) / 25.0);
		
		String finalPick = pick + " | " + ouPick;

		return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), pHome, 0, 1 - pHome, pOver, 0,
				finalPick, conf, "-");
	}

	private double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	private PredictionResult neutral(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.5, 0, 0.5, 0.5, 0, "-", 0.3, "-");
	}
}


