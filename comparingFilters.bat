ECHO Start of Loop

set GUAVA_DIR=./data/guava-30.1.1-jre.jar
#set FASTFILTER_DIR=../fastfilter_java/fastfilter/bin
set FASTFILTER_DIR=C:\Users\Alfonso\Documents\Research\ProbabilisticStructures\fastfilter_java\fastfilter\bin

set CLASSPATH=./bin;%GUAVA_DIR%;%FASTFILTER_DIR%

REM  java -cp %CLASSPATH% org.fastfilter.xor.tester.ComparingFilters

java -cp %CLASSPATH% org.fastfilter.xor.tester.ComparingFilters
 
