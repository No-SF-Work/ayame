package driver;

import java.io.FileDescriptor;
import java.io.FileOutputStream;

public class Debugger {

  public static Debugger debugger = new Debugger();

  private final StringBuilder sb;
  private boolean debug = false;

  public static Debugger getInstance() {
    return debugger;
  }

  public void dbg(String msg) {
    if (this.debug) {
      System.out.println(msg);
      sb.append(msg).append("\n");
    }
  }

  public void loadConfig(Config config) {
    this.debug = config.isDebugMode;
  }

  public void writeLog() {
  }

  private Debugger() {
    this.sb = new StringBuilder();
  }
}
