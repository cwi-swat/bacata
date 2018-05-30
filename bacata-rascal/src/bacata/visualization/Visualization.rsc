module bacata::visualization::Visualization

import IO;
import List;
import String;
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