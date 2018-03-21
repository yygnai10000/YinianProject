package yinian.controller;

import java.util.List;

import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.model.Picture;

public class NewDbFieldController extends Controller{
	public void setPictureField(){
		List<Record> list=Db.find("select groupid from groups where groupid<5000");
		for(Record r:list){
			int groupid=r.getLong("groupid").intValue();
			List<Record> eventList=Db.find("select eid,euserid from events where eGroupid="+groupid);
			for(Record e:eventList){
				List<Record> picList= Db.find("select pid from pictures where peid="+e.getLong("eid").intValue());
				for(Record p:picList){
					Picture pic=new Picture();
					pic.set("pid", p.getLong("pid").intValue());
					pic.set("pGroupid", groupid);
					pic.set("pUserid", e.getLong("euserid").intValue());
					pic.update();
				}
			}
		}
		System.out.println("ok");
	}
}
