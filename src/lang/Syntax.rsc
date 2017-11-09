module lang::Syntax

lexical IntegerLiteral = [0-9]+;

start syntax Goal 
	= "Keeper"
	| tmp: IntegerLiteral
	| tmp2: "Goalie" IntegerLiteral
	;