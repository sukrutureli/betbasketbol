package com.basketbol.algorithm;

import java.util.*;
import com.basketbol.model.Match;
import com.basketbol.model.Odds;
import com.basketbol.model.PredictionResult;

public class EnsembleModel implements BettingAlgorithm {

    private final List<BettingAlgorithm> models;

    public EnsembleModel(List<BettingAlgorithm> models) {
        this.models = models;
    }

    @Override
    public String name() { return "BasketballEnsembleModel"; }

    @Override
    public double weight() { return 1.0; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> odds) {
        double pHome = 0, pAway = 0, pOver = 0;
        double totW = 0;

        //String bestPick = "MS1";
        String bestScore = "";
        double bestWeight = -1;

        // Her modelin katkısını ağırlığına göre ortalama al
        for (BettingAlgorithm model : models) {
            PredictionResult r = model.predict(match, odds);
            double w = model.weight();

            pHome += w * r.getpHome();
            pAway += w * r.getpAway();
            pOver += w * r.getpOver25();

            if (w > bestWeight) {
                //bestPick = r.getPick();
                bestScore = r.getScoreline();
                bestWeight = w;
            }
            totW += w;
        }

        if (totW == 0) totW = 1;
        pHome /= totW;
        pAway /= totW;
        pOver /= totW;

        // normalize olasılıklar
        double sum = pHome + pAway;
        if (sum > 0) {
            pHome /= sum;
            pAway /= sum;
        }

        // Tahmin: yüksek olasılıklı sonucu belirle
        String msPick = (pHome > pAway) ? "MS1" : "MS2";

        // Confidence = fark oranı
        double confidence = Math.abs(pHome - pAway);

        // Alt/Üst tahmini (örnek: pOver > 0.5)
        String ouPick = (pOver > 0.5) ? "ÜST" : "ALT";

        return new PredictionResult(
            name(),
            match.getHomeTeam(),
            match.getAwayTeam(),
            pHome, 0.0, pAway,   // pDraw yok
            pOver, 0.0,          // BTTS yok
            msPick + " | " + ouPick,
            confidence,
            bestScore
        );
    }
}
