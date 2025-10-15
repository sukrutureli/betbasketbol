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
			String url = "https://www.nesine.com/iddaa/basketbol?et=2&dt=" + date + "&le=2&ocg=MS&gt=Popüler";
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 20);
			scrollToEnd();

			List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
			System.out.println("Final element sayısı: " + events.size());

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
		int stable = 0, prev = -1;
		while (stable < 3) {
			List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
			int size = events.size();
			js.executeScript("window.scrollBy(0,1500)");
			Thread.sleep(800);
			stable = (size == prev) ? stable + 1 : 0;
			prev = size;
		}
	}

	private MatchInfo extractMatchInfo(WebElement event, int i) {
		try {
			js.executeScript("arguments[0].scrollIntoView({block:'center'});", event);
			Thread.sleep(100);
			String name = "?", url = null;

			List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name a"));
			if (!nameLinks.isEmpty()) {
				name = nameLinks.get(0).getText().trim();
				url = nameLinks.get(0).getAttribute("href");
			}

			if (url == null) {
				for (WebElement a : event.findElements(By.tagName("a"))) {
					String href = a.getAttribute("href");
					if (href != null && href.contains("istatistik.nesine.com")) {
						url = href;
						break;
					}
				}
			}

			String time = extractMatchTime(event);
			Odds odds = extractOdds(event);
			return new MatchInfo(name, time, url, odds, i);
		} catch (Exception e) {
			System.out.println("extractMatchInfo hata: " + e.getMessage());
			return null;
		}
	}

	private String extractMatchTime(WebElement e) {
		try {
			List<WebElement> spans = e.findElements(By.cssSelector("div.time span"));
			return spans.isEmpty() ? "?" : spans.get(0).getText().trim();
		} catch (Exception ex) {
			return "?";
		}
	}

	private Odds extractOdds(WebElement event) {
		String[] o = { "-", "-", "-", "-", "-", "-", "-", "-", "-" };
		try {
			List<WebElement> main = event.findElements(By.cssSelector("dd.col-02.event-row .cell"));
			for (int i = 0; i < main.size() && i < 2; i++)
				o[i] = main.get(i).findElement(By.cssSelector(".odd")).getText().trim();

			List<WebElement> extra = event.findElements(By.cssSelector("dd.col-04.event-row .cell"));
			for (int i = 0; i < extra.size() && i < 4; i++)
				o[2 + i] = extra.get(i).findElement(By.cssSelector(".odd")).getText().trim();

			List<WebElement> overUnder = event.findElements(By.cssSelector("dd.col-03.event-row .cell"));
			for (int i = 0; i < overUnder.size() && i < 3; i++)
				o[6 + i] = overUnder.get(i).findElement(By.cssSelector(".odd")).getText().trim();

		} catch (Exception e) {
			System.out.println("Oran hatası: " + e.getMessage());
		}
		return new Odds(toDouble(o[0]), toDouble(o[1]), toDouble(o[2]), toDouble(o[3]), toDouble(o[4]), toDouble(o[5]),
				toDouble(o[6]), toDouble(o[7]), toDouble(o[8]));
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
			PageWaitUtils.safeWaitForLoad(driver, 10);
			selectTournament();

			try {
				wait.until(ExpectedConditions
						.presenceOfElementLocated(By.cssSelector("div[data-test-id='CompitionHistoryTable']")));
			} catch (Exception e) {
				System.out.println("Rekabet geçmişi tablosu yok");
				return list;
			}

			list = extractCompetitionHistoryResults("rekabet", url);
		} catch (Exception e) {
			System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> scrapeSonMaclar(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 10);
			selectTournament();
			String sel = (side == 1) ? "div[data-test-id='LastMatchesTableFirst'] tbody tr"
					: "div[data-test-id='LastMatchesTableSecond'] tbody tr";

			try {
				wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sel)));
			} catch (Exception e) {
				System.out.println("Son maçlar tablosu yok: " + ((side == 1) ? "EvSahibi" : "Deplasman"));
				return list;
			}

			list = extractMatchResults("sonmaclar", url, side);
		} catch (Exception e) {
			System.out.println("Son maç hatası: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> extractCompetitionHistoryResults(String type, String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			List<WebElement> rows = driver
					.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));
			for (WebElement r : rows) {
				try {
					String league = safeText(r, "[data-test-id='CompitionTableItemLeague']");
					String date = safeText(r, "[data-test-id='CompitionTableItemSeason']");
					String home = safeText(r, "div[data-test-id='HomeTeam']");
					String away = safeText(r, "div[data-test-id='AwayTeam']");
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(home, away, sc[0], sc[1], date, league, type));
				} catch (Exception ex) {
					System.out.println("Rekabet satırı hatası: " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractCompetitionHistoryResults hata: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> extractMatchResults(String type, String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		String sel = (side == 1) ? "div[data-test-id='LastMatchesTableFirst'] tbody tr"
				: "div[data-test-id='LastMatchesTableSecond'] tbody tr";
		try {
			List<WebElement> rows = driver.findElements(By.cssSelector(sel));
			for (WebElement r : rows) {
				try {
					String league = safeText(r, "td[data-test-id='TableBodyLeague']");
					String home = safeText(r, "div[data-test-id='HomeTeam']");
					String away = safeText(r, "div[data-test-id='AwayTeam']");
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(home, away, sc[0], sc[1], league, "", type));
				} catch (Exception ex) {
					System.out.println("Son maç satırı hatası: " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractMatchResults hata: " + e.getMessage());
		}
		return list;
	}

	private String extractScore(WebElement r) {
		try {
			for (WebElement s : r.findElements(By.cssSelector("button[data-test-id='NsnButton'] span"))) {
				String t = s.getText().replaceAll("\\(.*?\\)", "").trim();
				if (t.matches("\\d+\\s*-\\s*\\d+"))
					return t;
			}
		} catch (Exception e) {
			return "-";
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
			PageWaitUtils.safeClick(driver, By.cssSelector("div[data-test-id='CustomDropdown']"), 5);
			wait.until(ExpectedConditions.visibilityOfElementLocated(
					By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")));
			driver.findElement(By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")).click();
			Thread.sleep(300);
		} catch (Exception e) {
			System.out.println("Turnuva seçimi atlandı");
		}
	}

	public void close() {
		try {
			driver.quit();
		} catch (Exception ignore) {
		}
	}

	private String[] extractTeamsFromHeader(String url) {
		String home = "-";
		String away = "-";
		String name = "";
		try {
			driver.get(url);
			try {
				wait.until(ExpectedConditions
						.visibilityOfElementLocated(By.cssSelector("div[data-test-id='HeaderTeams']")));
			} catch (Exception e) {
				System.out.println("Takım adları çekilemedi.");
			}

			WebElement header = driver.findElement(By.cssSelector("div[data-test-id='HeaderTeams']"));
			List<WebElement> teams = header
					.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"));

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
