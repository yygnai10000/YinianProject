package yinian.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtils {
	
	// Redis服务器IP
	private static String IP = "10.117.12.247";
	//private static String IP = "121.41.26.122";
	
	// Redis的端口号
	private static int PORT = 6379;
	
	// 访问密码
	private static String AUTH = "";
	
	// 可用连接实例的最大数目
	// 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)
	private static int MAX_ACTIVE = 1024;
	
	// 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例
	private static int MAX_IDLE = 200;
	
	// 等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
	private static int MAX_WAIT = 10000;
	
	// 超时时间
	private static int TIMEOUT = 10000;
	
	//在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的
	private static boolean BORROW = true;
	
	private static JedisPool jedisPool = null;
	
	/**
	 * 初始化redis连接池
	 */
	static {
		try {
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(MAX_ACTIVE);
			config.setMaxIdle(MAX_IDLE);
			config.setMaxWaitMillis(MAX_WAIT);
			config.setTestOnBorrow(BORROW);
			jedisPool = new JedisPool(config, IP, PORT, TIMEOUT);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取redis实例
	 */
	public synchronized static Jedis getRedis() {
		try {
			if(jedisPool != null) {
				Jedis resource = jedisPool.getResource();
				return resource;
			}else {
				return null;
			}
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 释放jedis资源
	 * @param jedis
	 */
	public static void returnResource(final Jedis jedis) {
		if(jedis != null) {
			jedis.close();
		}
	}
	
	public static void main(String[] args) {
		Jedis jedis = getRedis();
		System.out.println(jedis.get("mykey"));
	}
}
