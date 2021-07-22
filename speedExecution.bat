ECHO Start of Loop

set GUAVA_DIR=./data/guava-30.1.1-jre.jar
set FASTFILTER_DIR=../fastfilter_java/fastfilter/bin

set CLASSPATH=./bin;%GUAVA_DIR%;%FASTFILTER_DIR%

REM  java -cp %CLASSPATH% org.fastfilter.xor.tester.SpeedTests <filterType:OF:TF:IF1:IF2> <s Size> <t Size> <r-1> 

FOR /L %%i IN (1,1,1000) DO (
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 10000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 100000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 1000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 10000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 10000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 100000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 1000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 10000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 10000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 100000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 1000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 10000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 10000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 100000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 1000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 10000000 3 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 10000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 100000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 1000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 10000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 10000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 100000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 1000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 10000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 10000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 100000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 1000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 10000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 10000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 100000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 1000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 10000000 7 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 10000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 100000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 1000000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests OF 100000 10000000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 10000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 100000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 1000000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests TF 100000 10000000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 10000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 100000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 1000000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF1 100000 10000000 15 
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 10000 15
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 100000 15
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 1000000 15
  java -cp %CLASSPATH% org.fastfilter.xor.tester.IndividualSpeedTests IF2 100000 10000000 15
)
 
