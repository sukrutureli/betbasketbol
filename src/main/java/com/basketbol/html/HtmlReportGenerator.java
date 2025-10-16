package com.basketbol.html;

import com.basketbol.MatchHistoryManager;
import com.basketbol.model.LastPrediction;
import com.basketbol.model.Match;
import com.basketbol.model.MatchInfo;
import com.basketbol.model.MatchResult;
import com.basketbol.model.PredictionResult;
import com.basketbol.model.TeamMatchHistory;
import com.basketbol.util.MathUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class HtmlReportGenerator {

	public static void generateHtml(List<MatchInfo> matches, MatchHistoryManager historyManager, List<Match> matchStats,
			List<PredictionResult> results, String fileName) {

		ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

		// HTML oluşturmaya başla
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
		html.append("<title>🏀 Basketbol Tahminleri</title>");
		html.append("<style>");
		html.append(
				"body { font-family: 'Segoe UI', Roboto, Arial, sans-serif; margin: 0; padding: 20px; background-color: #f3f6fa; color: #222; }");
		html.append("h1 { text-align: center; color: #004d80; margin-bottom: 25px; font-size: 26px; }");

		/* Kart yapısı */
		html.append(
				".match { background: #fff; border: 1px solid #dce3ec; margin: 18px 0; padding: 18px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: transform 0.2s, box-shadow 0.2s; }");
		html.append(".match:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.12); }");
		html.append(".match.insufficient { background-color: #fff1f1; border-left: 4px solid #dc3545; }");

		/* Başlık */
		html.append(
				".match-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }");
		html.append(".match-name { font-weight: 700; color: #003366; font-size: 1.1em; }");
		html.append(".match-time { color: #666; font-size: 0.9em; }");
		html.append(
				".match-header button { background: linear-gradient(180deg,#007bff,#0062cc); border: none; color: #fff; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.9em; }");
		html.append(".match-header button:hover { background: linear-gradient(180deg,#0069d9,#005cbf); }");

		/* Mini oran tablosu */
		html.append(
				".odds-mini { background: #f8fafc; border: 1px solid #dbe2ea; padding: 12px; border-radius: 8px; margin: 10px 0; }");
		html.append(".odds-mini h4 { margin: 0 0 8px 0; color: #004d80; font-size: 1em; }");
		html.append(
				".odds-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(100px, 1fr)); gap: 8px; }");
		html.append(
				".odds-cell { background: #fff; border: 1px solid #e0e6ee; border-radius: 6px; padding: 8px; text-align: center; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }");
		html.append(".odds-label { display: block; font-weight: 600; color: #004d80; }");
		html.append(".odds-value { font-size: 1em; color: #111; font-weight: 700; }");
		html.append(".odds-pct { display: block; font-size: 0.85em; color: #666; margin-top: 2px; }");
		html.append(
				".highlight { background-color: #e7f5e9; border-color: #28a745; box-shadow: 0 0 5px rgba(40,167,69,0.3); }");

		/* Quick summary */
		html.append(
				".quick-summary table.qs { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ccd6e0; border-radius: 8px; overflow: hidden; margin-top: 10px; }");
		html.append(
				".quick-summary th, .quick-summary td { padding: 8px; text-align: center; border: 1px solid #e1e7ef; }");
		html.append(".quick-summary th { background: #f0f5fb; color: #003366; font-weight: 600; }");
		html.append(
				".qs-pick .pick { display: inline-block; padding: 3px 10px; border-radius: 12px; background: #e7f1ff; color: #004d80; font-weight: 700; }");
		html.append(".qs-score { color: #111; font-weight: 600; }");

		/* History */
		html.append(".history { margin-top: 14px; }");
		html.append(
				".history-section { background: #f4f7fb; border: 1px solid #e1e7ef; padding: 14px; border-radius: 8px; margin: 10px 0; }");
		html.append(".history-section h5 { margin-top: 0; color: #004d80; }");
		html.append(
				".match-result { background: #fff; padding: 6px 10px; margin: 4px 0; border-left: 4px solid #007bff; border-radius: 4px; font-size: 0.9em; transition: background 0.2s; }");
		html.append(".match-result.win { border-left-color: #28a745; background-color: #e9f7ef; }");
		html.append(".match-result.loss { border-left-color: #dc3545; background-color: #fdeaea; }");
		html.append(".match-result:hover { background: #f3f7ff; }");

		/* Stats box */
		html.append(
				".team-stats { background: #e3f2fd; color: #0c5460; padding: 10px 12px; border-radius: 6px; margin: 8px 0; font-size: 0.9em; }");
		html.append(
				".stats { background: #fff; border: 1px solid #dbe2ea; padding: 18px; margin: 20px 0; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }");
		html.append(".stats h3 { color: #004d80; margin-top: 0; }");

		/* No-data & footer */
		html.append(
				".no-data { color: #999; font-style: italic; padding: 20px; text-align: center; background: #fff; border: 1px dashed #ccc; border-radius: 8px; }");
		html.append("footer { text-align: center; color: #777; font-size: 0.85em; margin-top: 40px; }");

		/* Mobil */
		html.append("@media (max-width: 600px) {");
		html.append("  .match-header { flex-direction: column; align-items: flex-start; gap: 6px; }");
		html.append("  .odds-grid { grid-template-columns: repeat(2, 1fr); }");
		html.append("}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<h1>🏀 Basketbol Tahminleri</h1>");
		html.append("<p>Son güncelleme: " + LocalDateTime.now(istanbulZone) + "</p>");

		int detailUrlCount = 0;
		int processedTeamCount = 0;

		// URL'li maçları say
		for (MatchInfo match : matches) {
			if (match.hasDetailUrl())
				detailUrlCount++;
		}

		html.append("<div class='stats'>");
		html.append("<h3>İstatistikler</h3>");
		html.append("<p>- Toplam maç: ").append(matches.size()).append("</p>");
		html.append("<p>- Detay URL'si olan: ").append(detailUrlCount).append("</p>");
		html.append("<p>- Geçmiş verisi çekilecek: ").append(detailUrlCount).append("</p>");
		html.append("</div>");

		for (int i = 0; i < matches.size(); i++) {
			MatchInfo match = matches.get(i);
			if (!match.hasDetailUrl()) {
				html.append("<div class='no-data'>Detay URL'si bulunamadı</div>");
				continue;
			}

			TeamMatchHistory teamHistory = historyManager.getTeamHistories().get(i);
			boolean insufficient = (teamHistory != null && !teamHistory.isInfoEnough());
			html.append("<div class='match").append(insufficient ? " insufficient" : "").append("'>");

			html.append("<div class='match-header'>");
			html.append("<div class='match-name'>").append(match.getName()).append("</div>");
			html.append("<div class='match-time'>").append(match.getTime()).append("</div>");
			html.append("<button onclick=\"toggleHistory(this)\">Göster/Gizle</button>");
			html.append("</div>");

			if (teamHistory != null && teamHistory.getTotalMatches() > 0) {
				html.append("<div class='odds-mini'>");
				html.append("<h4>Güncel Oranlar</h4>");
				html.append("<div class='odds-grid'>");
				html.append("<div class='odds-cell " + teamHistory.getStyle("MS1", results.get(i).getPick())
						+ "'><span class='odds-label'>MS1</span><span class='odds-value'>")
						.append(match.getOdds().getMs1()).append("</span><span class='odds-pct'>")
						.append(teamHistory.toStringAsPercentage(teamHistory.getMs1())).append("</span></div>");
				html.append("<div class='odds-cell " + teamHistory.getStyle("MS2", results.get(i).getPick())
						+ "'><span class='odds-label'>MS2</span><span class='odds-value'>")
						.append(match.getOdds().getMs2()).append("</span><span class='odds-pct'>")
						.append(teamHistory.toStringAsPercentage(teamHistory.getMs2())).append("</span></div>");
				html.append("<div class='odds-cell " + teamHistory.getStyle("Alt", results.get(i).getPick())
						+ "'><span class='odds-label'>Alt</span><span class='odds-value'>")
						.append(match.getOdds().getUnder()).append("</span><span class='odds-pct'>")
						.append(teamHistory.toStringAsPercentage(teamHistory.getAlt())).append("</span></div>");
				html.append("<div class='odds-cell " + teamHistory.getStyle("Üst", results.get(i).getPick())
						+ "'><span class='odds-label'>Üst</span><span class='odds-value'>")
						.append(match.getOdds().getOver()).append("</span><span class='odds-pct'>")
						.append(teamHistory.toStringAsPercentage(teamHistory.getUst())).append("</span></div>");
				html.append("</div></div>");

				html.append("<div class='quick-summary'>");
				html.append(
						"<table class='qs'><thead><tr><th>MS1</th><th>MS2</th><th>Alt</th><th>Üst</th><th>Tahmin</th><th>Skor</th><th>Güven</th></tr></thead><tbody><tr>");
				html.append("<td>").append(MathUtils.fmtPct(results.get(i).getpHome())).append("</td>");
				html.append("<td>").append(MathUtils.fmtPct(results.get(i).getpAway())).append("</td>");
				html.append("<td>").append(MathUtils.fmtPct(1 - results.get(i).getpOver25())).append("</td>");
				html.append("<td>").append(MathUtils.fmtPct(results.get(i).getpOver25())).append("</td>");
				html.append("<td class='qs-pick'><span class='pick'>").append(results.get(i).getPick())
						.append("</span></td>");
				html.append("<td>").append(results.get(i).getScoreline()).append("</td>");
				html.append("<td>").append(String.format("%.0f%%", results.get(i).getConfidence() * 100))
						.append("</td>");
				html.append("</tr></tbody></table></div>");

				html.append("<div class='history'>");
				html.append("<div class='team-stats'><p>Bakılan maç sayısı: Rekabet - ")
						.append(teamHistory.getRekabetGecmisi().size()).append(" | Ev - ")
						.append(teamHistory.getSonMaclarHome().size()).append(" | Deplasman - ")
						.append(teamHistory.getSonMaclarAway().size()).append("</p></div>");

				// Rekabet geçmişi
				if (!teamHistory.getRekabetGecmisi().isEmpty()) {
					html.append("<div class='history-section'><h5>Rekabet Geçmişi</h5>");
					for (MatchResult m : teamHistory.getRekabetGecmisi()) {
						String cls = getResultClass(m, teamHistory.getTeamName());
						html.append("<div class='match-result ").append(cls).append("'>").append(m.getMatchDate())
								.append(" - ").append(m.getHomeTeam()).append(" ").append(m.getScoreString())
								.append(" ").append(m.getAwayTeam()).append(" [").append(m.getTournament())
								.append("]</div>");
					}
					html.append("</div>");
				}

				// Son maçlar (home)
				if (!teamHistory.getSonMaclarHome().isEmpty()) {
					html.append("<div class='history-section'><h5>Ev Sahibi Son Maçlar</h5>");
					for (MatchResult m : teamHistory.getSonMaclarHome()) {
						String cls = getResultClass(m, teamHistory.getTeamName());
						html.append("<div class='match-result ").append(cls).append("'>").append(m.getMatchDate())
								.append(" - ").append(m.getHomeTeam()).append(" ").append(m.getScoreString())
								.append(" ").append(m.getAwayTeam()).append("</div>");
					}
					html.append("</div>");
				}

				// Son maçlar (away)
				if (!teamHistory.getSonMaclarAway().isEmpty()) {
					html.append("<div class='history-section'><h5>Deplasman Son Maçlar</h5>");
					for (MatchResult m : teamHistory.getSonMaclarAway()) {
						String cls = getResultClass(m, teamHistory.getTeamName());
						html.append("<div class='match-result ").append(cls).append("'>").append(m.getMatchDate())
								.append(" - ").append(m.getHomeTeam()).append(" ").append(m.getScoreString())
								.append(" ").append(m.getAwayTeam()).append("</div>");
					}
					html.append("</div>");
				}

				html.append("</div>");
				processedTeamCount++;
			} else {
				html.append("<div class='no-data'>Bu maç için geçmiş veri bulunamadı</div>");
			}

			html.append("<p><small>Element #").append(match.getIndex()).append("</small></p>");
			html.append("</div>");
		}

		html.append("<div class='stats'><h3>Final İstatistikleri</h3>");
		html.append("<p>- Toplam maç: ").append(matches.size()).append("</p>");
		html.append("<p>- Detay URL'si olan: ").append(detailUrlCount).append("</p>");
		html.append("<p>- Başarıyla geçmişi çekilen: ").append(processedTeamCount).append("</p>");
		html.append("<p>- Başarı oranı: ").append(
				detailUrlCount > 0 ? String.format("%.1f%%", processedTeamCount * 100.0 / detailUrlCount) : "0%")
				.append("</p></div>");

		html.append("<footer>Bu veriler otomatik olarak çekilmiştir - Son güncelleme: ")
				.append(LocalDateTime.now(istanbulZone)).append("</footer>");
		html.append("<script>");
		html.append(
				"function toggleHistory(btn){const d=btn.closest('.match');const s=d.querySelectorAll('.history .history-section');const h=s[0].style.display==='none';s.forEach(x=>x.style.display=h?'block':'none');btn.textContent=h?'Gizle':'Göster';}");
		html.append("document.querySelectorAll('.history .history-section').forEach(s=>s.style.display='none');");
		html.append("</script>");
		html.append("</body></html>");

		// Dosyaları kaydet
		File dir = new File("public");
		if (!dir.exists())
			dir.mkdirs();

		// HTML dosyasını kaydet
		try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
			fw.write(html.toString());
			html = null; // Reference'i sil
			System.gc(); // HTML string'i temizle
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void generateHtmlForSublist(List<LastPrediction> predictions, String fileName) {
		StringBuilder html = new StringBuilder();

		html.append("<!DOCTYPE html>\n");
		html.append("<html lang='tr'>\n");
		html.append("<head>\n");
		html.append("<meta charset='UTF-8'>\n");
		html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
		html.append("<title>✅ Hazır Kupon</title>\n");
		html.append(
				"<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css'>\n");
		html.append("<style>\n");

		/* --- Genel Stil --- */
		html.append(
				"body { font-family: Arial, sans-serif; background-color: #f7f8fa; margin: 0; padding: 20px; color: #222; }\n");
		html.append("h1 { text-align: center; margin-bottom: 20px; color: #333; font-size: 22px; }\n");

		/* --- Tablo --- */
		html.append("table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; ");
		html.append("box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }\n");
		html.append("th, td { padding: 10px 12px; text-align: left; }\n");
		html.append("th { background-color: #0077cc; color: white; font-size: 15px; }\n");
		html.append("tr:nth-child(even) { background-color: #f3f6fa; }\n");
		html.append("tr:hover { background-color: #eaf3ff; }\n");
		html.append("td { font-size: 14px; border-bottom: 1px solid #ddd; }\n");

		/* --- İkon hizalama --- */
		html.append(
				"td i, td svg, td img { display:inline-block; vertical-align:middle; margin-right:4px; color:#0077cc; }\n");

		/* --- Sütun oranları --- */
		html.append("th:nth-child(1), td:nth-child(1) { width: 60px; text-align: left; white-space: nowrap; }\n");
		html.append(
				"th:nth-child(2), td:nth-child(2) { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n");
		html.append("th:nth-child(3), td:nth-child(3) { width: auto; }\n");
		html.append(
				"th:nth-child(4), td:nth-child(4) { width: 120px; text-align: left; color: #333; font-weight: bold; }\n");

		html.append(".match { font-weight: bold; color: #1a1a1a; }\n");
		html.append(".prediction { color: #444; white-space: pre-line; }\n");

		/* --- Mobil görünüm (max 600px) --- */
		html.append("@media (max-width: 600px) {\n");
		html.append("  table, thead, tbody, th, td, tr { display: block; width: 100%; }\n");
		html.append("  thead { display: none; }\n");
		html.append(
				"  tr { margin-bottom: 12px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); background: #fff; padding: 8px; }\n");
		html.append("  td { border: none; padding: 6px 8px; }\n");
		html.append("  td i { margin-right: 6px; }\n");
		html.append("  td span.label { display:block; font-weight:bold; color:#0077cc; margin-bottom:3px; }\n");
		html.append("}\n");

		html.append("</style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("<h1>✅ Hazır Kupon</h1>\n");
		html.append("<table>\n");
		html.append(
				"<thead><tr><th>🕒 Saat</th><th>⚽ Maç</th><th>🎯 Tahmin</th><th>📊 Skor Tahmini</th></tr></thead>\n");
		html.append("<tbody>\n");

		for (LastPrediction p : predictions) {
			html.append("<tr>");
			html.append("<td><i class='fa-regular fa-clock'></i>").append(p.getTime()).append("</td>");
			html.append("<td class='match'><i class='fa-solid fa-futbol'></i>").append(p.getName()).append("</td>");
			html.append("<td class='prediction'><i class='fa-solid fa-bullseye'></i>").append(p.preditionsToString())
					.append("</td>");
			html.append("<td class='score'><i class='fa-solid fa-chart-line'></i>")
					.append(p.getScore() != null ? p.getScore() : "-").append("</td>");
			html.append("</tr>\n");
		}

		html.append("</tbody></table>\n");
		html.append("</body>\n</html>");

		File dir = new File("public");
		if (!dir.exists())
			dir.mkdirs();

		try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
			fw.write(html.toString());
			html = null;
			System.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getResultClass(MatchResult match, String teamName) {
		String result = match.getResult();

		// Takımın ev sahibi mi deplasman mı olduğunu kontrol et
		boolean isHome = teamName.contains(match.getHomeTeam());

		if (result.equals("D")) {
			return "draw";
		} else if ((isHome && result.equals("H")) || (!isHome && result.equals("A"))) {
			return "win";
		} else {
			return "loss";
		}
	}
}
