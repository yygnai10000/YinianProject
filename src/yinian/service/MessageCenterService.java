package yinian.service;

import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import org.apache.commons.lang3.StringUtils;
import yinian.model.MessageCenter;

import java.util.ArrayList;
import java.util.List;

/**
 * ��Ϣ���ķ���
 *
 * @author yu_chen
 * @date 2018/3/16 15:57
 */
public class MessageCenterService {

    private static final Logger log = Logger.getLogger(MessageCenterService.class);
    /**
     * ��ҳ��С
     */
    private static final int PAGE_SIZE = 20;

    public static final String MSG_TYPE_DZ = "dz";
    public static final String MSG_TYPE_PL = "pl";
    public static final String MSG_TYPE_JF = "jf";
    public static final String MSG_TYPE_TZ = "tz";

    /**
     * ������Ϣ״̬  ���i>0˵�����µ���Ϣ
     *
     * @param userId �û�ID
     * @return record��¼
     */
    public List<Record> showMyMsgStatus(String userId) {
        List<Record> records = new ArrayList<>();
        Record record = new Record();
        record.set(MSG_TYPE_DZ, MessageCenter.dao.selectSingleTypeStatusByUserId(userId, MSG_TYPE_DZ));
        record.set(MSG_TYPE_PL, MessageCenter.dao.selectSingleTypeStatusByUserId(userId, MSG_TYPE_PL));
        record.set(MSG_TYPE_JF, MessageCenter.dao.selectSingleTypeStatusByUserId(userId, MSG_TYPE_JF));
        record.set(MSG_TYPE_TZ, MessageCenter.dao.selectSingleTypeStatusByUserId(userId, MSG_TYPE_TZ));
        records.add(record);
        return records;
    }

    /**
     * ����Ϣ���Ϊ�Ѷ�
     *
     * @param userId  �û�ID
     * @param msgType ��Ϣ����
     */
    public void readMyMsg(String userId, String msgType) {
        MessageCenter.dao.readMsg(userId, msgType);
    }


    /**
     * ��ѯ���û��������ڵ���������
     *
     * @param userId �û�ID
     * @return ��¼�б�
     */
    public List<Record> selectAllComments(String userId, String commentId) {
        List<Record> records;
        boolean notEmpty = StringUtils.isNotEmpty(commentId);
        StringBuffer sb = new StringBuffer();
        sb.append("select c.cid,");
        sb.append("c.ceid as eventId,c.ceduserid as leftUserId,u2.unickname as leftUserName,u2.upic as leftUserPic,");
        sb.append("c.cuserid as rightUserId,u.unickname as rightUserName,u2.upic as rightUserPic,");
        sb.append("c.ccontent as commentContent,c.ctime as commentTime,c.cplace,c.cstatus from comments c ");
        sb.append("left join users u on c.cuserid=u.userid ");
        sb.append("left join users u2 on c.ceduserid=u2.userid ");
        sb.append("where u.userid=? and c.ctime>DATE_SUB(CURDATE(), INTERVAL 6 MONTH) ");
        if (notEmpty) {
            sb.append("and c.ctime<(select ctime from comments where cid=?) ");
        }
        sb.append("order by c.ctime DESC ");
        sb.append("LIMIT ");
        sb.append(PAGE_SIZE);
        log.error(sb.toString());
        if (notEmpty) {
            records = Db.find(sb.toString(), userId, commentId);
        } else {
            records = Db.find(sb.toString(), userId);
        }
        //�����ұߵ�ͼƬ
        setRightUserPic(records);
        return records;

    }

    /**
     * ��ѯ���еĵ�����Ϣ
     *
     * @param userId �û�ID
     * @param likeId ���һ���ĵ���ID
     * @return list����
     */
    public List<Record> selectAllLikes(String userId, String likeId) {
        boolean notEmpty = StringUtils.isNotEmpty(likeId);
        List<Record> records;
        StringBuffer sb = new StringBuffer();
        sb.append("select l.likeID as likeId,l.likeUserID as leftUserId,u.unickname as leftUserName,");
        sb.append("u.upic as leftUserPic,e.euserid as rightUserId,u2.unickname as rightUserName,");
        sb.append("u2.upic as rightUserPic,l.likeStatus,l.likeTime,l.likeEventID ");
        sb.append("from `like` l ");
        sb.append("LEFT JOIN `events` e on l.likeEventID=e.eid ");
        sb.append("LEFT JOIN users u on l.likeUserID=u.userid ");
        sb.append("LEFT JOIN users u2 on e.euserid=u2.userid ");
        sb.append("where e.euserid=? and l.likeTime>DATE_SUB(CURDATE(), INTERVAL 6 MONTH) ");
        if (notEmpty) {
            sb.append("and l.likeTime<(select lk.likeTime from `like` lk where lk.likeID=?) ");
        }
        sb.append("ORDER BY l.likeTime DESC ");
        sb.append("limit ");
        sb.append(PAGE_SIZE);
        log.error(sb.toString());
        if (notEmpty) {
            records = Db.find(sb.toString(), userId, likeId);
        } else {
            records = Db.find(sb.toString(), userId);
        }
        //�����ұߵ�ͼƬ
        setRightUserPic(records);
        return records;
    }

    /**
     * ���ö�̬��ͼƬ��Ϣ
     *
     * @param records
     */
    public void setRightUserPic(List<Record> records) {
        for (Record record : records) {
            String queryEventPicSql = " select * from pictures  where peid=?";
            List<Record> recordsList = Db.find(queryEventPicSql, record.get("likeEventID"));
            if (recordsList != null && recordsList.size() > 0) {
                record.set("rightUserPic", recordsList.get(0).get("poriginal"));
            } else {
                record.set("rightUserPic", "");
            }
        }
    }

}
