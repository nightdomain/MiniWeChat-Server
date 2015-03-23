package server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import model.HibernateSessionFactory;
import model.User;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.google.protobuf.InvalidProtocolBufferException;

import protocol.LoginMsg;
import protocol.PersonalSettingsMsg;
import protocol.ProtoHead;
import protocol.RegisterMsg;
import tools.Debug;

/**
 * ���������µ��ӷ��������������û�����¼�
 * 
 * @author Feng
 * 
 */
public class Server_User {
	public static Server_User instance = new Server_User();

	private Server_User() {

	}

	/**
	 * �� �û��������ظ� �Ĵ��� ��onlineֵ��ΪTrue
	 * 
	 * @param networkMessage
	 * @author Feng
	 */
	public void keepAlive(NetworkMessage networkMessage) {
		// System.out.println((networkMessage == null) + "      " +
		// (networkMessage.ioSession == null));
		Debug.log("Server_User", "��  �û�" + networkMessage.ioSession.getRemoteAddress() + "  �ظ���������  �Ĵ���");
		// System.out.println(ServerModel.instance.clientUserTable.keySet().size());
		// System.out.println("fuck   " +
		// ServerModel.instance.clientUserTable.containsKey(networkMessage.ioSession.getRemoteAddress().toString()));
		// ���ClientUser�Ѿ����߱�ɾ������ô�Ͳ�����
		try {
			if (ServerModel.instance.getClientUserFromTable(networkMessage.ioSession.getRemoteAddress().toString()) == null) {
				Debug.log("Server_User", "�û���(ClientUserTalbe)���Ҳ��� �û�" + networkMessage.ioSession.getRemoteAddress()
						+ "�������ظ���������!");
				return;
			}

			ServerModel.instance.getClientUserFromTable(networkMessage.ioSession.getRemoteAddress().toString()).onLine = true;
		} catch (NullPointerException e) {
			System.out.println("Server_User: �쳣���û�" + networkMessage.ioSession.getRemoteAddress() + "�ѵ��ߣ������ظ���������!");
			e.printStackTrace();
		}
	}

	/**
	 * �������û�ע���¼�
	 * 
	 * @param networkMessage
	 * @author Feng
	 */
	public void register(NetworkMessage networkMessage) {
		Debug.log("Server_User", "ע���¼��� ��  �û�" + networkMessage.ioSession.getRemoteAddress() + "  ��ע���¼�  �Ĵ���");

		try {
			RegisterMsg.RegisterReq registerObject = RegisterMsg.RegisterReq.parseFrom(networkMessage.getMessageObjectBytes());
			RegisterMsg.RegisterRsp.Builder responseBuilder = RegisterMsg.RegisterRsp.newBuilder();

			// �����Ƿ����ͬ���û�
			Session session = HibernateSessionFactory.getSession();
			Criteria criteria = session.createCriteria(User.class);
			criteria.add(Restrictions.eq("userId", registerObject.getUserId()));
			if (criteria.list().size() > 0) { // �Ѵ���
				// �Ѵ�����ͬ�˺��û������߿ͻ���
				Debug.log("Server_User", "ע���¼����û�" + networkMessage.ioSession.getRemoteAddress() + "  ��ע���˺��ظ������ش���!");

				responseBuilder.setResultCode(RegisterMsg.RegisterRsp.ResultCode.USER_EXIST);
			} else { // û���⣬���Կ�ʼע��
				User user = new User();
				user.setUserId(registerObject.getUserId());
				user.setUserName(registerObject.getUserName());
				user.setUserPassword(registerObject.getUserPassword());

				session = HibernateSessionFactory.getSession();
				session.save(user);
				HibernateSessionFactory.commitSession(session);

				// �ɹ������ûذ���
				Debug.log("Server_User", "ע���¼����û�" + networkMessage.ioSession.getRemoteAddress() + "  ע��ɹ���������Ϣ!");
				responseBuilder.setResultCode(RegisterMsg.RegisterRsp.ResultCode.SUCCESS);
			}

			// �ظ��ͻ���
			ServerNetwork.instance.sendMessageToClient(networkMessage.ioSession, NetworkMessage.packMessage(
					ProtoHead.ENetworkMessage.REGISTER_RSP.getNumber(), networkMessage.getMessageID(), responseBuilder.build()
							.toByteArray()));
		} catch (InvalidProtocolBufferException e) {
			System.err.println("Server_User : ע���¼��� ��Protobuf�����л� " + networkMessage.ioSession.getRemoteAddress() + " �İ�ʱ�쳣��");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Server_User : ע���¼��� " + networkMessage.ioSession.getRemoteAddress() + " ���ذ�ʱ�쳣��");
			e.printStackTrace();
		}
	}

