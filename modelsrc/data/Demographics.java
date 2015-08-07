package data;

import utilities.DoubleUnaryOperator;
import utilities.Pdf;

public class Demographics {
	public static final int TARGET_POPULATION = 5000;  	// target number of households
	public static final int SPINUP_YEARS = 80;			// number of years to spinup

	/**
	 * Target probability density of age of representative householder
	 * at time t=0
	 * Calibrated against Katie's data (LCFS 2012)
	 */
	public static Pdf pdfAge = new Pdf("modelsrc/data/AgeMarginalPDF.csv");

	/**
	 * Probability density by age of the representative householder given that
	 * the household is newly formed.
	 * New households can be formed by, e.g., children leaving home,
	 * divorce, separation, people leaving an HMO.
	 * TODO: ************ UNCALIBRATED ******************
	 */
	public static Pdf pdfHouseholdAgeAtBirth = new Pdf(15.0, 28.0, new DoubleUnaryOperator() {
		public double applyAsDouble(double age) {
			if(age>=15.0 && age < 16.0) {
				return(1.0);
			}
//			if(age>=18.0 && age<28.0) 
//				return(0.1);
			return(0.0);
		}	
	});

	/****
	 * Birth rates into the future (roughly calibrated against current individual birth rate)
	 * @param t	time (months) into the future
	 * @return number of births per year
	 * TODO: ********** CALIBRATE AGAINST FTB FLUX ********************
	 */
	public static double futureBirthRate(double t) {
		return(TARGET_POPULATION * 0.012);
	}

	/***
	 * Probability that a household 'dies' per year given age of the representative householder
	 * Death of a household may occur by marriage, death of single occupant, moving together
	 * TODO: ************** UNCALIBRATED ********************
	 */
	public static double probDeathGivenAge(double ageInYears) {
		double averageDeathRate = futureBirthRate(0)*1.0/TARGET_POPULATION;
		return(averageDeathRate*ageInYears*ageInYears/7500.0);
	}

	/*
	 * This calculates the pdf of Household age at death from probDeathGivenAge() according to
	 * 
	 * P(a) = r(a) exp(-integral_0^a r(a') da')
	 * 
	 * where r(a) is probDeathGivenAge.
	 * 
	 */
	/*
	public static Pdf pdfHouseholdAgeAtDeath = new Pdf(0.0, 150.0, new DoubleUnaryOperator() {
		public double applyAsDouble(double age) {
			double a = 0.0;
			double da = 0.1;
			double integral = 0.0;
			double p;
			do {
				p = probDeathGivenAge(a + 0.5*da);
				integral += p*da;
				a += da;
			} while(a<=age);
			integral -= (a - age)*p;
			return(p*Math.exp(-integral));
//			double p = probDeathGivenAge(0.0);
//			return(p*Math.exp(-age*p));
		}
	}, 100);
	*/
}
