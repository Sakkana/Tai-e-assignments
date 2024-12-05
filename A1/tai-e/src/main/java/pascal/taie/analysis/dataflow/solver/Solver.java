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

/**
 * Base class for data-flow analysis solver, which provides common
 * functionalities for different solver implementations.
 *
 * @param <Node> type of CFG nodes
 * @param <Fact> type of data-flow facts
 */
public abstract class Solver<Node, Fact> {

    protected final DataflowAnalysis<Node, Fact> analysis;

    protected Solver(DataflowAnalysis<Node, Fact> analysis) {
        this.analysis = analysis;
    }

    /**
     * Static factory method to create a new solver for given analysis.
     */
    public static <Node, Fact> Solver<Node, Fact> makeSolver(
            DataflowAnalysis<Node, Fact> analysis) {
        return new IterativeSolver<>(analysis);
    }

    /**
     * Starts this solver on the given CFG.
     *
     * @param cfg control-flow graph where the analysis is performed on
     * @return the analysis result
     */
    public DataflowResult<Node, Fact> solve(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = initialize(cfg);
        doSolve(cfg, result);
        return result;
    }

    /**
     * Creates and initializes a new data-flow result for given CFG.
     *
     * @return the initialized data-flow result
     */
    private DataflowResult<Node, Fact> initialize(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = new DataflowResult<>();
        if (analysis.isForward()) {
            initializeForward(cfg, result);
        } else {
            initializeBackward(cfg, result);
        }
        return result;
    }

    protected void initializeForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        throw new UnsupportedOperationException();
    }

    protected void initializeBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // IN[exit] = 空集
        // backward 的 exit 是第一个的结点
        result.setInFact(cfg.getExit(), analysis.newBoundaryFact(cfg));

        // 循环迭代给所有 BB 赋值为空集
        // 从 cfg 取出来 node, 塞进 result 这个 map 做 node -> fact (bit vector) 的对应
        // result 里有两个 map，分别是 cfg 上每个 node 的 IN 向量和 OUT 向量
        for (Node node: cfg) {
            // IN[BB] = 空集
            if (cfg.isExit(node)) {
                continue;
            }

            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
        }
    }

    /**
     * Solves the data-flow problem for given CFG.
     */
    private void doSolve(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        if (analysis.isForward()) {
            doSolveForward(cfg, result);
        } else {
            doSolveBackward(cfg, result);
        }
    }

    protected abstract void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result);

    protected abstract void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result);
}
