package jgrapes.test.statistics;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgrapes.JGrapesException;

public class CloudMonitorNode extends Thread {

  public static final String CLOUD_VIEW_KEY = "view";

  protected Connection conn;
  protected PrintStream out;
  protected int logtime;
  private String mysql_table;

  private volatile boolean terminated = false;

  public CloudMonitorNode(String chConf, PrintStream out, int logtime)
    throws JGrapesException, SQLException
  {
    String mysql_host;
    int mysql_port;
    String mysql_user;
    String mysql_pass;
    String mysql_db;

    this.logtime = logtime * 1000;

    Pattern p;
    Matcher m;

    p = Pattern.compile("mysql_host=([^,]+)");
    m = p.matcher(chConf);
    if (!m.find()) throw new JGrapesException("MonitorMode: cannot parse mysql_host cloud conf");
    mysql_host = m.group(1);

    p = Pattern.compile("mysql_user=([^,]+)");
    m = p.matcher(chConf);
    if (!m.find()) throw new JGrapesException("MonitorMode: cannot parse mysql_user cloud conf");
    mysql_user = m.group(1);

    p = Pattern.compile("mysql_pass=([^,]*)");
    m = p.matcher(chConf);
    if (!m.find()) throw new JGrapesException("MonitorMode: cannot parse mysql_pass cloud conf");
    mysql_pass = m.group(1);

    p = Pattern.compile("mysql_db=([^,]+)");
    m = p.matcher(chConf);
    if (!m.find()) throw new JGrapesException("MonitorMode: cannot parse mysql_pass cloud conf");
    mysql_db = m.group(1);

    p = Pattern.compile("mysql_table=([^,]+)");
    m = p.matcher(chConf);
    if (!m.find()) throw new JGrapesException("MonitorMode: cannot parse mysql_table cloud conf");
    mysql_table = m.group(1);

    try{
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new JGrapesException("Error: MySQL JDBC driver not found!");
    }
    String url =  String.format("jdbc:mysql://%s/%s", mysql_host, mysql_db);

    conn = DriverManager.getConnection(url, mysql_user, mysql_pass);

    this.out = out;
  }

  public synchronized void terminate() {
    terminated = true;
  }

  protected void print(String line) {
    if (this.out != null) {
      this.out.println(line);
      this.out.flush();
    }
  }

  public void run() {
    try {
      Statement stmt = conn.createStatement();

      print("# Initializing cloud...");
      stmt.executeUpdate("UPDATE " + mysql_table +" SET cloud_value='', cloud_timestamp=0, counter=0 WHERE cloud_key='view'");

      long lastCounter = -1;
      while (!terminated) {
        ResultSet rs = stmt.executeQuery("SELECT cloud_timestamp, counter FROM " + mysql_table +" WHERE cloud_key='view'");

        if (rs.next()) {
          long timestamp = rs.getLong("cloud_timestamp") * 1000;
          long counter = rs.getLong("counter");
          if (lastCounter != counter) {
            if (lastCounter < 0) lastCounter = 0;
            print(String.format("@ view time=%d timestamp=%d counter=%d (+%d)",
                                (System.currentTimeMillis()/1000),
                                timestamp,
                                counter, (counter-lastCounter)));
            lastCounter = counter;
          }
        } else {
          print("@ noview");
        }

        try {
          Thread.sleep(logtime);
        } catch (InterruptedException e) {}
      }

      out.close();
    } catch (SQLException e) {
      print("@ exception: " + e.getMessage());
      e.printStackTrace();
    }
  }
}