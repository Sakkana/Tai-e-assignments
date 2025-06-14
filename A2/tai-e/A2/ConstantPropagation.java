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

package pascal.taie.analysis.dataflow.analysis.constprop;

import org.checkerframework.common.value.qual.BoolVal;
import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import java.lang.reflect.Field;

public class ConstantPropagation extends
        AbstractDataflowAnalysis<Stmt, CPFact> {

    public static final String ID = "constprop";

    public ConstantPropagation(AnalysisConfig config) {
        super(config);
    }

    /**
     * 常量传播是前向分析
     * @return
     */
    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public CPFact newBoundaryFact(CFG<Stmt> cfg) {
        // TODO - finish me
        CPFact cpFact = new CPFact();
        // 给所有参数变量赋值
        for (Var var: cfg.getIR().getParams()) {
            if (canHoldInt(var)) {
                cpFact.update(var, Value.getNAC());
            }
        }

        return cpFact;
    }

    @Override
    public CPFact newInitialFact() {
        // TODO - finish me
        return new CPFact();
    }

    @Override
    public void meetInto(CPFact fact, CPFact target) {
        // TODO - finish me
        System.out.println("before meet");
        System.out.println(fact);
        System.out.println(target);

        for (Var var: fact.keySet()) {
            Value v1 = fact.get(var);
            Value v2 = target.get(var);
            target.update(var, meetValue(v1, v2));
        }

        System.out.println("after meet");
        System.out.println(fact);
        System.out.println(target);
    }

    /**
     * Meets two Values.
     */
    public Value meetValue(Value v1, Value v2) {
        // TODO - finish me
        // 核心部分，两个 Fact 的交汇

        // 1. 一旦有一个是 NAC，整体就是 NAC
        if (v1.isNAC() || v2.isNAC()) {
            return Value.getNAC();
        }

        // 2. 忽略 udf
        if (v1.isUndef()) {
            return v2;
        } else if (v2.isUndef()) {
            return v1;
        }

        // 3. 常量
        if (v1.getConstant() ==  v2.getConstant()) {
            return Value.makeConstant(v1.getConstant());
        }

        // 4. 不相等 -> 变量
        return Value.getNAC();
    }

    @Override
    public boolean transferNode(Stmt stmt, CPFact in, CPFact out) {
        // TODO - finish me
        // 某个程序点，某条语句
        System.out.println("before transfer");
        System.out.println("in: " + in.toString());
        System.out.println("out: " + out.toString());

        // 左值是个确定的变量，右值是一个 list 的参数
        if (stmt.getDef().isPresent()) {
            // 存在左值
            if (stmt.getDef().get() instanceof Var && canHoldInt((Var) stmt.getDef().get())) {
                CPFact resultFact = new CPFact();
                // ⚠️ 不应该新建，而是要复制 in
                resultFact = in.copy();
                Exp expression = (Exp) ((DefinitionStmt<?, ?>)stmt).getRValue();
                resultFact.update((Var) stmt.getDef().get(), evaluate(expression, in));
                System.out.println("intermediate: " + resultFact);

                // 经过这条语句的计算，左值更新了并且和 OUT 不同
                if (!resultFact.equals(out)) {
                    // 这里遍历的是临时变量，遍历 out 的话永远是 {}
                    for (Var var: resultFact.keySet()) {
                        out.update(var, resultFact.get(var));
                    }
                }

                System.out.println("after transfer");
                System.out.println("in: " + in.toString());
                System.out.println("out: " + out.toString());

                return out != resultFact;
            }
        }

        // 不是赋值语句，直接拿 IN 更新 OUT
        if (!in.equals(out)) {
            for (Var var: in.keySet()) {
                out.update(var, in.get(var));
            }

            System.out.println("after transfer");
            System.out.println("in: " + in.toString());
            System.out.println("out: " + out.toString());
            return true;
        }

        System.out.println("after transfer");
        System.out.println("in: " + in.toString());
        System.out.println("out: " + out.toString());

        return false;
    }


    /**
     * @return true if the given variable can hold integer value, otherwise false.
     */
    public static boolean canHoldInt(Var var) {
        Type type = var.getType();
        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                case BOOLEAN:
                    return true;
            }
        }
        return false;
    }

    /**
     * Evaluates the {@link Value} of given expression.
     *
     * @param exp the expression to be evaluated
     * @param in  IN fact of the statement
     * @return the resulting {@link Value}
     */
    public static Value evaluate(Exp exp, CPFact in)  {
        // TODO - finish me

        // 如果表达式是个变量 -> 直接拿过来
        if (exp instanceof Var) {
            return in.get((Var) exp);
        }

        // 如果表达式是个常量 -> 构造常量
        if (exp instanceof IntLiteral) {
            int constantValue = ((IntLiteral) exp).getValue();
            return Value.makeConstant(constantValue);
        }

        // 如果表达式是二元表达式
        if (exp instanceof BinaryExp) {
            Value v1 = in.get(((BinaryExp) exp).getOperand1());
            Value v2 = in.get(((BinaryExp) exp).getOperand2());

            // 但凡有一个 NAC，怎么运算都是 NAC
            // 除非除法和取模除数为 0 是 UDF
            if (v1.isNAC() || v2.isNAC()) {
                if (exp instanceof ArithmeticExp) {
                   if (((ArithmeticExp) exp).getOperator() == ArithmeticExp.Op.DIV
                           && v2.isConstant()
                           && v2.getConstant() == 0) {
                       return Value.getUndef();
                   }
                   if (((ArithmeticExp) exp).getOperator() == ArithmeticExp.Op.REM
                           && v2.isConstant()
                           && v2.getConstant() == 0) {
                       return Value.getUndef();
                   }
               }

                return Value.getNAC();
            }

            // 如果两个都是常量，怎么运算都是常量
            // 除非除法除 0 是 UDF
            if (v1.isConstant() && v2.isConstant()) {
                if (exp instanceof ArithmeticExp) {
                    if (((ArithmeticExp) exp).getOperator() == ArithmeticExp.Op.DIV
                            && v2.getConstant() == 0) {
                        return Value.getUndef();
                    }

                    if (((ArithmeticExp) exp).getOperator() == ArithmeticExp.Op.REM
                            && v2.getConstant() == 0) {
                        return Value.getUndef();
                    }

                    return switch (((ArithmeticExp) exp).getOperator()) {
                        case ADD -> Value.makeConstant(v1.getConstant() + v2.getConstant());
                        case SUB -> Value.makeConstant(v1.getConstant() - v2.getConstant());
                        case MUL -> Value.makeConstant(v1.getConstant() * v2.getConstant());
                        case DIV -> Value.makeConstant(v1.getConstant() / v2.getConstant());
                        case REM -> Value.makeConstant(v1.getConstant() % v2.getConstant());
                    };
                }

                if (exp instanceof ConditionExp) {
                    return switch (((ConditionExp) exp).getOperator()) {
                        case EQ -> Value.makeConstant(v1.getConstant() == v2.getConstant() ? 1 : 0);
                        case NE -> Value.makeConstant(v1.getConstant() != v2.getConstant() ? 1 : 0);
                        case GE -> Value.makeConstant(v1.getConstant() >= v2.getConstant() ? 1 : 0);
                        case GT -> Value.makeConstant(v1.getConstant() > v2.getConstant() ? 1 : 0);
                        case LE -> Value.makeConstant(v1.getConstant() <= v2.getConstant() ? 1 : 0);
                        case LT -> Value.makeConstant(v1.getConstant() < v2.getConstant() ? 1 : 0);
                    };
                }

                if (exp instanceof ShiftExp) {
                    return switch (((ShiftExp) exp).getOperator()) {
                        case SHL -> Value.makeConstant(v1.getConstant() << v2.getConstant());
                        case SHR -> Value.makeConstant(v1.getConstant() >> v2.getConstant());
                        case USHR -> Value.makeConstant(v1.getConstant() >>> v2.getConstant());
                    };
                }

                if (exp instanceof BitwiseExp) {
                    return switch (((BitwiseExp) exp).getOperator()) {
                        case OR -> Value.makeConstant(v1.getConstant() | v2.getConstant());
                        case AND -> Value.makeConstant(v1.getConstant() & v2.getConstant());
                        case XOR -> Value.makeConstant(v1.getConstant() ^ v2.getConstant());
                    };
                }
            }

//            // 不存在 NAC，但是存在一个 UDF -> 结果就是 UDF
            if (v1.isUndef() || v2.isUndef()) {
                return Value.getUndef();
            }

            return Value.getNAC();

//            // 如果相等，必然都是 UDF
//
//            // 获取Class对象
//            Class<?> clazz = v1.getClass();
//
//            // 获取私有字段
//            try {
//                Field kindField = clazz.getDeclaredField("kind");
//                kindField.setAccessible(true);
//
//                // 获取字段值（枚举类型）
//                Object enumValue1 = kindField.get(v1);
//                Object enumValue2 = kindField.get(v2);
//
//                Enum<?> kindValue1 = (Enum<?>) enumValue1;
//                Enum<?> kindValue2 = (Enum<?>) enumValue2;
//
//                if (kindValue1 == kindValue2) {
//                    return Value.getUndef();
//                }
//
//                return Value.getUndef();
//
//            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//                e.printStackTrace();
//            }

        }

        return Value.getNAC();
    }
}
