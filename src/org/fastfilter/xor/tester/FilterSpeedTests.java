package org.fastfilter.xor.tester;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.fastfilter.Filter;
import org.fastfilter.xor.XorIntegratedFilter;
import org.fastfilter.xor.XorSeveralFilters;
import org.fastfilter.xor.XorSimpleN;

public class FilterSpeedTests {

	public static void main(String[] args) {
		// Size of S
		int sSize = 100000;
		// Size of T
		int[] tSizeSet = {10000, 100000, 1000000, 10000000};
		int[] nBitsSet = {3, 7, 15};
		int runs = 100;
		// Size of a third set that is disjoint from S and T
		//int oSize=10000000;
		String filename = "speed.log";
		// Bits of Filter 1 (r-1)

		FileWriter fw = null;
		
		try {
			fw = new FileWriter(filename);
		} catch (IOException e) {
			System.out.println("Error creating file " + e.getMessage());
			System.exit(1);
		}
		
		try {
			fw.write("Type;TSize;r-1;Creation;CheckPos;CheckNeg;A\n");
		}catch(Exception e) {
			
		}
		for (int nBits: nBitsSet) {
			for (int tSize: tSizeSet) {
				System.out.println("-------------------------------------");
				System.out.println("|S|="+sSize+"; |T|="+tSize+"; r-1="+nBits);
				double[] timeCreateOF = new double[runs];
				double[] timeCheckOFnegative = new double[runs];
				double[] timeCheckOFpositive = new double[runs];
				double[] timeCreateTF = new double[runs];
				double[] timeCheckTFnegative = new double[runs];
				double[] timeCheckTFpositive = new double[runs];
				double[] timeCreateIF1 = new double[runs];
				double[] timeCheckIF1negative = new double[runs];
				double[] timeCheckIF1positive = new double[runs];
				double[] timeCreateIF2 = new double[runs];
				double[] timeCheckIF2negative = new double[runs];
				double[] timeCheckIF2positive = new double[runs];

				// Calculate the number of false positives from T ==> |F|~|T|/2^(r-1)
				double prob = tSize/Math.pow(2, nBits);
				// Calculate f as f>=(|S|+|F|)/|S|   as we want |S|>=(|S|+|F|)/f 
				int f = 2;//(int) Math.ceil((sSet.size()+prob)/sSet.size());
				int a = (int) Math.ceil(Math.log(prob/sSize)/Math.log(2));
				if(a<0) {a=0;}

				// Calculate best A value for the TF: Two filters
				int optimizedTFA = XorSeveralFilters.getOptimizedAddedBits(sSize, tSize, nBits);
				
				// Calculate best A value for the Integrated filter with 1 subfilter (f=1)
				int optimizedIF1 = XorIntegratedFilter.getOptimizedAddedBitsF1(sSize, tSize, nBits);

				// Execute runs times
				for (int i=0;i<runs;i++) {
					System.out.println("Starting Run " +i);
					
					// Generate the set S
					Set<Long> sSet = build(sSize, null);
					// Generate the set T
					Set<Long> tSet = build(tSize, sSet);
					// Build a set of elements disjoint from S and T
					/*Set<Long> exclude = new HashSet<Long>();
					exclude.addAll(sSet);
					exclude.addAll(tSet);
					Set<Long> oSet = build(oSize, exclude);*/		

					long init = System.nanoTime();
					// True filter with optimized A			
					XorSimpleN xn = XorSimpleN.construct(sSet, nBits+1);
					long end = System.nanoTime();
					timeCreateOF[i]=end-init;
					timeCheckOFpositive[i]=FilterSpeedTests.checkFilter(xn, sSet);
					timeCheckOFnegative[i]=FilterSpeedTests.checkFilter(xn, tSet, true);
					xn=null;
					
					init = System.nanoTime();
					// True filter with optimized A			
					XorSeveralFilters xsf = XorSeveralFilters.construct(sSet, tSet, nBits+optimizedTFA, 1);
					end = System.nanoTime();
					timeCreateTF[i]=end-init;
					
					timeCheckTFpositive[i]=FilterSpeedTests.checkFilter(xsf, sSet);
					timeCheckTFnegative[i]=FilterSpeedTests.checkFilter(xsf, tSet);
					xsf=null;
	
					init = System.nanoTime();
					XorIntegratedFilter xi = XorIntegratedFilter.construct(sSet, tSet, nBits+optimizedIF1, 1);
					end = System.nanoTime();
					timeCreateIF1[i]=end-init;
					
					timeCheckIF1positive[i]=FilterSpeedTests.checkFilter(xi, sSet);
					timeCheckIF1negative[i]=FilterSpeedTests.checkFilter(xi, tSet);
					
					init = System.nanoTime();
					xi = XorIntegratedFilter.construct(sSet, tSet, nBits+a, f);
					end = System.nanoTime();
					timeCreateIF2[i]=end-init;
					
					timeCheckIF2positive[i]=FilterSpeedTests.checkFilter(xi, sSet);
					timeCheckIF2negative[i]=FilterSpeedTests.checkFilter(xi, tSet);
				}
				try {
					// Calculate the estimated number of elements that will be false positives for TF
					fw.write("OF;"+tSize+";"+nBits+";"+FilterSpeedTests.mean(timeCreateOF)+";"+
							FilterSpeedTests.mean(timeCheckOFpositive)+";"+
							FilterSpeedTests.mean(timeCheckOFnegative)+";0");
					fw.write("\n");
					// Calculate the estimated number of elements that will be false positives for TF
					fw.write("TF;"+tSize+";"+nBits+";"+FilterSpeedTests.mean(timeCreateTF)+";"+
							FilterSpeedTests.mean(timeCheckTFpositive)+";"+
							FilterSpeedTests.mean(timeCheckTFnegative)+";"+optimizedTFA);
					fw.write("\n");
					//Calculate the estimated number of elements that will be false positives for IF (f=1)
					fw.write("IF1;"+tSize+";"+nBits+";"+FilterSpeedTests.mean(timeCreateIF1)+";"+
							FilterSpeedTests.mean(timeCheckIF1positive)+";"+
							FilterSpeedTests.mean(timeCheckIF1negative)+";"+optimizedIF1);
					fw.write("\n");
					fw.write("IF2;"+tSize+";"+nBits+";"+FilterSpeedTests.mean(timeCreateIF2)+";"+
							FilterSpeedTests.mean(timeCheckIF2positive)+";"+
							FilterSpeedTests.mean(timeCheckIF2negative)+";"+a);
					fw.write("\n");
					fw.flush();

				}catch(Exception e){
					
				}
			}
		}
		try {
			fw.close();
		}catch(Exception e) {
			
		}

	}
	
	/**
	 * Calculating mean value of elements in m array
	 * @param m
	 * @return
	 */
	private static double mean(double[] m) {
		double total = 0;
		for (double l : m) {
			total+=l;
		}
		double result = Math.round(100000*total/m.length)/100000D;
		return result;
	}

	private static double checkFilter(Filter xi, Set<Long> set, boolean onlyNegatives) {
		long init = 0;
		long end = 0;
		double acc = 0;
		int total = 0;
		for (long s: set ) {
			init=System.nanoTime();
			boolean res = xi.mayContain(s);
			end=System.nanoTime();
			if(!onlyNegatives || !res) {
				acc += end-init;
				total++;
			}
		}
		return acc/(double)total;
		
	}
	
	private static double checkFilter(Filter xi, Set<Long> set) {
		return checkFilter(xi, set, false);
	}


	public static Set<Long> build (int elements, Set<Long> exclude) {
		Set<Long> s = new HashSet<Long>();

		Random random = new Random();
		//SecureRandom random = new SecureRandom();
		for (int i=0; i< elements; i++) {
			do{
				long next = random.nextLong();
				if(exclude==null || !exclude.contains(next)) {
					s.add(next);
				}
			}while(s.size()<i+1);
		}
		return s;
	}


}
