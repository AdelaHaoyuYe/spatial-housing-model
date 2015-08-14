package housing;

import org.apache.commons.math3.distribution.LogNormalDistribution;

import ec.util.MersenneTwisterFast;

public class HouseholdBehaviour {// implements IHouseholdBehaviour {
	public double DOWNPAYMENT_FRACTION = 0.1 + 0.0025*Model.rand.nextGaussian(); // Fraction of bank-balance household would like to spend on mortgage downpayments
	protected MersenneTwisterFast 	rand = Model.rand;
	public int						desiredBTLProperties;	// number of properties

	
	public HouseholdBehaviour(double incomePercentile) {
		if( incomePercentile > 0.5 && rand.nextDouble() < data.Households.P_INVESTOR*2.0) {
			desiredBTLProperties = (int)data.Households.buyToLetDistribution.inverseCumulativeProbability(rand.nextDouble());
		} else {
			desiredBTLProperties = 0;
		}
	}
	///////////////////////////////////////////////////////////////////////////////////////////////
	// Owner-Ocupier behaviour
	///////////////////////////////////////////////////////////////////////////////////////////////

	/********************************
	 * How much a household consumes
	 * Consumption rule made to fit ONS wealth in Great Britain data.
	 * @author daniel
	 *
	 ********************************/
	public double desiredConsumptionB(double monthlyIncome, double bankBalance) {
		return(0.1*Math.max((bankBalance - Math.exp(4.07*Math.log(monthlyIncome*12.0)-33.1 + 0.2*Model.rand.nextGaussian())),0.0));
	}

	/***************************
	 * Decide on desired purchase price as a function of monthly income and current
	 *  of house price appreciation.
	 ****************************/
	public double desiredPurchasePrice(double monthlyIncome, double hpa) {
		final double A = 0.0;//0.48;			// sensitivity to house price appreciation
		final double EPSILON = 0.36;//0.36;//0.48;//0.365; // S.D. of noise
		final double SIGMA = 5.6*12.0;//5.6;	// scale
		return(SIGMA*monthlyIncome*Math.exp(EPSILON*Model.rand.nextGaussian())/(1.0 - A*hpa));
	}

	/********************************
	 * @param pbar average sale price of houses of the same quality
	 * @param d average number of days on the market before sale
	 * @param principal amount of principal left on any mortgage on this house
	 * @return initial sale price of a house 
	 ********************************/
	public double initialSalePrice(double pbar, double d, double principal) {
		final double C = 0.03;//0.095;	// initial markup from average price (more like 0.2 from BoE calibration)
		final double D = 0.02;//0.024;//0.01;//0.001;		// Size of Days-on-market effect
		final double E = 0.05; //0.05;	// SD of noise
		double exponent = C + Math.log(pbar) - D*Math.log((d + 1.0)/31.0) + E*Model.rand.nextGaussian();
		return(Math.max(Math.exp(exponent), principal));
	}
	

	/**
	 * @return Does an owner-occupier decide to sell house?
	 */
	public boolean decideToSellHome(Household me) {
		// Am I forced to move because of job change etc?
		// if(rand.nextDouble() < data.Households.P_FORCEDTOMOVE) return(true);
		// I can get a better house by moving?
		int potentialQualityChange = Model.housingMarket.maxQualityGivenPrice(Model.bank.getMaxMortgage(me,true))- me.home.getQuality();
		double p_move = data.Households.P_FORCEDTOMOVE + (data.Households.P_SELL-data.Households.P_FORCEDTOMOVE)/(1.0+Math.exp(5.0-2.0*potentialQualityChange));
		return(rand.nextDouble() < p_move);
	}

