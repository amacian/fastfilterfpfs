package org.fastfilter.xor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.fastfilter.StringFilter;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Class with the two (n) filter (TF) composed by r-1 (nBits) bits of a first filter
 * and 1 (f) additional 1-bit filters. Adapted for strings.
 * @author Alfonso
 *
 */
public class XorSeveralStringFilters implements StringFilter{
	
	/** First filter with r-1 (nBits) bits for S*/
	private XorSimpleNString originalXor;
	/** 1-bit filter (or set of f filters) for S U F*/ 
	private XorSimpleNRemoverString[] oneBitXors;
	
    /** Hash for c(x), used to select the 1-bit filter */
	private HashFunction chf; 
	
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
    	for (XorSimpleNRemoverString x : oneBitXors) {
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
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsN Number of bits for the first filter (Filter 1)
     * @param bitsF Number of 1-bit additional filters (1 for TF)
     * @return a new Filter with the selected parameters
     */
    public static XorSeveralStringFilters construct(Set<String> keysIn, Set<String> keysOut, int bitsN, int bitsF) {
        return new XorSeveralStringFilters (keysIn, keysOut, bitsN, bitsF);
    }

    /**
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsN Number of bits for the first filter (Filter 1)
     * @param bitsF Number of 1-bit additional filters (1 for TF)
     */	
	XorSeveralStringFilters (Set<String> keysIn, Set<String> keysOut, int bitsN, int bitsF){
		// create the initial filter only for S (keysIn) and r-1 (bitsN) bits
		originalXor = XorSimpleNString.construct(keysIn, bitsN);
		// create as many 1-bit filters as parameter bitF
		oneBitXors = new XorSimpleNRemoverString[bitsF];
		// Parameter used to assign the elements of S to a specific 1-bit filter
		List<Set<String>> xorInAssignation = new ArrayList<Set<String>>();
		// Parameter used to assign the elements of F to a specific 1-bit filter
		List<Set<String>> xorOutAssignation = new ArrayList<Set<String>>();
		
		// Create the sets for each 1-bit filter
		for (long i=0; i<bitsF; i++) {
			Set<String> s = new HashSet<String>();
			xorInAssignation.add(s);
			s = new HashSet<String>();
			xorOutAssignation.add(s);
		}
		
        // keep track of false positives for debugging purposes
        int fp = 0;

        // Create the hash to distribute the strings into the subfilters.
        int cseed = new Random().nextInt();
        chf = Hashing.murmur3_128(cseed);
        
		// Find the false positives (F) of T and assign them to a specific 1-bit filter
		// For TF there is only one 1-bit filter so all false positives will be assigned to it.
		for (String t : keysOut) {
			if (originalXor.mayContain(t)) {
				// Calculate the 1-bit filter it corresponds to
				int pos = c(t,bitsF);
				// Assign the false positive to that 1-bit filter
				xorOutAssignation.get(pos).add(t);
				fp++;
			}
		}
		// System.out.println("Total number of false positives from T:"+fp);
		this.fppT = (100*fp)/(double)keysOut.size();
		// Assign each element of S to a specific 1-bit filter
		// For TF there is only one 1-bit filter so all elements will be assigned to it.
		for (String t: keysIn) {
			int pos = c(t,bitsF);
			xorInAssignation.get(pos).add(t);			
		}
		
		// Build each 1-bit filter with the elements of S and F that were assigned to it
		for (int i=0; i<bitsF; i++) {
			oneBitXors[i] = XorSimpleNRemoverString.construct(xorInAssignation.get(i), 
					xorOutAssignation.get(i), 1);
		}
	}
	
	@Override
	public boolean mayContain(String key) {
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
	public int c(String key, int bitsF) {
		long x = chf.hashBytes(key.getBytes()).asLong();
		return (int)Math.abs((x>>6)%bitsF); 
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
