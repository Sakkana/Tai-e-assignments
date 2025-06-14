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
import pascal.taie.analysis.graph.cfg.CFG;

import java.util.ArrayList;

class WorkListSolver<Node, Fact> extends Solver<Node, Fact> {

    WorkListSolver(DataflowAnalysis<Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // TODO - finish me
        ArrayList<Node> nodeArrayList = new ArrayList<>();
        for (Node node : cfg.getNodes()) {
            if (cfg.isEntry(node)) {
                continue;
            }
            nodeArrayList.add(node);
        }

        while (!nodeArrayList.isEmpty()) {
            // 取出来头结点
            Node currentNode = nodeArrayList.remove(0);
            System.out.println("==========");
            System.out.println("in stmt: " + currentNode);

            // 做前向分析，应该拿到所有前面结点
            // ⚠️ for (Node p: cfg.getSuccsOf(currentNode)) {
            for (Node p: cfg.getPredsOf(currentNode)) {
                // 两个 fact 是空的
                // analysis.meetInto(result.getOutFact(p), result.getInFact(p));
                // ⚠️ 前面结点的 OUT 汇聚到当前结点的 IN
                analysis.meetInto(result.getOutFact(p), result.getInFact(currentNode));
            }
            boolean changed = analysis.transferNode(
                    currentNode,
                    result.getInFact(currentNode),
                    result.getOutFact(currentNode)
            );

            if (changed) {
                nodeArrayList.addAll(cfg.getSuccsOf(currentNode));
            }
        }

    }

    @Override
    protected void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        throw new UnsupportedOperationException();
    }
}
