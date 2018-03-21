package yinian.service;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.common.CommonParam;
import yinian.model.GroupCanPublish;
import yinian.utils.JsonData;

public class AdvertisementService {
	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	public List<Record> getTimeAxisAdv(String groupid){
		List<Record> list=new ArrayList<Record>();				
		if(null!=groupid&&!groupid.equals("")){
			int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
			if(advertisementsShow==1){
				list=Db.find("select groupid,timeAxisImg img,timeAxisJoinListid listid,timeAxisJoinGroupid joinGroupid,timeAxisJoinListOrGroup joinGroupOrList from groupadvertisements where groupid="+groupid+" and status=0");								
			}
		}
		return list;
	}
	public List<Record> getTimeAxisAdvById(){
		List<Record> list=new ArrayList<Record>();				
		if(CommonParam.pOpenBanner){
			String id=CommonParam.pAdvId;
//			int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
//			if(advertisementsShow==1){
				list=Db.find("select groupid,timeAxisImg img,timeAxisJoinListid listid,timeAxisJoinGroupid joinGroupid,timeAxisJoinListOrGroup joinGroupOrList from groupadvertisements where id="+id+" and status=0");								
			//}
		}
		return list;
	}
	public List<Record> getAdvList(String listid){
		List<Record> list=new ArrayList<Record>();				
		if(null!=listid&&!listid.equals("")){			
				list=Db.find("select groupid,img,text from advertisementsmessage where advertisementsid="+listid+" and status=0");											
		}
		return list;
	}
}
