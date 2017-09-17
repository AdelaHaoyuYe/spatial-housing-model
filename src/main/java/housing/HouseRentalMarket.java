package housing;

/***********************************************
 * Class that represents the market for houses for rent.
 * 
 * @author daniel
 *
 **********************************************/
public class HouseRentalMarket extends HousingMarket {
	private static final long serialVersionUID = -3039057421808432696L;

	private Config      config = Model.config; // Passes the Model's configuration parameters object to a private field
	private Region      region;

	public HouseRentalMarket(Region region) {
		this.region = region;
		for(int i=0; i< config.N_QUALITY; ++i) {
			monthsOnMarket[i] = 1.0;			
		}
		recalculateExpectedGrossYield();
		averageSoldGrossYield = config.RENT_GROSS_YIELD;
		longTermAverageGrossYield = config.RENT_GROSS_YIELD;
	}
	
	@Override
	public void completeTransaction(HouseBuyerRecord purchase, HouseSaleRecord sale) {
		super.completeTransaction(purchase, sale);
		monthsOnMarket[sale.house.getQuality()] = config.derivedParams.E*monthsOnMarket[sale.house.getQuality()] + (1.0-config.derivedParams.E)*(Model.getTime() - sale.tInitialListing);
		sale.house.rentalRecord = null;
		purchase.buyer.completeHouseRental(sale);
		sale.house.owner.completeHouseLet(sale);
		region.regionalRentalMarketStats.recordSale(purchase, sale);
		double yield = sale.getPrice()*config.constants.MONTHS_IN_YEAR/region.houseSaleMarket.getAverageSalePrice(sale.house.getQuality());
		averageSoldGrossYield = averageSoldGrossYield*config.derivedParams.K + (1.0-config.derivedParams.K)*yield;
		longTermAverageGrossYield = longTermAverageGrossYield*config.derivedParams.KL + (1.0-config.derivedParams.KL)*yield;
	}
	
	public HouseSaleRecord offer(House house, double price) {
//		if(house.resident != null) {
//			System.out.println("Got offer on rental market of house with resident");
//		}
		if(house.isOnMarket()) {
			System.out.println("Got offer on rental market of house already on sale market");			
		}
		HouseSaleRecord hsr = super.offer(house, price);
		house.putForRent(hsr);
		return(hsr);
	}
	
	@Override
	public void removeOffer(HouseSaleRecord hsr) {
		super.removeOffer(hsr);
		hsr.house.resetRentalRecord();
	}
	
	/***
	 * @param quality Quality of the house
	 * @return Expected fraction of time that the house will be occupied, based on
	 *         the average tenant stay in months (18 months according to ARLA figures) and the average number
	 *         of months on the rental market of a house of this quality.
	 */
	public double expectedOccupancy(int quality) {
		return(config.AVERAGE_TENANCY_LENGTH/(config.AVERAGE_TENANCY_LENGTH + monthsOnMarket[quality]));
	}

	public double getExpectedGrossYield(int quality) {
		return expectedGrossYield[quality];
	}

	
	protected void recalculateExpectedGrossYield() {
//		bestGrossYield = 0.0;
		for(int q=0; q < config.N_QUALITY; ++q) {
			expectedGrossYield[q] = getAverageSalePrice(q)*config.constants.MONTHS_IN_YEAR*expectedOccupancy(q)/region.houseSaleMarket.getAverageSalePrice(q);
//			if(expectedGrossYield[q] > bestGrossYield) bestGrossYield = expectedGrossYield[q];
		}		
	}

	//	@Override
//	protected void recordMarketStats() {
//		super.recordMarketStats();
//		recalculateExpectedGrossYield();
//	}
	
	@Override
	protected void recordMarketStats() {
		super.recordMarketStats();
		recalculateExpectedGrossYield();
	}

	
	public double monthsOnMarket[] = new double[config.N_QUALITY];
	public double expectedGrossYield[] = new double[config.N_QUALITY];
	public double averageSoldGrossYield;
	public double longTermAverageGrossYield; // averaged over a long time
//	public double bestGrossYield;
}
