package com.basketbol;

import com.basketbol.scraper.BasketballScraper;
import com.basketbol.algorithm.HeuristicPredictor;
import com.basketbol.html.HtmlReportGenerator;
import com.basketbol.model.MatchInfo;

import java.util.List;

public class Application {

    public static void main(String[] args) {
        System.out.println("🏀 Basketbol Tahmin Uygulaması Başlatıldı...");

        // 1. Maç verilerini çek
        BasketballScraper scraper = new BasketballScraper();
        List<MatchInfo> matches = scraper.fetchMatches();

        // 2. Tahminleri üret
        HeuristicPredictor predictor = new HeuristicPredictor();
        predictor.predict(matches);

        // 3. HTML raporunu oluştur
        HtmlReportGenerator.generateHtml(matches, "basketbol.html");

        System.out.println("✅ İşlem tamamlandı. public/basketbol.html dosyası oluşturuldu.");
    }
}
