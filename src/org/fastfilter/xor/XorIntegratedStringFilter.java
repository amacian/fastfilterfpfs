package org.fastfilter.xor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.fastfilter.StringFilter;
import org.fastfilter.utils.Hash;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/** 
 * Class with the integrated filter (IF) composed by r-1 (nBits) bits of a first filter
 * and f additional bits for 1-bit equivalent subfilters.
 * @author Alfonso
 *
 */
public class XorIntegratedStringFilter implements StringFilter {

	/** Hash function used in the filters to select positions */
    private HashFunction hf;
    /** Seed for c(x), used to select the 1-bit filter */
    private long cseed;
    
    /** stored fingerprints */
    private long[] data;
    /** lenght of the blocks used for each of the three hash function */
    int blockLength;
    /** Default total number of bits per fingerprint (r)*/
    int bitsPerFingerprint=8;
    /** Default number of bits used in the first filter Filter 1 (r-1) */
    int bitsN = 7;
    /** Default number of the 1-bit subfilters (f) */
    int bitsF = 1;
    /** Default mask to check the first r-1 bits (bitsN)*/
    long maskN = 0xFE;
    /** Default mask to check the 1-bit Xor filter */
    long maskF = 0x01;
       
    /** Percentage of false positives from T */
	double fppT = 0;
	
	/** Number of internal iterations to converge */
	int tries = 0;
	
	/**
	 * Get the actual probability of false positives produced by T
	 * @return false positive probability  
	 */
	public double getFppT() {
		return fppT;
	}


    /** Number of total bits stored in the filter */
    public long getBitCount() {
        return data.length * bitsPerFingerprint;
    }

    /** Method to construct the filter
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsN Number of bits for the first filter (Filter 1)
     * @param bitsF Number of 1-bit subfilters
     * @return a new Filter with the selected parameters
     */
    public static XorIntegratedStringFilter construct(Set<String> keysIn, Set<String> keysOut, int bitsN, int bitsF) {
        return new XorIntegratedStringFilter (keysIn, keysOut, bitsN, bitsF);
    }    
    
