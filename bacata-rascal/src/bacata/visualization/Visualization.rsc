module bacata::visualization::Visualization

import IO;
import List;
import salix::App;
import bacata::salix::Bridge;
import salix::HTML;
import salix::Core;
import salix::Node;

SalixMultiplexer visualizationServer(loc http = |http://localhost:3535|, loc static = |tmp:///|){
	try{
		return makeSalixMultiplexer(http, static);
	}
	catch Ed:
		println("<ed>");
}
str toHTML(void() mvm) { 
	salix::Node::Node nod = render(mvm);
	return parse2HTML(nod);
}

str parse2HTML(salix::Node::Node root){
	switch(root){
		case element(str tagName, list[salix::Node::Node] kids, map[str, str] attrs, map[str, str] props, map[str, salix::Node::Hnd] events):
			return "\<<tagName> <parseAttrs(attrs)>\><parseNodesList(kids)>\</<tagName>\>";		
		case txt(str contents):
			return "<contents>";
		default: "";
	}
	return "";
}

str parseAttrs(map[str, str] attrs){
	return (""|it+"<key> = \"<attrs[key]> \""| key <- attrs);
}

str parseNodesList(list[salix::Node::Node] lstnodes){
	if(!isEmpty(lstnodes))
		return (""|it + parse2HTML(x)| salix::Node::Node x <- lstnodes);
	else
		return "";
}