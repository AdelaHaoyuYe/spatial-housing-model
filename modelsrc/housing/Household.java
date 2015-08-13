package housing;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.LogNormalDistribution;

import ec.util.MersenneTwisterFast;
/**********************************************
 * This represents a household who receives an income, consumes,
 * saves and can buy/sell/let/invest-in houses.
 * 
 * @author daniel, davidrpugh
 *
 **********************************************/
public class Household implements IHouseOwner {


	//////////////////////////////////////////////////////////////////////////////////////
	// Model
	//////////////////////////////////////////////////////////////////////////////////////

	/********************************************************
	 * Constructor.
	 ********************************************************/
	public Household(double age) {
		bank = Model.bank;
		houseMarket = Model.housingMarket;
		rentalMarket = Model.rentalMarket;
		rand = Model.rand;
		home = null;
		isFirstTimeBuyer = true;
		id = ++id_pool;
		lifecycle = new Lifecycle(age);
		behaviour = new HouseholdBehaviour(lifecycle.incomePercentile);
		annualEmploymentIncome = lifecycle.annualIncome();
		bankBalance = Math.exp(4.07*Math.log(annualEmploymentIncome)-33.1);
		monthlyPropertyIncome = 0.0;
	}


	/////////////////////////////////////////////////////////
	// Inheritance behaviour
	/////////////////////////////////////////////////////////

	public void transferAllWealthTo(Household beneficiary) {
		if(beneficiary == this) System.out.println("Strange: I'm transfering all my wealth to myself");
		for(House h : housePayments.keySet()) {
			if(home == h) {
				h.resident = null;
				home = null;
			}
			if(h.owner == this) {
				if(h.isOnRentalMarket()) rentalMarket.removeOffer(h.getRentalRecord());
				if(h.isOnMarket()) houseMarket.removeOffer(h.getSaleRecord());
				beneficiary.inheritHouse(h);
			} else {
				h.owner.endOfLettingAgreement(h, housePayments.get(h));
			}
		}
		housePayments.clear();
		beneficiary.bankBalance += bankBalance;
	}
	
	/**
	 * 
	 * @param h House to inherit
	 */
	public void inheritHouse(House h) {
		MortgageAgreement nullMortgage = new MortgageAgreement(this,false);
		nullMortgage.nPayments = 0;
		nullMortgage.downPayment = 0.0;
		nullMortgage.monthlyInterestRate = 0.0;
		nullMortgage.monthlyPayment = 0.0;
		nullMortgage.principal = 0.0;
		nullMortgage.purchasePrice = 0.0;
		housePayments.put(h, nullMortgage);
		h.owner = this;
		if(!isHomeowner()) {
			// move into house if not already a homeowner
			if(isRenting()) {
				endTenancyEarly();				
			}
			if(h.resident != null) {
				h.resident.endTenancyEarly();
			}
			home = h;
			h.resident = this;
		} else if(behaviour.isPropertyInvestor()) {
			if(decideToSellHouse(h)) {
				putHouseForSale(h);
			} else if(h.resident == null) {
				// endOfLettingAgreement(h); // put inherited house on rental market
				rentalMarket.offer(h, buyToLetRent(h));
			}
		} else {
			putHouseForSale(h);
		}
	}
	
	/////////////////////////////////////////////////////////
	// House market behaviour
	/////////////////////////////////////////////////////////

	/********************************************************
	 * First step in a time-step:
	 * Receive income, pay rent/mortgage, make consumption decision
	 * and make decision to buy/sell house.
	 ********************************************************/
	public void preSaleClearingStep() {
		double disposableIncome;
		
		lifecycle.step();
		annualEmploymentIncome = lifecycle.annualIncome();
		disposableIncome = getMonthlyPostTaxIncome() - 0.8 * Government.Config.INCOME_SUPPORT;

		// ---- Pay rent/mortgage(s)
		Iterator<Map.Entry<House,PaymentAgreement> > mapIt = housePayments.entrySet().iterator();
		Map.Entry<House,PaymentAgreement> payment;
		while(mapIt.hasNext()) {
			payment = mapIt.next();
			if(payment.getValue().nPayments > 0) {
				disposableIncome -= payment.getValue().makeMonthlyPayment();
				if(payment.getValue().nPayments == 0 && payment.getKey().owner != this) { // end of rental period for renter
					endOfTenancyAgreement(payment.getKey(), payment.getValue());
					mapIt.remove();
				}
			}
		}
		
		// --- consume
		bankBalance += disposableIncome - behaviour.desiredConsumptionB(annualEmploymentIncome/12.0,bankBalance);
		
		if(bankBalance < 0.0) {
			// bankrupt behaviour				
			// TODO: cash injection for now...
			bankBalance = 1.0;
		}
		
		makeHousingDecision();
		if(behaviour.decideToBuyBuyToLet(this)) {
			houseMarket.BTLbid(this, behaviour.btlPurchaseBid(this));
		}
	}

