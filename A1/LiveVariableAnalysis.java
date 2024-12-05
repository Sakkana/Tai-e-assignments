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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of classic live variable analysis.
 */
public class LiveVariableAnalysis extends
        AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

    public static final String ID = "livevar";

    public LiveVariableAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public SetFact<Var> newBoundaryFact(CFG<Stmt> cfg) {
        return new SetFact<>();
    }

    @Override
    public SetFact<Var> newInitialFact() {
        return new SetFact<>();
    }

    /**
     *
     * @param fact
     * @param target
     *
     * 做交集操作，将 fact 集合并入 target 集合
     */
    @Override
    public void meetInto(SetFact<Var> fact, SetFact<Var> target) {
        target.union(fact);
    }

    /**
     * 活跃变量分析使用 backward analysis
     * 从 OUT 推断 IN
     * 1. OUT[B] = U IN[S]
     * 2. IN[B] = use_B U (OUT[B] - def_B)
     * @param stmt
     * @param in    IN[] 变量集合
     * @param out   OUT[] 变量集合
     * @return
     */
    @Override
    public boolean transferNode(Stmt stmt, SetFact<Var> in, SetFact<Var> out) {
        SetFact<Var> inTmp = new SetFact<>();
        inTmp.union(out);

        List<RValue> uses = stmt.getUses();
        Optional<LValue> defs = stmt.getDef();

        // 如果本 stmt 存在定义行为 -> (OUT[B] - def_B)
        if (defs.isPresent()) {
            // 获得被定义变量名
            LValue lv = defs.get();
            // 如果是 variable（而不是字段访问 x.a、数组访问等 x[i]）
            if (lv instanceof Var) {
                // 在 bit vector 删去这个变量所在的 bit
                inTmp.remove((Var) lv);
            }
        }

        // 并上 use -> use_B U (OUT[B] - def_B)
        for (RValue rv: uses) {
            if (rv instanceof Var) {
                inTmp.add((Var) rv);
            }
        }

        // 某个 BB 的
        if (inTmp.equals(in)) {
            return false;
        } else {
            in.set(inTmp);
            return true;
        }
    }
}
