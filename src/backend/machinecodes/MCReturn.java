package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.CodeGenManager;
import backend.reg.PhyReg;
import backend.reg.Reg;

import java.util.ArrayList;

import static backend.CodeGenManager.canEncodeImm;

public class MCReturn extends MachineCode{

    @Override
    public String toString(){
        String res="";
        int stackSize=getMb().getMF().getStackSize();
        MachineFunction mf=getMb().getMF();
        if(stackSize>0){
            String op = canEncodeImm(-stackSize) ? "sub" : "add";
            MachineOperand v1 = canEncodeImm(-stackSize) ? new MachineOperand(-stackSize) : new MachineOperand(stackSize);
            if(canEncodeImm(stackSize)||canEncodeImm(-stackSize)){
                res+="\t"+op;
                res+="\tsp, sp, "+v1.getName()+"\n";
            }else{
                MCMove mv=new MCMove();
                mv.setRhs(v1);
                mv.setDst(mf.getPhyReg("r5"));
                res+=mv.toString();
                res+=op+"\tsp,\tsp,\t"+mf.getPhyReg(5).getName()+"\n";
            }
        }
        StringBuilder sb = new StringBuilder();
        mf.getUsedSavedRegs().forEach(phyReg -> {
            sb.append(phyReg.getName());
            sb.append(", ");
        });
        if (mf.isUsedLr()) {
            res += "\tpop\t{";
            if (!mf.getUsedSavedRegs().isEmpty()) {
                res += sb.toString();
            }
            res += "pc}\n";
        } else {
            if (!mf.getUsedSavedRegs().isEmpty()) {
                //删去多余','
                sb.deleteCharAt(sb.length() - 1);
                res += "\tpush\t{";
                res += sb.toString();
                res += "}\n";
            }
        }
        res+="\tbx\tlr\n";
        return res;
    }

    static ArrayList<Reg> use=new ArrayList<>();
    static{
        use.add(new PhyReg(0));
    }

    @Override
    public ArrayList<Reg> getUse(){
        return use;
    }

    public MCReturn(MachineBlock mb){
        super(TAG.Return ,mb);
    }
}