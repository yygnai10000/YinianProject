package yinian.interceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;

import yinian.model.YiNianLog;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.jfinal.log.Logger;

/**
 * 异常处理日志 全局拦截器
 *
 */
public class ExceptionIntoLogInterceptor implements Interceptor {

	private static final Logger log = Logger
			.getLogger(ExceptionIntoLogInterceptor.class);

	@Override
	public void intercept(Invocation invocation) {
		try {
			invocation.invoke(); // 一定要注意，把处理放在invoke之后，因为放在之前的话，是会空指针
		} catch (Exception e) {
			// log 处理
			logWrite(invocation, e);
		}
	}

	/**
	 * 输出日志
	 * 
	 * @param inv
	 * @param e
	 */
	private void logWrite(Invocation inv, Exception e) {

		// 开发模式下打印堆栈信息
		// if (JFinal.me().getConstants().getDevMode()) {
		// e.printStackTrace();
		// }

		// 获取基本数据
		Controller controller = inv.getController();
		String controllerName = controller.getClass().getName();
		String methodName = inv.getMethodName();
		String exceptionType = e.getClass().getName();

		// 获取参数与参数值
		String parameter = "";
		Enumeration<String> param = controller.getParaNames();
		while (param.hasMoreElements()) {
			String paraName = param.nextElement();
			String paraValue = controller.getPara(paraName);
			parameter += (paraName + "=" + paraValue + "&");
		}
		if (!parameter.equals(""))
			parameter = parameter.substring(0, parameter.length() - 1);

		// 打印日志
		StringBuilder sb = new StringBuilder("\n---Exception Log Begin---\n");
		sb.append("Controller:").append(controllerName).append("\n");
		sb.append("Method:").append(methodName).append("\n");
		sb.append("params:").append(parameter).append("\n");
		sb.append("Exception Type:").append(exceptionType).append("\n");
		sb.append("Exception Details:");
		log.error(sb.toString(), e);

		// 将日志写入数据库
		try {
			YiNianLog yn = new YiNianLog().set("controller", controllerName)
					.set("method", methodName).set("param", parameter)
					.set("exception", exceptionType)
					.set("trace", getErrorInfoFromException(e));
			//不写入日志 yn.save();
		} catch (Exception ee) {
			// 不做处理
		}
	}

	/**
	 * 获取堆栈信息
	 * 
	 * @param e
	 * @return
	 */
	public String getErrorInfoFromException(Exception e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return "\r\n" + sw.toString() + "\r\n";
		} catch (Exception e2) {
			return "ErrorInfoFromException";
		}
	}
}