	/********************************************************
	 * Second step in a time-step. At this point, the
	 * household may have sold their house, but not managed
	 * to buy a new one, so must enter the rental market.
	 * 
	 * This is also where investors get to bid for buy-to-let
	 * housing.
	 ********************************************************/
	public void preRentalClearingStep() {
		if(isHomeless()) {
			rentalMarket.bid(this, behaviour.desiredRent(annualEmploymentIncome/12.0));
		}
	}
	
	/********************************************************
	 *  Make decision to buy/sell houses
	 ********************************************************/
	void makeHousingDecision() {
		// --- add and manage houses for sale
		HouseSaleRecord forSale, forRent;
		double newPrice;
		
		for(House h : housePayments.keySet()) {
			if(h.owner == this) {
				forSale = h.getSaleRecord();
				if(forSale != null) { // reprice house for sale
					newPrice = behaviour.rethinkHouseSalePrice(forSale);
					if(newPrice > mortgageFor(h).principal) {
						houseMarket.updateOffer(forSale, newPrice);						
					} else {
						houseMarket.removeOffer(forSale);
						if(h != home && h.resident == null) {
							rentalMarket.offer(h, buyToLetRent(h));
						}
					}
				} else if(decideToSellHouse(h)) { // put house on market?
					if(h.isOnRentalMarket()) rentalMarket.removeOffer(h.getRentalRecord());
					putHouseForSale(h);
				}
				
				forRent = h.getRentalRecord();
				if(forRent != null) {
					newPrice = behaviour.rethinkBuyToLetRent(forRent);
					rentalMarket.updateOffer(forRent, newPrice);		
				}
			}
		}
		
		// ---- try to buy house?
		if(!isHomeowner()) {
			decideToStopRenting();
		}
	}

	protected void putHouseForSale(House h) {
		double principal;
		MortgageAgreement mortgage = mortgageFor(h);
		if(mortgage != null) {
			principal = mortgage.principal;
		} else {
			principal = 0.0;
		}
		houseMarket.offer(h, behaviour.initialSalePrice(
				houseMarket.averageSalePrice[h.getQuality()],
				houseMarket.averageDaysOnMarket,
				principal
		));
	}
	
	/////////////////////////////////////////////////////////
	// Houseowner interface
	/////////////////////////////////////////////////////////

	/********************************************************
	 * Do all the stuff necessary when this household
	 * buys a house:
	 * Give notice to landlord if renting,
	 * Get loan from mortgage-lender,
	 * Pay for house,
	 * Put house on rental market if buy-to-let and no tenant.
	 ********************************************************/
	public void completeHousePurchase(HouseSaleRecord sale) {
		if(isRenting()) { // give immediate notice to landlord and move out
			if(sale.house.resident != null) System.out.println("Strange: my new house has someone in it!");
			if(home == sale.house) {
				System.out.println("Strange: I've just bought a house I'm renting out");
			} else {
				endTenancyEarly();
			}
		}
		MortgageAgreement mortgage = bank.requestLoan(this, sale.getPrice(), behaviour.downPayment(bankBalance), home == null);
		if(mortgage == null) {
			// TODO: need to either provide a way for house sales to fall through or to
			// TODO: ensure that pre-approvals are always satisfiable
			System.out.println("Can't afford to buy house: strange");
//			System.out.println("Want "+sale.getPrice()+" but can only get "+bank.getMaxMortgage(this,home==null));
			System.out.println("Bank balance is "+bankBalance+". DisposableIncome is "+ getMonthlyPostTaxIncome());
			System.out.println("Annual income is "+ annualEmploymentIncome);
			if(isRenting()) System.out.println("Is renting");
			if(isHomeowner()) System.out.println("Is homeowner");
			if(isHomeless()) System.out.println("Is homeless");
			if(isFirstTimeBuyer()) System.out.println("Is firsttimebuyer");
			if(behaviour.isPropertyInvestor()) System.out.println("Is investor");
			System.out.println("House owner = "+sale.house.owner);
			System.out.println("me = "+this);
		}
		bankBalance -= mortgage.downPayment;
		housePayments.put(sale.house, mortgage);
		if(home == null) { // move in to house
			home = sale.house;
			sale.house.resident = this;
		} else if(sale.house.resident == null) { // put empty buy-to-let house on rental market
			rentalMarket.offer(sale.house, buyToLetRent(sale.house));
//			endOfLettingAgreement(sale.house);
		}
		isFirstTimeBuyer = false;
	}
		
