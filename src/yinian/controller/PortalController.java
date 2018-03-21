package yinian.controller;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.interceptor.CrossDomain;
import yinian.model.Article;
import yinian.utils.JsonData;

public class PortalController extends Controller{
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	/**
	 * 发布文章
	 */
	@Before(CrossDomain.class)
	public void publish() {
		String title=this.getPara("title");
		String description=this.getPara("description");
		String source=this.getPara("source");
		String thumbnail=this.getPara("thumbnail");
		String msg=this.getPara("msg");
		int isPublish=this.getParaToInt("isPublish");
		Article a=new Article();
		a.set("title", title).set("description", description).set("source", source)
		.set("thumbnail", thumbnail).set("msg", msg).set("is_publish", isPublish);
		if(a.save()){
			jsonString = jsonData.getSuccessJson();
		}else{
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		renderText(jsonString);
	}
	/*
	 * 修改文章内容
	 */
	@Before(CrossDomain.class)
	public void update() {
		String id=this.getPara("id");
		String title=this.getPara("title");
		String description=this.getPara("description");
		String source=this.getPara("source");
		String thumbnail=this.getPara("thumbnail");
		String msg=this.getPara("msg");
		int isPublish=this.getParaToInt("isPublish");
		Article a=new Article();
		a.set("title", title).set("description", description).set("source", source)
		.set("thumbnail", thumbnail).set("msg", msg).set("is_publish", isPublish).set("id", id);
		if(a.update()){
			jsonString = jsonData.getSuccessJson();
		}else{
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		renderText(jsonString);
	}
	/*
	 * 根据文章id获取内容
	 */
	@Before(CrossDomain.class)
	public void getMsgById() {
		int id=this.getParaToInt("id");		
		List<Record> list=Db.find("select * from portal_articles where id="+id);
		jsonString = jsonData.getSuccessJson(list);	
		renderText(jsonString);
	}
	/*
	 * 根据文章列表
	 */
	@Before(CrossDomain.class)
	public void getMsgList() {
		String page=this.getPara("page");
		String isPublish=this.getPara("isPublish");
		String conds="";
		int end=15;
		int begin=0;
		if(null==isPublish||isPublish.equals("")){
			conds=" and is_publish=0 ";
		}
		if((null!=page&&!page.equals(""))&&!page.equals("0")&&!page.equals("1")){
			begin=Integer.parseInt(page);
			begin=(begin-1)*end;
		}
		List<Record> list=Db.find("select * from portal_articles where 1 "+conds+" order by addtime desc limit "+begin+","+end );
		List<Record> cntList=Db.find("select count(*) cnt from portal_articles where 1 "+conds);
		List<Record> returnList=new ArrayList<Record>();
		Record va=new Record();
		if(null!=cntList&&!cntList.isEmpty()){
				va.set("cnt", cntList.get(0).get("cnt"));			
		}
		va.set("values", list);
		returnList.add(va);
		jsonString = jsonData.getSuccessJson(returnList);	
		renderText(jsonString);
	}
}
