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
import java.util.List;

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
			String date = LocalDate.now(ZoneId.of("Europe/Istanbul")).plusDays(1)
					.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
//			String date = LocalDate.now(ZoneId.of("Europe/Istanbul")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
			String url = "https://www.nesine.com/iddaa/basketbol?et=2&le=1&dt=" + date;

			driver.manage().deleteAllCookies();
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 25);

			scrollToEnd();

			wait.until(ExpectedConditions
					.presenceOfAllElementsLocatedBy(By.cssSelector("div[data-test-id^='r_'][data-sport-id='2']")));

			Thread.sleep(1000);
			List<WebElement> events = driver.findElements(By.cssSelector("div[data-test-id^='r_'][data-sport-id='2']"));

			System.out.println("🏀 Toplam basketbol maçı: " + events.size());

			for (int i = 0; i < events.size(); i++) {
				WebElement e = events.get(i);
				MatchInfo info = extractMatchInfo(e, i);
				if (info != null)
					list.add(info);
			}

		} catch (Exception e) {
			System.out.println("fetchMatches hata: " + e.getMessage());
		}
		return list;
	}

	private void scrollToEnd() throws InterruptedException {
		int stable = 0;
		int prev = -1;

		for (int i = 0; i < 8; i++) {
			js.executeScript("window.scrollBy(0, 2000)");
			Thread.sleep(1000);
		}

		while (stable < 3) {
			List<WebElement> events = driver.findElements(By.cssSelector("div[data-test-id^='r_'][data-sport-id='2']"));
			int size = events.size();

			js.executeScript("window.scrollBy(0, 2000)");
			Thread.sleep(1000);

			stable = (size == prev) ? stable + 1 : 0;
			prev = size;
		}

		for (int i = 0; i < 8; i++) {
			js.executeScript("window.scrollBy(0, -2000)");
			Thread.sleep(1000);
		}
	}

	private MatchInfo extractMatchInfo(WebElement event, int index) {
		try {
			js.executeScript("arguments[0].scrollIntoView({block:'center'});", event);
			Thread.sleep(100);

			WebElement link = event.findElement(By.cssSelector("a[data-test-id='matchName']"));
			String name = link.getText().trim();
			String url = link.getAttribute("href");

			String time = extractMatchTime(event);
			Odds odds = extractOdds(event);

			return new MatchInfo(name, time, url, odds, index);
		} catch (Exception e) {
			System.out.println("extractMatchInfo hata: " + e.getMessage());
			return null;
		}
	}

	private String extractMatchTime(WebElement e) {
		try {
			WebElement timeEl = e.findElement(By.cssSelector("span[data-testid^='time']"));
			return timeEl.getText().trim();
		} catch (Exception ex) {
			return "?";
		}
	}

	// =============================================================
	// ORANLARI AYIKLA (Basketbol)
	// =============================================================
	private Odds extractOdds(WebElement event) {
		try {
			String ms1 = getOdd(event, "odd_Maç Sonucu_1");
			String ms2 = getOdd(event, "odd_Maç Sonucu_2");

			String h1Baremi = getOdd(event, "odd_Handikaplı Maç Sonucu_H1");
			String h2Baremi = getOdd(event, "odd_Handikaplı Maç Sonucu_H2");
			String h1 = getOdd(event, "odd_Handikaplı Maç Sonucu_1");
			String h2 = getOdd(event, "odd_Handikaplı Maç Sonucu_2");

			String alt = getOdd(event, "odd_Alt/Üst_Alt");
			String ust = getOdd(event, "odd_Alt/Üst_Üst");
			String barem = getOdd(event, "odd_Alt/Üst_Limit");

			Odds o = new Odds(toDouble(ms1), toDouble(ms2), toDouble(h1Baremi), toDouble(h1), toDouble(h2),
					toDouble(h2Baremi), toDouble(alt), toDouble(barem), toDouble(ust));

			// Ek verileri Odds objene dilersen sonradan eklersin (barem, h1/h2 baremleri
			// vs.)
			return o;
		} catch (Exception e) {
			System.out.println("extractOdds hata: " + e.getMessage());
			return new Odds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private String getOdd(WebElement event, String testId) {
		try {
			WebElement el = event.findElement(By.cssSelector("button[data-testid='" + testId + "']"));
			return el.getText().trim();
		} catch (Exception e) {
			return "-";
		}
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
