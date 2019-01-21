# Bacata

## Eclipse Plugin
To install Bacatá please use the following update site [https://update.rascal-mpl.org/libs/](https://update.rascal-mpl.org/libs/) and select the __Bacata__ feature.

### Requirements
* [Jupyter](https://github.com/maveme/notebook).
* [Rascal](https://www.rascal-mpl.org/start/).

### Set-up
* Install Bacatá
* Set the following environment variables.
	* __ECLIPSE_HOME__: this variable points to the _Contents_ folder of Eclipse.
		* Example: ECLIPSE_HOME = /Applications/Eclipse\ 3.app/Contents
	* __JUPYTER_HOME__: this variable points to the Jupyter installation.
		* _Example:_ JUPYTER_HOME = /Library/Frameworks/Python.framework/Versions/3.7/bin/jupyter
			
__NOTE__: Verify that the previous environment variables are available when you start Eclipse.

### Usage
* Right click on the root of your project and initiate a _Rascal Console_.
* Import the Bacatá notebook module.
	* `import bacata::Notebook_`
* Create a _Kernel_.
	* `k = kernel("Calc", |home:///Documents/calc/src|, "CalcREPL::myRepl", salixPath=|home:///Documents/Rascal/salix/src|, logo = |home:///calc.png|);`
* Create a Notebook object.
	* `nb = createNotebook(k);`
* Start the Jupyter notebook.
	* `nb.serve();`
* After executing the previous command, you will obtain in the console the url in which Jupyter is running.	
* To stop the server use the following command.
	* `nb.stop();`
____

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
java -Drascal.path="\<pathBacataRascal.jar>" -jar \<rascal.jar> bacata::Generator \<languageName> \<rascalDSLProjectPath> \<REPLFunctionName> \<REPLModule> \<home:///pathBacataRascal.jar > \<salixProjectPath>

### Examples
java -Drascal.path="/Users/mveranom/Documents/bacata/bacata-rascal/target/bacata-dsl.jar" -jar /Users/mveranom/Documents/Rascal/rascal/target/rascal-0.11.0-SNAPSHOT.jar bacata::Generator TwoStones home:///Documents/TwoStones/src twoStonedREPL TwoStonesREPL home:///Documents/bacata/bacata-rascal/target/bacata-dsl.jar home:///Documents/RascalProjects/salix/src

##### tmp [outdated]
java -Drascal.path="/Users/mveranom/Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT.jar" -jar /Users/mveranom/Documents/Rascal/rascal/target/rascal-0.11.0-SNAPSHOT.jar bacata::Generator salix home:///Documents/neclipse/wt/salixIntegration/src tmpREPL TmpREPL home:///Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT-jar-with-dependencies.jar home:///Documents/RascalProjects/salix/src home:///geometric.jpg

#### Halide example (May 2018) [Working]
java -Drascal.path="/Users/mveranom/Documents/bacata/bacata-rascal/target/bacata-dsl.jar" -jar /Users/mveranom/Documents/Rascal/rascal/target/rascal-0.11.0-SNAPSHOT.jar bacata::Generator salixTEMP home:///Documents/RascalProjects/halide-syntax/src halideREPL lang::halide::HalideREPL home:///Documents/bacata/bacata-rascal/target/bacata-dsl.jar home:///Documents/RascalProjects/salix/src home:///geometric.jpg
