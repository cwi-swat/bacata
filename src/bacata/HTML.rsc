module bacata::HTML

import IO;


@javaClass{bacata.HTML}
public java str compile(str templateLoc);

public str toHTML(str template){
	writeFile(|tmp:///jade/tmp.jade|, template);
	return compile(resolveLocation(|tmp:///jade/tmp.jade|).path);
}