	/********************************************************
	 * Do all stuff necessary when this household sells a house
	 ********************************************************/
	public void completeHouseSale(HouseSaleRecord sale) {
		double profit = sale.getPrice() - mortgageFor(sale.house).payoff(bankBalance+sale.getPrice());
		if(profit < 0) System.out.println("Negative equity in house.");
		bankBalance += profit;
		if(sale.house.isOnRentalMarket()) {
			rentalMarket.removeOffer(sale);
		}
		if(housePayments.get(sale.house).nPayments == 0) {
			housePayments.remove(sale.house);
		}
		if(sale.house == home) { // move out of home and become (temporarily) homeless
			home.resident = null;
			home = null;
			bidOnHousingMarket(1.0);
		} else if(sale.house.resident != null) { // evict current renter
			sale.house.resident.getEvicted();
		}
	}
	
	/********************************************************
	 * A BTL investor receives this message when a tenant moves
	 * out of one of its buy-to-let houses.
	 * 
	 * The household simply puts the house back on the rental
	 * market.
	 ********************************************************/
	@Override
	public void endOfLettingAgreement(House h, PaymentAgreement contract) {
		monthlyPropertyIncome -= contract.monthlyPayment;

		// put house back on rental market
		if(!housePayments.containsKey(h)) {
			System.out.println("Strange: I don't own this house in endOfLettingAgreement");
		}
//		if(h.resident != null) System.out.println("Strange: renting out a house that has a resident");		
//		if(h.resident != null && h.resident == h.owner) System.out.println("Strange: renting out a house that belongs to a homeowner");		
		if(h.isOnRentalMarket()) System.out.println("Strange: got endOfLettingAgreement on house on rental market");
		if(!h.isOnMarket()) rentalMarket.offer(h, buyToLetRent(h));
	}

	/**********************************************************
	 * This household moves out of current rented accommodation
	 * and becomes homeless (possibly temporarily). Move out,
	 * inform landlord and delete rental agreement.
	 **********************************************************/
	public void endTenancyEarly() {
		endOfTenancyAgreement(home, housePayments.remove(home));
	}
	
	/*** Landlord has told this household to get out: leave without informing landlord */
	public void getEvicted() {
		if(home == null) {
			System.out.println("Strange: got evicted but I'm homeless");			
		}
		if(home.owner == this) {
			System.out.println("Strange: got evicted from a home I own");
		}
		housePayments.remove(home);
		home.resident = null;
		home = null;		
	}

	/***
	 * This gets called when we are a renter and a tenancy
	 * agreement has come to an end. Move out and inform landlord.
	 * Don't delete rental agreement because we're probably iterating over payments.
	 */
	public void endOfTenancyAgreement(House house, PaymentAgreement rentalContract) {
		if(home == null) System.out.println("Strange: paying rent and homeless");
		if(house != home) System.out.println("Strange: I seem to have been renting a house but not living in it");
		if(home.resident != this) System.out.println("home/resident link is broken");
		home.resident = null;
		home = null;
		house.owner.endOfLettingAgreement(house, rentalContract);		
	}
	
	/********************************************************
	 * Do all the stuff necessary when this household moves
	 * in to rented accommodation (i.e. set up a regular
	 * payment contract. At present we use a MortgageApproval).
	 ********************************************************/
	public void completeHouseRental(HouseSaleRecord sale) {
		if(sale.house.owner != this) { // if renting own house, no need for contract
			RentalAgreement rent = new RentalAgreement();
			rent.monthlyPayment = sale.getPrice();
			rent.nPayments = data.HouseRentalMarket.AVERAGE_TENANCY_LENGTH + rand.nextInt(13) - 6;
//			rent.principal = rent.monthlyPayment*rent.nPayments;
			housePayments.put(sale.house, rent);
		}
		if(home != null) System.out.println("Strange: I'm renting a house but not homeless");
		home = sale.house;
		if(sale.house.resident != null) {
			System.out.println("Strange: tennant moving into an occupied house");
			if(sale.house.resident == this) System.out.println("...It's me!");
			if(sale.house.owner == this) System.out.println("...It's my house!");
			if(sale.house.owner == sale.house.resident) System.out.println("...It's a homeowner!");
		}
		sale.house.resident = this;
	}


