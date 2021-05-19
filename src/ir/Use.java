package ir;

import ir.values.User;
import ir.values.Value;

import java.util.List;

/**
 * 用来记录Value之间的使用关系，一个Value使用了另一个Value，就有一条有向边连接他们
 */
public class Use {
    
    public Use(Value v, User u) {
    this.v=v;
    this.u=u;
    }
    private User u;
    private Value v;
}
