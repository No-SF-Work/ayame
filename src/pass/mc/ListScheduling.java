package pass.mc;

import backend.CodeGenManager;
import backend.machinecodes.*;
import backend.reg.*;
import pass.Pass;

import java.util.*;
import java.util.stream.Collectors;

public class ListScheduling implements Pass.MCPass {
    @Override
    public String getName() {
        return "ListScheduling";
    }

    enum A72FUType {
        Branch, Integer, Multiple, Load, Store, FP
    }

    private static class A72Unit {
        A72FUType type;
        Node curNode;
        int completeCycles;

        A72Unit(A72FUType type) {
            this.type = type;
            this.curNode = null;
            this.completeCycles = 0;
        }

        public void runTask(Node curNode, int completeCycles, Map<A72FUType, Integer> freeUnits) {
            this.curNode = curNode;
            this.completeCycles = completeCycles;
            freeUnits.compute(type, (key, value) -> value - 1);
        }

        public void freeTask(Map<A72FUType, Integer> freeUnits) {
            curNode = null;
            freeUnits.compute(type, (key, value) -> value + 1);
        }
    }

    private static class Node implements Comparable<Node> {
        private final MachineCode instr;
        private final int latency;
        private final ArrayList<A72FUType> needFU = new ArrayList<>();
        private final HashSet<Node> outSet = new HashSet<>();
        private final HashSet<Node> inSet = new HashSet<>();
        private int outDegree;
        private int inDegree;
        private int criticalLatency;

        public Node(MachineCode instr) {
            this.instr = instr;
            this.criticalLatency = 0;
            this.latency = switch (instr.getTag()) {
                case Add, Sub, Rsb, And, Or -> instr.getShift().getType() == ArmAddition.ShiftType.None ? 1 : 2;
                case Mul -> 3;
                case Div -> 8;
                // binary
                case Compare -> 1;
                case LongMul -> 3;
                case FMA -> 4;
                case Mv -> instr.getCond() == ArmAddition.CondType.Any ? 1 : 2; // fixme movw & movt
                case Branch, Jump, Return -> 1;
                case Load -> 4;
                case Store -> 3;
                case Call -> 1;
                case Global -> 1;
                default -> throw new IllegalStateException("Unexpected value: " + instr.getTag());
            };
            this.needFU.add(switch (instr.getTag()) {
                case Add, Sub, Rsb, And, Or, Mv ->
                        instr.getShift().getType() == ArmAddition.ShiftType.None ? A72FUType.Integer : A72FUType.Multiple;
                case Mul, Div -> A72FUType.Multiple;
                // binary
                case Compare -> A72FUType.Integer;
                case LongMul, FMA -> A72FUType.Multiple;
                case Branch, Jump, Return, Call -> A72FUType.Branch;
                case Load -> A72FUType.Load;
                case Store -> A72FUType.Store;
                case Global -> A72FUType.Integer;
                default -> throw new IllegalStateException("Unexpected value: " + instr.getTag());
            });
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

    private ArrayList<Node> buildConflictGraph(MachineBlock block) {
        var nodes = new ArrayList<Node>();
        
        var readRegNodes = new HashMap<MachineOperand, ArrayList<Node>>();
        var writeRegNodes = new HashMap<MachineOperand, Node>();
        var loadNodes = new ArrayList<Node>();
        Node sideEffectNode = null;
        
        for (var instrEntry : block.getmclist()) {
            var instr = instrEntry.getVal();
            if (instr instanceof MCComment) {
                continue;
            }

            var defs = instr.getMCDef();
            var uses = instr.getMCUse();
            assert defs.stream().allMatch(def -> def instanceof PhyReg);
            assert uses.stream().allMatch(use -> use instanceof PhyReg);
            var curNode = new Node(instr);
            nodes.add(curNode);

            defs.stream().filter(readRegNodes::containsKey)
                    .flatMap(defReg -> readRegNodes.get(defReg).stream())
                    .forEach(readNode -> readNode.addEdge(curNode));

            defs.stream().filter(writeRegNodes::containsKey)
                    .map(writeRegNodes::get)
                    .forEach(writeNode -> writeNode.addEdge(curNode));

            uses.stream().filter(writeRegNodes::containsKey)
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

            if (instr instanceof MCBranch || instr instanceof MCJump || instr instanceof MCReturn) {
                nodes.stream().filter(node -> node != curNode).forEach(node -> node.addEdge(curNode));
            }
        }
        
        return nodes;
    }

    private void calculateCriticalLatency(ArrayList<Node> nodes) {
        var toVisit = new LinkedList<Node>();
        nodes.forEach(node -> node.outDegree = node.outSet.size());
        nodes.stream().filter(n -> n.outDegree != 0).forEach(n -> {
            toVisit.add(n);
            n.criticalLatency = n.latency;
        });

        while (!toVisit.isEmpty()) {
            var n = toVisit.pollLast();
            n.inSet.forEach(inNode -> {
                inNode.criticalLatency = Math.max(inNode.criticalLatency, inNode.latency + n.criticalLatency);
                --inNode.outDegree;
                if (inNode.outDegree == 0) {
                    toVisit.add(inNode);
                }
            });
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

        var freeUnits = units.stream()
                .collect(Collectors.toMap(unit -> unit.type, unit -> 1, Integer::sum));

        block.getmclist().clear();

        nodes.forEach(node -> node.inDegree = node.inSet.size());
        var readyNodeList = nodes.stream()
                .filter(n -> n.inDegree == 0)
                .collect(Collectors.toCollection(PriorityQueue::new));

        int cntInflight = 0;
        int cycle = 0;
        while (!readyNodeList.isEmpty() || cntInflight > 0) {
            // Simulate Frontend Firing
            for (var iter = readyNodeList.iterator(); iter.hasNext();) {
                var curNode = iter.next();

                var needUnitMap = curNode.needFU.stream()
                        .collect(Collectors.toMap(type -> type, type -> 1, Integer::sum));
                boolean canFire = needUnitMap.entrySet().stream()
                        .allMatch(entry -> freeUnits.get(entry.getKey()) >= entry.getValue());
                if (!canFire) continue;

                for (var needUnit: curNode.needFU) {
                    for (var unit: units) {
                        if (unit.type.equals(needUnit) && unit.curNode == null) {
                            block.addAtEndMC(curNode.instr.getNode());
                            unit.runTask(curNode, cycle + curNode.latency, freeUnits);
                            iter.remove();
                            ++cntInflight;
                            break;
                        }
                    }
                }
            }

            ++cycle;

            // Simulate Backend Execution
            for (A72Unit unit : units) {
                if (unit.curNode != null && unit.completeCycles == cycle) {
                    unit.curNode.outSet.forEach(outNode -> --outNode.inDegree);
                    unit.curNode.outSet.stream().filter(outNode -> outNode.inDegree == 0).forEach(readyNodeList::add);
                    unit.freeTask(freeUnits);
                    --cntInflight;
                }
            }
        }
    }

    public void run(CodeGenManager manager) {
        for (var func : manager.getMachineFunctions()) {
            for (var blockEntry : func.getmbList()) {
                var block = blockEntry.getVal();
                var nodes = buildConflictGraph(block);
                calculateCriticalLatency(nodes);
                scheduling(block, nodes);
            }
        }
    }
}
