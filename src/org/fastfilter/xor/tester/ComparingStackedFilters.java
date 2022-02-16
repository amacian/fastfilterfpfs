package org.fastfilter.xor.tester;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.fastfilter.xor.XorSeveralFilters;
import org.fastfilter.xor.XorStackedFilters;

public class ComparingStackedFilters {

	public static void main(String[] args) {
		// Size of S
		int sSize = 100000;
		// Size of T
		int[] tSizeSet = {10000, 100000, 1000000, 10000000};
		int[] nBitsSet = {4, 8, 16};
		int[] bBits = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		// Number of executions to get the mean
		int runs = 100;
		// Size of a third set that is disjoint from S and T
		int oSize=10000000;
		// Filename to store memory used and FP probability
		String filename = "comparisonSF.log";

		// The file writers to store the information
		FileWriter fw = null;
		
		try {
			fw = new FileWriter(filename);
		} catch (IOException e) {
			System.out.println("Error creating file " + e.getMessage());
			System.exit(1);
		}
		
		try {
			// Type of filter, size of T, r-1, calculated memory, estimated memory, estimated FP probality of elements from T
			// Actual percentage of false positives from T, false positives from another set once the filter is built, and optimized A value
			fw.write("Type;TSize;r;b;NumberOfFilters;ActualMemory;TActualFPP;FinalFPP;A\n");
		}catch(Exception e) {
			
		}
		for (int nBits: nBitsSet) {
			for (int b: bBits) {
				for (int tSize: tSizeSet) {
					// Elements to store the actual memory size per run and type of filter
					double[] msize = new double[runs];
					// Elements to store the fpp of elements from T per run and type of filter
					double[] internalfpp = new double[runs];
					// Elements to store the fpp of elements from a disjoint set per run and type of filter
					double[] fpp = new double[runs];
					// Number of stacked filters
					double[] nStack = new double[runs];
					
					System.out.println("-------------------------------------");
					// Calculate best A value for the Two-filter
					int optimizedTFA = XorSeveralFilters.getOptimizedAddedBits(sSize, tSize, nBits);
					System.out.println("|S|="+sSize+"; |T|="+tSize+"; r="+nBits+"; a="+optimizedTFA+"; b="+b);
					// Execute runs times
					for (int i=0;i<runs;i++) {
						System.out.println("Starting Run " +i);
						
						// Generate the set S
						Set<Long> sSet = ComparingFilters.build(sSize, null);
						// Generate the set T
						Set<Long> tSet = ComparingFilters.build(tSize, sSet);
						// Build a set of elements disjoint from S and T
						Set<Long> exclude = new HashSet<Long>();
						exclude.addAll(sSet);
						exclude.addAll(tSet);
						Set<Long> oSet = ComparingFilters.build(oSize, exclude);
						XorStackedFilters xsf = XorStackedFilters.construct(sSet, tSet, nBits+optimizedTFA, b);
						// Check the behavior and retrieve the FPP for a disjoint set O
						fpp[i]=ComparingFilters.checkFilter(xsf, sSet, tSet, oSet);
						// Get the actual size
						msize[i]=xsf.getBitCount();
						// Get the FPP of T (i.e. %T that makes F)
						internalfpp[i] = xsf.getFppT();
						// Get the number of stacked filters, including the first one
						nStack[i] = xsf.getNStacked();

					}
					try {
						fw.write("SF;"+tSize+";"+nBits+";"+b+";"+ComparingFilters.mean(nStack)+";"+ComparingFilters.mean(msize)+";"+ComparingFilters.mean(internalfpp)+";"+ComparingFilters.mean(fpp)+";"+optimizedTFA);
						fw.write("\n");
						fw.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
		try {
			fw.close();
		}catch(Exception e) {
			
		}

	}

}
