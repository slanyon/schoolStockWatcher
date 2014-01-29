package com.sampleapp.client;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;

import com.sampleapp.shared.FieldVerifier;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.rpc.AsyncCallback; 
import com.google.gwt.core.client.GWT; 


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {
	private static final int REFRESH_INTERVAL = 5000;
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable stocksFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addStockButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private ArrayList<String> stocks = new ArrayList<String>();
	private LoginInfo loginInfo = null;
	private VerticalPanel loginPanel = new VerticalPanel();
	private Label loginLabel = new Label("Please sign in to your Google Account to access the StockWatcher application.");
	private Anchor signInLink = new Anchor("Sign In");
	private Anchor signOutLink = new Anchor("Sign Out");
	private final StockServiceAsync stockService = GWT.create(StockService.class);
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		   // Check login status using login service.
	    LoginServiceAsync loginService = GWT.create(LoginService.class);
	    loginService.login(GWT.getHostPageBaseURL(), new AsyncCallback<LoginInfo>() {
	      public void onFailure(Throwable error) {
	      }

	      public void onSuccess(LoginInfo result) {
	        loginInfo = result;
	        if(loginInfo.isLoggedIn()) {
	        	loadStockWatcher();
	        } else {
	            loadLogin();
	          }
	        }
	      });
	    }

	    private void loadLogin() {
	      // Assemble login panel.
	      signInLink.setHref(loginInfo.getLoginUrl());
	      loginPanel.add(loginLabel);
	      loginPanel.add(signInLink);
	      RootPanel.get("stockList").add(loginPanel);
	    }
	
	private void loadStockWatcher(){
		signOutLink.setHref(loginInfo.getLogoutUrl());
		stocksFlexTable.setText(0, 0, "Symbols");
		stocksFlexTable.setText(0, 1, "Price");
		stocksFlexTable.setText(0, 2, "Change");
		stocksFlexTable.setText(0, 3, "Remove");
		stocksFlexTable.setCellPadding(6);
		stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		stocksFlexTable.addStyleName("watchList");
		stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");
		
		mainPanel.add(signOutLink);
		addPanel.add(newSymbolTextBox);
		addPanel.add(addStockButton);
		addPanel.addStyleName("addPanel");
		
		mainPanel.add(stocksFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);
		
		RootPanel.get("stockList").add(mainPanel);
		
		newSymbolTextBox.setFocus(true);
		addStockButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				addStock();
			}
		});
		
		newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER){
					addStock();
				}
			}
		});
		
		Timer refreshTimer = new Timer(){
			@Override
			public void run(){
				refereshWatchList();
			}	
		};
		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
		loadStocks();
	}
	

	private void addStock(){
		final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
		if(!symbol.matches("^[0-9A-Z\\.]{1,10}$")){
			Window.alert("'" + symbol + "' is not a valid symbol");
			newSymbolTextBox.setText("");
			return;
		}
		newSymbolTextBox.setText("");
		if(stocks.contains(symbol)){
			return;
		}
		addStock(symbol);
	}
	
	 private void addStock(final String symbol) {
		    stockService.addStock(symbol, new AsyncCallback<Void>() {
		      public void onFailure(Throwable error) {
		    	  handleError(error);
		      }
		      public void onSuccess(Void ignore) {
		        displayStock(symbol);
		      }
		    });
	 }
	
	private void loadStocks() {  
		stockService.getStocks(new AsyncCallback<String[]>() {  
			public void onFailure(Throwable error) { 
				handleError(error);
			}  
			public void onSuccess(String[] symbols) {  
				displayStocks(symbols);  
				}  
			});  
	}
	private void displayStocks(String[] symbols) {  
		for (String symbol : symbols) {  
			displayStock(symbol);  
		}  
	} 
	
	private void displayStock(final String symbol){
		int row = stocksFlexTable.getRowCount();
		stocks.add(symbol);
		
		stocksFlexTable.setText(row, 0, symbol);
		Button removeStockButton = new Button("x");
		removeStockButton.addStyleDependentName("remove");
		removeStockButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {					// TODO Auto-generated method stub
				removeStock(symbol);
			}
		});
		
		stocksFlexTable.setWidget(row, 3, removeStockButton);
		stocksFlexTable.setWidget(row, 2, new Label());
		stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
	    stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
	    stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
		
		refereshWatchList();
	}
	
	  private void removeStock(final String symbol) {
		    stockService.removeStock(symbol, new AsyncCallback<Void>() {
		      public void onFailure(Throwable error) {
		    	  handleError(error);
		      }
		      public void onSuccess(Void ignore) {
		        undisplayStock(symbol);
		      }
		    });
		  }

		  private void undisplayStock(String symbol) {
		    int removedIndex = stocks.indexOf(symbol);
		    stocks.remove(removedIndex);
		    stocksFlexTable.removeRow(removedIndex+1);
		  }
	
	private void refereshWatchList() {
		   final double MAX_PRICE = 100.0; // $100.00
		    final double MAX_PRICE_CHANGE = 0.02; // +/- 2%

		    StockPrice[] prices = new StockPrice[stocks.size()];
		    for (int i = 0; i < stocks.size(); i++) {
		      double price = Random.nextDouble() * MAX_PRICE;
		      double change = price * MAX_PRICE_CHANGE
		          * (Random.nextDouble() * 2.0 - 1.0);

		      prices[i] = new StockPrice(stocks.get(i), price, change);
		    }

		    updateTable(prices);
		
	}
	private void updateTable(StockPrice[] prices){ 
		for(int i=0; i < prices.length; i++){
			updateTable(prices[i]);
		}
		lastUpdatedLabel.setText("Last update : "  + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));
	}


	private void updateTable(StockPrice stockPrice) {
		if(!stocks.contains(stockPrice.getSymbol())){
			return;
		}
		int row = stocks.indexOf(stockPrice.getSymbol()) + 1;
		String priceText = NumberFormat.getFormat("#,##0.00").format(stockPrice.getPrice());
		NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
		String changeText = changeFormat.format(stockPrice.getChange());
		String changePercentageText = changeFormat.format(stockPrice.getChangePercent());
		
		stocksFlexTable.setText(row, 1, priceText);
		Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
	    changeWidget.setText(changeText + " (" + changePercentageText + "%)");
	    
	    String changeStyleName = "noChange";
	    if (stockPrice.getChangePercent() < -0.1f) {
	      changeStyleName = "negativeChange";
	    }
	    else if (stockPrice.getChangePercent() > 0.1f) {
	      changeStyleName = "positiveChange";
	    }

	    changeWidget.setStyleName(changeStyleName);
	}
	
	private void handleError(Throwable error) {
	    Window.alert(error.getMessage());
	    if (error instanceof NotLoggedInException) {
	      Window.Location.replace(loginInfo.getLogoutUrl());
	    }
	  }

}
