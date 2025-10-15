package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

public class PaceAdjustedModel implements BettingAlgorithm {

    @Override
    public String name() { return "PaceAdjustedModel"; }

    @Override
    public double weight() { return 0.25; } // biraz artırdım çünkü toplam skor hesaplarında güçlü

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        BasketballStats h = match.getHomeStats();
        BasketballStats a = match.getAwayStats();

        if (h == null || a == null || h.isEmpty() || a.isEmpty())
            return neutral(match);

        // --- Barem al ---
        Double barem = null;
        if (match.getOdds() != null && match.getOdds().gethOverUnderValue() != null
                && match.getOdds().gethOverUnderValue() > 0)
            barem = match.getOdds().gethOverUnderValue();

        // --- Güvenli değerler ---
        double paceH = safe(h.getAvgPossessions(), 90);
        double paceA = safe(a.getAvgPossessions(), 90);
        double offH = safe(h.getAvgOffensiveRating(), 105);
        double offA = safe(a.getAvgOffensiveRating(), 105);
        double defH = safe(h.getAvgDefensiveRating(), 105);
        double defA = safe(a.getAvgDefensiveRating(), 105);

        // --- Tempo & Etkinlik birleşimi ---
        double paceFactor = (paceH + paceA) / 2.0; // tipik 85–100 arası
        double effCombined = ((offH + offA) - (defH + defA - 200)) / 2.0; // hücum güçlü, savunma zayıf → yüksek skor

        // --- Beklenen toplam skor ---
        double expectedTotal = (paceFactor / 100.0) * effCombined;
        expectedTotal = clamp(expectedTotal, 120, 210); // basketbol real aralığı

        // --- pOver hesabı ---
        double pOver = 0.5;
        if (barem != null && barem > 0)
            pOver = sigmoid((expectedTotal - barem) / 10.0); // 10 puan fark = %75 olasılık civarı

        // --- Maç sonucu (Rating farkı) ---
        double ratingH = safe(h.getRating100(), 100);
        double ratingA = safe(a.getRating100(), 100);
        double pHome = sigmoid((ratingH - ratingA) / 30.0);

        // --- Karar ---
        String msPick = pHome > 0.55 ? "MS1" : (pHome < 0.45 ? "MS2" : "Yakın");
        String ouPick = (barem == null) ? "-" :
                        (pOver > 0.55 ? "Üst" : (pOver < 0.45 ? "Alt" : "Sınırda"));

        double confidence = Math.max(Math.abs(pOver - 0.5), Math.abs(pHome - 0.5)) * 1.5;
        confidence = clamp(confidence, 0.3, 0.9);

        String finalPick = msPick + " | " + ouPick;

        return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(),
                pHome, 0.0, 1 - pHome, pOver, 0.0,
                finalPick, confidence, "");
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double safe(Double v, double def) {
        return (v == null || Double.isNaN(v)) ? def : v;
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private PredictionResult neutral(Match m) {
        return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(),
                0.5, 0, 0.5, 0.5, 0, "-", 0.3, "-");
    }
}
