package api.process;

import java.util.Calendar;

import uti.utility.MyConfig;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import api.process.Charge.ErrorCode;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;
import dat.service.MOLog;
import dat.service.MatchObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyDataRow;
import db.define.MyTableModel;

public class ProDeregister
{
	public enum DeregResult
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

		private DeregResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static DeregResult FromInt(int iValue)
		{
			for (DeregResult type : DeregResult.values())
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
	Calendar mCal_SendMO = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	DefineMT.MTType mMTType = MTType.RegFail;

	String MTContent = "";
	MyTableModel mTableLog = null;

	String MSISDN = "";
	String RequestID = "";
	String Code = "";
	String Note = "";
	String Channel = "";
	
	String Keyword = "HUY API";
	String AppName = "";
	String UserName = "";
	String IP = "";

	public ProDeregister(String MSISDN, String RequestID, String Code,String Note, String Channel, String AppName, String UserName, String IP)
	{
		this.MSISDN = MSISDN.trim();
		this.RequestID = RequestID.trim();
		this.Code = Code.trim();
		this.Note = Note.trim();
		this.Channel = Channel.toUpperCase().trim();
		this.AppName = AppName.trim();
		this.UserName = UserName.trim();
		this.IP = IP.trim();
	}

	/**
	 * Lấy thông tin MO từ VNP gửi sang
	 */
	private void GetMO()
	{
		try
		{
			String[] arr = Note.split("\\|");
			if(arr.length >=2)
			{
				Keyword = arr[1];
			}
		}
		catch(Exception ex)
		{
			mLog.log.error(ex);
		}
	}
	
	private void Init() throws Exception
	{
		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);

			mTableLog = (MyTableModel) TableTemplate.Get_mMOLog().clone();
			mTableLog.Clear();
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private MTType AddToList()
	{
		try
		{
			if(MSISDN.startsWith("8484"))
				return mMTType;
			
			MTContent = Common.GetDefineMT_Message(mSubObj, mMatchObj, mMTType);
			
			if (Common.SendMT(MSISDN, Keyword, MTContent, RequestID))
				AddToMOLog(mMTType, MTContent);
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mMTType;
	}

	private void AddToMOLog(MTType mMTType_Current, String MTContent_Current)
	{
		try
		{
			MyDataRow mRow_Log = mTableLog.CreateNewRow();

			mRow_Log.SetValueCell("MatchID", mMatchObj.MatchID);
			mRow_Log.SetValueCell("MSISDN", MSISDN);
			mRow_Log.SetValueCell("LogDate", MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()));
			mRow_Log.SetValueCell("ChannelTypeID", Common.GetChannelType(Channel).GetValue());
			mRow_Log.SetValueCell("ChannelTypeName", Common.GetChannelType(Channel).toString());
			mRow_Log.SetValueCell("MTTypeID", mMTType_Current.GetValue());
			mRow_Log.SetValueCell("MTTypeName", mMTType_Current.toString());
			mRow_Log.SetValueCell("MO", Keyword);
			mRow_Log.SetValueCell("MT", MTContent_Current);
			mRow_Log.SetValueCell("PID", MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID));
			mRow_Log.SetValueCell("RequestID", RequestID.toString());
			mRow_Log.SetValueCell("PartnerID", mSubObj.PartnerID);

			mTableLog.AddNewRow(mRow_Log);

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	private void Insert_MOLog()
	{
		try
		{
			MOLog mMOLog = new MOLog(LocalConfig.mDBConfig_MSSQL);
			mMOLog.Insert(0, mTableLog.GetXML());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	private MyTableModel AddInfo() throws Exception
	{
		try
		{
			MyTableModel mTable_UnSub = (MyTableModel) TableTemplate.Get_mUnSubscriber().clone();
			mTable_UnSub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mRow_Sub = mTable_UnSub.CreateNewRow();
			mRow_Sub.SetValueCell("MSISDN", mSubObj.MSISDN);

			mRow_Sub.SetValueCell("FirstDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.FirstDate));
			mRow_Sub.SetValueCell("EffectiveDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.EffectiveDate));
			mRow_Sub.SetValueCell("ExpiryDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ExpiryDate));

			mRow_Sub.SetValueCell("RetryChargeCount", mSubObj.RetryChargeCount);

			if (mSubObj.RetryChargeDate != null)
				mRow_Sub.SetValueCell("RetryChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RetryChargeDate));

			if (mSubObj.ChargeDate != null)
				mRow_Sub.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ChargeDate));

			if (mSubObj.RenewChargeDate != null)
				mRow_Sub.SetValueCell("RenewChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RenewChargeDate));

			mRow_Sub.SetValueCell("ChannelTypeID", mSubObj.ChannelTypeID);
			mRow_Sub.SetValueCell("StatusID", mSubObj.StatusID);
			mRow_Sub.SetValueCell("PID", mSubObj.PID);
			mRow_Sub.SetValueCell("OrderID", mSubObj.OrderID);

			mRow_Sub.SetValueCell("MOByDay", mSubObj.MOByDay);
			mRow_Sub.SetValueCell("ChargeMark", mSubObj.ChargeMark);
			mRow_Sub.SetValueCell("WeekMark", mSubObj.WeekMark);
			mRow_Sub.SetValueCell("CodeByDay", mSubObj.CodeByDay);
			mRow_Sub.SetValueCell("TotalCode", mSubObj.TotalCode);
			mRow_Sub.SetValueCell("MatchID", mMatchObj.MatchID);
			mRow_Sub.SetValueCell("IsNotify", mSubObj.IsNotify);
			mRow_Sub.SetValueCell("AppID", mSubObj.AppID);
			mRow_Sub.SetValueCell("AppName", mSubObj.AppName);
			mRow_Sub.SetValueCell("UserName", mSubObj.UserName);
			mRow_Sub.SetValueCell("IP", mSubObj.IP);
			mRow_Sub.SetValueCell("PartnerID", mSubObj.PartnerID);

			if (mSubObj.LastUpdate != null)
				mRow_Sub.SetValueCell("LastUpdate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.LastUpdate));

			if (mSubObj.CofirmDeregDate != null)
			{
				mRow_Sub.SetValueCell("CofirmDeregDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.CofirmDeregDate));
			}
			
			if (mSubObj.DeregDate != null)
			{
				mRow_Sub.SetValueCell("DeregDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.DeregDate));
			}

			if (mSubObj.NotifyDate != null)
				mRow_Sub.SetValueCell("NotifyDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.NotifyDate));
			
			mTable_UnSub.AddNewRow(mRow_Sub);
			return mTable_UnSub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean MoveToSub() throws Exception
	{
		try
		{
			MyTableModel mTable_UnSub = AddInfo();

			if (!mUnSub.Move(0, mTable_UnSub.GetXML()))
			{
				mLog.log.info(" Move Tu Sub Sang UnSub KHONG THANH CONG: XML Insert-->" + mTable_UnSub.GetXML());
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * tạo dữ liệu cho những đăng ký lại (trước đó đã hủy dịch vụ)
	 * 
	 * @throws Exception
	 */
	private void CreateDeReg() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = Common.GetChannelType(Channel).GetValue();
			mSubObj.DeregDate = mCal_Current.getTime();

			mSubObj.AppID = Common.GetApplication(AppName).GetValue();
			mSubObj.AppName = Common.GetApplication(AppName).toString();
			mSubObj.UserName = UserName;
			mSubObj.IP = IP;
		}
		catch (Exception ex)
		{
			throw ex;
		}

	}

	public MTType Process()
	{
		try
		{
			// Khoi tao
			Init();

			GetMO();
			
			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN);

			if (mTable_Sub.GetRowCount() > 0)
				mSubObj = SubscriberObject.Convert(mTable_Sub, false);

			mSubObj.PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			// Nếu chưa đăng ký dịch vụ
			if (mSubObj.IsNull())
			{
				mMTType = MTType.DeregNotRegister;
				return AddToList();
			}

			CreateDeReg();
			ErrorCode mResult = Charge.ChargeDereg(mSubObj.PartnerID,MSISDN, Keyword, Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);
			if (mResult != ErrorCode.ChargeSuccess)
			{
				mMTType = MTType.RegFail;
				return AddToList();
			}

			if (MoveToSub())
			{
				mMTType = MTType.DeregSuccess;
				return AddToList();
			}

			mMTType = MTType.DeregFail;

			return AddToList();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			mMTType = MTType.DeregFail;
			return AddToList();
		}
		finally
		{
			// Insert vao log
			Insert_MOLog();
		}
	}

}
