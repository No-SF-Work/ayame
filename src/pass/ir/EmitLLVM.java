package pass.ir;

import ir.MyModule;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.text.Style;
import pass.Pass.IRPass;
import util.Mylogger;

public class EmitLLVM implements IRPass {


  public EmitLLVM() {
    sb = new StringBuilder();
  }

  StringBuilder sb;
  private int vnc = 0;//value name counter
  Logger log = Mylogger.getLogger(EmitLLVM.class);

  private String newName() {
    var v = String.valueOf(vnc);
    vnc++;
    return v;
  }

  @Override
  public String getName() {
    return "emitllvm";
  }

  @Override
  public void run(MyModule m) {
    nameVariable(m);
    m.__globalVariables.forEach(gb -> {
      sb.append(gb).append("\n");
    });
    m.__functions.forEach(func -> {
      var val = func.getVal();
      if (!val.isBuiltin_()) {
        sb.append("define dso_local ")
            .append(val)
            .append("{\n");
        val.getList_().forEach(
            bbNode -> {
              var bbval = bbNode.getVal();
              if (!val.getList_().getEntry().equals(bbNode)) {
                sb.append(bbval.getName());

                sb.append(":");
                sb.append("       ; precessor_:[");
                bbval.getPredecessor_().forEach(b -> {
                  sb.append(b.getName()).append(",");
                });
                sb.deleteCharAt(sb.length() - 1);
                sb.append("]\n");
              }
              bbval.getList().forEach(
                  instNode -> {
                    var instVal = instNode.getVal();
                    sb.append(instVal.toString()).append("\n");
                  }
              );
            }
        );
        sb.append("}\n");
      }
    });
    try {
      FileWriter fw = new FileWriter(new File("out.ll"));
      System.out.println(sb);
      fw.append(sb);
      fw.close();
      log.info("successfully export out.ll, system exit");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void nameVariable(MyModule m) {
    m.__globalVariables.forEach(gv -> {
      gv.setName("@" + gv.getName());
    });
    m.__functions.forEach(
        f -> {
          vnc = 0;
          var func = f.getVal();
          if (!func.isBuiltin_()) {
            func.getArgList().forEach(arg -> {
              arg.setName("%" + newName());
            });
            func.getList_().forEach(bbInode -> {
              // if (!bbInode.equals(func.getList_().getEntry())) {
              bbInode.getVal().setName(newName());
              //}
              bbInode.getVal().getList().forEach(
                  instNode -> {
                    if (instNode.getVal().needname) {
                      instNode.getVal().setName("%" + newName());
                    }
                  }
              );
            });
          }
        }
    );
  }

  private void printIR() {

  }

}
