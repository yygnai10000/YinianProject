package yinian.utils;

import java.util.List;

import yinian.model.JsonBean;

import com.jfinal.kit.JsonKit;
import com.jfinal.plugin.activerecord.Record;

public class JsonData {
	
	public String getSuccessJson(){
		String jsonString ="";
		JsonBean jb = new JsonBean();
		jb.setCode(0);
		jb.setMsg("success");
		jsonString += JsonKit.toJson(jb);
		return jsonString;
	}
	
	public String getSuccessJson(List<Record> list){
		String jsonString ="";
		JsonBean jb = new JsonBean();
		jb.setCode(0);
		jb.setMsg("success");
		jb.setData(list);
		jsonString += JsonKit.toJson(jb);
		return jsonString;
	}
	
	public String getJson(int code,String msg){
		String jsonString ="";
		JsonBean jb = new JsonBean();
		jb.setCode(code);
		jb.setMsg(msg);
		jsonString += JsonKit.toJson(jb);
		return jsonString;
	}
	
	public String getJson(int code,String msg,List<Record> list){
		String jsonString ="";
		JsonBean jb = new JsonBean();
		jb.setCode(code);
		jb.setMsg(msg);
		jb.setData(list);
		jsonString += JsonKit.toJson(jb);
		return jsonString;
	}
	
	
}
