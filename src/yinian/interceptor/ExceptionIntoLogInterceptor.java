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
 * �쳣������־ ȫ��������
 *
 */
public class ExceptionIntoLogInterceptor implements Interceptor {

	private static final Logger log = Logger
			.getLogger(ExceptionIntoLogInterceptor.class);

	@Override
	public void intercept(Invocation invocation) {
		try {
			invocation.invoke(); // һ��Ҫע�⣬�Ѵ������invoke֮����Ϊ����֮ǰ�Ļ����ǻ��ָ��
		} catch (Exception e) {
			// log ����
			logWrite(invocation, e);
		}
	}

	/**
	 * �����־
	 * 
	 * @param inv
	 * @param e
	 */
	private void logWrite(Invocation inv, Exception e) {

		// ����ģʽ�´�ӡ��ջ��Ϣ
		// if (JFinal.me().getConstants().getDevMode()) {
		// e.printStackTrace();
		// }

		// ��ȡ��������
		Controller controller = inv.getController();
		String controllerName = controller.getClass().getName();
		String methodName = inv.getMethodName();
		String exceptionType = e.getClass().getName();

		// ��ȡ���������ֵ
		String parameter = "";
		Enumeration<String> param = controller.getParaNames();
		while (param.hasMoreElements()) {
			String paraName = param.nextElement();
			String paraValue = controller.getPara(paraName);
			parameter += (paraName + "=" + paraValue + "&");
		}
		if (!parameter.equals(""))
			parameter = parameter.substring(0, parameter.length() - 1);

		// ��ӡ��־
		StringBuilder sb = new StringBuilder("\n---Exception Log Begin---\n");
		sb.append("Controller:").append(controllerName).append("\n");
		sb.append("Method:").append(methodName).append("\n");
		sb.append("params:").append(parameter).append("\n");
		sb.append("Exception Type:").append(exceptionType).append("\n");
		sb.append("Exception Details:");
		log.error(sb.toString(), e);

		// ����־д�����ݿ�
		try {
			YiNianLog yn = new YiNianLog().set("controller", controllerName)
					.set("method", methodName).set("param", parameter)
					.set("exception", exceptionType)
					.set("trace", getErrorInfoFromException(e));
			//��д����־ yn.save();
		} catch (Exception ee) {
			// ��������
		}
	}

	/**
	 * ��ȡ��ջ��Ϣ
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