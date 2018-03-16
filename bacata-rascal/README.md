# Bacata
### Intall
* Python 3 
* Pip3
	* pip3 install --upgrade pip
	* pip3 install --upgrade setuptools pip 
* NodeJS
* NPM
* Bower
 
### Clone and compile
* https://github.com/maveme/notebook
	* cd notebook && pip3 install -e  	
* Rascal (Jupyter branch)
	* mvn clean package
* https://github.com/cwi-swat/bacata
	* mvn clean package
* Salix project


### Execution from command line
java -Drascal.path="pathBacataRascal.jar" -jar rascal.jar bacata::Generator languageName rascalDSLProjectPath REPLFunctionName REPLModule home:///pathBacataRascal.jar salixProjectPath

### Example
java -Drascal.path="/Users/mveranom/Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT.jar" -jar /Users/mveranom/Documents/Rascal/rascal/target/rascal-0.11.0-SNAPSHOT.jar bacata::Generator TwoStones home:///Documents/TwoStones/src twoStonedREPL TwoStonesREPL home:///Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT-jar-with-dependencies.jar home:///Documents/RascalProjects/salix/src

##### tmp
java -Drascal.path="/Users/mveranom/Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT.jar" -jar /Users/mveranom/Documents/Rascal/rascal/target/rascal-0.11.0-SNAPSHOT.jar bacata::Generator salix home:///Documents/neclipse/wt/salixIntegration/src tmpREPL TmpREPL home:///Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT-jar-with-dependencies.jar home:///Documents/RascalProjects/salix/src home:///geometric.jpg