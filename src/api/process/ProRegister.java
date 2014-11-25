package api.process;

import java.util.Calendar;
import java.util.Vector;

import uti.utility.MyConfig;
import uti.utility.VNPApplication;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import api.process.Charge.ErrorCode;
import api.process.PromotionObject.AdvertiseType;
import api.process.PromotionObject.BundleType;
import api.process.PromotionObject.PromotionType;
import api.process.PromotionObject.TrialType;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;
import dat.service.News.NewsType;
import dat.service.MOLog;
import dat.service.MatchObject;
import dat.service.NewsObject;
import dat.service.WapRegLog;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyDataRow;
import db.define.MyTableModel;

public class ProRegister
{
	public enum RegResult
	{
		// 0 Đăng ký thành công dịch vụ
		Success(0),
		// 1 Thuê bao này đã tồn tại
		ExistSub(1),
		// 2 Đăng ký rồi và đăng ký lại dịch vụ
		Repeat(2),
		// 3 Đăng ký thành công dịch vụ và không bị trừ cước đăng ký
		SuccessFree(3),
		// 4 Đăng ký thành công dịch vụ và bị trừ cước đăng ký
		SucessPay(4),
		// 5 Đăng ký không thành công do không đủ tiền trong tài khoản
		EnoughMoney(5),
		// 1xx Đều là đăng ký không thành công
		Fail(100),
		// Lỗi hệ thống
		SystemError(101),
		// Thông tin nhập vào không hợp lệ
		InputInvalid(102), ;

		private int value;

		private RegResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static RegResult FromInt(int iValue)
		{
			for (RegResult type : RegResult.values())
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

	PromotionObject mPromoObj = new PromotionObject();

	Calendar mCal_Current = Calendar.getInstance();
	Calendar mCal_SendMO = Calendar.getInstance();
	Calendar mCal_Expire = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	DefineMT.MTType mMTType = MTType.RegFail;

	String MTContent = "";
	MyTableModel mTableLog = null;

	MyTableModel mTable_WapRegLog = null;

	String MSISDN = "";
	String RequestID = "";
	String Code = "";
	String Channel = "";

	String Keyword = "DK API";
	String AppName = "";
	String UserName = "";
	String IP = "";

	String Promotion = "";
	String Trial = "";
	String Note = "";
	String Bundle = "";

	// Thời gian miễn phí để chèn vào MT trả về cho khách hàng
	String FreeTime = "ngay dau tien";

	// Nếu > 0 thì ID của partner cần pass
	int PartnerID_Pass = 0;

	public ProRegister(String MSISDN, String RequestID, String Code, String Promotion, String Trial, String Bundle,
			String Note, String Channel, String AppName, String UserName, String IP)
	{
		this.MSISDN = MSISDN.trim();
		this.RequestID = RequestID.trim();
		this.Code = Code.trim();

		this.Promotion = Promotion.trim();
		this.Trial = Trial.trim();
		this.Bundle = Bundle.trim();
		this.Note = Note.trim();

		this.Channel = Channel.toUpperCase().trim();
		this.AppName = AppName.trim();
		this.UserName = UserName.trim();
		this.IP = IP.trim();
	}

	/**
	 * Tính toán trường hợp khuyến mãi
	 * 
	 * @throws Exception
	 */
	private void CalculatePromotion() throws Exception
	{
		try
		{
			if (Bundle.equalsIgnoreCase("1"))
			{
				mPromoObj.mAdvertiseType = AdvertiseType.Bundle;
				mPromoObj.mBundleType = BundleType.NeverExpire;
			}
			else if (!Trial.equalsIgnoreCase("") && !Trial.equalsIgnoreCase("0"))
			{
				if (Trial.indexOf("c") > 0)
				{
					mPromoObj.mTrialType = TrialType.Day;
				}
				else if (Trial.indexOf("d") > 0)
				{
					mPromoObj.mTrialType = TrialType.Day;
				}
				else if (Trial.indexOf("w") > 0)
				{
					mPromoObj.mTrialType = TrialType.Week;
				}
				else if (Trial.indexOf("m") > 0)
				{
					mPromoObj.mTrialType = TrialType.Month;
				}

				mPromoObj.TrialNumberFree = Integer.parseInt(Trial.substring(0, Trial.length() - 1));
				mPromoObj.mAdvertiseType = AdvertiseType.Trial;
			}
			else if (!Promotion.equalsIgnoreCase("") && !Promotion.equalsIgnoreCase("0"))
			{
				if (Promotion.indexOf("c") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Day;
				}
				else if (Promotion.indexOf("d") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Day;
				}
				else if (Promotion.indexOf("w") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Week;
				}
				else if (Promotion.indexOf("m") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Month;
				}

				mPromoObj.PromotionNumberFree = Integer.parseInt(Promotion.substring(0, Promotion.length() - 1));
				mPromoObj.mAdvertiseType = AdvertiseType.Promotion;
			}

		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * Thiết lập thông tin Promotion cho đối tượng sub
	 * 
	 * @throws Exception
	 */
	private void SetPromotionToSub() throws Exception
	{
		mCal_Expire = Calendar.getInstance();
		mCal_Expire.set(Calendar.MILLISECOND, 0);
		mCal_Expire.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH),
				mCal_Current.get(Calendar.DATE), 23, 59, 59);
		if (mPromoObj.mAdvertiseType == AdvertiseType.Bundle)
		{
			if (mPromoObj.mBundleType == BundleType.NeverExpire)
			{
				mCal_Expire.set(2030, mCal_Current.get(Calendar.MONTH), mCal_Current.get(Calendar.DATE), 23, 59, 59);
			}
			mSubObj.StatusID = dat.sub.Subscriber.Status.ActiveBundle.GetValue();
		}
		else if (mPromoObj.mAdvertiseType == AdvertiseType.Trial)
		{
			if (mPromoObj.mTrialType == TrialType.Day)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.TrialNumberFree - 1);
				FreeTime = "" + mPromoObj.TrialNumberFree + " ngay";
			}
			else if (mPromoObj.mTrialType == TrialType.Week)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.TrialNumberFree * 7);
				FreeTime = "" + mPromoObj.TrialNumberFree + " tuan";
			}
			else if (mPromoObj.mTrialType == TrialType.Month)
			{
				mCal_Expire.add(Calendar.MONTH, mPromoObj.TrialNumberFree);
				FreeTime = "" + mPromoObj.TrialNumberFree + " thang";
			}
			mSubObj.StatusID = dat.sub.Subscriber.Status.ActiveTrial.GetValue();
		}
		else if (mPromoObj.mAdvertiseType == AdvertiseType.Promotion)
		{
			if (mPromoObj.mPromotionType == PromotionType.Day)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.PromotionNumberFree - 1);
				FreeTime = "" + mPromoObj.PromotionNumberFree + " ngay";
			}
			else if (mPromoObj.mPromotionType == PromotionType.Week)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.PromotionNumberFree * 7);
				FreeTime = "" + mPromoObj.PromotionNumberFree + " tuan";
			}
			else if (mPromoObj.mPromotionType == PromotionType.Month)
			{
				mCal_Expire.add(Calendar.MONTH, mPromoObj.PromotionNumberFree);
				FreeTime = "" + mPromoObj.PromotionNumberFree + " thang";
			}
			mSubObj.StatusID = dat.sub.Subscriber.Status.ActivePromotion.GetValue();
		}

		mSubObj.ExpiryDate = mCal_Expire.getTime();
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

			mCal_Expire.set(Calendar.MILLISECOND, 0);
			mCal_Expire.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH),
					mCal_Current.get(Calendar.DATE), 23, 59, 59);

		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private void AddToMOLog(MTType mMTType_Current, String MTContent_Current) throws Exception
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
			mRow_Log.SetValueCell("RequestID", RequestID);
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
			MyTableModel mTable_Sub = mSub.Select(0);
			mTable_Sub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mRow_Sub = mTable_Sub.CreateNewRow();
			mRow_Sub.SetValueCell("MSISDN", mSubObj.MSISDN);

			mRow_Sub.SetValueCell("FirstDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.FirstDate));
			mRow_Sub.SetValueCell("EffectiveDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.EffectiveDate));
			mRow_Sub.SetValueCell("ExpiryDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ExpiryDate));

			mRow_Sub.SetValueCell("RetryChargeCount", mSubObj.RetryChargeCount);

			if (mSubObj.ChargeDate != null)
				mRow_Sub.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ChargeDate));

			if (mSubObj.RenewChargeDate != null)
				mRow_Sub.SetValueCell("RenewChargeDate",
						MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RenewChargeDate));

			if (mSubObj.RetryChargeDate != null)
				mRow_Sub.SetValueCell("RetryChargeDate",
						MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RetryChargeDate));

			if (mSubObj.CofirmDeregDate != null)
				mRow_Sub.SetValueCell("CofirmDeregDate",
						MyConfig.Get_DateFormat_InsertDB().format(mSubObj.CofirmDeregDate));

			if (mSubObj.DeregDate != null)
			{
				mRow_Sub.SetValueCell("DeregDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.DeregDate));
			}

			mRow_Sub.SetValueCell("ChannelTypeID", mSubObj.ChannelTypeID);
			mRow_Sub.SetValueCell("StatusID", mSubObj.StatusID);
			mRow_Sub.SetValueCell("PID", mSubObj.PID);

			mRow_Sub.SetValueCell("MOByDay", mSubObj.MOByDay);
			mRow_Sub.SetValueCell("ChargeMark", mSubObj.ChargeMark);
			mRow_Sub.SetValueCell("WeekMark", mSubObj.WeekMark);
			mRow_Sub.SetValueCell("CodeByDay", mSubObj.CodeByDay);
			mRow_Sub.SetValueCell("TotalCode", mSubObj.TotalCode);
			mRow_Sub.SetValueCell("MatchID", mMatchObj.MatchID);
			mRow_Sub.SetValueCell("IsNotify", mSubObj.IsNotify);

			mRow_Sub.SetValueCell("AnswerKQ", mSubObj.AnswerKQ);
			mRow_Sub.SetValueCell("AnswerBT", mSubObj.AnswerBT);
			mRow_Sub.SetValueCell("AnswerGB", mSubObj.AnswerGB);
			mRow_Sub.SetValueCell("AnswerTS", mSubObj.AnswerTS);
			mRow_Sub.SetValueCell("AnswerTV", mSubObj.AnswerTV);

			mRow_Sub.SetValueCell("AppID", mSubObj.AppID);
			mRow_Sub.SetValueCell("AppName", mSubObj.AppName);
			mRow_Sub.SetValueCell("UserName", mSubObj.UserName);
			mRow_Sub.SetValueCell("IP", mSubObj.IP);
			mRow_Sub.SetValueCell("PartnerID", mSubObj.PartnerID);

			if (mSubObj.LastUpdate != null)
				mRow_Sub.SetValueCell("LastUpdate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.LastUpdate));

			mTable_Sub.AddNewRow(mRow_Sub);
			return mTable_Sub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean Insert_Sub() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = AddInfo();

			if (!mSub.Insert(0, mTable_Sub.GetXML()))
			{
				mLog.log.info("Insert vao table Subscriber KHONG THANH CONG: XML Insert-->" + mTable_Sub.GetXML());
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean MoveToUnSub() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = AddInfo();

			if (!mSub.Move(0, mTable_Sub.GetXML()))
			{
				mLog.log.info("Move tu UnSub Sang Sub KHONG THANH CONG: XML Insert-->" + mTable_Sub.GetXML());
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
	private void CreateRegAgain() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = Common.GetChannelType(Channel).GetValue();

			mSubObj.EffectiveDate = mCal_Current.getTime();
			mSubObj.ExpiryDate = mCal_Expire.getTime();

			mSubObj.LastUpdate = mCal_Current.getTime();
			mSubObj.MSISDN = MSISDN;
			mSubObj.PID = MyConvert.GetPIDByMSISDN(mSubObj.MSISDN, LocalConfig.MAX_PID);
			mSubObj.StatusID = dat.sub.Subscriber.Status.Active.GetValue();

			mSubObj.ChargeDate = mCal_Current.getTime();
			mSubObj.MOByDay = 0;
			mSubObj.ChargeMark = LocalConfig.RegMark;
			mSubObj.WeekMark = LocalConfig.RegMark;
			mSubObj.CodeByDay = LocalConfig.RegMark / LocalConfig.MarkPerCode;
			mSubObj.TotalCode = LocalConfig.RegMark / LocalConfig.MarkPerCode;

			mSubObj.MatchID = mMatchObj.MatchID;
			mSubObj.AnswerKQ = "";
			mSubObj.AnswerBT = "";
			mSubObj.AnswerGB = "";
			mSubObj.AnswerTS = "";
			mSubObj.AnswerTV = "";

			mSubObj.CofirmDeregDate = null;

			mSubObj.IsNotify = true;
			mSubObj.AppID = Common.GetApplication(AppName).GetValue();
			mSubObj.AppName = Common.GetApplication(AppName).toString();
			mSubObj.UserName = UserName;
			mSubObj.IP = IP;
			mSubObj.PartnerID = GetPartnerID();
		}
		catch (Exception ex)
		{
			throw ex;
		}

	}

	/**
	 * Tạo dữ liệu cho một đăng ký mới
	 * 
	 * @throws Exception
	 */
	private void CreateNewReg() throws Exception
	{
		try
		{
			mSubObj.MSISDN = MSISDN;
			mSubObj.FirstDate = mCal_Current.getTime();
			mSubObj.EffectiveDate = mCal_Current.getTime();
			mSubObj.ExpiryDate = mCal_Expire.getTime();

			mSubObj.ChargeDate = null;
			mSubObj.ChannelTypeID = Common.GetChannelType(Channel).GetValue();
			mSubObj.StatusID = dat.sub.Subscriber.Status.Active.GetValue();
			mSubObj.PID = MyConvert.GetPIDByMSISDN(mSubObj.MSISDN, LocalConfig.MAX_PID);
			mSubObj.MOByDay = 0;
			mSubObj.ChargeMark = LocalConfig.RegMark;
			mSubObj.WeekMark = LocalConfig.RegMark;
			mSubObj.CodeByDay = LocalConfig.RegMark / LocalConfig.MarkPerCode;
			mSubObj.TotalCode = LocalConfig.RegMark / LocalConfig.MarkPerCode;
			mSubObj.MatchID = mMatchObj.MatchID;
			mSubObj.AnswerKQ = "";
			mSubObj.AnswerBT = "";
			mSubObj.AnswerGB = "";
			mSubObj.AnswerTS = "";
			mSubObj.AnswerTV = "";

			mSubObj.IsNotify = true;

			mSubObj.CofirmDeregDate = null;
			mSubObj.IsDereg = false;
			mSubObj.LastUpdate = mCal_Current.getTime();
			mSubObj.AppID = Common.GetApplication(AppName).GetValue();
			mSubObj.AppName = Common.GetApplication(AppName).toString();
			mSubObj.UserName = UserName;
			mSubObj.IP = IP;
			mSubObj.PartnerID = GetPartnerID();
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * Chuyển Sản lượng sang PartnerID của HB
	 * 
	 * @param PartnerID
	 * @return
	 */
	private int GetPartnerID_Pass(int PartnerID)
	{
		try
		{
			Calendar BeginDate = Calendar.getInstance();
			Calendar EndDate = Calendar.getInstance();

			BeginDate.set(Calendar.MILLISECOND, 0);
			BeginDate.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH),
					mCal_Current.get(Calendar.DATE), 0, 0, 0);

			EndDate.set(Calendar.MILLISECOND, 0);
			EndDate.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH),
					mCal_Current.get(Calendar.DATE), 23, 59, 59);

			WapRegLog mWapRegLog = new WapRegLog(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable_Count = mWapRegLog.Select(5, "2", Integer.toString(PartnerID), MyConfig
					.Get_DateFormat_InsertDB().format(BeginDate.getTime()),
					MyConfig.Get_DateFormat_InsertDB().format(EndDate.getTime()));
			
			if (mTable_Count.GetRowCount() > 0)
			{
				int PartnetCount = Integer.parseInt(mTable_Count.GetValueAt(0, "Total").toString());
				if (PartnetCount % 10 == 2 || PartnetCount % 10 == 8)
				{
					PartnerID_Pass = 28;
					return 28;
				}
			}
			PartnerID_Pass = 0;
			return PartnerID;
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			return PartnerID;
		}

	}
	
	private int GetPartnerID() throws Exception
	{
		if (Common.GetApplication(AppName).mApp == VNPApplication.TelcoApplication.MOBILE_ADS
				|| Common.GetApplication(AppName).mApp == VNPApplication.TelcoApplication.MOBILEADS)
		{
			WapRegLog mWapRegLog = new WapRegLog(LocalConfig.mDBConfig_MSSQL);
			mTable_WapRegLog = mWapRegLog.Select(2, mSubObj.MSISDN);

			if (mTable_WapRegLog != null && mTable_WapRegLog.GetRowCount() > 0)
			{

				return GetPartnerID_Pass(Integer.parseInt(mTable_WapRegLog.GetValueAt(0, "PartnerID").toString()));
			}
		}
		return 0;
	}

	private void Update_WapRegLog()
	{
		try
		{
			if (Common.GetApplication(AppName).mApp == VNPApplication.TelcoApplication.MOBILE_ADS
					|| Common.GetApplication(AppName).mApp == VNPApplication.TelcoApplication.MOBILEADS)
			{

				if (mTable_WapRegLog == null || mTable_WapRegLog.GetRowCount() < 1)
					return;

				mTable_WapRegLog.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), 0,
						"RegisterDate");

				mTable_WapRegLog.SetValueAt(WapRegLog.Status.Registered.GetValue(), 0, "StatusID");

				if (PartnerID_Pass > 0)
				{
					mTable_WapRegLog.SetValueAt("PartnerID_Pass:" + Integer.toString(PartnerID_Pass), 0, "Note");
				}
				
				WapRegLog mWapRegLog = new WapRegLog(LocalConfig.mDBConfig_MSSQL);
				mWapRegLog.Update(1, mTable_WapRegLog.GetXML());
			}
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	private MTType AddToList()
	{
		try
		{
			if (MSISDN.startsWith("8484"))
				return mMTType;
			MTContent = Common.GetDefineMT_Message(mSubObj, mMatchObj, mMTType);
			MTContent = MTContent.replace("[FreeTime]", FreeTime);

			if (Common.SendMT(MSISDN, Keyword, MTContent, RequestID))
				AddToMOLog(mMTType, MTContent);

			if (mMTType == MTType.RegNewSuccess || mMTType == MTType.RegCCOSSuccessFree

			|| mMTType == MTType.RegAgainSuccessFree || mMTType == MTType.RegAgainSuccessNotFree
					|| mMTType == MTType.RegCCOSSuccessNotFree)
			{
				Vector<NewsObject> mListNews = Common.Get_List_Two_News();

				String MTContent_Current_Push = "";
				String MTContent_Current_Remider = "";
				if (mListNews.size() > 0)
				{
					for (NewsObject mNewObj : mListNews)
					{
						if (mNewObj.mNewsType == NewsType.Push)
							MTContent_Current_Push = mNewObj.Content;

						if (mNewObj.mNewsType == NewsType.Reminder)
							MTContent_Current_Remider = mNewObj.Content;
					}
				}

				if (!MTContent_Current_Push.equalsIgnoreCase(""))
				{
					if (Common.SendMT(MSISDN, Keyword, MTContent_Current_Push, RequestID))
						AddToMOLog(MTType.PushMT, MTContent_Current_Push);
				}

				if (!MTContent_Current_Remider.equalsIgnoreCase(""))
				{
					if (Common.SendMT(MSISDN, Keyword, MTContent_Current_Remider, RequestID))
						AddToMOLog(MTType.PushMTReminder, MTContent_Current_Remider);
				}
			}
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mMTType;
	}

	public MTType Process()
	{
		mMTType = MTType.RegFail;
		try
		{
			// Khoi tao
			Init();

			GetMO();
			
			// Tính toàn khuyến mãi
			CalculatePromotion();

			// Lấy trận đấu đang diễn ra
			mMatchObj = Common.GetCurrentMatch(mCal_SendMO.getTime());

			if (mMatchObj.IsNull())
			{
				// nếu không có trận đầu nào đang diễn ra, thì lấy trận
				// đấu tiếp theo
				mMatchObj = Common.GetNextMatch();
			}

			/*
			 * if (mMatchObj.IsNull()) {
			 * mLog.log.info("Tran dau khong khong ton tai."); mMTType =
			 * MTType.Invalid; return AddToList(); }
			 */

			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			// Lấy thông tin khách hàng đã đăng ký
			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN);

			mSubObj = SubscriberObject.Convert(mTable_Sub, false);

			if (mSubObj.IsNull())
			{
				mTable_Sub = mUnSub.Select(2, PID.toString(), MSISDN);

				if (mTable_Sub.GetRowCount() > 0)
					mSubObj = SubscriberObject.Convert(mTable_Sub, true);
			}

			mSubObj.PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			// Nếu đã đăng ký rồi và tiếp tục đăng ký
			if (!mSubObj.IsNull() && mSubObj.IsDereg == false)
			{
				// Kiểm tra còn free hay không
				if (mSubObj.IsFreeReg())
				{
					mMTType = MTType.RegRepeatFree;
					return AddToList();
				}
				else
				{
					mMTType = MTType.RegRepeatNotFree;
					return AddToList();
				}
			}

			// nếu được quảng cáo Bundel, trial hoặc promotion
			if (mPromoObj.mAdvertiseType != AdvertiseType.Normal)
			{
				// Tiến hành xử lý đăng ký khuyến mãi
				// Tạo dữ liệu cho đăng ký mới
				if (mSubObj.IsNull())
				{
					CreateNewReg();
					SetPromotionToSub();

					ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID, MSISDN, Keyword,
							Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);

					if (mResult != ErrorCode.ChargeSuccess)
					{
						mMTType = MTType.RegFail;
						return AddToList();
					}

					if (Insert_Sub())
					{
						if (mSubObj.AppID == VNPApplication.TelcoApplication.CCOS.GetValue())
						{
							mMTType = MTType.RegCCOSSuccessFree;
						}
						else
						{
							mMTType = MTType.RegNewSuccess;
						}
					}
					else
					{
						mMTType = MTType.RegFail;
					}

					return AddToList();
				}
				else if (mSubObj.IsDereg && mSubObj.StatusID == dat.sub.Subscriber.Status.UndoSub.GetValue())
				{
					CreateNewReg();
					SetPromotionToSub();

					ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID, MSISDN, Keyword,
							Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);

					if (mResult != ErrorCode.ChargeSuccess)
					{
						mMTType = MTType.RegFail;
						return AddToList();
					}
					if (MoveToUnSub())
					{
						if (mSubObj.AppID == VNPApplication.TelcoApplication.CCOS.GetValue())
						{

							mMTType = MTType.RegCCOSSuccessFree;
						}
						else
						{
							mMTType = MTType.RegNewSuccess;
						}
					}
					else
					{
						mMTType = MTType.RegFail;
					}

					return AddToList();
				}
				else
				{
					// đã từng sử dụng dịch vụ trước đây
					CreateRegAgain();

					SetPromotionToSub();

					ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID, MSISDN, Keyword,
							Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);

					if (mResult != ErrorCode.ChargeSuccess)
					{
						mMTType = MTType.RegFail;
						return AddToList();
					}
					if (MoveToUnSub())
					{
						if (mSubObj.AppID == VNPApplication.TelcoApplication.CCOS.GetValue())
						{

							mMTType = MTType.RegCCOSSuccessFree;
						}
						else
						{
							mMTType = MTType.RegNewSuccess;
						}
					}
					else
					{
						mMTType = MTType.RegFail;
					}

					return AddToList();
				}
			}

			// Đăng ký mới (chưa từng đăng ký trước đây)
			if (mSubObj.IsNull())
			{
				// Tạo dữ liệu cho đăng ký mới
				CreateNewReg();

				ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID, MSISDN, Keyword,
						Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);

				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail;
					return AddToList();
				}

				if (Insert_Sub())
				{
					if (mSubObj.AppID == VNPApplication.TelcoApplication.CCOS.GetValue())
					{

						mMTType = MTType.RegCCOSSuccessFree;
					}
					else
					{
						mMTType = MTType.RegNewSuccess;
					}
				}
				else
				{
					mMTType = MTType.RegFail;
				}

				return AddToList();
			}

			// nếu số điện thoại đã từng đăng ký trước đây nhưng bị Vinaphone
			// Hủy thuê bao
			if (mSubObj.IsDereg && mSubObj.StatusID == dat.sub.Subscriber.Status.UndoSub.GetValue())
			{
				CreateRegAgain();

				ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID, MSISDN, Keyword,
						Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);

				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail;
					return AddToList();
				}

				if (MoveToUnSub())
				{
					if (mSubObj.AppID == VNPApplication.TelcoApplication.CCOS.GetValue())
					{

						mMTType = MTType.RegCCOSSuccessFree;
					}
					else
					{
						mMTType = MTType.RegNewSuccess;
					}
				}
				else
				{
					mMTType = MTType.RegFail;
				}

				return AddToList();
			}

			// Đã đăng ký trước đó nhưng đang hủy
			if (mSubObj.IsDereg)
			{
				CreateRegAgain();

				// đồng bộ thuê bao sang Vinpahone
				ErrorCode mResult = Charge.ChargeReg(mSubObj.PartnerID, MSISDN, Keyword,
						Common.GetChannelType(Channel), Common.GetApplication(AppName), UserName, IP);

				// Charge
				if (mResult == ErrorCode.BlanceTooLow)
				{
					mMTType = MTType.RegNotEnoughMoney;
					return AddToList();
				}
				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail; // Đăng ký lại nhưng mất tiền
					return AddToList();
				}

				// Nếu xóa unsub hoặc Insert sub không thành công thì thông
				// báo lỗi
				if (MoveToUnSub())
				{
					if (mSubObj.AppID == VNPApplication.TelcoApplication.CCOS.GetValue())
					{

						mMTType = MTType.RegCCOSSuccessNotFree;
					}
					else
					{
						mMTType = MTType.RegAgainSuccessNotFree;
					}

					return AddToList();
				}

				mMTType = MTType.RegFail; // Đăng ký lại nhưng mất tiền
				return AddToList();
			}
			mMTType = MTType.RegFail;

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			mMTType = MTType.RegFail;
		}
		finally
		{
			// Insert vao log
			Insert_MOLog();
			Update_WapRegLog();
		}
		return AddToList();
	}
}
