package yinian.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.AuthorizePhoto;
import yinian.model.Peep;
import yinian.model.User;
import yinian.model.UserPhoto;
import yinian.service.SliverService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class SliverController extends Controller {

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private QiniuOperate operate = new QiniuOperate(); // 七牛云处理类
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
	private SimpleDateFormat sdfOfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SliverService service = new SliverService();

	/**
	 * 存储用户照片
	 */
	@Before(Tx.class)
	public void SaveUserPhoto() {

		String userid = this.getPara("userid");
		String[] url = this.getPara("url").split(",");
		String[] key = this.getPara("key").split(",");
		String time = this.getPara("time");

		String[] timeArray;
		if (time == null) {
			// 没有传时间则将当前时间设为默认值
			String now = sdf.format(new Date());
			timeArray = new String[url.length];
			for (int i = 0; i < timeArray.length; i++)
				timeArray[i] = now;
		} else {
			timeArray = time.split(",");
			// 对时间格式进行检查
			for (int i = 0; i < timeArray.length; i++) {
				int year = Integer.parseInt(timeArray[i].split("-")[0]);
				if (year > 2100)
					timeArray[i] = sdf.format(new Date());
			}
		}

		if (userid.equals("2")) {
			jsonString = jsonData.getSuccessJson();
		} else {
			if (url.length == key.length) {
				boolean flag = false;
				for (int i = 0; i < url.length; i++) {
					UserPhoto photo = new UserPhoto()
							.set("userid", userid)
							.set("address",
									CommonParam.qiniuPrivateAddress + url[i])
							.set("key", key[i]).set("shootTime", timeArray[i]);
					if (photo.save()) {
						flag = true;
					} else {
						flag = false;
						break;
					}
				}
				jsonString = dataProcess.insertFlagResult(flag);
			} else {
				jsonString = jsonData.getJson(2, "请求参数错误");
			}
		}

		renderText(jsonString);
	}

	/**
	 * 删除用户照片
	 */
	@Before(Tx.class)
	public void DeleteUserPhoto() {

		String userid = this.getPara("userid");
		String[] keys = this.getPara("key").split(",");

		String key = "";
		for (int i = 0; i < keys.length; i++) {
			key += "'" + keys[i] + "',";
		}
		key = key.substring(0, key.length() - 1);

		int count = Db.update("update userPhoto set status=1 where `key` in ("
				+ key + ") and userid=" + userid + " and status=0 ");
		jsonString = dataProcess.updateFlagResult(count == keys.length);

		renderText(jsonString);

	}

	/**
	 * 获取用户所有的图片hash值
	 */
	public void GetUserAllPhotoHash() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select distinct `key`,shootTime from userPhoto where userid="
						+ userid
						+ " and status=0 and `key`!='' and `key` is not null");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 获取成员授权列表
	 */
	public void GetGroupMemberAuthorizeList() {
		int userid = Integer.parseInt(this.getPara("userid"));
		int groupid = this.getParaToInt("groupid");

		Peep peep = new Peep();
		String today = sdfOfDate.format(new Date());

		// 判断用户是否完成教程
		User user = new User().findById(userid);
		String isFinishWatchPhotoTutorial = user.get(
				"isFinishWatchPhotoTutorial").toString();// 0--未完成 1--已完成

		// 获取成员列表和授权信息
		List<Record> groupMemberList = Db
				.find("select userid,unickname,upic from users,groupmembers where userid=gmuserid and gmgroupid='"
						+ groupid
						+ "' and gmuserid != "
						+ userid
						+ "  and gmstatus=0 ");
		List<Record> authorizeList = Db
				.find("select authorizeUserID,authorizeBeUserID,authorizeStatus,authorizeCancelTime from authorizePhoto where (authorizeUserID="
						+ userid
						+ " or authorizeBeUserID="
						+ userid
						+ ") and authorizeGroupID=" + groupid + " ");

		// 四种授权情况的列表
		List<Record> mutualAuthorizeList = new ArrayList<Record>(); // 互相授权列表
		List<Record> mutualUnauthorizeList = new ArrayList<Record>();// 互不授权列表
		List<Record> unilateralOfMeList = new ArrayList<Record>();// 我单向授权列表
		List<Record> unilateralOfOthersList = new ArrayList<Record>();// 对方单向授权列表

		// 每个成员遍历，查看授权情况并归类
		for (Record record : groupMemberList) {

			int memberID = Integer.parseInt(record.get("userid").toString());
			boolean meAuthorize = false; // 我是否对该成员授权，默认为无
			boolean youAuthorize = false; // 该成员是否对我授权，默认为无

			for (Record authorize : authorizeList) {
				int authorizeUserID = Integer.parseInt(authorize.get(
						"authorizeUserID").toString());
				int authorizeBeUserID = Integer.parseInt(authorize.get(
						"authorizeBeUserID").toString());
				int status = Integer.parseInt(authorize.get("authorizeStatus")
						.toString());

				if (authorizeUserID == userid && authorizeBeUserID == memberID
						&& status == 1) {
					// 我授权了对方
					meAuthorize = true;
				}
				if (authorizeUserID == memberID && authorizeBeUserID == userid) {
					if (status == 1) {
						// 对方授权了我
						youAuthorize = true;
					} else {
						// 对方对我取消授权，但是今天看了我的内容，则今天对我的显示依然是对方对我授权了
						if (peep.JudgeIsUserPeepOtherToday(memberID, userid,
								groupid, today))
							youAuthorize = true;
					}
				}

			}

			if (meAuthorize && youAuthorize) {
				// 双方互相授权
				mutualAuthorizeList.add(record);
			}
			if (!meAuthorize && !youAuthorize) {
				// 双方互未授权
				mutualUnauthorizeList.add(record);
			}
			if (meAuthorize && !youAuthorize) {
				// 我单向授权对方
				unilateralOfMeList.add(record);
			}
			if (!meAuthorize && youAuthorize) {
				// 对方单向授权我
				unilateralOfOthersList.add(record);
			}

		}

		// 获取互相授权列表中当天用户间查看的信息
		for (Record record : mutualAuthorizeList) {
			int isMeSee = 0; // 我今天是否查看完对方的3张照片 0--未查看完 1--已查看完
			int seeNum = 0;// 对方查看的张数
			int memberID = Integer.parseInt(record.get("userid").toString());

			seeNum = peep.GetPeepPhotoNum(memberID, userid, groupid, today);
			// 判断我是否看过对方
			if (peep.JudgeIsUserPeepOtherToday(userid, memberID, groupid, today)
					&& peep.GetPeepPhotoNum(userid, memberID, groupid, today) == 3) {
				isMeSee = 1;
			}
			record.set("isMeSee", isMeSee).set("seeNum", seeNum);
		}

		// 构造返回对象
		List<Record> resultList = new ArrayList<Record>();
		Record record = new Record();
		record.set("mutualAuthorizeList", mutualAuthorizeList)
				.set("mutualUnauthorizeList", mutualUnauthorizeList)
				.set("unilateralOfMeList", unilateralOfMeList)
				.set("unilateralOfOthersList", unilateralOfOthersList)
				.set("isFinishWatchPhotoTutorial", isFinishWatchPhotoTutorial);
		resultList.add(record);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);

	}

	/**
	 * 授权与取消授权
	 */
	@Before(Tx.class)
	public void AuthorizeOrCancelForWatchPhoto() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String beUserid = this.getPara("beUserid");
		String type = this.getPara("type");

		String nowTime = sdf.format(new Date());
		AuthorizePhoto ap;
		switch (type) {
		case "authorize":
			// 单个授权
			// 判断是否有过授权
			List<Record> list = Db
					.find("select authorizeID from authorizePhoto where authorizeGroupID="
							+ groupid
							+ " and authorizeUserID="
							+ userid
							+ " and authorizeBeUserID=" + beUserid + "");
			if (list.size() == 0) {
				ap = new AuthorizePhoto().set("authorizeGroupID", groupid)
						.set("authorizeUserID", userid)
						.set("authorizeBeUserID", beUserid)
						.set("authorizeStatus", 1);
				jsonString = dataProcess.insertFlagResult(ap.save());
			} else {
				ap = new AuthorizePhoto().findById(list.get(0)
						.get("authorizeID").toString());
				ap.set("authorizeStatus", 1);
				jsonString = dataProcess.updateFlagResult(ap.update());
			}

			break;
		case "cancel":
			// 单个取消授权
			int count = Db
					.update("update authorizePhoto set authorizeStatus=0,authorizeCancelTime='"
							+ nowTime
							+ "' where authorizeGroupID="
							+ groupid
							+ " and authorizeUserID="
							+ userid
							+ " and authorizeBeUserID= " + beUserid + "  ");
			jsonString = dataProcess.updateFlagResult(count == 1);
			break;
		case "cancelAll":
			// 整个忆年内的好友全取消授权
			List<Record> judge = Db
					.find("select * from authorizePhoto where authorizeUserID="
							+ userid + " and authorizeStatus=1 ");
			int batch = Db
					.update("update authorizePhoto set authorizeStatus=0,authorizeCancelTime='"
							+ nowTime
							+ "' where authorizeUserID="
							+ userid
							+ " and authorizeStatus=1  ");
			jsonString = dataProcess.updateFlagResult(batch == judge.size());
			break;
		case "authorizeAll":
			// 授权空间内的全部好友
			List<Record> groupMemberList = Db
					.find("select userid,unickname,upic from users,groupmembers where userid=gmuserid and gmgroupid='"
							+ groupid
							+ "' and gmuserid != "
							+ userid
							+ "  and gmstatus=0 ");

			// 获取空间内已授权过的用户数据
			List<Record> authorizeBeforeList = Db
					.find("select authorizeID,authorizeBeUserID from authorizePhoto where authorizeGroupID="
							+ groupid + " and authorizeUserID=" + userid + " ");

			boolean flag = false;
			for (Record record : groupMemberList) {
				int be = Integer.parseInt(record.get("userid").toString());

				boolean judgeFlag = false; // falses表示没有授权过，则插入数据
											// true表示授权过，则更新数据
				int tempData = 0;
				for (Record temp : authorizeBeforeList) {
					int authorizeID = Integer.parseInt(temp.get("authorizeID")
							.toString());
					int authorizeBeUserID = Integer.parseInt(temp.get(
							"authorizeBeUserID").toString());
					if (be == authorizeBeUserID) {
						judgeFlag = true;
						tempData = authorizeID;
						break;
					}
				}
				if (judgeFlag) {
					ap = new AuthorizePhoto().findById(tempData);
					ap.set("authorizeStatus", 1);
					flag = ap.update();
					if (!flag) {
						jsonString = dataProcess.updateFlagResult(flag);
						break;
					}
				} else {
					ap = new AuthorizePhoto().set("authorizeGroupID", groupid)
							.set("authorizeUserID", userid)
							.set("authorizeBeUserID", be)
							.set("authorizeStatus", 1);
					flag = ap.save();
					if (!flag) {
						jsonString = dataProcess.insertFlagResult(flag);
						break;
					}
				}

			}
			if (flag) {
				jsonString = jsonData.getSuccessJson();
			}
			break;
		default:
			jsonString = jsonData.getJson(2, "请求参数错误");
			break;
		}
		renderText(jsonString);
	}

	/**
	 * 查看互看界面
	 */
	public void ShowPeepInterface() {
		int userid = this.getParaToInt("userid");
		int beUserid = this.getParaToInt("beUserid");
		int groupid = this.getParaToInt("groupid");

		String today = sdfOfDate.format(new Date());

		// 获取双方信息
		List<Record> userList = Db
				.find("select userid,unickname,upic from users where userid in ("
						+ userid + "," + beUserid + ")  ");
		// 获取双方互看信息
		List<Record> photoList = Db
				.find("select peepBeUserid,peepPhoto,peepPosition from peep where peepGroupid="
						+ groupid
						+ " and peepUserid in ("
						+ userid
						+ ","
						+ beUserid + ") and peepDate='" + today + "'  ");

		// 信息存储对象
		Record myInfo = new Record();
		Record yourInfo = new Record();

		// 添加个人信息
		for (Record record : userList) {
			int tempID = Integer.parseInt(record.get("userid").toString());
			if (userid == tempID) {
				myInfo.set("userInfo", record);
			}
			if (beUserid == tempID) {
				yourInfo.set("userInfo", record);
			}
		}

		// boolean flag = true; // true表示还未看该好友的照片

		// 添加偷窥信息
		if (photoList.size() == 0) {
			myInfo.set("photoInfo", null);
			yourInfo.set("photoInfo", null);
		} else {
			for (Record record : photoList) {
				int tempID = Integer.parseInt(record.get("peepBeUserid")
						.toString());
				// 图片授权
				String[] peepPhoto = record.get("peepPhoto").toString()
						.split(",");
				String[] position = record.get("peepPosition").toString()
						.split(",");
				List<Record> photoInfoArray = new ArrayList<Record>();
				for (int i = 0; i < peepPhoto.length; i++) {
					Record temp = new Record()
							.set("photo",
									operate.getDownloadToken(peepPhoto[i]
											+ CommonParam.pictureThumbnailPara))
							.set("year", position[i].split(";")[0])
							.set("position", position[i].split(";")[1]);
					photoInfoArray.add(temp);
				}

				if (userid == tempID) {
					myInfo.set("photoInfo", photoInfoArray);
				}
				if (beUserid == tempID) {
					yourInfo.set("photoInfo", photoInfoArray);
					// // 判断看了好友几张照片
					// int size = peepPhoto.length;
					// if (size < 3) {
					// yourInfo.set("unseenPhoto",
					// service.GetFriendPhoto(3 - size, beUserid));
					// }
					// flag = false;
				}
			}

		}

		// 添加对方本地相册信息
		yourInfo.set("unseenPhoto", service.GetFriendPhoto(3, beUserid));

		Record result = new Record().set("myInfo", myInfo).set("yourInfo",
				yourInfo);
		List<Record> list = new ArrayList<Record>();
		list.add(result);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * 偷窥用户图片
	 */
	public void PeepUserPhoto() {
		int userid = this.getParaToInt("userid");
		int beUserid = this.getParaToInt("beUserid");
		int groupid = this.getParaToInt("groupid");
		int year = this.getParaToInt("year");
		int position = this.getParaToInt("position");

		// 获取偷窥的图片
		List<Record> peepPhoto = Db
				.find("select address from userPhoto where year(shootTime)="
						+ year + " and userid=" + beUserid + " and status=0 ");
		int size = peepPhoto.size();
		int random = (int) (Math.random() * (size - 1));
		String address = peepPhoto.get(random).get("address").toString();
		String data = address;
		address = operate.getDownloadToken(address
				+ CommonParam.pictureThumbnailPara);
		List<Record> result = dataProcess.makeSingleParamToList("photo",
				address);

		// 存储图片位置
		String location = (year + ";" + position);

		String today = sdfOfDate.format(new Date());
		// 判断今天是否看过好友照片
		Peep peep = new Peep();
		List<Record> list = peep.GetPeepDataToday(userid, beUserid, groupid,
				today);
		if (list.size() == 0) {
			// 未看过
			peep = new Peep().set("peepGroupid", groupid)
					.set("peepUserid", userid).set("peepBeUserid", beUserid)
					.set("peepPhoto", data).set("peepDate", today)
					.set("peepPosition", location);
			if (peep.save()) {
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else {
			// 已有数据
			if (list.size() < 3) {
				// 能看数据
				String peepID = list.get(0).get("peepID").toString();
				peep = new Peep().findById(peepID);
				peep.set("peepPhoto",
						peep.get("peepPhoto").toString() + "," + data).set(
						"peepPosition",
						peep.get("peepPosition").toString() + "," + location);
				if (peep.update()) {
					jsonString = jsonData.getSuccessJson(result);
				} else {
					jsonString = dataProcess.updateFlagResult(false);
				}
			} else {
				// 不能再看数据
				jsonString = jsonData.getJson(1038, "当天不能再偷窥该好友");
			}
		}

		// 向好友发送推送

		renderText(jsonString);

	}

	/**
	 * 完成教程
	 */
	public void FinishWatchPhotoTutorial() {
		String userid = this.getPara("userid");
		User user = new User().findById(userid);
		user.set("isFinishWatchPhotoTutorial", 1);
		jsonString = dataProcess.updateFlagResult(user.update());
		renderText(jsonString);
	}

}
