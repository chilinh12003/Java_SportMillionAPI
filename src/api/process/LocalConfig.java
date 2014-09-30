package api.process;

import java.util.Vector;

import dat.service.DefineMT;
import dat.service.DefineMTObject;
import db.define.DBConfig;

public class LocalConfig
{
	public static String LogConfigPath = "log4j.properties";
	public static String LogDataFolder = ".\\LogFile\\";

	public static String  DBConfigPath = "ProxoolConfig.xml";
	public static String  MySQLPoolName = "MySQL";
	public static String  MSSQLPoolName = "MSSQL";
	
	public static DBConfig mDBConfig_MSSQL = new DBConfig("MySQL");	
	public static DBConfig mDBConfig_MySQL = new DBConfig("MSSQL");
	

	public static String SHORT_CODE = "9696";
	// ----------------cau hinh Charging-----------------------------
	public static String VNPURLCharging = "";
	public static String VNPCPName = "MTRAFFIC";
	public static String VNPUserName = "mtraffic";
	public static String VNPPassword = "mtraffic#1235";

	public static Integer MAX_PID = 50;

	/**
	 * Số lượng MO cho phép trả lời trong 1 ngày
	 */
	public static Integer MaxAnswerByDay = 10;

	/**
	 * Số điểm quy ra 1 MDT
	 */
	public static Integer MarkPerCode = 50;
	public static Integer RegMark = 1000;

	// Các keyword dành để dự đoán
	public static String KeywordKQ = "KQ";
	public static String KeywordBT = "BT";
	public static String KeywordGB = "GB";
	public static String KeywordTS = "TS";
	public static String KeywordTV = "TV";

	public static Integer DELAY_SENT_MT = 0;
	
	public static Integer CHARGE_MAX_ERROR_RETRY = 1;
	
	public static Integer LONG_MESSAGE_CONTENT_TYPE = 21;
	
	public static String PackageName = "TRIEUPHUTT";
	public static String ServiceName = "Trieu Phu The Thao";
	

	private static Vector<DefineMTObject> mListDefineMT = new Vector<DefineMTObject>();

	public static Vector<DefineMTObject> GetListDefineMT() throws Exception
	{
		if (mListDefineMT == null || mListDefineMT.size() == 0)
		{
			DefineMT mDefineMT = new DefineMT(LocalConfig.mDBConfig_MSSQL);
			mListDefineMT = mDefineMT.GetAllMT();
		}
		return mListDefineMT;
	}

}
