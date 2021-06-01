package frontend;

import ir.values.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

public class Scope {

  private Stack<HashMap<String, Value>> symbols_;
  private ArrayList<HashMap<String, Value>> params;

  public void find(String name) {
  }

  public void addLayer() {

  }

  public void pop() {

  }

  public boolean isGlobal() {
    return this.symbols_.size() == 1;
  }

}
