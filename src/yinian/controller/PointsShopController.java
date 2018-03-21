package yinian.controller;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

import jersey.repackaged.com.google.common.primitives.Ints;
import redis.clients.jedis.Jedis;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Comment;
import yinian.model.Encourage;
import yinian.model.Event;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.Like;
import yinian.model.Picture;
import yinian.model.Points;
import yinian.model.PointsBonus;
import yinian.model.PointsGift;
import yinian.model.PointsReceive;
import yinian.model.PointsType;
import yinian.model.Sign;
import yinian.model.Signold;
import yinian.model.User;
import yinian.push.SmallAppPush;
import yinian.schedule.SmallAppOldUserCallbackByPicCntTimer;
import yinian.service.ActivityService;
import yinian.service.EventService;
import yinian.service.NewH5Service;
import yinian.service.PointsService;
import yinian.service.YinianService;
import yinian.thread.EventPushPicNumThread;
import yinian.thread.PictureVerifyThread;
import yinian.utils.JsonData;
import yinian.utils.RedisUtils;

public class PointsShopController extends Controller {
	
	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	
	private ActivityService activityService = new ActivityService();
	// enhance������Ŀ�����AOP��ǿ
	EventService TxService = enhance(EventService.class);
	private YinianService TxYinianService = enhance(YinianService.class);
	private EventService eventService = new EventService();// ҵ������
	private PointsService pointsService = new PointsService();
	private YinianService yinianService = new YinianService();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	

	
	/**
	 * �ɹ��������
	 */
	@Before(Tx.class)
	public void SuccessInviteFriend() {
		String userid = this.getPara("userid");
		// ������������
		boolean flag = pointsService.recordSuccessInviteFriend(userid);
		
		// �����û��ܿռ�0.5G = 524288 KB
		boolean increaseFlag = false;
		boolean pointsFlag = false;
		if (flag) {
			User user = new User();
			increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 524288.00);
			Encourage en = new Encourage();
			List<Record> encourageInfo = Encourage.getEncourageInfo(userid);
			List<Record> statusInfo = Db.find("select ptstatus from pointstype where ptypeid=8");
			int status = statusInfo.get(0).get("ptstatus");//״̬Ϊ0ʱ���ֿ�
			if(status == 0) {
				pointsFlag = pointsService.recordLongPoints(userid,8);//����ɹ����ӻ���
			}
		}
		jsonString = dataProcess.updateFlagResult(flag && increaseFlag && pointsFlag);
		renderText(jsonString);
	}
	
	/**
	 * �ϴ���̬
	 */
	@Before(CrossDomain.class)
	public void UploadEvent() {
		// ˭�����ĸ��ռ���
		String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
		String groupid = this.getPara("groupid") == null ? "" : this.getPara("groupid");
		// ͼƬ
		String picAddress = this.getPara("picAddress") == null ? "" : this.getPara("picAddress");
		// ����
		String content = this.getPara("content") == null ? "" : this.getPara("content");
		// ����
		String audio = this.getPara("audio") == null ? "" : this.getPara("audio");
		// �ص�
		String place = this.getPara("place") == null ? "" : this.getPara("place");
		String placePic = this.getPara("placePic") == null ? "" : this.getPara("placePic");// λ�����ɵ�ͼƬ��ַ
		String placeLongitude = this.getPara("placeLongitude") == null ? "" : this.getPara("placeLongitude");// ����
		String placeLatitude = this.getPara("placeLatitude") == null ? "" : this.getPara("placeLatitude");// γ��
		// ��˭
		String peopleName = this.getPara("peopleName") == null ? "" : this.getPara("peopleName");
		// ��̬���ĸ�Ҫ��Ϊ��
		String main = this.getPara("main") == null ? "" : this.getPara("main"); // 0--��Ƭ 1--���� 2--���� 3--�ص�
		// ����Ԫ��
		String storage = this.getPara("storage") == null ? "" : this.getPara("storage");// �洢�ռ�
		String source = this.getPara("source") == null ? "" : this.getPara("source");// �жϽӿ���Դ
		String isPush = this.getPara("isPush") == null ? "" : this.getPara("isPush"); // �����ж� app:yes/no С����:true/false
		String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // С�������ͱ�ID

		// ����formID
		// FormID.insertFormID(userid, formID);
		if (!userid.equals("") && !formID.equals("")) {
			FormID.insert(userid, formID);
		}
		// �жϴ洢�ռ��Ƿ��д�
		double storagePlace;
		if (storage == null || storage.equals("")) {
			storagePlace = 0.00;
		} else {
			storagePlace = Double.parseDouble(storage);
		}

		// �ӿ���ԴΪweb����Ҫ����
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		// ��ַ�ֶ����ݴ���
		String firstPic = null;
		String[] picArray = new String[0];
		if (picAddress != null && !picAddress.equals("")) {
			picArray = dataProcess.getPicAddress(picAddress, "private");
			// ͼƬ����
			picArray = dataProcess.PictureVerify(picArray);
			// ��ȡ��̬��һ��ͼƬ��ַ,����û���ϴ�ͼƬ
			firstPic = (picArray.length == 0 ? null : picArray[0]);
		}

		// ͼƬ�������˵�������������
		int eid = 0;
		if (main.equals("0") && picArray.length == 0) {
			List<Record> errorList = new ArrayList<Record>();
			Record r = new Record();
			r.set("picList", new ArrayList<String>());
			errorList.add(r);
			jsonString = jsonData.getSuccessJson(errorList);
		} else {
			// ֧��ͬʱ�ϴ�������ռ�
			String[] IDs = groupid.split(",");
			// ����ռ��ϴ�
			for (int i = 0; i < IDs.length; i++) {
				// �ϴ�Ȩ�޿���
				boolean uploadFlag = true;
				Group group = new Group().findById(IDs[i]);
				int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
				if (gAuthority == 1) {
					String gcreator = group.get("gcreator").toString();
					if (!gcreator.equals(userid)) {
						uploadFlag = false;
					}
				} else {
					if (gAuthority == 2) {
						List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", userid + "Authority");
						if (uploadAuthority == null) {
							uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmuserid="
									+ userid + " and gmauthority=1 ");
							if (uploadAuthority == null && uploadAuthority.size() == 0) {
								uploadFlag = false;
							}
							CacheKit.put("ConcurrencyCache", userid + "Authority", uploadAuthority);
						}
					}
				}
				if (uploadFlag) {
					// ͬ�����,0--��ͬ�� 1--ͬ�� ,��һ���ռ�Ϊԭ��������Ϊͬ��
					int isSynchronize = (i == 0 ? 0 : 1);
					eid = TxService.upload(userid, IDs[i], picArray, content, audio, place, placePic, placeLongitude,
							placeLatitude, peopleName, main, storagePlace, firstPic, isPush, source, isSynchronize,
							formID);
					if (eid != 0) {
						// ˵���ϴ��ɹ�
						List<Record> result = eventService.getSingleEvent(eid, source);// ��ȡ��̬����Ϣ
						System.out.println(result);
						jsonString = jsonData.getSuccessJson(result);
						//�������� by lk
//						if(CommonParam.canPublish){
//							//Group group = new Group().findById(IDs[i]);
//							String gOrigin = String.valueOf(group.getLong("gOrigin"));
//							if (gOrigin.equals("0")) {
//								ExecutorService exec = Executors.newCachedThreadPool();
//								exec.execute(new EventPushPicNumThread(IDs[i], picArray.length,userid));
////								// �ر��̳߳�
//								exec.shutdown();
//							}
//						}
						/*
						 * //��ʱ�ϴ���̬���Ϳ��� Boolean eventIsPush = true; //�ж��Ƿ�����ͨ��� ����ͨ�����޲����� String
						 * gOrigin =String.valueOf(group.getLong("gOrigin"));;
						 * 
						 * if(gOrigin.equals("1")) { eventIsPush = false; } if(eventIsPush) { // ����һ���̳߳�
						 * ExecutorService exec = Executors.newCachedThreadPool();
						 * System.out.println(picArray.length); exec.execute(new
						 * EventPushPicNumThread(IDs[i],picArray.length)); // �ر��̳߳� exec.shutdown();
						 * 
						 * }
						 */
					} else {
						jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
						break;

					}
				}else {
					List<Record> errorList = new ArrayList<Record>();
					Record r = new Record();
					r.set("picList", new ArrayList<String>());
					errorList.add(r);
					System.out.println(errorList);
					jsonString = jsonData.getSuccessJson(errorList);
				}
			}

		}
		// ���ؽ��
		renderText(jsonString);
