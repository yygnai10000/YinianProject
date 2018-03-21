package yinian.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.model.FormID;
import yinian.push.SmallAppPush;

public class SmallAppOldUserCallbackTimer extends TimerTask {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// ��ȡ��������
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String now = sdf.format(new Date());
		// ��ȡ���������������û�
//		System.out.println("select id,users.userid,formID,uopenid from users,formid where users.userid=formid.userID and status=0 and DATEDIFF('"
//						+ now + "',date(time))>5 and users.userid=2004306 "
//								+ " group by userid order by time asc  ");
		List<Record> list = Db.find(
				"select id,users.userid,formID,uopenid from users,formid where users.userid=formid.userID and status=0 and DATEDIFF('"
						+ now + "',date(time))>5 "
								+ " group by userid order by time asc  ");
		// �����������
		for (Record record : list) {
			String openid = record.get("uopenid");
			// openid���ڲŽ�������
			if (openid != null && !openid.equals("")) {
				String formID = record.get("formID").toString();
				SmallAppPush.callbackPush(formID, openid);
				String id = record.get("id").toString();
				new FormID().deleteById(id);
				Db.update("delete from formid where userID="+record.get("userid").toString()+" and formID='"+formID+"'");
			}
			
			//FormID form = new FormID().findById(id);
			//form.set("status", 1);			
			//form.update();
		}
	}

}
