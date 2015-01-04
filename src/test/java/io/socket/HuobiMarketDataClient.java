package io.socket;

/*
 * socket.io-java-client Test.java
 *
 * Copyright (c) 2012, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
import java.text.SimpleDateFormat;
import java.util.Date;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HuobiMarketDataClient implements IOCallback {
	private static final String huobiURL = "http://hq.huobi.com:80/";
	private final SocketIO socket;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new HuobiMarketDataClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HuobiMarketDataClient() throws Exception {
		socket = new SocketIO();
		socket.connect( huobiURL , this);
	}

	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		try {
			System.out.println("Server said:" + json.toString(2));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack) {
		System.out.println("Server said: " + data);
	}

	@Override
	public void onError(SocketIOException socketIOException) {
		System.out.println("an Error occured");
		socketIOException.printStackTrace();
	}

	@Override
	public void onDisconnect() {
		System.out.println("Connection terminated.");
	}

	@Override
	public void onConnect() {
		System.out.println("Connection established");
		try {
			JSONObject symbolList = new JSONObject();
			
//			symbolList.put("tradeDetail", new JSONArray().put(0, new JSONObject().put("symbolId", "btccny")));
//			symbolList.put("marketDetail", new JSONArray().put(0, new JSONObject().put("symbolId", "btccny")));
//			symbolList.put("marketOverview", new JSONArray().put(0, new JSONObject().put("symbolId", "btccny")));
//			symbolList.put("lastTimeLine", new JSONArray().put(0, new JSONObject().put("symbolId", "btccny")));
			symbolList.put("marketDepthTopShort", new JSONArray().put(0, new JSONObject().put("symbolId", "btccny")));
//			symbolList.put("marketDepth", new JSONArray().put(0, new JSONObject().put("symbolId", "btccny").put("percent", 10)));
			
			// Sends a JSON object to the server.
			socket.emit("request",new JSONObject().put("version", 1)
					.put("msgType", "reqMsgSubscribe")
					.put("symbolList", symbolList));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {
		try {
			JSONObject json =  (JSONObject)args[0];
			if("message".equals(event) == true){
				String msgType = json.getString("msgType");
				JSONObject payload = null;
				if(json.has("payload") == true){
					payload = json.getJSONObject("payload");
				}
				if( "tradeDetail".equals(msgType) == true){
					onTradeDetailUpdate(payload);
				}
				else if("marketDetail".equals(msgType) == true){
					System.out.println("marketDetail << "+ json);
				}
				else if("marketDepth".equals(msgType) == true){
					System.out.println("marketDepth << "+ payload);
				}
				else if("marketOverview".equals(msgType) == true){
					onMarketOverviewUpdate(payload);
				}
				else if("lastTimeLine".equals(msgType) == true){
					
					double lastPrx = payload.getDouble("priceLast");
					double volume = payload.getDouble("volume");
					double amount = payload.getDouble("amount");
					long count = payload.getLong("count");
					Date time = parseTime(payload.getLong("time"));
					
					System.out.println("lastTimeLine << "+ fmt.format(time)+"\t"+lastPrx+"\t"+volume+"\t"+amount+"\t"+count);
				}
				else if("marketStatic".equals(msgType) == true){
					System.out.println("marketStatic << "+ json);
				}
				else if("marketDepthTopShort".equals(msgType) == true){
					onMarketDepthUpdate(payload);
				}else {
					System.out.println(" Message << "+ json);
				}
			}else {
				System.out.println(event + " << "+ json.getJSONObject("payload"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void onMarketDepthUpdate(JSONObject payload) {
		System.out.println(payload);
		try {
			JSONArray askAmountArray = payload.getJSONArray("askAmount");
			JSONArray askPriceArray = payload.getJSONArray("askPrice");
			JSONArray bidAmountArray = payload.getJSONArray("bidAmount");
			JSONArray bidPriceArray = payload.getJSONArray("bidPrice");
			for (int i = askAmountArray.length()-1 ; i >= 0; i--) {
				System.out.print(askAmountArray.getDouble(i));
				System.out.println("\t"+askPriceArray.getDouble(i));
			}
			for (int i = 0; i < bidAmountArray.length(); i++) {
				System.out.print("\t\t\t"+bidPriceArray.getDouble(i));
				System.out.println("\t"+bidAmountArray.getDouble(i));
			}
			System.out.println("-----------------------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private void onMarketOverviewUpdate(JSONObject payload) {
//		{
//			  "payload": {
//			    "totalVolume": 7.198339248123412E7,
//			    "priceBid": 1917.22,
//			    "priceNew": 1917.22,
//			    "priceHigh": 1941.7,
//			    "symbolId": "btccny",
//			    "totalAmount": 37339.255300000405,
//			    "priceAsk": 1917.29,
//			    "priceLow": 1913.5,
//			    "priceOpen": 1929.4
//			  },
//			}	
		try {
			double totalVolume = payload.getDouble("totalVolume");
			double priceBid = payload.getDouble("priceBid");
			double priceNew = payload.getDouble("priceNew");
			double priceHigh = payload.getDouble("priceHigh");
			double totalAmount = payload.getDouble("totalAmount");
			double priceAsk = payload.getDouble("priceAsk");
			double priceLow = payload.getDouble("priceLow");
			double priceOpen = payload.getDouble("priceOpen");

			System.out.println(priceNew+"\t"+priceOpen + "\t" + priceHigh + "\t" + priceLow + "\t" + totalAmount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Date parseTime(long time) {
		return new Date(time * 1000);
	}
	
	SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
	public void onTradeDetailUpdate(JSONObject json){
		try {
			JSONArray timeArray = json.getJSONArray("time");
			JSONArray priceArray = json.getJSONArray("price");
			JSONArray amountArray = json.getJSONArray("amount");
			JSONArray directionArray = json.getJSONArray("direction");
			for (int i = 0; i < timeArray.length(); i++) {
				System.out.print(fmt.format(new Date(timeArray.getLong(i)*1000)));
				System.out.print("\t"+priceArray.getDouble(i));
				System.out.print("\t"+amountArray.getDouble(i));
				System.out.println("\t"+directionArray.getLong(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
