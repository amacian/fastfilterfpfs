package org.fastfilter.xor.tester;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.fastfilter.Filter;
import org.fastfilter.xor.XorIntegratedFilter;
import org.fastfilter.xor.XorIntegratedFilterTByFile;
import org.fastfilter.xor.XorSeveralFilters;
import org.fastfilter.xor.XorSeveralFiltersTByFile;

/**
 * Class to test the Bitcoin SPV use case
 * @author Alfonso
 *
 */
public class BitcoinSPVMemoryTester {

	public static void main(String[] args) throws IOException, FileNotFoundException{
		int iters=100;
		// Size of S
		int sSize = 2500;
		// Size of T
		int[] tSizeSet = {20000000, 700000000};//{20000000};//, 700000000};
		int[] nBitsSet = {7}; //{3, 7, 15};
		// Bits of Filter 1 (r-1)
		
		int delta = 32;
		
		String filename="random.txt";

		for (int nBits: nBitsSet) {
			for (int tSize: tSizeSet) {
				float[] totalMemory = new float[7];
				for (int i=0; i<iters;i++) {
					System.out.println("-------------------------------------");
					System.out.println("|S|="+sSize+"; |T|="+tSize+"; r-1="+nBits);
					// Size of a third set that is disjoint from S and T
					int oSize=100000;
					
					// Generate the set S
					Set<Long> sSet = build(sSize, null);
					System.out.println("sSet built");
					// Generate the set T writing it to a file
					BitcoinSPVMemoryTester.generateTFile(tSize, filename, sSet);
					System.out.println("tSet built");
			
					// Calculate the number of false positives from T ==> |F|~|T|/2^(r-1)
					System.out.println("fpp="+1/Math.pow(2, nBits));
					double prob = tSize/Math.pow(2, nBits);
					System.out.println("Expected number of false positives: "+ (int)Math.ceil(prob));
					// Calculate f as f>=(|S|+|F|)/|S|   as we want |S|>=(|S|+|F|)/f 
					int f = 2;//(int) Math.ceil((sSet.size()+prob)/sSet.size());
					//int f2 = (int) Math.ceil((sSet.size()+prob)/sSet.size());
					int a = (int) Math.ceil(Math.log(prob/sSet.size())/Math.log(2));
					if(a<0) {a=0;}
					System.out.println("Expected number of false positives with a="+a+": "+ (int)Math.ceil(tSize/Math.pow(2, nBits+a)));
					System.out.println("Estimated size for naive approach is "+ (int)Math.ceil((1.23*(sSize+tSize)+delta)*(nBits+1)));
					System.out.println("Estimated size for TF approach is "+ Math.ceil(((1.23*sSize+delta)*nBits + 1.23*(sSize+prob+delta))));
					int optimizedTFA = XorSeveralFilters.getOptimizedAddedBits(sSize, tSize, nBits);
					double prob2 = tSize/Math.pow(2, nBits+optimizedTFA);
					System.out.println("Estimated size for TF approach with a="+optimizedTFA+" is "+ Math.ceil(((1.23*sSize)+delta)*(nBits+optimizedTFA) + 1.23*(sSize+prob2+delta)));
					System.out.println("Estimated size for IF with f="+f+ " and a="+a+" is "+ Math.ceil((1.23*sSize)+delta)*(f+nBits+a));
					System.out.println("Estimated size for IF with f=1 is " + (int)Math.ceil((1.23*(sSet.size()+prob)+delta)*(nBits+1)));
					int optimizedIF1 = XorIntegratedFilter.getOptimizedAddedBitsF1(sSize, tSize, nBits);
					double prob3 = tSize/Math.pow(2, nBits+optimizedIF1);
					System.out.println("Estimated size for IF with f=1 and a="+optimizedIF1+" is " + (int)Math.ceil((1.23*(sSet.size()+prob3)+delta)*(nBits+optimizedIF1+1)));
					if(sSize*(f+nBits) > Math.ceil((sSet.size()+prob)*(nBits+1))){
						f=1;
					}
			
					/*System.out.println("-------------------------------------");
					System.out.println("First test: Naive filter");
					XorSimpleNRemover xsnr = XorSimpleNRemover.construct(sSet, tSet, nBits+1);
					System.out.println("Testing Naive filter");*/				
					Set<Long> exclude = new HashSet<Long>();
					exclude.addAll(sSet);
					//exclude.addAll(tSet); //TODO
					Set<Long> oSet = build(oSize, exclude);		
					/*BitcoinSPVMemoryTester.checkFilter(xsnr, sSet, tSet, oSet);
					totalMemory[0]+=xsnr.getBitCount();
					xsnr=null;*/

					LineNumberReader lnr = new LineNumberReader(new FileReader(filename));
					System.out.println("TF without 'a' optimization");
					XorSeveralFiltersTByFile xsf = XorSeveralFiltersTByFile.construct(sSet, lnr, tSize, nBits, 1);
					lnr.close();
					lnr = new LineNumberReader(new FileReader(filename));
					BitcoinSPVMemoryTester.checkFilter(xsf, sSet, lnr, tSize, oSet);
					totalMemory[1]+=xsf.getBitCount();
					lnr.close();
					
					lnr = new LineNumberReader(new FileReader(filename));
					System.out.println("TF with 'a' optimization");
					xsf = XorSeveralFiltersTByFile.construct(sSet, lnr, tSize, nBits+optimizedTFA, 1);
					lnr.close();
					lnr = new LineNumberReader(new FileReader(filename));
					BitcoinSPVMemoryTester.checkFilter(xsf, sSet, lnr, tSize, oSet);
					totalMemory[2]+=xsf.getBitCount();
					xsf=null;

					lnr = new LineNumberReader(new FileReader(filename));
					System.out.println("IF with just 1 bit");
					XorIntegratedFilterTByFile xi = XorIntegratedFilterTByFile.construct(sSet, lnr, tSize, nBits, 1);
					lnr.close();
					lnr = new LineNumberReader(new FileReader(filename));
					BitcoinSPVMemoryTester.checkFilter(xi, sSet, lnr, tSize, oSet);
					totalMemory[3]+=xi.getBitCount();
					lnr.close();

					lnr = new LineNumberReader(new FileReader(filename));
					System.out.println("IF with just 1 bit and 'a' optimization");
					xi = XorIntegratedFilterTByFile.construct(sSet, lnr, tSize, nBits+optimizedIF1, 1);
					lnr.close();
					lnr = new LineNumberReader(new FileReader(filename));
					BitcoinSPVMemoryTester.checkFilter(xi, sSet, lnr, tSize, oSet);
					totalMemory[4]+=xi.getBitCount();
					lnr.close();

					lnr = new LineNumberReader(new FileReader(filename));
					System.out.println("Seventh test: IF with 'a' optimization");
					xi = XorIntegratedFilterTByFile.construct(sSet, lnr, tSize, nBits+a, f);
					lnr.close();
					lnr = new LineNumberReader(new FileReader(filename));
					BitcoinSPVMemoryTester.checkFilter(xi, sSet, lnr, tSize, oSet);
					totalMemory[5]+=xi.getBitCount();
					lnr.close();
				}
				System.out.println("Mean of");
				System.out.println("r-1;Naive;TF;TFa;IF2a;IF1;IF1a");
				System.out.println(nBits+";"+(int)(totalMemory[0]/(double)iters)+";"+totalMemory[1]/(double)iters+";"+
									totalMemory[2]/(double)iters+";"+(int)(totalMemory[3]/(double)iters)+";"+
									totalMemory[4]/(double)iters+";"+totalMemory[5]/(double)iters
									);
			}
		}

	}
	

