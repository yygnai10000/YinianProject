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
	
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	
	private ActivityService activityService = new ActivityService();
	// enhance方法对目标进行AOP增强
	EventService TxService = enhance(EventService.class);
	private YinianService TxYinianService = enhance(YinianService.class);
	private EventService eventService = new EventService();// 业务层对象
	private PointsService pointsService = new PointsService();
	private YinianService yinianService = new YinianService();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	

	
	/**
	 * 成功邀请好友
	 */
	@Before(Tx.class)
	public void SuccessInviteFriend() {
		String userid = this.getPara("userid");
		// 增加邀请人数
		boolean flag = pointsService.recordSuccessInviteFriend(userid);
		
		// 增加用户总空间0.5G = 524288 KB
		boolean increaseFlag = false;
		boolean pointsFlag = false;
		if (flag) {
			User user = new User();
			increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 524288.00);
			Encourage en = new Encourage();
			List<Record> encourageInfo = Encourage.getEncourageInfo(userid);
			List<Record> statusInfo = Db.find("select ptstatus from pointstype where ptypeid=8");
			int status = statusInfo.get(0).get("ptstatus");//状态为0时积分开
			if(status == 0) {
				pointsFlag = pointsService.recordLongPoints(userid,8);//邀请成功增加积分
			}
		}
		jsonString = dataProcess.updateFlagResult(flag && increaseFlag && pointsFlag);
		renderText(jsonString);
	}
	
	/**
	 * 上传动态
	 */
	@Before(CrossDomain.class)
	public void UploadEvent() {
		// 谁发到哪个空间内
		String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
		String groupid = this.getPara("groupid") == null ? "" : this.getPara("groupid");
		// 图片
		String picAddress = this.getPara("picAddress") == null ? "" : this.getPara("picAddress");
		// 文字
		String content = this.getPara("content") == null ? "" : this.getPara("content");
		// 语音
		String audio = this.getPara("audio") == null ? "" : this.getPara("audio");
		// 地点
		String place = this.getPara("place") == null ? "" : this.getPara("place");
		String placePic = this.getPara("placePic") == null ? "" : this.getPara("placePic");// 位置生成的图片地址
		String placeLongitude = this.getPara("placeLongitude") == null ? "" : this.getPara("placeLongitude");// 经度
		String placeLatitude = this.getPara("placeLatitude") == null ? "" : this.getPara("placeLatitude");// 纬度
		// 和谁
		String peopleName = this.getPara("peopleName") == null ? "" : this.getPara("peopleName");
		// 动态以哪个要素为主
		String main = this.getPara("main") == null ? "" : this.getPara("main"); // 0--照片 1--文字 2--语音 3--地点
		// 其他元素
		String storage = this.getPara("storage") == null ? "" : this.getPara("storage");// 存储空间
		String source = this.getPara("source") == null ? "" : this.getPara("source");// 判断接口来源
		String isPush = this.getPara("isPush") == null ? "" : this.getPara("isPush"); // 推送判断 app:yes/no 小程序:true/false
		String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // 小程序推送表单ID

		// 插入formID
		// FormID.insertFormID(userid, formID);
		if (!userid.equals("") && !formID.equals("")) {
			FormID.insert(userid, formID);
		}
		// 判断存储空间是否有传
		double storagePlace;
		if (storage == null || storage.equals("")) {
			storagePlace = 0.00;
		} else {
			storagePlace = Double.parseDouble(storage);
		}

		// 接口来源为web，需要解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		// 地址字段数据处理
		String firstPic = null;
		String[] picArray = new String[0];
		if (picAddress != null && !picAddress.equals("")) {
			picArray = dataProcess.getPicAddress(picAddress, "private");
			// 图片鉴黄
			picArray = dataProcess.PictureVerify(picArray);
			// 获取动态第一张图片地址,可能没有上传图片
			firstPic = (picArray.length == 0 ? null : picArray[0]);
		}

		// 图片都被过滤掉，不插入数据
		int eid = 0;
		if (main.equals("0") && picArray.length == 0) {
			List<Record> errorList = new ArrayList<Record>();
			Record r = new Record();
			r.set("picList", new ArrayList<String>());
			errorList.add(r);
			jsonString = jsonData.getSuccessJson(errorList);
		} else {
			// 支持同时上传到多个空间
			String[] IDs = groupid.split(",");
			// 逐个空间上传
			for (int i = 0; i < IDs.length; i++) {
				// 上传权限开关
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
					// 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
					int isSynchronize = (i == 0 ? 0 : 1);
					eid = TxService.upload(userid, IDs[i], picArray, content, audio, place, placePic, placeLongitude,
							placeLatitude, peopleName, main, storagePlace, firstPic, isPush, source, isSynchronize,
							formID);
					if (eid != 0) {
						// 说明上传成功
						List<Record> result = eventService.getSingleEvent(eid, source);// 获取动态的信息
						System.out.println(result);
						jsonString = jsonData.getSuccessJson(result);
						//开启推送 by lk
//						if(CommonParam.canPublish){
//							//Group group = new Group().findById(IDs[i]);
//							String gOrigin = String.valueOf(group.getLong("gOrigin"));
//							if (gOrigin.equals("0")) {
//								ExecutorService exec = Executors.newCachedThreadPool();
//								exec.execute(new EventPushPicNumThread(IDs[i], picArray.length,userid));
////								// 关闭线程池
//								exec.shutdown();
//							}
//						}
						/*
						 * //即时上传动态推送开关 Boolean eventIsPush = true; //判断是否是普通相册 在普通相册点赞才推送 String
						 * gOrigin =String.valueOf(group.getLong("gOrigin"));;
						 * 
						 * if(gOrigin.equals("1")) { eventIsPush = false; } if(eventIsPush) { // 创建一个线程池
						 * ExecutorService exec = Executors.newCachedThreadPool();
						 * System.out.println(picArray.length); exec.execute(new
						 * EventPushPicNumThread(IDs[i],picArray.length)); // 关闭线程池 exec.shutdown();
						 * 
						 * }
						 */
					} else {
						jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
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
		// 返回结果
		renderText(jsonString);
//		if (eid != 0) {
//			String[] IDs = groupid.split(",");
//			for (int i = 0; i < IDs.length; i++) {
//				// 即时上传动态推送开关
//				Boolean eventIsPush = true;
//				// 判断是否是普通相册 在普通相册点赞才推送
//				Group group = new Group().findById(IDs[i]);
//				String gOrigin = String.valueOf(group.getLong("gOrigin"));
//				if (gOrigin.equals("1")) {
//					eventIsPush = false;
//				}
//				if (eventIsPush) {
//					// 创建一个线程池
//					ExecutorService exec = Executors.newCachedThreadPool();
//					System.out.println(picArray.length);
//					exec.execute(new EventPushPicNumThread(IDs[i], picArray.length));
//					// 关闭线程池
//					exec.shutdown();
//
//				}
//			}
//		}
		// 鉴黄线程
		// if(eid != 0) {
		// // 创建一个线程池
		// ExecutorService exec = Executors.newCachedThreadPool();
		// // 执行鉴黄线程
		// exec.execute(new PictureVerifyThread(userid,eid, main));
		// // 关闭线程池
		// exec.shutdown();
		// }
	}
	
	
	/**
	 * 上传短视频
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

		// 判断存储空间是否有传
		double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
		cover = (cover == null ? "" : cover);
		time = (time == null ? "0" : time);

		// 资源地址加前缀
		address = CommonParam.qiniuPrivateAddress + address;
		// 视频鉴黄,视频封面图片鉴黄true为色情视频
		boolean videoJudge = dataProcess.VideoVerify(address);
		boolean coverJudge = false;
		if (!cover.equals(""))
			coverJudge = dataProcess.SinglePictureVerify(cover);

		if (videoJudge || coverJudge) {
			jsonString = jsonData.getJson(1039, "资源违规");
		} else {
			// 支持同时上传到多个空间
			String[] IDs = groupid.split(",");
			boolean flag = true;
			int eventID = 0;
			// 逐个空间上传
			for (int i = 0; i < IDs.length; i++) {
				// 上传权限开关
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
					// 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
					int isSynchronize = (i == 0 ? 0 : 1);
					// 上传短视频
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
				// 说明上传成功
				List<Record> result = eventService.getSingleEvent(eventID, source);// 获取动态的信息
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}
		}
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 每天签到+10积分，连续7天签到额外奖励+100积分
	 */
	public void DailySignInNew() throws ParseException {
		String userid = this.getPara("userid");
		String signType = this.getPara("signType");
		// 签到
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
			jsonString = jsonData.getJson(-50, "参数错误");
			renderText(jsonString);
		}
		

		// 增加用户空间100M=102400KB
		boolean increaseFlag = false;
		// 增加用户积分
		boolean receiveFlag = false;
		if (signInFlag) {
			User user = new User();
			increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 102400.00);
		}
		if (signInFlag) {
			List<Record> statusListInfo = Db.find("select ptstatus from pointstype where ptypeid=1");
			int status = statusListInfo.get(0).get("ptstatus");//状态为0时积分开
			if(status==0) {
				receiveFlag = pointsService.recordPoints(userid, 1);
				long to = sdf.parse(signEndDate).getTime();
				long from = sdf.parse(signStartDate).getTime();
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				//当连续签到七天时，获得额外的100积分奖励
				if(signDay == 7) {
					receiveFlag = pointsService.recordPoints(userid, 2);
				}
			}
			
		}
		if (signInFlag && increaseFlag && receiveFlag) {
			jsonString = jsonData.getSuccessJson();
		} else {
			jsonString = jsonData.getJson(2030, "当天已签到");
		}

		renderText(jsonString);

	}
	
	/**
	 * 共享相册点赞与取消点赞 lk 修改返回对象：
	 * 每天第一个点赞，每个获得+10积分
	 */
	public void AttachOrRemoveExpressionByLkNew() {
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID

		// 插入formID
		//FormID.insertFormID(userid, formID);
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		int status = 0;
		if (source != null && source.equals("app")) {
			// app直接传值
			status = Integer.parseInt(type);
		} else {
			// 小程序传英文,将type改成对应status
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
		// 判断用户是否有相关操作
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
					// 没有操作过
					like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
					if (like.save()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						Record r=new Record();
						r.set("likeCnt",0);
						//点赞数读取redis缓存，若没有缓存则读取数据库，同时更新缓存
						Jedis jedis = RedisUtils.getRedis();
						if(null!=jedis) {
							//从缓存中读取当前eid的点赞数
							String likeCnt = jedis.get("likeCnt_"+eid);
							if(null!=likeCnt&&!"".equals(likeCnt)) {
								//点赞成功后缓存点赞数加1
								int likeCntInt = Integer.valueOf(likeCnt) + 1;
								String jr = jedis.set("likeCnt_"+eid, String.valueOf(likeCntInt));
								if(null!=jr&&"OK".equals(jr)) {
									r.set("likeCnt", likeCntInt);
								}
							}else {
								//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
								List<Record> cntList=Db.find(likeCntSql);
								if(!cntList.isEmpty()){
									r.set("likeCnt",cntList.get(0).get("cnt"));
									jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
								}
							}
							//释放redis
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
							//点赞推送 begin
							if(CommonParam.canPublish&&status==0){
								Event event = new Event().findById(eid);
								String egroupid = event.get("egroupid").toString();
								String uid = event.get("euserid").toString();
								//判断是否是普通相册 在普通相册点赞才推送
								String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
								if(gOrigin.equals("0")&&!userid.equals(uid)) {
									User user = new User().findById(userid);
									//点赞人的姓名
									String username = user.get("unickname").toString();
									//发布人的uopenid
									
									String uopenid = new User().findById(uid).get("uopenid");
									if(null!=uopenid&&!uopenid.equals("")){
										//提取发布人的formid
										List<Record> formidList = Db.find("select formID from formid where 1 "
												+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
												+ "order by time asc limit 1");
										//点赞成功发送推送
										if (null!=formidList&&!formidList.isEmpty()) {
											String formid = formidList.get(0).get("formID");
											System.out.println("formid=="+formid);
											//点赞成功发送推送
											SmallAppPush smallAppPush = new SmallAppPush();
											smallAppPush.likeIsPush(formid, uopenid, eid, username);
											System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
											Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
										}
									}
								}
							}
							//点赞推送 end
//							//点赞推送开关
//							Boolean likePushFlag = true;
//							Event event = new Event().findById(eid);
//							String egroupid = event.get("egroupid").toString();
//							//判断是否是普通相册 在普通相册点赞才推送
//							String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//							if(gOrigin.equals("1")) {
//								likePushFlag = false;
//							}
//							if(likePushFlag) {
//								User user = new User().findById(userid);
//								//点赞人的姓名
//								String username = user.get("unickname").toString();
//								//发布人的uopenid
//								String uid = event.get("euserid").toString();
//								String uopenid = new User().findById(uid).get("uopenid");
//								//提取发布人的formid
//								List<Record> formidList = Db.find("select formID from formid where 1 "
//										+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//										+ "order by time asc limit 1");
//								//点赞成功发送推送
//								if (null!=formidList&&!formidList.isEmpty()) {
//									String formid = formidList.get(0).get("formID");
//									System.out.println("formid=="+formid);
//									//点赞成功发送推送
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
					// 有操作过
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
						//点赞数读取redis缓存，若没有缓存则读取数据库，同时更新缓存
						Jedis jedis = RedisUtils.getRedis();
						if(null!=jedis) {
							//从缓存中读取当前eid的点赞数
							String likeCnt = jedis.get("likeCnt_"+eid);
							if(null!=likeCnt&&!"".equals(likeCnt)) {
								if(status == 1) {
									//取消点赞时，缓存点赞数减1
									int likeCntInt = Integer.valueOf(likeCnt)>0?Integer.valueOf(likeCnt) - 1:0;
									String jr = jedis.set("likeCnt_"+eid, String.valueOf(likeCntInt));
									if(null!=jr&&"OK".equals(jr)) {
										r.set("likeCnt", likeCntInt);
									}
								}else {
									//非取消点赞，缓存点赞数不变
									r.set("likeCnt", Integer.valueOf(likeCnt));
								}
							}else {
								//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
								List<Record> cntList=Db.find(likeCntSql);
								if(!cntList.isEmpty()){
									r.set("likeCnt",cntList.get(0).get("cnt"));
									jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
								}
							}
							//释放redis
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
		// 判断用户是否有相关操作
	}
	
	/**
	 * 共享相册点赞与取消点赞
	 * 
	 */
	public void AttachOrRemoveExpression() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID"); // 小程序推送表单ID
		// 插入formID
		FormID.insertFormID(userid, formID);
		int status = 0;
		if (source != null && source.equals("app")) {
			// app直接传值
			status = Integer.parseInt(type);
		} else {
			// 小程序传英文,将type改成对应status
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

		// 判断用户是否有相关操作
		List<Record> judge = Db
				.find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");

		List<Record> result = new ArrayList<Record>();
		String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
				+ eid + " and likeStatus!=1";

		Like like;
		if (judge.size() == 0) {
			// 没有操作过
			like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
			if (like.save()) {
				result = Db.find(sql);
				result = dataProcess.changeLikeStatusToWord(result);
				jsonString = jsonData.getSuccessJson(result);
				//点赞推送 begin
				if(CommonParam.canPublish&&status==0){
					Event event = new Event().findById(eid);
					String egroupid = event.get("egroupid").toString();
					String uid = event.get("euserid").toString();
					//判断是否是普通相册 在普通相册点赞才推送
					String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
					if(gOrigin.equals("0")&&!userid.equals(uid)) {
						User user = new User().findById(userid);
						//点赞人的姓名
						String username = user.get("unickname").toString();
						//发布人的uopenid
						
						String uopenid = new User().findById(uid).get("uopenid");
						if(null!=uopenid&&!uopenid.equals("")){
							//提取发布人的formid
							List<Record> formidList = Db.find("select formID from formid where 1 "
									+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
									+ "order by time asc limit 1");
							//点赞成功发送推送
							if (null!=formidList&&!formidList.isEmpty()) {
								String formid = formidList.get(0).get("formID");
								System.out.println("formid=="+formid);
								//点赞成功发送推送
								SmallAppPush smallAppPush = new SmallAppPush();
								smallAppPush.likeIsPush(formid, uopenid, eid, username);
								System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
								Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
							}
						}
					}
				}
				//点赞推送 end
//				//点赞推送开关
//				Boolean likePushFlag = true;
//				Event event = new Event().findById(eid);
//				String egroupid = event.get("egroupid").toString();
//				//判断是否是普通相册 在普通相册点赞才推送
//				String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//				if(gOrigin.equals("1")) {
//					likePushFlag = false;
//				}
//				if(likePushFlag) {
//					User user = new User().findById(userid);
//					//点赞人的姓名
//					String username = user.get("unickname").toString();
//					//发布人的uopenid
//					String uid = event.get("euserid").toString();
//					String uopenid = new User().findById(uid).get("uopenid");
//					//提取发布人的formid
//					List<Record> formidList = Db.find("select formID from formid where 1 "
//							+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//							+ "order by time asc limit 1");
//					//点赞成功发送推送
//					if (null!=formidList&&!formidList.isEmpty()) {
//						String formid = formidList.get(0).get("formID");
//						System.out.println("formid=="+formid);
//						//点赞成功发送推送
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
			// 有操作过
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
	 * 发表评论1 1.1版本 新增返回字段 cid
	 * 每天第一个评论加20分
	 */
	public void SendComment1() {
		String commentUserId = this.getPara("commentUserId")==null?"":this.getPara("commentUserId");// 评论人ID
		String commentedUserId = this.getPara("commentedUserId");// 被评论人ID
		String eventId = this.getPara("eventId");// 事件ID
		String content = this.getPara("content");// 评论内容
		String place = this.getPara("place");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID

		if(!commentUserId.equals("")&&!formID.equals("")){
			FormID.insert(commentUserId, formID);
		}
		String cid = TxYinianService.sendComment1(commentUserId, commentedUserId, eventId, content, place);
		if (cid.equals("")) {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		} else {
			List<Record> list = dataProcess.makeSingleParamToList("cid", cid);
			jsonString = jsonData.getJson(0, "success", list);
			//评论推送 begin
			if(CommonParam.canPublish){
				Event event = new Event().findById(eventId);
				String egroupid = event.get("egroupid").toString();
//				//判断是否是普通相册 在普通相册才推送
				String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
				if(gOrigin.equals("0")) {
					User user = new User().findById(commentUserId);
					//评论人的姓名
					String username = user.get("unickname").toString();
					//被评论人的uopenid
					String uid = event.get("euserid").toString();
					String uopenid = new User().findById(uid).get("uopenid");
					if(null!=uopenid&&!uopenid.equals("")&&!uid.equals(commentUserId)){
						//提取发布人的formid
						List<Record> formidList = Db.find("select formID from formid where 1 "
								+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
								+ "order by time asc limit 1");
						
						if(null!=formidList&&!formidList.isEmpty()) {
							String formid = formidList.get(0).get("formID");
							//评论成功发送推送
							SmallAppPush smallAppPush = new SmallAppPush();
							smallAppPush.commentIsPush(formid, uopenid, eventId, username);
							Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
						}
					}
				}
			}
			//评论推送 end
//			//评论推送开关
//			Boolean commentPushFlag = true;
//			Event event = new Event().findById(eventId);
//			String egroupid = event.get("egroupid").toString();
//			//判断是否是普通相册 在普通相册才推送
//			String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//			if(gOrigin.equals("1")) {
//				commentPushFlag = false;
//			}
//			if(commentPushFlag) {
//				User user = new User().findById(commentUserId);
//				//评论人的姓名
//				String username = user.get("unickname").toString();
//				//被评论人的uopenid
//				String uid = event.get("euserid").toString();
//				String uopenid = new User().findById(uid).get("uopenid");
//				//提取发布人的formid
//				List<Record> formidList = Db.find("select formID from formid where 1 "
//						+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//						+ "order by time asc limit 1");
//				
//				if(null!=formidList&&!formidList.isEmpty()) {
//					String formid = formidList.get(0).get("formID");
//					//评论成功发送推送
//					SmallAppPush smallAppPush = new SmallAppPush();
//					smallAppPush.commentIsPush(formid, uopenid, eventId, username);
//					Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
//				}
//			}
		}
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 领取动态积分并记录用户积分和积分领取详情
	 */
	public void receivePoints() {
		int type = Integer.parseInt(this.getPara("type"));
		String userid = this.getPara("userid");
		//判断是否领取，避免重复领取
		List<Record> list = Db.find("select * from pointsReceive where puserid=" + userid 
				+ " and ptid="+ type + " and to_days(receiveTime)=to_days(now())");
		if(null!=list && list.size()!=0) {
			int preceivestatus = list.get(0).get("preceivestatus");
			//判断是否已领取，控制快速点击重复领取
			if(preceivestatus == 2) {
				jsonString = jsonData.getJson(1000, "已领取");
			}else {
				boolean flag = pointsService.recordPoints(userid, type);
				if (flag) {
					jsonString = jsonData.getJson(0, "success");
				} else {
					jsonString = jsonData.getJson(1002, "领取失败");
				}
			}
			
		}else {
			jsonString = jsonData.getJson(1002, "领取失败");
		}
		// 返回结果
		renderText(jsonString);
		
	}
	
	/**
	 * 显示个人积分信息
	 */
	public void showPersonInfo() throws ParseException {
		String userid = this.getPara("userid");
		Record resultRecord = new Record();
		
		String today = sdf.format(new Date());
		//判断今日是否签到
		Signold sign = new Signold();
		List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
		if(signInfoList.size()==0) {
			resultRecord.set("isTodaySign", false);
		}else {
			String signStartDate = signInfoList.get(0).get("signStartDate").toString();
			String signEndDate = signInfoList.get(0).get("signEndDate").toString();
			// 签到天数
			long to = sdf.parse(signEndDate).getTime();
			long from = sdf.parse(signStartDate).getTime();
			long todays = sdf.parse(today).getTime();
			int count = (int) ((todays - to) / (1000 * 60 * 60 * 24));
			System.out.println(count);
			//判断是否断签。如果断签显示signDay为0
			if(count>=2) {
				int signDay = 0;
				resultRecord.set("signDay", signDay);
			}else {
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				resultRecord.set("signDay", signDay);
			}
			//是否显示签到福利
			String firstSign=signInfoList.get(0).get("signFirstTime").toString();
			long f=sdf.parse(firstSign).getTime();
			if(f>sdf.parse("2017-12-14 23:59:59").getTime()){
				resultRecord.set("showSign", 1);//不显示签到福利
			}else{
				resultRecord.set("showSign", 0);//显示签到福利
			}
			// 当天是否签到
			if (signEndDate.equals(today)) {
				resultRecord.set("isTodaySign", true);
			} else {
				resultRecord.set("isTodaySign", false);
			}
		}
		
		if (userid != null) {
			User user = new User().findById(userid);
			String upic = user.get("upic");//用户头像
			String uname = user.get("unickname");//用户昵称
			Double uusespace = user.get("uusespace");//用户已使用空间
			resultRecord.set("uusespace", uusespace);
			resultRecord.set("upic", upic);//用户头像
			resultRecord.set("unickname", uname);//用户昵称
			
			//获取用户积分
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
	 * 显示积分任务详情
	 */
	
	public void showPointsTask() throws ParseException {
		String userid = this.getPara("userid");
		Record resultRecord = new Record();

		String today = sdf.format(new Date());
		//判断今日是否签到
		Signold sign = new Signold();
		List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
		if(signInfoList.size()==0) {
			resultRecord.set("isTodaySign", false);
		} else {
			String signStartDate = signInfoList.get(0).get("signStartDate").toString();
			String signEndDate = signInfoList.get(0).get("signEndDate").toString();
			// 签到天数
			long to = sdf.parse(signEndDate).getTime();
			long from = sdf.parse(signStartDate).getTime();
			long todays = sdf.parse(today).getTime();
			int count = (int) ((todays - to) / (1000 * 60 * 60 * 24));
			System.out.println(count);
			//判断是否断签。如果断签显示signDay为0
			if(count>=2) {
				int signDay = 0;
				resultRecord.set("signDay", signDay);
			}else {
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				resultRecord.set("signDay", signDay);
			}
			// 当天是否签到
			if (signEndDate.equals(today)) {
				resultRecord.set("isTodaySign", true);
			} else {
				resultRecord.set("isTodaySign", false);
			}
		}
		
		//评论积分开关
		List<Record> statusCommentsInfo = Db.find("select ptstatus from pointstype where ptypeid=6");
		int statusComments = statusCommentsInfo.get(0).get("ptstatus");//状态为0时积分开
		if(statusComments==0) {
			List<Record> commentsList = Db.find("select ctime from comments where cuserid=" + userid + " and cstatus=0" +" and to_days(ctime)=to_days(now())");
			//System.out.println("commentsList="+commentsList.size());
			//System.out.println("commentsList="+commentsList);
			if(commentsList.size()!=0) {
				String ctime=commentsList.get(0).get("ctime").toString();
				System.out.println("ctime=" + ctime);
				//获取今天的日期
				if(ctime!=null && !ctime.equals("")){
					Timestamp timestamp = Timestamp.valueOf(ctime);
					String commtime = sdf.format(timestamp);
					if(commtime != today && !commtime.equals(today)) {
						resultRecord.set("commentsPoints", 0);
					}else {
						//查询每日评论积分状态
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
		
		//点赞开关积分
		List<Record> statusLikeInfo = Db.find("select ptstatus from pointstype where ptypeid=7");
		int statusLike = statusLikeInfo.get(0).get("ptstatus");//状态为0时积分开
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
						//查询每日点赞状态
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
		
		//上传积分开关
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
						
						//查询每日发布动态积分状态
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
	
		//查询第一次创建相册状态
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
		
		
		//查询100条动态积分开关
		List<Record> statusEventOneInfo = Db.find("select ptstatus from pointstype where ptypeid=5");
		int statusEventOne = statusUploadInfo.get(0).get("ptstatus");
		if(statusEventOne == 0) {
			//查询已发布100条动态积分状态
			List<Record> eventsList = Db.find("select count(eid) as num from events where euserid=" + userid + " and estatus=0" +" and isSynchronize=0"+ " and euploadtime>'2018-02-01 00:00:00'");
			if(eventsList.size()!=0) {
				Long num = eventsList.get(0).get("num");
				//查询100条积分奖励状态
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
		
		//查询邀请50人积分开关
		List<Record> statusInviteInfo = Db.find("select ptstatus from pointstype where ptypeid=9");
		int statusInvite = statusUploadInfo.get(0).get("ptstatus");
		if(statusInvite == 0) {
			//查询邀请人数积分状态
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
	 * 显示积分记录
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
	 * 显示兑换记录
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
	 * 显示积分商城
	 */
	public void showPointsShop() {
		List<Record> list= Db.find("select * from pointsgift");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * 显示单个商品
	 */
	public void showSingleGoods() {
		String pgid = this.getPara("pgid");
		List<Record> list= Db.find("select * from pointsgift where pgid=" + pgid);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * 验证是否绑定手机号
	 */
	public void RegisterVerifyPhone() {
		String userid = this.getPara("userid");
		List<Record> list= Db.find("select uphone from users where userid=" + userid + " and uphone is not null");
		System.out.println(list.size());
		if (list.size()==0) {
			jsonString = jsonData.getJson(1002, "手机号码未绑定");
		} else {
			jsonString = jsonData.getJson(0, "手机号码已绑定");
		}
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 发送验证码
	 */
	public void SendVerifyMessage() {
		// 获取参数
		String phonenumber = this.getPara("phonenumber");
		// 获取短信发送结果
		jsonString = pointsService.verifyMessage(phonenumber);
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 绑定手机号码
	 */
	public void bindingPhone() {
		String phonenumber = this.getPara("phonenumber");
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select uphone from users where uphone=" + phonenumber);
		if(list!=null&&list.size()!=0) {
			jsonString = jsonData.getJson(-100, "手机号已存在");
		}else {
			User user = new User().findById(userid);
			user.set("uphone", phonenumber);
			boolean flag = user.update();
			if (flag) {
				jsonString = jsonData.getJson(0, "success");

			} else {
				jsonString = jsonData.getJson(-50, "绑定失败");
			}
		}
		
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 领取奖品
	 */
	@Before(Tx.class)
	public void receivePointsGift() {
		String accountNumber = this.getPara("accountNumber");//虚拟商品账号
		String pgid = this.getPara("pgid");
		String userid = this.getPara("userid");
		String username = this.getPara("username");//收件人姓名
		String phoneNumber = this.getPara("phoneNumber");//收件人电话
		String address = this.getPara("address");//收件人地址
		
		User user = new User().findById(userid);
		PointsGift gift = new PointsGift().findById(pgid);
		
		String pgname = gift.get("pgname");//消费的商品名字
		int pgpoints = gift.get("pgpoints");//消费的积分
		String unickname = user.get("unickname");//消费的用户昵称
		
		//消费后改变积分
		List<Record> list = Db.find("select * from points where puserid=" +userid);
		//消费后记录到积分记录里面
		List<Record> list2 = Db.find("select * from pointsreceive where puserid=" + userid);
		if(list.size()==0) {
			jsonString = jsonData.getJson(-100, "积分不足");
		}else {
			String poid = list.get(0).get("poid").toString();
			int totalPoints = list.get(0).get("totalPoints");
			int consumePoints = list.get(0).get("consumePoints");//原来的消费积分
			int useablePoints = list.get(0).get("useablePoints");//原来的可用积分
			boolean flag2 = false;
			boolean flag1 = false;
			boolean flag3 = false;
			if(useablePoints<pgpoints) {
				jsonString = jsonData.getJson(-100, "积分不足");
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
							.set("prstatus", 1).set("premark", "购买" + pgname +"消耗了" +pgpoints );
				flag3 = pointsReceive.save();
				
			}
			
			if (flag1 && flag2 && flag3) {
				jsonString = jsonData.getJson(0, "success");

			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		}
		
		// 返回结果
		renderText(jsonString);
		
	}
}
