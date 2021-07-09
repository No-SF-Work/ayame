package backend;

import backend.machinecodes.ArmAddition;
import backend.machinecodes.MCBranch;
import backend.machinecodes.MCCall;
import backend.machinecodes.MCComment;
import backend.machinecodes.MCJump;
import backend.machinecodes.MCLoad;
import backend.machinecodes.MCStore;
import backend.machinecodes.MachineCode;
import backend.reg.PhyReg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class ListScheduling {
    enum A72FUType {
        Branch, Integer, Multiple, Load, Store, FP
    }

    private class A72Unit {
        A72FUType type;
        Node curNode;
        int completeCycles;

        A72Unit(A72FUType type) {
            this.type = type;
            this.curNode = null;
            this.completeCycles = 0;
        }
    }

    private int getInstrLatency(MachineCode code) {
        return switch (code.getTag()) {
            case Add, Sub, Rsb, And, Or -> code.getShift().getType() == ArmAddition.ShiftType.None ? 1 : 2;
            case Mul -> 3;
            case Div -> 8;
            // binary
            case Compare -> 1;
            case LongMul -> 3;
            case FMA -> 4;
            case Mv -> code.getCond() == ArmAddition.CondType.Any ? 1 : 2; // fixme movw & movt
            case Branch, Jump, Return -> 1;
            case Load -> 4;
            case Store -> 3;
            case Call -> 1;
            case Global -> 1;
            default -> throw new IllegalStateException("Unexpected value: " + code.getTag());
        };
    }

    private A72FUType getInstrFUType(MachineCode code) {
        return switch (code.getTag()) {
            case Add, Sub, Rsb, And, Or, Mv -> code.getShift().getType() == ArmAddition.ShiftType.None ? A72FUType.Integer : A72FUType.Multiple;
            case Mul, Div -> A72FUType.Multiple;
            // binary
            case Compare -> A72FUType.Integer;
            case LongMul, FMA -> A72FUType.Multiple;
            case Branch, Jump, Return, Call -> A72FUType.Branch;
            case Load -> A72FUType.Load;
            case Store -> A72FUType.Store;
            case Global -> A72FUType.Integer;
            default -> throw new IllegalStateException("Unexpected value: " + code.getTag());
        };
    }

    private class Node implements Comparable {
        private final MachineCode instr;
        private final int latency;
        private final A72FUType FUType;
        private final ArrayList<Node> outSet;
        private final ArrayList<Node> inSet;
        private int outDegree;
        private int inDegree;
        private int criticalLatency;

        public Node(MachineCode instr) {
            this.instr = instr;
            this.criticalLatency = 0;
            this.latency = getInstrLatency(instr);
            this.FUType = getInstrFUType(instr);
            this.outSet = new ArrayList<>();
            this.inSet = new ArrayList<>();
        }

        public void addOutNode(Node node) {
            this.outSet.add(node);
        }

        public void addInNode(Node node) {
            this.inSet.add(node);
        }

        public int getOutDegree() {
            return outDegree;
        }

        public void setOutDegree() {
            this.outDegree = this.outSet.size();
        }

        public int getInDegree() {
            return inDegree;
        }

        public void setInDegree() {
            this.inDegree = this.inSet.size();
        }

        public int getLatency() {
            return latency;
        }

        public int getCriticalLatency() {
            return criticalLatency;
        }

        public void setCriticalLatency(int criticalLatency) {
            this.criticalLatency = criticalLatency;
        }

        public ArrayList<Node> getInSet() {
            return inSet;
        }

        public static void addEdge(Node from, Node to) {
            from.addOutNode(to);
            to.addInNode(from);
        }

        public A72FUType getFUType() {
            return FUType;
        }

        public MachineCode getInstr() {
            return instr;
        }

        @Override
        public int compareTo(Object o) {
            assert o instanceof Node;
            var rhs = (Node) o;
            return rhs.getCriticalLatency() == getCriticalLatency() ?
                    rhs.getLatency() - getLatency() :
                    rhs.getCriticalLatency() - getCriticalLatency();
        }
    }

    public void list_scheduling(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();

                var readRegNodes = new HashMap<PhyReg, ArrayList<Node>>();
                var writeRegNodes = new HashMap<PhyReg, Node>();
                Node sideEffectNode = null;
                var loadNodes = new ArrayList<Node>();
                var nodes = new ArrayList<Node>();

                for (var instrEntry : block.getmclist()) {
                    var instr = instrEntry.getVal();
                    if (instr instanceof MCComment) {
                        continue;
                    }

                    var defs = instr.getPhyDef();
                    var uses = instr.getPhyUses();
                    var node = new Node(instr);
                    nodes.add(node);

                    uses.stream().filter(writeRegNodes::containsKey)
                            .map(writeRegNodes::get)
                            .forEach(writeNode -> Node.addEdge(writeNode, node));

                    defs.stream().filter(readRegNodes::containsKey)
                            .flatMap(defReg -> readRegNodes.get(defReg).stream())
                            .forEach(readNode -> Node.addEdge(readNode, node));

                    defs.stream().filter(writeRegNodes::containsKey)
                            .map(writeRegNodes::get)
                            .forEach(writeNode -> Node.addEdge(writeNode, node));

                    uses.forEach(useReg -> {
                        if (!readRegNodes.containsKey(useReg)) {
                            readRegNodes.put(useReg, new ArrayList<>());
                        }
                        readRegNodes.get(useReg).add(node);
                    });

                    defs.forEach(defReg -> {
                        readRegNodes.put(defReg, new ArrayList<>());
                        writeRegNodes.put(defReg, node);
                    });

                    if (instr instanceof MCStore || instr instanceof MCCall) {
                        if (sideEffectNode != null) {
                            Node.addEdge(sideEffectNode, node);
                        }

                        loadNodes.forEach(loadNode -> Node.addEdge(loadNode, node));
                        loadNodes.clear();
                        sideEffectNode = node;
                    } else if (instr instanceof MCLoad) {
                        if (sideEffectNode != null) {
                            Node.addEdge(sideEffectNode, node);
                        }
                        loadNodes.add(node);
                    }

                    if (instr instanceof MCBranch || instr instanceof MCJump /* fixme: instr instanceof MCReturn */) {
                        nodes.stream().filter(n -> n != node).forEach(n -> Node.addEdge(n, node));
                    }
                }

                var visit = new LinkedList<Node>();
                nodes.forEach(Node::setOutDegree);
                nodes.stream().filter(n -> n.getOutDegree() != 0).forEach(n -> {
                    visit.add(n);
                    n.setCriticalLatency(n.getLatency());
                });

                while (!visit.isEmpty()) {
                    var n = visit.pollLast();
                    n.getInSet().forEach(t -> {
                        t.setCriticalLatency(Math.max(t.getCriticalLatency(), t.getLatency() + n.getCriticalLatency()));
                        --t.outDegree;
                        if (t.outDegree == 0) {
                            visit.add(t);
                        }
                    });
                }

                block.getmclist().clear();

                nodes.forEach(Node::setInDegree);
                var readyNodeList = nodes.stream()
                        .filter(n -> n.getInDegree() == 0)
                        .collect(Collectors.toCollection(PriorityQueue::new));
                var units = List.of(
                        new A72Unit(A72FUType.Branch),
                        new A72Unit(A72FUType.Integer),
                        new A72Unit(A72FUType.Integer),
                        new A72Unit(A72FUType.Multiple),
                        new A72Unit(A72FUType.Load),
                        new A72Unit(A72FUType.Store)
                );

                int cntInflight = 0;
                int cycle = 0;
                while (!readyNodeList.isEmpty() || cntInflight > 0) {
                    for (var iter = readyNodeList.iterator(); iter.hasNext();) {
                        var instr = iter.next();
                        var fuType = instr.getFUType();

                        for (var unit: units) {
                            if (unit.type == fuType && unit.curNode == null) {
                                block.addMC(instr.getInstr());
                                ++cntInflight;
                                unit.curNode = instr;
                                unit.completeCycles = cycle + instr.getLatency();
                                iter.remove();
                                break;
                            }
                        }
                    }

                    ++cycle;
                    for (A72Unit unit : units) {
                        if (unit.curNode != null && unit.completeCycles == cycle) {
                            unit.curNode.outSet.forEach(t -> --t.inDegree);
                            unit.curNode.outSet.stream().filter(t -> t.inDegree == 0).forEach(readyNodeList::add);
                            unit.curNode = null;
                            --cntInflight;
                        }
                    }
                }
            }
        }
    }
}
