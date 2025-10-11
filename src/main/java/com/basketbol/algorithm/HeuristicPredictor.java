package com.basketbol.algorithm;

import com.basketbol.model.MatchInfo;
import java.util.List;

public class HeuristicPredictor {

    public void predict(List<MatchInfo> matches) {
        System.out.println("🧮 Tahminler yapılıyor...");
        for (MatchInfo m : matches) {
            // Basit örnek: ev sahibi avantajı
            //m.setPrediction("MS 1 (örnek tahmin)");
        }
    }
}
