package com.basketbol;

import com.basketbol.scraper.BasketballScraper;
import com.basketbol.algorithm.BettingAlgorithm;
import com.basketbol.algorithm.EnsembleModel;
import com.basketbol.algorithm.HeuristicPredictor;
import com.basketbol.html.HtmlReportGenerator;
import com.basketbol.model.Match;
import com.basketbol.model.MatchInfo;
import com.basketbol.model.PredictionResult;
import com.basketbol.model.TeamMatchHistory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Application {

	public static void main(String[] args) {
		BasketballScraper scraper = null;
		MatchHistoryManager historyManager = new MatchHistoryManager();
		List<MatchInfo> matches = null;
		List<Match> matchStats = new ArrayList<Match>();
		ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

		try {
			System.out.println("=== İddaa Scraper Başlatılıyor ===");
			System.out.println("Zaman: " + LocalDateTime.now(istanbulZone));

			// Scraper'ı başlat
			scraper = new BasketballScraper();

			// Ana sayfa verilerini çek
			System.out.println("\n1. Ana sayfa maçları çekiliyor...");
			matches = scraper.fetchMatches();

			System.out.println("Ana sayfadan " + matches.size() + " maç çekildi");

			for (int i = 0; i < matches.size(); i++) {
				MatchInfo match = matches.get(i);

				// Detay URL'si varsa geçmiş verilerini çek
				if (match.hasDetailUrl()) {
					System.out.println("Geçmiş çekiliyor " + (i + 1) + "/" + matches.size() + ": " + match.getName());

					try {
						TeamMatchHistory teamHistory = scraper.scrapeTeamHistory(match.getDetailUrl(), match.getName(),
								match.getOdds());

						if (teamHistory != null) {
							historyManager.addTeamHistory(teamHistory);
                            matchStats.add(teamHistory.createStats(match));
						}

						// Rate limiting - 3 saniye bekle
						Thread.sleep(1000);

						if ((i + 1) % 5 == 0) {
							System.gc(); // Garbage collection tetikle
						}

					} catch (Exception e) {
						System.out.println("Geçmiş çekme hatası: " + e.getMessage());
					}
				}

				// Her 20 maçta bir progress yazdır
				if ((i + 1) % 20 == 0) {
					System.out.println("İşlendi: " + (i + 1) + "/" + matches.size());
				}
			}
			
            BettingAlgorithm heur = new HeuristicPredictor();
            EnsembleModel ensemble = new EnsembleModel(List.of(heur));

            List<PredictionResult> results = new ArrayList<>();
            for (Match m : matchStats) {
                results.add(ensemble.predict(m, Optional.empty()));
            }

			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "basketbol_.html");

			System.out.println("basketbol.html oluşturuldu.");

		} catch (Exception e) {
			System.out.println("GENEL HATA: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (scraper != null) {
				scraper.close();
			}
		}
	}
}
