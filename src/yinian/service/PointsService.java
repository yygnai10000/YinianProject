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
	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类

	
	/**
	 * 记录成功邀请好友
	 */
	public boolean recordSuccessInviteFriend(String userid) {
		Points points = new Points();
		List<Record> pointsInfo = Db.find("select * from points where puserid=" + userid);
		System.out.println(pointsInfo.size());
		if (pointsInfo.size() == 0) {
			// 无数据，插入新数据
			points.set("puserid", userid).set("inviteNum", 1);
			return points.save();
		} else {
			// 有数据，更新原有数据
			String poid = pointsInfo.get(0).get("poid").toString();
			int inviteNum = Integer.parseInt(pointsInfo.get(0).get("inviteNum").toString());
			points = new Points().findById(poid).set("inviteNum", inviteNum + 1);
			return points.update();
		}
	}
	
	/**
	 * 改变积分获取状态
	 */
	public boolean updatePointsStatus(String userid, int type) {
		PointsReceive pointsreceive = new PointsReceive();
		pointsreceive.set("puserid", userid).set("ptid", type).set("preceivestatus", 1);
		return pointsreceive.save();
	}
	
	/**
	 * 领取长期任务--邀请好友
	 */
	public boolean recordLongPoints(String userid,int type) {
		PointsReceive pointsReceive = new PointsReceive();
		Points points = new Points();
		List<Record> pointsInfo = points.getPointsInfo(userid);
		PointsType pointsType = new PointsType().findById(type);//
		int ptpoints = pointsType.get("ptpoints");//积分类型的分数
		String ptname = pointsType.get("ptypeName");//积分类型名字
		boolean receiveFlag = false;//记录领取积分详情
		boolean recordFlag = false;//记录积分 
		int totalPoints = 0;
		int useablePoints = 0;
		if(pointsInfo.size()==0) {
			points.set("puserid", userid).set("totalPoints", ptpoints).set("useablePoints", ptpoints);
			recordFlag = points.save();
			if(recordFlag) {
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
				.set("receivePoints", ptpoints).set("preceivestatus", 2).set("beforepoints", useablePoints)
				.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "领取了" + ptpoints);
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
				.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "领取了" + ptpoints);
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
	 * 积分领取记录
	 */
	@Before(Tx.class)
	public boolean recordPoints(String userid,int type) {
		Points points = new Points();
		List<Record> pointsInfo = points.getPointsInfo(userid);
		PointsReceive pointsReceive = new PointsReceive();
		List<Record> pointsReceiveInfo = null;
		if(type==3 || type==5 || type==9) {
			pointsReceiveInfo = pointsReceive.getPointsOnlyOneInfo(userid, type);//查询一次性任务
		}else {
			pointsReceiveInfo = pointsReceive.getPointsReceTodayInfo(userid,type);//查询每日任务
		}
		System.out.println(pointsReceiveInfo);
		PointsType pointsType = new PointsType().findById(type);//
		int ptpoints = pointsType.get("ptpoints");//积分类型的分数
		String ptname = pointsType.get("ptypeName");//积分类型名字
		boolean receiveFlag = false;//记录领取积分详情
		boolean recordFlag = false;//记录积分 
		//记录用户积分 用户没有积分记录，插入一条数据
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
			//记录用户领取记录 ，没有该类型的记录插入一条新纪录
			if(pointsReceiveInfo.size()==0) {
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
				.set("receivePoints", ptpoints).set("preceivestatus", 2).set("beforepoints", useablePoints)
				.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "领取了" + ptpoints);
				receiveFlag = pointsReceive.save();
			}else {
			
				String prid = pointsReceiveInfo.get(0).get("prid").toString();
				pointsReceive = new PointsReceive().findById(prid);
				//记录领取详情
				pointsReceive.set("puserid", userid).set("ptid", type).set("ptname", ptname)
						.set("receivePoints", ptpoints).set("preceivestatus", 2)
						.set("beforepoints", useablePoints)
						.set("laterpoints", useablePoints+ptpoints).set("premark", ptname + "领取了" + ptpoints);
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
	 * 发送验证短信
	 * 
	 * @param phonenumber
	 * @return
	 */
	public String verifyMessage(String phonenumber) {
		String feedback = "";// 短信网关返回结果
		String content = "【忆年】注册验证码："; // 短信内容
		List<Record> list = new ArrayList<Record>();
		// 随机生成四位整数作为验证码
		int verifyCode = (int) (Math.random() * 9000 + 1000);
		SendMessage sm = new SendMessage();
		content += verifyCode + "，此验证码只用于“忆年”小程序，请勿转发他人。";
		// 发送验证码并获取返回结果
		try {
			feedback = sm.send(phonenumber, content);
			System.out.println(feedback);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ((feedback.substring(0, 5)).equals("error")) {
			jsonString = jsonData.getJson(1003, "短信验证码发送失败");
		} else {
			Record record = new Record();
			record.set("verifyCode", verifyCode);
			list.add(record);
			jsonString = jsonData.getJson(0, "success", list);
		}
		return jsonString;
	}

	/**
	 * 查询积分
	 */
	public int getUseablePoints(String userid) {
		int useablePoints = 0;
		if (userid != null) {
			//获取用户积分
			Record points = Db.findFirst("select * from points where puserid="+userid);
			if(points!=null) {
				useablePoints = points.getInt("useablePoints");
			} 
		}
		return useablePoints;
	}
	
	
}
