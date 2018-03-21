package yinian.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;

import yinian.interceptor.CrossDomain;
import yinian.utils.JsonData;
import yinian.utils.QiNiuZipUtil;

public class PcController  extends Controller{
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	
	/*
	 * 七牛云打包下载
	 */
	@Before(CrossDomain.class)
	public void makeZip(){
		List<Record> list=new ArrayList<Record>();
		jsonString=jsonData.getJson(2, "参数错误",list);
		String pictures=this.getPara("pictures");
		if(null!=pictures&&!pictures.equals("")){
			String[] picArray=pictures.split(",");
//			String downUrl=;
//			if(null!=downUrl&&!downUrl.equals("")){
//				Record r=new Record();
//				r.set("downUrl", downUrl);
//				list.add(r);
				jsonString=jsonData.getSuccessJson(new QiNiuZipUtil().mkzip(picArray));
		//	}	
		}	
		renderText(jsonString);
	}
	@Before(CrossDomain.class)
	public void check(){
		String id=this.getPara("id");
		String value=sentNetworkRequest("http://api.qiniu.com/status/get/prefop?id="+id);
//		List<Record> list=new ArrayList<Record>();
//		jsonString=jsonData.getJson(2, "参数错误",list);
//		String pictures=this.getPara("pictures");
//		if(null!=pictures&&!pictures.equals("")){
//			String[] picArray=pictures.split(",");
////			String downUrl=;
////			if(null!=downUrl&&!downUrl.equals("")){
////				Record r=new Record();
////				r.set("downUrl", downUrl);
////				list.add(r);
//				jsonString=jsonData.getSuccessJson(new QiNiuZipUtil().mkzip(picArray));
//		//	}	
//		}	
		renderText(value);
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
