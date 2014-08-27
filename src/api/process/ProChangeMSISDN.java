package api.process;

import java.util.Calendar;

import uti.utility.MyConvert;
import uti.utility.MyLogger;
import dat.service.MatchObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyTableModel;

public class ProChangeMSISDN
{

	public enum ChangeMSISDNResult
	{
		// 0 Đăng ký thành công dịch vụ
		Success(0),
		// 1 Thuê bao này đã tồn tại
		NotExistSub(1),
		// 1xx Đều là đăng ký không thành công
		Fail(100),
		// Lỗi hệ thống
		SystemError(101),
		// Thông tin nhập vào không hợp lệ
		InputInvalid(102), ;

		private int value;

		private ChangeMSISDNResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static ChangeMSISDNResult FromInt(int iValue)
		{
			for (ChangeMSISDNResult type : ChangeMSISDNResult.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Fail;
		}
	}

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());

	SubscriberObject mSubObj = new SubscriberObject();
	MatchObject mMatchObj = new MatchObject();

	Calendar mCal_Current = Calendar.getInstance();
	Calendar ExpireDate = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	ChangeMSISDNResult mDeregAllResult = ChangeMSISDNResult.Fail;

	String msisdnA = "";
	String msisdnB = "";
	String RequestID = "";
	String Channel = "";
	String AppName = "";
	String UserName = "";
	String IP = "";

	public ProChangeMSISDN(String msisdnA, String msisdnB, String RequestID, String Channel, String AppName, String UserName, String IP)
	{
		this.msisdnA = msisdnA;
		this.msisdnB = msisdnB;
		this.RequestID = RequestID;
		this.Channel = Channel.toUpperCase().trim();
		this.AppName = AppName;
		this.UserName = UserName;
		this.IP = IP;
	}

	private void Init() throws Exception
	{
		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean ChangeMSISDN(Integer PIDOld, String MSISDNOld, Integer PIDNew, String MSISDNNew) throws Exception
	{
		try
		{
			if (!mSub.ChangeMSISDN(PIDOld, MSISDNOld, PIDNew, MSISDNNew))
			{
				mLog.log.info(" Chuyen so thue bao KHONG THANH CONG: XML Info-->PIDOld:" + PIDOld + "|MSISDNOld:" + MSISDNOld + "|PIDNew:" + PIDNew
						+ "|MSISDNNew:" + MSISDNNew);
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public String Process()
	{
		mDeregAllResult = ChangeMSISDNResult.Fail;
		MyTableModel mTable_Sub_A = null;
		MyTableModel mTable_Sub_B = null;

		try
		{
			// Khoi tao
			Init();

			Integer PIDOld = MyConvert.GetPIDByMSISDN(msisdnA, LocalConfig.MAX_PID);
			Integer PIDNew = MyConvert.GetPIDByMSISDN(msisdnB, LocalConfig.MAX_PID);

			mTable_Sub_A = mSub.Select(2, PIDOld.toString(), msisdnA);
			mTable_Sub_B = mSub.Select(2, PIDOld.toString(), msisdnB);
			
			if (mTable_Sub_A.GetRowCount() < 1)
			{
				mDeregAllResult = ChangeMSISDNResult.NotExistSub;
				return GetResponse(mDeregAllResult);
			}

			// Kiểm tra và xóa số điện thoại B trước nếu có
			if (mTable_Sub_B.GetRowCount() > 0)
			{
				if (!mSub.Delete(2, mTable_Sub_B.GetXML()))
				{
					mDeregAllResult = ChangeMSISDNResult.Fail;
					return GetResponse(mDeregAllResult);
				}
			}

			if (ChangeMSISDN(PIDOld, msisdnA, PIDNew, msisdnB))
			{
				mDeregAllResult = ChangeMSISDNResult.Success;
			}
			else
			{
				mDeregAllResult = ChangeMSISDNResult.Fail;
			}
		}
		catch (Exception ex)
		{
			mDeregAllResult = ChangeMSISDNResult.SystemError;
			mLog.log.error(ex);
		}
		finally
		{
			try
			{
				MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_CHANGE_MSISDN",
						"MSISDN_A --> " + mTable_Sub_A.GetXML() + "|Result:" + mDeregAllResult.toString());
				MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_CHANGE_MSISDN",
						"MSISDN_B --> " + mTable_Sub_B.GetXML() + "|Result:" + mDeregAllResult.toString());
			}
			catch (Exception ex)
			{
				mLog.log.error(ex);
			}
		}
		return GetResponse(mDeregAllResult);
	}

	private String GetResponse(ChangeMSISDNResult mDeregAllResult)
	{
		String XMLReturn = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<RESPONSE>" + "<ERRORID>" + mDeregAllResult.GetValue() + "</ERRORID>"
				+ "<ERRORDESC>" + mDeregAllResult.toString() + "</ERRORDESC>" + "</RESPONSE>";
		return XMLReturn;
	}

}