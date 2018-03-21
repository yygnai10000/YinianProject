package yinian.controller;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.interceptor.CrossDomain;
import yinian.service.SimplificationH5Service;
import yinian.service.YinianService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

public class SimplificationH5Controller extends Controller{
	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	/*
	 * GetGroupPhotoList ������ᡢ�û�id������ʽ����ѯʱ���ȡ��Ƭ�б�
	 */
	public void GetGroupPhotoList(){
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupid = this.getPara("groupid")==null?"":this.getPara("groupid");
		String orderBy=this.getPara("orderBy")==null?"desc":this.getPara("orderBy");
		String searchTime=this.getPara("searchTime")==null?"":this.getPara("searchTime");
		int pid=this.getPara("pid")==null?0:this.getParaToInt("pid");		
		String source = this.getPara("source");
		if(!groupid.equals("")&&!userid.equals("")){
			SimplificationH5Service service=new SimplificationH5Service();
			List<Record> list=service.getGroupPhotoList(groupid, userid, orderBy, searchTime,pid);
			jsonString = jsonData.getSuccessJson(list);			
		}
		renderText(jsonString);
	}
	
	/*
	 * GetGroupPhotoList ������ᡢ�û�id������ʽ����ѯʱ���ȡ��Ƭ�б�
	 */
	public void GetGroupPhotoListNew(){
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupid = this.getPara("groupid")==null?"":this.getPara("groupid");
		String orderBy=this.getPara("orderBy")==null?"desc":this.getPara("orderBy");
		String searchTime=this.getPara("searchTime")==null?"":this.getPara("searchTime");
		int pid=this.getPara("pid")==null?0:this.getParaToInt("pid");		
		String source = this.getPara("source");
		if(!groupid.equals("")&&!userid.equals("")){
			SimplificationH5Service service=new SimplificationH5Service();
			List<Record> list=service.getGroupPhotoListNew(groupid, userid, orderBy, searchTime,pid);
			jsonString = jsonData.getSuccessJson(list);			
		}
		renderText(jsonString);
	}
	/**
	 * ����ͼƬID���û�id�ж��ܷ�ɾ��ͼƬ�����û�ɾ����һ����̬�����е�ͼƬ��ɾ����̬
	 */
	public void deletePic(){
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pid = this.getPara("pid")==null?"":this.getPara("pid");
		String source = this.getPara("source");
		if(!pid.equals("")&&!userid.equals("")){
			SimplificationH5Service service=new SimplificationH5Service();
			List<Record> list=service.deletePic(userid, pid);
				jsonString = jsonData.getSuccessJson(list);			
		}
		System.out.println("jsonString:"+jsonString);
		renderText(jsonString);
	}
//	public void deletePic_old(){
//		jsonString = jsonData.getJson(2, "�����������");
//		String userid = this.getPara("userid")==null?"":this.getPara("userid");
//		String pid = this.getPara("pid")==null?"":this.getPara("pid");
//		String source = this.getPara("source");
//		if(!pid.equals("")&&!userid.equals("")){
//			SimplificationH5Service service=new SimplificationH5Service();
//			if(service.deletePic(userid, pid)){
//				jsonString = jsonData.getSuccessJson();	
//			}
//		}
//		renderText(jsonString);
//	}
	/*
	 * ͼƬ����Ƶ����
	 */
	public void shareImgOrMV(){
		String ids=this.getPara("ids");
		String userid=this.getPara("userid");
		if(ids!=null&&!ids.equals("")&&userid!=null&&!userid.equals("")){
			SimplificationH5Service service=new SimplificationH5Service();
			String shareId=service.addShare(ids, userid);
			List<Record> list=new ArrayList<>();
			Record r=new Record();
			r.set("shareId", shareId);
			list.add(r);
			jsonString = jsonData.getSuccessJson(list);	
		}else{
			jsonString = jsonData.getJson(2, "�����������");
		}
		renderText(jsonString);
	}
	/**
	 * ���ݷ���id��ȡͼƬ����Ƶ����
	 */
	public void getShareValue(){
		String id=this.getPara("id");
		if(id!=null){
			SimplificationH5Service service=new SimplificationH5Service();
			jsonString = jsonData.getSuccessJson(service.getShareValue(id));
		}else{
			jsonString = jsonData.getJson(2, "�����������");
		}
		renderText(jsonString);
	}
	/**
	 * ��ʾ��� ���ö� ����С����ר��
	 */
	@Before(CrossDomain.class)
	public void ShowGroupWithTop() {
		 YinianService service = new YinianService();
		// ��ȡ����
		int userid = Integer.parseInt(this.getPara("userid"));
		jsonString = service.simH5ShowGroupWithTop(userid);
		// ���ؽ��
		renderText(jsonString);
	}
	
	/**
	 * ��ʾ��� ���ö� ����С����ר��(����ҳ)
	 */
	@Before(CrossDomain.class)
	public void ShowGroupWithTopNew() {
		YinianService service = new YinianService();
		// ��ȡ����
		int userid = Integer.parseInt(this.getPara("userid"));
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		jsonString = service.simH5ShowGroupWithTopNew(userid,pagenum);
		// ���ؽ��
		renderText(jsonString);
	}
	/**
	 * ��ȡͬ���ռ��б����ų����ϴ�Ȩ�޿ռ� by lk ����С����ר��
	 */
	@Before(CrossDomain.class)
	public void GetSynchronizeSpaceList() {
		QiniuOperate qiniu = new QiniuOperate();
		String userid = this.getPara("userid");
		if(userid!=null&&!userid.equals("")){
		List<Record> list = Db.find(
				"select groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gstatus=0 and gmstatus=0 and gmuserid="
						+ userid + " and gtype!=5 and (gcreator=" + userid + " or (gcreator!=" + userid
						+ " and (gAuthority=0 or (gAuthority=2 and gmauthority=1) )))");
		for(Record r:list){ 
			//��ȡ��Ƶ���棬���û�з��棬���ȡ���һ���ϴ���ͼƬ
			//if(null==r.get("simAppPic")||r.get("simAppPic").equals("")){
				List<Record> eventList=Db.find("select eid from events where eMain=0 and estatus=0 and egroupid="+r.get("groupid").toString()+" order by euploadtime desc limit 0,1");
				if(!eventList.isEmpty()){
					List<Record> topPicList=Db.find("select poriginal from pictures where pstatus = 0 and peid ="+eventList.get(0).get("eid")+" order by puploadtime desc limit 0,1");
					if(!topPicList.isEmpty()){
						r.set("simAppPic",qiniu.getDownloadToken(topPicList.get(0).getStr("poriginal")+"?imageView2/2/w/600"));
					}else{
						r.set("simAppPic","http://oibl5dyji.bkt.clouddn.com/noSimAppPic.png");
					}
				}
				if(null==r.get("simAppPic")||r.get("simAppPic").toString().equals("")){
					r.set("simAppPic","http://oibl5dyji.bkt.clouddn.com/noSimAppPic.png");
				}
			//}
		}		
		jsonString = jsonData.getSuccessJson(list);
		}else{
			jsonString = jsonData.getJson(2, "�����������");
		}
		renderText(jsonString);
	}
}
