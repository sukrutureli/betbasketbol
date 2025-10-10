package com.basketbol.scraper;

import com.basketbol.model.MatchInfo;
import java.util.ArrayList;
import java.util.List;

public class BasketballScraper {

    public List<MatchInfo> fetchMatches() {
        System.out.println("📊 Maç verileri çekiliyor (örnek veri ile)...");
        List<MatchInfo> list = new ArrayList<>();

        // Şimdilik örnek veri (ileride Selenium entegre ederiz)
        list.add(new MatchInfo("Fenerbahçe", "Anadolu Efes"));
        list.add(new MatchInfo("Real Madrid", "Barcelona"));
        return list;
    }
}
