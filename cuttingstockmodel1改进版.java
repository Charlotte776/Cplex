package usecplex;
/*
 115
[25, 40, 50, 55, 70]
[50, 36, 24, 8, 30]
115
[25, 40, 50, 55, 70]
[5, 6, 4, 8, 3]
 */

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import usecplex.cut.IloNumVarArray;

public class cutmodel1 {
	static double rollWidth = 115;// 木材长度
	static double[] size = { 25, 40, 50, 55, 70 };// 木材需求尺寸
	static double[] amount = {50, 36, 24, 8, 1000};// 需求量
	static int t_amount = 0;

	static IloCplex cplex;
	static IloIntVar[] y;
	static IloIntVar[] x;

	// 主函数
	public static void main(String[] args) throws IloException {
		// TODO 自动生成的方法存根
		cutmodel1 a = new cutmodel1();
		a.showData();
		a.model();
		a.solve();
		cplex.exportModel("cutmodel1.lp");
	}
	//函数功能：求解初始的目标变量中的变量个数amount
	public void submodel()  throws IloException {
		IloCplex cutSolver = new IloCplex();//子问题求解器
		IloObjective RollsUsed = cutSolver.addMinimize();
		IloRange[] Fill = new IloRange[amount.length];
		for (int f = 0; f < amount.length; f++) {
			Fill[f] = cutSolver.addRange(amount[f], Double.MAX_VALUE);
		}
		int nWdth = size.length;
		IloNumVarArray Cut = new IloNumVarArray();
		IloColumn col = cutSolver.column(RollsUsed, 1.0);
		for (int j = 0; j < nWdth; j++) {
			col=col.and(cutSolver.column(Fill[j], (int) (rollWidth / size[j])));
			Cut.add(cutSolver.numVar(col, 0.0, Double.MAX_VALUE));
			col=col.and(cutSolver.column(Fill[j], 0));
		}
		//end of generate model by column
		//选择控制器
		cutSolver.setOut(null);
        cutSolver.solve();
        cutSolver.exportModel("a.1.lp");
        for (int i = 0; i < amount.length; i++) {
			t_amount += cutSolver.getValue(Cut.getElement(i));
		}
	}
	// 建立模型
	public void model() throws IloException {
		submodel() ;
		try {
			// 目标变量
			cplex = new IloCplex();
			y = cplex.boolVarArray(t_amount);

			int[] objvals = new int[t_amount];
			for (int i = 0; i < objvals.length; i++) {
				objvals[i] = 1;
			}
			cplex.addMinimize(cplex.scalProd(y, objvals));
			x = cplex.intVarArray(t_amount * size.length, 0, Integer.MAX_VALUE);
			// 约束1
			IloLinearNumExpr lin = cplex.linearNumExpr();
			for (int i = 0; i < size.length; i++) {// 对所有的i
				for (int j = 0; j < t_amount; j++) {// 所有roll上相同种类木棒相加是否等于amount
					lin.addTerm(objvals[j], x[j + i * t_amount]);
				}
				cplex.addEq(lin, amount[i]);
				lin.clear();
			}
			// 约束2
			for (int j = 0; j < t_amount; j++) {
				for (int i = 0; i < size.length; i++) {
					lin.addTerm(size[i], x[j + i * t_amount]);
				}
				cplex.addLe(lin, cplex.prod(y[j], rollWidth));
				lin.clear();
			}

		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
		}
	}

	// 函数功能：求解，输出公式和结果
	public void solve() {
		try {
			if (cplex.solve()) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				System.out.println("obj=" + cplex.getObjValue() + "\n");
			}
		} catch (IloException e) {
			System.err.println("Exception e: " + e);
		}
	}

	// 函数功能：输出题目数据
	public void showData() {
		System.out.println("roll width=" + rollWidth);
		System.out.println("demand size:");
		for (int i = 0; i < size.length; i++) {
			System.out.print(size[i] + " ");
		}
		System.out.println();
		System.out.println("size amount:");
		for (int i = 0; i < amount.length; i++) {
			System.out.print(amount[i] + " ");
		}
		System.out.println();
	}

}
