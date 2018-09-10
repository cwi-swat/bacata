module bacata::Generator

import IO;
import List;
import String;
import bacata::Deploy;
import bacata::Notebook;
//
// [0] Language name
// [1] rascal project path
// [2] variableName
// [3] moduleName
// [4] Bacata path
void main(list[str] args) {
	params = size(args);
	if(args[0] =="docker"){
		if(params==5)
			kernel = kernel(args[1], toLocation(args[2]), args[3], args[4]);
		else
			kernel = kernel(args[1], toLocation(args[2]), args[3], args[4], logo = toLocation(args[5]));
		
		generateKernel(kernel, false, true);
		// TODO: I don't know how to create a type[&T <: Tree] from an arg
		//generateCodeMirror(kernel, #Command, true);
	}
	else{
	  path = |tmp:///|;
	  //params = size(args);
	  if( params == 5){
	  	path = createKernel(args[0], toLocation(args[1]), args[2], toLocation(args[3]));
	  }
	  else if(params == 6){
	  	path = createKernel(args[0], toLocation(args[1]), args[2], toLocation(args[3]), salixPath = toLocation(args[4]));
	  }
	  else if(params == 7){
	  	path = createKernel(args[0], toLocation(args[1]), args[2], toLocation(args[3]), salixPath = toLocation(args[4]), langLogo= toLocation(args[5]));
	  }
	  installKernel(path);
	  startJupyterServer();
	}
}

loc createKernel(str languageName, loc projectPath, str replQualifiedName, loc bacataJar, loc salixPath = |tmp:///|, loc langLogo = |tmp:///|){
	writeFile(|tmp:///<languageName>|+"kernel.json", kernelContent(languageName, projectPath, replQualifiedName, bacataJar, salixPath));
	if(langLogo!= |tmp:///|)
	{
		copyLogoToKernel(langLogo, |tmp:///<languageName>|);
	}
	return |tmp:///<languageName>|;
}

str kernelContent(str languageName, loc projectPath, str replQualifiedName, loc bacataJar, loc salixPath) = 
	"{
  	'	\"argv\": [
    '		\"java\",
    '		\"-cp\",
    '		\"<resolveLocation(|home:///|).path><"<bacataJar>"[9..-1]>:<resolveLocation(|home:///|).path><"<projectPath>"[9..-4]>\",
    '		\"bacata.dslNotebook.DSLNotebook\",
    '		\"{connection_file}\",
    '		\"<"<projectPath>"[1..-1]>\",
    '		\"<replQualifiedName>\",
    '		\"<languageName>\"
    ' 		<if(salixPath != |tmp:///|){>,\"<"<salixPath>"[1..-1]>\"<}>
  	'	],
  	'	\"display_name\": \"<firstUpperCase(languageName)>\",
  	'	\"language\": \"<languageName>\"
	'}";