package com.basketbol.html;

import com.basketbol.MatchHistoryManager;
import com.basketbol.model.*;
import com.basketbol.util.MathUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class HtmlReportGenerator {

	// === DETAYLI RAPOR ===
	public static void generateHtml(List<MatchInfo> matches, MatchHistoryManager historyManager, List<Match> matchStats,
			List<PredictionResult> results, String fileName, List<RealScores> realScores) {

		ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

		// HTML oluşturmaya başla
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
		html.append("<title>🏀 Basketbol Tahminleri</title>");
		html.append("<style>");
		// BURAYA EKLENECEK ⬇⬇⬇
		html.append(
				"body{margin:0!important;padding:0!important;width:100%!important;max-width:100%!important;box-sizing:border-box!important;}");
		html.append(
				".match,.stats,.odds-mini,.quick-summary,.history-section{width:100%!important;max-width:100%!important;margin-left:0!important;margin-right:0!important;padding-left:10px!important;padding-right:10px!important;box-sizing:border-box!important;}");
		html.append("table,.qs,.qs td,.qs th{width:100%!important;}");

		html.append("body { font-family: 'Segoe UI', Roboto, Arial, sans-serif; ... }");

		html.append("h1 { text-align: center; color: #004d80; margin-bottom: 25px; font-size: 26px; }");

		/* --- Match card --- */
		html.append(
				".match { background: #fff; border: 1px solid #dce3ec; margin: 18px 0; padding: 18px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }");
		html.append(".match:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.12); }");
		html.append(".match.insufficient { background-color: #fff1f1; border-left: 4px solid #dc3545; }");

		/* --- Header --- */
		html.append(
				".match-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }");
		html.append(".match-name { font-weight: 700; color: #003366; font-size: 1.5em; line-height: 1.2; }");
		html.append(".match-time { color: #004d80; font-size: 1.3em; font-weight: 600; }");
		html.append(
				".match-header button { background: linear-gradient(180deg,#007bff,#0062cc); border: none; color: #fff; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.9em; }");
		html.append(".match-header button:hover { background: linear-gradient(180deg,#0069d9,#005cbf); }");

		/* --- Odds Grid --- */
		html.append(
				".odds-mini { background: #f8fafc; border: 1px solid #dbe2ea; padding: 12px; border-radius: 10px; margin: 14px 0; }");
		html.append(
				".odds-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; }");
		html.append(
				".odds-cell { background: #fff; border: 1px solid #ccd6e0; border-radius: 8px; padding: 8px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }");
		html.append(".highlight { background: #e9f9ec; border-color: #28a745 !important; }");
		html.append(".odds-label { font-weight: 700; color: #004d80; font-size: 0.95em; display: inline-block; }");
		html.append(
				".odds-value { display: inline-block; font-weight: 600; color: #111; font-size: 0.9em; margin-left: 4px; }");
		html.append(".odds-line { margin-bottom: 4px; }");
		html.append(".odds-pct { display: block; color: #555; font-size: 0.82em; margin-top: 2px; }");

		/* --- Quick Summary --- */
		html.append(
				".quick-summary table.qs { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ccd6e0; border-radius: 8px; overflow: hidden; margin-top: 10px; }");
		html.append(
				".quick-summary th, .quick-summary td { padding: 8px; text-align: center; border: 1px solid #e1e7ef; }");
		html.append(".quick-summary th { background: #f0f5fb; color: #003366; font-weight: 600; }");
		html.append(".qs-odd { font-variant-numeric: tabular-nums; color: #333; }");
		html.append(
				".qs-pick .pick { display: inline-block; padding: 3px 10px; border-radius: 12px; background: #e7f1ff; color: #004d80; font-weight: 700; }");
		html.append(".qs-score { color: #111; font-weight: 600; }");

		/* --- Team Stats & History --- */
		html.append(
				".team-stats { background: #e3f2fd; color: #0c5460; padding: 10px 12px; border-radius: 6px; margin: 8px 0; font-size: 0.9em; }");
		html.append(
				".stats { background: #fff; border: 1px solid #dbe2ea; padding: 18px; margin: 20px 0; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }");
		html.append(".stats h3 { color: #004d80; margin-top: 0; }");
		html.append(
				".history-section { background: #f4f7fb; border: 1px solid #e1e7ef; padding: 14px; border-radius: 8px; margin: 10px 0; }");
		html.append(
				".match-result { background: #fff; padding: 6px 10px; margin: 4px 0; border-left: 4px solid #007bff; border-radius: 4px; font-size: 0.9em; }");
		html.append(".match-result.win { border-left-color: #28a745; background-color: #e9f7ef; }");
		html.append(".match-result.loss { border-left-color: #dc3545; background-color: #fdeaea; }");

		/* --- Responsive --- */
		html.append("@media (max-width: 600px) { .odds-grid { grid-template-columns: repeat(2, 1fr); } }");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<h1>🏀 Basketbol Tahminleri</h1>");
		html.append("<p>Son güncelleme: " + LocalDateTime.now(ZoneId.of("Europe/Istanbul")) + "</p>");

		// İstatistik bilgileri
		int detailUrlCount = 0;
		int processedTeamCount = 0;

		// URL'li maçları say
		for (MatchInfo match : matches) {
			if (match.hasDetailUrl()) {
				detailUrlCount++;
			}
		}

		html.append("<div class='stats'>");
		html.append("<h3>İstatistikler</h3>");
		html.append("<p>- Toplam maç: ").append(matches.size()).append("</p>");
		html.append("<p>- Detay URL'si olan: ").append(detailUrlCount).append("</p>");
		html.append("<p>- Geçmiş verisi çekilecek: ").append(detailUrlCount).append("</p>");
		html.append("</div>");

		for (int i = 0; i < matches.size(); i++) {
			MatchInfo match = matches.get(i);
			TeamMatchHistory teamHistory = historyManager.getTeamHistories().get(i);
			boolean insufficient = (teamHistory != null && !teamHistory.isInfoEnough()
					&& !teamHistory.isInfoEnoughWithoutRekabet());

			String homeStr = match.getName().split(" - ")[0];
			String awayStr = match.getName().split(" - ")[1];

			html.append("<div class='match").append(insufficient ? " insufficient" : "").append("'>");
			html.append("<div class='match-header'>");
			html.append("<div class='match-name'>").append(match.getName())
					.append(getRealScore(realScores, homeStr, awayStr)).append("</div>");
			html.append("<div class='match-time'>").append(match.getTime()).append("</div>");
//			html.append("<button onclick=\"toggleHistory(this)\">Göster</button>");
			html.append("</div>");

			if (teamHistory != null && teamHistory.getTotalMatches() > 0) {
				html.append("<div class='odds-mini'>");
				html.append("<h4>Güncel Oranlar ve Yüzdeler</h4>");
				html.append("<div class='odds-grid'>");

				// ÜST: MS1 - MS2 - H1 - H2
				html.append("<div class='odds-cell' style='"
						+ teamHistory.getStyle("MS1", teamHistory.getMs1(), match.getOdds().getMs1()) + "'>");
				html.append("<div class='odds-line'><span class='odds-label'>MS1:</span><span class='odds-value'>"
						+ match.getOdds().getMs1() + "</span></div>");
				html.append("<span class='odds-pct'>" + MathUtils.fmtPct(teamHistory.getMs1()) + "</span></div>");

				html.append("<div class='odds-cell' style='"
						+ teamHistory.getStyle("MS2", teamHistory.getMs2(), match.getOdds().getMs2()) + "'>");
				html.append("<div class='odds-line'><span class='odds-label'>MS2:</span><span class='odds-value'>"
						+ match.getOdds().getMs2() + "</span></div>");
				html.append("<span class='odds-pct'>" + MathUtils.fmtPct(teamHistory.getMs2()) + "</span></div>");

				html.append("<div class='odds-cell " + teamHistory.getStyle("H1", 0.0, match.getOdds().getH1Value())
						+ "'>");
				html.append("<div class='odds-line'><span class='odds-label'>H1 (" + match.getOdds().getH1Value()
						+ "):</span><span class='odds-value'>" + match.getOdds().getH1() + "</span></div></div>");

				html.append("<div class='odds-cell " + teamHistory.getStyle("H2", 0.0, match.getOdds().getH2Value())
						+ "'>");
				html.append("<div class='odds-line'><span class='odds-label'>H2 (" + match.getOdds().getH2Value()
						+ "):</span><span class='odds-value'>" + match.getOdds().getH2() + "</span></div></div>");
				html.append("</div>");

				// ALT / ÜST
				html.append("<div class='odds-grid' style='margin-top:8px;'>");

				// ALT: Alt - Üst
				html.append("<div class='odds-cell' style='"
						+ teamHistory.getStyle("Alt", teamHistory.getAlt(), match.getOdds().getUnder()) + "'>");
				html.append(
						"<div class='odds-line'><span class='odds-label'>Alt (" + match.getOdds().gethOverUnderValue()
								+ "):</span><span class='odds-value'>" + match.getOdds().getUnder() + "</span></div>");
				html.append("<span class='odds-pct'>" + MathUtils.fmtPct(teamHistory.getAlt()) + "</span></div>");

				html.append("<div class='odds-cell' style='"
						+ teamHistory.getStyle("Üst", teamHistory.getUst(), match.getOdds().getOver()) + "'>");
				html.append(
						"<div class='odds-line'><span class='odds-label'>Üst (" + match.getOdds().gethOverUnderValue()
								+ "):</span><span class='odds-value'>" + match.getOdds().getOver() + "</span></div>");
				html.append("<span class='odds-pct'>" + MathUtils.fmtPct(teamHistory.getUst()) + "</span></div>");

				html.append("</div>"); // odds-grid
				html.append("</div>"); // odds-mini

				// Team stats
				int rekabet = teamHistory.getRekabetGecmisi().size();
				int home = teamHistory.getSonMaclarHome().size();
				int away = teamHistory.getSonMaclarAway().size();

				if (home > 0 && away > 0) {
					// Quick summary
					html.append("<div class='quick-summary'>");
					html.append("<table class='qs'><thead><tr>");
					html.append(
							"<th>MS1</th><th>MS2</th><th>Alt</th><th>Üst</th><th>Tahmin</th><th>Skor</th><th>Güven</th>");
					html.append("</tr></thead><tbody><tr>");
					html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpHome()) + "</td>");
					html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpAway()) + "</td>");
					html.append("<td class='qs-odd'>" + MathUtils.fmtPct(1 - results.get(i).getpOver25()) + "</td>");
					html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpOver25()) + "</td>");
					html.append("<td class='qs-pick'><span class='pick'>" + results.get(i).getPick() + "</span></td>");
					html.append("<td class='qs-score'>" + results.get(i).getScoreline() + "</td>");
					html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getConfidence()) + "</td>");
					html.append("</tr></tbody></table></div>");
				}

				html.append("<div class='team-stats'>Bakılan maç sayısı: Rekabet - " + rekabet + " | Ev sahibi - "
						+ home + " | Deplasman - " + away + "</div>");

				// History
