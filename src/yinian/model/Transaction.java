package yinian.model;

import java.math.BigDecimal;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Transaction extends Model<Transaction> {
	public static final Transaction dao = new Transaction();

	/**
	 * 初始化交易记录
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> initializeTransactionRecord(String userid) {
		List<Record> list = Db
				.find("select transactionID,transactionMoney,transactionType,transactionTime from transaction where transactionUserID="
						+ userid
						+ " and transactionStatus=0 and transactionStatus='交易成功' order by transactionID desc limit 10 ");
		return list;
	}

	/**
	 * 加载交易记录
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> loadingTransactionRecord(String userid,
			String transactionID) {
		List<Record> list = Db
				.find("select transactionID,transactionMoney,transactionType,transactionTime from transaction where transactionUserID="
						+ userid
						+ " and transactionID<"
						+ transactionID
						+ " and transactionStatus=0 and transactionStatus='交易成功' order by transactionID desc limit 10 ");
		return list;
	}

	/**
	 * 插入收入交易记录
	 */
	public boolean insertIncomeRecord(String userid, BigDecimal money,
			String type, String data) {
		Transaction tran = new Transaction().set("transactionUserID", userid)
				.set("transactionMoney", money).set("transactionData", data);
		switch (type) {
		case "充值":
			tran.set("transactionType", type);
			break;
		case "收红包":
			tran.set("transactionType", type);
			break;
		case "退款":
			tran.set("transactionType", type);
			break;
		default:
			return false;
		}
		return tran.save();
	}

	/**
	 * 插入支出交易记录
	 */
	public boolean insertExpenseRecord(String userid, BigDecimal money,
			String type, String data) {
		// 钱转为负数
		money = money.multiply(new BigDecimal("-1"));
		Transaction tran = new Transaction().set("transactionUserID", userid)
				.set("transactionMoney", money).set("transactionData", data);
		switch (type) {
		case "发红包":
			tran.set("transactionType", type);
			break;
		case "提现":
			tran.set("transactionType", type);
			break;
		case "系统奖励":
			tran.set("transactionType", type);
			break;
		default:
			return false;
		}

		return tran.save();
	}

	/**
	 * 根据订单号更新订单状态
	 */
	public boolean UpdateTransactionStatusByOrderNumber(String userid,
			String orderNumber) {
		int count = Db
				.update(" update transaction set transactionStatus='交易成功' where transactionData='"
						+ orderNumber + "' ");
		return count == 1;

	}

	/**
	 * 根据订单号查询交易记录ID
	 */
	public int GetTransactionIDByOrderNumber(String orderNumber) {
		List<Record> list = Db
				.find("select transactionID from transaction where transactionData='"
						+ orderNumber + "' ");
		if (list.size() == 0) {
			return 0;
		} else {
			return Integer
					.parseInt(list.get(0).get("transactionID").toString());
		}
	}
	/**
	  * 插入交易失败记录
	  */
	 public boolean insertFaildRecord(String userid, BigDecimal money,
	   String type, String data) {
	  // 钱转为负数
	  money = money.multiply(new BigDecimal("-1"));
	  Transaction tran = new Transaction().set("transactionUserID", userid)
	    .set("transactionMoney", money).set("transactionData", data).set("transactionStatus", "交易失败");
	  switch (type) {
	  case "发红包":
	   tran.set("transactionType", type);
	   break;
	  case "提现":
	   tran.set("transactionType", type);
	   break;
	  case "系统奖励":
	   tran.set("transactionType", type);
	   break;
	  default:
	   return false;
	  }

	  return tran.save();
	 }
}
