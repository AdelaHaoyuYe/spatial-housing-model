package housing;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.lang.Integer;

/************************************************
 * Class to encapsulate all the configuration parameters
 * of the model
 * It also contains all methods needed to read these
 * parameter values from a configuration properties file
 *
 * @author Adrian Carro
 * @since 20/02/2017
 *
 ************************************************/
public class Config {

    /** Declaration of parameters **/       // They can be package-private

    // General model control parameters
    int N_STEPS;				            // Simulation duration in time steps
    int TIME_TO_START_RECORDING;	        // Time steps before recording statistics (initialisation time)
    int N_SIMS; 					        // Number of simulations to run (monte-carlo)
    boolean recordCoreIndicators;		    // True to write time series for each core indicator
    boolean recordMicroData;			    // True to write micro data for each transaction made

    // House parameters
    public int N_QUALITY;                   // Number of quality bands for houses

    // Housing market parameters
    int DAYS_UNDER_OFFER;                   // Time (in days) that a house remains under offer
    double BIDUP;                           // Smallest proportional increase in price that can cause a gazump
    double MARKET_AVERAGE_PRICE_DECAY;      // Decay constant for the exponential moving average of sale prices


    // Demographic parameters
    int TARGET_POPULATION;                  // Target number of households
    boolean SPINUP;                         // TODO: Unclear parameter related to the creation of the population

    // Household parameters
    boolean BTL_ENABLED;                    // True to have a buy-to-let sector // TODO: Useless parameter!
    double RETURN_ON_FINANCIAL_WEALTH;      // Monthly percentage growth of financial investments
    int TENANCY_LENGTH_AVERAGE;             // Average number of months a tenant will stay in a rented house
    int TENANCY_LENGTH_EPSILON;             // Standard deviation of the noise in determining the tenancy length

