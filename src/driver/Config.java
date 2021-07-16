package driver;

import java.io.IOException;
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



  private Config() {
  }
}
