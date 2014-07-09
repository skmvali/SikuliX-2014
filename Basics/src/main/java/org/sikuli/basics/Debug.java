/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.basics;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

/**
 * Debug is a utility class that wraps println statements and allows more or less command line
 * output to be turned on.<br> <br> For debug messages only ( Debug.log() ):<br> Use system
 * property: sikuli.Debug to set the debug level (default = 1)<br> On the command line, use
 * -Dsikuli.Debug=n to set it to level n<br> -Dsikuli.Debug will disable any debug messages <br>
 * (which is equivalent to using Settings.Debuglogs = false)<br> <br> It prints if the level
 * number is less than or equal to the currently set DEBUG_LEVEL.<br> <br> For messages
 * ActionLogs, InfoLogs see Settings<br> <br> You might send all messages generated by this
 * class to a file:<br>-Dsikuli.Logfile=pathname (no path given: SikuliLog.txt in working
 * folder)<br> This can be restricted to Debug.user only (others go to System.out):<br>
 * -Dsikuli.LogfileUser=pathname (no path given: UserLog.txt in working folder)<br>
 *
 * This solution is NOT threadsafe !!!
 */
public class Debug {

  private static final int DEFAULT_LEVEL = 1;
  private static int DEBUG_LEVEL = DEFAULT_LEVEL;
  private long _beginTime = 0;
  private String _message;
  private String _title = null;
  private static PrintStream printout = null;
  private static PrintStream printoutuser = null;
  private static final DateFormat df =
          DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
  public static String logfile;

  static {
    String debug = System.getProperty("sikuli.Debug");
    if (debug != null && "".equals(debug)) {
      DEBUG_LEVEL = 0;
      Settings.DebugLogs = false;
    } else {
      try {
        DEBUG_LEVEL = Integer.parseInt(debug);
        if (DEBUG_LEVEL > 0) {
          Settings.DebugLogs = true;
        } else {
          Settings.DebugLogs = false;
        }
      } catch (NumberFormatException numberFormatException) {
      }
    }
    setLogFile(null);
    setUserLogFile(null);
  }

	/**
	 * specify, where the logs should be written:<br>
	 * null - use from property sikuli.Logfile
	 * empty - use SikuliLog.txt in working folder
	 * not empty - use given filename
	 * @param fileName null, empty or absolute filename
	 * @return success
	 */
	public static boolean setLogFile(String fileName) {
    if (fileName == null) {
      fileName = System.getProperty("sikuli.Logfile");
    }
    if (fileName != null) {
      if ("".equals(fileName)) {
        if (Settings.isMacApp) {
          fileName = "SikuliLog.txt";
        } else {
          fileName = FileManager.slashify(System.getProperty("user.dir"), true) + "SikuliLog.txt";
        }
      }
      try {
        logfile = fileName;
        if (printout != null) {
          printout.close();
        }
        printout = new PrintStream(fileName);
        log(3, "Debug: setLogFile: " + fileName);
        return true;
      } catch (Exception ex) {
        System.out.printf("[Error] Logfile %s not accessible - check given path", fileName);
        System.out.println();
        return false;
      }
    }
    return false;
  }

	/**
	 * does Sikuli log go to a file?
	 * @return true if yes, false otherwise
	 */
	public static boolean isLogToFile() {
    return (printout != null);
  }

	/**
	 * specify, where the user logs (Debug.user) should be written:<br>
	 * null - use from property sikuli.LogfileUser
	 * empty - use UserLog.txt in working folder
	 * not empty - use given filename
	 * @param fileName null, empty or absolute filename
	 * @return success
	 */
	public static boolean setUserLogFile(String fileName) {
    if (fileName == null) {
      fileName = System.getProperty("sikuli.LogfileUser");
    }
    if (fileName != null) {
      if ("".equals(fileName)) {
        if (Settings.isMacApp) {
          fileName = "UserLog.txt";
        } else {
          fileName = FileManager.slashify(System.getProperty("user.dir"), true) + "UserLog.txt";
        }
      }
      try {
        if (printoutuser != null) {
          printoutuser.close();
        }
        printoutuser = new PrintStream(fileName);
        log(3, "Debug: setLogFile: " + fileName);
        return true;
      } catch (FileNotFoundException ex) {
        System.out.printf("[Error] User logfile %s not accessible - check given path", fileName);
        System.out.println();
        return false;
      }
    }
    return false;
  }