    // Household behaviour parameters: buy-to-let
    double P_INVESTOR;                      // Prior probability of being (wanting to be) a BTL investor
    double MIN_INVESTOR_PERCENTILE;         // Minimum income percentile for a household to be a BTL investor
    double FUNDAMENTALIST_CAP_GAIN_COEFF;   // Weight that fundamentalists put on cap gain
    double TREND_CAP_GAIN_COEFF;			// Weight that trend-followers put on cap gain
    double P_FUNDAMENTALIST; 			    // Probability that a BTL investor is a fundamentalist versus a trend-follower
    boolean BTL_YIELD_SCALING;			    // Chooses between two possible equations for BTL investors to make their buy/sell decisions
    // Household behaviour parameters: rent
    double DESIRED_RENT_INCOME_FRACTION;    // Desired proportion of income to be spent on rent
    double PSYCHOLOGICAL_COST_OF_RENTING;   // Annual psychological cost of renting
    double SENSITIVITY_RENT_OR_PURCHASE;    // Sensitivity parameter of the decision between buying and renting
    // Household behaviour parameters: general
    double BANK_BALANCE_FOR_CASH_DOWNPAYMENT;   // If bankBalance/housePrice is above this, payment will be made fully in cash
    double HPA_EXPECTATION_FACTOR;              // Weight assigned to current trend when computing expectations
    int HPA_YEARS_TO_CHECK = 1;                 // Number of years of the HPI record to check when computing the annual HPA
    double HOLD_PERIOD;                         // Average period, in years, for which owner-occupiers hold their houses
    // Household behaviour parameters: sale price reduction
    double P_SALE_PRICE_REDUCE;             // Monthly probability of reducing the price of a house on the market
    double REDUCTION_MU;                    // Mean percentage reduction for prices of houses on the market
    double REDUCTION_SIGMA;                 // Standard deviation of percentage reductions for prices of houses on the market
    // Household behaviour parameters: consumption
    double CONSUMPTION_FRACTION;            // Fraction of monthly budget for consumption (monthly budget = bank balance - minimum desired bank balance)
    double ESSENTIAL_CONSUMPTION_FRACTION;  // Fraction of Government support necessarily spent monthly by all households as essential consumption
    // Household behaviour parameters: initial sale price
    double SALE_MARKUP;                     // Initial markup over average price of same quality houses
    double SALE_WEIGHT_DAYS_ON_MARKET;      // Weight of the days-on-market effect
    double SALE_EPSILON;                    // Standard deviation of the noise
    // Household behaviour parameters: buyer's desired expenditure
    double BUY_SCALE;                       // Scale, number of annual salaries the buyer is willing to spend for buying a house
    double BUY_WEIGHT_HPA;                  // Weight given to house price appreciation when deciding how much to spend for buying a house
    double BUY_EPSILON;                     // Standard deviation of the noise
    // Household behaviour parameters: demand rent
    double RENT_MARKUP;                     // Markup over average rent demanded for houses of the same quality
    double RENT_EQ_MONTHS_ON_MARKET;        // Number of months on the market in an equilibrium situation
    double RENT_EPSILON;                    // Standard deviation of the noise
    double RENT_MAX_AMORTIZATION_PERIOD;    // Maximum period BTL investors are ready to wait to get back their investment, this determines their minimum demanded rent
    double RENT_REDUCTION;                  // Percentage reduction of demanded rent for every month the property is in the market, not rented
    // Household behaviour parameters: downpayment
    double DOWNPAYMENT_FTB_SCALE;           // Scale parameter for the log-normal distribution of downpayments by first-time-buyers
    double DOWNPAYMENT_FTB_SHAPE;           // Shape parameter for the log-normal distribution of downpayments by first-time-buyers
    double DOWNPAYMENT_OO_SCALE;            // Scale parameter for the log-normal distribution of downpayments by owner-occupiers
    double DOWNPAYMENT_OO_SHAPE;            // Shape parameter for the log-normal distribution of downpayments by owner-occupiers
    double DOWNPAYMENT_MIN_INCOME;          // Minimum income percentile to consider any downpayment, below this level, downpayment is set to 0
    double DOWNPAYMENT_BTL_MEAN;            // Average downpayment, as percentage of house price, by but-to-let investors
    double DOWNPAYMENT_BTL_EPSILON;         // Standard deviation of the noise
    // Household behaviour parameters: desired bank balance
    double DESIRED_BANK_BALANCE_ALPHA;
    double DESIRED_BANK_BALANCE_BETA;
    double DESIRED_BANK_BALANCE_EPSILON;
    // Household behaviour parameters: selling decision
    double DECISION_TO_SELL_ALPHA;          // Weight of houses per capita effect
    double DECISION_TO_SELL_BETA;           // Weight of interest rate effect
    double DECISION_TO_SELL_HPC;            // TODO: fudge parameter, explicitly explained otherwise in the paper
    double DECISION_TO_SELL_INTEREST;       // TODO: fudge parameter, explicitly explained otherwise in the paper
    // Household behaviour parameters: BTL buy/sell choice
    double BTL_CHOICE_INTENSITY;            // Shape parameter, or intensity of choice on effective yield
    double BTL_CHOICE_MIN_BANK_BALANCE;     // Minimun bank balance, as a percentage of the desired bank balance, to buy new properties

    // Bank parameters
    int MORTGAGE_DURATION_YEARS;            // Mortgage duration in years
    double BANK_INITIAL_BASE_RATE;          // Bank initial base-rate (currently remains unchanged)
    double BANK_CREDIT_SUPPLY_TARGET;       // Bank's target supply of credit per household per month

