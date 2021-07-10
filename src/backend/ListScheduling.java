package backend;

import backend.machinecodes.ArmAddition;
import backend.machinecodes.MCBranch;
import backend.machinecodes.MCCall;
import backend.machinecodes.MCComment;
import backend.machinecodes.MCJump;
import backend.machinecodes.MCLoad;
import backend.machinecodes.MCStore;
import backend.machinecodes.MachineBlock;
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

        public void runTask(Node curNode, int completeCycles) {
            this.curNode = curNode;
            this.completeCycles = completeCycles;
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
            case Add, Sub, Rsb, And, Or, Mv ->
                    code.getShift().getType() == ArmAddition.ShiftType.None ? A72FUType.Integer : A72FUType.Multiple;
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

    private class Node implements Comparable<Node> {
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

        public void addEdge(Node to) {
            this.outSet.add(to);
            to.inSet.add(this);
        }

        @Override
        public int compareTo(Node rhs) {
            return rhs.criticalLatency == this.criticalLatency ?
                    rhs.latency - this.latency :
                    rhs.criticalLatency - this.latency;
        }
    }

    public void list_scheduling(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var nodes = buildConflictGraph(block);
                calculateCriticalLatency(nodes);
                scheduling(block, nodes);
            }
        }
    }

    private void scheduling(MachineBlock block, ArrayList<Node> nodes) {
        var units = List.of(
                new A72Unit(A72FUType.Branch),
                new A72Unit(A72FUType.Integer),
                new A72Unit(A72FUType.Integer),
                new A72Unit(A72FUType.Multiple),
                new A72Unit(A72FUType.Load),
                new A72Unit(A72FUType.Store)
        );

        block.getmclist().clear();

        nodes.forEach(node -> node.inDegree = node.inSet.size());
        var readyNodeList = nodes.stream()
                .filter(n -> n.inDegree == 0)
                .collect(Collectors.toCollection(PriorityQueue::new));

        int cntInflight = 0;
        int cycle = 0;
        while (!readyNodeList.isEmpty() || cntInflight > 0) {
            for (var iter = readyNodeList.iterator(); iter.hasNext();) {
                var curNode = iter.next();

                for (var unit: units) {
                    if (unit.type == curNode.FUType && unit.curNode == null) {
                        block.addMC(curNode.instr);
                        ++cntInflight;
                        unit.runTask(curNode, cycle + curNode.latency);
                        iter.remove();
                        break;
                    }
                }
            }

            ++cycle;
            for (A72Unit unit : units) {
                if (unit.curNode != null && unit.completeCycles == cycle) {
                    unit.curNode.outSet.forEach(outNode -> --outNode.inDegree);
                    unit.curNode.outSet.stream().filter(outNode -> outNode.inDegree == 0).forEach(readyNodeList::add);
                    unit.curNode = null;
                    --cntInflight;
                }
            }
        }
    }

    private void calculateCriticalLatency(ArrayList<Node> nodes) {
        var visit = new LinkedList<Node>();
        nodes.forEach(node -> node.outDegree = node.outSet.size());
        nodes.stream().filter(n -> n.outDegree != 0).forEach(n -> {
            visit.add(n);
            n.criticalLatency = n.latency;
        });

        while (!visit.isEmpty()) {
            var n = visit.pollLast();
            n.inSet.forEach(t -> {
                t.criticalLatency = Math.max(t.criticalLatency, t.latency + n.criticalLatency);
                --t.outDegree;
                if (t.outDegree == 0) {
                    visit.add(t);
                }
            });
        }
    }

    private ArrayList<Node> buildConflictGraph(MachineBlock block) {
        var nodes = new ArrayList<Node>();
        
        var readRegNodes = new HashMap<PhyReg, ArrayList<Node>>();
        var writeRegNodes = new HashMap<PhyReg, Node>();
        var loadNodes = new ArrayList<Node>();
        Node sideEffectNode = null;
        
        for (var instrEntry : block.getmclist()) {
            var instr = instrEntry.getVal();
            if (instr instanceof MCComment) {
                continue;
            }

            var defs = instr.getPhyDef();
            var uses = instr.getPhyUses();
            var curNode = new Node(instr);
            nodes.add(curNode);

            uses.stream().filter(writeRegNodes::containsKey)
                    .map(writeRegNodes::get)
                    .forEach(writeNode -> writeNode.addEdge(curNode));

            defs.stream().filter(readRegNodes::containsKey)
                    .flatMap(defReg -> readRegNodes.get(defReg).stream())
                    .forEach(readNode -> readNode.addEdge(curNode));

            defs.stream().filter(writeRegNodes::containsKey)
                    .map(writeRegNodes::get)
                    .forEach(writeNode -> writeNode.addEdge(curNode));

            uses.forEach(useReg -> {
                if (!readRegNodes.containsKey(useReg)) {
                    readRegNodes.put(useReg, new ArrayList<>());
                }
                readRegNodes.get(useReg).add(curNode);
            });

            defs.forEach(defReg -> {
                readRegNodes.put(defReg, new ArrayList<>());
                writeRegNodes.put(defReg, curNode);
            });

            if (instr instanceof MCStore || instr instanceof MCCall) {
                if (sideEffectNode != null) {
                    sideEffectNode.addEdge(curNode);
                }

                loadNodes.forEach(loadNode -> loadNode.addEdge(curNode));
                loadNodes.clear();
                sideEffectNode = curNode;
            } else if (instr instanceof MCLoad) {
                if (sideEffectNode != null) {
                    sideEffectNode.addEdge(curNode);
                }
                loadNodes.add(curNode);
            }

            if (instr instanceof MCBranch || instr instanceof MCJump /* fixme: instr instanceof MCReturn */) {
                nodes.stream().filter(node -> node != curNode).forEach(node -> node.addEdge(curNode));
            }
        }
        
        return nodes;
    }
}
