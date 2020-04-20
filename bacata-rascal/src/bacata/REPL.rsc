module bacata::REPL

import salix::App;

import Message;

import Content;

extend util::REPL;
//extend Content;

//data CommandResult(list[Message] messages = [])
//  = salix(SalixApp[&T] salixApp)
//  ; 
  
  
//data REPL(SalixMultiplexer visualization = noOp())
//  =  repl( 
//     	CommandResult (str line) handler,
//        Completion(str line, int cursor) completor)
//  | replization(
// 	    &T (str program, &T config) newHandler,
// 	    &T initConfig,
// 	    CommandResult (&T old, &T prev) printer,
// 	    Completion(str line, int cursor, &T config) tabcompletor)
//  ;
  
  
  data REPL
  = replB(
  	//SalixMashup salixProxy = <(App[value] x) {}, () {}>,
  	tuple[void (App[&T] web) serve, void () stop, loc port] salixProxy = <(App[value] x) {}, () {}>,
     str titleB = "", 
     str welcomeB = "", 
     str promptB = "\n\>",
     str quitB = "", 
     loc historyB = |home:///.term-repl-history|, 
     Content (str command) handlerB = echo,
     Completion(str line, int cursor) completorB = noSuggestions,
     str () stacktraceB = str () { return "";} 
   );
  
  
  data REPL
  = repl2(
     str title = "", 
     str welcome = "", 
     str prompt = "\n\>",
     str quit = "", 
     loc history = |home:///.term-repl-history|,
     &T initConfig = "", 
     &T (str command, &T config) newHandler = echo,
     Completion (str line, int cursor, &T ctx) completor = noSuggestions,
     str () stacktrace = str () { return ""; },
     Content (&T old, &T prev) printer = Content (str _, str _) {return text(old + p);}
   );

private Completion noSuggestions(str _, int _, &T _) = <0, []>;
private Response echo(str line) = plain(line);
  
alias SalixConsumer
  = VisOutput(SalixApp[value] app, str scope);

alias SalixMultiplexer
  = tuple[SalixConsumer consumer, void() stop, loc http];
  
alias VisOutput = str;

SalixMultiplexer noOp() = <(SalixApp[value] x) {}, () {}, |http://localhost|>;