	/////////////////////////////////////////////////////////
	// Homeowner helper stuff
	/////////////////////////////////////////////////////////

	/****************************************
	 * Put a bid on the housing market if this household can afford a
	 * mortgage at its desired price.
	 * 
	 * @param p The probability that the household will actually bid,
	 * given that it can afford a mortgage.
	 ****************************************/
	protected void bidOnHousingMarket(double p) {
		double desiredPrice = behaviour.desiredPurchasePrice(getMonthlyPreTaxIncome(), houseMarket.housePriceAppreciation());
		double maxMortgage = bank.getMaxMortgage(this, true);
//		double ltiConstraint =  annualEmploymentIncome * bank.loanToIncome(isFirstTimeBuyer(),true)/bank.loanToValue(isFirstTimeBuyer(), true); // ##### TEST #####
//		if(desiredPrice > ltiConstraint) desiredPrice = ltiConstraint - 1.0; // ##### TEST #####
		if(desiredPrice >= maxMortgage) desiredPrice = maxMortgage - 1;
//		desiredPrice = maxMortgage-1; // ####################### TEST!!!!!
		if(desiredPrice <= maxMortgage) {
			if(p<1.0) {
				if(rand.nextDouble() < p) houseMarket.bid(this, desiredPrice);
			} else {
				// no need to call random if p = 1.0
				houseMarket.bid(this, desiredPrice);				
			}
		}
	}
	
	/********************************************************
	 * Make the decision whether to bid on the housing market when renting.
	 * This is an "intensity of choice" decision (sigma function)
	 * on the cost of renting compared to the cost of owning, with
	 * COST_OF_RENTING being an intrinsic psychological cost of not
	 * owning. 
	 ********************************************************/
	protected void decideToStopRenting() {
		double costOfRent;
		double housePrice = behaviour.desiredPurchasePrice(getMonthlyPreTaxIncome(), houseMarket.housePriceAppreciation());
		double maxMortgage = bank.getMaxMortgage(this, true);
//		double ltiConstraint =  annualEmploymentIncome * bank.loanToIncome(isFirstTimeBuyer(),true)/bank.loanToValue(isFirstTimeBuyer(), true); // ##### TEST #####
//		if(housePrice > ltiConstraint) housePrice = ltiConstraint - 1.0; // ##### TEST #####
		if(housePrice >= maxMortgage) housePrice = maxMortgage - 1.0;
		if(Model.housingMarket.getAverageSalePrice(0)*0.85 > housePrice) return; // I can't afford a house anyway
		if(home != null) {
			costOfRent = housePayments.get(home).monthlyPayment*12;
		} else {
			costOfRent = rentalMarket.averageSalePrice[0]*12;
		}
		if(behaviour.renterPurchaseDecision(this, housePrice, costOfRent)) {
			houseMarket.bid(this, housePrice);
		}
	}
	
	
	/********************************************************
	 * Decide whether to sell ones own house.
	 ********************************************************/
	private boolean decideToSellHouse(House h) {
		if(h == home) {
			return(behaviour.decideToSellHome(this));
		}
		return(behaviour.decideToSellInvestmentProperty(h, this));
	}

		
	/********************************************************
	 * Decide whether to buy a house as a buy-to-let investment
	 ********************************************************/
//	public boolean decideToBuyBuyToLet(House h, double price) {
//		return(behaviour.decideToBuyBuyToLet(h, this, price));
//	}

	/***
	 * Do stuff necessary when BTL investor lets out a rental
	 * property
	 */
	@Override
	public void completeHouseLet(HouseSaleRecord sale) {
		if(sale.house.isOnMarket()) {
			houseMarket.removeOffer(sale.house.getSaleRecord());
		}
		monthlyPropertyIncome += sale.getPrice();
	}

