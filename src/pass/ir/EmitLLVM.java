package pass.ir;

import ir.MyModule;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction.TAG_;
import pass.Pass.IRPass;
import util.Mylogger;

import java.io.FileWriter;
import java.util.logging.Logger;

public class EmitLLVM implements IRPass {


  public EmitLLVM(String outputName) {
    this.outputName = outputName;
    sb = new StringBuilder();
  }

  public EmitLLVM() {
    sb = new StringBuilder();
  }

  String outputName = "out.ll";
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
            .append("{");

        if (val.getStoreGVSet().size() != 0) {
          sb.append("\t; store gv: ");
          val.getStoreGVSet().forEach(
              globalVariable -> {
                sb.append(globalVariable.getName()).append(", ");
              }
          );
          sb.deleteCharAt(sb.length() - 1);
          sb.deleteCharAt(sb.length() - 1);
        }
        if (val.getLoadGVSet().size() != 0) {
          sb.append("\t; load gv: ");
          val.getLoadGVSet().forEach(
              globalVariable -> {
                sb.append(globalVariable.getName()).append(", ");
              }
          );
          sb.deleteCharAt(sb.length() - 1);
          sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("\n");

        val.getList_().forEach(
            bbNode -> {
              var bbval = bbNode.getVal();
              if (!val.getList_().getEntry().equals(bbNode)) {
                sb.append(bbval.getName());
                sb.append(":");
                sb.append("       ; predecessor_:[");
                bbval.getPredecessor_().forEach(b -> {
                  sb.append(b.getName()).append(",");
                });
                sb.deleteCharAt(sb.length() - 1);
                sb.append("]  ");
                sb.append("successor:[");
                bbval.getSuccessor_().forEach(b -> {
                  sb.append(b.getName()).append(",");
                });
                sb.deleteCharAt(sb.length() - 1);
                sb.append("]");

                // === start print loop info ===
                if (val.getLoopInfo().isLoopHeader(bbval)) {
                  sb.append(", is LOOP HEADER");
                  if (val.getLoopInfo().getLoopForBB(bbval).isSimpleForLoop()) {
                    sb.append(" (simple for loop)");
                  } else if (val.getLoopInfo().getLoopForBB(bbval).isCanonical()) {
                    sb.append(" (canonical loop)");
                  }
                }
                sb.append(", loop depth: ");
                sb.append(val.getLoopInfo().getLoopDepthForBB(bbval));
                // === end print loop info ===

                sb.append("\n");
              }
              bbval.getList().forEach(
                  instNode -> {
                    if (instNode.getVal().tag.equals(TAG_.Mod)) {
                      BinaryInst inst = (BinaryInst) instNode.getVal();
                      sb.append(inst.secondName).append(" = sdiv i32 ")
                          .append(inst.getOperands().get(0).getName()).append(",")
                          .append(inst.getOperands().get(1).getName()).append(" ;-----------")
                          .append("\n");
                      sb.append(inst.thirdName).append(" = mul i32 ")
                          .append(inst.secondName).append(",")
                          .append(inst.getOperands().get(1).getName()).append("   ;MOD \n");
                      sb.append(inst.getName()).append(" = sub i32 ")
                          .append(inst.getOperands().get(0).getName()).append(",")
                          .append(inst.thirdName).append(" ").append(";----------- \n");
                    } else {
                      var instVal = instNode.getVal();
                      sb.append(instVal.toString());

                      // === start print loop info ===
                      if (val.getLoopInfo().getLoopDepthForBB(bbval) != 0) {
                        var loop = val.getLoopInfo().getLoopForBB(bbval);
                        if (loop.getIndVar() == instVal) {
                          sb.append(" ; is indvar");
                        } else if (loop.getStepInst() == instVal) {
                          sb.append(" ; is step inst");
                        } else if (loop.getLatchCmpInst() == instVal) {
                          sb.append(" ; is latchCmpInst");
                        }
                      }
                      // === end print loop info ===
                      sb.append("             ;user ->  ");
                      instVal.getUsesList().forEach(use->{
                        sb.append(use.getUser().getName()+" ");
                      });
                      sb.append("\n");
                    }
                  }
              );
            }
        );
        sb.append("}\n");
      } else {
        sb.append("declare ")
            .append(val)
            .append("\n");
      }
    });
    try {
      FileWriter fw = new FileWriter(outputName);
//      System.out.println(sb);
      fw.append(sb);
      fw.close();
      log.info("successfully export out.ll");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void nameVariable(MyModule m) {
    m.__globalVariables.forEach(gv -> {
      if (!gv.getName().startsWith("@")) {
        gv.setName("@" + gv.getName());
      }
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
                    if (instNode.getVal().tag.equals(TAG_.Mod)) {
                      ((BinaryInst) instNode.getVal()).secondName = "%" + newName();
                      ((BinaryInst) instNode.getVal()).thirdName = "%" + newName();
                      instNode.getVal().setName("%" + newName());
                    } else if (instNode.getVal().needname) {
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
