# Bacatá
Jupyter language kernel generator for DSLs developed using the [Rascal Language Workbench](http://rascal-mpl.org/).

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





## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 
<!--See deployment for notes on how to deploy the project on a live system.
-->
### Prerequisites

What things you need to install the software and how to install them

```
Give examples
```
* [Docker](https://docs.docker.com/install/) (Optional)
* Bacatá Rascal.jar ([download]())
* [Rascal](http://update.rascal-mpl.org/console/rascal-shell-unstable.jar)

### Installing

A step by step series of examples that tell you have to get a development env running

Say what the step will be

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo

<!--## Running the tests

Explain how to run the automated tests for this system

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```
-->
<!--## Deployment

Add additional notes about how to deploy this on a live system-->

## Built With

* [Jupyter](http://www.jupyter.org) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

<!--## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). -->

## Authors

* **Mauricio Verano Merino** - *Initial work* -
* **Jurgen Vinju**
* **Tijs van der Storm**

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

<!--## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details-->

## Acknowledgments

* Hat tip to anyone who's code was used
* Inspiration
* etc

---------------------------

# Bacatá
## DSL Dockerization using Bacatá
### Set Up
In order to execute Notebooks for DSLs using Bacatá you need to fulfill the follow prerequisites:

* Docker installation ([installation guide](https://docs.docker.com/install/))
* Bacatá Rascal.jar ([download]())
* Rascal jar ([download](http://update.rascal-mpl.org/console/rascal-shell-unstable.jar))

### Dockerfile generation
In order to *Dockerize* a Rascal DSL, you need a Rascal DSL Project with:

* A module that defines a REPL ADT ([details about REPL defitinion](https://github.com/cwi-swat/bacata/blob/master/bacata-rascal/src/bacata/salix/Bridge.rsc))

#### From Eclipse
TODO
#### From CommandLine
**NOTE:** *From the command line syntactic auto-completion is not supported nor the Codemirror mode generation.*
`java -Drascal.path="Path to Bacatá Rascal jar" -jar <Rascal jar> bacata::Generator docker <language Name> <Path to DSL project src folder> <module that contains REPL> <binding REPL adt> <path to the language logo>`


##### Example
The following command will work with a DSL implemented using the [Rascal Language workbench](http://rascal-mpl.org/) called *QL*.

`java -Drascal.path="/Users/mveranom/Downloads/bacata-rascal.jar" -jar /Users/mveranom/Downloads/rascal.jar bacata::Generator docker qlLanguage home:///Documents/RascalProjects/QL/src lang::ql::QLREPL qlREPL home:///ql-logo.jpg`

After executing this command Bacatá generates the following artifacts in the root folder of the DSL project:

* Dockerfile.
* A folder called *kernel* that contains other folder with the name of the language.
	* *kernel.json*: this file contains the Jupyter Language kernel of the DSL that works with the [Docker Bacata Image.](https://hub.docker.com/r/maveme/bacata/)
	* *logo-64x64.png*: this file is optional and represents the logo of the language.

### Dockerfile Image Build
In order to create the Docker image that does the DSL Dockerization, the user needs to execute the following command two commands from the command line:

1. `cd dsl-project/` 
2. `docker build -t <name-of-image> .`

The later command will read the Dockerfile and produces a Docker image with the DSL project source code.

`<name-of-image>`: is an identifier for the Docker image. 
#### example:
1. `cd Documents/RascalProjects/QL/` 
2. `docker build -t qlLanguage .`


### Dockerfile Image Execution
Now we are going to run the Docker image built in the  previous step.
To run the Docker image you have to execute this command in the command line:

`docker run -p <port>:8888 <image-name>`

The `<port>` is an available port in your machine.
The `<image-name>` should be the same name used in the previous step.

After running this command, you will get some output:
> output

Now, you have to follow the next two steps:

1. Copy the `token` from the output
2. Open a browser and type `http://localhost:<port>`

Then you will see an interface like this:
