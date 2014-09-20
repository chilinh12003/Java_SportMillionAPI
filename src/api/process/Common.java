package api.process;

import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uti.utility.MyCheck;
import uti.utility.MyConfig;
import uti.utility.MyConfig.VNPApplication;
import uti.utility.MyLogger;
import uti.utility.MyText;
import dat.gateway.ems_send_queue;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;
import dat.service.Match;
import dat.service.MatchObject;
import dat.service.News;
import dat.service.NewsObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import db.define.MyTableModel;

public class Common
{
	static MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, Common.class.toString());

	public static boolean SendMT(String MSISDN, String Keyword, String MTContent, String RequestID) throws Exception
	{
		try
		{
			if (MTContent.trim().equalsIgnoreCase(""))
				return true;

			ems_send_queue mSendQueue = new ems_send_queue(LocalConfig.mDBConfig_MySQL);

			String USER_ID = MSISDN;
			String SERVICE_ID = LocalConfig.SHORT_CODE;

			String COMMAND_CODE = Keyword;

			Long Temp = 0l;
			try
			{
				Temp = Long.parseLong(RequestID);
			}
			catch (Exception ex)
			{
				Temp = System.currentTimeMillis();
			}
			String REQUEST_ID = Temp.toString();

			return mSendQueue.Insert(USER_ID, SERVICE_ID, COMMAND_CODE, MTContent, REQUEST_ID, LocalConfig.LONG_MESSAGE_CONTENT_TYPE.toString());

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			return false;
		}
	}

	/**
	 * Lấy PID theo số điện thoại VD: 097(99)67755 thì (99%20+1) là số được lấy
	 * làm PID
	 * 
	 * @param MSISDN
	 * @return
	 * @throws Exception
	 */
	public static int GetPIDByMSISDN(String MSISDN) throws Exception
	{
		try
		{
			int PID = 1;
			String PID_Temp = "1";

			// hiệu chỉnh số điện thoại thành dạng 9xxx hoặc 1xxx
			String MSISDN_Temp = MyCheck.ValidPhoneNumber(MSISDN, "");

			if (MSISDN_Temp.startsWith("9"))
			{
				PID_Temp = MSISDN_Temp.substring(2, 4);
			}
			else
			{
				// là số điện thoại 11 số
				PID_Temp = MSISDN_Temp.substring(3, 5);
			}

			PID = Integer.parseInt(PID_Temp);

			PID = PID % 20 + 1;

			return PID;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * Kiếm tra trong danh sách đăng ký (trong db) có số điện thoại này chưa
	 * 
	 * @param PID
	 * @param MSISDN
	 * @return
	 * @throws Exception
	 */
	public static boolean CheckRegister(int PID, String MSISDN, int ServcieID) throws Exception
	{
		try
		{
			Subscriber mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);

			MyTableModel mTable = mSub.Select(2, Integer.toString(PID), MSISDN, Integer.toString(ServcieID));
			if (mTable.GetRowCount() > 0)
				return true;
			else
				return false;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * Lấy MT đã được định nghĩa trong DB, nếu ko có thì lấy MT mặc định
	 * 
	 * @param mMTType
	 * @return
	 * @throws Exception
	 */
	public static String GetDefineMT_Message(dat.service.DefineMT.MTType mMTType) throws Exception
	{
		try
		{
			String MT = DefineMT.GetMTContent(LocalConfig.GetListDefineMT(), mMTType);
			MT = MyText.RemoveSpecialLetter(2, MT, ".,;?:-_/[]{}()@!%&*=+ ");
			return MT;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public static String GetDefineMT_Message(dat.service.DefineMT.MTType mMTType, String FreeTime) throws Exception
	{
		try
		{
			String MT = DefineMT.GetMTContent(LocalConfig.GetListDefineMT(), mMTType);
			MT = MyText.RemoveSpecialLetter(2, MT, ".,;?:-_/[]{}()@!%&*=+ ");

			MT = MT.replace("[FreeTime]", FreeTime);
			return MT;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public static String GetDefineMT_Message(SubscriberObject mSubObj, MatchObject mMatchObj, DefineMT.MTType mMTType)
			throws Exception
	{
		try
		{
			String MT = DefineMT.GetMTContent(LocalConfig.GetListDefineMT(), mMTType);
			MT = MyText.RemoveSpecialLetter(2, MT, ".,;?:-_/[]{}()@!%&*=+ ");

			if (mMatchObj.IsNull())
			{
				mMatchObj = GetTodayMatch();
			}

			String CurrentHour="";
			String CurrentDate = "";
			
			String Match_ = "hom nay";
			String PlayDate = "hom nay";
			String PlayHour = "";
			String BeginHour = "08:30";
			String BeginDate = "hom nay";
			String EndHour = "";

			String Match1 = "ngay mai";
			String PlayDate1 = "ngay mai";
			String PlayHour1 = "";
			String BeginHour1 = "08:30";
			String BeginDate1 = "ngay mai";
			String EndHour1 = "";

			String Match2 = "ngay kia";
			String PlayDate2 = "ngay kia";
			// String PlayHour2 = "";
			String BeginHour2 = "08:30";
			String BeginDate2 = "ngay kia";
			String EndHour2 = "";

			String Team1 = "";
			String Team2 = "";

			String MOCount = "";
			String Value = "";
			String DayMark = "";
			String WeekMark ="";
			String DayCode = "";
			String AnswerCount = "";

			String X = "";
			String Y = "";

			if (!mMatchObj.IsNull() && mMatchObj.StatusID == Match.Status.Active.GetValue())
			{
				Team1 = mMatchObj.TeamName1;
				Team2 = mMatchObj.TeamName2;
				Match_ = mMatchObj.GetMatchName();
				PlayDate = mMatchObj.GetPlayDate();
				PlayHour = mMatchObj.GetPlayHour();
				BeginHour = mMatchObj.GetBeginHour();
				BeginDate = mMatchObj.GetBeginDate();
				EndHour = mMatchObj.GetEndHour();
			}
			
			if (mMTType == MTType.AnswerOver || mMTType == MTType.AnswerFinal || mMTType == MTType.ConsultMatch
					|| mMTType == MTType.AnswerExpire)
			{
				MatchObject mMatchObj1 = Common.GetNextMatch();
				if (!mMatchObj1.IsNull() && mMatchObj1.StatusID == Match.Status.Next.GetValue())
				{
					Match1 = mMatchObj1.GetMatchName();
					PlayDate1 = mMatchObj1.GetPlayDate();
					PlayHour1 = mMatchObj1.GetPlayHour();
					BeginHour1 = mMatchObj1.GetBeginHour();
					BeginDate1 = mMatchObj1.GetBeginDate();
					EndHour1 = mMatchObj1.GetEndHour();
				}

				MatchObject mMatchObj2 = Common.GetNext1Match();
				if (!mMatchObj2.IsNull() && mMatchObj2.StatusID == Match.Status.Next1.GetValue())
				{
					Match2 = mMatchObj2.GetMatchName();
					PlayDate2 = mMatchObj2.GetPlayDate();
					// PlayHour2 = mMatchObj2.GetPlayHour();
					BeginHour2 = mMatchObj2.GetBeginHour();
					BeginDate2 = mMatchObj2.GetBeginDate();
					EndHour2 = mMatchObj2.GetEndHour();
				}
			}

			if (!mSubObj.IsNull())
			{
				Integer TempCount = (LocalConfig.MaxAnswerByDay - mSubObj.MOByDay);
				MOCount = TempCount.toString();

				DayMark = Integer.toString(mSubObj.ChargeMark);
				WeekMark = Integer.toString(mSubObj.WeekMark);
				DayCode = Integer.toString(mSubObj.CodeByDay);

				Integer TempAnserCount = 0;

				if (mSubObj.AnswerBT != null && !mSubObj.AnswerBT.equalsIgnoreCase("")) TempAnserCount++;
				if (mSubObj.AnswerGB != null && !mSubObj.AnswerGB.equalsIgnoreCase("")) TempAnserCount++;

				if (mSubObj.AnswerKQ != null && !mSubObj.AnswerKQ.equalsIgnoreCase("")) TempAnserCount++;

				if (mSubObj.AnswerTS != null && !mSubObj.AnswerTS.equalsIgnoreCase("")) TempAnserCount++;

				if (mSubObj.AnswerTV != null && !mSubObj.AnswerTV.equalsIgnoreCase("")) TempAnserCount++;

				AnswerCount = TempAnserCount.toString();

				if (!mSubObj.AnswerTS.isEmpty())
				{
					String[] Arr = mSubObj.AnswerTS.split("-");
					if (Arr.length == 2)
					{
						X = Arr[0];
						Y = Arr[1];
					}
				}
			}
			CurrentHour = MyConfig.Get_DateFormat_VNTimeShort().format(Calendar.getInstance().getTime());
			CurrentDate = MyConfig.Get_DateFormat_VNShort().format(Calendar.getInstance().getTime());

			MT = MT.replace("[CurrentDate]",CurrentDate);
			MT = MT.replace("[CurrentHour]", CurrentHour);
			
			MT = MT.replace("[DayMark]", DayMark);
			MT = MT.replace("[WeekMark]", WeekMark);
			MT = MT.replace("[DayCode]", DayCode);
			MT = MT.replace("[Match]", Match_);
			MT = MT.replace("[PlayDate]", PlayDate);
			MT = MT.replace("[PlayHour]", PlayHour);
			MT = MT.replace("[Team1]", Team1);
			MT = MT.replace("[Team2]", Team2);
			MT = MT.replace("[BeginHour]", BeginHour);
			MT = MT.replace("[BeginDate]", BeginDate);
			MT = MT.replace("[EndHour]", EndHour);
			MT = MT.replace("[MOCount]", MOCount);

			if (!mSubObj.IsNull() && mMTType == MTType.AnswerBT)
			{
				Value = mSubObj.AnswerBT;
			}
			else if (!mSubObj.IsNull() && mMTType == MTType.AnswerTS)
			{
				Value = mSubObj.AnswerTS;
			}
			else if (!mSubObj.IsNull() && mMTType == MTType.AnswerTV)
			{
				Value = mSubObj.AnswerTV;
			}

			MT = MT.replace("[Value]", Value);

			MT = MT.replace("[X]", X);
			MT = MT.replace("[Y]", Y);
			MT = MT.replace("[AnswerCount]", AnswerCount);
			MT = MT.replace("[Match1]", Match1);
			MT = MT.replace("[PlayHour1]", PlayHour1);
			MT = MT.replace("[PlayDate1]", PlayDate1);
			MT = MT.replace("[BeginHour1]", BeginHour1);
			MT = MT.replace("[EndHour1]", EndHour1);
			MT = MT.replace("[BeginDate1]", BeginDate1);
			MT = MT.replace("[Match2]", Match2);
			MT = MT.replace("[PlayDate2]", PlayDate2);
			MT = MT.replace("[BeginHour2]", BeginHour2);
			MT = MT.replace("[BeginDate2]", BeginDate2);
			MT = MT.replace("[EndHour2]", EndHour2);

			return MT;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}
	
	public static MatchObject GetCurrentMatch(Date SendDate)
	{
		MatchObject mObject = new MatchObject();
		try
		{
			Match mMatch = new Match(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable = mMatch.Select(2, Match.Status.Active.GetValue().toString());

			if (mTable.GetRowCount() > 0)
			{
				MatchObject TempObject = new MatchObject();

				TempObject = MatchObject.Convert(mTable);

				Calendar mCal_SendDate = Calendar.getInstance();
				Calendar mCal_Begin = Calendar.getInstance();
				Calendar mCal_End = Calendar.getInstance();
				mCal_SendDate.setTime(SendDate);
				mCal_Begin.setTime(TempObject.BeginDate);
				mCal_End.setTime(TempObject.EndDate);

				if (mCal_SendDate.after(mCal_Begin) && mCal_SendDate.before(mCal_End))
					mObject = TempObject;
			}
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mObject;
	}

	/**
	 * Lấy 1 trận đấu tiếp theo
	 * 
	 * @return
	 */
	public static MatchObject GetNextMatch()
	{
		MatchObject mObject = new MatchObject();
		try
		{
			Match mMatch = new Match(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable = mMatch.Select(2, Match.Status.Next.GetValue().toString());
			if (mTable.GetRowCount() > 0)
				mObject = MatchObject.Convert(mTable);

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mObject;
	}

	public static MatchObject GetNext1Match()
	{
		MatchObject mObject = new MatchObject();
		try
		{
			Match mMatch = new Match(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable = mMatch.Select(2, Match.Status.Next1.GetValue().toString());
			if (mTable.GetRowCount() > 0)
				mObject = MatchObject.Convert(mTable);

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mObject;
	}

	/**
	 * Lấy trận đấu vừa kết thúc để xứ lý cho việc tính điểm
	 * 
	 * @return
	 */
	public static MatchObject GetComputeMatch()
	{
		MatchObject mObject = new MatchObject();
		try
		{
			Match mMatch = new Match(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable = mMatch.Select(3, Match.Status.Compute.GetValue().toString());
			if (mTable.GetRowCount() > 0)
				mObject = MatchObject.Convert(mTable);

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mObject;
	}

	/**
	 * Lấy trận đấu đang diễn ra
	 * 
	 * @return
	 */
	public static MatchObject GetTodayMatch()
	{
		MatchObject mObject = new MatchObject();
		try
		{
			Match mMatch = new Match(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable = mMatch.Select(2, Match.Status.Active.GetValue().toString());

			if (mTable.GetRowCount() > 0)
			{
				mObject = MatchObject.Convert(mTable);
			}
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mObject;
	}

	public static MatchObject GetMatchByID(Integer MatchID)
	{
		MatchObject mObject = new MatchObject();
		try
		{
			Match mMatch = new Match(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTable = mMatch.Select(1, MatchID.toString());

			if (mTable.GetRowCount() > 0)
			{
				mObject = MatchObject.Convert(mTable);
			}
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return mObject;
	}

	public static MyConfig.ChannelType GetChannelType(String Channel)
	{
		try
		{
			return MyConfig.ChannelType.valueOf(Channel);
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return MyConfig.ChannelType.NOTHING;
	}

	/**
	 * Lấy
	 * 
	 * @param AppName
	 * @return
	 */
	public static VNPApplication GetApplication(String AppName)
	{

		try
		{
			return VNPApplication.valueOf(AppName.toUpperCase());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return VNPApplication.NoThing;
	}

	/**
	 * Lấy giá trị 1 node trong chỗi XML
	 * 
	 * @param XML
	 * @param NodeName
	 * @return
	 * @throws Exception
	 */
	public static String GetValueNode(String XML, String NodeName) throws Exception
	{

		try
		{
			String Value = "";
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(XML));

			Document doc = db.parse(is);
			NodeList nodes = doc.getElementsByTagName(NodeName);

			Element line = (Element) nodes.item(0);

			Node child = line.getFirstChild();

			if (child instanceof CharacterData)
			{
				CharacterData cd = (CharacterData) child;
				Value = cd.getData();
			}
			return Value;
		}
		catch (Exception ex)
		{
			throw ex;
		}

	}


	private static Vector<NewsObject> mList_Two_News = new Vector<NewsObject>();
	
	
	/**
	 * Lấy 2 tin tức mới nhất
	 * @return
	 * @throws Exception
	 */
	public static synchronized Vector<NewsObject> Get_List_Two_News() throws Exception
	{
		Calendar mCal_Current = Calendar.getInstance();
		
		if (mList_Two_News != null && mList_Two_News.size() > 0)
		{
			Calendar mCal_PushTime = Calendar.getInstance();

			mCal_PushTime.setTime(mList_Two_News.get(0).PushTime);
			if (mCal_PushTime.get(Calendar.YEAR) == mCal_Current.get(Calendar.YEAR)
					&& mCal_PushTime.get(Calendar.MONTH) == mCal_Current.get(Calendar.MONTH)
					&& mCal_PushTime.get(Calendar.DATE) == mCal_Current.get(Calendar.DATE)) return mList_Two_News;
		}

		if (mList_Two_News != null) mList_Two_News.clear();

		News mNews = new News(LocalConfig.mDBConfig_MSSQL);

		Calendar mCal_Begin = Calendar.getInstance();
		Calendar mCal_End = Calendar.getInstance();

		mCal_Begin.set(Calendar.MILLISECOND, 0);
		mCal_End.set(Calendar.MILLISECOND, 0);
		
		mCal_Begin.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH),
				mCal_Current.get(Calendar.DATE), 0, 0, 1);

		MyTableModel mTable = mNews.Select(3, News.NewsType.Reminder.GetValue().toString(), MyConfig
				.Get_DateFormat_InsertDB().format(mCal_Begin.getTime()),
				MyConfig.Get_DateFormat_InsertDB().format(mCal_End.getTime()));

		NewsObject mObject = NewsObject.Convert(mTable);
		if (!mObject.IsNull()) mList_Two_News.add(mObject);

		mTable = mNews.Select(3, News.NewsType.Push.GetValue().toString(),
				MyConfig.Get_DateFormat_InsertDB().format(mCal_Begin.getTime()), MyConfig.Get_DateFormat_InsertDB()
						.format(mCal_End.getTime()));

		NewsObject mObject_2 = NewsObject.Convert(mTable);
		if (!mObject.IsNull()) mList_Two_News.add(mObject_2);

		return mList_Two_News;
	}
}