    /**
     * 
     * @param keysIn Set S with the elements that are added to the filter
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsN Number of bits for the first filter (Filter 1)
     * @param bitsF Number of 1-bit subfilters
     */
    XorIntegratedStringFilter(Set<String> keysIn, Set<String> keysOut, int bitsN, int bitsF) {
    	// The total number of bits is the sum of bits from the first filter and
    	// the bits used for the equivalent 1-bit subfilters.
    	int totalBits = bitsN+bitsF;
    	// With this implementation we can only have a maximum of 64 bits as we are
    	// using longs to store the information
    	if (totalBits<=64 && totalBits>0) {
    		// Set the values
    		this.bitsPerFingerprint = totalBits;
    		this.bitsN = bitsN;
    		this.bitsF = bitsF;
    		// The mask for the most significant bitsN bits 
    		this.maskN = (long)(Math.pow(2, bitsN)-1)<<bitsF;
    		// Mask for the last bitsF bits
    		this.maskF = (long)(Math.pow(2, bitsF)-1);
    	}else {
    		// >64 bits
    		System.out.println("Too many bits. r-1="+bitsN+"; f="+bitsF);
    		System.exit(1);
    	}

    	// S=keysIn, T=keysOut, r-1=nBits
    	// Size is |S|+|T|/2^(r-1) when only 1 subfilter bitsF=1
    	// Size is |S| when bitsF>1
    	int size = keysIn.size() + ((bitsF==1)?(int) Math.ceil(keysOut.size()/Math.pow(2, bitsN)):0);
    	// We increase the size by epsilon=0.23 and add delta=32 
    	float epsilon = 1.23f;
    	size = (int)(Math.ceil(epsilon * size) +32);
    	// Each block for the hash functions correspond to 1/3 of the total size
        blockLength = (int) Math.ceil(size / 3.0f);
        
        // Sets used to keep track of the assignation of elements from S (bitAssignationIn) 
        // and T (bitAssignationOut) to the different 1-bit subfilters
        List<Set<String>> bitAssignationIn = new ArrayList<Set<String>>();
        List<Set<String>> bitAssignationOut = new ArrayList<Set<String>>();
        
        // Create the sets for the f subfilters
        for (int i=0; i<bitsF;i++) {
        	bitAssignationIn.add(new HashSet<String>());
        	bitAssignationOut.add(new HashSet<String>());
        }
        
        // We will repeat this process until we are able to fill all the bits
        // using the same hash functions.
        boolean success=false;
        while (!success) {
            // Recreate data
        	
        	Random r = new Random();
        	data = r.longs(3*blockLength).toArray();
        	
            // Recreating the seed for c(x)
        	cseed = r.nextLong(); 
            
        	/* Populating first bitsN from the filter */        	
	        while (true) {
	        	tries++;
	        	// Seed for the hash functions
	            int seed = r.nextInt();
	            hf = Hashing.murmur3_128(seed); 
	            // Stack that will hold the pair element hash, selected position
	            long[] stack = new long[keysIn.size() * 3];
	            // Try to create a map for each s of S to a position i and add it to the stack
	            if (map(keysIn, null, hf, stack)) {
	            	// If it was possible to create the map, assign value to the
	            	// position so the fingerprint is equal to the xors of the hash positions 
	                assign(stack, data, this.maskN, false);
	                break;
	            }
	        }
	        // keep track of false positives for debugging purposes
	        int fp = 0;
	        // Check which elements of T belong to F (false positives)
			for (String t : keysOut) {
				// Compare only the most significant bitsN
				if (mayContain(t, this.maskN)) {
					// If it is a false positive, calculate the hash
					long hx = hf.hashBytes(t.getBytes()).asLong();
					// and identify which subfilter should be populated
					bitAssignationOut.get(c(hx)).add(t);
					// Increase the false positive info.
					fp++;
				}
			}
			this.fppT = (100*fp)/(double)keysOut.size();
			
			// For each key in S, identify which subfilter should be populated
			for (String t : keysIn) {
				long hx = hf.hashBytes(t.getBytes()).asLong();
				bitAssignationIn.get(c(hx)).add(t);				
			}
			
			// The hash functions used for bitsN should be used for the subfilters
			// It could be possible that no mapping could be done with those functions
			success = true;
			// For each subfilter
			for (int i=0; i<bitsF;i++) {
				// Retrieve the elements from S belonging to that subfilter
				Set<String> ins = bitAssignationIn.get(i);
				// Retrieve the elements from F belonging to that subfilter
				Set<String> outs = bitAssignationOut.get(i);
		        /* Populating last bitsF from the filter */
	            // Stack that will hold the pair element hash, selected position
				long[] stack = new long[(ins.size()+outs.size()) * 3];
	            // Try to create a map for each s of S, and t of F 
				// to a position i and add it to the stack
				if (map(ins, outs, hf, stack)) {
	            	// If it was possible to create the map, assign value to the
	            	// position so the fingerprint is equal/opposite to the xors of the 
					// hash positions 
					assign(stack, data, this.maskN, true);
		        	continue;
		        }else {
		        	// We could not perform the mapping with these hash functions
		        	// We need to start the process from the beginning
		        	System.out.println("The selected seed did not work for the last bits "+ i);
		        	success = false;
		        	break;
		        }
			}
			// Clear all the sets that assign elements to subfilters.
	        for (int i=0; i<bitsF;i++) {
	        	//System.out.println("Elements assigned to subfilter "+i+"="+(bitAssignationIn.get(i).size()+bitAssignationOut.get(i).size()));
	        	bitAssignationIn.get(i).clear();
	        	bitAssignationOut.get(i).clear();
	        }
        }
    }

    /**
     * Function that calculates the mapping of an element to a subfilter
     * @param x Element from S or F
     * @return subfilter that correspond to x
     */
	int c(long x) {
		return (int)Math.abs(Hash.hash64(x, cseed)%bitsF); 
	}
	
