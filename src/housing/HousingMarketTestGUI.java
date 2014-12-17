package housing;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

//import org.jfree.chart.ChartUtilities;
//import org.jfree.data.xy.XYSeries;

import sim.display.Console;
import sim.display.Controller;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.ScatterPlotGenerator;
import sim.util.media.chart.ScatterPlotSeriesAttributes;
import sim.display.ChartUtilities;

/********************************************
 * Mason Graphic Interface for the housing market simulation.
 * 
 * @author daniel
 *
 ********************************************/
@SuppressWarnings("serial")
public class HousingMarketTestGUI extends GUIState implements Steppable {

    private javax.swing.JFrame myChartFrame;
    
    // Chart generators

	public ScatterPlotGenerator
		housingChart,
		housePriceChart,
		bankBalanceChart,
		mortgageStatsChart,
		mortgagePhaseChart;
            
    protected ArrayList<TimeSeriesPlot> timeSeriesPlots;
    
    /** Create an instance of MySecondModelGUI */
    HousingMarketTestGUI() { 
        super(new HousingMarketTest(1L));
        timeSeriesPlots = new ArrayList<TimeSeriesPlot>();
    }

    
    public void load(final SimState state) {
    	super.start();
    }
        
    /** Called once, when the simulation is to begin. */
    @Override
    public void start() {
        super.start();
        Household.diagnostics.init();
        
        addSeries(housingChart, "Homeless", Household.diagnostics.homelessData);
        addSeries(housingChart, "Renting", Household.diagnostics.rentingData);
        addSeries(housePriceChart, "Modelled prices", HousingMarketTest.housingMarket.diagnostics.priceData);
        addSeries(bankBalanceChart, "Bank balances", Household.diagnostics.bankBalData);
        addSeries(mortgageStatsChart, "LTV distribution", HousingMarketTest.bank.diagnostics.ltv_distribution);
        addSeries(mortgageStatsChart, "LTI distribution", HousingMarketTest.bank.diagnostics.lti_distribution);
        addSeries(mortgagePhaseChart, "Approved mortgages", HousingMarketTest.bank.diagnostics.approved_mortgages);
        
        housePriceChart.addSeries(HousingMarketTest.housingMarket.diagnostics.referencePriceData, "Reference price", null);
        bankBalanceChart.addSeries(Household.diagnostics.referenceBankBalData, "Reference bank balance", null);

        // Execute when each time-step is complete
        scheduleRepeatingImmediatelyAfter(this);
    }
    
    /** Called after each simulation step. */
    @Override
    public void step(SimState state) {
        HousingMarketTest myModel = (HousingMarketTest)state;
        double t = myModel.schedule.getTime()/12.0;
        
        for(TimeSeriesPlot plot : timeSeriesPlots) {
        	plot.recordValues(t);
        }
        
        Household.diagnostics.step();
        HousingMarketTest.bank.diagnostics.step();
        HousingMarketTest.housingMarket.diagnostics.step();
    }
    
    
    /** Called once, to initialise display windows. */
    @Override
    public void init(Controller controller) {
        super.init(controller);
        myChartFrame = new JFrame("My Graphs");
                		
        controller.registerFrame(myChartFrame);
       
        // Create a tab interface
        JTabbedPane newTabPane = new JTabbedPane();
        housingChart = makeScatterPlot(newTabPane, "Housing stats", "Probability", "Household Income");
        housePriceChart = makeScatterPlot(newTabPane, "House prices", "Modelled Price", "Reference Price");
        bankBalanceChart = makeScatterPlot(newTabPane, "Bank balances", "Balance", "Income");
        mortgageStatsChart = makeScatterPlot(newTabPane, "Mortgage stats", "Frequency", "Ratio");
        mortgagePhaseChart = makeScatterPlot(newTabPane, "Mortgage phase",  "Deposit/income", "Loan/income");
        mortgagePhaseChart.setXAxisRange(0.0, 8.0);
        mortgagePhaseChart.setYAxisRange(0.0, 8.0);
        
        timeSeriesPlots.add(
        		new TimeSeriesPlot("Market Statistics","Time (years)","Value")
        			.addVariable(HousingMarketTest.housingMarket,"housePriceIndex", "HPI")
        			.addVariable(HousingMarketTest.housingMarket.diagnostics,"averageSoldPriceToOLP", "Sold Price/List price")
        			.addVariable(HousingMarketTest.housingMarket,"averageDaysOnMarket", "Years on market", new DataRecorder.Transform() {
        				public double exec(double x) {return(Math.min(x/360.0, 1.0));}
        			})
    	);

        timeSeriesPlots.add(
        		new TimeSeriesPlot("Bid/Offer quantities","Time (years)","Number")
        			.addVariable(HousingMarketTest.housingMarket.diagnostics,"nSales", "Transactions")
        			.addVariable(HousingMarketTest.housingMarket.diagnostics,"nSellers", "Sellers")
        			.addVariable(HousingMarketTest.housingMarket.diagnostics,"nBuyers", "Buyers")
    	);
        
        timeSeriesPlots.add(
        		new TimeSeriesPlot("Bid/Offer Prices","Time (years)","Price")
        			.addVariable(HousingMarketTest.housingMarket.diagnostics,"averageBidPrice", "Average Bid Price")
        			.addVariable(HousingMarketTest.housingMarket.diagnostics,"averageOfferPrice", "Average Offer Price")        			
        );

        timeSeriesPlots.add(
        		new TimeSeriesPlot("Affordability","Time (years)","mortgage payment/income")
        			.addVariable(HousingMarketTest.bank.diagnostics,"affordability", "Affordability")
    	);
        
        for(TimeSeriesPlot plot : timeSeriesPlots) {
        	plot.addToPane(newTabPane);
        }
        
        myChartFrame.add(newTabPane);
        
        myChartFrame.pack();
    }
    
    /** Add titles and labels to charts. */

    private ScatterPlotGenerator makeScatterPlot(JTabbedPane pane, String title, String xAxis, String yAxis) {
    	ScatterPlotGenerator chart = ChartUtilities.buildScatterPlotGenerator(title, xAxis, yAxis);
        pane.addTab(title, chart);
        return(chart);
    }
    
    private void addSeries(ScatterPlotGenerator chart, String title, final double[][] data) {
    	ScatterPlotSeriesAttributes attributes = ChartUtilities.addSeries(chart, title);
        ChartUtilities.scheduleSeries(this, attributes, new sim.display.ChartUtilities.ProvidesDoubleDoubles() {
        	public double[][] provide() {
        		return(data);
        	}
        });
    }
    
    /** Called once, when the console quits. */
    @Override
   public void quit() {
        super.quit();
        myChartFrame.dispose();
    }

    // Java entry point
    public static void main(String[] args) {
        // Create a console for the GUI
        Console console = new Console(new HousingMarketTestGUI());
        console.setVisible(true);
    }

    ////////////////////////////////////////////////////////////////////
    // Console stuff
    ////////////////////////////////////////////////////////////////////
	@Override
	public Object getSimulationInspectedObject() {
		return state;
	}

}
