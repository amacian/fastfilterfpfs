package org.fastfilter.xor;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fastfilter.Filter;


/**
 * Class with the two (n) filter (TF) composed by r-1 (nBits) bits of a first filter
 * and 1 (f) additional 1-bit filters.
 * @author Alfonso
 *
 */
public class XorSeveralFiltersTByFile implements Filter{
	
	/** First filter with r-1 (nBits) bits for S*/
	XorSimpleN originalXor;
	/** 1-bit filter (or set of f filters) for S U F*/ 
	XorSimpleNRemover[] oneBitXors;

	/** Actual probability of false positives produced by T */
	double fppT = 0;

	/**
	 * Get the actual probability of false positives produced by T
	 * @return false positive probability  
	 */
    public double getFppT() {
		return fppT;
	}


	/**
	 * Get the number of hash function tried until the filter is built
	 * @return number of tries until a proper set of hash functions is found.
	 */
    public int getTries() {
    	int totalTries = originalXor.getTries();
    	for (XorSimpleNRemover x : oneBitXors) {
    		totalTries+=x.getTries();
    	}
		return totalTries;
	}
    
    /**
     * Get the percentage of hash function that were tried and failed
     * @return percentage of failures
     */
    public double getPercentFails() {
    	int totalTries = getTries();
		int totalFilters = oneBitXors.length+1;
		// All the filters were filled so 1 try was successful per each filter.
		return (totalTries-totalFilters)/(double)totalTries;
    }
    
	/** Method to construct the filter
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOutFile Reader to get the Set T with the elements that must return a negative match
     * @param outSize Number of elements that are included in the T set 
     * @param bitsN Number of bits for the first filter (Filter 1)
     * @param bitsF Number of 1-bit additional filters (1 for TF)
     * @return a new Filter with the selected parameters
     */
    public static XorSeveralFiltersTByFile construct(Set<Long> keysIn, LineNumberReader keysOutFile, int outSize,  int bitsN, int bitsF) {
        return new XorSeveralFiltersTByFile (keysIn, keysOutFile, outSize,  bitsN, bitsF);
    }

    /**
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOutFile Reader to get the Set T with the elements that must return a negative match
     * @param outSize Number of elements that are included in the T set 
     * @param bitsN Number of bits for the first filter (Filter 1)
     * @param bitsF Number of 1-bit additional filters (1 for TF)
     */	
	XorSeveralFiltersTByFile (Set<Long> keysIn, LineNumberReader keysOutFile, int outSize,  int bitsN, int bitsF){
		// create the initial filter only for S (keysIn) and r-1 (bitsN) bits
		originalXor = XorSimpleN.construct(keysIn, bitsN);
		// create as many 1-bit filters as parameter bitF
		oneBitXors = new XorSimpleNRemover[bitsF];
		// Parameter used to assign the elements of S to a specific 1-bit filter
		List<Set<Long>> xorInAssignation = new ArrayList<Set<Long>>();
		// Parameter used to assign the elements of F to a specific 1-bit filter
		List<Set<Long>> xorOutAssignation = new ArrayList<Set<Long>>();
		
		// Create the sets for each 1-bit filter
		for (long i=0; i<bitsF; i++) {
			Set<Long> s = new HashSet<Long>();
			xorInAssignation.add(s);
			s = new HashSet<Long>();
			xorOutAssignation.add(s);
		}
		
        // keep track of false positives for debugging purposes
        int fp = 0;

		// Find the false positives (F) of T and assign them to a specific 1-bit filter
		// For TF there is only one 1-bit filter so all false positives will be assigned to it.
		String nextLine;
		try {	
			// Read long elements of T from the reader line by line
	        while ((nextLine = keysOutFile.readLine())!=null) {
	        	// Each line should one of the elements (long) from T
				Long t = Long.parseLong(nextLine);
				// Compare only the most significant bitsN
				if (originalXor.mayContain(t)) {
					// Calculate the 1-bit filter it corresponds to
					int pos = c(t,bitsF);
					// Assign the false positive to that 1-bit filter
					xorOutAssignation.get(pos).add(t);
					fp++;
				}
			}
		}catch (IOException ioe) {
			System.out.println("Error reading T set from file");
			System.exit(1);
		}
		
		// System.out.println("Total number of false positives from T:"+fp);
		this.fppT = (100*fp)/(double)outSize;
		// Assign each element of S to a specific 1-bit filter
		// For TF there is only one 1-bit filter so all elements will be assigned to it.
		for (long t: keysIn) {
			int pos = c(t,bitsF);
			xorInAssignation.get(pos).add(t);			
		}
		
		// Build each 1-bit filter with the elements of S and F that were assigned to it
		for (int i=0; i<bitsF; i++) {
			oneBitXors[i] = XorSimpleNRemover.construct(xorInAssignation.get(i), 
					xorOutAssignation.get(i), 1);
		}
	}
	
	@Override
	public boolean mayContain(long key) {
		// Check the key in the r-1 (nBits) filter and in the specific 1-bit filter it is mapped to 
		return originalXor.mayContain(key) && oneBitXors[c(key, oneBitXors.length)].mayContain(key);
	}
	
	@Override
	public long getBitCount() {
		// Return the number of bits
		return originalXor.getBitCount()+ oneBitXors.length*oneBitXors[0].getBitCount();
	}
	
    /**
     * Function that calculates the mapping of an element to a subfilter
     * @param x Element from S or F
     * @return subfilter that correspond to x
     */
	public int c(long key, int bitsF) {
		return (int)Math.abs((key>>6)%bitsF); 
	}
	
	
	/**
	 * Calculate the best a (additional bits for the first filter) to optimize area
	 * @param sSize Number of elements from S
	 * @param tSize Number of elements from T
	 * @param nBits (r-1) number of bits of the first filter before a is applied.
	 * @return The best value for a
	 */
	public static int getOptimizedAddedBits (int sSize, int tSize, int nBits) {
		int a = 0;
		int prob = (int) Math.ceil(tSize/Math.pow(2, nBits+a));
		while (2*sSize<prob) {
			a++;
			prob = (int) Math.ceil(tSize/Math.pow(2, nBits+a));
		}
		return a;
	}
}
