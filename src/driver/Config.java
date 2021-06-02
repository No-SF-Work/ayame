package driver;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.sourceforge.argparse4j.inf.Namespace;
import util.Mylogger;

/***
 * This is the config file of the compiler
 * options' meaning are self-explanatory
 ***/
public class Config {

  private static Config config = new Config();

  public static Config getInstance() {

    return config;
  }

  public boolean isDebugMode = false;
  public boolean isIRMode = false;
  public boolean isOutPutMode = false;

  public void setConfig(Namespace res) throws Exception {
    isIRMode = res.get("ir");
    isDebugMode = res.get("debug");
    isOutPutMode = res.get("output");
    //only severe level msg will be recorded in console if not in debug mode
    ConsoleHandler ch = new ConsoleHandler();
    FileHandler fh = new FileHandler("record.log");
    isDebugMode = true;//todo 默认打开方便debug记得release后删除
    Mylogger.loadLogConfig(isDebugMode);

  }

  private Config() {
  }
}
