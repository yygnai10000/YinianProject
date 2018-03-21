package yinian.thread;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import yinian.model.Group;
import yinian.model.GroupMember;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class TestThread extends Thread {

	private int[] target;

	public TestThread(int[] target) {
		this.target = target;
	}

	@Override
	public void run() {

		int[] album = new int[] { 833623 };
		int[] target = this.target;

		String threadName = Thread.currentThread().getName();
		for (int j = 0; j < target.length; j++) {
			System.out.println("��ʼ��ռ�" + target[j] + "����");

			Set<String> id = new HashSet<String>();
			List<Record> temp = Db
					.find("select gmuserid from groupmembers where gmgroupid="
							+ target[j] + "");
			for (Record record : temp) {
				String tempid = record.get("gmuserid").toString();
				id.add(tempid);
			}

			int count = 1;

			for (int i = 0; i < album.length; i++) {
				System.out.println("��ʼ�����ռ�" + album[i] + "����");
				List<Record> list = Db
						.find("select gmuserid from groupmembers where gmgroupid="
								+ album[i] + " ");
				for (Record record : list) {
					String userid = record.get("gmuserid").toString();
					if (!id.contains(userid)) {
						id.add(userid);
						GroupMember gm = new GroupMember();
						gm.set("gmgroupid", target[j]).set("gmuserid", userid);
						gm.save();
						count++;
						System.out.println(threadName + "  " + count);
					}

				}
				System.out.println("���������ռ�" + album[i] + "����");
			}

			Group group = new Group().findById(target[j]);
			group.set("gnum", count);
			group.update();

			System.out.println("������ռ�" + target[j] + "����");
		}

	}
	// TODO Auto-generated method stub

}