	public double downPayment(double bankBalance) {
		return(bankBalance*DOWNPAYMENT_FRACTION);
	}

	
	/********************************************************
	 * Decide how much to drop the list-price of a house if
	 * it has been on the market for (another) month and hasn't
	 * sold. Calibrated against Zoopla dataset in Bank of England
	 * 
	 * @param sale The HouseSaleRecord of the house that is on the market.
	 ********************************************************/
	public double rethinkHouseSalePrice(HouseSaleRecord sale) {
//		return(sale.getPrice() *0.95);
		if(rand.nextDouble() > data.Households.P_SALEPRICEREDUCE) {
			double logReduction = Math.min(-5.1e-3, data.Households.REDUCTION_MU+(rand.nextGaussian()*data.Households.REDUCTION_SIGMA));
			return(sale.getPrice() * (1.0-Math.exp(logReduction)));
		}
		return(sale.getPrice());
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Renter behaviour
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean renterPurchaseDecision(Household h, double housePrice, double annualRent) {
		final double COST_OF_RENTING = 600; // Annual psychological cost of renting
		final double FTB_K = 1.0/2000.0;//1.0/100000.0;//0.005 // Heterogeneity of sensitivity of desire to first-time-buy to cost
		double costOfHouse;
		
		if(Model.housingMarket.getAverageSalePrice(0)*0.85 > housePrice) return(false); // can't afford a house anyway?

//		if(Model.rand.nextDouble() < 0.5) return false; // ########### TEST
		int q = 0;
		if(h.home != null) q = h.home.getQuality();
		housePrice = Model.housingMarket.getAverageSalePrice(q); // ############ TEST compare same quality
		costOfHouse = housePrice*(Model.bank.loanToValue(h.isFirstTimeBuyer(),true)*Model.bank.getMortgageInterestRate() - Model.housingMarket.housePriceAppreciation());
//		costOfHouse = housePrice*(Model.bank.loanToValue(h.isFirstTimeBuyer(),true)*Model.bank.getMortgageInterestRate());
		return(Model.rand.nextDouble() < 1.0/(1.0 + Math.exp(-FTB_K*(annualRent + COST_OF_RENTING - costOfHouse))));
	}

	/********************************************************
	 * Decide how much to bid on the rental market
	 * Source: Zoopla rental prices 2008-2009 (at Bank of England)
	 ********************************************************/
	public double desiredRent(double monthlyIncome) {
//		return(monthlyIncome * 0.33);
		double annualIncome = monthlyIncome*12.0; // TODO: this should be net annual income, not gross
		double rent;
		if(annualIncome < 12000.0) {
			rent = 386.0;
		} else {
			rent = 11.72*Math.pow(annualIncome, 0.372);
		}
		rent *= Math.exp(Model.rand.nextGaussian()*0.0826);
		return(rent);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Property investor behaviour
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 
	 * @param h The house in question
	 * @param me The investor
	 * @return Does an investor decide to sell a buy-to-let property
	 */
	public boolean decideToSellInvestmentProperty(House h, Household me) {
//		System.out.println(me.desiredPropertyInvestmentFraction + " " + me.getDesiredPropertyInvestmentValue() + " -> "+me.getPropertyInvestmentValuation());
//		if(me.getDesiredPropertyInvestmentValue() < 
//				(me.getPropertyInvestmentValuation() - me.houseMarket.getAverageSalePrice(h.quality))) {
//			return(true);
//		}
		/*
		if(me.housePayments.size()+1 > desiredBTLProperties) {
			// only count properties on market if necessary as it's a slow operation
			if(me.housePayments.size()+1 > desiredBTLProperties - me.nPropertiesForSale()) {
				return(true);
			}
		}
		//return(rand.nextDouble() < P_SELL);
		 */
		return(false);
	}
	

	/**
	 * How much rent does an investor decide to charge on a buy-to-let house? 
	 * @param pbar average rent for house of this quality
	 * @param d average days on market
	 */
	public double buyToLetRent(double pbar, double d, double mortgagePayment) {
		final double C = 0.01;//0.095;	// initial markup from average price
		final double D = 0.02;//0.024;//0.01;//0.001;		// Size of Days-on-market effect
		final double E = 0.05; //0.05;	// SD of noise
		double exponent = C + Math.log(pbar) - D*Math.log((d + 1.0)/31.0) + E*Model.rand.nextGaussian();
//		return(Math.max(Math.exp(exponent), mortgagePayment));
		double result = Math.exp(exponent);
		return(result);
//		return(mortgagePayment*(1.0+RENT_PROFIT_MARGIN));
	}

	public double rethinkBuyToLetRent(HouseSaleRecord sale) {
		return(0.95*sale.getPrice());
//		if(rand.nextDouble() > 0.944) {
//			double logReduction = Math.min(4.6, 1.603+(rand.nextGaussian()*0.6173));
//			return(sale.getPrice() * (1.0-0.01*Math.exp(logReduction)));
//		}
//		return(sale.getPrice());
	}

	public boolean decideToBuyBuyToLet(Household me) {
		return(isPropertyInvestor() && (me.nInvestmentProperties() < nDesiredBTLProperties()));
	}
	
	public double btlPurchaseBid(Household me) {
		return(Model.bank.getMaxMortgage(me, false));
	}
//	public boolean decideToBuyBuyToLet(House h, Household me, double price) {
		// --- give preference to cheaper properties
//		if(Model.rand.nextDouble() < (h.getQuality()*1.0/House.Config.N_QUALITY)-0.5) return(false);
//		if(price <= Model.bank.getMaxMortgage(me, false)) {
//			MortgageApproval mortgage;
//			mortgage = Model.bank.requestApproval(me, price, 0.0, false); // maximise leverege with min downpayment
//			return(buyToLetPurchaseDecision(price, mortgage.monthlyPayment, mortgage.downPayment));

//		}
//		System.out.println("BTL refused mortgage on "+price+" can get "+Model.bank.getMaxMortgage(me, false));
//		return(false);
//	}
	/**
	 * @param price The asking price of the house
	 * @param monthlyPayment The monthly payment on a mortgage for this house
	 * @param downPayment The minimum downpayment on a mortgage for this house
	 * @return will the investor decide to buy this house?
	 */
//	public boolean buyToLetPurchaseDecision(double price, double monthlyPayment, double downPayment) {
//		double yield;
//		yield = (monthlyPayment*12*RENT_PROFIT_MARGIN + Model.housingMarket.housePriceAppreciation()*price)/
//				downPayment;
//		if(Model.rand.nextDouble() < 1.0/(1.0 + Math.exp( - yield*4.0))) {
//			System.out.println("BTL: bought");
//			return(true);
//		}
//		System.out.println("BTL: didn't buy");
//		return(false);
//	}

	public boolean isPropertyInvestor() {
		return(desiredBTLProperties > 0);
	}

	public int nDesiredBTLProperties() {
		return desiredBTLProperties;
	}


}
