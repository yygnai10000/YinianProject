package yinian.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.interceptor.CrossDomain;
import yinian.model.Charm;
import yinian.model.FateHistory;
import yinian.model.FateOpen;
import yinian.model.GiveCharm;
import yinian.service.DivineService;
import yinian.utils.JsonData;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

@Before(CrossDomain.class)
public class DivineController extends Controller {

	private DivineService service = new DivineService();
	private YinianDataProcess dataProcess = new YinianDataProcess();
	// 返回的json字符串
	private String jsonString;
	// json操作类
	private JsonData jsonData = new JsonData();

	/**
	 * 获取今日运势
	 */
	public void GetTodayFate() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select * from fate where fateStatus=0  ");
		int size = list.size();
		int temp = (int) (Math.random() * size);
		Record record = list.get(temp);
		String fateID = record.get("fateID").toString();
		FateHistory histoty = new FateHistory()
				.set("fateHistoryFateID", fateID).set("fateHistoryUserID",
						userid);
		if (histoty.save()) {
			list = new ArrayList<Record>();
			list.add(record);
			list = service.getDivineNumber(list);
			jsonString = jsonData.getSuccessJson(list);
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		renderText(jsonString);

	}

	/**
	 * 显示历史运势列表
	 */
	public void ShowFateHistoryList() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select fateID,fateTotal,fateLove,fateCareer,fateMoney,fateHistoryTime from fate,fatehistory where fateHistoryFateID=fateID and fateHistoryUserID="
						+ userid + " and fateHistoryStatus=0 ");
		list = service.getDivineNumber(list);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 查看空间内他人抽签运势
	 */
	public void ShowOtherFateInSpace() {
		String groupid = this.getPara("groupid");
		List<Record> list = Db
				.find("select userid,unickname,upic,fateID,fateTotal,fateLove,fateCareer,fateMoney,fateOpenTime from fate,fateopen,users where userid=fateOpenUserID and fateOpenFateID=fateID and fateOpenGroupID="
						+ groupid + " and fateOpenStatus=0 ");
		list = service.getDivineNumber(list);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 分享运势到空间
	 */
	@Before(Tx.class)
	public void ShareFateToSpace() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String fateID = this.getPara("fateID");
		String[] array = groupid.split(",");
		boolean flag = true;
		for (int i = 0; i < array.length; i++) {
			FateOpen open = new FateOpen().set("fateOpenUserID", userid)
					.set("fateOpenGroupID", array[i])
					.set("fateOpenFateID", fateID);
			if (!open.save()) {
				flag = false;
				break;
			}
		}
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 获取空间列表
	 */
	public void ShowSpaceList() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gmuserid="
						+ userid
						+ " and gtype!=5 and gmstatus=0 and gstatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 查看灵符列表
	 */
	public void ShowCharmList() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select * from charm where charmUserID="
				+ userid + "");
		if (list.size() == 0) {
			Charm charm = new Charm().set("charmUserID", userid);
			if (charm.save()) {
				list = Db.find("select * from charm where charmUserID="
						+ userid + "");
				jsonString = jsonData.getSuccessJson(list);
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		} else {
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 查看空间内赠送列表
	 */
	public void ShowSpaceGiveCharmList() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		List<Record> list = Db
				.find("select userid,unickname,upic,gcCharmNo,gcTime from givecharm,users where gcSender=userid and gcReceiver="
						+ userid
						+ " and gcGroupID="
						+ groupid
						+ " and gcStatuts=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 赠送灵符
	 */
	@Before(Tx.class)
	public void GiveCharms() {
		String sender = this.getPara("sender");
		String receiver = this.getPara("receiver");
		String groupid = this.getPara("groupid");
		String charmNo = this.getPara("charmNo");
		// 判断赠送者当日是否已经赠送两次
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String today = sdf.format(new Date());
		List<Record> judge = Db.find("select * from givecharm where gcSender="
				+ sender + " and gcTime like '" + today
				+ "%' and gcStatuts=0  ");
		if (judge.size() == 2) {
			jsonString = jsonData.getJson(1030, "当日已赠送两次灵符");
		} else {
			GiveCharm gc = new GiveCharm().set("gcGroupID", groupid)
					.set("gcSender", sender).set("gcReceiver", receiver)
					.set("gcCharmNo", charmNo);
			boolean flag = (gc.save() && service.GetCharm(sender, charmNo) && service
					.GetCharm(receiver, charmNo));
			jsonString = dataProcess.insertFlagResult(flag);
		}
		renderText(jsonString);
	}

	/**
	 * 随机开启灵符
	 */
	public void RandomOpenCharm() {
		String userid = this.getPara("userid");
		String charmNo = this.getPara("charmNo");
		if (charmNo.equals("7") || charmNo.equals("8") || charmNo.equals("9")
				|| charmNo.equals("10")) {
			jsonString = dataProcess.insertFlagResult(service.GetCharm(userid,
					charmNo));
		} else {
			jsonString = jsonData.getJson(1031, "改符不能随机获取");
		}
		renderText(jsonString);
	}

	/**
	 * 获取空间成员列表
	 */
	public void GetSpaceMembersList() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		List<Record> list = Db
				.find("select userid,unickname,upic from users,groupmembers where gmuserid=userid and gmgroupid="
						+ groupid
						+ " and gmuserid!="
						+ userid
						+ " and gmstatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

}