	/**
	 * does user log go to a file?
	 * @return true if yes, false otherwise
	 */
  public static boolean isUserLogToFile() {
    return (printoutuser != null);
  }

  /**
   *
   * @return current debug level
   */
  public static int getDebugLevel() {
    return DEBUG_LEVEL;
  }

  /**
   * set debug level to default level
   *
   * @return default level
   */
  public static int setDebugLevel() {
    setDebugLevel(DEFAULT_LEVEL);
    if (DEBUG_LEVEL > 0) {
      Settings.DebugLogs = true;
    } else {
      Settings.DebugLogs = false;
    }
    return DEBUG_LEVEL;
  }

  /**
   * set debug level to given value
   *
   * @param level value
   */
  public static void setDebugLevel(int level) {
    DEBUG_LEVEL = level;
    if (DEBUG_LEVEL > 0) {
      Settings.DebugLogs = true;
    } else {
      Settings.DebugLogs = false;
    }
  }

  /**
   * set debug level to given number value as string (ignored if invalid)
   *
   * @param level valid number string
   */
  public static void setDebugLevel(String level) {
    try {
      DEBUG_LEVEL = Integer.parseInt(level);
      if (DEBUG_LEVEL > 0) {
        Settings.DebugLogs = true;
      } else {
        Settings.DebugLogs = false;
      }
    } catch (NumberFormatException e) {
    }
  }

  /**
   * Sikuli messages from actions like click, ...<br> switch on/off: Settings.ActionLogs
   *
   * @param message String or format string (String.format)
   * @param args to use with format string
   */
  public static void action(String message, Object... args) {
    if (Settings.ActionLogs) {
      log(-1, "log", message, args);
    }
  }

  /**
   * use Debug.action() instead
   * @param message String or format string (String.format)
   * @param args to use with format string
   * @deprecated
   */
  @Deprecated
  public static void history(String message, Object... args) {
    action(message, args);
  }

  /**
   * informative Sikuli messages <br> switch on/off: Settings.InfoLogs
   *
   * @param message String or format string (String.format)
   * @param args to use with format string
   */
  public static void info(String message, Object... args) {
    if (Settings.InfoLogs) {
      log(-1, "info", message, args);
    }
  }

  /**
   * Sikuli error messages<br> switch on/off: always on
   *
   * @param message String or format string (String.format)
   * @param args to use with format string
   */
  public static void error(String message, Object... args) {
    log(-1, "error", message, args);
  }

  /**
   * Sikuli debug messages with default level<br> switch on/off: Settings.DebugLogs (off) and/or
   * -Dsikuli.Debug
   *
   * @param message String or format string (String.format)
   * @param args to use with format string
   */
  public static void log(String message, Object... args) {
    log(DEFAULT_LEVEL, message, args);
  }

  /**
   * messages given by the user<br> switch on/off: Settings.UserLogs<br> depending on
   * Settings.UserLogTime, the prefix contains a timestamp <br> the user prefix (default "user")
   * can be set: Settings,UserLogPrefix
   *
   * @param message String or format string (String.format)
   * @param args to use with format string
   */
  public static void user(String message, Object... args) {
    if (Settings.UserLogs) {
      if (Settings.UserLogTime) {
//TODO replace the hack -99 to filter user logs
        log(-99, String.format("%s (%s)",
                Settings.UserLogPrefix, df.format(new Date())), message, args);
      } else {
        log(-99, String.format("%s", Settings.UserLogPrefix), message, args);
      }
    }
  }

  /**
   * Sikuli debug messages with level<br> switch on/off: Settings.DebugLogs (off) and/or
   * -Dsikuli.Debug
   *
   * @param level value
   * @param message String or format string (String.format)
   * @param args to use with format string
   */
  public static void log(int level, String message, Object... args) {
    if (Settings.DebugLogs) {
      log(level, "debug", message, args);
    }
  }

