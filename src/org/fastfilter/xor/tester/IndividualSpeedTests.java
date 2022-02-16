package org.fastfilter.xor.tester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.fastfilter.Filter;
import org.fastfilter.xor.XorIntegratedFilter;
import org.fastfilter.xor.XorSeveralFilters;
import org.fastfilter.xor.XorSimpleN;
import org.fastfilter.xor.XorStackedFilters;

import com.google.common.collect.Sets;

public class IndividualSpeedTests {

	public static void main(String[] args) {
		Set<String> types = new HashSet<String>();
		types.add("OF");
		types.add("TF");
		types.add("IF1");
		types.add("IF2");
		types.add("SF");
		if (args.length<4){
			System.out.println("Incorrect number of arguments");
			System.exit(1);
		}
		String type = args[0];
		if(!types.contains(type)){
			System.out.println("Type not supported: "+type);
			System.exit(1);
		}
		// Size of S
		int sSize = 100000;
		String header =	"Type;TSize;r-1;Creation;CheckPos;CheckNeg;A;Second\n";
		// Size of T
		int tSize = 10000;
		int nBits = 3;
		int otherBits = 1;
		String filename = "speed_details.log";

		boolean printHeader = !(new File(filename)).exists();
		try{
			sSize = Integer.parseInt(args[1]);
			tSize = Integer.parseInt(args[2]);
			nBits = Integer.parseInt(args[3]);
		}catch (NumberFormatException nfe){
			System.out.println("Error in the format of the arguments");
			System.exit(1);
		}
		
		if(args.length>4) {
			otherBits = Integer.parseInt(args[4]);
		}
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(filename, true);
		} catch (IOException e) {
			System.out.println("Error creating file " + e.getMessage());
			System.exit(1);
		}
		
		try {
			if (printHeader){
				fw.write(header);
			}
		}catch(Exception e) {
			
		}
		System.out.println("-------------------------------------");
		System.out.println("|S|="+sSize+"; |T|="+tSize+"; r-1="+nBits);

		// Calculate the number of false positives from T ==> |F|~|T|/2^(r-1)
		double prob = tSize/Math.pow(2, nBits);
		// Calculate f as f>=(|S|+|F|)/|S|   as we want |S|>=(|S|+|F|)/f 
		int f = 2;//(int) Math.ceil((sSet.size()+prob)/sSet.size());
		int a = (int) Math.ceil(Math.log(prob/sSize)/Math.log(2));
		if(a<0) {a=0;}

		// Calculate best A value for the TF: Two filters
		int optimizedTFA = XorSeveralFilters.getOptimizedAddedBits(sSize, tSize, nBits);
				
		// Calculate best A value for the Integrated filter with 1 subfilter (f=1)
		int optimizedIF1 = XorIntegratedFilter.getOptimizedAddedBitsF1(sSize, tSize, nBits);

		// Generate the set S
		Set<Long> sSet = build(sSize, null);
		// Generate the set T
		Set<Long> tSet = build(tSize, sSet);
		
		Set<Long> oSet = build(sSize, null);
		
		Filter fil = null;

		long init = 0;
		long end = 0;

		int selectedA = 0;

		if (type.equals("OF")){
			init = System.nanoTime();
			fil=XorSimpleN.construct(sSet, nBits+1);
			end = System.nanoTime();
		}else if (type.equals("TF")){
			init = System.nanoTime();
			fil=XorSeveralFilters.construct(sSet, tSet, nBits+optimizedTFA, 1);
			end = System.nanoTime();
			selectedA = optimizedTFA;
		}else if (type.equals("IF1")){
			init = System.nanoTime();
			fil=XorIntegratedFilter.construct(sSet, tSet, nBits+optimizedIF1, 1);
			end = System.nanoTime();
			selectedA = optimizedIF1;
		}else if (type.equals("IF2")){
			init = System.nanoTime();
			fil=XorIntegratedFilter.construct(sSet, tSet, nBits+a, f);
			end = System.nanoTime();
			selectedA = a;
		}else if (type.equals("SF")) {
			init = System.nanoTime();
			fil=XorStackedFilters.construct(sSet, tSet, nBits+optimizedTFA, otherBits);
			end = System.nanoTime();
			selectedA = optimizedTFA;			
		}
		long timeCreate=end-init;

		Set<Long> union =Sets.union(oSet, sSet);
		ArrayList<Long> l = new ArrayList<Long>(union);
		Collections.shuffle(l);
		double[] times = IndividualSpeedTests.checkFilter(fil, l);
		double timeCheckpositive = times[0];
		double timeChecknegative = times[1];


		
		try {
			
			fw.write(type+";"+tSize+";"+nBits+";"+(timeCreate)+";"+
					(timeCheckpositive)+";"+
					(timeChecknegative)+";"+
					selectedA+";"+((type.equals("SF")?otherBits:0)));
			fw.write("\n");
			fw.flush();
			fw.close();
		}catch(Exception e){
				
		}

	}
	
	private static double[] checkFilter(Filter xi, Collection<Long> set) {
		long init = 0;
		long end = 0;
		double accPos = 0;
		double accNeg = 0;
		int totalPos = 0;
		int totalNeg = 0;
		for (long s: set ) {
			init=System.nanoTime();
			boolean res = xi.mayContain(s);
			end=System.nanoTime();
			if(res) {
				accPos+= end-init;
				totalPos++;
			}else {
				accNeg+= end-init;
				totalNeg++;
			}
		}
		System.out.println(accPos+":"+totalPos);
		System.out.println(accNeg+":"+totalNeg);
		double[] times = {(accPos/(double)totalPos), (accNeg/(double)totalNeg)};
		return times;
		
	}


	public static Set<Long> build (int elements, Set<Long> exclude) {
		Set<Long> s = new HashSet<Long>();

		Random random = new Random();
		//SecureRandom random = new SecureRandom();
		for (int i=0; i< elements; i++) {
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
