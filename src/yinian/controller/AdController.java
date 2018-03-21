package yinian.controller;

import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.model.Ad;
import yinian.utils.JsonData;

import com.jfinal.core.ActionKey;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class AdController extends Controller {

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类

	/**
	 * 排重
	 */
	@ActionKey("/query")
	public void QueryRepetition() {
		String addpid = this.getPara("appid");
		String idfa = this.getPara("idfa");

		List<Record> list = Db.find("select * from ad where adIDfa='" + idfa
				+ "' and adAppID='" + addpid + "' ");
		Record result = new Record();
		if (list.size() == 0) {
			result.set(idfa, 0);
		} else {
			result.set(idfa, 1);
		}
		renderText(result.toJson());
	}

	/**
	 * 点击
	 */
	@ActionKey("/click")
	public void ClickAd() {
		String addpid = this.getPara("appid");
		String idfa = this.getPara("idfa");
		String source = this.getPara("source");
		String callback = this.getPara("callback");

		Ad ad = new Ad().set("adIDfa", idfa).set("adAppID", addpid)
				.set("adSource", source).set("adCallback", callback);
		Record result = new Record();
		if (ad.save()) {
			result.set("code", 0).set("message", "success");
		} else {
			result.set("code", 1).set("message", "false");
		}
		renderText(result.toJson());
	}

	/**
	 * 使用
	 */
	@ActionKey("/use")
	public void UseApp() {
		String idfa = this.getPara("idfa");
		List<Record> list = Db.find("select adCallback from ad where adIDfa='"
				+ idfa + "' ");
		if (list.size() == 0) {
			jsonString = jsonData.getJson(1044, "用户来源不是广告渠道");
		} else {
			String callback = list.get(0).get("adCallback").toString();
			YinianDataProcess data = new YinianDataProcess();
			data.sentNetworkRequest(callback);
			jsonString = jsonData.getSuccessJson();
		}
		renderText(jsonString);
	}

}
