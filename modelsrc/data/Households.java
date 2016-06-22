package data;

import org.apache.commons.math3.distribution.LogNormalDistribution;

public class Households {
	static public double P_SELL = 1.0/(11.0*12.0);  // monthly probability of Owner-Occupier selling home (British housing survey 2008)
	static public double P_FORCEDTOMOVE = P_SELL*0.1;		// monthly probability of an OO being forced to move due to external factors (job change etc)
//	static public double BTL_MU = Math.log(3.44); 	// location parameter for No. of houses owned by BtL
//	static public double BTL_SIGMA = 1.050;			// shape parameter for No. of houses owned by BtL
	static public double P_INVESTOR = 0.04;//0.04; 		// Prior probability of being (wanting to be) a property investor (should be 4%)
	static public double MIN_INVESTOR_PERCENTILE = 0.5; // minimum income percentile for a HH to be a BTL investor
//	static public LogNormalDistribution buyToLetDistribution  = new LogNormalDistribution(BTL_MU, BTL_SIGMA); // No. of houses owned by buy-to-let investors Source: ARLA review and index Q2 2014

	// House price reduction behaviour. Calibrated against Zoopla data at BoE
	static public double P_SALEPRICEREDUCE = 1.0-0.945; 	// monthly probability of reducing the price of house on market
	static public double REDUCTION_MU = 1.603; 	// mean of house price %age reductions for houses on the market. 
	static public double REDUCTION_SIGMA = 0.617;		// SD of house price %age reductions for houses on the market
	static public double CONSUMPTION_FRACTION=0.5; // Fraction of the monthly budget for consumption (budget is bank balance - minimum desired bank balance)
}
