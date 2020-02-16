module bacata::REPL

import salix::App;

extend util::REPL;

data CommandResult(list[Message] messages = [])
  = salix(SalixApp[&T] salixApp)
  ; 
  
data REPL(SalixMultiplexer visualization = noOp())
  =  repl( 
     	CommandResult (str line) handler,
        Completion(str line, int cursor) completor
  );
  
alias SalixConsumer
  = VisOutput(SalixApp[value] app, str scope);

alias SalixMultiplexer
  = tuple[SalixConsumer consumer, void() stop, loc http];
  
alias VisOutput = str;