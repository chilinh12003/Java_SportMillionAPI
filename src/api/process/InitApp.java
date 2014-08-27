package api.process;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import db.define.DBConfig;

public class InitApp implements ServletContextListener
{

	public void contextInitialized(ServletContextEvent event)
	{
		try
		{
			ServletContext context = event.getServletContext();
			LocalConfig.LogDataFolder = context.getInitParameter("LogDataFolder");
			LocalConfig.VNPURLCharging = context.getInitParameter("VNPURLCharging");
			LocalConfig.VNPCPName = context.getInitParameter("VNPCPName");
			LocalConfig.VNPUserName = context.getInitParameter("VNPUserName");
			LocalConfig.VNPPassword = context.getInitParameter("VNPPassword");

			if (!context.getInitParameter("MySQLPoolName").equalsIgnoreCase(""))
			{
				LocalConfig.MySQLPoolName = context.getInitParameter("MySQLPoolName");
			}

			if (!context.getInitParameter("MSSQLPoolName").equalsIgnoreCase(""))
			{
				LocalConfig.MSSQLPoolName = context.getInitParameter("MSSQLPoolName");
			}
			
			
			if (!context.getInitParameter("DBConfigPath").equalsIgnoreCase(""))
			{
				LocalConfig.DBConfigPath = context.getInitParameter("DBConfigPath");

				LocalConfig.mDBConfig_MSSQL = new DBConfig(LocalConfig.DBConfigPath, LocalConfig.MSSQLPoolName);
				LocalConfig.mDBConfig_MySQL = new DBConfig(LocalConfig.DBConfigPath, LocalConfig.MySQLPoolName);
			}

			if (!context.getInitParameter("LogConfigPath").equalsIgnoreCase(""))
				LocalConfig.LogConfigPath = context.getInitParameter("LogConfigPath");
			
			LocalConfig.GetListDefineMT();
			
			String Temp = context.getInitParameter("DELAY_SENT_MT");
			if(!Temp.trim().equalsIgnoreCase(""))
				LocalConfig.DELAY_SENT_MT = Integer.parseInt(Temp);
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}
	}

	public void contextDestroyed(ServletContextEvent event)
	{
		ServletContext context = event.getServletContext();
		context.log("***ShowLifecycles - Destroyed the servlet context");
	}

}
