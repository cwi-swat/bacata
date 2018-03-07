module bacata::Generator

import IO;
import List;
import String;
import bacata::Notebook;

// [0] Language name
// [1] rascal project path
// [2] variableName
// [3] moduleName
// [4] Bacata path
void main(list[str] args) {
  path = |tmp:///|;
  params = size(args);
  if( params == 5){
  	path = createKernel(args[0], toLocation(args[1]), args[2], args[3], toLocation(args[4]));
  }
  else if(params == 6){
  	path = createKernel(args[0], toLocation(args[1]), args[2], args[3], toLocation(args[4]), salixPath = toLocation(args[5]));
  }
  else if(params == 7){
  	path = createKernel(args[0], toLocation(args[1]), args[2], args[3], toLocation(args[4]), salixPath = toLocation(args[5]), langLogo= toLocation(args[6]));
  }
  installKernel(path);
  startJupyterServer();
}

loc createKernel(str languageName, loc projectPath, str variableName, str moduleName, loc bacataJar, loc salixPath = |tmp:///|, loc langLogo = |tmp:///|){
	writeFile(|tmp:///<languageName>|+"kernel.json", kernelContent(languageName, projectPath, variableName, moduleName, bacataJar, salixPath));
	if(langLogo!= |tmp:///|)
	{
		copyLogoToKernel(langLogo, |tmp:///<languageName>|);
	}
	return |tmp:///<languageName>|;
}

str kernelContent(str languageName, loc projectPath, str variableName, str moduleName, loc bacataJar, loc salixPath) = 
	"{
  	'	\"argv\": [
    '		\"java\",
    '		\"-cp\",
    '		\"<resolveLocation(|home:///|).path><"<bacataJar>"[9..-1]>:<resolveLocation(|home:///|).path><"<projectPath>"[9..-4]>\",
    '		\"bacata.TermKernel\",
    '		\"{connection_file}\",
    '		\"<"<projectPath>"[1..-1]>\",
    '		\"<moduleName>\",
    '		\"<variableName>\",
    '		\"<languageName>\"
    ' 		<if(salixPath != |tmp:///|){>,\"<"<salixPath>"[1..-1]>\"<}>
  	'	],
  	'	\"display_name\": \"<firstUpperCase(languageName)>\",
  	'	\"language\": \"<languageName>\"
	'}";