	/**
	 * ����Client�ġ���½����
	 * 
	 * @param networkMessage
	 * @author Feng
	 */
	public void login(NetworkMessage networkMessage) {
		Debug.log(new String[] { "Server_User", "login" }, " ��  �û�" + networkMessage.ioSession.getRemoteAddress()
				+ "  �ĵ�½�¼�  �Ĵ���");

		try {
			LoginMsg.LoginReq loginObject = LoginMsg.LoginReq.parseFrom(networkMessage.getMessageObjectBytes());
			LoginMsg.LoginRsp.Builder loginBuilder = LoginMsg.LoginRsp.newBuilder();

			// �����Ƿ����ͬ���û�
			Session session = HibernateSessionFactory.getSession();
			Criteria criteria = session.createCriteria(User.class);
			criteria.add(Restrictions.eq("userId", loginObject.getUserId()));
			if (criteria.list().size() > 0) { // �Ѵ���
				// �û����ڣ���ʼУ��
				User user = (User) criteria.list().get(0);
				if (user.getUserPassword().equals(loginObject.getUserPassword())) { // ������ȷ
					Debug.log(new String[] { "Server_User", "login" }, "�û�" + networkMessage.ioSession.getRemoteAddress()
							+ "  �ĵ�½У��ɹ�!");
					loginBuilder.setResultCode(LoginMsg.LoginRsp.ResultCode.SUCCESS);
				} else { // �������
					Debug.log(new String[] { "Server_User", "login" }, "�û�" + networkMessage.ioSession.getRemoteAddress()
							+ "  �ĵ�½�������!");
					loginBuilder.setResultCode(LoginMsg.LoginRsp.ResultCode.FAIL);
				}
			} else { // �û�������
				Debug.log(new String[] { "Server_User", "login" }, "�û�" + networkMessage.ioSession.getRemoteAddress()
						+ "  ���û�������!");
				loginBuilder.setResultCode(LoginMsg.LoginRsp.ResultCode.FAIL);
			}
			session.close();

			// �ظ��ͻ���
			ServerNetwork.instance.sendMessageToClient(networkMessage.ioSession, NetworkMessage.packMessage(
					ProtoHead.ENetworkMessage.LOGIN_RSP.getNumber(), networkMessage.getMessageID(), loginBuilder.build()
							.toByteArray()));
		} catch (InvalidProtocolBufferException e) {
			System.err.println("Server_User : ע���¼��� ��Protobuf�����л� " + networkMessage.ioSession.getRemoteAddress() + " �İ�ʱ�쳣��");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Server_User : ע���¼��� " + networkMessage.ioSession.getRemoteAddress() + " ���ذ�ʱ�쳣��");
			e.printStackTrace();
		}
	}

