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

		StringBuilder html = new StringBuilder();

		html.append("<style>");
		html.append(
				".match{background:#fff;border:1px solid #dce3ec;margin:18px 0;padding:18px;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,0.08);}");
		html.append(
				".match-header{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;margin-bottom:10px;}");
		html.append(".match-name{font-weight:700;color:#003366;font-size:1.3em;}");
		html.append(".match-time{color:#004d80;font-size:1.1em;font-weight:600;}");

		html.append(
				".odds-mini{background:#f8fafc;border:1px solid #dbe2ea;padding:12px;border-radius:10px;margin:14px 0;}");
		html.append(".odds-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:12px;}");

		html.append(
				".odds-cell{background:#fff;border:1px solid #ccd6e0;border-radius:8px;padding:8px;text-align:center;}");
		html.append(".odds-pct{color:#555;font-size:0.85em;}");
		html.append(".odds-label{font-weight:700;color:#004d80;}");

		html.append(".quick-summary{overflow-x:auto;margin-top:10px;}");
		html.append(
				".quick-summary table{min-width:650px;border-collapse:collapse;width:100%;background:#fff;border:1px solid #dce3ec;}");
		html.append(".quick-summary th,.quick-summary td{padding:8px;border:1px solid #e1e7ef;text-align:center;}");

		html.append(
				"@media(max-width:600px){.odds-grid{grid-template-columns:repeat(2,1fr);} .match-name{font-size:1.1em;}}");
		html.append("</style>");

		html.append("<div>");

		for (int i = 0; i < matches.size(); i++) {
			MatchInfo m = matches.get(i);
			TeamMatchHistory h = historyManager.getTeamHistories().get(i);

			html.append("<div class='match'>");

			html.append("<div class='match-header'>");
			html.append("<div class='match-name'>").append(m.getName()).append("</div>");
			html.append("<div class='match-time'>").append(m.getTime()).append("</div>");
			html.append("</div>");

			// Odds bölümü
			html.append("<div class='odds-mini'>");
			html.append("<div class='odds-grid'>");

			html.append("<div class='odds-cell'><span class='odds-label'>MS1:</span> ").append(m.getOdds().getMs1())
					.append("<div class='odds-pct'>").append(MathUtils.fmtPct(h.getMs1())).append("</div></div>");

			html.append("<div class='odds-cell'><span class='odds-label'>MS2:</span> ").append(m.getOdds().getMs2())
					.append("<div class='odds-pct'>").append(MathUtils.fmtPct(h.getMs2())).append("</div></div>");

			html.append("</div></div>");

			// Summary tablosu
			html.append("<div class='quick-summary'>");
			html.append("<table><tr>");
			html.append("<th>MS1</th><th>MS2</th><th>Alt</th><th>Üst</th><th>Tahmin</th>");
			html.append("</tr><tr>");
			html.append("<td>").append(MathUtils.fmtPct(h.getMs1())).append("</td>");
			html.append("<td>").append(MathUtils.fmtPct(h.getMs2())).append("</td>");
			html.append("<td>").append(MathUtils.fmtPct(h.getAlt())).append("</td>");
			html.append("<td>").append(MathUtils.fmtPct(h.getUst())).append("</td>");
			html.append("<td>").append(results.get(i).getPick()).append("</td>");
			html.append("</tr></table>");
			html.append("</div>");

			html.append("</div>");
		}

		html.append("</div>");

		writeToFile("public/" + fileName, html.toString());
	}

	// === SUBLIST (Kupon) ===
	public static void generateHtmlForSublist(List<LastPrediction> predictions, List<PredictionData> predictionData,
			String fileName) {

		StringBuilder html = new StringBuilder();

		html.append("<style>");
		html.append("table{border-collapse:collapse;width:100%;min-width:650px;background:#fff;}");
		html.append("th,td{padding:10px;border:1px solid #ddd;text-align:center;font-size:0.9rem;}");
		html.append("th{background:#0077cc;color:white;}");
		html.append("tr:nth-child(even){background:#f3f6fa;}");
		html.append(".table-wrapper{overflow-x:auto;}");
		html.append("</style>");

		html.append("<div class='table-wrapper'><table>");
		html.append("<tr><th>Saat</th><th>Maç</th><th>Tahmin</th><th>Skor</th><th>Durum</th></tr>");

		for (LastPrediction p : predictions) {
			html.append("<tr>");
			html.append("<td>").append(p.getTime()).append("</td>");
			html.append("<td>").append(p.getName()).append("</td>");
			html.append("<td>").append(p.preditionsToString()).append("</td>");
			html.append("<td>").append(p.getScore()).append("</td>");
			html.append("<td>-</td>");
			html.append("</tr>");
		}

		html.append("</table></div>");

		writeToFile("public/" + fileName, html.toString());
	}

	private static void writeToFile(String path, String value) {
		try {
			File f = new File(path);
			f.getParentFile().mkdirs();
			try (FileWriter fw = new FileWriter(f)) {
				fw.write(value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
