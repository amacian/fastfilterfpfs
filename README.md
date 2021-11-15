# Filters with a False Positive Free Set
This repository includes the code in Java of the different filter implementations and use cases proposed in the paper:

P. Reviriego, A. Sánchez-Macián, S. Walzer and P.C. Dillinger, "Approximate Membership Check Filters with a False Positive Free Set" Arxiv: http://arxiv.org/abs/2111.06856

# Dependencies
- Guava >=30.1.1
- Fastfilter Java implementation: https://github.com/FastFilter/fastfilter_java/ tag 1.0.2
- For the Birkberk words spell checker use case, the data provided at https://www.dcs.bbk.ac.uk/~ROGER/missp.dat
- For the URL deny list use case, the information provided at https://www.kaggle.com/teseract/urldataset

# Content
This directory includes two batch scripts to execute the speed tests and the bitcoin SPV use case.

*src* directory includes the following files:

Classes in the package org.fastfilter:
- StringFilter.java (interface equivalent to the original Filter from fastfilter, but for Strings)

Classes in the package org.fastfilter.xor:
- XorSimpleN.java (implementation of the original xor filter using a variable number N of bits for the fingerprint)
- XorSimpleNString.java (as the XorSimpleN, but adapted to receive String values instead of longs - for the use cases)
- XorSimpleNRemover.java (implementation of the naive approach from the paper for longs) 
- XorSimpleNRemoverString.java (same as XorSimpleNRemover, but T is read from a Reader to cope with sets of big size)
- XorIntegratedFilter.java (implementation of the Integrated Filter approach from the paper for longs) 
- XorIntegratedFilterTByFile.java (same as XorIntegratedFilter, but T is read from a Reader to cope with sets of big size)
- XorIntegratedStringFilter.java (same as XorIntegratedFilter, but for Strings instead of longs - for the use cases)
- XorSeveralFilters.java (implementation of the Two-Filter approach, actually N-filter, from the paper for longs) 
- XorSeveralFiltersTByFile.java (same as XorSeveralFilters, but T is read from a Reader to cope with sets of big size)
- XorSeveralStringFilters.java (same as XorSeveralFilters, but for Strings instead of longs - for the use cases)


# Execution of the code
Compile the java classes and resolve dependencies. Then:
- Modify GUAVA_DIR, FASTFILTER_DIR and CLASSPATH from the comparingFilters.bat script and run it to generate the log files that provide memory and FP information.
- Modify GUAVA_DIR, FASTFILTER_DIR and CLASSPATH from the speedExecution.bat script and run it to get the speed comparison logs.
- Modify GUAVA_DIR, FASTFILTER_DIR and CLASSPATH from the BirkberkUseCaseTester.bat script and run it to get the results for the Bitcoin SPV use case.
- Modify GUAVA_DIR, FASTFILTER_DIR and CLASSPATH from the URLDenyListUseCaseTester.bat script and run it to get the results for the Bitcoin SPV use case.
- Modify GUAVA_DIR, FASTFILTER_DIR and CLASSPATH from the BitcoinSPVMemoryTester.bat script and run it to get the results for the Bitcoin SPV use case.
- GlobalTesterLong in org.fastfilter.tester.xor just validates the correct behavior of the implemented approaches
