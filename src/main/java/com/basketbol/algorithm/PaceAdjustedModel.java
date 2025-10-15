package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

public class PaceAdjustedModel implements BettingAlgorithm {

    @Override
    public String name() { return "PaceAdjustedModel"; }

    @Override
    public double weight() { return 0.0; }

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

        // --- Ortalama tempo ve etkinlik ---
        double pace = clamp((paceH + paceA) / 2.0, 80, 110); // basketbol temposu 80–110 arası
        double effOff = (offH + offA) / 2.0;
        double effDef = (defH + defA) / 2.0;

        // --- Beklenen toplam skor ---
        double expectedTotal = (pace / 100.0) * ((effOff + (200 - effDef)) / 2.0);
        expectedTotal = clamp(expectedTotal, 130, 200); // normal maç aralığı

        // --- pOver hesabı ---
        double pOver = 0.5; // nötr
        if (barem != null && barem > 0) {
            double diff = expectedTotal - barem;
            // 20 sayı fark = %75 olasılık civarı, yani daha yumuşak sigmoid
            pOver = sigmoid(diff / 20.0);
        }

        // --- Maç sonucu (rating farkı) ---
        double ratingH = safe(h.getRating100(), 100);
        double ratingA = safe(a.getRating100(), 100);
        double pHome = sigmoid((ratingH - ratingA) / 25.0);

        // --- Karar ---
        String msPick = pHome > 0.55 ? "MS1" : (pHome < 0.45 ? "MS2" : "Yakın");
        String ouPick;
        if (barem == null)
            ouPick = "-";
        else if (pOver > 0.55)
            ouPick = "Üst";
        else if (pOver < 0.45)
            ouPick = "Alt";
        else
            ouPick = "Sınırda";

        // --- Güven hesabı ---
        double confidence = Math.max(Math.abs(pOver - 0.5), Math.abs(pHome - 0.5));
        confidence = clamp(confidence * 1.2, 0.35, 0.9);

        String finalPick = msPick + " | " + ouPick;

        return new PredictionResult(
                name(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                pHome, 0.0, 1 - pHome,
                pOver, 0.0,
                finalPick,
                confidence,
                "-"
        );
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double safe(Double v, double def) {
        return (v == null || Double.isNaN(v) || v == 0) ? def : v;
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private PredictionResult neutral(Match m) {
        return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(),
                0.5, 0, 0.5, 0.5, 0, "-", 0.3, "-");
    }
}
