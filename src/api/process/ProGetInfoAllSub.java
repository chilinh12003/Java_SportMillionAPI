package api.process;

import java.util.Collections;
import java.util.Vector;

import uti.utility.MyConfig;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyTableModel;

public class ProGetInfoAllSub
{
	public enum InfoSubResult
	{
		// 0 Đăng ký thành công dịch vụ
		Success(0),

		// 1xx Đều là đăng ký không thành công
		Fail(100),
		// Lỗi hệ thống
		SystemError(101),
		// Thông tin nhập vào không hợp lệ
		InputInvalid(102);

		private int value;

		private InfoSubResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static InfoSubResult FromInt(int iValue)
		{
			for (InfoSubResult type : InfoSubResult.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Fail;
		}
	}

	public enum Status
	{
		// 0 Đăng ký thành công dịch vụ
		NotReg(0), Register(1), NotExist(2), NotSpecify(3), ;

		private int value;

		private Status(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static Status FromInt(int iValue)
		{
			for (Status type : Status.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return NotSpecify;
		}
	}

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	String MSISDN = "";
	String RequestID = "";
	String Channel = "";
	String AppName = "";
	String UserName = "";
	String IP = "";

	public ProGetInfoAllSub(String MSISDN, String RequestID, String Channel, String AppName, String UserName, String IP)
	{
		this.MSISDN = MSISDN;
		this.RequestID = RequestID;
		this.Channel = Channel.toUpperCase().trim();
		this.AppName = AppName;
		this.UserName = UserName;
		this.IP = IP;
	}

	private String GetResponse(Vector<SubscriberObject> mList) throws Exception
	{
		InfoSubResult mInfoSubResult = InfoSubResult.Fail;
		Status mStatus = Status.NotSpecify;
		StringBuilder mBuilder = new StringBuilder("");

		for (SubscriberObject mSubObj : mList)
		{
			String packagename = "NULL";
			String last_time_subscribe = "NULL";
			String last_time_unsubscribe = "NULL";
			String last_time_renew = "NULL";
			String last_time_retry = "NULL";
			String expire_time = "NULL";

			if (mSubObj.IsNull() || (mSubObj.IsDereg && mSubObj.StatusID == Subscriber.Status.UndoSub.GetValue()))
			{
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.NotExist;
			}
			else if (!mSubObj.IsNull() && mSubObj.IsDereg == false)
			{
				// Đang sử dụng dịch vụ
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.Register;

				packagename = LocalConfig.PackageName;
				if (mSubObj.EffectiveDate != null)
					last_time_subscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.EffectiveDate);

				if (mSubObj.RenewChargeDate != null)
					last_time_renew = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RenewChargeDate);

				if (mSubObj.RetryChargeDate != null)
					last_time_retry = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RetryChargeDate);

				if (mSubObj.ExpiryDate != null)
					expire_time = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.ExpiryDate);

				if (mSubObj.DeregDate != null)
					last_time_unsubscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.DeregDate);
			}
			else if (!mSubObj.IsNull() && mSubObj.IsDereg == true)
			{
				// Đã hủy dịch vụ
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.NotReg;

				if (mSubObj.EffectiveDate != null)
					last_time_subscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.EffectiveDate);

				if (mSubObj.RenewChargeDate != null)
					last_time_renew = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RenewChargeDate);

				if (mSubObj.RetryChargeDate != null)
					last_time_retry = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RetryChargeDate);

				if (mSubObj.ExpiryDate != null)
					expire_time = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.ExpiryDate);

				if (mSubObj.DeregDate != null)
					last_time_unsubscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.DeregDate);
			}
			String Format = "<SERVICE><error>%s</error><error_desc>%s</error_desc><packagename>%s</packagename><status>%s</status><last_time_subscribe>%s</last_time_subscribe><last_time_unsubscribe>%s</last_time_unsubscribe><last_time_renew>%s</last_time_renew><last_time_retry>%s</last_time_retry><expire_time>%s</expire_time></SERVICE>";
			mBuilder.append(String.format(Format,
					new Object[]{mInfoSubResult.GetValue().toString(), mInfoSubResult.toString(), packagename,
							mStatus.GetValue().toString(), last_time_subscribe, last_time_unsubscribe, last_time_renew,
							last_time_retry, expire_time}));
		}
		return mBuilder.toString();
	}

	public String Process()
	{

		InfoSubResult mInfoSubResult = InfoSubResult.Fail;
		Status mStatus = Status.NotSpecify;
		String packagename = "NULL";
		String last_time_subscribe = "NULL";
		String last_time_unsubscribe = "NULL";
		String last_time_renew = "NULL";
		String last_time_retry = "NULL";
		String expire_time = "NULL";

		Vector<SubscriberObject> mList = new Vector<SubscriberObject>();
		Vector<SubscriberObject> mListUnSub = new Vector<SubscriberObject>();

		String ListService = "";
		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);

			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);
			// Lấy thông tin khách hàng đã đăng ký
			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN);
			mList = SubscriberObject.ConvertToList(mTable_Sub, false);

			MyTableModel mTable_UnSub = mUnSub.Select(2, PID.toString(), MSISDN);
			mListUnSub = SubscriberObject.ConvertToList(mTable_UnSub, true);
			if (mList.size() > 0 && mListUnSub.size() > 0)
			{
				Collections.addAll(mListUnSub);
			}
			else if (mList.size() == 0 && mListUnSub.size() > 0)
			{
				mList = mListUnSub;
			}

			if (mList.size() == 0)
			{
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.NotExist;
				String Format = "<SERVICE><error>%s</error><error_desc>%s</error_desc><packagename>%s</packagename><status>%s</status><last_time_subscribe>%s</last_time_subscribe><last_time_unsubscribe>%s</last_time_unsubscribe><last_time_renew>%s</last_time_renew><last_time_retry>%s</last_time_retry><expire_time>%s</expire_time></SERVICE>";
				ListService = String.format(Format,
						new Object[]{mInfoSubResult.GetValue().toString(), mInfoSubResult.toString(), packagename,
								mStatus.GetValue().toString(), last_time_subscribe, last_time_unsubscribe,
								last_time_renew, last_time_retry, expire_time});
			}
			else
			{
				ListService = GetResponse(mList);
			}
		}
		catch (Exception ex)
		{
			mInfoSubResult = InfoSubResult.SystemError;

			mStatus = Status.NotSpecify;
			String Format = "<SERVICE><error>%s</error><error_desc>%s</error_desc><packagename>%s</packagename><status>%s</status><last_time_subscribe>%s</last_time_subscribe><last_time_unsubscribe>%s</last_time_unsubscribe><last_time_renew>%s</last_time_renew><last_time_retry>%s</last_time_retry><expire_time>%s</expire_time></SERVICE>";
			ListService = String.format(Format,
					new Object[]{mInfoSubResult.GetValue().toString(), mInfoSubResult.toString(), packagename,
							mStatus.GetValue().toString(), last_time_subscribe, last_time_unsubscribe, last_time_renew,
							last_time_retry, expire_time});

			mLog.log.error(ex);
		}

		return "<?xml version=\"1.0\" encoding=\"utf-8\" ?><RESPONSE>" + ListService + "</RESPONSE>";
	}

}