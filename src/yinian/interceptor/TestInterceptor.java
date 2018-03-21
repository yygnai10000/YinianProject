package yinian.interceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.jfinal.log.Logger;

public class TestInterceptor implements Interceptor {

	private static final Logger log = Logger.getLogger(TestInterceptor.class);
	
	@Override
	public void intercept(Invocation inv) {
		// TODO Auto-generated method stub
		Controller controller = inv.getController();
		Map<String, String[]> map = controller.getParaMap();
		Set<String> set = map.keySet();
		StringBuilder sb = new StringBuilder();

		for (String key : set) {
			sb.append(key).append("=").append(Arrays.toString(map.get(key)))
					.append(",");
		}

		log.info(sb.toString());
		
		inv.invoke();

	}

}
