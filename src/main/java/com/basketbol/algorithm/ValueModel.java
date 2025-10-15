package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

public class ValueModel implements BettingAlgorithm {
	@Override
	public String name() {
		return "ValueModel";
	}

	@Override
	public double weight() {
		return 0.15;
	}

	@Override
	public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
		if (oddsOpt.isEmpty())
			return neutral(match);
		Odds o = oddsOpt.get();

		double pHomeImp = 1.0 / o.getMs1();
		double pAwayImp = 1.0 / o.getMs2();
		double pHomeFair = 0.5 + (match.getHomeStats().getRating100() - match.getAwayStats().getRating100()) / 400.0;
		double diff = pHomeFair - pHomeImp;

		String pick = diff > 0.05 ? "Değerli MS1" : (diff < -0.05 ? "Değerli MS2" : "Piyasa Dengede");
		double conf = 0.5 + Math.abs(diff) * 2.0;

		return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), pHomeFair, 0, 1 - pHomeFair, 0.5,
				0, pick, conf, "-");
	}

	private PredictionResult neutral(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.5, 0, 0.5, 0.5, 0, "-", 0.3, "-");
	}
}
