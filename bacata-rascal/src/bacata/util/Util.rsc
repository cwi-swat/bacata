module bacata::util::Util

import IO;
import List;
import String;
import Message;
import salix::Node;
import salix::App;
import salix::HTML;
import salix::Core;
import salix::Node;
import util::ShellExec;

@doc{This function translate standard Rascal error Messages into a plain text to be displayed in the notebooks.}
list[Message] translateErrorMessages(set[Message] errors) =
	[error("Error: <err.msg> at line <err.at.begin[0]>, column <err.at.begin[1]> to <err.at.end[1]>.\n") |err <- errors && err.msg != "Undefined name"];

str toHTML(void() mvm) { 
	salix::Node::Node nod = render(mvm);
	return parse2HTML(nod);
}

str parse2HTML(salix::Node::Node root){
	switch(root){
		case element(str tagName, list[salix::Node::Node] kids, map[str, str] attrs, map[str, str] props, map[str, salix::Node::Hnd] events):
			return "\<<tagName> <parseAttrs(attrs)> \> <parseNodesList(kids)> \</<tagName>\>";		
		case txt(str contents):
			return "<contents>";
		default: "";
	}
	return "";
}

str parseAttrs(map[str, str] attrs){
	return (""|it+"<key> = \"<trim(attrs[key])>\""| key <- attrs);
}

str parseNodesList(list[salix::Node::Node] lstnodes){
	if(!isEmpty(lstnodes))
		return (""|it + parse2HTML(x)| salix::Node::Node x <- lstnodes);
	else
		return "";
}

/*
* This function reads the environment variable received as parameter.
*/
str readEnvVariable(str key) {
	process = createProcess("printenv", args=[key]);
	return replaceAll(readEntireStream(process),"\n","");
}