	/**
	 * ����������������
	 * 
	 * @param networkMessage
	 * @author wangfei
	 * @time 2015-03-21
	 */
	public void personalSettings(NetworkMessage networkMessage) {
		Debug.log(new String[] { "Server_User", "personalSettings" }, " ��  �û�" + networkMessage.ioSession.getRemoteAddress()
				+ "  �ĸ��������¼�  �Ĵ���");
		try {
			PersonalSettingsMsg.PersonalSettingsReq personalSettingsObject = PersonalSettingsMsg.PersonalSettingsReq
					.parseFrom(networkMessage.getMessageObjectBytes());
			PersonalSettingsMsg.PersonalSettingsRsp.Builder personalSettingsBuilder = PersonalSettingsMsg.PersonalSettingsRsp
					.newBuilder();

			Session session = HibernateSessionFactory.getSession();
			Criteria criteria = session.createCriteria(User.class);
			criteria.add(Restrictions.eq("userId", personalSettingsObject.getUserId()));
			if (criteria.list().size() > 0) {
				User user = (User) criteria.list().get(0);
				// �޸��ǳ�
				if (personalSettingsObject.getUserName() != null && personalSettingsObject.getUserName() != "") {
					user.setUserName(personalSettingsObject.getUserName());
				}
				// �޸�����
				if (personalSettingsObject.getUserPassword() != null && personalSettingsObject.getUserPassword() != "") {
					user.setUserPassword(personalSettingsObject.getUserPassword());
				}
				// ���ݿ��޸��ǳƻ�����
				try {
					Transaction trans = session.beginTransaction();
					session.update(user);
					trans.commit();
					personalSettingsBuilder.setResultCode(PersonalSettingsMsg.PersonalSettingsRsp.ResultCode.SUCCESS);
				} catch (Exception e) {
					personalSettingsBuilder.setResultCode(PersonalSettingsMsg.PersonalSettingsRsp.ResultCode.FAIL);
					e.printStackTrace();
				}

				// �޸�ͷ��
				if (personalSettingsObject.getHeadIndex() >= 1 && personalSettingsObject.getHeadIndex() <= 6) {
					BufferedImage image = null;
					try {
						// ��Ĭ��ͷ���ļ��л�ȡͼƬ
						image = ImageIO.read(new File(ResourcePath.headDefaultPath + personalSettingsObject.getHeadIndex()
								+ ".png"));
						File file = new File(ResourcePath.headPath);
						// ��鱣��ͷ����ļ����Ƿ����
						if (!file.exists() && !file.isDirectory()) {
							// ��������� �򴴽��ļ���
							file.mkdir();
						}
						// �����ȡ��Ĭ��ͷ��ͷ���ļ���
						File saveFile = new File(ResourcePath.headPath + personalSettingsObject.getUserId() + ".png");
						ImageIO.write(image, "png", saveFile);
						personalSettingsBuilder.setResultCode(PersonalSettingsMsg.PersonalSettingsRsp.ResultCode.SUCCESS);
					} catch (IOException e) {
						System.err.println("����ͷ��ͼƬʧ��");
						personalSettingsBuilder.setResultCode(PersonalSettingsMsg.PersonalSettingsRsp.ResultCode.FAIL);
						e.printStackTrace();
					}
				}

			} else {
				// �û�������
				Debug.log(new String[] { "Server_User", "personalSettings" }, "�û�" + networkMessage.ioSession.getRemoteAddress()
						+ "  ���û�������!");
				personalSettingsBuilder.setResultCode(PersonalSettingsMsg.PersonalSettingsRsp.ResultCode.FAIL);
			}
			session.close();

			// �ظ��ͻ���
			ServerNetwork.instance.sendMessageToClient(
					networkMessage.ioSession,
					NetworkMessage.packMessage(ProtoHead.ENetworkMessage.PERSONALSETTINGS_RSP.getNumber(),
							networkMessage.getMessageID(), personalSettingsBuilder.build().toByteArray()));
		} catch (InvalidProtocolBufferException e) {
			System.err.println("Server_User : ���������¼��� ��Protobuf�����л� " + networkMessage.ioSession.getRemoteAddress() + " �İ�ʱ�쳣��");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Server_User : ���������¼��� " + networkMessage.ioSession.getRemoteAddress() + " ���ذ�ʱ�쳣��");
			e.printStackTrace();
		}
	}
}