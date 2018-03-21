package yinian.schedule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimerTask;

import com.alibaba.fastjson.JSONArray;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class PayPhoto extends TimerTask{
	public int money=100000;
	private int i;
	PayPhoto(int i){
		this.i=i;
	}
	@Override
	public void run() {
		System.out.println("开始");
		// TODO Auto-generated method stub
//		CanUserdMoney.INSTANCE.setMoney(money);
//		for(int z=0;z<20;z++){		
//		CanUserdMoney.INSTANCE.setMoney(CanUserdMoney.INSTANCE.getMoney()-100);
//		System.out.println("i="+i+"还剩："+CanUserdMoney.INSTANCE.getMoney());
//		}
		
		for(int z=0;z<20;z++){
			String money=sentNetworkRequest("http://localhost:8080/YinianProject/test/getAllMoney");
			if(null!=money){
				JSONObject j1=JSONObject.fromObject(money);
				net.sf.json.JSONArray a=net.sf.json.JSONArray.fromObject(j1.get("data"));
				JSONObject j2=JSONObject.fromObject(a.get(0));
				
				int m=Integer.parseInt(j2.get("money").toString());
				System.out.println("i="+i+"   z="+z+"剩余总金额"+m);
				sentNetworkRequest("http://localhost:8080/YinianProject/test/setAllMoney?userdMoney="+1);
				String money2=sentNetworkRequest("http://localhost:8080/YinianProject/test/getAllMoney");
				JSONObject j12=JSONObject.fromObject(money2);
				net.sf.json.JSONArray a1=net.sf.json.JSONArray.fromObject(j12.get("data"));
				JSONObject j22=JSONObject.fromObject(a1.get(0));
				
				int m2=Integer.parseInt(j22.get("money").toString());
				System.out.println("i="+i+"   z="+z+"使用后总金额"+m2);
			}
			//JSONObject j=JSONObject.fromObject(money);
			
//			CanUserdMoney.getInstance().setMoney(CanUserdMoney.getInstance().getMoney()-100);
//			System.out.println("i="+i+"   z="+z+"  还剩："+CanUserdMoney.getInstance().getMoney());
		}
	}
	 public String sentNetworkRequest(String url) {
	        String result = "";

	        try {
	            URL obj = new URL(url);
	            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	            con.setRequestMethod("POST");
	            con.setRequestProperty("accept", "*/*");
	            con.setDoOutput(true);
	            con.setDoInput(true);
	            con.connect();

	            InputStream input = con.getInputStream();
	            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
	            StringBuilder builder = new StringBuilder();
	            String line = null;
	            while ((line = reader.readLine()) != null) {
	                builder.append(line);
	            }
	            result = builder.toString();
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        return result;
	    }
}
