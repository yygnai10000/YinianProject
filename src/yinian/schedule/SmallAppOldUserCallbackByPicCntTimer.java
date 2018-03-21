package yinian.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.model.FormID;
import yinian.push.SmallAppPush;

public class SmallAppOldUserCallbackByPicCntTimer extends TimerTask {
	int begin = 0;
	int end=0;
	SmallAppOldUserCallbackByPicCntTimer(int begin,int end) {
		this.begin = begin;
		this.end=end;
	}

	@Override
	public void run() {
		System.out.println("提示开始"+System.currentTimeMillis());
		// 先删除7天前的formid数据
		if (end <= 500000) {
			Db.update("delete from formid where time <DATE_ADD(Now(),INTERVAL -7 day)");
		}
//		System.out.println("sql="+"select * from formid where 1 and userID >"+begin+" and userID <="+end+" and status=0 and "
//				+ "time between DATE_ADD(Now(),INTERVAL -7 day) " + "and DATE_ADD(Now(),INTERVAL -24 hour) and "
//				+ "(userID not in (select userID from formid where time between "
//				+ "DATE_ADD(Now(),INTERVAL -24 hour) and Now() and status=0) ) " + "group by userID order by time asc");
//		System.out.println("sql="+"select * from formid where 1 and userID >"+begin+" and userID <="+end+" and status=0 and "
//				+ "time between DATE_ADD(Now(),INTERVAL -7 day) " + "and DATE_ADD(Now(),INTERVAL -24 hour) and "
//				+ "(userID not in (select userID from formid where time between "
//				+ "DATE_ADD(Now(),INTERVAL -24 hour) and Now() and status=0) ) " + "group by userID order by time asc");
		// 获取7天内之后一天formid 且不是今天的最早的数据 2
		List<Record> formidList = Db.find("select * from formid where 1 and userID >"+begin+" and userID <="+end+" and status=0 and "
				+ "time between DATE_ADD(Now(),INTERVAL -7 day) " + "and Now() "			
				+ " group by userID order by time asc");
//		List<Record> formidList = Db.find("select * from formid where 1 and status=0 and "
//				+ "time between DATE_ADD(Now(),INTERVAL -7 day) " + "and Now() "			
//				+ " group by userID order by time asc");
//		List<Record> formidList = Db.find("select * from formid where 1 and userID =129348 and status=0 and "
//				+ "time between DATE_ADD(Now(),INTERVAL -7 day) " + "and Now() "			
//				+ " group by userID order by time asc");
//		List<Record> formidList = Db.find("select * from formid where 1 and userID=1963837 and status=0 and "
//				+ "time between DATE_ADD(Now(),INTERVAL -7 day) " + "and DATE_ADD(Now(),INTERVAL -24 hour) and "
//				+ "(userID not in (select userID from formid where time between "
//				+ "DATE_ADD(Now(),INTERVAL -24 hour) and Now() and status=0) ) " + "group by userID order by time asc");
		int i = 0;
		int z = 0;
		List<Record> picCntList=Db.find("select * from groupPicCnt");
		
		for (Record fr : formidList) {
			//System.out.println("开始：" + System.currentTimeMillis());
			List<Record> groupList=Db.find("select group_concat(gmgroupid) gids from groupmembers where gmstatus=0 and gmuserid="+fr.get("userID"));
			i++;
			//System.out.println(begin+"   i"+i+"  userid=" + fr.get("userID"));
			
			String pubGroupid="";
			int pubCnt=0;
			String groupName="";
			if(null!=groupList&&!groupList.isEmpty()){
				for(Record picR:picCntList){	
					//System.out.println(groupList.get(0).get("gids").toString()+"      "+picR.get("groupid").toString());
					//if(null!=groupList.get(0).get("gids")&&null!=picR.get("groupid")&&groupList.get(0).get("gids").toString().indexOf(picR.get("groupid").toString())!=-1){
					if(null!=groupList.get(0).get("gids")&&null!=picR.get("groupid")&&useArraysBinarySearch(groupList.get(0).get("gids").toString().split(","),picR.get("groupid").toString())){
						if(Integer.parseInt(picR.get("pcnt").toString())>pubCnt){
							pubCnt=Integer.parseInt(picR.get("pcnt").toString());
							pubGroupid=picR.get("groupid").toString();
							groupName=picR.get("gname").toString();
						}
						//System.out.println(begin+"   i"+i+"  userid=" + fr.get("userID")+"   pubGroupid="+pubGroupid+"  pubCnt= "+pubCnt);
						z++;
						//System.out.println("z=" + z);
					}
				}
				if(pubCnt>0){
					List<Record> list=Db.find("select uopenid from users where userid="+fr.get("userID"));
					if(null!=list&&!list.isEmpty()){
						//if(fr.get("userID").toString().equals("1984228")){
							//推送
							SmallAppPush.callbackPushPicCnt(fr.get("formID"), list.get(0).get("uopenid"), pubCnt+"",pubGroupid,groupName);
							//删除formid
							//Db.update("delete from formid where id="+fr.get("id"));
							Db.update("delete from formid where userID="+fr.get("userID")+" and formID='"+fr.get("formID")+"'");
						//}
					}
				}
			}
		//	System.out.println("结束：" + System.currentTimeMillis());
			
//			// 获取每个用户所在空间最多的照片数
//		//	i++;
//			System.out.println("开始：" + System.currentTimeMillis());
//			List<Record> picList=Db.find("select groupid,max(pcnt) cnt from groupmembers,groupPicCnt where 1 and gmUserid="+fr.get("userID")+" and gmStatus=0 and gmGroupid=groupid" );
//				System.out.println("结束：" + System.currentTimeMillis());
//				if (null != picList && !picList.isEmpty()) {
//					Record pr = picList.get(0);
//					System.out.println("  userid=" + fr.get("userID") + "   picCnt=" + pr.get("cnt")
//							+ "   groupid=" + pr.get("groupid"));
//					if (null != pr.get("cnt")) {
//						z++;
//						System.out.println("z=" + z);
//						//推送
//						//删除formid
//						//Db.update("delete from formid where id="+fr.get("id"));
//					}
//				}		

		}
		System.out.println("提示结束"+System.currentTimeMillis());
		System.out.println(begin+"  进程结束： z=" + z);
	}
	public boolean useArraysBinarySearch(String[] arr,String targetValue){
		 for(String s:arr){
		        if(s.equals(targetValue))
		            return true;
		        }  
		        return false;
	}
}
