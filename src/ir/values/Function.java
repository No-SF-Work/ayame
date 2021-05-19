package ir.values;

import ir.Type;
import ir.values.Value;
import jdk.jfr.Unsigned;

import java.util.ArrayList;

/**
 * Function类
 * */
public class Function extends User {
    //参数声明，不含值
    public class Arg extends Value{
        //todo
        public Arg(Type type) {
            super(type);
        }
        private int rank;//排第几,非负
    }

    public Function(Type type) {
        super(type);
    }


    private ArrayList<BasicBlock>basicBlocks=new ArrayList<>();//func 不存基本块的话也没什么存在的必要了，所以直接new了
    private ArrayList<Arg>argList;//有序参数列表
}