	private static void checkFilter(Filter xi, Set<Long> sSet, LineNumberReader lnr, int tSize, Set<Long> oSet) throws NumberFormatException, IOException {
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
		String nextLine;
		while ((nextLine = lnr.readLine())!=null) {
			// Compare only the most significant bitsN
			Long s = Long.parseLong(nextLine);
			if (!xi.mayContain(s)) {
				totalTN++;
			}
		}
				

		// This should be 100%
		double TNFromTotal = Math.round(totalTN/(float)tSize*10000)/100.0;
		System.out.println("% negatives from T set: "+TNFromTotal +"%");
		System.out.println("Total negatives from T set: "+totalTN);

		totalTN=0;
		
		// Check all elements from O
		/*for (long o: oSet ) {
			if (!xi.mayContain(o)) {
				totalTN++;
			}
		}
		// This should be ~ |O|/(2^r) 
		TNFromTotal = Math.round(totalTN/(float)oSet.size()*10000)/100.0;
		System.out.println("% negatives from disjoint set: "+TNFromTotal +"%");
		System.out.println("Total negatives from disjoint set: "+totalTN);*/
		
		System.out.println("-------------------------------------");
		
	}


	public static Set<Long> build (int elements, Set<Long> exclude) {
		Set<Long> s = new HashSet<Long>();
		Random random = new Random();
		int pending = elements;
		do{
			s.addAll(getMore(pending, random));
			if(exclude!=null) {
				s.removeAll(exclude);
			}
			pending = elements-s.size();
		}while(pending>0);
		return s;
	}
	
	/*public static Set<Long> buildFromFile (int elements, Set<Long> exclude, String filename) {
		Set<Long> s = new HashSet<Long>();
		try {
			int read = 0;
			LineNumberReader lnr = new LineNumberReader(new FileReader(filename));
			do {
				for (;read<elements;read++) {
					s.add(Long.parseLong(lnr.readLine()));
					if (read%10000000==0) {
						System.out.println (read + "read.");
					}
				}
				System.out.println ("All read.");
				s.removeAll(exclude);
				System.out.println ("Excluded.");
				read=s.size();
			} while (read<elements);
			System.out.println ("built.");
			lnr.close();
		}catch(Exception e) {
		}
		return s;
	}*/
	
	private static Set<Long> getMore(int pending, Random random) {
		return random.longs(pending).boxed().collect(Collectors.toSet());
	}

	private static void generateTFile(int elements, String filename, Set<Long> exclude) throws IOException{
		PrintWriter pw = new PrintWriter(new FileWriter(filename));
		
		Random random = new Random();
		for (int i=0; i< elements; i++) {
			long next = random.nextLong();
			if (!exclude.contains(next)) {
				pw.println(next);
				if (i%10000000==0) {
					System.out.println(i);
					pw.flush();
				}
			}
		}
		
		pw.close();
	}
}
