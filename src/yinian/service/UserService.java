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
	 * ��ȡ�û����׼�¼
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
	//��ȡ���
	@Before(Tx.class)
	public double getUserBalance(String userid) {
		List<Record> list = Db.find("select uBalance from users where userid = " + userid);
		if(!list.isEmpty()) {
			return list.get(0).getDouble("uBalance");
		}
		return 0;
	}
	/**
	 * �û�����
	 */
	@Before(Tx.class)
	public boolean ExpenseMoney(String userid, BigDecimal money, String type, String data) {
		// �ж��û�����Ƿ����
		User user = new User();
		if (user.JudgeUserBalanceIsEnough(userid, money)) {
			// �����û����
			boolean reduceFlag = user.UpdateUserBalance(userid, money, "sub");
			// ���뽻�׼�¼
			boolean insertFlag = new Transaction().insertExpenseRecord(userid, money, type, data);
			return reduceFlag && insertFlag;
		}

		return false;

	}

	/**
	 * �û�����
	 */
	@Before(Tx.class)
	public boolean incomeMoney(String userid, BigDecimal money, String type, String data) {
		boolean addFlag = new User().UpdateUserBalance(userid, money, "add");
		boolean insertFlag = new Transaction().insertIncomeRecord(userid, money, type, data);
		return addFlag && insertFlag;
	}

	/**
	 * ����û���ʷ������Ϣ
	 */
	@Before(Tx.class)
	public boolean AddHistoryAccessInfo(String userid, String source, String version, String port,String openID) {
		// �޸��û�����¼��Ϣ
		User user = new User().findById(userid);
		user.set("uloginSource", source).set("uopenid", openID);
		boolean userFlag = user.update();
		// �����¼��ʷ
		History history = new History().set("historyUserID", userid).set("historySource", source)
				.set("historyVersion", version).set("historyPort", port);
		boolean historyFlag = history.save();
		// ���ؽ��
		return userFlag && historyFlag;
	}

}
