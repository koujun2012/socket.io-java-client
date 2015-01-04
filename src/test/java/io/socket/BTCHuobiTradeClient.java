package io.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

class Parameter implements Comparable<Parameter>{
	public final String name;
	public final Object value;
	public final boolean includeSign;
	
	public Parameter(String name, Object value) {
		this.name = name;
		this.value = value;
		this.includeSign = true;
	}
	
	public Parameter(String name, Object value, boolean includeSign) {
		this.name = name;
		this.value = value;
		this.includeSign = includeSign;
	}


	@Override
	public int compareTo(Parameter o) {
		return this.name.compareTo(o.name);
	}
	
}
public class BTCHuobiTradeClient {
	private String access_key;
	private String secret_key;
	private static final String ENDPOINT="https://api.huobi.com/apiv2.php";
	private static final Logger logger = Logger.getLogger(BTCHuobiTradeClient.class);
	public BTCHuobiTradeClient(String access_key, String secret_key) {
		this.access_key = access_key;
		this.secret_key = secret_key;
	}
	
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		BTCHuobiTradeClient client = new BTCHuobiTradeClient(args[0],args[1]);
		System.out.println(client.getBalance());
		System.out.println(client.getOrders());
//		System.out.println(client.tradeView(client.getOrderIDByTradeID("00003")));
		
//		System.out.println(client.getNewDealOrders());
//		System.out.println(client.tradeView("88084892"));
//		System.out.println(client.tradeAdd(0.01,1931,"sell"));
//		System.out.println(client.tradeAdd(0.01,1958.99,"sell","00003"));
//		System.out.println(client.tradeModify(client.getOrderIDByTradeID("00003"),0.01,1959));//NG
//		System.out.println(client.tradeAdd(0.001,1929.71,"buy"));
//		System.out.println(client.tradeAdd(10,0,"buy"));
//		System.out.println(client.tradeCancel(client.getOrderIDByTradeID("00003")));
		
	}
	
	private String request(String method, List<Parameter> parameters) throws Exception {
		if(parameters==null){
			parameters = new ArrayList<>();
		}
		parameters.add(new Parameter("method", method));
		parameters.add(new Parameter("access_key", this.access_key));
		parameters.add(new Parameter("created", System.currentTimeMillis()/1000));
		parameters.add(new Parameter("secret_key", this.secret_key));
		
		Collections.sort(parameters);
		StringBuffer buffer = null;
		for (Parameter p : parameters) {
			
			if(p.includeSign  == false) continue;
			
			if (buffer == null) {
				buffer = new StringBuffer();
			} else {
				buffer.append("&");
			}
			buffer.append(p.name + "=" + p.value);
		}
		
		String signature = getSignature(buffer.toString(), secret_key);
		
		buffer = null;
		for (Parameter p : parameters) {
			if(p.name.equals("secret_key") == true) continue;
			if (buffer == null) {
				buffer = new StringBuffer();
			} else {
				buffer.append("&");
			}
			buffer.append(p.name + "=" + p.value);
		}
		
		buffer.append("&sign="+signature);
		logger.debug("Request >> " + buffer.toString());
		URL url = new URL(ENDPOINT);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		connection.setDoOutput(true);
		PrintWriter wr = new PrintWriter(connection.getOutputStream());
		wr.print(buffer.toString());
		wr.flush();
		wr.close();
		
		InputStream inputStream = connection.getInputStream();
		String string = inputStreamToStringBuffer(inputStream).toString();
		logger.debug("Response << " + string);
		return string;
	}

	public JSONObject getBalance() throws Exception{
		
		String response =  request("get_account_info",null);
		return new JSONObject(response);
	}
	
	
	
	public String getOrderIDByTradeID(String tradeID) throws Exception{
		
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("trade_id",tradeID,true));
		p.add(new Parameter("coin_type",1));
		
		String response =  request("get_order_id_by_trade_id",p);
		
		JSONObject jsonObject = new JSONObject(response.toString());
		if(jsonObject.has("order_id") == true){
			return jsonObject.getString("order_id");
		}
		return null;
	}
	
	public String tradeAdd(double amount, double price, String type) throws Exception {
		return tradeAdd(amount, price, type, null);
	}
	
	public String tradeAdd(double amount,double price,String type,String tradeID) throws Exception{
//		if (amount < 0.01) {
//			throw new Exception("Too small for the number of trading");
//		}
		
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("amount",amount));
		if(price == 0){
			if(type.equals("buy")== true){
				type= "buy_market";
			}
			if(type.equals("sell")== true){
				type= "sell_market";
			}
		}else {
			p.add(new Parameter("price",price));
		}
		p.add(new Parameter("coin_type",1));
		if (tradeID != null) {
			p.add(new Parameter("trade_id", tradeID, false));
		}
		
		String response =  request(type,p);
		
		JSONObject jsonObject = new JSONObject(response.toString());
		if("success".equals(jsonObject.getString("result")) == true){
			return jsonObject.getString("id");
		}
		return null;
	}
	
	public String tradeModify(String id,double amount,double price) throws Exception{
//		if (amount < 0.01) {
//			throw new Exception("Too small for the number of trading");
//		}
		
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("id",id));
		p.add(new Parameter("amount",amount));
