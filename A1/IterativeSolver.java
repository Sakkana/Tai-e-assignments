/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.solver;

import pascal.taie.analysis.dataflow.analysis.DataflowAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;

class IterativeSolver<Node, Fact> extends Solver<Node, Fact> {

    public IterativeSolver(DataflowAnalysis<Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        throw new UnsupportedOperationException();
    }

    /**
     * 迭代求解
     * @param cfg
     * @param result
     */
    @Override
    protected void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // 初始状态：未抵达不动点
        while (true) {
            boolean transferOccured = false;

            // 遍历 cfg 上所有结点
            for (Node node: cfg) {
                if (cfg.isExit(node)) {
                    // exit 结点不用分析，因为是个虚拟结点，没有 IN
                    continue;
                }

                Fact currentNodeOutFact = result.getOutFact(node);
                Fact currentNodeInFact = result.getInFact(node);

                // 将所有后继结点的 IN 合并到当前结点的 OUT
                for (Node successorNode: cfg.getSuccsOf(node)) {
                    // OUT[BB] = U 所有 IN[后继者]
                    // meetInto:       fact -> 合并进入 target
                    //    successor node IN -> 合并进入 current node OUT
                    Fact successorNodeInFact = result.getInFact(successorNode);
                    // System.out.println("successor: " + successorNode);

                    // 刚开始的第二个 nop 的 successor infact 是空怎么办？ -> 初始化错了 exit
                    assert successorNodeInFact != null: "Successor should not be NULLLLLLLLLL!";
                    analysis.meetInto(successorNodeInFact, currentNodeOutFact);
                }

                // 计算当前结点的 IN
                boolean transferResult = analysis.transferNode(node, currentNodeInFact, currentNodeOutFact);

                // 检查该结点是否收敛
                if (transferResult) {
                    // 如果有变化，继续迭代
                    transferOccured = true;
                }
            }

            if (!transferOccured) {
                break;
            }
        }
    }
}