//				html.append("<div class='history'>");
//
//				html.append("<div class='history-section'>");
//				html.append("<strong>").append(teamHistory.getTeamName()).append("</strong>");
//				html.append("</div>");
//
//				// Rekabet Geçmişi
//				if (!teamHistory.getRekabetGecmisi().isEmpty()) {
//					html.append("<div class='history-section'>");
//					html.append("<h5>Rekabet Geçmişi</h5>");
//					for (MatchResult mr : teamHistory.getRekabetGecmisi()) {
//						String cls = getResultClass(mr, teamHistory.getTeamName());
//						html.append("<div class='match-result ").append(cls).append("'>").append(mr.getMatchDate())
//								.append(" - ").append(mr.getHomeTeam()).append(" ").append(mr.getScoreString())
//								.append(" ").append(mr.getAwayTeam()).append("</div>");
//					}
//					html.append("</div>");
//				}
//
//				// Ev Sahibi Son Maçlar
//				if (!teamHistory.getSonMaclarHome().isEmpty()) {
//					html.append("<div class='history-section'>");
//					html.append("<h5>Ev Sahibi Son Maçlar</h5>");
//					for (MatchResult mr : teamHistory.getSonMaclarHome()) {
//						String cls = getResultClass(mr, teamHistory.getTeamName());
//						html.append("<div class='match-result ").append(cls).append("'>").append(mr.getMatchDate())
//								.append(" - ").append(mr.getHomeTeam()).append(" ").append(mr.getScoreString())
//								.append(" ").append(mr.getAwayTeam()).append("</div>");
//					}
//					html.append("</div>");
//				}
//
//				// Deplasman Son Maçlar
//				if (!teamHistory.getSonMaclarAway().isEmpty()) {
//					html.append("<div class='history-section'>");
//					html.append("<h5>Deplasman Son Maçlar</h5>");
//					for (MatchResult mr : teamHistory.getSonMaclarAway()) {
//						String cls = getResultClass(mr, teamHistory.getTeamName());
//						html.append("<div class='match-result ").append(cls).append("'>").append(mr.getMatchDate())
//								.append(" - ").append(mr.getHomeTeam()).append(" ").append(mr.getScoreString())
//								.append(" ").append(mr.getAwayTeam()).append("</div>");
//					}
//					html.append("</div>");
//				}
//				html.append("</div>");
				processedTeamCount++;
			} else {
				html.append("<div class='no-data'>Bu maç için geçmiş veri bulunamadı</div>");
			}

			html.append("</div>");
		}
		// Final istatistikleri
		html.append("<div class='stats'>");
		html.append("<h3>Final İstatistikleri</h3>");
		html.append("<p>- Toplam maç: ").append(matches.size()).append("</p>");
		html.append("<p>- Detay URL'si olan: ").append(detailUrlCount).append("</p>");
		html.append("<p>- Başarıyla geçmişi çekilen: ").append(processedTeamCount).append("</p>");
		html.append("<p>- Toplam takım: ").append(historyManager.getTotalTeams()).append("</p>");
		html.append("<p>- Başarı oranı: ").append(
				detailUrlCount > 0 ? String.format("%.1f%%", (processedTeamCount * 100.0 / detailUrlCount)) : "0%")
				.append("</p>");
		html.append("</div>");

		html.append("<p style='text-align: center; color: #666; margin-top: 30px;'>");
		html.append("Bu veriler otomatik olarak çekilmiştir - Son güncelleme: ")
				.append(LocalDateTime.now(istanbulZone));
		html.append("</p>");

		html.append("<script>");
		html.append("function toggleHistory(button) {");
		html.append("  const matchDiv = button.closest('.match');");
		html.append("  const historySections = matchDiv.querySelectorAll('.history .history-section');");
		html.append("  let isHidden = historySections[0].style.display === 'none';");
		html.append("  historySections.forEach(section => {");
		html.append("    section.style.display = isHidden ? 'block' : 'none';");
		html.append("  });");
		html.append("  button.textContent = isHidden ? 'Gizle' : 'Göster';");
		html.append("}");
		html.append("document.querySelectorAll('.history .history-section').forEach(s => s.style.display = 'none');");
		html.append("</script>");
		html.append("</body></html>");

		File dir = new File("public");
		if (!dir.exists())
			dir.mkdirs();
		try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
			fw.write(html.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// === HAZIR KUPON (SUBLIST) RAPOR ===
	public static void generateHtmlForSublist(List<LastPrediction> predictions, List<PredictionData> predictionData,
			String fileName) {
		StringBuilder html = new StringBuilder();

		html.append("<!DOCTYPE html><html lang='tr'><head><meta charset='UTF-8'>");
		html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
		html.append("<title>💰 Basketbol Kuponu</title>");
		html.append("<style>");
		html.append("body{font-family:Arial,sans-serif;background:#f7f8fa;margin:0;padding:20px;color:#222}");
		html.append("h1{text-align:center;color:#333;font-size:22px;margin-bottom:20px}");
		html.append("table{width:100%;border-collapse:collapse;background:#fff;border-radius:8px;"
				+ "box-shadow:0 2px 8px rgba(0,0,0,0.1);}");
		html.append("th,td{padding:10px 12px;text-align:center;border-bottom:1px solid #ddd}");
		html.append("th{background:#0077cc;color:#fff;font-size:15px}");
		html.append("tr:nth-child(even){background:#f3f6fa}");
		html.append("tr:hover{background:#eaf3ff}");
		html.append(".status-icon{font-size:1.2em}");
		html.append(".won{color:#28a745}.lost{color:#dc3545}.pending{color:#999}");
		html.append("</style></head><body>");
		html.append("<h1>💰 Basketbol Kuponu</h1>");
		html.append("<p style='text-align:center;color:#555;'>Sistemin oluşturduğu öneri kuponu</p>");
		html.append("<table><thead><tr>");
		html.append(
				"<th>🕒 Saat</th><th>🏀 Maç</th><th>🎯 Tahmin</th><th>📊 Skor Tahmini</th><th>📈 Gerçek Skor</th><th>Durum</th>");
		html.append("</tr></thead><tbody>");

		for (int i = 0; i < predictions.size(); i++) {
			LastPrediction p = predictions.get(i);
			PredictionData d = (predictionData != null && i < predictionData.size()) ? predictionData.get(i) : null;

			String actualScore = (d != null && d.getScore() != null) ? d.getScore() : "-";

			StringBuilder statusIcons = new StringBuilder();
			if (d != null && p.getPredictions() != null) {
				for (String pick : p.getPredictions()) {
					String st = d.getStatuses() != null ? d.getStatuses().getOrDefault(pick, "pending") : "pending";
					switch (st) {
					case "won" -> statusIcons.append("<span class='status-icon won'>✅</span>");
					case "lost" -> statusIcons.append("<span class='status-icon lost'>❌</span>");
					default -> statusIcons.append("<span class='status-icon pending'>⏳</span>");
					}
				}
			} else
				statusIcons.append("<span class='status-icon pending'>⏳</span>");

			html.append("<tr>");
			html.append("<td>").append(p.getTime()).append("</td>");
			html.append("<td>").append(p.getName()).append("</td>");
			html.append("<td>").append(p.preditionsToString()).append("</td>");
			html.append("<td>").append(p.getScore() != null ? p.getScore() : "-").append("</td>");
			html.append("<td>").append(actualScore).append("</td>");
			html.append("<td>").append(statusIcons).append("</td>");
			html.append("</tr>");
		}

		html.append("</tbody></table></body></html>");

		File dir = new File("public");
		if (!dir.exists())
			dir.mkdirs();

		try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
			fw.write(html.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getResultClass(MatchResult match, String teamName) {
		String result = match.getResult();
		boolean isHome = teamName.contains(match.getHomeTeam());
		if (result.equals("D"))
			return "draw";
		else if ((isHome && result.equals("H")) || (!isHome && result.equals("A")))
			return "win";
		else
			return "loss";
	}

	private static String getRealScore(List<RealScores> rsList, String home, String away) {
		String score = String.format("(%s)", "⏳");

		int count = 0;

		for (RealScores rs : rsList) {
			if (home.equals(rs.getHomeTeam()) && away.equals(rs.getAwayTeam())) {
				score = String.format(" (%s)", rs.getScore());
				count = 1;
				break;
			}

			if (home.equals(rs.getHomeTeam()) || away.equals(rs.getAwayTeam())) {
				score = String.format(" (%s)", rs.getScore());
				count++;
			}
		}

		if (count != 1) {
			score = String.format(" (%s)", "⏳");
		}

		return score;
	}
}
