package util;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/***
 * 我们的Compiler不涉及到多线程，用Log4j太过臃肿，自己封装了个小的
 */
public class Mylogger {

  private static FileHandler fh;

  private Mylogger() {
    try {
      fh = new FileHandler("record.log");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void init() {
    try {
      fh = new FileHandler("record.log");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void loadLogConfig(boolean isDebugMode) {
    if (isDebugMode) {
      fh.setLevel(Level.ALL);
    } else {
      fh.setLevel(Level.OFF);
    }
    fh.setFormatter(new Formatter() {
      @Override
      public String format(LogRecord logRecord) {
        return "[" + logRecord.getLevel() + "]" + logRecord.getLoggerName() + " " + logRecord
            .getMessage()
            + "\n";
      }
    });
  }


  public static Logger getLogger(Class c) {
    Logger tmp = Logger.getLogger(c.getName());
    tmp.addHandler(fh);
    tmp.setLevel(Level.OFF);
    return tmp;
  }

}