	/**
	 * Try to create a map for each s of S, and t of F to a position i and add it to the stack
	 * @param keysIn elements to be added to the filter and should return a positive
	 * @param keysOut elements to be added to the filter and should return a negative
	 * @param hf Hash function
	 * @param stack Stack where each element will be mapped to a unique position
	 * @param hashes Set that will store the hashes for keysIn for later purposes
	 * @return true if the map could be done, false otherwise.
	 */
    boolean map(Set<String> keysIn, Set<String> keysOut, HashFunction hf, long[] stack) {
    	// The total number of elements will be the sum of |S| and |F|
    	int size = keysIn.size() + ((keysOut==null)?0:keysOut.size());
    	// C and H will store the number of elements mapped to a hash and the corresponding h position
        int[] C = new int[3 * blockLength];
        long[] H = new long[3 * blockLength];
        int[] Cout = null;
        long[] Hout = null; 
        if (keysOut!=null) {
        	Cout = new int[3 * blockLength];
        	Hout = new long[3 * blockLength];
        }
        int[] Ctot = new int[3 * blockLength];

        // For each element in S
        for (String k : keysIn) {
        	// Calculate the base hash for 64 bits
            long x = hf.hashBytes(k.getBytes()).asLong();
            // Calculate the three hash functions ==> positions and add the values to C and H
            for (int j = 0; j < 3; j++) {
                int index = h(x, j);
                C[index]++;
                Ctot[index]++;
                H[index] ^= x;
            }
        }
        // For elements in F do the same process with Cout and Hout
        if (keysOut!=null) {
	        for (String k : keysOut) {
	            long x = hf.hashBytes(k.getBytes()).asLong();
	            for (int j = 0; j < 3; j++) {
	                int index = h(x, j);
	                Cout[index]++;
	                Ctot[index]++;
	                Hout[index] ^= x;
	            }
	        }
        }
        // Array that stores the positions with just one element
        int[] Q = new int[3 * blockLength];
        // next position to be filled
        int qi = 0;
        // Look for positions with only 1 element assigned
        for (int i = 0; i < Ctot.length; i++) {
            if (Ctot[i] == 1) {
            	// Add those with just one element to Q
                Q[qi++] = i;
            }
        }
        // index for next position to be used in the stack
        int si = 0;
        // Three values are stored per element, the element itself, the unique position assigned
        // and the indicator of including or excluding the element
        while (si < 3 * size) {
        	// It was not possible to cover all elements. At this point all the remaining
        	// positions have, at least, 2 or more elements.
        	if(qi==0) {
        		break;
        	}
        	// Take the next element from the stack
            int i = Q[--qi];
            // If that position is only assigned to 1 element
            if (Ctot[i] == 1) {
            	boolean in = (C[i]==1);
            	// Get the 64-bit hash from that position
            	long x = (in)?H[i]:Hout[i];
                // Assign the hash to the position including both in the stack 
                stack[si++] = x;
                stack[si++] = i;
                // indicate if it is to be included or excluded too
                stack[si++] = (in)?1:0;
                // Remove the element from every position
                for (int j = 0; j < 3; j++) {
                	int index = h(x, j);
                    Ctot[index]--;
                    if (in) {
                    	C[index]--;
                        H[index] ^= x;
                    }else {
                    	Cout[index]--;
                        Hout[index] ^= x;
                    }
                    // If, after removal, one of the positions is only assigned to 1 element 
                    if (Ctot[index] == 1) {
                    	// Add the position to the stack
                        Q[qi++] = index;
                    }
                }
            }
        }
        // Have all the elements a unique position assigned
        return si == 3 * size;
    }

