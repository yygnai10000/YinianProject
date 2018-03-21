package yinian.thread;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.model.User;

public class PictureVerifyThread extends Thread{

	private int eid;
	private String main;
	private String userid;
	private YinianDataProcess dataProcess = new YinianDataProcess();
	
	public PictureVerifyThread(String userid,int eid, String main) {
		this.eid = eid;
		this.main = main;
		this.userid=userid;
	}
	
	public void run() {
		//动态和图片保存成功并返回数据后再进行鉴黄过滤--by ylm
		//先查出当前动态包含的图片集
		List<Record> picList = Db.find("select pid,poriginal from pictures where peid=" + eid);
		if(!picList.isEmpty()) {
			//获取图片id和地址，处理成数组以便鉴黄和更新图片地址
			Long[] pidArr = new Long[picList.size()];
			String[] picArr = new String[picList.size()];
			for(int i=0 ; i<picList.size(); i++) {
				pidArr[i] = picList.get(i).getLong("pid");
				picArr[i] = picList.get(i).get("poriginal");
			}
			//图片鉴黄
			picArr = dataProcess.PictureVerifyNew(picArr);
			//被过滤的图片根据pid更新图片地址
			if(pidArr.length==picArr.length) {
				for(int i=0; i<picArr.length; i++) {
					if("".equals(picArr[i])) {
						switch (main) {
						case "0"://图片
							Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_pic.jpg' where pid=" + pidArr[i]);
							break;
						case "4"://视频
							Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_vedio.mp4' where pid=" + pidArr[i]);
							break;
						}
					}
				}
			}
		}		
	}
}
