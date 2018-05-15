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
