package yinian.model;

import com.jfinal.plugin.activerecord.Model;
import redis.clients.jedis.Jedis;
import yinian.utils.RedisUtils;

/**
 * ��Ϣ���Ķ���
 *
 * @author yu_chen
 * @date 2018/3/16 16:06
 */
public class MessageCenter extends Model<MessageCenter> {

    /**
     * ����ʵ������
     */
    private Jedis jedis = RedisUtils.getRedis();

    /**
     * ��̬ʵ������
     */
    public static final MessageCenter dao = new MessageCenter();

    /**
     * ��װkey�ķ���
     *
     * @param userId
     * @param msgType
     * @return
     */
    private String getKeyStr(String userId, String msgType) {
        return userId + "-" + msgType;
    }

    /**
     * ͨ���û�ID��ѯ��Ϣ��¼
     * ��Ϣ֪ͨ����  dz---����֪ͨ  pl---����֪ͨ jf---���ֶ�̬  tz--֪ͨ
     *
     * @param userId �û�ID
     * @return 0, 1
     */
    public int selectSingleTypeStatusByUserId(String userId, String msgType) {
        //�ӻ�����ȡ����Ϣ��״̬
        if (jedis == null) {
            return 0;
        }
        String msgStatusStr = jedis.get(getKeyStr(userId, msgType));
        //������Ѷ�״̬
        return msgStatusStr == null ? 0 : 1;
    }

    /**
     * ��ѯ�ܵ���Ϣ״̬ͨ���û�ID
     *
     * @param userId
     * @return
     */
    public int selectAllTypeStatusByUserId(String userId) {
        return this.selectSingleTypeStatusByUserId(userId, "dz")
                + this.selectSingleTypeStatusByUserId(userId, "pl")
                + this.selectSingleTypeStatusByUserId(userId, "jf")
                + this.selectSingleTypeStatusByUserId(userId, "tz");
    }

    /**
     * ������Ϣ
     * ����һ��״̬
     *
     * @param userId  �û�ID
     * @param msgType ��Ϣ����  dz---����֪ͨ  pl---����֪ͨ jf---���ֶ�̬  tz--֪ͨ
     */
    public void newMsg(String userId, String msgType) {
        //����Ѿ��������Key�Ͳ���Ҫ�ظ�����
        jedis.set(getKeyStr(userId, msgType), "1", "NX");
    }

    /**
     * ��ȡ��Ϣ
     * ����Ѷ��ͽ����key�Ƴ�
     *
     * @param userId
     * @param msgType
     */
    public void readMsg(String userId, String msgType) {
        jedis.move(getKeyStr(userId, msgType), 0);
    }


}
