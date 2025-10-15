package com.basketbol.model;

public class BasketballStats {
	private Double avgPointsFor;
	private Double avgPointsAgainst;
	private Double avgTotalPoints;
	private Double ppg;
	
	private Double avgPossessions;
	private Double avgOffensiveRating;
	private Double avgDefensiveRating;
	private Double rating100;
	
	public BasketballStats(Double avgPointsFor, Double avgPointsAgainst, Double avgTotalPoints, Double ppg) {
		this.avgPointsFor = avgPointsFor;
		this.avgPointsAgainst = avgPointsAgainst;
		this.avgTotalPoints = avgTotalPoints;
		
		calculateDerivedRatings();
	}

	public Double getAvgPointsFor() {
		return avgPointsFor;
	}

	public void setAvgPointsFor(Double avgPointsFor) {
		this.avgPointsFor = avgPointsFor;
	}

	public Double getAvgPointsAgainst() {
		return avgPointsAgainst;
	}

	public void setAvgPointsAgainst(Double avgPointsAgainst) {
		this.avgPointsAgainst = avgPointsAgainst;
	}

	public Double getAvgTotalPoints() {
		return avgTotalPoints;
	}

	public void setAvgTotalPoints(Double avgTotalPoints) {
		this.avgTotalPoints = avgTotalPoints;
	}
	
	public Double getAvgPossessions() {
		return avgPossessions;
	}

	public Double getAvgOffensiveRating() {
		return avgOffensiveRating;
	}

	public Double getAvgDefensiveRating() {
		return avgDefensiveRating;
	}

	public Double getRating100() {
		return rating100;
	}

	public Double getPpg() {
		return ppg;
	}

	/**
	 * Takım verisi eksik veya anlamsız mı? Eğer hem goller hem form hem rating
	 * sıfırsa 'boş' kabul edilir.
	 */
	public boolean isEmpty() {
		boolean noGoals = avgPointsFor == 0 && avgPointsAgainst == 0 && avgTotalPoints == 0;
		return noGoals;
	}
	
	private void calculateDerivedRatings() {
        if (avgPossessions <= 0)
            avgPossessions = (avgPointsFor + avgPointsAgainst) / 2.0;

        avgOffensiveRating = (avgPointsFor / avgPossessions) * 100.0;
        avgDefensiveRating = (avgPointsAgainst / avgPossessions) * 100.0;

        rating100 = clamp(100 + (avgOffensiveRating - avgDefensiveRating) / 3.0, 50, 150);
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
	
}
