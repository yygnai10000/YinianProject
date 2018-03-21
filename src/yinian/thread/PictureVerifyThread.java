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
		//��̬��ͼƬ����ɹ����������ݺ��ٽ��м��ƹ���--by ylm
		//�Ȳ����ǰ��̬������ͼƬ��
		List<Record> picList = Db.find("select pid,poriginal from pictures where peid=" + eid);
		if(!picList.isEmpty()) {
			//��ȡͼƬid�͵�ַ������������Ա���ƺ͸���ͼƬ��ַ
			Long[] pidArr = new Long[picList.size()];
			String[] picArr = new String[picList.size()];
			for(int i=0 ; i<picList.size(); i++) {
				pidArr[i] = picList.get(i).getLong("pid");
				picArr[i] = picList.get(i).get("poriginal");
			}
			//ͼƬ����
			picArr = dataProcess.PictureVerifyNew(picArr);
			//�����˵�ͼƬ����pid����ͼƬ��ַ
			if(pidArr.length==picArr.length) {
				for(int i=0; i<picArr.length; i++) {
					if("".equals(picArr[i])) {
						switch (main) {
						case "0"://ͼƬ
							Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_pic.jpg' where pid=" + pidArr[i]);
							break;
						case "4"://��Ƶ
							Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_vedio.mp4' where pid=" + pidArr[i]);
							break;
						}
					}
				}
			}
		}		
	}
}
