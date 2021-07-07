package pass;

import ir.MyModule;
import ir.values.Function;

public interface Pass {

  //Pass的名字，用来控制是否运行
  public String getName();

  public abstract interface MAPass extends Pass {

    public void run(/*MaModule ma*/);
  }

  public abstract interface IRPass extends Pass {

    public void run(MyModule m);
  }


}
