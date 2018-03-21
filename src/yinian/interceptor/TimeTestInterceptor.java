package yinian.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;

public class TimeTestInterceptor implements Interceptor {

	@Override
	public void intercept(Invocation inv) {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		inv.invoke();
		long end = System.currentTimeMillis();
		System.out.println("����ִ��ʱ�䣺" + (end - start) + "ms");

	}

}
