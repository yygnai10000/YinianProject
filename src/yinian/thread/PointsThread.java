package yinian.thread;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class PointsThread extends Thread {

	private int type;
	private int count1=0;
	private int count2=0;
	private int count3=0;
	private int count4=0;
	private int count5=0;

	public PointsThread(int type) {
		this.type = type;
	}
	
	@Override
	public void run() {
		List<Record> picList = new ArrayList<>();
		switch (type) {
		case 0:
			picList = Db.find("SELECT DISTINCT puserid FROM pictures where puserid BETWEEN 1 and 800000 ORDER BY pUserid");
			for (Record record : picList) {
				count1 +=1;
				Integer userid = record.get("puserid");
				List<Record> picturesInfo = Db.find("select count(*) as number from events,pictures where euserid=" + userid + " and eid=peid and estatus=0 and pstatus=0");
				Long number = picturesInfo.get(0).get("number");
				if(number>=300) {
					int num = (int) Math.floor((number-300)/100);
					int point = (num+1)*10;
					String typeName = "老用户上传照片奖励积分";
					//service.addPoints(userid+"", point,typeName);
					System.out.println("userid=" + userid +"-----"+ "point=" + point);
				}
			}
			System.out.println(count1);
			break;
		case 1:
			picList = Db.find("SELECT DISTINCT puserid FROM pictures where puserid BETWEEN 800001 and 1600000 ORDER BY puserid");
			for (Record record : picList) {
				count2+=1;
				Integer userid = record.get("puserid");
				List<Record> picturesInfo = Db.find("select count(*) as number from events,pictures where euserid=" + userid + " and eid=peid and estatus=0 and pstatus=0");
				Long number = picturesInfo.get(0).get("number");
				if(number>=300) {
					int num = (int) Math.floor((number-300)/100);
					int point = (num+1)*10;
					String typeName = "老用户上传照片奖励积分";
					//service.addPoints(userid+"", point,typeName);
					System.out.println("userid=" + userid +"-----"+ "point=" + point);
				}
			}
			System.out.println(count2);
			break;
		case 2:
			picList = Db.find("SELECT DISTINCT puserid FROM pictures where puserid BETWEEN 1600001 and 2400000 ORDER BY puserid");
			for (Record record : picList) {
				count3+=1;
				Integer userid = record.get("pUserid");
				List<Record> picturesInfo = Db.find("select count(*) as number from events,pictures where euserid=" + userid + " and eid=peid and estatus=0 and pstatus=0");
				Long number = picturesInfo.get(0).get("number");
				if(number>=300) {
					int num = (int) Math.floor((number-300)/100);
					int point = (num+1)*10;
					String typeName = "老用户上传照片奖励积分";
					//service.addPoints(userid+"", point,typeName);
					System.out.println("userid=" + userid +"-----"+ "point=" + point);
				}
			}
			System.out.println(count3);
			break;
		case 3:
			picList = Db.find("SELECT DISTINCT puserid FROM pictures where puserid BETWEEN 2400001 and 3200000 ORDER BY puserid");
			for (Record record : picList) {
				count4+=4;
				Integer userid = record.get("puserid");
				List<Record> picturesInfo = Db.find("select count(*) as number from events,pictures where euserid=" + userid + " and eid=peid and estatus=0 and pstatus=0");
				Long number = picturesInfo.get(0).get("number");
				if(number>=300) {
					int num = (int) Math.floor((number-300)/100);
					int point = (num+1)*10;
					String typeName = "老用户上传照片奖励积分";
					//service.addPoints(userid+"", point,typeName);
					System.out.println("userid=" + userid +"-----"+ "point=" + point);
				}
			}
			System.out.println(count4);
			break;
		case 4:
			picList = Db.find("SELECT DISTINCT puserid FROM pictures where puserid>3200000 ORDER BY puserid");
			for (Record record : picList) {
				count5+=1;
				Integer userid = record.get("puserid");
				List<Record> picturesInfo = Db.find("select count(*) as number from events,pictures where euserid=" + userid + " and eid=peid and estatus=0 and pstatus=0");
				Long number = picturesInfo.get(0).get("number");
				if(number>=300) {
					int num = (int) Math.floor((number-300)/100);
					int point = (num+1)*10;
					String typeName = "老用户上传照片奖励积分";
					//service.addPoints(userid+"", point,typeName);
					System.out.println("userid=" + userid +"-----"+ "point=" + point);
				}
			}
			System.out.println(count4);
			break;
		default:
			break;
		}
	}
	
}
