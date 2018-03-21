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
	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	/**
	 * ��������
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
			jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
		}
		renderText(jsonString);
	}
	/*
	 * �޸���������
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
			jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
		}
		renderText(jsonString);
	}
	/*
	 * ��������id��ȡ����
	 */
	@Before(CrossDomain.class)
	public void getMsgById() {
		int id=this.getParaToInt("id");		
		List<Record> list=Db.find("select * from portal_articles where id="+id);
		jsonString = jsonData.getSuccessJson(list);	
		renderText(jsonString);
	}
	/*
	 * ���������б�
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