//		if(price == 0){
//			if(type.equals("buy")== true){
//				type= "buy_market";
//			}
//			if(type.equals("sell")== true){
//				type= "sell_market";
//			}
//		}else {
			p.add(new Parameter("price",price));
//		}
		p.add(new Parameter("coin_type",1));
		
		String response =  request("modify_order",p);
		
		JSONObject jsonObject = new JSONObject(response.toString());
		if("success".equals(jsonObject.getString("result")) == true){
			return jsonObject.getString("id");
		}else{
			logger.warn(jsonObject.getString("msg"));
		}
		return null;
	}
	
	public boolean tradeCancel(String id ) throws Exception{
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("id",id));
		p.add(new Parameter("coin_type",1));
		
		String response =  request("cancel_order",p);
		JSONObject jsonObject = new JSONObject(response);
		
		if("success".equals(jsonObject.getString("result")) == false){
			logger.warn(jsonObject.getString("msg"));
		}
		return "success".equals(jsonObject.getString("result")) == true;
	}
	
	public JSONArray getOrders() throws Exception{
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("coin_type",1));
		String response =  request("get_orders",p);
		JSONArray jsonObject = new JSONArray(response);
//		[
//		  {
//		    "id": 88083538,
//		    "order_amount": "0.0010",
//		    "processed_amount": "0.0000",
//		    "order_time": 1420035908,
//		    "type": 2,
//		    "order_price": "1930.29"
//		  }
//		]
		return jsonObject;
	}
	public JSONArray getNewDealOrders() throws Exception{
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("coin_type",1));
		String response =  request("get_new_deal_orders",p);
		JSONArray jsonObject = new JSONArray(response);
//		[
//		  {
//		    "id": 88083538,
//		    "order_amount": "0.0010",
//		    "processed_amount": "0.0000",
//		    "order_time": 1420035908,
//		    "type": 2,
//		    "order_price": "1930.29"
//		  }
//		]
		return jsonObject;
	}
	
	public JSONObject tradeView(String id ) throws Exception{
		
		List<Parameter> p = new ArrayList<>();
		p.add(new Parameter("coin_type",1));
		p.add(new Parameter("id",id));
		String response =  request("order_info",p);
//		{
//			  "fee": "0.00",
//			  "total": "1.93",
//			  "id": 88083538,
//			  "order_amount": "0.0010",
//			  "processed_amount": "0.0010",
//			  "status": 2,
//			  "vot": "1.93",
//			  "processed_price": "1930.29",
//			  "type": 2,
//			  "order_price": "1930.29"
//			}
		JSONObject jsonObject = new JSONObject(response);
		return jsonObject;
	}

	protected String getSignature(String data, String key) throws Exception {
		String md5Key = MD5Util.MD5(data);
		// get an hmac_sha1 key from the raw key bytes
		return md5Key;
	}

	public JSONObject getTicker() throws Exception {
		URL url = new URL("https://www.btcbox.co.jp/api/v1/ticker/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		StringBuffer resonse = inputStreamToStringBuffer(connection.getInputStream());
		return new JSONObject(resonse.toString());
	}
	
	public static StringBuffer inputStreamToStringBuffer(InputStream in) throws IOException{
		BufferedReader reader= new BufferedReader(new InputStreamReader(in));
		StringBuffer response = new StringBuffer();
		while(true){
			String line = reader.readLine();
			if(line== null){
				break;
			}
			response.append(line);
		}
		return response;
		
	}
	
}
