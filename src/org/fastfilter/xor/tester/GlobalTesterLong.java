package org.fastfilter.xor.tester;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.fastfilter.Filter;
import org.fastfilter.xor.XorIntegratedFilter;
import org.fastfilter.xor.XorSeveralFilters;
import org.fastfilter.xor.XorSimpleNRemover;

/**
 * Class that tests different T sizes and r-1 values for S=100000
 * @author Alfonso
 *
 */
public class GlobalTesterLong {

	public static void main(String[] args) {
		// Size of S
		int sSize = 100000;
		// Size of T
		int[] tSizeSet = {10000, 100000, 1000000, 10000000};
		// Bits of Filter 1 (r-1)
		int[] nBitsSet = {7, 15};

		// Value delta for XOR filter
		int delta = 32;

		// Iterate over the different r-1 values
		for (int nBits: nBitsSet) {
			// And over the different sizes of T
			for (int tSize: tSizeSet) {
				System.out.println("-------------------------------------");
				System.out.println("|S|="+sSize+"; |T|="+tSize+"; r-1="+nBits);
				// Size of a third set that is disjoint from S and T
				int oSize=100000;
				
				// Generate the set S
				Set<Long> sSet = build(sSize, null);
				// Generate the set T
				Set<Long> tSet = build(tSize, sSet);
				// Build a set of elements disjoint from S and T
				Set<Long> exclude = new HashSet<Long>();
				exclude.addAll(sSet);
				exclude.addAll(tSet);
				Set<Long> oSet = build(oSize, exclude);		
		
				// Calculate the number of false positives from T ==> |F|~|T|/2^(r-1)
				System.out.println("fpp="+1/Math.pow(2, nBits));
				double prob = tSet.size()/Math.pow(2, nBits);
				System.out.println("Expected number of false positives: "+ (int)Math.ceil(prob));
				// Calculate f as f>=(|S|+|F|)/|S|   as we want |S|>=(|S|+|F|)/f 
				// To be used in the IntegratedFilter IF
				int f = 2;//(int) Math.ceil((sSet.size()+prob)/sSet.size());
				int f2 = (int) Math.ceil((sSet.size()+prob)/sSet.size());
				// Calculate parameter "a" as additional bits for the first part of the filter to optimize the total filter in IntegratedFilter f=1.
				int a = (int) Math.ceil(Math.log(prob/sSet.size())/Math.log(2));
				if(a<0) {a=0;}
				System.out.println("Expected number of false positives with a="+a+": "+ (int)Math.ceil(tSet.size()/Math.pow(2, nBits+a)));
				// Size of the Naive filter based on |T|+|S|
				System.out.println("Estimated size for naive approach is "+ (int)Math.ceil((1.23*(sSize+tSize)+delta)*(nBits+1)));
				// Size of the Two-filter approach without a optimization
				System.out.println("Estimated size for TF approach is "+ Math.ceil((1.23*(sSize+delta)*nBits + 1.23*(sSize+prob+delta))));
				// Optimized value of A for the Two-filter (additional bits for the first filter to optimize the total filter)
				int optimizedTFA = XorSeveralFilters.getOptimizedAddedBits(sSize, tSize, nBits);
				// Expected probability when using the A value in Two-filter implementation
				double prob2 = tSet.size()/Math.pow(2, nBits+optimizedTFA);
				// Size of the Two-filter approach with a optimization
				System.out.println("Estimated size for TF approach with a="+optimizedTFA+" is "+ Math.ceil(((1.23*sSize)+delta)*(nBits+optimizedTFA) + 1.23*(sSize+prob2+delta)));
				
				// Size of the Integrated filter approach with f=2 and with a optimization
				System.out.println("Estimated size for IF with f="+f+ " and a="+a+" is "+ Math.ceil((1.23*sSize)+delta)*(f+nBits+a));
				// Size of the Integrated filter approach with f calculated and without a optimization
				System.out.println("Estimated size for IF with f2="+f2+ " is "+ Math.ceil((1.23*sSize+delta)*(f2+nBits)));
				// Size of the Integrated filter approach with f=1 and without a optimization
				System.out.println("Estimated size for IF with f=1 is " + (int)Math.ceil((1.23*(sSet.size()+prob)+delta)*(nBits+1)));
				// Calculating best value of a for the Integrated Filter with f=1 and its FP probability
				int optimizedIF1 = XorIntegratedFilter.getOptimizedAddedBitsF1(sSize, tSize, nBits);
				double prob3 = tSet.size()/Math.pow(2, nBits+optimizedIF1);
				// Size of the Integrated filter approach with f=1 and with a optimization
				System.out.println("Estimated size for IF with f=1 and a="+optimizedIF1+"is " + (int)Math.ceil((1.23*(sSet.size()+prob3)+delta)*(nBits+optimizedIF1+1)));
				if(sSize*(f+nBits) > Math.ceil((sSet.size()+prob)*(nBits+1))){
					f=1; //Select f=2 or f=1 depending on the smallest size
				}
		
				System.out.println("-------------------------------------");
				System.out.println("First test: Naive filter");
				XorSimpleNRemover xsnr = XorSimpleNRemover.construct(sSet, tSet, nBits+1);
				System.out.println("Testing Naive filter");				
				GlobalTesterLong.checkFilter(xsnr, sSet, tSet, oSet);
				xsnr=null;
				
				System.out.println("Second test: TF without 'a' optimization");
				XorSeveralFilters xsf = XorSeveralFilters.construct(sSet, tSet, nBits, 1);
				GlobalTesterLong.checkFilter(xsf, sSet, tSet, oSet);
		
				System.out.println("Third test: TF with 'a' optimization");
				xsf = XorSeveralFilters.construct(sSet, tSet, nBits+optimizedTFA, 1);
				GlobalTesterLong.checkFilter(xsf, sSet, tSet, oSet);
				xsf=null;

				System.out.println("Fourth test: IF with just 1 bit");
				XorIntegratedFilter xi = XorIntegratedFilter.construct(sSet, tSet, nBits, 1);
				GlobalTesterLong.checkFilter(xi, sSet, tSet, oSet);

				System.out.println("Fifth test: IF with just 1 bit and 'a' optimization");
				xi = XorIntegratedFilter.construct(sSet, tSet, nBits+optimizedIF1, 1);
				GlobalTesterLong.checkFilter(xi, sSet, tSet, oSet);
				
				System.out.println("Sixth test: IF without 'a' optimization");
				xi = XorIntegratedFilter.construct(sSet, tSet, nBits, f2);
				GlobalTesterLong.checkFilter(xi, sSet, tSet, oSet);
				
				System.out.println("Seventh test: IF with 'a' optimization");
				xi = XorIntegratedFilter.construct(sSet, tSet, nBits+a, f);
				GlobalTesterLong.checkFilter(xi, sSet, tSet, oSet);		

			}
		}

	}
	

