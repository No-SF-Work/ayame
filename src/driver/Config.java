package driver;

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

  public boolean isDebugMode = false;
  public boolean isIRMode = false;
  public boolean isOutPutMode = false;

  public void setConfig(Namespace res) {
    isIRMode = res.get("ir");
    isDebugMode = res.get("debug");
    isOutPutMode = res.get("output");
  }

  private Config() {
  }
}
