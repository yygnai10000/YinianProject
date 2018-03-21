package yinian.utils;

import java.math.BigDecimal;

public class GrabRedEnvelop {

	/**
	 * 获取随机红包
	 * 
	 * @param leftMoney
	 * @param leftPackage
	 * @return
	 */
	public static BigDecimal getRandomMoney(BigDecimal leftMoney,
			int leftPackage) {

		if (leftPackage == 0)
			return new BigDecimal("0.00");
		if (leftPackage == 1)
			return leftMoney;

		// 转成分避免小数
		int totalMoney = leftMoney.multiply(BigDecimal.valueOf(100)).intValue();

		int min = 1;
		int max = totalMoney / leftPackage * 2;

		int money = (int) (Math.random() * max);
		money = money <= min ? 1 : (int) Math.floor(money);
		return new BigDecimal(String.valueOf(money))
				.divide(new BigDecimal(100));
	}

}
