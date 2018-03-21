package yinian.controller;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.GroupCanPublish;
import yinian.service.AdvertisementService;
import yinian.utils.JsonData;

public class AdvertisementController extends Controller{
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	/*
	 * 获取时间轴广告信息
	 */
	@Before(CrossDomain.class)
	public void getTimeAxisAdv(){
		AdvertisementService service=new AdvertisementService();
		List<Record> list=new ArrayList<Record>();		
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		String groupid=this.getPara("groupid");
		if(CommonParam.pOpenBanner){
			list=service.getTimeAxisAdvById();
			jsonString=jsonData.getSuccessJson(list);
			renderText(jsonString);
			return;
		}
		if(null!=groupid&&!groupid.equals("")){			
			list=service.getTimeAxisAdv(groupid);
			jsonString=jsonData.getSuccessJson(list);
		}		
		renderText(jsonString);
	}
	/*
	 * 广告列表
	 */
	@Before(CrossDomain.class)
	public void getAdvList(){
		AdvertisementService service=new AdvertisementService();
		List<Record> list=new ArrayList<Record>();		
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		String listid=this.getPara("listid");
		if(CommonParam.pOpenBanner){
			listid=CommonParam.pAdvListId;
		}
		if(null!=listid&&!listid.equals("")){			
			list=service.getAdvList(listid);
			jsonString=jsonData.getSuccessJson(list);
		}		
		renderText(jsonString);
	}
}