//		if (eid != 0) {
//			String[] IDs = groupid.split(",");
//			for (int i = 0; i < IDs.length; i++) {
//				// ��ʱ�ϴ���̬���Ϳ���
//				Boolean eventIsPush = true;
//				// �ж��Ƿ�����ͨ��� ����ͨ�����޲�����
//				Group group = new Group().findById(IDs[i]);
//				String gOrigin = String.valueOf(group.getLong("gOrigin"));
//				if (gOrigin.equals("1")) {
//					eventIsPush = false;
//				}
//				if (eventIsPush) {
//					// ����һ���̳߳�
//					ExecutorService exec = Executors.newCachedThreadPool();
//					System.out.println(picArray.length);
//					exec.execute(new EventPushPicNumThread(IDs[i], picArray.length));
//					// �ر��̳߳�
//					exec.shutdown();
//
//				}
//			}
//		}
		// �����߳�
		// if(eid != 0) {
		// // ����һ���̳߳�
		// ExecutorService exec = Executors.newCachedThreadPool();
		// // ִ�м����߳�
		// exec.execute(new PictureVerifyThread(userid,eid, main));
		// // �ر��̳߳�
		// exec.shutdown();
		// }
	}
	
	
	/**
	 * �ϴ�����Ƶ
	 */
	@Before(CrossDomain.class)
	public void UploadShortVideo() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String address = this.getPara("address");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String place = this.getPara("place");
		String cover = this.getPara("cover");
		String time = this.getPara("time");
		String source = this.getPara("source");

		// �жϴ洢�ռ��Ƿ��д�
		double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
		cover = (cover == null ? "" : cover);
		time = (time == null ? "0" : time);

		// ��Դ��ַ��ǰ׺
		address = CommonParam.qiniuPrivateAddress + address;
		// ��Ƶ����,��Ƶ����ͼƬ����trueΪɫ����Ƶ
		boolean videoJudge = dataProcess.VideoVerify(address);
		boolean coverJudge = false;
		if (!cover.equals(""))
			coverJudge = dataProcess.SinglePictureVerify(cover);

		if (videoJudge || coverJudge) {
			jsonString = jsonData.getJson(1039, "��ԴΥ��");
		} else {
			// ֧��ͬʱ�ϴ�������ռ�
			String[] IDs = groupid.split(",");
			boolean flag = true;
			int eventID = 0;
			// ����ռ��ϴ�
			for (int i = 0; i < IDs.length; i++) {
				// �ϴ�Ȩ�޿���
				boolean uploadFlag = true;
				Group group = new Group().findById(IDs[i]);
				int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
				if (gAuthority == 1) {
					String gcreator = group.get("gcreator").toString();
					if (!gcreator.equals(userid)) {
						uploadFlag = false;
					}
				} else {
					if (gAuthority == 2) {
						List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", userid + "Authority");
						if (uploadAuthority == null) {
							uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmuserid="
									+ userid + " and gmauthority=1 ");
							if (uploadAuthority == null && uploadAuthority.size() == 0) {
								uploadFlag = false;
							}
							CacheKit.put("ConcurrencyCache", userid + "Authority", uploadAuthority);
						}
					}
				}
				if (uploadFlag) {
					// ͬ�����,0--��ͬ�� 1--ͬ�� ,��һ���ռ�Ϊԭ��������Ϊͬ��
					int isSynchronize = (i == 0 ? 0 : 1);
					// �ϴ�����Ƶ
					int eid = TxService.uploadShortVedio(userid, IDs[i], address, content, storagePlace, place, cover,
							time, isSynchronize, source);
					eventID = eid;
					if (eid == 0) {
						flag = false;
						break;
					}
				}else {
					List<Record> errorList = new ArrayList<Record>();
					Record r = new Record();
					r.set("picList", new ArrayList<String>());
					errorList.add(r);
					System.out.println(errorList);
					jsonString = jsonData.getSuccessJson(errorList);
				}
			}

			if (flag) {
				// ˵���ϴ��ɹ�
				List<Record> result = eventService.getSingleEvent(eventID, source);// ��ȡ��̬����Ϣ
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
			}
		}
		// ���ؽ��
		renderText(jsonString);
	}
	
	/**
	 * ÿ��ǩ��+10���֣�����7��ǩ�����⽱��+100����
	 */
	public void DailySignInNew() throws ParseException {
		String userid = this.getPara("userid");
		String signType = this.getPara("signType");
		// ǩ��
		boolean signInFlag = activityService.signInPoints(userid, "0");
		Signold sign = new Signold();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid,signType);
		String signEndDate = "";
		String signStartDate = "";
		String signID;
		if(null!=userSignInfo && userSignInfo.size()!=0) {
			signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			signStartDate = userSignInfo.get(0).get("signStartDate").toString();
			signID = userSignInfo.get(0).get("signID").toString();
		}else {
			jsonString = jsonData.getJson(-50, "��������");
			renderText(jsonString);
		}
		

		// �����û��ռ�100M=102400KB
		boolean increaseFlag = false;
		// �����û�����
		boolean receiveFlag = false;
		if (signInFlag) {
			User user = new User();
			increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 102400.00);
		}
		if (signInFlag) {
			List<Record> statusListInfo = Db.find("select ptstatus from pointstype where ptypeid=1");
			int status = statusListInfo.get(0).get("ptstatus");//״̬Ϊ0ʱ���ֿ�
			if(status==0) {
				receiveFlag = pointsService.recordPoints(userid, 1);
				long to = sdf.parse(signEndDate).getTime();
				long from = sdf.parse(signStartDate).getTime();
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				//������ǩ������ʱ����ö����100���ֽ���
				if(signDay == 7) {
					receiveFlag = pointsService.recordPoints(userid, 2);
				}
			}
			
		}
		if (signInFlag && increaseFlag && receiveFlag) {
			jsonString = jsonData.getSuccessJson();
		} else {
			jsonString = jsonData.getJson(2030, "������ǩ��");
		}

		renderText(jsonString);

	}
	
	/**
	 * ������������ȡ������ lk �޸ķ��ض���
	 * ÿ���һ�����ޣ�ÿ�����+10����
	 */
	public void AttachOrRemoveExpressionByLkNew() {
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // С�������ͱ�ID

		// ����formID
		//FormID.insertFormID(userid, formID);
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		int status = 0;
		if (source != null && source.equals("app")) {
			// appֱ�Ӵ�ֵ
			status = Integer.parseInt(type);
		} else {
			// С����Ӣ��,��type�ĳɶ�Ӧstatus
			switch (type) {
			case "like":
				status = 0;
				break;
			case "happy":
				status = 2;
				break;
			case "sad":
				status = 3;
				break;
			case "mad":
				status = 4;
				break;
			case "surprise":
				status = 5;
				break;
			case "unlike":
				status = 1;
				break;
			}
		}
		// �ж��û��Ƿ�����ز���
				List<Record> judge = Db
						.find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");
				
				List<Record> result = new ArrayList<Record>();
//				String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
//						+ eid + " and likeStatus!=1";
				String sql = "select unickname,likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
						+ eid + " and likeStatus!=1 order by likeID desc limit 0,10";
				String likeCntSql = "select count(*) cnt from `like` where likeEventID="
						+ eid + " and likeStatus!=1 ";
				String iLikeSql="select count(*) cnt from `like`,users where userid=likeUserID and likeEventID="
             +eid+ " and userid='"+userid+ "' and likeStatus!=1 ";
				Like like;
				if (judge.size() == 0) {
					// û�в�����
					like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
					if (like.save()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						Record r=new Record();
						r.set("likeCnt",0);
						//��������ȡredis���棬��û�л������ȡ���ݿ⣬ͬʱ���»���
						Jedis jedis = RedisUtils.getRedis();
						if(null!=jedis) {
							//�ӻ����ж�ȡ��ǰeid�ĵ�����
							String likeCnt = jedis.get("likeCnt_"+eid);
							if(null!=likeCnt&&!"".equals(likeCnt)) {
								//���޳ɹ��󻺴��������1
								int likeCntInt = Integer.valueOf(likeCnt) + 1;
								String jr = jedis.set("likeCnt_"+eid, String.valueOf(likeCntInt));
								if(null!=jr&&"OK".equals(jr)) {
									r.set("likeCnt", likeCntInt);
								}
							}else {
								//��ǰeidδ����������������ݿ�count����������ͬ��������
								List<Record> cntList=Db.find(likeCntSql);
								if(!cntList.isEmpty()){
									r.set("likeCnt",cntList.get(0).get("cnt"));
									jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
								}
							}
							//�ͷ�redis
							RedisUtils.returnResource(jedis);
						}else {
							List<Record> cntList=Db.find(likeCntSql);
							if(!cntList.isEmpty()){
								r.set("likeCnt",cntList.get(0).get("cnt"));
							}
						}
						r.set("likeUser",0);
						List<Record> iLikeCntList=Db.find(iLikeSql);
						if(!iLikeCntList.isEmpty()){
							r.set("likeUser",iLikeCntList.get(0).get("cnt"));
						}
						r.set("like", result);
							returnList.add(r);
							jsonString = jsonData.getSuccessJson(returnList);
							//�������� begin
							if(CommonParam.canPublish&&status==0){
								Event event = new Event().findById(eid);
								String egroupid = event.get("egroupid").toString();
								String uid = event.get("euserid").toString();
								//�ж��Ƿ�����ͨ��� ����ͨ�����޲�����
								String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
								if(gOrigin.equals("0")&&!userid.equals(uid)) {
									User user = new User().findById(userid);
									//�����˵�����
									String username = user.get("unickname").toString();
									//�����˵�uopenid
									
									String uopenid = new User().findById(uid).get("uopenid");
									if(null!=uopenid&&!uopenid.equals("")){
										//��ȡ�����˵�formid
										List<Record> formidList = Db.find("select formID from formid where 1 "
												+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
												+ "order by time asc limit 1");
										//���޳ɹ���������
										if (null!=formidList&&!formidList.isEmpty()) {
											String formid = formidList.get(0).get("formID");
											System.out.println("formid=="+formid);
											//���޳ɹ���������
											SmallAppPush smallAppPush = new SmallAppPush();
											smallAppPush.likeIsPush(formid, uopenid, eid, username);
											System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
											Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
										}
									}
								}
							}
							//�������� end
//							//�������Ϳ���
//							Boolean likePushFlag = true;
//							Event event = new Event().findById(eid);
//							String egroupid = event.get("egroupid").toString();
//							//�ж��Ƿ�����ͨ��� ����ͨ�����޲�����
//							String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//							if(gOrigin.equals("1")) {
//								likePushFlag = false;
//							}
//							if(likePushFlag) {
//								User user = new User().findById(userid);
//								//�����˵�����
//								String username = user.get("unickname").toString();
//								//�����˵�uopenid
//								String uid = event.get("euserid").toString();
//								String uopenid = new User().findById(uid).get("uopenid");
//								//��ȡ�����˵�formid
//								List<Record> formidList = Db.find("select formID from formid where 1 "
//										+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//										+ "order by time asc limit 1");
//								//���޳ɹ���������
//								if (null!=formidList&&!formidList.isEmpty()) {
//									String formid = formidList.get(0).get("formID");
//									System.out.println("formid=="+formid);
//									//���޳ɹ���������
//									SmallAppPush smallAppPush = new SmallAppPush();
//									smallAppPush.likeIsPush(formid, uopenid, eid, username);
//									System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
//									Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
//								}
//								
							//}
						//jsonString = jsonData.getSuccessJson(result);
					} else {
						jsonString = dataProcess.insertFlagResult(false);
					}

				} else {
					// �в�����
					int likeID = Integer.parseInt(judge.get(0).get("likeID").toString());
					like = new Like().findById(likeID);
					like.set("likeStatus", status);
					if (like.update()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						//Record r=new Record();
						Record r=new Record();
						r.set("likeCnt",0);
						//��������ȡredis���棬��û�л������ȡ���ݿ⣬ͬʱ���»���
						Jedis jedis = RedisUtils.getRedis();
						if(null!=jedis) {
							//�ӻ����ж�ȡ��ǰeid�ĵ�����
							String likeCnt = jedis.get("likeCnt_"+eid);
							if(null!=likeCnt&&!"".equals(likeCnt)) {
								if(status == 1) {
									//ȡ������ʱ�������������1
									int likeCntInt = Integer.valueOf(likeCnt)>0?Integer.valueOf(likeCnt) - 1:0;
									String jr = jedis.set("likeCnt_"+eid, String.valueOf(likeCntInt));
									if(null!=jr&&"OK".equals(jr)) {
										r.set("likeCnt", likeCntInt);
									}
								}else {
									//��ȡ�����ޣ��������������
									r.set("likeCnt", Integer.valueOf(likeCnt));
								}
							}else {
								//��ǰeidδ����������������ݿ�count����������ͬ��������
								List<Record> cntList=Db.find(likeCntSql);
								if(!cntList.isEmpty()){
									r.set("likeCnt",cntList.get(0).get("cnt"));
									jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
								}
							}
							//�ͷ�redis
							RedisUtils.returnResource(jedis);
						}else {
							List<Record> cntList=Db.find(likeCntSql);
							if(!cntList.isEmpty()){
								r.set("likeCnt",cntList.get(0).get("cnt"));
							}
						}
						r.set("like", result);
						r.set("likeUser",0);
						List<Record> iLikeCntList=Db.find(iLikeSql);
						if(!iLikeCntList.isEmpty()){
							r.set("likeUser",iLikeCntList.get(0).get("cnt"));
						}
						returnList.add(r);
						jsonString = jsonData.getSuccessJson(returnList);
					} else {
						jsonString = dataProcess.updateFlagResult(false);
					}

				}

				renderText(jsonString);
		// �ж��û��Ƿ�����ز���
	}
	
	/**
	 * ������������ȡ������
	 * 
	 */
	public void AttachOrRemoveExpression() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID"); // С�������ͱ�ID
		// ����formID
		FormID.insertFormID(userid, formID);
		int status = 0;
		if (source != null && source.equals("app")) {
			// appֱ�Ӵ�ֵ
			status = Integer.parseInt(type);
		} else {
			// С����Ӣ��,��type�ĳɶ�Ӧstatus
			switch (type) {
			case "like":
				status = 0;
				break;
			case "happy":
				status = 2;
				break;
			case "sad":
				status = 3;
				break;
			case "mad":
				status = 4;
				break;
			case "surprise":
				status = 5;
				break;
			case "unlike":
				status = 1;
				break;
			}
		}

		// �ж��û��Ƿ�����ز���
		List<Record> judge = Db
				.find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");

		List<Record> result = new ArrayList<Record>();
		String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
				+ eid + " and likeStatus!=1";

		Like like;
		if (judge.size() == 0) {
			// û�в�����
			like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
			if (like.save()) {
				result = Db.find(sql);
				result = dataProcess.changeLikeStatusToWord(result);
				jsonString = jsonData.getSuccessJson(result);
				//�������� begin
				if(CommonParam.canPublish&&status==0){
					Event event = new Event().findById(eid);
					String egroupid = event.get("egroupid").toString();
					String uid = event.get("euserid").toString();
					//�ж��Ƿ�����ͨ��� ����ͨ�����޲�����
					String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
					if(gOrigin.equals("0")&&!userid.equals(uid)) {
						User user = new User().findById(userid);
						//�����˵�����
						String username = user.get("unickname").toString();
						//�����˵�uopenid
						
						String uopenid = new User().findById(uid).get("uopenid");
						if(null!=uopenid&&!uopenid.equals("")){
							//��ȡ�����˵�formid
							List<Record> formidList = Db.find("select formID from formid where 1 "
									+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
									+ "order by time asc limit 1");
							//���޳ɹ���������
							if (null!=formidList&&!formidList.isEmpty()) {
								String formid = formidList.get(0).get("formID");
								System.out.println("formid=="+formid);
								//���޳ɹ���������
								SmallAppPush smallAppPush = new SmallAppPush();
								smallAppPush.likeIsPush(formid, uopenid, eid, username);
								System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
								Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
							}
						}
					}
				}
				//�������� end
//				//�������Ϳ���
//				Boolean likePushFlag = true;
//				Event event = new Event().findById(eid);
//				String egroupid = event.get("egroupid").toString();
//				//�ж��Ƿ�����ͨ��� ����ͨ�����޲�����
//				String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//				if(gOrigin.equals("1")) {
//					likePushFlag = false;
//				}
//				if(likePushFlag) {
//					User user = new User().findById(userid);
//					//�����˵�����
//					String username = user.get("unickname").toString();
//					//�����˵�uopenid
//					String uid = event.get("euserid").toString();
//					String uopenid = new User().findById(uid).get("uopenid");
//					//��ȡ�����˵�formid
//					List<Record> formidList = Db.find("select formID from formid where 1 "
//							+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//							+ "order by time asc limit 1");
//					//���޳ɹ���������
//					if (null!=formidList&&!formidList.isEmpty()) {
//						String formid = formidList.get(0).get("formID");
//						System.out.println("formid=="+formid);
//						//���޳ɹ���������
//						SmallAppPush smallAppPush = new SmallAppPush();
//						smallAppPush.likeIsPush(formid, uopenid, eid, username);
//						System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
//						Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
//					}
//				}
			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else {
			// �в�����
			int likeID = Integer.parseInt(judge.get(0).get("likeID").toString());
			like = new Like().findById(likeID);
			like.set("likeStatus", status);
			if (like.update()) {
				result = Db.find(sql);
				result = dataProcess.changeLikeStatusToWord(result);
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = dataProcess.updateFlagResult(false);
			}

		}

		renderText(jsonString);

	}
	
	/**
	 * ��������1 1.1�汾 ���������ֶ� cid
	 * ÿ���һ�����ۼ�20��
	 */
	public void SendComment1() {
		String commentUserId = this.getPara("commentUserId")==null?"":this.getPara("commentUserId");// ������ID
		String commentedUserId = this.getPara("commentedUserId");// ��������ID
		String eventId = this.getPara("eventId");// �¼�ID
		String content = this.getPara("content");// ��������
		String place = this.getPara("place");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // С�������ͱ�ID

		if(!commentUserId.equals("")&&!formID.equals("")){
			FormID.insert(commentUserId, formID);
		}
		String cid = TxYinianService.sendComment1(commentUserId, commentedUserId, eventId, content, place);
		if (cid.equals("")) {
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		} else {
			List<Record> list = dataProcess.makeSingleParamToList("cid", cid);
			jsonString = jsonData.getJson(0, "success", list);
			//�������� begin
			if(CommonParam.canPublish){
				Event event = new Event().findById(eventId);
				String egroupid = event.get("egroupid").toString();
//				//�ж��Ƿ�����ͨ��� ����ͨ��������
				String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
				if(gOrigin.equals("0")) {
					User user = new User().findById(commentUserId);
					//�����˵�����
					String username = user.get("unickname").toString();
					//�������˵�uopenid
					String uid = event.get("euserid").toString();
					String uopenid = new User().findById(uid).get("uopenid");
					if(null!=uopenid&&!uopenid.equals("")&&!uid.equals(commentUserId)){
						//��ȡ�����˵�formid
						List<Record> formidList = Db.find("select formID from formid where 1 "
								+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
								+ "order by time asc limit 1");
						
						if(null!=formidList&&!formidList.isEmpty()) {
							String formid = formidList.get(0).get("formID");
							//���۳ɹ���������
							SmallAppPush smallAppPush = new SmallAppPush();
							smallAppPush.commentIsPush(formid, uopenid, eventId, username);
							Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
						}
					}
				}
			}
			//�������� end
//			//�������Ϳ���
//			Boolean commentPushFlag = true;
//			Event event = new Event().findById(eventId);
//			String egroupid = event.get("egroupid").toString();
//			//�ж��Ƿ�����ͨ��� ����ͨ��������
//			String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//			if(gOrigin.equals("1")) {
//				commentPushFlag = false;
//			}
//			if(commentPushFlag) {
//				User user = new User().findById(commentUserId);
//				//�����˵�����
//				String username = user.get("unickname").toString();
//				//�������˵�uopenid
//				String uid = event.get("euserid").toString();
//				String uopenid = new User().findById(uid).get("uopenid");
//				//��ȡ�����˵�formid
//				List<Record> formidList = Db.find("select formID from formid where 1 "
//						+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//						+ "order by time asc limit 1");
//				
//				if(null!=formidList&&!formidList.isEmpty()) {
//					String formid = formidList.get(0).get("formID");
//					//���۳ɹ���������
//					SmallAppPush smallAppPush = new SmallAppPush();
//					smallAppPush.commentIsPush(formid, uopenid, eventId, username);
//					Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
//				}
//			}
		}
		// ���ؽ��
		renderText(jsonString);
	}
	
	/**
	 * ��ȡ��̬���ֲ���¼�û����ֺͻ�����ȡ����
	 */
	public void receivePoints() {
		int type = Integer.parseInt(this.getPara("type"));
		String userid = this.getPara("userid");
		//�ж��Ƿ���ȡ�������ظ���ȡ
		List<Record> list = Db.find("select * from pointsReceive where puserid=" + userid 
				+ " and ptid="+ type + " and to_days(receiveTime)=to_days(now())");
		if(null!=list && list.size()!=0) {
			int preceivestatus = list.get(0).get("preceivestatus");
			//�ж��Ƿ�����ȡ�����ƿ��ٵ���ظ���ȡ
			if(preceivestatus == 2) {
				jsonString = jsonData.getJson(1000, "����ȡ");
			}else {
				boolean flag = pointsService.recordPoints(userid, type);
				if (flag) {
					jsonString = jsonData.getJson(0, "success");
				} else {
					jsonString = jsonData.getJson(1002, "��ȡʧ��");
				}
			}
			
		}else {
			jsonString = jsonData.getJson(1002, "��ȡʧ��");
		}
		// ���ؽ��
		renderText(jsonString);
		
	}
	
	/**
	 * ��ʾ���˻�����Ϣ
	 */
	public void showPersonInfo() throws ParseException {
		String userid = this.getPara("userid");
		Record resultRecord = new Record();
		
		String today = sdf.format(new Date());
		//�жϽ����Ƿ�ǩ��
		Signold sign = new Signold();
		List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
		if(signInfoList.size()==0) {
			resultRecord.set("isTodaySign", false);
		}else {
			String signStartDate = signInfoList.get(0).get("signStartDate").toString();
			String signEndDate = signInfoList.get(0).get("signEndDate").toString();
			// ǩ������
			long to = sdf.parse(signEndDate).getTime();
			long from = sdf.parse(signStartDate).getTime();
			long todays = sdf.parse(today).getTime();
			int count = (int) ((todays - to) / (1000 * 60 * 60 * 24));
			System.out.println(count);
			//�ж��Ƿ��ǩ�������ǩ��ʾsignDayΪ0
			if(count>=2) {
				int signDay = 0;
				resultRecord.set("signDay", signDay);
			}else {
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				resultRecord.set("signDay", signDay);
			}
			//�Ƿ���ʾǩ������
			String firstSign=signInfoList.get(0).get("signFirstTime").toString();
			long f=sdf.parse(firstSign).getTime();
			if(f>sdf.parse("2017-12-14 23:59:59").getTime()){
				resultRecord.set("showSign", 1);//����ʾǩ������
			}else{
				resultRecord.set("showSign", 0);//��ʾǩ������
			}
			// �����Ƿ�ǩ��
			if (signEndDate.equals(today)) {
				resultRecord.set("isTodaySign", true);
			} else {
				resultRecord.set("isTodaySign", false);
			}
		}
		
		if (userid != null) {
			User user = new User().findById(userid);
			String upic = user.get("upic");//�û�ͷ��
			String uname = user.get("unickname");//�û��ǳ�
			Double uusespace = user.get("uusespace");//�û���ʹ�ÿռ�
			resultRecord.set("uusespace", uusespace);
			resultRecord.set("upic", upic);//�û�ͷ��
			resultRecord.set("unickname", uname);//�û��ǳ�
			
			//��ȡ�û�����
			Record points = Db.findFirst("select * from points where puserid="+userid);
			if(points!=null) {
				Integer useablePoints = points.getInt("useablePoints");
				resultRecord.set("useablePoints", useablePoints);
			} else {
				resultRecord.set("useablePoints", 0);
			}
			
		}
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}
	/**
	 * ��ʾ������������
	 */
	
	public void showPointsTask() throws ParseException {
		String userid = this.getPara("userid");
		Record resultRecord = new Record();

		String today = sdf.format(new Date());
		//�жϽ����Ƿ�ǩ��
		Signold sign = new Signold();
		List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
		if(signInfoList.size()==0) {
			resultRecord.set("isTodaySign", false);
		} else {
			String signStartDate = signInfoList.get(0).get("signStartDate").toString();
			String signEndDate = signInfoList.get(0).get("signEndDate").toString();
			// ǩ������
			long to = sdf.parse(signEndDate).getTime();
			long from = sdf.parse(signStartDate).getTime();
			long todays = sdf.parse(today).getTime();
			int count = (int) ((todays - to) / (1000 * 60 * 60 * 24));
			System.out.println(count);
			//�ж��Ƿ��ǩ�������ǩ��ʾsignDayΪ0
			if(count>=2) {
				int signDay = 0;
				resultRecord.set("signDay", signDay);
			}else {
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				resultRecord.set("signDay", signDay);
			}
			// �����Ƿ�ǩ��
			if (signEndDate.equals(today)) {
				resultRecord.set("isTodaySign", true);
			} else {
				resultRecord.set("isTodaySign", false);
			}
		}
		
		//���ۻ��ֿ���
		List<Record> statusCommentsInfo = Db.find("select ptstatus from pointstype where ptypeid=6");
		int statusComments = statusCommentsInfo.get(0).get("ptstatus");//״̬Ϊ0ʱ���ֿ�
		if(statusComments==0) {
			List<Record> commentsList = Db.find("select ctime from comments where cuserid=" + userid + " and cstatus=0" +" and to_days(ctime)=to_days(now())");
			//System.out.println("commentsList="+commentsList.size());
			//System.out.println("commentsList="+commentsList);
			if(commentsList.size()!=0) {
				String ctime=commentsList.get(0).get("ctime").toString();
				System.out.println("ctime=" + ctime);
				//��ȡ���������
				if(ctime!=null && !ctime.equals("")){
					Timestamp timestamp = Timestamp.valueOf(ctime);
					String commtime = sdf.format(timestamp);
					if(commtime != today && !commtime.equals(today)) {
						resultRecord.set("commentsPoints", 0);
					}else {
						//��ѯÿ�����ۻ���״̬
						List<Record> list3 = Db.find("select preceivestatus from pointsReceive where puserid=" + userid 
								+ " and ptid="+ 6 + " and to_days(receiveTime)=to_days(now())");
						if(list3.size()==0) {
							resultRecord.set("commentsPoints", 1);
							pointsService.updatePointsStatus(userid,6);
						}else {
							int preceivestatus = list3.get(0).get("preceivestatus");
							resultRecord.set("commentsPoints", preceivestatus);
						}
					}
				}else {
					resultRecord.set("commentsPoints", 0);
				}
			}else {
				resultRecord.set("commentsPoints", 0);
			}
		}
		
		//���޿��ػ���
		List<Record> statusLikeInfo = Db.find("select ptstatus from pointstype where ptypeid=7");
		int statusLike = statusLikeInfo.get(0).get("ptstatus");//״̬Ϊ0ʱ���ֿ�
		if(statusLike==0) {
			List<Record> likeList = Db.find("select likeTime from `like` where likeUserID=" + userid+ " and to_days(likeTime)=to_days(now())");
			System.out.println("likeList="+likeList.size());
			if(likeList.size()!=0) {
				String likeTime = likeList.get(0).get("likeTime").toString();
				if(likeTime!=null&&!likeTime.equals("")){
					Timestamp time = Timestamp.valueOf(likeTime);
					String ltime = sdf.format(time);
					if(ltime != today && !ltime.equals(today)) {
						resultRecord.set("likePoints", 0);
					}else {
						//��ѯÿ�յ���״̬
						List<Record> list2 = Db.find("select preceivestatus from pointsReceive where puserid=" + userid 
								+ " and ptid="+ 7 + " and to_days(receiveTime)=to_days(now())");
						if(list2.size()==0) {
							resultRecord.set("likePoints", 1);
							pointsService.updatePointsStatus(userid,7);
						}else {
							int preceivestatus = list2.get(0).get("preceivestatus");
							resultRecord.set("likePoints", preceivestatus);
						}
					}
				}else {
					resultRecord.set("likePoints", 0);
				}
			}else {
				resultRecord.set("likePoints", 0);
			}
			
		}
		
		//�ϴ����ֿ���
		List<Record> statusUploadInfo = Db.find("select ptstatus from pointstype where ptypeid=4");
		int statusUpload = statusUploadInfo.get(0).get("ptstatus");
		if(statusUpload==0) {
			List<Record> eventsList = Db.find("select euploadtime from events where euserid=" + userid + " and estatus=0"+" and isSynchronize=0"+" and to_days(euploadtime)=to_days(now())");
			System.out.println("eventsList="+eventsList.size());
			if(eventsList.size()!=0) {
				String euploadtime = eventsList.get(0).get("euploadtime").toString();
				System.out.println("euploadtime=" + euploadtime);
				if(euploadtime!=null && !euploadtime.equals("")){
					Timestamp timestamp = Timestamp.valueOf(euploadtime);
					String etime = sdf.format(timestamp);
					if(etime != today && !etime.equals(today)) {
						resultRecord.set("eventsPoints", 0);
					}else {
						
						//��ѯÿ�շ�����̬����״̬
						List<Record> list1 = Db.find("select preceivestatus from pointsReceive where puserid=" + userid 
								+ " and ptid="+ 4 + " and to_days(receiveTime)=to_days(now())");
						if(list1.size()==0) {
							resultRecord.set("eventsPoints", 1);
							pointsService.updatePointsStatus(userid,4);
						}else {
							int preceivestatus = list1.get(0).get("preceivestatus");
							resultRecord.set("eventsPoints", preceivestatus);
						}
					}
				}else {
					resultRecord.set("eventsPoints", 0);
				}
			}else {
				resultRecord.set("eventsPoints", 0);
			}
			
		}
	
		//��ѯ��һ�δ������״̬
		List<Record> statusAlbumInfo = Db.find("select ptstatus from pointstype where ptypeid=3");
		int statusAlbum = statusUploadInfo.get(0).get("ptstatus");
		if(statusAlbum == 0) {
			List<Record> groupsList = Db.find("select groupid from groups where gcreator=" + userid + " and gtime>'2018-02-01 00:00:00'");
			if(groupsList.size() == 0) {
				resultRecord.set("creatPhotoPoints", 0);
			} else {
				List<Record> groupsStatus = Db.find("select preceivestatus from pointsReceive where puserid=" + userid
						+ " and ptid=" + 3);
				if(groupsStatus.size()==0) {
					pointsService.updatePointsStatus(userid, 3);
					resultRecord.set("creatPhotoPoints", 1);
				}else {
					int preceivestatus = groupsStatus.get(0).get("preceivestatus");
					resultRecord.set("creatPhotoPoints", preceivestatus);
				}
			}
		}
		
		
		//��ѯ100����̬���ֿ���
		List<Record> statusEventOneInfo = Db.find("select ptstatus from pointstype where ptypeid=5");
		int statusEventOne = statusUploadInfo.get(0).get("ptstatus");
		if(statusEventOne == 0) {
			//��ѯ�ѷ���100����̬����״̬
			List<Record> eventsList = Db.find("select count(eid) as num from events where euserid=" + userid + " and estatus=0" +" and isSynchronize=0"+ " and euploadtime>'2018-02-01 00:00:00'");
			if(eventsList.size()!=0) {
				Long num = eventsList.get(0).get("num");
				//��ѯ100�����ֽ���״̬
				List<Record> list4 = Db.find("select preceivestatus from pointsReceive where puserid=" + userid
							+ " and ptid="+5);
				if(num>=100) {
					if(list4.size()==0) {
						pointsService.updatePointsStatus(userid, 5);
						resultRecord.set("eventsPointsOne", 1);
					}else {
						int preceivestatus = list4.get(0).get("preceivestatus");
						if(preceivestatus == 0) {
							Db.update("update pointsReceive set preceivestatus=1 where puserid=" + userid+ " and ptid=5");
							resultRecord.set("eventsPointsOne", 1);
						}else {
							resultRecord.set("eventsPointsOne", preceivestatus);
						}
						
						resultRecord.set("eventNum", num);
					}
				}else {
					if(list4.size()==0) {
						resultRecord.set("eventsPointsOne", 0);
					}else {
						int preceivestatus = list4.get(0).get("preceivestatus");
						if(preceivestatus==1) {
							Db.update("update pointsReceive set preceivestatus=0 where puserid=" + userid+ " and ptid=5");
							resultRecord.set("eventsPointsOne", 0);
						}else {
							resultRecord.set("eventsPointsOne", preceivestatus);
						}
						
					}
					resultRecord.set("eventNum", num);
				}
			} else {
				resultRecord.set("eventNum", 0);
				resultRecord.set("eventsPointsOne", 0);
			}
		}
		
		//��ѯ����50�˻��ֿ���
		List<Record> statusInviteInfo = Db.find("select ptstatus from pointstype where ptypeid=9");
		int statusInvite = statusUploadInfo.get(0).get("ptstatus");
		if(statusInvite == 0) {
			//��ѯ������������״̬
			List<Record> pointsInfo = Db.find("select inviteNum from points where puserid=" + userid);
			if(pointsInfo.size()!=0) {
				Integer inviteNum = pointsInfo.get(0).get("inviteNum");
				
				List<Record> list5 = Db.find("select preceivestatus from pointsReceive where puserid=" + userid
							+ " and ptid=" + 9);
				if(inviteNum>=50) {
					if(list5.size()==0) {
						pointsService.updatePointsStatus(userid, 9);
						resultRecord.set("inviteFriendsFifty", 1);
					}else {
						int preceivestatus = list5.get(0).get("preceivestatus");
						resultRecord.set("inviteFriendsFifty", preceivestatus);
					}
				}else {
					if(list5.size()==0) {
						resultRecord.set("inviteFriendsFifty", 0);
					}else {
						int preceivestatus = list5.get(0).get("inviteFriendsFifty");
						resultRecord.set("inviteFriendsFifty", preceivestatus);
					}
				}
				
				resultRecord.set("inviteNum", inviteNum);
			} else {
				resultRecord.set("inviteNum", 0);
				resultRecord.set("inviteFriendsFifty", 0);
			}
		}
		
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}
	
	/**
	 * ��ʾ���ּ�¼
	 */
	public void historyReceivePoints() {
		String userid = this.getPara("userid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		int pagesize = 10;
		int page = (pagenum-1)*pagesize;
		List<Record> list= Db.find
				("select ptname,receivePoints,prstatus,receiveTime from pointsreceive where puserid=" + userid + 
						" and (TO_DAYS(NOW()) - TO_DAYS(receiveTime) <= 10) and ptname is not null ORDER BY receiveTime desc LIMIT "+ page+","+ pagesize);
		
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * ��ʾ�һ���¼
	 */
	public void exchangePoints() {
		String userid = this.getPara("userid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		int pagesize = 10;
		int page = (pagenum-1)*pagesize;
		List<Record> list= Db.find
				("select pbTypeName,pbpoints,pbstatus,pbtime from pointsbonus where pbuserid=" + userid 
						+ " and (TO_DAYS(NOW()) - TO_DAYS(pbtime) <= 365) ORDER BY pbtime desc LIMIT " + page+","+ pagesize);
		
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
		
	}
	
	/**
	 * ��ʾ�����̳�
	 */
	public void showPointsShop() {
		List<Record> list= Db.find("select * from pointsgift");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * ��ʾ������Ʒ
	 */
	public void showSingleGoods() {
		String pgid = this.getPara("pgid");
		List<Record> list= Db.find("select * from pointsgift where pgid=" + pgid);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * ��֤�Ƿ���ֻ���
	 */
	public void RegisterVerifyPhone() {
		String userid = this.getPara("userid");
		List<Record> list= Db.find("select uphone from users where userid=" + userid + " and uphone is not null");
		System.out.println(list.size());
		if (list.size()==0) {
			jsonString = jsonData.getJson(1002, "�ֻ�����δ��");
		} else {
			jsonString = jsonData.getJson(0, "�ֻ������Ѱ�");
		}
		// ���ؽ��
		renderText(jsonString);
	}
	
	/**
	 * ������֤��
	 */
	public void SendVerifyMessage() {
		// ��ȡ����
		String phonenumber = this.getPara("phonenumber");
		// ��ȡ���ŷ��ͽ��
		jsonString = pointsService.verifyMessage(phonenumber);
		// ���ؽ��
		renderText(jsonString);
	}
	
	/**
	 * ���ֻ�����
	 */
	public void bindingPhone() {
		String phonenumber = this.getPara("phonenumber");
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select uphone from users where uphone=" + phonenumber);
		if(list!=null&&list.size()!=0) {
			jsonString = jsonData.getJson(-100, "�ֻ����Ѵ���");
		}else {
			User user = new User().findById(userid);
			user.set("uphone", phonenumber);
			boolean flag = user.update();
			if (flag) {
				jsonString = jsonData.getJson(0, "success");

			} else {
				jsonString = jsonData.getJson(-50, "��ʧ��");
			}
		}
		
		// ���ؽ��
		renderText(jsonString);
	}
	
	/**
	 * ��ȡ��Ʒ
	 */
	@Before(Tx.class)
	public void receivePointsGift() {
		String accountNumber = this.getPara("accountNumber");//������Ʒ�˺�
		String pgid = this.getPara("pgid");
		String userid = this.getPara("userid");
		String username = this.getPara("username");//�ռ�������
		String phoneNumber = this.getPara("phoneNumber");//�ռ��˵绰
		String address = this.getPara("address");//�ռ��˵�ַ
		
		User user = new User().findById(userid);
		PointsGift gift = new PointsGift().findById(pgid);
		
		String pgname = gift.get("pgname");//���ѵ���Ʒ����
		int pgpoints = gift.get("pgpoints");//���ѵĻ���
		String unickname = user.get("unickname");//���ѵ��û��ǳ�
		
		//���Ѻ�ı����
		List<Record> list = Db.find("select * from points where puserid=" +userid);
		//���Ѻ��¼�����ּ�¼����
		List<Record> list2 = Db.find("select * from pointsreceive where puserid=" + userid);
		if(list.size()==0) {
			jsonString = jsonData.getJson(-100, "���ֲ���");
		}else {
			String poid = list.get(0).get("poid").toString();
			int totalPoints = list.get(0).get("totalPoints");
			int consumePoints = list.get(0).get("consumePoints");//ԭ�������ѻ���
			int useablePoints = list.get(0).get("useablePoints");//ԭ���Ŀ��û���
			boolean flag2 = false;
			boolean flag1 = false;
			boolean flag3 = false;
			if(useablePoints<pgpoints) {
				jsonString = jsonData.getJson(-100, "���ֲ���");
			}else {
				PointsBonus bonus = new PointsBonus();
				bonus.set("pbuserid", userid).set("pbgiftid", pgid).set("pbuname", unickname)
					.set("pbTypeName", pgname).set("pbpoints", pgpoints).set("pbusername", username)
					.set("pbphone", phoneNumber).set("pbaccount", accountNumber).set("pbaddress", address)
					.set("beforepoints", useablePoints).set("laterpoints", useablePoints-pgpoints);
				flag1 = bonus.save();
				Points points = new Points().findById(poid);
				points.set("consumePoints", consumePoints+pgpoints).set("useablePoints", useablePoints-pgpoints);
				flag2 = points.update();
				PointsReceive pointsReceive = new PointsReceive();
				pointsReceive.set("puserid", userid).set("pgid", pgid).set("ptname", pgname).set("receivePoints", pgpoints)
							.set("beforepoints", useablePoints).set("laterpoints", useablePoints-pgpoints)
							.set("prstatus", 1).set("premark", "����" + pgname +"������" +pgpoints );
				flag3 = pointsReceive.save();
				
			}
			
			if (flag1 && flag2 && flag3) {
				jsonString = jsonData.getJson(0, "success");

			} else {
				jsonString = jsonData.getJson(-50, "��������ʧ��");
			}
		}
		
		// ���ؽ��
		renderText(jsonString);
		
	}
}