	/**
	 * Validate that the filters work properly
	 * @param xi filter
	 * @param sSet S set with the positive elements
	 * @param tSet T set free of false positives
	 * @param oSet O set independent from the previous ones
	 */
	private static void checkFilter(Filter xi, Set<Long> sSet, Set<Long> tSet, Set<Long> oSet) {
		System.out.println("Actual size of the filter =" +xi.getBitCount());
		
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
		System.out.println("True positives: "+totalTP);
		
		//  Check all the elements in T, totalTN must be t.size()
		for (long t: tSet ) {
			if (!xi.mayContain(t)) {
				totalTN++;
			}
		}
		// This should be 100%
		double TNFromTotal = Math.round(totalTN/(float)tSet.size()*10000)/100.0;
		System.out.println("% negatives from T set: "+TNFromTotal +"%");
		System.out.println("Total negatives from T set: "+totalTN);

		totalTN=0;
		
		// Check all elements from O
		for (long o: oSet ) {
			if (!xi.mayContain(o)) {
				totalTN++;
			}
		}
		// This should be ~ |O|/(2^r) 
		TNFromTotal = Math.round(totalTN/(float)oSet.size()*10000)/100.0;
		System.out.println("% negatives from disjoint set: "+TNFromTotal +"%");
		System.out.println("Total negatives from disjoint set: "+totalTN);
		
		System.out.println("-------------------------------------");
		
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
