package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

/**
 * Basketbol maçları için basit ama dengeli bir heuristic model.
 * Kullanılan veriler:
 *  - Ev sahibinin son maç ortalamaları
 *  - Deplasman takımının son maç ortalamaları
 *  - Aralarındaki maçların ortalamaları (h2h)
 *  - Barem (alt/üst referansı)
 */
public class HeuristicPredictor implements BettingAlgorithm {

    @Override
    public String name() { return "HeuristicPredictor"; }

    @Override
    public double weight() { return 1.0; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        BasketballStats h = match.getHomeStats();
        BasketballStats a = match.getAwayStats();

        // barem
        double barem = -1.0;
        if (match.getOdds() != null && match.getOdds().gethOverUnderValue() > 0)
            barem = match.getOdds().gethOverUnderValue();

        // head-to-head ortalaması
        double h2hAvgForHome = 0.0;
        double h2hAvgForAway = 0.0;
        double h2hTotal = 0.0;
        if (match.getH2hAvgTotalPoints() != null) {
            h2hAvgForHome = match.getAvgPointsForHome();
            h2hAvgForAway = match.getAvgPointsForAway();
            h2hTotal = match.getH2hAvgTotalPoints();
        }
        
        double forWeight = h2hTotal > 0 ? 0.55 : 0.6;
        double againstWeight = h2hTotal > 0 ? 0.35 : 0.4;
        double h2hWeight = h2hTotal > 0 ? 0.1 : 0.0;

        // ---- Beklenen skor hesaplama ----
        // Ev: kendi hücum ortalaması + rakibin yediği sayı + h2h katkısı + ev avantajı
        double expectedHome = (forWeight * h.getAvgPointsFor()) +
                              (againstWeight * a.getAvgPointsAgainst()) +
                              (h2hWeight * h2hAvgForHome) +
                              4.0; // ev avantajı

        // Deplasman: kendi hücum ortalaması + rakibin yediği sayı + h2h katkısı
        double expectedAway = (forWeight * a.getAvgPointsFor()) +
                              (againstWeight * h.getAvgPointsAgainst()) +
                              (h2hWeight * h2hAvgForAway);
        
        System.out.println(expectedHome + " - " + expectedAway);
        System.out.println(h2hTotal + " - " + h2hAvgForHome + " - " + h2hAvgForAway);

        // H2H toplam skor ortalamasıyla uyumlu hale getir (stabilizasyon)
        if (h2hTotal > 0) {
            double currentTotal = expectedHome + expectedAway;
            double diff = h2hTotal - currentTotal;
            expectedHome += diff * 0.25;
            expectedAway += diff * 0.25;
        }
        
        System.out.println(expectedHome + " - " + expectedAway);
        
        // ---- Tahmin hesaplama ----
        double diff = expectedHome - expectedAway;
        double total = expectedHome + expectedAway;

        // MS tahmini
        String msPick = diff > 3 ? "MS1" : (diff < -3 ? "MS2" : "Yakın");

        // Alt/Üst tahmini
        String ouPick = total > barem + 3 ? "Üst"
                         : total < barem - 3 ? "Alt" : "Sınırda";
        
        if (barem < 0) {
        	ouPick = "-";
        }

        // Skor tahmini (yuvarlanmış)
        String score = String.format("%d-%d",
                Math.round(expectedHome),
                Math.round(expectedAway));

        // Güven oranı
        double confidence = Math.min(1.0, 0.5 + (Math.abs(diff) / 25.0));

        // Over ve BTTS benzeri olasılıklar (yaklaşık)
        double pOver = clamp((total - barem) / 30.0 + 0.5, 0, 1);
        double pHome = clamp(0.5 + (diff / 20.0), 0, 1);
        double pAway = 1.0 - pHome;

        return new PredictionResult(
                name(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                pHome, 0.0, pAway,
                pOver, 0.0,
                msPick + " | " + ouPick,
                confidence,
                score
        );
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
