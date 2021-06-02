package driver;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.sourceforge.argparse4j.inf.Namespace;

/***
 * This is the config file of the compiler
 * options' meaning are self-explanatory
 ***/
public class Config {

  private static Config config = new Config();

  public static Config getInstance() {

    return config;
  }

  private static Logger logger = Logger.getLogger("SysY Logger");
  public boolean isDebugMode = false;
  public boolean isIRMode = false;
  public boolean isOutPutMode = false;

  public static Logger getLogger() {
    return logger;
  }

  public void setConfig(Namespace res) throws Exception {
    isIRMode = res.get("ir");
    isDebugMode = res.get("debug");
    isOutPutMode = res.get("output");
    //only severe level msg will be recorded in console if not in debug mode
    ConsoleHandler ch = new ConsoleHandler();
    FileHandler fh = new FileHandler("record.log");
    if (isDebugMode) {
      logger.setLevel(Level.ALL);
      ch.setLevel(Level.SEVERE);
      fh.setLevel(Level.ALL);
      fh.setFormatter(new Formatter() {
        @Override
        public String format(LogRecord logRecord) {
          return "[" + logRecord.getLevel() + "]" + logRecord.getClass() + logRecord.getMessage()
              + "\n";
        }
      });
      logger.addHandler(ch);
      logger.addHandler(fh);
    } else {
      logger.setLevel(Level.WARNING);
    }
    ch.setFormatter(new Formatter() {
      @Override
      public String format(LogRecord logRecord) {
        return "[" + logRecord.getLevel() + "]" + logRecord.getClass() + logRecord.getMessage()
            + "\n";
      }
    });
  }

  private Config() {
  }
}
