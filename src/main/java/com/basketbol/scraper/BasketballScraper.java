package com.basketbol.scraper;

import com.basketbol.model.MatchInfo;
import com.basketbol.model.MatchResult;
import com.basketbol.model.Odds;
import com.basketbol.model.TeamMatchHistory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BasketballScraper {
	private WebDriver driver;
	private JavascriptExecutor js;
	private WebDriverWait wait;

	public BasketballScraper() {
		setupDriver();
	}

	private void setupDriver() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-gpu");
		options.addArguments("--window-size=1920,1080");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-blink-features=AutomationControlled");
		options.addArguments("--disable-default-apps");
		options.addArguments("--disable-background-timer-throttling");
		options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

		options.setBinary("/usr/bin/google-chrome");
		options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
		options.setExperimentalOption("useAutomationExtension", false);

		this.driver = new ChromeDriver(options);
		this.js = (JavascriptExecutor) driver;
		this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
	}

	public List<MatchInfo> fetchMatches() {
		List<MatchInfo> list = new ArrayList<>();

		ZoneId turkeyZone = ZoneId.of("Europe/Istanbul");
		LocalDate today = LocalDate.now(turkeyZone);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		String todayStr = today.format(formatter);

		try {
			String url = "https://www.nesine.com/iddaa/basketbol?et=2&dt=" + todayStr + "&le=2&ocg=MS&gt=Popüler";
			driver.get(url);
			wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
			Thread.sleep(3000);
			performScrolling();

			int retries = 0;
			while (driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event")).isEmpty() && retries < 20) {
				Thread.sleep(500); // 0.5 saniye bekle
				retries++;
			}

			List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
			System.out.println("Final element sayısı: " + events.size());

			for (int idx = 0; idx < events.size(); idx++) {
				try {
					WebElement event = events.get(idx);
					MatchInfo matchInfo = extractMatchInfo(event, idx);

					if (matchInfo != null) {
						System.out.println(matchInfo.getName());
						list.add(matchInfo);
					}

				} catch (Exception e) {
					System.out.println("Element " + idx + " işlenirken hata: " + e.getMessage());
					continue;
				}
			}

		} catch (Exception e) {
			System.out.println("Ana sayfa scraping hatası: " + e.getMessage());
		}
		return list;
	}

	private void performScrolling() {
		try {
			int previousCount = -1;
			int stableRounds = 0;

			while (stableRounds < 3) {
				List<WebElement> matches = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
				int currentCount = matches.size();

				js.executeScript("window.scrollBy(0, 1500);");
				Thread.sleep(1500);

				if (currentCount == previousCount) {
					stableRounds++;
				} else {
					stableRounds = 0;
				}
				previousCount = currentCount;
			}
		} catch (Exception e) {
			System.out.println("Scroll işlemi hatası: " + e.getMessage());
		}
	}

	private MatchInfo extractMatchInfo(WebElement event, int idx) {
		try {
			// Lazy load tetikle
			try {
				js.executeScript("arguments[0].scrollIntoView({block:'center'});", event);
				Thread.sleep(200);
			} catch (Exception ignored) {
			}

			// Maç adı ve detay URL
			String matchName = "İsim bulunamadı";
			String detailUrl = null;

			try {
				List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name a"));
				if (!nameLinks.isEmpty()) {
					WebElement link = nameLinks.get(0);
					matchName = link.getText().trim();
					detailUrl = link.getAttribute("href");
				}
			} catch (Exception ignored) {
			}

			// Alternatif: istatistik linki
			if ((detailUrl == null || detailUrl.isEmpty())) {
				for (WebElement link : event.findElements(By.tagName("a"))) {
					String href = link.getAttribute("href");
					if (href != null && href.contains("istatistik.nesine.com")) {
						detailUrl = href;
						if (matchName.equals("İsim bulunamadı")) {
							String t = link.getText().trim();
							if (!t.isEmpty())
								matchName = t;
						}
						break;
					}
				}
			}

			// Zaman
			String matchTime = extractMatchTime(event);

			// Oranlar
			Odds odds = extractOdds(event);

			// Minimum veri kontrolü
			if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı")) {
				System.out.println("Element " + idx + " veri eksik, atlanıyor");
				return null;
			}

			return new MatchInfo(matchName, matchTime, detailUrl, odds, idx);

		} catch (Exception e) {
			System.out.println("Element " + idx + " extract hatası: " + e.getMessage());
			return null;
		}
	}

	private String extractMatchTime(WebElement event) {
		try {
			List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
			if (!timeList.isEmpty())
				return timeList.get(0).getText().trim();
			return "Zaman bulunamadı";
		} catch (Exception e) {
			return "Zaman hatası";
		}
	}

	private Odds extractOdds(WebElement event) {
		String[] odds = { "-", "-", "-", "-", "-", "-", "-", "-", "-" };

		try {
			List<WebElement> mainOdds = event.findElements(By.cssSelector("dd.col-02.event-row .cell"));
			for (int i = 0; i < 2; i++) {
				try {
					WebElement oddLink = mainOdds.get(i).findElement(By.cssSelector("a.odd"));
					String text = oddLink.getText().trim();
					odds[i] = text.isEmpty() ? "-" : text;
				} catch (IndexOutOfBoundsException | NoSuchElementException e) {
					odds[i] = "-";
				}
			}

			List<WebElement> extraOdds = event.findElements(By.cssSelector("dd.col-04.event-row .cell"));
			for (int i = 0; i < 4; i++) {
				try {
					WebElement oddLink = extraOdds.get(i).findElement(By.cssSelector(".odd"));
					String text = oddLink.getText().trim();
					odds[2 + i] = text.isEmpty() ? "-" : text;
				} catch (IndexOutOfBoundsException | NoSuchElementException e) {
					odds[2 + i] = "-";
				}
			}

			List<WebElement> overUnderOdds = event.findElements(By.cssSelector("dd.col-03.event-row .cell"));
			for (int i = 0; i < 3; i++) {
				try {
					WebElement oddLink = overUnderOdds.get(i).findElement(By.cssSelector(".odd"));
					String text = oddLink.getText().trim();
					odds[6 + i] = text.isEmpty() ? "-" : text;
				} catch (IndexOutOfBoundsException | NoSuchElementException e) {
					odds[6 + i] = "-";
				}
			}

		} catch (Exception e) {
			System.out.println("Oran çekme hatası: " + e.getMessage());
		}

		return new Odds(toDouble(odds[0]), toDouble(odds[1]), toDouble(odds[2]), toDouble(odds[3]), toDouble(odds[4]),
				toDouble(odds[5]), toDouble(odds[6]), toDouble(odds[7]), toDouble(odds[8]));
	}

	public Double toDouble(String oddInString) {
		oddInString = oddInString.replaceAll(",", ".");
		if (oddInString.equals("-")) {
			return 0.0;
		} else {
			return Double.valueOf(oddInString);
		}
	}

	public TeamMatchHistory scrapeTeamHistory(String detailUrl, String teamName, Odds odds) {
		if (detailUrl == null || detailUrl.isEmpty())
			return null;
		List<String> names = scrapeDetailUrl(detailUrl);
		TeamMatchHistory teamHistory = new TeamMatchHistory(names.get(0), names.get(1), names.get(2), detailUrl, odds);
		try {
			List<MatchResult> rekabetGecmisi = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
			rekabetGecmisi.forEach(teamHistory::addRekabetGecmisiMatch);

			List<MatchResult> sonMaclarHome = scrapeSonMaclar(detailUrl + "/son-maclari", 1);
			sonMaclarHome.forEach(m -> teamHistory.addSonMacMatch(m, 1));

			List<MatchResult> sonMaclarAway = scrapeSonMaclar(detailUrl + "/son-maclari", 2);
			sonMaclarAway.forEach(m -> teamHistory.addSonMacMatch(m, 2));

		} catch (Exception e) {
			System.out.println("Takım geçmişi çekme hatası: " + e.getMessage());
			return teamHistory;
		}
		return teamHistory;
	}

	private List<String> scrapeDetailUrl(String url) {
		List<String> names = new ArrayList<>();
		try {
			driver.get(url);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
			Thread.sleep(2000);

			int retries = 0;
			while (driver.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"))
					.isEmpty() && retries < 20) {
				Thread.sleep(500); // 0.5 saniye bekle
				retries++;
			}
			// Tüm takım linklerini bul
			List<WebElement> teamLinks = driver
					.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"));

			// İlk takım
			String homeTeam = teamLinks.size() > 0 ? teamLinks.get(0).getText().trim() : "-";

			// İkinci takım
			String awayTeam = teamLinks.size() > 1 ? teamLinks.get(1).getText().trim() : "-";

			String teamNameShort = homeTeam + " - " + awayTeam;

			names.add(teamNameShort);
			names.add(homeTeam);
			names.add(awayTeam);
		} catch (Exception e) {
			System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
		}
		return names;
	}

	private List<MatchResult> scrapeRekabetGecmisi(String url) {
		List<MatchResult> matches = new ArrayList<>();
		try {
			driver.get(url);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
			Thread.sleep(2000);

			List<WebElement> container = driver
					.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTable']"));
			if (!container.isEmpty()) {
				if (hasNoData(container.get(0))) {
					System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
					return matches;
				}
			}

			selectTournament();
			matches = extractCompetitionHistoryResults("rekabet-gecmisi", url);
		} catch (Exception e) {
			System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
			return matches;
		}
		return matches;
	}

	private List<MatchResult> extractCompetitionHistoryResults(String matchType, String originalUrl) {
		List<MatchResult> matches = new ArrayList<>();

		try {
			int retries = 0;
			while (driver.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']")).isEmpty()
					&& retries < 20) {
				Thread.sleep(500); // 0.5 saniye bekle
				retries++;
			}

			List<WebElement> rows = driver
					.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));

			for (WebElement row : rows) {
				try {
					String league = row.findElement(By.cssSelector("[data-test-id='CompitionTableItemLeague']"))
							.getText().trim();
					String dateText = row.findElement(By.cssSelector("[data-test-id='CompitionTableItemSeason']"))
							.getText().trim();
					String homeTeam = row.findElement(By.cssSelector("div[data-test-id='HomeTeam']")).getText().trim();
					String awayTeam = row.findElement(By.cssSelector("div[data-test-id='AwayTeam']")).getText().trim();
					String scoreText = extractScore(row);
					String[] parts = scoreText.split("-");
					int homeScore = Integer.parseInt(parts[0].trim());
					int awayScore = Integer.parseInt(parts[1].trim());

					MatchResult match = new MatchResult(homeTeam, awayTeam, homeScore, awayScore, dateText, league,
							matchType);
					matches.add(match);
				} catch (Exception e) {
					System.out.println("Satır işlenemedi: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractCompetitionHistoryResults hatası: " + e.getMessage());
		}
		return matches;
	}

	private List<MatchResult> scrapeSonMaclar(String url, int homeOrAway) {
		List<MatchResult> matches = new ArrayList<>();
		try {
			driver.get(url);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
			Thread.sleep(2000);

			String selectorString = "";
			if (homeOrAway == 1) {
				selectorString = "div[data-test-id='LastMatchesTableFirst'] table";
			} else if (homeOrAway == 2) {
				selectorString = "div[data-test-id='LastMatchesTableSecond'] table";
			}
			List<WebElement> container = driver.findElements(By.cssSelector(selectorString));
			if (!container.isEmpty()) {
				if (hasNoData(container.get(0))) {
					System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
					return matches;
				}
			}

			selectTournament();
			matches = extractMatchResults("son-maclari", url, homeOrAway);
		} catch (Exception e) {
			System.out.println("Son maçlar hatası: " + e.getMessage());
			return matches;
		}
		return matches;
	}

	private List<MatchResult> extractMatchResults(String matchType, String originalUrl, int homeOrAway) {
	    List<MatchResult> matches = new ArrayList<>();

	    String selectorString = (homeOrAway == 1)
	            ? "div[data-test-id='LastMatchesTableFirst'] div[data-test-id='LastMatchesTable'] table"
	            : "div[data-test-id='LastMatchesTableSecond'] div[data-test-id='LastMatchesTable'] table";

	    try {
	        int retries = 0;
	        while (driver.findElements(By.cssSelector(selectorString)).isEmpty() && retries < 20) {
	            Thread.sleep(500);
	            retries++;
	        }

	        List<WebElement> table = driver.findElements(By.cssSelector(selectorString));
	        if (table.isEmpty()) {
	            System.out.println("⚠️ Tablo bulunamadı: " + selectorString);
	            return matches;
	        }

	        if (hasNoData(table.get(0))) {
	            System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
	            return matches;
	        }

	        List<WebElement> rows = table.get(0).findElements(By.cssSelector("tbody tr"));
	        if (rows.isEmpty()) {
	            System.out.println("⚠️ Tablo satırları bulunamadı, muhtemelen boş sayfa.");
	            return matches;
	        }

	        for (WebElement row : rows) {
	            try {
	                String homeTeam = row.findElement(By.cssSelector("div[data-test-id='HomeTeam'] span")).getText().trim();
	                String awayTeam = row.findElement(By.cssSelector("div[data-test-id='AwayTeam'] span")).getText().trim();

	                String scoreText = extractScore(row);
	                int homeScore = -1, awayScore = -1;
	                if (!scoreText.equals("-")) {
	                    String[] parts = scoreText.split("-");
	                    if (parts.length == 2) {
	                        homeScore = Integer.parseInt(parts[0].trim());
	                        awayScore = Integer.parseInt(parts[1].trim());
	                    }
	                }

	                String leagueAndDate = row.findElement(By.cssSelector("td[data-test-id='TableBodyLeague']")).getText();

	                MatchResult match = new MatchResult(homeTeam, awayTeam, homeScore, awayScore, leagueAndDate, "", matchType);
	                matches.add(match);

	            } catch (Exception e) {
	                System.out.println("Satır işlenemedi: " + e.getMessage());
	            }
	        }

	    } catch (Exception e) {
	        System.out.println("extractMatchResults hatası: " + e.getMessage());
	    }

	    return matches;
	}

	private void selectTournament() {
		try {
			// Timeout'u 5 saniyeye düşür
			WebElement dropdown = new WebDriverWait(driver, Duration.ofSeconds(15)).until(
					ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-test-id='CustomDropdown']")));
			dropdown.click();
			Thread.sleep(1000); // 1000'den 300'e

			WebElement option = new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions
					.elementToBeClickable(By.xpath("//div[@role='option']//span[contains(text(), 'Bu Turnuva')]")));
			option.click();
			Thread.sleep(1000); // 1500'den 500'e
		} catch (Exception e) {
			// Hızla geç, takılma
			System.out.println("Turnuva seçimi atlandı");
		}
	}

	// Skor çekme metodu
	private String extractScore(WebElement row) {
		try {
			List<WebElement> spans = row.findElements(By.cssSelector("button[data-test-id='NsnButton'] span"));
			for (WebElement span : spans) {
				String txt = span.getText().trim();
				txt = txt.replaceAll("(\\(.*?\\))", "").trim(); // (H) engeller
				// Skor formatı "X - Y" (Ör. "2 - 1")
				if (txt.matches("\\d+\\s*-\\s*\\d+")) {
					return txt;
				}
			}
		} catch (Exception e) {
			System.out.println("Skor çekme hatası: " + e.getMessage());
		}
		return "-";
	}

	private boolean hasNoData(WebElement container) {
		try {
			List<WebElement> noDataElems = container.findElements(By.cssSelector("div[data-test-id='NoData']"));
			return !noDataElems.isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	public void close() {
		if (driver != null)
			driver.quit();
	}
}
