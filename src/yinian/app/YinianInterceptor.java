package yinian.app;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;

public class YinianInterceptor implements Interceptor{

	@Override
	public void intercept(Invocation inv) {
		// TODO Auto-generated method stub
		System.out.println("before");
		
//		inv.invoke();
		
		String actionKey = inv.getActionKey();
		System.out.println(actionKey);
		
		String controllerKey = inv.getControllerKey();
		System.out.println(controllerKey);
		
		String viewPath = inv.getViewPath();
		System.out.println(viewPath);
		
		Object[] ob = inv.getArgs();
		System.out.println(ob.length);
		
		String methodName = inv.getMethodName();
		System.out.println(methodName);
		
		Controller con = inv.getController();
		con.renderText("hahaha");
		
		System.out.println("after");
	}

}