    // Central bank parameters
    double CENTRAL_BANK_MAX_FTB_LTV;		    // Maximum LTV ratio that the bank would allow for first-time-buyers when not regulated
    double CENTRAL_BANK_MAX_OO_LTV;		        // Maximum LTV ratio that the bank would allow for owner-occupiers when not regulated
    double CENTRAL_BANK_MAX_BTL_LTV;	        // Maximum LTV ratio that the bank would allow for BTL investors when not regulated
    double CENTRAL_BANK_FRACTION_OVER_MAX_LTV;  // Maximum fraction of mortgages that the bank can give over the LTV ratio limit
    double CENTRAL_BANK_MAX_FTB_LTI;		    // Maximum LTI ratio that the bank would allow for first-time-buyers when not regulated
    double CENTRAL_BANK_MAX_OO_LTI;		        // Maximum LTI ratio that the bank would allow for owner-occupiers when not regulated
    double CENTRAL_BANK_FRACTION_OVER_MAX_LTI;  // Maximum fraction of mortgages that the bank can give over the LTI ratio limit
    double CENTRAL_BANK_AFFORDABILITY_COEFF;    // Maximum fraction of the household's income to be spent on mortgage repayments under stressed conditions
    double CENTRAL_BANK_BTL_STRESSED_INTEREST;  // Interest rate under stressed condition for BTL investors when calculating interest coverage ratios (ICR)
    double CENTRAL_BANK_MAX_ICR;                // Interest coverage ratio (ICR) limit imposed by the central bank

    // Construction sector parameters
    double CONSTRUCTION_HOUSES_PER_HOUSEHOLD;   // Target ratio of houses per household

    // Government parameters
    double GOVERNMENT_PERSONAL_ALLOWANCE_LIMIT;     // Maximum personal allowance
    double GOVERNMENT_INCOME_SUPPORT;               // Minimum monthly earnings for a married couple from income support

    /** Declaration of addresses **/        // They must be public to be accessed from data package

    // Data addresses: Government
    public String DATA_TAX_RATES;                  // Address for tax bands and rates data
    public String DATA_NATIONAL_INSURANCE_RATES;   // Address for national insurance bands and rates data

    /** Construction of objects to contain derived parameters and constants **/

    // Create object containing all constants
    Config.Constants constants = new Constants();

    // Finally, create object containing all derived parameters
    Config.DerivedParams derivedParams = new DerivedParams();

    /**
     * Class to contain all parameters which are not read from the configuration (.properties) file, but derived,
     * instead, from these configuration parameters
     */
    public class DerivedParams {
        // Housing market parameters
        int HPI_RECORD_LENGTH;          // Number of months to record HPI (to compute price growth at different time scales)
        double MONTHS_UNDER_OFFER;      // Time (in months) that a house remains under offer
        double T;                       // Characteristic number of data-points over which to average market statistics
        double E;                       // Decay constant for averaging days on market (in transactions)
        double G;                       // Decay constant for averageListPrice averaging (in transactions)
        // Household behaviour parameters: general
        double MONTHLY_P_SELL;          // Monthly probability for owner-occupiers to sell their houses
        // Bank parameters
        int N_PAYMENTS;                 // Number of monthly repayments (mortgage duration in months)
        // House rental market parameters
        double K;                       // Decay factor for exponential moving average of gross yield from rentals (averageSoldGrossYield)
        double KL;                      // Decay factor for long-term exponential moving average of gross yield from rentals (longTermAverageGrossYield)
    }

    /**
     * Class to contain all constants (not read from the configuration file nor derived from it)
     */
    public class Constants {
        final int DAYS_IN_MONTH = 30;
        final int MONTHS_IN_YEAR = 12;
    }

    /**
     * Empty constructor, mainly used for copying local instances of the Model Config instance into other classes
     */
    public Config () {}

    /**
     * Constructor with full initialization, used only for the original Model Config instance
     */
    public Config (String configFileName) {
        getConfigValues(configFileName);
    }

