package pass;

import ir.MyModule;
import ir.values.Function;
import backend.CodeGenManager;

public interface Pass {

  //Pass的名字，用来控制是否运行
  public String getName();

  public abstract interface MCPass extends Pass {

    public void run(CodeGenManager codeGenManager);
  }

  public abstract interface IRPass extends Pass {

    public void run(MyModule m);
  }


}
