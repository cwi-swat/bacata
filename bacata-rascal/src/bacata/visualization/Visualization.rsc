module bacata::visualization::Visualization

import IO;
import salix::App;
import bacata::salix::Bridge;


SalixMultiplexer visualizationServer(loc http = |http://localhost:3535|, loc static = |tmp:///|){
	try{
		return makeSalixMultiplexer(http, static);
	}
	catch Ed:
		println("<ed>");
}