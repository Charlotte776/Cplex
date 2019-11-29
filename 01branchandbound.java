package usecplex;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
/*max 9x1+5x2+6x3+4x4
 * 
 * 6x1+3x2+5x3+2x4<=10
 *          x3+ x4<=1
 * -x1    + x3    <=0
 *     -x2    + x4<=0
 *     
 * xj = 0 or 1 (j=1,2,3,4)
 * 
 */
//分支
//定界，求边界值，向下取整得到Z*
//剪枝，1)边界值<=Z*; 2)不含可行解；3）最优解是整数
public class bab {
	IloCplex cplex;
	IloNumVar[] x;
	static int nVar=4;//变量数量
	static double[] res=new double[nVar];//储存结果
	static int nowIbest=Integer.MIN_VALUE;//目前最优的整数解
	
	public static void main(String[] args) throws IloException {
		bab q= new bab();
		q.model1();//初始模型
		int b=q.solve(res);//解取整数 
		q.dfs(b,res,0);                       
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
		        double[] ub = {1,1,1,1};
		        x = cplex.numVarArray(nVar, lb, ub);//（变量个数，下界，上界）
		        
		        double[] objvals = {9, 5, 6,4};
		        cplex.addMaximize(cplex.scalProd(x, objvals));
		        
		        double[][] coeff = {{6,3,5,2},{0,0,1,1},{-1,0,1,0},{0,-1,0,1}};
		        double[] cons= {10,1 ,0,0};
		        for(int i=0;i<4;i++) {
		        	cplex.addLe(cplex.scalProd(x, coeff[i]), cons[i]);
		        }        
       } catch (IloException e) {
           System.err.println("Concert exception caught: " + e);
       }
	}
	//向下取整得到Z*
	public int solve(double[] res2) {
		try {
			if (cplex.solve()) {
		        cplex.output().println("Solution status = " + cplex.getStatus());
				for (int i = 0; i < nVar; i++) {
					res2[i] = cplex.getValues(x)[i];
					System.out.println("x"+(i+1)+"="+res2[i]);
				}
				System.out.println("obj="+cplex.getObjValue()+"\n");
				return (int) cplex.getObjValue();
			}
			return Integer.MIN_VALUE;
		} catch (IloException e) {
			System.err.println("Exception e: " + e);
			return Integer.MIN_VALUE;
		}
	}
	//加上条件:哪个整数，取什么方向（0左，1右）
	public IloConstraint addCons(int xb,int dir) throws IloException {
		IloConstraint exCons=null;
		if(dir==0) {//向下取整
			exCons=cplex.addLe(x[xb], 0);
		}else {//向上取整
			exCons=cplex.addGe(x[xb], 1);
		}
		return exCons;
	}
	//减去条件:
	public void delCons(IloConstraint exCons) throws IloException {
		cplex.remove(exCons);
	}
	//判断是不是均为小数,int直接是去掉小数部分
	public int isInt(double[] res) {
		for(double a:res) {
			if(a>=0) {
				if((a-(int)a<1e-9)||((int)a+1-a)<1e-9)
					continue;
				return 0;
			}else {
				if(((int)a-a<1e-9)||((int)a+1+a)<1e-9)
					continue;
				return 0;
			}
		}
		return -1;
	}

	//目前式子算出来的向下取整的z*,储存解的数组，层级。
	public void dfs(int z,double[] store,int level) throws IloException {
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
		}else if(level>=nVar) {
			System.out.println("level>=nVar\n");
			return;
		}else {
		//分支，先遍历左子树，再遍历右子树
			double[] leftVar=new double[nVar];
			double[] rightVar=new double[nVar];
			IloConstraint addleftCons = addCons(level, 0);
			int leftres=solve(leftVar);
			cplex.exportModel(level+".1.lp");
			delCons(addleftCons);
			IloConstraint addrightcons = addCons(level, 1);
			int rightres=solve(rightVar);
			cplex.exportModel(level+".2.lp");
			delCons(addrightcons);
		//贪心	,每一层都要走遍左右两个节点
			if (leftres >= rightres) {
				//先左后右
				addleftCons = addCons(level, 0);
				dfs(leftres, leftVar, level + 1);
				delCons(addleftCons);
				addrightcons = addCons(level, 1);
				dfs(rightres, rightVar, level + 1);
				delCons(addrightcons);
			} else {
				//先右后左
				addrightcons = addCons(level, 1);        
				dfs(rightres, rightVar, level + 1);
				delCons(addrightcons);
				addleftCons = addCons(level, 0);
				dfs(leftres, leftVar, level + 1);
				delCons(addleftCons);
			}
			
		}
	}
}
