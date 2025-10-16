package com.basketbol.algorithm;

import com.basketbol.model.*;
import java.util.Optional;

public class PaceAdjustedModel implements BettingAlgorithm {

    @Override
    public String name() { return "PaceAdjustedModel"; }

    // Ensemble'a katkı verebilsin diye 0.30 öneriyorum
    @Override
    public double weight() { return 0.30; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        BasketballStats h = match.getHomeStats();
        BasketballStats a = match.getAwayStats();
        if (h == null || a == null || h.isEmpty() || a.isEmpty())
            return neutral(match);

        // --- Barem (O/U) ---
        Double barem = null;
        if (match.getOdds() != null && match.getOdds().gethOverUnderValue() != null
                && match.getOdds().gethOverUnderValue() > 0)
            barem = match.getOdds().gethOverUnderValue();

        // --- Güvenli değerler ---
        double paceH = safe(h.getAvgPossessions(), 90);
        double paceA = safe(a.getAvgPossessions(), 90);
        double offH  = safe(h.getAvgOffensiveRating(), 105);
        double offA  = safe(a.getAvgOffensiveRating(), 105);
        double defH  = safe(h.getAvgDefensiveRating(), 105);
        double defA  = safe(a.getAvgDefensiveRating(), 105);

        // --- Dinamik pace bandı (bareme göre lig temposunu sez) ---
        double lowerPace, upperPace;
        if (barem != null) {
            if (barem > 190) {          // NBA tipi
                lowerPace = 90; upperPace = 115;
            } else if (barem < 170) {   // Avrupa/FIBA
                lowerPace = 70; upperPace = 95;
            } else {                    // Orta tempo
                lowerPace = 80; upperPace = 105;
            }
        } else {
            // Barem yoksa geniş ama makul band
            lowerPace = 75; upperPace = 110;
        }

        double pace = clamp((paceH + paceA) / 2.0, lowerPace, upperPace);

        // --- Beklenen toplam skor (doğru ölçek) ---
        double expectedTotal = (pace * (offH + offA)) / 200.0;

        // Savunma kalitesine küçük düzeltme (iyi savunma toplamı düşürür, kötü artırır)
        // def rating ~100 normal; (defH+defA - 200) pozitifse savunma zayıf demektir.
        expectedTotal -= ((defH + defA - 200.0) / 4.0);

        // Güvenlik bandı (barem varsa ±50, yoksa geniş)
        double lower = (barem != null) ? barem - 50.0 : 130.0;
        double upper = (barem != null) ? barem + 50.0 : 250.0;
        expectedTotal = clamp(expectedTotal, lower, upper);

        // --- pOver ---
        double pOver = 0.5; // nötr
        if (barem != null) {
            double diff = expectedTotal - barem;
            // diff=8–10 civarı ~ %70-75 üretir; biraz daha duyarlı istersen 8 kullan
            pOver = sigmoid(diff / 8.0);
        }

        // --- Maç sonucu (rating farkı) ---
        double ratingH = safe(h.getRating100(), 100);
        double ratingA = safe(a.getRating100(), 100);
        double pHome = sigmoid((ratingH - ratingA) / 25.0);

        String msPick = pHome > 0.55 ? "MS1" : (pHome < 0.45 ? "MS2" : "Yakın");
        String ouPick;
        if (barem == null) {
            ouPick = "-";
        } else if (pOver > 0.55) {
            ouPick = "Üst";
        } else if (pOver < 0.45) {
            ouPick = "Alt";
        } else {
            ouPick = "Sınırda";
        }

        // --- Güven ---
        double confidence;
        if (barem == null) {
            // pOver yoksa güveni sadece maç sonucuna göre hesapla
            confidence = clamp(Math.abs(pHome - 0.5) * 1.2, 0.30, 0.90);
        } else {
            confidence = Math.max(Math.abs(pOver - 0.5), Math.abs(pHome - 0.5));
            confidence = clamp(confidence * 1.2, 0.35, 0.95);
        }

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
