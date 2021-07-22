package org.fastfilter.xor.tester;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Set;

import org.fastfilter.StringFilter;
import org.fastfilter.xor.XorIntegratedFilter;
import org.fastfilter.xor.XorIntegratedStringFilter;
import org.fastfilter.xor.XorSeveralFilters;
import org.fastfilter.xor.XorSeveralStringFilters;
import org.fastfilter.xor.XorSimpleNRemoverString;
import org.fastfilter.xor.XorSimpleNString;

/**
 * Class to validate the Birkberk use case
 * @author Alfonso
 *
 */
public class ValidateBirkberk {

	private static final String INSYMBOL = "$";
	public static void main(String[] args) {
		String filename = "./data/Birkberk_missp.dat";
		// Bits of Filter 1 (r-1)

		Set<String> s = new HashSet<String>();
		Set<String> t = new HashSet<String>();
		//FileWriter fw = null;
		LineNumberReader lnr = null;
		
		int nBits=7; //r-1
		int repeated = 0;
		
		//Birbeck 
		try {
			lnr = new LineNumberReader(new FileReader (filename));
			String line = null;
			while((line=lnr.readLine())!=null) {
				if (line.startsWith(INSYMBOL)) {
					line = line.substring(1);
					s.add(line);
				}else {
					int size =t.size();
					t.add(line);
					if(t.size()==size) {
						repeated++;						
					}
				}
			}
			System.out.println(repeated + " elements repeated as misspelled for different words.");
			Set<String> intersection = new HashSet<String>(s);
			intersection.retainAll(t);
			int bothSets=0;
			for (String str: intersection) {
				bothSets++;
				//System.out.println(str + " found as valid and misspelled, removed from valid");
				t.remove(str);
			}
			System.out.println(bothSets + " elements found as valid and misspelled, removed from misspelled.");

		} catch (IOException e) {
			System.out.println("Error reading file " + e.getMessage());
			System.exit(1);
		}

		System.out.println("------------------");
		System.out.println("Birkberk validation");
		System.out.println("------------------");
		System.out.println("Correct words: "+s.size());
		System.out.println("Misspelled words: " +t.size());
		try {
			lnr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Näive Filter");
		XorSimpleNRemoverString xsnr = XorSimpleNRemoverString.construct(s, t, nBits+1);
		System.out.println("Size of Näive: "+xsnr.getBitCount());
		checkFilter(xsnr, s,t);

		System.out.println();
		System.out.println("Two Filters");
		// Calculate best A value for the True filter
		int optimizedTFA = XorSeveralFilters.getOptimizedAddedBits(s.size(), t.size(), nBits);
		// Two filters with optimized A			
		XorSeveralStringFilters xsf = XorSeveralStringFilters.construct(s, t, nBits+optimizedTFA, 1);
		System.out.println("Size of TF with a="+optimizedTFA+": "+xsf.getBitCount());
		checkFilter(xsf, s,t);
		
		System.out.println();
		System.out.println("Integrated Filters with f=1");
		// Calculate best A value for the Integrated filter with 1 subfilter (f=1)
		int optimizedIF1 = XorIntegratedFilter.getOptimizedAddedBitsF1(s.size(), t.size(), nBits);
		XorIntegratedStringFilter xi = XorIntegratedStringFilter.construct(s, t, optimizedIF1+nBits,1);
		System.out.println("Size of IF with f=1, a="+optimizedIF1+": "+xi.getBitCount());
		checkFilter(xi, s,t);

		System.out.println();
		System.out.println("Integrated Filters with f=2");
		double prob = t.size()/Math.pow(2, nBits);
		// Calculate f as f>=(|S|+|F|)/|S|   as we want |S|>=(|S|+|F|)/f 
		int f = 2;//(int) Math.ceil((sSet.size()+prob)/sSet.size());
		int a = (int) Math.ceil(Math.log(prob/s.size())/Math.log(2));
		if(a<0) {a=0;}
		xi = XorIntegratedStringFilter.construct(s, t, nBits+a, f);
		System.out.println("Size of IF with f=2, a="+a+": "+xi.getBitCount());
		checkFilter(xi, s,t);
		
		System.out.println();
		System.out.println("Testing false positives in original filter");
		// storing false positives and false negatives for the filter
		int[] totals = new int[2]; 
		int[] max = new int[2];
		int runs = 1000;
		for (int i=0; i<runs;i++) {
			XorSimpleNString xss = XorSimpleNString.construct(s, nBits+1);
			System.out.println("Size of OF: "+xss.getBitCount());
			int[] res = checkFilter(xss, s,t);
			totals[0]+=res[2];
			totals[1]+=res[3];
			if(res[2]>max[0]) {
				max[0]=res[2];
			}
			if(res[3]>max[1]) {
				max[1]=res[3];
			}
		}
		System.out.println("Average after "+runs+" executions--> FP: "+totals[0]/(double)runs+"; FN: "+totals[1]/(double)runs);
		System.out.println("Maximum after "+runs+" executions--> FP: "+max[0]+"; FN: "+max[1]);
		
	}

	public static int[] checkFilter(StringFilter sf, Set<String> testIn, Set<String> testOut) {
		int tp=0;
		int fp=0;
		int tn=0;
		int fn=0;
		for (String str: testIn) {
			if (sf.mayContain(str)) {
				tp++;
			}else {
				fn++;
			}
		}
		for (String str: testOut) {
			if (sf.mayContain(str)) {
				fp++;
			}else {
				tn++;
			}
		}
		System.out.println("TP: "+tp+"; TN: "+ tn + "; FP: "+fp+"; FN: "+fn);
		int[] res= {tp, tn, fp, fn};
		return res;
	}

}
