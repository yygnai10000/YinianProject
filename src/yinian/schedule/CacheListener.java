package yinian.schedule;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import yinian.app.CacheData;

public class CacheListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("»º´æ¼àÌýÆ÷¹Ø±Õ");

	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("»º´æ¼àÌýÆ÷¿ªÆô");

	}

}
