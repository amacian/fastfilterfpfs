ECHO Start of Loop

set GUAVA_DIR=./data/guava-30.1.1-jre.jar
set FASTFILTER_DIR=../fastfilter_java/fastfilter/bin

set CLASSPATH=./bin;%GUAVA_DIR%;%FASTFILTER_DIR%

REM  java -cp %CLASSPATH% org.fastfilter.xor.tester.SpeedTests <filterType:SF> <s Size> <t Size> <r> <b> 

FOR /L %%i IN (1,1,100) DO (
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000 4 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 100000 4 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 100000 4 6
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 1000000 4 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 1000000 4 1
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000000 4 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000000 4 1
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000 8 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 100000 8 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 1000000 8 10 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 1000000 8 7
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000000 8 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000000 8 2
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000 16 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000 16 7
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 100000 16 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 100000 16 8
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 1000000 16 10
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 1000000 16 9
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests SF 100000 10000000 16 10
  )
