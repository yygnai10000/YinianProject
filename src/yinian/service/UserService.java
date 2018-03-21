package yinian.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import yinian.model.History;
import yinian.model.RedEnvelop;
import yinian.model.Transaction;
import yinian.model.User;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class UserService {

	/**
	 * 获取用户交易记录
	 * 
	 * @param userid
	 * @param transactionID
	 * @param type
	 * @return
	 */
	public List<Record> getUserTransactionRecords(String userid, String transactionID, String type) {
		List<Record> list = null;
		Transaction tran = new Transaction();
		switch (type) {
		case "initialize":
			list = tran.initializeTransactionRecord(userid);
			break;
		case "loading":
			list = tran.loadingTransactionRecord(userid, transactionID);
			break;
		default:
			break;
		}
		return list;
	}
	//获取余额
	@Before(Tx.class)
	public double getUserBalance(String userid) {
		List<Record> list = Db.find("select uBalance from users where userid = " + userid);
		if(!list.isEmpty()) {
			return list.get(0).getDouble("uBalance");
		}
		return 0;
	}
	/**
	 * 用户消费
	 */
	@Before(Tx.class)
	public boolean ExpenseMoney(String userid, BigDecimal money, String type, String data) {
		// 判断用户余额是否充足
		User user = new User();
		if (user.JudgeUserBalanceIsEnough(userid, money)) {
			// 减少用户余额
			boolean reduceFlag = user.UpdateUserBalance(userid, money, "sub");
			// 插入交易记录
			boolean insertFlag = new Transaction().insertExpenseRecord(userid, money, type, data);
			return reduceFlag && insertFlag;
		}

		return false;

	}

	/**
	 * 用户收入
	 */
	@Before(Tx.class)
	public boolean incomeMoney(String userid, BigDecimal money, String type, String data) {
		boolean addFlag = new User().UpdateUserBalance(userid, money, "add");
		boolean insertFlag = new Transaction().insertIncomeRecord(userid, money, type, data);
		return addFlag && insertFlag;
	}

	/**
	 * 添加用户历史访问信息
	 */
	@Before(Tx.class)
	public boolean AddHistoryAccessInfo(String userid, String source, String version, String port,String openID) {
		// 修改用户最后登录信息
		User user = new User().findById(userid);
		user.set("uloginSource", source).set("uopenid", openID);
		boolean userFlag = user.update();
		// 插入登录历史
		History history = new History().set("historyUserID", userid).set("historySource", source)
				.set("historyVersion", version).set("historyPort", port);
		boolean historyFlag = history.save();
		// 返回结果
		return userFlag && historyFlag;
	}

}
