package org.fastfilter.xor;

import java.util.Random;
import java.util.Set;

import org.fastfilter.Filter;
import org.fastfilter.utils.Hash;

/**
 * Class that implements the naive XOR filter with elements to be match and a
 * false positive free set. It calculates its size based on the sum of T and S.
 * @author Alfonso
 *
 */
public class XorSimpleNRemover implements Filter {

	/** Seed for the hash functions used in the filters */
    private long seed;
    /** stored fingerprints */
    private long[] data;
    /** lenght of the blocks used for each of the three hash function */
    int blockLength;
    /** Default total number of bits per fingerprint (r)*/
    int bitsPerFingerprint=8;
    /** Default mask to check the bits (bitsN)*/
    long mask = 0xFF;
	/** Number of iterations to converge */
	private int tries;

    /** Number of total bits stored in the filter */
    public long getBitCount() {
        return data.length * bitsPerFingerprint;
    }

    /** Method to construct the filter
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsPerFingerprint Number of bits for each entry of the filter
     * @param tSize If a specific size different from the calculated one is preferred
     * @return a new Filter with the selected parameters
     */
    public static XorSimpleNRemover construct(Set<Long> keysIn, Set<Long> keysOut, int bitsPerFingerprint, int tSize) {
        return new XorSimpleNRemover (keysIn, keysOut, bitsPerFingerprint, tSize);
    }
    
    /** Method to construct the filter
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsPerFingerprint Number of bits for each entry of the filter
     * @return a new Filter with the selected parameters
     */
    public static XorSimpleNRemover construct(Set<Long> keysIn, Set<Long> keysOut, int bitsPerFingerprint) {
        return new XorSimpleNRemover (keysIn, keysOut, bitsPerFingerprint);
    }    

    /** 
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsPerFingerprint Number of bits for each entry of the filter
     * @return a new Filter with the selected parameters
     */ 
    XorSimpleNRemover(Set<Long> keysIn, Set<Long> keysOut, int bitsPerFingerprint) {
    	this(keysIn, keysOut, bitsPerFingerprint, ((int)(1.23 * (keysIn.size() + keysOut.size())+32)));
    }
    
    /** 
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsPerFingerprint Number of bits for each entry of the filter
     * @param tSize If a specific size different from the calculated one is preferred
     * @return a new Filter with the selected parameters
     */
    XorSimpleNRemover(Set<Long> keysIn, Set<Long> keysOut, int bitsPerFingerprint, int tSize) {
    	// With this implementation we can only have a maximum of 64 bits as we are
    	// using longs to store the information
    	if (bitsPerFingerprint<=64 && bitsPerFingerprint>0) {
    		this.bitsPerFingerprint = bitsPerFingerprint;
    		this.mask = (long)(Math.pow(2, bitsPerFingerprint)-1);
    	}
    	// Each block for the hash functions correspond to 1/3 of the total size
        blockLength = tSize/3;
        // Recreate data
    	Random r = new Random();
    	data = r.longs(3*blockLength).toArray();
    	
    	/* Populating the filter */
        while (true) {
        	tries++;
        	// Seed for the hash functions
            seed = r.nextLong();
            // Stack that will hold the pair element hash, selected position
            long[] stack = new long[(keysIn.size()+keysOut.size()) * 3];
            // Try to create a map for each s of S to a position i and add it to the stack
            if (map(keysIn, keysOut, seed, stack)) {
            	// If it was possible to create the map, assign value to the
            	// position so the fingerprint is equal/different to the xors of the hash positions 
                assign(stack, data);
                return;
            }
            // If after 10 tries it does not work, give up
            if (tries>10) {
            	System.out.println("+10 iterations completed"); //TODO: Change to a configurable number
            	return;
            }
        }
    }

    /**
     * Internal iterations to converge
     * @return
     */
    public int getTries() {
		return tries;
	}
    
	/**
	 * Try to create a map for each s of S, and t of F to a position i and add it to the stack
	 * @param keysIn elements to be added to the filter and should return a positive
	 * @param keysOut elements to be added to the filter and should return a negative
	 * @param seed Seed for the hash functions
	 * @param stack Stack where each element will be mapped to a unique position
	 * @return true if the map could be done, false otherwise.
	 */    
    boolean map(Set<Long> keysIn, Set<Long> keysOut, long seed, long[] stack) {
    	// The total number of elements will be the sum of |S| and |F|
    	int size = keysIn.size() + keysOut.size();
    	// C and H will store the number of elements mapped to a hash and the corresponding h position
    	// for the keys from keysIn
        int[] C = new int[3 * blockLength];
        long[] H = new long[3 * blockLength]; 
    	// Cout and Hout will store the number of elements mapped to a hash and the corresponding h position
    	// for the keys from keysOut
        int[] Cout = new int[3 * blockLength];
        long[] Hout = new long[3 * blockLength];
        // Used as the sum of C and Cout
        int[] Ctot = new int[3 * blockLength];  
        
        // For each element in S
        for (long k : keysIn) {
        	// Calculate the base hash for 64 bits
            long x = Hash.hash64(k, seed);
            // Calculate the three hash functions ==> positions and add the values to C and H
            for (int j = 0; j < 3; j++) {
                int index = h(x, j);
                C[index]++;
                Ctot[index]++;
                H[index] ^= x;
            }
        }
        // For elements in F do the same process with Cout and Hout
        for (long k : keysOut) {
            long x = Hash.hash64(k, seed);
            for (int j = 0; j < 3; j++) {
                int index = h(x, j);
                Cout[index]++;
                Ctot[index]++;
                Hout[index] ^= x;
            }
        }
        // Array that stores the positions with just one element
        int[] Q = new int[3 * blockLength];
        // next position to be filled
        int qi = 0;
        // Look for positions with only 1 element assigned
        for (int i = 0; i < C.length; i++) {
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
            	// Check if the element is from keysIn (S)
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
     */
    void assign(long[] stack, long[] b) {
    	// Traverse the stack
        for(int stackPos = stack.length; stackPos > 0;) {
        	boolean in = stack[--stackPos]==1;
        	// Get the position assigned
            int index = (int) stack[--stackPos];
            // Get the element (its 64-bit hash) assigned to that position
            long x = stack[--stackPos];
            // When the element is in the positive set, assign the fingerprint, 
            // otherwise, assign its opposite value
            long stored = in?fingerprint(x):(fingerprint(x)^mask);
            // Include the XOR of the three positions and the stored value in the selected
            // position for the bits corresponding to the mask.
            b[index] = 0;
            b[index] = (stored ^ b[h(x, 0)] ^ b[h(x, 1)] ^ b[h(x, 2)]) ;
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
    public boolean mayContain(long key) {
        long x = Hash.hash64(key, seed);
        return fingerprint(x) == ((data[h(x, 0)] ^ data[h(x, 1)] ^ data[h(x, 2)]) & mask);
    }

    // Generate the fingerprint for a specific mask
    private long fingerprint(long x) {
        return x & mask;
    }

}
