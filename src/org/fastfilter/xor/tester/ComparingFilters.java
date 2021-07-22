package org.fastfilter.xor.tester;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.fastfilter.Filter;
import org.fastfilter.xor.XorIntegratedFilter;
import org.fastfilter.xor.XorSeveralFilters;

/**
 * Class to compare memory used, and false positive probability for the different filters
 * @author Alfonso
 *
 */
public class ComparingFilters {

	public static void main(String[] args) {
		// Size of S
		int sSize = 100000;
		// Size of T
		int[] tSizeSet = {10000, 100000, 1000000, 10000000};
		int[] nBitsSet = {3, 7, 15};
		// Number of executions to get the mean
		int runs = 100;
		// Size of a third set that is disjoint from S and T
		int oSize=10000000;
		// Filename to store memory used and FP probability
		String filename = "comparison.log";
		// Filename to store the number of seeds used until each filter converges
		String filename2 = "tries.log";
		// Bits of Filter 1 (r-1)

		// The file writers to store the information
		FileWriter fw = null;
		FileWriter fw2 = null;
		
		try {
			fw = new FileWriter(filename);
			fw2 = new FileWriter(filename2);
		} catch (IOException e) {
			System.out.println("Error creating file " + e.getMessage());
			System.exit(1);
		}
		
		// Delta value for the XOR filters
		int delta = 32;
		try {
			// Type of filter, size of T, r-1, calculated memory, estimated memory, estimated FP probality of elements from T
			// Actual percentage of false positives from T, false positives from another set once the filter is built, and optimized A value
			fw.write("Type;TSize;r-1;ActualMemory;EstimatedMemory;TEstimatedFPP;TActualFPP;FinalFPP;A\n");
			fw2.write("Type;TSize;r-1;Tries;PercentFails\n");
		}catch(Exception e) {
			
		}
		for (int nBits: nBitsSet) {
			for (int tSize: tSizeSet) {
				System.out.println("-------------------------------------");
				System.out.println("|S|="+sSize+"; |T|="+tSize+"; r-1="+nBits);
				// Elements to store the actual memory size per run and type of filter
				double[] msizeTFa = new double[runs];
				double[] msizeIF1a = new double[runs];
				double[] msizeIF2a = new double[runs];
				// Elements to store the fpp of elements from T per run and type of filter
				double[] TFInternalfpp = new double[runs];
				double[] IF1Internalfpp = new double[runs];
				double[] IF2Internalfpp = new double[runs];
				// Elements to store the fpp of elements from a disjoint set per run and type of filter
				double[] TFfpp = new double[runs];
				double[] IF1fpp = new double[runs];
				double[] IF2fpp = new double[runs];
				// Elements to store the number of hash function tried per run and type of filter
				double[] triesTF = new double[runs];
				double[] triesIF1 = new double[runs];
				double[] triesIF2 = new double[runs];
				// Elements to store the percentage of hash functions that failed per run and type of filter
				double[] percFailsTF = new double[runs];
				double[] percFailsIF1 = new double[runs];
				double[] percFailsIF2 = new double[runs];

				// Calculate the number of false positives from T ==> |F|~|T|/2^(r-1)
				double prob = tSize/Math.pow(2, nBits);
				// Calculate f as f>=(|S|+|F|)/|S|   as we want |S|>=(|S|+|F|)/f 
				int f = 2;//(int) Math.ceil((sSet.size()+prob)/sSet.size());
				int a = (int) Math.ceil(Math.log(prob/sSize)/Math.log(2));
				if(a<0) {a=0;}

				// Calculate best A value for the Two-filter
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
					Set<Long> exclude = new HashSet<Long>();
					exclude.addAll(sSet);
					exclude.addAll(tSet);
					Set<Long> oSet = build(oSize, exclude);		
			
					// Two filters with optimized A			
					XorSeveralFilters xsf = XorSeveralFilters.construct(sSet, tSet, nBits+optimizedTFA, 1);
					// Check the behavior and retrieve the FPP for a disjoint set O
					TFfpp[i]=ComparingFilters.checkFilter(xsf, sSet, tSet, oSet);
					// Get the actual size
					msizeTFa[i]=xsf.getBitCount();
					// Get the FPP of T (i.e. %T that makes F)
					TFInternalfpp[i] = xsf.getFppT();
					// Get the number of hash functions that were tried until it converged.
					triesTF[i] = xsf.getTries();
					// Get the percentage of hash functions that failes.
					percFailsTF[i]=xsf.getPercentFails();
					xsf=null;
	
					// Integrated filter with optimized A for f=1
					XorIntegratedFilter xi = XorIntegratedFilter.construct(sSet, tSet, nBits+optimizedIF1, 1);
					// Check the behavior and retrieve the FPP for a disjoint set O
					IF1fpp[i]=ComparingFilters.checkFilter(xi, sSet, tSet, oSet);
					//Get the FPP of T (i.e. %T that makes F)
					IF1Internalfpp[i] = xi.getFppT();
					//Get the actual size
					msizeIF1a[i]=xi.getBitCount();
					// Get the number of hash functions that were tried until it converged.
					triesIF1[i] = xi.getTries();
					// Get the percentage of hash functions that failes.
					percFailsIF1[i]=xi.getPercentFails();
					
					//Integrated filter with optimized A for f=2
					xi = XorIntegratedFilter.construct(sSet, tSet, nBits+a, f);
					// Check the behavior and retrieve the FPP for a disjoint set O
					IF2fpp[i]=ComparingFilters.checkFilter(xi, sSet, tSet, oSet);
					//Get the FPP of T (i.e. %T that makes F)
					IF2Internalfpp[i] = xi.getFppT();
					//Get the actual size
					msizeIF2a[i]=xi.getBitCount();
					// Get the number of hash functions that were tried until it converged.
					triesIF2[i] = xi.getTries();
					// Get the percentage of hash functions that failes.
					percFailsIF2[i]=xi.getPercentFails();
				}
				try {
					// Calculate the estimated number of elements that will be false positives for TF
					double prob2 = tSize/Math.pow(2, nBits+optimizedTFA);
					fw.write("TF;"+tSize+";"+nBits+";"+ComparingFilters.mean(msizeTFa)+";"+Math.ceil(((1.23*sSize)+delta)*(nBits+optimizedTFA) + 1.23*(sSize+prob2+delta))+
							";"+100*prob2/(double)tSize+";"+ComparingFilters.mean(TFInternalfpp)+";"+
							ComparingFilters.mean(TFfpp)+";"+optimizedTFA);
					fw.write("\n");
					double totalTries = ComparingFilters.sum(triesTF);
					fw2.write("TF;"+tSize+";"+nBits+";"+ComparingFilters.mean(triesTF)+
							";"+ (totalTries-(2*runs))/totalTries);
					//		";"+ComparingFilters.mean(percFailsTF));
					fw2.write("\n");
					//Calculate the estimated number of elements that will be false positives for IF (f=1)
					double prob3 = tSize/Math.pow(2, nBits+optimizedIF1);
					fw.write("IF1;"+tSize+";"+nBits+";"+ComparingFilters.mean(msizeIF1a)+";"+(int)Math.ceil((1.23*(sSize+prob3)+delta)*(nBits+optimizedIF1+1))+
							";"+100*prob3/(double)tSize+";"+ComparingFilters.mean(IF1Internalfpp)+";"
							+ComparingFilters.mean(IF1fpp)+";"+optimizedIF1);
					fw.write("\n");
					totalTries = ComparingFilters.sum(triesIF1);
					fw2.write("IF1;"+tSize+";"+nBits+";"+ComparingFilters.mean(triesIF1)+
							";"+ (totalTries-runs)/totalTries);
					//		";"+ComparingFilters.mean(percFailsIF1));
					fw2.write("\n");					//Calculate the estimated number of elements that will be false positives for IF (f=2)
					double prob4 = tSize/Math.pow(2, nBits+a);
					fw.write("IF2;"+tSize+";"+nBits+";"+ComparingFilters.mean(msizeIF2a)+";"+(int)Math.ceil((1.23*sSize)+delta)*(f+nBits+a)+
							";"+100*prob4/(double)tSize+";"+ComparingFilters.mean(IF2Internalfpp)+";"+ComparingFilters.mean(IF2fpp)+";"+a);
					fw.write("\n");
					totalTries = ComparingFilters.sum(triesIF2);
					fw2.write("IF2;"+tSize+";"+nBits+";"+ComparingFilters.mean(triesIF2)+
							";"+ (totalTries-runs)/totalTries);
							//";"+ComparingFilters.mean(percFailsIF2));
					fw2.write("\n");
					fw.flush();
					fw2.flush();

				}catch(Exception e){
					
				}
			}
		}
		try {
			fw.close();
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
		double total = sum(m);
		double result = Math.round(100000*total/m.length)/100000D;
		return result;
	}

