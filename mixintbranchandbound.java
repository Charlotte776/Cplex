package usecplex;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
/*max 4x1-2x2+7x3-x4
 * 
 * x1       +5x3    <=10
 * x1+  x2  -x3     <=1
 *6x1  -5x2         <=0
 *-x1       +2x3-2x4<=3
 *     
 * xj >=0 (j=1,2,3,4)
 * xj are int(j=1,2,3)
 */

public class Intbab {
	IloCplex cplex;
	IloNumVar[] x;
	static int nVar=4;//变量数量
	static double[] res=new double[nVar];//储存结果
	static double nowIbest=Double.MIN_VALUE;//目前最优的整数解
	
	public static void main(String[] args) throws IloException {
		Intbab q= new Intbab();
		q.model1();//初始模型
		double b=q.solve(res);//解取整数 
		q.dfs(b,res,0);       
		System.out.println("+++++++++++++++++++++++++++++");
		System.out.println("Result:obj="+nowIbest);
		for(int i=0;i<nVar;i++) {
			System.out.println("x"+i+"="+res[i]);
		}
	}
	//初始模型
	public void model1() {
		try {
			 	cplex = new IloCplex(); // creat a model
			 	cplex.setOut(null);
		        double[] lb = {0,0,0,0};
		        double[] ub = {Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
		        x = cplex.numVarArray(nVar, lb, ub);//（变量个数，下界，上界）
		        
		        double[] objvals = {4, -2, 7, -1};
		        cplex.addMaximize(cplex.scalProd(x, objvals));
		        
		        double[][] coeff = {{1,0,5,0},{1,1,-1,0},{6,-5,0,0},{-1,0,2,-2}};
		        double[] cons= {10,1 ,0,3};
		        for(int i=0;i<4;i++) {
		        	cplex.addLe(cplex.scalProd(x, coeff[i]), cons[i]);
		        }        
       } catch (IloException e) {
           System.err.println("Concert exception caught: " + e);
       }
	}
	//得到目前的最优解
	public double solve(double[] res2) {
		try {
			if (cplex.solve()) {
				for (int i = 0; i < nVar; i++) {
					res2[i] = cplex.getValues(x)[i];
					System.out.println("x"+(i+1)+"="+res2[i]);
				}
				System.out.println("obj="+cplex.getObjValue()+"\n");
				return  cplex.getObjValue();
			}
			return Double.MIN_VALUE;//无可行解时返回
		} catch (IloException e) {
			System.err.println("Exception e: " + e);
			return Integer.MIN_VALUE;
		}
	}
	//加上条件:哪个整数，取什么方向（0左，1右）
	public IloConstraint addCons(int xb,double num,int dir) throws IloException {
		IloConstraint exCons=null;
		if(dir==0) {//向下取整
			exCons=cplex.addLe(x[xb],(int)num);
		}else {//向上取整
			exCons=cplex.addGe(x[xb], (int)num+1);
		}
		return exCons;
	}
	//减去条件:
	public void delCons(IloConstraint exCons) throws IloException {
		cplex.remove(exCons);
	}
	//判断是不是均为小数,int直接是去掉小数部分
	public int isInt(double[] temp) {
		for (int i = 0; i < temp.length-1; i++) {
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

	//目前式子算出来的向下取整的z*,储存解的数组，层级。
	public void dfs(double z,double[] store,int level) throws IloException {
		//剪枝，1)边界值<=Z*; 3）最优解是整数 2)不含可行解；顺序不能换
		int isint=isInt(store);
		System.out.println("\n\nlevel="+level+" z="+z+" nowIbest="+nowIbest+" isint="+isint);
		if(z<=nowIbest){
			System.out.println("z<=nowIbest\n");
			return;
		}else if(isint==-1&&z>nowIbest) {
			System.out.println("isint==-1&&z>nowIbest\n");
			nowIbest=z;
			for(int i=0;i<nVar;i++) {
				res[i]=store[i];
			}
			System.out.println("*"+nowIbest+"*");
			return;
		}else if(level>=nVar-1) {
			System.out.println("level>=nVar\n");
			return;
		}else {
		//分支，先遍历左子树，再遍历右子树
			double[] leftVar=new double[nVar];
			double[] rightVar=new double[nVar];
			IloConstraint addleftCons = addCons(isint, store[isint],0);
			double leftres=solve(leftVar);
			cplex.exportModel(level+".1.lp");
			delCons(addleftCons);
			IloConstraint addrightcons = addCons(isint,store[isint], 1);
			double rightres=solve(rightVar);
			cplex.exportModel(level+".2.lp");
			delCons(addrightcons);
		//贪心	,每一层都要走遍左右两个节点
			if (leftres >= rightres) {
				//先左后右
				addleftCons = addCons(isint,store[isint], 0);
				dfs(leftres, leftVar, level + 1);
				delCons(addleftCons);
				addrightcons = addCons(isint,store[isint], 1);
				dfs(rightres, rightVar, level + 1);
				delCons(addrightcons);
			} else {
				//先右后左
				addrightcons = addCons(isint,store[isint], 1);        
				dfs(rightres, rightVar, level + 1);
				delCons(addrightcons);
				addleftCons = addCons(isint,store[isint], 0);
				dfs(leftres, leftVar, level + 1);
				delCons(addleftCons);
			}
			
		}
	}
}
