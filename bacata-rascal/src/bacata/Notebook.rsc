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

	
data NotebookServer =
	notebook(void () serve, void() stop);	
	
data KernelInfo
	= kernelInfo(str languageName, loc projectPath, str moduleName, str variableName, loc salixPath= |tmp:///|, loc logo = |tmp:///|);

str JUPYTER_PATH = "/Library/Frameworks/Python.framework/Versions/3.6/bin/jupyter";
loc JUPYTER_FRONTEND_PATH = |home:///Documents/Jupyter/forked-notebook/notebook/static/components/codemirror/mode/|;

/*
* This function starts a notebook WITHOUT a custom codemirror mode
*/
NotebookServer createNotebook(KernelInfo kernelInfo, bool debug = false,bool docker=false){
	generateKernel(kernelInfo, debug, docker);
	int pid = -1;
	return notebook( void () { pid = startJupyterServer(); }, void () { killProcess(pid); });
}

/*
* This function starts a notebook with a custom codemirror mode generated based on the grammar
*/
//TODO: This method and the previous one should be merged. (Which is the empty way of type[&T <: Tree]?)
NotebookServer createNotebook(KernelInfo kernelInfo, type[&T <: Tree] sym, bool debug = false, bool docker=false){
	generateKernel(kernelInfo, debug, docker);
	generateCodeMirror(kernelInfo, sym, docker);
	int pid = -1;
	return notebook( void () { pid = startJupyterServer(); }, void () { killProcess(pid); });
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
void generateCodeMirror(KernelInfo kernelInfo, type[&T <: Tree] sym, bool docker=false){
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

void generateKernel(KernelInfo kernelInfo, bool debug, bool docker){
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
	 pid= createProcess(JUPYTER_PATH, args=["kernelspec", "install", resolveLocation(kernelPath).path]);
	 printErrTrace(pid);
}

void printErrTrace(PID pid){
	for (line := readLineFromErr(pid), line != "") {
		println("<line>");
    }
}

/*
* This function starts the jupyter server and returns the url in which the webserver is runing.
*/
PID startJupyterServer(){
	PID jupyterExecution = createProcess(JUPYTER_PATH, args =["notebook", "--no-browser"]);
	bool guard = false;
	for (_ <- [1..19], line := readLineFromErr(jupyterExecution), line != "") {
		if(contains(line,"http://localhost:"))
		{
			println("The notebook is running at: <|http://localhost:<split("localhost:", line)[1]>|>");
		}
    }
    return jupyterExecution;
}
/*
* This function produces the content of the kernel.json file using the kernel information received as parameter.
*/
//    '		\"-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 \",
str kernelFileContent(KernelInfo kernelInfo, bool debug) = 
	"{
  	'	\"argv\": [
    '		\"java\",
    '		\"-jar\",
    ' 		<if(debug){>\"-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 \",<}>
    '		\"/Users/mveranom/Documents/bacata/bacata-rascal/target/bacata-rascal-0.1.0-SNAPSHOT-jar-with-dependencies.jar\",
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