	/**
	 * Calculate the sum of an array of elements.
	 * @param m
	 * @return
	 */
	private static double sum(double[] m) {
		double total = 0;
		for (double l : m) {
			total+=l;
		}
		return total;
	}

	/**
	 * Validate that the filters work properly
	 * @param xi filter
	 * @param sSet S set with the positive elements
	 * @param tSet T set free of false positives
	 * @param oSet O set independent from the previous ones
	 */
	private static double checkFilter(Filter xi, Set<Long> sSet, Set<Long> tSet, Set<Long> oSet) {
		//System.out.println("Actual size of the filter =" +xi.getBitCount());
		
		// Total number of true positives
		int totalTP = 0;
		// Total number of true negatives
		int totalTN = 0;
		// Check all the elements in S, totalTP must be s.size()
		for (long s: sSet ) {
			if (xi.mayContain(s)) {
				totalTP++;
			}
		}
		if (totalTP<sSet.size()) {
			System.out.println("Error in the filter");
		}
		
		//  Check all the elements in T, totalTN must be t.size()
		for (long t: tSet ) {
			if (!xi.mayContain(t)) {
				totalTN++;
			}
		}
		if (totalTN<tSet.size()) {
			System.out.println("Error in the filter");
		}

		int totalFP=0;
		
		// Check all elements from O
		for (long o: oSet ) {
			if (xi.mayContain(o)) {
				totalFP++;
			}
		}
		// This should be ~ |O|/(2^r) 
		double FPFromTotal = Math.round(totalFP/(float)oSet.size()*1000000)/10000.0;
		return FPFromTotal;
		
	}


	/**
	 * Method to build the sets
	 * @param elements number of elements (longs) to be generated
	 * @param exclude Set with elements that should not be included in the new Set
	 * @return the generated Set with the "long" elements.
	 */
	public static Set<Long> build (int elements, Set<Long> exclude) {
		Set<Long> s = new HashSet<Long>();

		// Generate the elements pseudorandomly
		Random random = new Random();

		// Until all the elements have been generated
		for (int i=0; i< elements; i++) {
			// Generate a long, check that is not in the excluded set and add it to the new set
			// If it is in the excluded set, repeat the operation
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