    /**
     * Store the appropriate values into each position to get the correct positives and negatives
     * @param stack The stack with the information of the elements (64-bit hash) and the unique position
     * @param b The data array where the elements are to be stored
     * @param mask Bits to be set if filter = false
     * @param filter If true, calculate the mask for the subfilter and use that
     */
    void assign(long[] stack, long[] b, long mask, boolean filter) {
    	// Optimize pow calculation performing it just once and storing it in an array
    	long[] pows = new long[bitsF];
    	// Only required when masking for one of the additional subfilters
    	if (filter) {
    		for (int j=0; j< bitsF;j++) {
    			pows[j] = (long) Math.pow(2, j);
    		}
    	}    	
    	// Traverse the stack
        for(int stackPos = stack.length; stackPos > 0;) {
        	boolean in = stack[--stackPos]==1;
        	// Get the position assigned
            int index = (int) stack[--stackPos];
            // Get the element (its 64-bit hash) assigned to that position
            long x = stack[--stackPos];
            // Depending on the filter parameter, use the default mask or the one for
            // the specific filter
            long nmask = (filter)?pows[c(x)]:mask;
            // When the element is in the positive set, assign the fingerprint, 
            // otherwise, assign its opposite value
            long stored = in?fingerprint(x, nmask):(fingerprint(x, nmask)^nmask);
            // Include the XOR of the three positions and the stored value in the selected
            // position for the bits corresponding to the mask.
            // First clear the bits that correspond to the mask selecting only the opposite ones. 
            b[index] &=~nmask;
            // We fill those bits with the XOR. The rest stay as they were.
            b[index] |= (stored ^ b[h(x, 0)] ^ b[h(x, 1)] ^ b[h(x, 2)])& nmask ;
        }
    }

    /**
     * Calculate one of the positions corresponding to an element based on the hash
     * @param x 64-bit hash of the element
     * @param index Which of the n hash positions to be retrieved.
     * @return The position in the array
     */
    int h(long x, int index) {
        return Hash.reduce((int) Long.rotateLeft(x, index * 21), blockLength) + index * blockLength;
    }

    /**
     * Check if the key is in the filter
     * @param key Key to be checked
     * @return if it gives a positive
     */
    @Override
    public boolean mayContain(String key) {
    	// Create the 64-bit hash
    	long x = hf.hashBytes(key.getBytes()).asLong();
    	// Generate the complete mask calculating the subfilter
    	long mask = maskN | (long)Math.pow(2,c(x)); // TODO: can be optimized storing pows as in the non string
    	// Call the equivalent function with the mask
    	return mayContain(key, mask);
    }

    /**
     * Check if the key is in the filter using a subset of the stored bits
     * @param key Key to be checked
     * @param mask Which bits from the filter are to be used
     * @return if it gives a positive
     */
    boolean mayContain(String key, long mask) {
    	// Create the 64-bit hash
    	long x = hf.hashBytes(key.getBytes()).asLong();
    	// Check the subset of bits from the fingerprint
        return fingerprint(x, mask) == ((data[h(x, 0)] ^ data[h(x, 1)] ^ data[h(x, 2)])&mask);
    }
    
    // Generate the fingerprint for a specific mask
    private long fingerprint(long x, long mask) {
        return x & mask;
    }

    /**
     * Internal iterations to converge
     * @return
     */
    public int getTries() {
		return tries;
	}

    /**
     * Percentage of tries that failed compared to the total
     * @return
     */
    public double getPercentFails() {
    	int totalTries = getTries();
		// Only one filter has been filled.
		return (totalTries-1)/(double)totalTries;
    }
    
	/**
	 * Calculate the best a (additional bits for the first filter) to optimize area
	 * when f=1
	 * @param sSize Number of elements from S
	 * @param tSize Number of elements from T
	 * @param nBits (r-1) number of bits of the first filter before a is applied.
	 * @return The best value for a
	 */
	public static int getOptimizedAddedBitsF1 (int sSize, int tSize, int nBits) {
		int a = 0;
		int prob = (int) Math.ceil(tSize/Math.pow(2, nBits+a)*(nBits+a));
		while (2*sSize<prob) {
			a++;
			prob = (int) Math.ceil(tSize/Math.pow(2, nBits+a)*(nBits+a));
		}
		return a;
	}

}
