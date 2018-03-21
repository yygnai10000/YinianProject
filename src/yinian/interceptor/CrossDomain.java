package yinian.interceptor;


import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;

public class CrossDomain implements Interceptor{

	@Override
	public void intercept(Invocation inv) {
		// TODO Auto-generated method stub
		inv.getController().getResponse().addHeader("Access-Control-Allow-Origin", "*");
		inv.getController().getResponse().addHeader("Access-Control-Allow-Methods", "POST,GET,PATCH,PUT,OPTIONS");
		inv.invoke();
	}


}
