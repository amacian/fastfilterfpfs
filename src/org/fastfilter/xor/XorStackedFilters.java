package org.fastfilter.xor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.fastfilter.Filter;

/** Cascade of XOR filters **/
public class XorStackedFilters implements Filter {

	/** First filter to store the True positives **/
	private XorSimpleN first;
	
	/** Rest of the filters*/ 
	ArrayList<XorSimpleN> xors = new ArrayList<XorSimpleN>();

	/** Actual probability of false positives produced by T */
	double fppT = 0;

	/**
	 * Get the actual probability of false positives produced by T
	 * @return false positive probability  
	 */
    public double getFppT() {
		return fppT;
	}

	/** Method to construct the filter
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsFirst Number of bits for the first filter (Filter 1)
     * @param bitsRest Number of bits for the rest of the stacked filters
     * @return a new Filter with the selected parameters
     */
    public static XorStackedFilters construct(Set<Long> keysIn, Set<Long> keysOut, int bitsFirst, int bitsRest) {
        return new XorStackedFilters (keysIn, keysOut, bitsFirst, bitsRest);
    } 
    
    /**
     * Constructor
     * @param keysIn Set S with the elements that are added to the filter 
     * @param keysOut Set T with the elements that must return a negative match
     * @param bitsFirst Number of bits for the first filter (Filter 1)
     * @param bitsRest Number of bits for the rest of the stacked filters
     */
    private XorStackedFilters(Set<Long> keysIn, Set<Long> keysOut, int bitsFirst, int bitsRest) {
    	first = XorSimpleN.construct(keysIn, bitsFirst);
    	
    	Set<Long> fps = new HashSet<Long>();
    	int fp=0;
		for (long t : keysOut) {
			if (first.mayContain(t)) {
				fps.add(t);
				fp++;
			}
		}
		// System.out.println("Total number of false positives from T:"+fp);
		this.fppT = (100*fp)/(double)keysOut.size();
		this.createNextFilter(fps, bitsRest, keysIn);
	}

    /**
     * Method to create the stacked filters apart from the first one
     * @param elements Elements to be included in this level
     * @param bits Bits of the filter
     * @param fpfs Elements where the filter should return a negative 
     */
    private void createNextFilter(Set<Long> elements, int bits, Set<Long> fpfs) {
    	if (elements == null || elements.size()==0) {
    		return;
    	}
    	XorSimpleN filter = XorSimpleN.construct(elements, bits);
    	xors.add(filter);
    	
    	Set<Long> fps = new HashSet<Long>();
		for (long t : fpfs) {
			if (filter.mayContain(t)) {
				fps.add(t);
			}
		}
		this.createNextFilter(fps, bits, elements);
    }
    

    
	@Override
	public boolean mayContain(long key) {
		// Get the result from the initial filter
		boolean initial = first.mayContain(key);
		// IF false, it is a true negative
		if (!initial) {
			return false;
		}
		// Otherwise, check the stacked filters
		// If a negative is found, what will be the result?
		boolean isPositive = true;
		// Traverse the filters until a negative is found
		for (XorSimpleN filter: xors) {
			boolean result = filter.mayContain(key);
			// If a negative is found, return if it is positive
			if (!result) {
				return isPositive;
			}
			// For next filter, a negative results means the opposite.
			isPositive = !isPositive;
		}
		return isPositive;
	}

	@Override
	public long getBitCount() {
		// Return the number of bits
		long total = first.getBitCount();
		for (XorSimpleN filter: xors) {
			total+=filter.getBitCount();
		}
		return total;

	}

	public double getNStacked() {
		return xors.size()+1;
	}


}
