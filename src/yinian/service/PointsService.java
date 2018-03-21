package yinian.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Encourage;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.Points;
import yinian.model.PointsReceive;
import yinian.model.PointsType;
import yinian.model.User;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.SendMessage;

public class PointsService {
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private String jsonString;// ���ؽ��
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����

	
	/**
	 * ��¼�ɹ��������
	 */
	public boolean recordSuccessInviteFriend(String userid) {
		Points points = new Points();
		List<Record> pointsInfo = Db.find("select * from points where puserid=" + userid);
		System.out.println(pointsInfo.size());
		if (pointsInfo.size() == 0) {
			// �����ݣ�����������
			points.set("puserid", userid).set("inviteNum", 1);
			return points.save();
		} else {
			// �����ݣ�����ԭ������
			String poid = pointsInfo.get(0).get("poid").toString();
			int inviteNum = Integer.parseInt(pointsInfo.get(0).get("inviteNum").toString());
			points = new Points().findById(poid).set("inviteNum", inviteNum + 1);
			return points.update();
		}
	}
	
	/**
	 * �ı���ֻ�ȡ״̬
	 */
	public boolean updatePointsStatus(String userid, int type) {
		PointsReceive pointsreceive = new PointsReceive();
		pointsreceive.set("puserid", userid).set("ptid", type).set("preceivestatus", 1);
		return pointsreceive.save();
	}
	
	/**
	 * ��ȡ��������--�������
	 */
	public boolean recordLongPoints(String userid,int type) {
		PointsReceive pointsReceive = new PointsReceive();
		Points points = new Points();
		List<Record> pointsInfo = points.getPointsInfo(userid);
		PointsType pointsType = new PointsType().findById(type);//
		int ptpoints = pointsType.get("ptpoints");//�������͵ķ���
		String ptname = pointsType.get("ptypeName");//������������
		boolean receiveFlag = false;//��¼��ȡ��������
		boolean recordFlag = false;//��¼���� 
		int totalPoints = 0;
		int useablePoints = 0;
		if(pointsInfo.size()==0) {
			points.set("puserid", userid).set("totalPoints", ptpoints).set("useablePoints", ptpoints);
			recordFlag = points.save();
			if(recordFlag) {
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
				.set("receivePoints", ptpoints).set("preceivestatus", 2).set("beforepoints", useablePoints)
				.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "��ȡ��" + ptpoints);
				receiveFlag = pointsReceive.save();
				if(receiveFlag) {
					return true;
				}else {
					return false;
				}
			}else {
				return false;
			}
			
		} else {
			int poid = pointsInfo.get(0).get("poid");
			totalPoints = Integer.parseInt(pointsInfo.get(0).get("totalPoints").toString());
			useablePoints = Integer.parseInt(pointsInfo.get(0).get("useablePoints").toString());
			points = new Points().findById(poid);
			points.set("totalPoints", totalPoints+ptpoints).set("useablePoints", useablePoints+ptpoints);
			recordFlag = points.update();
			if(recordFlag) {
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
				.set("receivePoints", ptpoints).set("preceivestatus", 2).set("beforepoints", useablePoints)
				.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "��ȡ��" + ptpoints);
				receiveFlag = pointsReceive.save();
				if(receiveFlag) {
					return true;
				}else {
					return false;
				}
			}else {
				return false;
			}
		}
		
	}
	/**
	 * ������ȡ��¼
	 */
	@Before(Tx.class)
	public boolean recordPoints(String userid,int type) {
		Points points = new Points();
		List<Record> pointsInfo = points.getPointsInfo(userid);
		PointsReceive pointsReceive = new PointsReceive();
		List<Record> pointsReceiveInfo = null;
		if(type==3 || type==5 || type==9) {
			pointsReceiveInfo = pointsReceive.getPointsOnlyOneInfo(userid, type);//��ѯһ��������
		}else {
			pointsReceiveInfo = pointsReceive.getPointsReceTodayInfo(userid,type);//��ѯÿ������
		}
		System.out.println(pointsReceiveInfo);
		PointsType pointsType = new PointsType().findById(type);//
		int ptpoints = pointsType.get("ptpoints");//�������͵ķ���
		String ptname = pointsType.get("ptypeName");//������������
		boolean receiveFlag = false;//��¼��ȡ��������
		boolean recordFlag = false;//��¼���� 
		//��¼�û����� �û�û�л��ּ�¼������һ������
		int totalPoints = 0;
		int useablePoints = 0;
		if(pointsInfo.size()==0) {
			points.set("puserid", userid).set("totalPoints", ptpoints).set("useablePoints", ptpoints);
			recordFlag = points.save();
		} else {
			int poid = pointsInfo.get(0).get("poid");
			totalPoints = Integer.parseInt(pointsInfo.get(0).get("totalPoints").toString());
			useablePoints = Integer.parseInt(pointsInfo.get(0).get("useablePoints").toString());
			points = new Points().findById(poid);
			points.set("totalPoints", totalPoints+ptpoints).set("useablePoints", useablePoints+ptpoints);
			recordFlag = points.update();
		}
		if(recordFlag) {
			//��¼�û���ȡ��¼ ��û�и����͵ļ�¼����һ���¼�¼
			if(pointsReceiveInfo.size()==0) {
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
				.set("receivePoints", ptpoints).set("preceivestatus", 2).set("beforepoints", useablePoints)
				.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "��ȡ��" + ptpoints);
				receiveFlag = pointsReceive.save();
			}else {
			
				String prid = pointsReceiveInfo.get(0).get("prid").toString();
				pointsReceive = new PointsReceive().findById(prid);
				//��¼��ȡ����
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
						.set("receivePoints", ptpoints).set("preceivestatus", 2)
						.set("beforepoints", useablePoints)
						.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "��ȡ��" + ptpoints);
				receiveFlag = pointsReceive.update();
			}
			System.out.println("receiveFlag=" + receiveFlag);
			if(receiveFlag) {
				return true;
			}else {
				return false;
			}
		}else {
			return false;
		}
	}
	
	/**
	 * ������֤����
	 * 
	 * @param phonenumber
	 * @return
	 */
	public String verifyMessage(String phonenumber) {
		String feedback = "";// �������ط��ؽ��
		String content = "�����꡿ע����֤�룺"; // ��������
		List<Record> list = new ArrayList<Record>();
		// ���������λ������Ϊ��֤��
		int verifyCode = (int) (Math.random() * 9000 + 1000);
		SendMessage sm = new SendMessage();
		content += verifyCode + "������֤��ֻ���ڡ����ꡱС��������ת�����ˡ�";
		// ������֤�벢��ȡ���ؽ��
		try {
			feedback = sm.send(phonenumber, content);
			System.out.println(feedback);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ((feedback.substring(0, 5)).equals("error")) {
			jsonString = jsonData.getJson(1003, "������֤�뷢��ʧ��");
		} else {
			Record record = new Record();
			record.set("verifyCode", verifyCode);
			list.add(record);
			jsonString = jsonData.getJson(0, "success", list);
		}
		return jsonString;
	}

	/**
	 * ��ѯ����
	 */
	public int getUseablePoints(String userid) {
		int useablePoints = 0;
		if (userid != null) {
			//��ȡ�û�����
			Record points = Db.findFirst("select * from points where puserid="+userid);
			if(points!=null) {
				useablePoints = points.getInt("useablePoints");
			} 
		}
		return useablePoints;
	}
	
	
}
