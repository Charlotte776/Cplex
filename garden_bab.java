package usecplex;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class garden {
	IloCplex cplex;
	IloNumVar[] x;
	static int nVar = 20;// 变量数量
	static double[] res = new double[nVar];// 储存结果
	static double nowIbest = Double.MAX_VALUE;// 目前最优的整数解

	public static void main(String[] args) throws IloException, FileNotFoundException {
		long startTime = System.currentTimeMillis(); // 获取开始时间
		garden q = new garden();
		q.model1();// 初始模型
		double b = q.solve(res);// 解取整数
		q.dfs(b, res, 0);
		System.out.println(Double.MIN_VALUE);
		System.out.println("Result:obj=" + nowIbest);
		for (int i = 0; i < nVar; i++) {
			System.out.println("x" + (i + 1) + "=" + res[i]);
		}
		long endTime = System.currentTimeMillis(); // 获取结束时间

		System.out.println("程序运行时间：" + (endTime - startTime) + "ms"); // 输出程序运行时间
	}

	// 初始模型
	public void model1() throws FileNotFoundException {
		try {
			cplex = new IloCplex(); // creat a model
			cplex.setOut(null);
			// 会自动覆盖之前的内容
			//cplex.setOut(new PrintStream(new FileOutputStream("result.txt")));
			// System.setOut(new PrintStream(new BufferedOutputStream(new
			// FileOutputStream("debug.txt"))));
			// System.setErr(new PrintStream(new BufferedOutputStream(new
			// FileOutputStream("error.txt"))));
			double[] lb = new double[nVar];
			double[] ub = new double[nVar];
			for (int t = 0; t < nVar; t++) {
				lb[t] = 0;
				ub[t] = Integer.MAX_VALUE;
			}
			x = cplex.numVarArray(nVar, lb, ub);// （变量个数，下界，上界）

			double[] objvals = { 6, 6.2, 7.2, 10, 10.7, 12, 8, 8.5, 9.6, 10, 10.7, 12, 3, 3.1, 5, 5.4, 4, 4.3, 5, 5.4 };
			cplex.addMinimize(cplex.scalProd(x, objvals));

			double[][] coeff = { { 0.04, 0.17, 0.06, 0.12 }, { 0.05, 0.14, 0, 0.14 }, { 0.06, 0.13, 0.05, 0.1 },
					{ 0.05, 0.21, 0.02, 0.1 }, { 0.03, 0.15, 0.04, 0.15 } };
			double[] cons = { 500, 400, 600, 550, 500 };
			// Total Hours Available per Month
			IloLinearNumExpr linear1 = cplex.linearNumExpr();
			for (int j = 0; j < 5; j++) {
				for (int t = 0; t < 4; t++) {
					int q = t * 3;
					int p = 12 + t * 2;
					if (j < 2) {
						linear1.addTerm(coeff[j][t], x[q]);
					} else {
						linear1.addTerm(coeff[j][t], x[p]);
					}
				}
				cplex.addLe(linear1, cons[j]);
				linear1.clear();
			}
			// number of step1=number of step2
			for (int i = 0, j = 0; i < 10; i += 3, j += 1) {
				cplex.addEq(cplex.sum(x[i], x[i + 1], x[i + 2]), cplex.sum(x[i + 12 - j], x[i + 13 - j]));
			}
			// >=contract
			double[] cons2 = { 1800, 1400, 1600, 1800 };
			for (int i = 0; i < 7; i += 2) {
				cplex.addGe(cplex.sum(x[12 + i], x[13 + i]), cons2[i / 2]);
			}
			// iron
			double[] coe3 = { 1.2, 1.6, 2.1, 2.4 };
			for (int i = 0, j = 0; i < 12; i++) {
				if ((i - 2) % 3 == 0) {
					j++;
				} else
					linear1.addTerm(x[i], coe3[j]);
			}
			cplex.addLe(linear1, 10000);
			linear1.clear();
			// over time
			for (int j = 0; j < 5; j++) {
				for (int t = 0; t < 4; t++) {
					int q = t * 3;
					int p = 12 + t * 2;
					if (j < 2) {
						linear1.addTerm(coeff[j][t], x[q + 1]);
					} else {
						linear1.addTerm(coeff[j][t], x[p + 1]);
					}
				}
				cplex.addLe(linear1, 100);
				linear1.clear();
			}
			cplex.exportModel("model.lp");

		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
		}
	}

	// 得到目前的最优解
	public double solve(double[] res2) {
		try {
			if (cplex.solve()) {
				for (int i = 0; i < nVar; i++) {
					res2[i] = cplex.getValues(x)[i];
					System.out.println("x" + (i + 1) + "=" + res2[i]);
				}
				System.out.println("obj=" + cplex.getObjValue() + "\n");
				return cplex.getObjValue();
			}
			return Double.MIN_VALUE;// 无可行解时返回
		} catch (IloException e) {
			System.err.println("Exception e: " + e);
			return Double.MIN_VALUE;
		}
	}

	// 加上条件:哪个整数，取什么方向（0左，1右）
	public IloConstraint addCons(int xb, double num, int dir) throws IloException {
		IloConstraint exCons = null;
		if (dir == 0) {// 向下取整
			exCons = cplex.addLe(x[xb], (int) num);
		} else {// 向上取整
			exCons = cplex.addGe(x[xb], (int) num + 1);
		}
		return exCons;
	}

	// 减去条件:
	public void delCons(IloConstraint exCons) throws IloException {
		cplex.remove(exCons);
	}

	// 判断是不是均为小数,int直接是去掉小数部分
	public int isInt(double[] temp) {
		for (int i = 0; i < temp.length - 1; i++) {
			if (temp[i] >= 0) {
				if ((temp[i] - (int) temp[i] < 1e-9) || ((int) temp[i] + 1 - temp[i]) < 1e-9)
					continue;
				else
					return i;
			} else {
				if (temp[i] - (int) temp[i] + 1 < 1e-9 || -temp[i] + (int) temp[i] < 1e-9)
					continue;
				return i;
			}
		}
		return -1;
	}

	// 目前式子算出来的向下取整的z*,储存解的数组，层级。
	public void dfs(double z, double[] store, int level) throws IloException {
		// 剪枝，1)边界值>=Z*; 3）最优解是整数 2)不含可行解；顺序不能换
		int isint = isInt(store);
		System.out.println("\n\nlevel=" + level + " z=" + z + " nowIbest=" + nowIbest + " isint=" + isint);
		if (z < 1e-9) {
			System.out.println("z-(int)z<1e-9\n");
			return;
		} else if (z >= nowIbest) {
			System.out.println("z>=nowIbest\n");
			return;
		} else if (isint == -1 && z < nowIbest) {
			System.out.println("isint==-1&&z<nowIbest\n");
			nowIbest = z;
			for (int i = 0; i < nVar; i++) {
				res[i] = store[i];
			}
			System.out.println("*" + nowIbest + "*");
			return;
		} else if (level >= nVar) {
			System.out.println("level>=nVar\n");
			return;
		} else {
			// 分支，先遍历左子树，再遍历右子树
			double[] leftVar = new double[nVar];
			double[] rightVar = new double[nVar];
			IloConstraint addleftCons = addCons(isint, store[isint], 0);
			System.out.println("left");
			double leftres = solve(leftVar);
			cplex.exportModel(level + ".1.lp");
			delCons(addleftCons);
			IloConstraint addrightcons = addCons(isint, store[isint], 1);
			System.out.println("right");
			double rightres = solve(rightVar);
			cplex.exportModel(level + ".2.lp");
			delCons(addrightcons);
			// 贪心 ,每一层都要走遍左右两个节点
			if (leftres <= rightres) {
				// 先左后右
				addleftCons = addCons(isint, store[isint], 0);
				dfs(leftres, leftVar, level + 1);
				delCons(addleftCons);
				addrightcons = addCons(isint, store[isint], 1);
				dfs(rightres, rightVar, level + 1);
				delCons(addrightcons);
			} else {
				// 先右后左
				addrightcons = addCons(isint, store[isint], 1);
				dfs(rightres, rightVar, level + 1);
				delCons(addrightcons);
				addleftCons = addCons(isint, store[isint], 0);
				dfs(leftres, leftVar, level + 1);
				delCons(addleftCons);
			}

		}
	}
}
