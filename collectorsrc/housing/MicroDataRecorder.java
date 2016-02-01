package housing;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.apache.commons.math3.linear.ArrayRealVector;

public class MicroDataRecorder {

	public void start() throws FileNotFoundException, UnsupportedEncodingException {
		openNewFile();
	}

	public void openNewFile() {
		String simID = Integer.toHexString(UUID.randomUUID().hashCode());
		try {
			outfile = new PrintWriter("transactions-"+simID+".csv", "UTF-8");
			outfile.println(
					"Timestamp, transactionType, houseId, houseQuality, initialListedPrice, timeFirstOffered, transactionPrice, "+
					"buyerId, buyerHasBTLGene, buyerMonthlyPreTaxIncome, buyerMonthlyEmploymentIncome, buyerBankBalance, "+
					"mortgageDownpayment, firstTimeBuyerMortgage, buyToLetMortgage, "+
					"sellerId, sellerHasBTLGene, sellerMonthlyPreTaxIncome, sellerMonthlyEmploymentIncome, sellerBankBalance");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void recordSale(HouseBuyerRecord purchase, HouseSaleRecord sale, MortgageAgreement mortgage, HousingMarket market) {
		if(!active) return;
		outfile.print(
    			Model.getTime()+", "
    			);
		if(market instanceof HouseSaleMarket) {
			outfile.print("sale, ");
		} else {
			outfile.print("rental, ");
		}
		outfile.print(
    			sale.house.id+", "+
    			sale.house.getQuality()+", "+
    			sale.initialListedPrice+", "+
    			sale.tInitialListing+", "+
    			sale.getPrice()+", "+
    			purchase.buyer.id+", "+
    			purchase.buyer.behaviour.isPropertyInvestor()+", "+
    			purchase.buyer.getMonthlyPreTaxIncome()+", "+
    			purchase.buyer.monthlyEmploymentIncome+", "+
    			purchase.buyer.bankBalance+", "
				);
		if(mortgage != null) {
			outfile.print(
					mortgage.downPayment+", "+
					mortgage.isFirstTimeBuyer+", "+
					mortgage.isBuyToLet+", "
					);			
		} else {
			outfile.print("-1, false, false, ");
		}
		if(sale.house.owner instanceof Household) {
			Household seller = (Household)sale.house.owner;
			outfile.println(
					seller.id+", "+
					seller.behaviour.isPropertyInvestor()+", "+
					seller.getMonthlyPreTaxIncome()+", "+
					seller.monthlyEmploymentIncome+", "+
					seller.bankBalance
					);			
		} else {
			// must be construction sector
			outfile.println("-1, false, 0, 0, 0");
		}
	}
	
	public void finish() {
		outfile.close();
	}
		
	public void endOfSim() {
		outfile.close();
		openNewFile();
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean isActive) {
		this.active = isActive;
		if(isActive) {
			try {
				Model.collectors.housingMarketStats.setActive(true);
				Model.collectors.rentalMarketStats.setActive(true);
				start();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			finish();
		}

	}

	PrintWriter 	outfile;
	public boolean  active=false;
}