	/**
	 * INTERNAL USE: special debug messages
	 * @param level value
	 * @param prefix not used
	 * @param message text or format string
	 * @param args for use with format string
	 */
	public static void logx(int level, String prefix, String message, Object... args) {
    if (level == -1) {
      log(level, "error", message, args);
    } else if (level == -2) {
      log(level, "action", message, args);
    } else {
      log(level, "debug", message, args);
    }
  }

  private static synchronized void log(int level, String prefix, String message, Object... args) {
//TODO replace the hack -99 to filter user logs
    String sout;
    String stime = "";
    if (isEnabled(level)) {
      if (Settings.LogTime && level != -99) {
        stime = String.format(" (%s)", df.format(new Date()));
      }
      if (args.length != 0) {
        sout = String.format("[" + prefix + stime + "] " + message, args);
      } else {
        sout = "[" + prefix + stime + "] " + message;
      }
      if (level == -99 && printoutuser != null) {
        printoutuser.print(sout);
        printoutuser.println();
      } else if (printout != null) {
        printout.print(sout);
        printout.println();
      } else {
        System.out.print(sout);
        System.out.println();
      }
    }
  }

  private static boolean isEnabled(int level) {
    return level <= DEBUG_LEVEL;
  }

  private static boolean isEnabled() {
    return isEnabled(DEFAULT_LEVEL);
  }

  /**
   * Sikuli profiling messages<br> switch on/off: Settings.ProfileLogs, default off
   *
   * @param message String or format string
   * @param args to use with format string
   */
  public static void profile(String message, Object... args) {
    if (Settings.ProfileLogs) {
      log(-1, "profile", message, args);
    }
  }

	/**
	 * profile convenience: entering a method
   * @param message String or format string
   * @param args to use with format string
	 */
	public static void enter(String message, Object... args) {
    profile("entering: " + message, args);
  }

	/**
	 * profile convenience: exiting a method
   * @param message String or format string
   * @param args to use with format string
	 */
	public static void exit(String message, Object... args) {
    profile("exiting: " + message, args);
  }

	/**
	 * start timer
	 * <br>log output depends on Settings.ProfileLogs
	 * @return timer
	 */
	public static Debug startTimer() {
    return startTimer("");
  }

	/**
	 * start timer with a message
	 * <br>log output depends on Settings.ProfileLogs
   * @param message String or format string
   * @param args to use with format string
	 * @return timer
	 */
  public static Debug startTimer(String message, Object... args) {
    Debug timer = new Debug();
    timer.startTiming(message, args);
    return timer;
  }

  /**
   * stop timer and print timer message
 	 * <br>log output depends on Settings.ProfileLogs
  *
   * @return the time in msec
   */
  public long end() {
    if (_title == null) {
      return endTiming(_message, false, new Object[0]);
    } else {
      return endTiming(_title, false, new Object[0]);
    }
  }

  /**
   * lap timer and print message with timer message
	 * <br>log output depends on Settings.ProfileLogs
   *
	 * @param message String or format string
   * @return the time in msec
   */
  public long lap(String message) {
    if (_title == null) {
      return endTiming("(" + message + ") " + _message, true, new Object[0]);
    } else {
      return endTiming("(" + message + ") " + _title, true, new Object[0]);
    }
  }

	private void startTiming(String message, Object... args) {
    int pos;
    if ((pos = message.indexOf("\t")) < 0) {
      _title = null;
      _message = message;
    } else {
      _title = message.substring(0, pos);
      _message = message.replace("\t", " ");
    }
    if (!"".equals(_message)) {
      profile("TStart: " + _message, args);
    }
    _beginTime = (new Date()).getTime();
  }

  private long endTiming(String message, boolean isLap, Object... args) {
    if (_beginTime == 0) {
      profile("TError: timer not started (%s)", message);
      return -1;
    }
    long t = (new Date()).getTime();
    long dt = t - _beginTime;
    if (!isLap) {
      _beginTime = 0;
    }
    if (!"".equals(message)) {
      profile(String.format((isLap ? "TLap:" : "TEnd") +
              " (%.3f sec): ", (float) dt / 1000) + message, args);
    }
    return dt;
  }
}