    /**
     * Method to read configuration parameters from a configuration (.properties) file
     * @param   configFileName    String with name of configuration (.properties) file (address inside source folder)
     */
    public void getConfigValues(String configFileName) {
        // Try-with-resources statement
        try (FileReader fileReader = new FileReader(configFileName)) {
            Properties prop = new Properties();
            prop.load(fileReader);
            // Run through all the fields of the Class using reflection
            for (Field field : this.getClass().getDeclaredFields()) {
                System.out.println(field.getName());
                // For int fields, parse the int with appropriate exception handling
                if (field.getType().toString().equals("int")) {
                    try {
                        field.set(this, Integer.parseInt(prop.getProperty(field.getName())));
                    } catch (NumberFormatException nfe) {
                        System.out.println("Exception " + nfe + " while trying to parse the field " +
                                field.getName() + " for an integer");
                        nfe.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        System.out.println("Exception " + iae + " while trying to set the field " +
                                field.getName());
                        iae.printStackTrace();
                    }
                // For double fields, parse the double with appropriate exception handling
                } else if (field.getType().toString().equals("double")) {
                        try {
                            field.set(this, Double.parseDouble(prop.getProperty(field.getName())));
                        } catch (NumberFormatException nfe) {
                            System.out.println("Exception " + nfe + " while trying to parse the field " +
                                    field.getName() + " for an double");
                            nfe.printStackTrace();
                        } catch (IllegalAccessException iae) {
                            System.out.println("Exception " + iae + " while trying to set the field " +
                                    field.getName());
                            iae.printStackTrace();
                        }
                // For boolean fields, parse the boolean with appropriate exception handling
                } else if (field.getType().toString().equals("boolean")) {
                    try {
                        if (prop.getProperty(field.getName()).equals("true") ||
                                prop.getProperty(field.getName()).equals("false")) {
                            field.set(this, Boolean.parseBoolean(prop.getProperty(field.getName())));
                        } else {
                            throw new BooleanFormatException("For input string \"" + prop.getProperty(field.getName()) +
                                    "\"");
                        }
                    } catch (BooleanFormatException bfe) {
                        System.out.println("Exception " + bfe + " while trying to parse the field " +
                                field.getName() + " for a boolean");
                        bfe.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        System.out.println("Exception " + iae + " while trying to set the field " +
                                field.getName());
                        iae.printStackTrace();
                    }
                // For string fields, parse the string with appropriate exception handling
                }
                else if (field.getType().toString().equals("class java.lang.String")) {
                    try {
                        field.set(this, prop.getProperty(field.getName()).replace("\"", "").replace("\'", ""));
                    } catch (IllegalAccessException iae) {
                        System.out.println("Exception " + iae + " while trying to set the field " +
                                field.getName());
                        iae.printStackTrace();
                    }
                }
                // TODO: Add warning for unknown field type (taking into account derivedParams field type) and for fields without value
            }
        } catch (IOException ioe) {
            System.out.println("Exception " + ioe + " while trying to read file '" + configFileName + "'");
            ioe.printStackTrace();
        }
        // Finally, compute and set values for all derived parameters
        setDerivedParams();
    }

    /**
     * Method to compute and set values for all derived parameters
     */
    private void setDerivedParams() {
        // Housing market parameters
        derivedParams.HPI_RECORD_LENGTH = HPA_YEARS_TO_CHECK*constants.MONTHS_IN_YEAR + 3;  // Plus three months in a quarter
        derivedParams.MONTHS_UNDER_OFFER = DAYS_UNDER_OFFER/constants.DAYS_IN_MONTH;
        derivedParams.T = 0.02*TARGET_POPULATION;                   // TODO: Clarify where does this 0.2 come from
        derivedParams.E = Math.exp(-1.0/derivedParams.T);
        derivedParams.G = Math.exp(-N_QUALITY/derivedParams.T);
        // Household behaviour parameters: general
        derivedParams.MONTHLY_P_SELL = 1.0/(HOLD_PERIOD*constants.MONTHS_IN_YEAR);
        // Bank parameters
        derivedParams.N_PAYMENTS = MORTGAGE_DURATION_YEARS*constants.MONTHS_IN_YEAR;
        // House rental market parameters
        derivedParams.K = Math.exp(-10000.0/(TARGET_POPULATION*50.0));
        derivedParams.KL = Math.exp(-10000.0/(TARGET_POPULATION*50.0*200.0));
    }

    /**
     * Equivalent to NumberFormatException for detecting problems when parsing for boolean values
     */
    public class BooleanFormatException extends RuntimeException {
        public BooleanFormatException() { super(); }
        public BooleanFormatException(String message) { super(message); }
        public BooleanFormatException(Throwable cause) { super(cause); }
        public BooleanFormatException(String message, Throwable cause) { super(message, cause); }
    }
}
