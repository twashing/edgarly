package com.interrupt.edgar;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ib.client.EWrapper;
import com.ib.client.EClientSocket;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.ContractDetails;
import com.ib.client.Execution;
import com.ib.client.CommissionReport;
import com.ib.client.UnderComp;

//import edgar.splitter;
import clojure.lang.RT;
import clojure.lang.Var;
import clojure.lang.IFn;


public class EWrapperImpl implements com.ib.client.EWrapper {


  /**
   * EWrapper members
   */
  protected EClientSocket client = null;
  protected Logger logger = null;

    protected final static String TWS_HOST = "localhost"; // "192.168.0.17"; //
  protected final static int TWS_PORT = 7497;  // 4001; //
  protected final static int TWS_CLIENT_ID = 1;
  protected final static int MAX_WAIT_COUNT = 15; // 15 secs
  protected final static int WAIT_TIME = 1000; // 1 sec


    protected IFn publishFn = null;

    public EWrapperImpl() {

	/**
	 * Load the namespace
	 * and find a function in the namespace
	 */
	RT.var("clojure.core","eval").invoke(RT.var("clojure.core","read-string").invoke("(use 'edgar.ib.market)"));
	publishFn = (IFn)RT.var("edgar.ib.market","publish-event-from-java");

	client = new EClientSocket(this);
	logger = Logger.getLogger("user");

    }


  protected void connectToTWS() {
    client.eConnect(TWS_HOST, TWS_PORT, TWS_CLIENT_ID);
  }
  protected void disconnectFromTWS() {
    if (client.isConnected()) client.eDisconnect();
  }

  protected Contract createContract(String symbol, String securityType,
                                    String exchange, String currency) {
      return createContract(symbol, securityType, exchange, currency, null, null, 0.0);
  }

  protected Contract createContract(String symbol, String securityType,
                                    String exchange, String currency,
                                    String expiry, String right, double strike) {
    Contract contract = new Contract();

    contract.m_symbol = symbol;
    contract.m_secType = securityType;
    contract.m_exchange = exchange;
    contract.m_currency = currency;

    if (expiry != null) contract.m_expiry = expiry;
    if (strike != 0.0) contract.m_strike = strike;
    if (right != null) contract.m_right = right;

    return contract;
  }



  /**
   * EWrapper interface functions
   */

  // Connection & Server
  public void currentTime (long time) {
    logger.debug("EWrapper.currentTime > time["+ time +"]");
  }
  public void error (int id, int errorCode, String errorString) {
    logger.error("EWrapper.error > id["+ id +"] > errorCode["+ errorCode +"] > errorString["+ errorString +"]");
  }
  public void error (Exception error) {
    logger.error("EWrapper.error[Exception] > error["+ error +"]");
    error.printStackTrace();
  }
  public void error (String error) {
    logger.error("EWrapper.error[String] > error["+ error +"]");
  }
  public void connectionClosed () {
    logger.debug("EWrapper.connectionClosed");
  }
  public void tickSnapshotEnd (int arg) {

    //logger.debug("EWrapper.tickSnapshotEnd > arg["+ arg +"]");

		Map tentry = new HashMap();
    tentry.put("tickerId", arg);
		tentry.put("type", "tickSnapshotEnd");


		/**
		 * Call that function
		 */
		Object result = publishFn.invoke(tentry);

  }

  // Market Data
  public void tickPrice (int tickerId, int field, double price, int canAutoExecute) {

    //logger.debug("");
    //logger.debug("EWrapper.tickPrice > field["+ field +"]");
    if (field == 1) {
      //logger.debug("case 1 > bid-price["+ price +"]");
    }
    else if (field == 2) {
      //logger.debug("case 2 > ask-price["+ price +"]");
    }
    else if (field == 4) {
      //logger.debug("case 4 > last-price["+ price +"]");
    }
    else if (field == 6) {
      //logger.debug("case 6 > high["+ price +"]");
    }
    else if (field == 7) {
      //logger.debug("case 7 > low["+ price +"]");
    }
    else if(field == 9) {
      //logger.debug("case 9 > close["+ price +"]");
    }
    else {
      //logger.debug("default > noop");
    }


		Map tentry = new HashMap();
    tentry.put("tickerId", tickerId);
		tentry.put("type", "tickPrice");
    tentry.put("field", field);
    tentry.put("price", price);
    tentry.put("canAutoExecute", canAutoExecute);
		//tentry.put("timeStamp", DateTimeZone.forID("America/New_York").getOffset(DateTime.now()));
		tentry.put("timeStamp", DateTime.now());   // IB Systems use GMT (UTC +/-0)

		//Call that function
		Object result = publishFn.invoke(tentry);

  }


  public void tickSize (int tickerId, int field, int size) {

    //logger.debug("EWrapper.tickSize > tickerId["+ tickerId +"] > field["+ field +"] > size["+ size +"]");
  }