	public double buyToLetRent(House h) {
		return(behaviour.buyToLetRent(
				rentalMarket.getAverageSalePrice(h.getQuality()), 
				rentalMarket.averageDaysOnMarket,
				housePayments.get(h).monthlyPayment));
	}
	
	/////////////////////////////////////////////////////////
	// Helpers
	/////////////////////////////////////////////////////////


	public boolean isHomeowner() {
		if(home == null) return(false);
		return(home.owner == this);
	}

	public boolean isRenting() {
		if(home == null) return(false);
		return(home.owner != this);
	}

	public boolean isHomeless() {
		return(home == null);
	}

	public boolean isFirstTimeBuyer() {
		return isFirstTimeBuyer;
	}

	//////////////////////////////////////////////////////////////////
	// Fraction of property+financial wealth that I want to invest
	// in buy-to-let housing
	//////////////////////////////////////////////////////////////////
//	public void setDesiredPropertyInvestmentFraction(double val) {
//		this.desiredPropertyInvestmentFraction = val;
//	}

	/////////////////////////////////////////////////////////////////
	// Current valuation of buy-to-let properties, not including
	// houses up for sale.
	/////////////////////////////////////////////////////////////////
	public double getPropertyInvestmentValuation() {
		double valuation = 0.0;
		for(House h : housePayments.keySet()) {
			if(h.owner == this && h != home && !h.isOnMarket()) {
				valuation += houseMarket.getAverageSalePrice(h.getQuality());
			}
		}
		return(valuation);
	}
	
	///////////////////////////////////////////////////////////////
	// returns current desired cash value of buy-to-let property investment
	//////////////////////////////////////////////////////////////
//	public double getDesiredPropertyInvestmentValue() {
//		return(desiredPropertyInvestmentFraction * (getPropertyInvestmentValuation() + bankBalance));
//	}

	/***
	 * @return Number of properties this household currently has on the sale market
	 */
	public int nPropertiesForSale() {
		int n=0;
		for(House h : housePayments.keySet()) {
			if(h.isOnMarket()) ++n;
		}
		return(n);
	}
	
//	public boolean isCollectingRentFrom(House h) {
//		return(h.owner == this && h != home && h.resident != null);
//	}

	/**
	 * @return monthly disposable (i.e., after tax) income
	 */
	public double getMonthlyPostTaxIncome() {
		return getMonthlyPreTaxIncome() - (Model.government.incomeTaxDue(annualEmploymentIncome) + Model.government.class1NICsDue(annualEmploymentIncome)) / 12.0;
	}
	
	/**
	 * @return gross monthly total income
	 */
	public double getMonthlyPreTaxIncome() {
		double monthlyTotalIncome = ((annualEmploymentIncome/12.0) +
				monthlyPropertyIncome + bankBalance * RETURN_ON_FINANCIAL_WEALTH);
		return monthlyTotalIncome;
	}
	
	public int nInvestmentProperties() {
		return(housePayments.size()-1);
	}
	
	/***
	 * @return Current mark-to-market equity in this household's home.
	 */
	public double getHomeEquity() {
		if(!isHomeowner()) return(0.0);
		return(Model.housingMarket.getAverageSalePrice(home.getQuality()) - mortgageFor(home).principal);
	}
	
	public MortgageAgreement mortgageFor(House h) {
		PaymentAgreement payment = housePayments.get(h);
		if(payment instanceof MortgageAgreement) {
			return((MortgageAgreement)payment);
		}
		return(null);
	}
	
	///////////////////////////////////////////////
	
	public static double RETURN_ON_FINANCIAL_WEALTH = 0.002; // monthly percentage growth of financial investements

	HouseSaleMarket		houseMarket;
	HouseRentalMarket	rentalMarket;

	public double	 	annualEmploymentIncome;
	protected double 	bankBalance;
	protected House		home; // current home
	protected Map<House, PaymentAgreement> 		housePayments = new TreeMap<House, PaymentAgreement>(); // houses owned
	protected double	monthlyPropertyIncome;
	private boolean		isFirstTimeBuyer;
//	public	double		desiredPropertyInvestmentFraction;
	Bank				bank;
	public int		 	id;		// only to ensure deterministic execution
	protected MersenneTwisterFast 	rand;
	
	public Lifecycle	lifecycle;	// lifecycle plugin
	public HouseholdBehaviour behaviour;
	public int 			desiredQuality;
	
//	static Diagnostics	diagnostics = new Diagnostics(Model.households);
	static int		 id_pool;


}
