package com.basketbol.scraper;

import com.basketbol.model.MatchInfo;
import com.basketbol.model.MatchResult;
import com.basketbol.model.Odds;
import com.basketbol.model.TeamMatchHistory;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasketballScraper {

	private WebDriver driver;
	private JavascriptExecutor js;
	private WebDriverWait wait;

	public BasketballScraper() {
		setupDriver();
	}

	private void setupDriver() {
		System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
				"--window-size=1920,1080", "--disable-blink-features=AutomationControlled");
		driver = new ChromeDriver(options);
		js = (JavascriptExecutor) driver;
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	// =============================================================
	// ANA SAYFA MAÇLARINI ÇEK
	// =============================================================
	public List<MatchInfo> fetchMatches() {
		List<MatchInfo> list = new ArrayList<>();
		try {
			String date = LocalDate.now(ZoneId.of("Europe/Istanbul")).plusDays(0)
					.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
			String url = "https://www.nesine.com/iddaa/basketbol?et=2&dt=" + date + "&le=2&ocg=MS&gt=Pop%C3%BCler";

			driver.manage().deleteAllCookies();
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 25);
wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
    By.cssSelector("div.event-list.pre-event")
));
Thread.sleep(2000);
			
			List<Map<String, String>> raw = scrollAndCollectMatchData();
			System.out.println("🏀 Toplam basketbol maçı: " + raw.size());

			int index = 0;
			for (Map<String, String> d : raw) {
				Odds o = new Odds(toDouble(d.get("ms1")), toDouble(d.get("ms2")), toDouble(d.get("h1Value")),
						toDouble(d.get("h1")), toDouble(d.get("h2")), toDouble(d.get("h2Value")),
						toDouble(d.get("alt")), toDouble(d.get("barem")), toDouble(d.get("ust")));
				list.add(new MatchInfo(d.get("name"), d.get("time"), d.get("url"), o, index++));
			}

		} catch (Exception e) {
			System.out.println("fetchMatches hata: " + e.getMessage());
		}
		return list;
	}

	// =============================================================
	// SCROLL VE VERİ TOPLAMA
	// =============================================================
	private List<Map<String, String>> scrollAndCollectMatchData() throws InterruptedException {
		By eventSelector = By.cssSelector("div[id^='r_'].event-list[data-sport-id='2']");
		Set<String> seen = new HashSet<>();
		List<Map<String, String>> collected = new ArrayList<>();

		int stable = 0, prevCount = 0;
		int minScroll = 12;

		int waitTry = 0;
		while (driver.findElements(eventSelector).isEmpty() && waitTry < 10) {
			Thread.sleep(1000);
			waitTry++;
		}
		System.out.println("⏳ İlk basketbol maçları göründü (" + waitTry + "sn) sonra scroll başlıyor...");

		for (int i = 0; (i < 120 && stable < 8) || i < minScroll; i++) {
			List<WebElement> matches = driver.findElements(eventSelector);

			for (WebElement el : matches) {
				try {
					String name = el.findElement(By.cssSelector("div.name a")).getText().trim();
					if (!seen.contains(name) && !name.isEmpty()) {
						seen.add(name);
						Map<String, String> map = new HashMap<>();
						map.put("name", name);

						String href = el.findElement(By.cssSelector("div.name a")).getAttribute("href");
						if (href == null || href.contains("javascript:void") || href.isEmpty()) {
							// canlı maç veya geçersiz link
							continue;
						}
						map.put("url", href);

						map.put("time", el.findElement(By.cssSelector("div.time span")).getText().trim());

						// 🎯 1-2 Maç Sonucu
						List<WebElement> ms = el.findElements(By.cssSelector("dd.col-02.event-row .cell"));
						if (ms.size() >= 2) {
							map.put("ms1", ms.get(0).getText());
							map.put("ms2", ms.get(1).getText());
						} else {
							map.put("ms1", "-");
							map.put("ms2", "-");
						}

						// 🎯 Handikaplı Oranlar (H1 - H2 - Barem)
						List<WebElement> hand = el.findElements(By.cssSelector("dd.col-04.event-row .cell"));
						if (hand.size() >= 4) {
							map.put("h1Value", hand.get(0).getText());
							map.put("h1", hand.get(1).getText());
							map.put("h2", hand.get(2).getText());
							map.put("h2Value", hand.get(3).getText());
						} else {
							map.put("h1", "-");
							map.put("h1Value", "-");
							map.put("h2", "-");
							map.put("h2Value", "-");
						}

						// 🎯 Alt / Üst
						List<WebElement> altust = el.findElements(By.cssSelector("dd.col-03.event-row .cell"));
						if (altust.size() >= 3) {
							map.put("alt", altust.get(0).getText());
							map.put("barem", altust.get(1).getText());
							map.put("ust", altust.get(2).getText());
						} else {
							map.put("alt", "-");
							map.put("ust", "-");
							map.put("barem", "-");
						}

						collected.add(map);
					}
				} catch (Exception ignore) {
				}
			}

			if (seen.size() == prevCount)
				stable++;
			else
				stable = 0;
			prevCount = seen.size();

			js.executeScript("window.scrollBy(0, 2500)");
			Thread.sleep(1200);
		}

		System.out.println("🧩 Toplanan benzersiz basket maç: " + seen.size());
		return collected;
	}

	private double toDouble(String s) {
		try {
			if (s == null || s.equals("-") || s.isEmpty())
				return 0.0;
			return Double.parseDouble(s.replace(",", "."));
		} catch (Exception e) {
			return 0.0;
		}
	}

	// =============================================================
	// TAKIM GEÇMİŞİ (REKABET + SON MAÇLAR)
	// =============================================================
	public TeamMatchHistory scrapeTeamHistory(String detailUrl, String name, Odds odds) {
		if (detailUrl == null || !detailUrl.startsWith("http"))
			return null;

		// String[] teams = scrapeDetailTeams(detailUrl, name);
		String[] teams = extractTeamsFromHeader(detailUrl);
		String homeTeam = teams[0];
		String awayTeam = teams[1];
		String title = teams[2]; // "Home - Away"

		TeamMatchHistory th = new TeamMatchHistory(title, homeTeam, awayTeam, detailUrl, odds);
		try {
			List<MatchResult> rekabet = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
			rekabet.forEach(th::addRekabetGecmisiMatch);

			List<MatchResult> sonHome = scrapeSonMaclar(detailUrl + "/son-maclari", 1);
			sonHome.forEach(m -> th.addSonMacMatch(m, 1));

			List<MatchResult> sonAway = scrapeSonMaclar(detailUrl + "/son-maclari", 2);
			sonAway.forEach(m -> th.addSonMacMatch(m, 2));
		} catch (Exception e) {
			System.out.println("scrapeTeamHistory hata: " + e.getMessage());
		}
		return th;
	}

	private List<MatchResult> scrapeRekabetGecmisi(String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 12);
			Thread.sleep(1000);

			selectTournament();

			try {
				wait.until(ExpectedConditions.or(
						ExpectedConditions
								.presenceOfElementLocated(By.cssSelector("div[data-test-id='CompitionHistoryTable']")),
						ExpectedConditions.presenceOfElementLocated(
								By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"))));
			} catch (Exception e) {
				System.out.println("⚠️ Basketbol rekabet geçmişi tablosu yok");
				return list;
			}

			Thread.sleep(800);
			list = extractCompetitionHistoryResults(url);
		} catch (Exception e) {
			System.out.println("⚠️ Basketbol rekabet geçmişi hatası: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> scrapeSonMaclar(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 12);
			Thread.sleep(1000);

			selectTournament();

			String sel = (side == 1)
					? "div[data-test-id^='LastMatchesTable'][data-test-id*='First'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Home'] tbody tr"
					: "div[data-test-id^='LastMatchesTable'][data-test-id*='Second'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Away'] tbody tr";

			try {
				wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sel)));
			} catch (Exception e) {
				System.out.println("⚠️ Son maçlar tablosu yok: " + ((side == 1) ? "Ev Sahibi" : "Deplasman"));
				return list;
			}

			Thread.sleep(800);
			list = extractMatchResults(url, side);
		} catch (Exception e) {
			System.out.println("⚠️ Basketbol son maç hatası: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> extractCompetitionHistoryResults(String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			List<WebElement> rows = driver
					.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));
			System.out.println("🔹 Rekabet geçmişi satır sayısı: " + rows.size());
			for (WebElement r : rows) {
				try {
					String league = safeText(r, "[data-test-id='CompitionTableItemLeague']");
					String date = safeText(r, "[data-test-id='CompitionTableItemSeason']");
					String home = safeText(r, "div[data-test-id='HomeTeam']");
					String away = safeText(r, "div[data-test-id='AwayTeam']");
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(home, away, sc[0], sc[1], date, league, "rekabet-gecmisi"));
				} catch (Exception ex) {
					System.out.println("⚠️ Rekabet satırı hatası: " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractCompetitionHistoryResults hata: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> extractMatchResults(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		String sel = (side == 1)
				? "div[data-test-id^='LastMatchesTable'][data-test-id*='First'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Home'] tbody tr"
				: "div[data-test-id^='LastMatchesTable'][data-test-id*='Second'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Away'] tbody tr";

		try {
			List<WebElement> rows = driver.findElements(By.cssSelector(sel));
			System.out.println("🔹 Son maç (" + (side == 1 ? "Ev" : "Dep") + ") satır sayısı: " + rows.size());
			for (WebElement r : rows) {
				try {
					String league = safeText(r, "td[data-test-id='TableBodyLeague']");
					String home = safeText(r, "div[data-test-id='HomeTeam']");
					String away = safeText(r, "div[data-test-id='AwayTeam']");
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(home, away, sc[0], sc[1], league, "", "son-maclari"));
				} catch (Exception ex) {
					System.out.println("⚠️ Son maç satırı hatası: " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractMatchResults hata: " + e.getMessage());
		}
		return list;
	}

	private String extractScore(WebElement r) {
		try {
			List<WebElement> scoreEls = r
					.findElements(By.cssSelector("div[data-test-id='Score'], button[data-test-id='NsnButton'] span"));
			for (WebElement s : scoreEls) {
				String t = s.getText().replaceAll("\\(.*?\\)", "").trim();
				if (t.matches("\\d+\\s*-\\s*\\d+"))
					return t;
			}
		} catch (Exception e) {
			// ignore
		}
		return "-";
	}

	private int[] parseScore(String s) {
		try {
			String[] p = s.split("-");
			return new int[] { Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()) };
		} catch (Exception e) {
			return new int[] { -1, -1 };
		}
	}

	private String safeText(WebElement parent, String css) {
		try {
			return parent.findElement(By.cssSelector(css)).getText().trim();
		} catch (Exception e) {
			return "-";
		}
	}

	private void selectTournament() {
		try {
			WebElement dropdown = wait.until(
					ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-test-id='CustomDropdown']")));
			dropdown.click();
			wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")))
					.click();
			Thread.sleep(500);
		} catch (Exception e) {
			System.out.println("Turnuva seçimi atlandı veya zaten seçili.");
		}
	}

	public void close() {
		try {
			driver.quit();
		} catch (Exception ignore) {
		}
	}

	private String[] extractTeamsFromHeader(String url) {
		String home = "-", away = "-", name = "";
		try {
			driver.get(url);
			PageWaitUtils.waitForPageLoad(driver, 12);

			try {
				wait.until(ExpectedConditions
						.visibilityOfElementLocated(By.cssSelector("div[data-test-id='HeaderTeams']")));
			} catch (Exception e) {
				System.out.println("Takım adları çekilemedi.");
			}

			WebElement header = driver.findElement(By.cssSelector("div[data-test-id='HeaderTeams']"));
			List<WebElement> teams = header.findElements(By.cssSelector(
					"a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams'], a[data-test-id='TeamLink'] div[data-test-id='HeaderTeams']"));

			if (teams.size() >= 2) {
				home = teams.get(0).getText().trim();
				away = teams.get(1).getText().trim();
			}

		} catch (Exception e) {
			System.out.println("Takım adları çekilemedi: " + e.getMessage());
		}
		name = home + " - " + away;
		return new String[] { home, away, name };
	}

}