  public void tickOptionComputation (int tickerId, int field, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {

    logger.debug( "EWrapper.tickOptionComputation > tickerId["+ tickerId
                        +"] > field["+ field +"] > impliedVol["+ impliedVol +"] > delta["+ delta +"] > optPrice["+ optPrice
                        +"] > pvDividend["+ pvDividend +"] > gamma["+ gamma +"] > vega["+ vega +"] > theta["+ theta +"] > undPrice["+ undPrice +"]");
  }

  public void tickGeneric (int tickerId, int tickType, double value) {
    logger.debug("EWrapper.tickGeneric > tickerId["+ tickerId +"] > tickType["+ tickType +"] > value["+ value +"]");
  }

  public void tickString (int tickerId, int tickType, String value) {

    logger.debug("EWrapper.tickString > tickerId["+ tickerId +"] > tickType["+ tickType +"] > value["+ value +"]");


		/**
		 * A tickType (field) of 48, is RTVolume, which has a format of:
		 *
		 * Last trade price / size / time / Total volume / vwap / single trade flag
		 * RTVolume=429.85;1;1367266029045;221114;429.05715837;true
		 */
		Map tentry = new HashMap();
    tentry.put("tickerId", tickerId);
		tentry.put("type", "tickString");
    tentry.put("tickType", tickType);
    tentry.put("value", value);


		//Call that function
		Object result = publishFn.invoke(tentry);

  }

  public void tickEFP ( int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture,
                        int holdDays, String futureExpiry, double dividendImpact, double dividendsToExpiry) {

    logger.debug( "EWrapper.tickEFP > tickerId["+ tickerId +"], tickType["+ tickType +"], basisPoints["+ basisPoints
                        +"], formattedBasisPoints["+ formattedBasisPoints +"], impliedFuture["+ impliedFuture +"], holdDays["+ holdDays
                        +"], futureExpiry["+ futureExpiry +"], dividendImpact["+ dividendImpact +"], dividendsToExpiry["+ dividendsToExpiry +"]");
  }



  // Orders
  public void orderStatus ( int orderId, String status, int filled, int remaining, double avgFillPrice, int permId,
                            int parentId, double lastFillPrice, int clientId, String whyHeld) {
    logger.debug("...");
  }
  public void openOrder (int orderId, Contract contract, Order order, OrderState orderState) {
    logger.debug("...");
  }
  public void nextValidId (int orderId) {
    logger.debug("...");
  }


  // Account & Portfolio
  public void updateAccountValue (String key, String value, String currency, String accountName) {
    logger.debug("...");
  }
  public void updatePortfolio (Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
    logger.debug("...");
  }
  public void updateAccountTime (String timeStamp) {
    logger.debug("...");
  }


  // Contract Details
  public void contractDetails (int reqId, ContractDetails contractDetails) {
    //logger.debug(String.format("EWrapper.contractDetails > reqId[%i] > contractDetails[%2]", reqId, contractDetails));
  }
  public void contractDetailsEnd (int reqId) {
    //logger.debug(String.format("EWrapper.contractDetailsEnd > reqId[%i]", reqId));
  }
  public void bondContractDetails (int reqId, ContractDetails contractDetails) {
    //logger.debug(String.format("EWrapper.bondContractDetails > reqId[%i] > contractDetails[%2]", reqId, contractDetails));
  }


  // Executions
  public void execDetails (int reqId, Contract contract, Execution execution) { logger.debug("..."); }

  // Market Depth
  public void updateMktDepth (int tickerId, int position, int operation, int side, double price, int size) {
    logger.debug("...");
  }
  public void updateMktDepthL2 (int tickerId, int position, String marketMaker, int operation, int side, double price, int size) {
    logger.debug("...");
  }

  // News Bulletins
  public void updateNewsBulletin (int msgId, int msgType, String message, String origExchange) {
    logger.debug("...");
  }

  // Financial Advisors
  public void managedAccounts (String accountsList) {
    logger.debug("...");
  }
  public void receiveFA (int faDataType, String xml) {
    logger.debug("...");
  }

  /* Historical Data
	 */
  public void historicalData (int reqId, String date, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps) {

    //logger.debug("EWrapper.historicalData > reqId["+ reqId +"] > date["+ date +"] > open["+ open +"] > high["+ high +"] > low["+ low +"] > close["+ close +"] > volume["+ volume +"] > count["+ count +"] > WAP["+ WAP +"] > hasGaps["+ hasGaps +"]");

		Map tentry = new HashMap();
    tentry.put("tickerId", reqId);
		tentry.put("type", "historicalData");
    tentry.put("field", "historicalData");
    tentry.put("date", date);
		tentry.put("open", open);
    tentry.put("close", close);
    tentry.put("high", high);
    tentry.put("low", low);
    tentry.put("volume", volume);
    tentry.put("count", count);
    tentry.put("WAP", WAP);
		tentry.put("hasGaps", hasGaps);

		/* Call that function
		 */
		Object result = publishFn.invoke(tentry);
		//logger.debug("EWrapper.historicalData: " + result);

  }

  // Market Scanners
  public void scannerParameters (String xml) {
    //logger.debug(String.format("EWrapper.scannerParameters > xml[%s]", xml));
  }
  public void scannerData (int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
    /*logger.debug(
      String.format("EWrapper.scannerData > reqId[%i] > rank[%i] > contractDetails[%3] > distance[%s] > benchmark[%s] > projection[%s] > legalStr[%s]",
       reqId, rank, contractDetails, distance, benchmark, projection, legsStr));
    */
  }


  // Real Time Bars
  public void realtimeBar (int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count) {
    logger.debug("...");
  }

  // Fundamental Data
  public void fundamentalData (int reqId, String data) {
    logger.debug("...");
  }

  // et al.
  public void openOrderEnd() { logger.debug("..."); }
  public void accountDownloadEnd(java.lang.String arg1) { logger.debug("..."); }
  public void execDetailsEnd(int arg1) { logger.debug("..."); }
  public void scannerDataEnd(int arg1) { logger.debug("..."); }
  public void deltaNeutralValidation(int arg1, com.ib.client.UnderComp arg2) { logger.debug("..."); }
  public void marketDataType(int arg1, int arg2) { logger.debug("..."); }
  public void commissionReport(com.ib.client.CommissionReport commisionReport) { logger.debug("..."); }

}
