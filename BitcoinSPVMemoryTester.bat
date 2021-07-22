ECHO Start of Loop

set GUAVA_DIR=./data/guava-30.1.1-jre.jar
set FASTFILTER_DIR=../fastfilter_java/fastfilter/bin

set CLASSPATH=./bin;%GUAVA_DIR%;%FASTFILTER_DIR%

REM  java -cp %CLASSPATH% org.fastfilter.xor.tester.BitcoinSPVMemoryTester

FOR /L %%i IN (1,1,1) DO (
  java -Xms4096M -Xmx8000M -cp %CLASSPATH% org.fastfilter.xor.tester.BitcoinSPVMemoryTester
)
 
