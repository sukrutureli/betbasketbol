package com.basketbol.model;

public class BasketballStats {
	private Double avgPointsFor;
	private Double avgPointsAgainst;
	private Double avgTotalPoints;
	
	public BasketballStats(Double avgPointsFor, Double avgPointsAgainst, Double avgTotalPoints) {
		this.avgPointsFor = avgPointsFor;
		this.avgPointsAgainst = avgPointsAgainst;
		this.avgTotalPoints = avgTotalPoints;
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
	
	
	
}
