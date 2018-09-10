module bacata::Notebook

import IO;
import List;
import String;
import Message;
import ParseTree;
import util::REPL;
import util::Resources;
import util::ShellExec;
import bacata::Deploy;
import bacata::util::Mode;
//import bacata::HTML;
import bacata::util::CodeMirror;

	
data NotebookServer 
	=notebook(void () serve, void() stop)
	|notebook(void () serve, void() stop, void() logs)
	|notebook();
	
data Kernel
	= kernel(str languageName, loc projectPath, str moduleName, str variableName, loc salixPath= |tmp:///|, loc logo = |tmp:///|);

//str JUPYTER_PATH = "/Library/Frameworks/Python.framework/Versions/3.6/bin/jupyter";
//loc JUPYTER_FRONTEND_PATH = |home:///Documents/Jupyter/forked-notebook/notebook/static/components/codemirror/mode/|;
loc JUPYTER_FRONTEND_PATH = |tmp:///|;
str JUPYTER_HOME = "";
str BACATA_HOME = "";

/*
* This function reads the environment variable received as parameter.
*/
str readEnvVariable(str key){
	f= createProcess("printenv", args=[key]);
	return replaceAll(readEntireStream(f),"\n","");
}


void verifyPreRequisites(){
	verifyJupyterInstallation();
	verifyBacataInstallation();
}

/*
* This function verifies the definition of the JUPYTER_FRONTEND_PATH
*/
void verifyJupyterFrontendPath(){
	JUPYTER_FRONTEND_PATH = |home:///| + readEnvVariable("JUPYTER_FRONTEND_PATH");
	if(JUPYTER_FRONTEND_PATH == "")
		throw "JUPYTER_FRONTEND_PATH is not defined as environment variable";
}

/*
* This function verifies the definition of the JUPYTER_HOME
*/
void verifyJupyterInstallation(){
	JUPYTER_HOME = readEnvVariable("JUPYTER_HOME");
	if(JUPYTER_HOME == "")
		throw "JUPYTER_HOME is not defined as environment variable";
}

/*
* This function verifies the definition of the BACATA_HOME
*/
void verifyBacataInstallation(){
	BACATA_HOME = readEnvVariable("BACATA_HOME");
	if(BACATA_HOME == "")
		throw "BACATA_HOME is not defined as environment variable";
}

/*
* This function starts a notebook WITHOUT a custom codemirror mode
*/
NotebookServer createNotebook(Kernel kernelInfo, bool debug = false, bool docker=false){
	verifyPreRequisites();
	try {
		generateKernel(kernelInfo, debug, docker);
		int pid = -1;
		return notebook( void () { pid = startJupyterServer(); }, void () { killProcess(pid);}, void (){ readLogs(pid);});
	}
	catch Exc:
		throw "ERROR: Something went wrong while creating the notebook. \n <Exc>";
}

NotebookServer createNotebook(){
	verifyPreRequisites();
	try {
		int pid = -1;
		return notebook( void () { pid = startJupyterServer(); }, void () { killProcess(pid);}, void (){ readLogs(pid);});
	}
	catch Exc:
		throw "ERROR: Something went wrong while creating the notebook. \n <Exc>";
}

/*
* This function reads the logs generated as side-effect from running Jupyter Server.
*/
void readLogs(PID pid){
	try
		printErrTrace(pid);
	catch Exc:
		"";
	try
		printTrace(pid);
	catch ex:
		"";
}
/*
* This function starts a notebook with a custom codemirror mode generated based on the grammar
*/
//TODO: This method and the previous one should be merged. (Which is the empty way of type[&T <: Tree]?)
NotebookServer createNotebook(Kernel kernelInfo, type[&T <: Tree] sym, bool debug = false, bool docker=false){
	verifyPreRequisites();
	generateKernel(kernelInfo, debug, docker);
	generateCodeMirror(kernelInfo, sym, docker=docker);
	int pid = -1;
	return notebook( void () { pid = startJupyterServer(); }, void () { killProcess(pid); }, void (){ readLogs(pid);});
}

/*
* This function takes the url of a logo image (64x64) for the language to be displayed in the browser when the kernel is loaded
*/
void copyLogoToKernel(loc urlLogo, loc destPath){
	list[int] imgBytes= readFileBytes(urlLogo);
	writeFileBytes(destPath + "logo-64x64.png", imgBytes);
}

/*
* This function creates a code mirror mode using the mode received as parameter and re-builds the notebook front-end project.
*/
void generateCodeMirror(Kernel kernelInfo, type[&T <: Tree] sym, bool docker=false){
	verifyJupyterFrontendPath();
	Mode mode = grammar2mode(kernelInfo.languageName, sym);
	// Jupyter front-end path
	if(!docker)
		createCodeMirrorModeFile(mode, JUPYTER_FRONTEND_PATH + "<mode.name>/<mode.name>.js");
	else
		createCodeMirrorModeFile(mode, kernelInfo.projectPath.parent + "kernel2/codemirror/<mode.name>/<mode.name>.js");
	
	// Re-build notebook front end
	//pid=createProcess("/usr/local/bin/node", args=["/usr/local/bin/npm", "run", "build"]);
	//printErrTrace(pid);
}

void generateKernel(Kernel kernelInfo, bool debug, bool docker){
	kernelPath = kernelInfo.projectPath.parent + "kernel2/<kernelInfo.languageName>/";
	if(kernelInfo.logo != |tmp:///|)
		copyLogoToKernel(kernelInfo.logo, kernelPath);
	if(docker){
		kernelContent = dockerLanguageKernelContent(kernelInfo);
		writeFile(kernelPath + "kernel.json", kernelContent);
		content = dockerFileContent(last(split("/", kernelInfo.projectPath.parent.path)), kernelInfo.languageName);
		writeFile(kernelInfo.projectPath.parent + "Dockerfile2", content);
	}
	else{
		kernelContent = kernelFileContent(kernelInfo, debug);
		writeFile(kernelPath + "kernel.json", kernelContent);
		installKernel(kernelPath);
	}
}

void installKernel(loc kernelPath){
	try{
		 pid= createProcess(JUPYTER_HOME, args=["kernelspec", "install", resolveLocation(kernelPath).path]);
	 	printErrTrace(pid);
	 }
	 catch Exc: 
	 	throw "Error while installing the kernel <Exc>";
}

void printErrTrace(PID pid){
	//for (_ <- [1..10], line := readLineFromErr(pid), line != "") {
	//	println("<line>");
 //   }
 	line = readFromErr(pid);
    while(line!= ""){
    	println("<line>");
    	line = readFromErr(pid);
    }
}

void printTrace(PID pid){
    line = readFrom(pid);
    while(line!= ""){
    	println("<line>");
    	line = readFrom(pid);
    }
}

/*
* This function starts the jupyter server and returns the url in which the webserver is runing.
*/
PID startJupyterServer(){
	PID jupyterExecution = createProcess(JUPYTER_HOME, args =["notebook", "--no-browser"]);
	bool guard = false;
	for (_ <- [1..19], line := readLineFromErr(jupyterExecution), line != "") {
		if(contains(line,"http://localhost:"))
		{
			println("The notebook is running at: <|http://localhost:<split("localhost:", line)[1]>|>");
			guard = true;
		}
	}
	if(!guard)
		throw "Error while starting the Jupyter server"; 
	return jupyterExecution;
}
/*
* This function produces the content of the kernel.json file using the kernel information received as parameter.
*/
//    '		\"-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 \",
str kernelFileContent(Kernel kernelInfo, bool debug) = 
	"{
  	'	\"argv\": [
    '		\"java\",
    '		\"-jar\",
    ' 		<if(debug){>\"-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 \",<}>
    '		\"<BACATA_HOME>\",
    '		\"{connection_file}\",
    '		\"<"<kernelInfo.projectPath>"[1..-1]>\",
    '		\"<kernelInfo.moduleName>\",
    '		\"<kernelInfo.variableName>\",
    '		\"<kernelInfo.languageName>\"
    ' 		<if(kernelInfo.salixPath != |tmp:///|){>,\"<"<kernelInfo.salixPath>"[1..-1]>\"<}>
  	'	],
  	'	\"display_name\": \"<firstUpperCase(kernelInfo.languageName)>\",
  	'	\"language\": \"<kernelInfo.languageName>\"
	'}";
	
/*
* This function replaces the first character of the string for the corresponding character in uppercase
*/
str firstUpperCase(str input){
	str first = stringChar(charAt(input, 0));
	return replaceFirst(input, first, toUpperCase(first)); 
}	

//@javaClass{org.rascalmpl.library.util.Notebook}
//@reflect
//java str startNotebook(REPL repl);