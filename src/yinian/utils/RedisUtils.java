package yinian.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtils {
	
	// Redis������IP
	private static String IP = "10.117.12.247";
	//private static String IP = "121.41.26.122";
	
	// Redis�Ķ˿ں�
	private static int PORT = 6379;
	
	// ��������
	private static String AUTH = "";
	
	// ��������ʵ���������Ŀ
	// �����ֵΪ-1�����ʾ�����ƣ����pool�Ѿ�������maxActive��jedisʵ�������ʱpool��״̬Ϊexhausted(�ľ�)
	private static int MAX_ACTIVE = 1024;
	
	// ����һ��pool����ж��ٸ�״̬Ϊidle(���е�)��jedisʵ��
	private static int MAX_IDLE = 200;
	
	// �ȴ��������ӵ����ʱ�䣬��λ���룬Ĭ��ֵΪ-1����ʾ������ʱ����������ȴ�ʱ�䣬��ֱ���׳�JedisConnectionException��
	private static int MAX_WAIT = 10000;
	
	// ��ʱʱ��
	private static int TIMEOUT = 10000;
	
	//��borrowһ��jedisʵ��ʱ���Ƿ���ǰ����validate���������Ϊtrue����õ���jedisʵ�����ǿ��õ�
	private static boolean BORROW = true;
	
	private static JedisPool jedisPool = null;
	
	/**
	 * ��ʼ��redis���ӳ�
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
	 * ��ȡredisʵ��
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
	 * �ͷ�jedis��Դ
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
