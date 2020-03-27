//package org.rascalmpl.bacata.repl;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.PrintWriter;
//import java.io.Writer;
//import java.net.URISyntaxException;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import org.rascalmpl.interpreter.Evaluator;
//import org.rascalmpl.interpreter.IEvaluatorContext;
//import org.rascalmpl.interpreter.StackTrace;
//import org.rascalmpl.interpreter.result.ICallableValue;
//import org.rascalmpl.interpreter.utils.ReadEvalPrintDialogMessages;
//import org.rascalmpl.repl.CompletionResult;
//import org.rascalmpl.repl.ILanguageProtocol;
//
//import io.usethesource.vallang.IConstructor;
//import io.usethesource.vallang.IInteger;
//import io.usethesource.vallang.IList;
//import io.usethesource.vallang.ISourceLocation;
//import io.usethesource.vallang.IString;
//import io.usethesource.vallang.ITuple;
//import io.usethesource.vallang.IValue;
//import io.usethesource.vallang.IValueFactory;
//import io.usethesource.vallang.IWithKeywordParameters;
//import io.usethesource.vallang.io.StandardTextWriter;
//import io.usethesource.vallang.type.Type;
//import io.usethesource.vallang.type.TypeFactory;
//
//public class BacataREPL implements ILanguageProtocol {
//	 private final TypeFactory tf = TypeFactory.getInstance();
//     private PrintWriter stdout;
//     private PrintWriter stderr;
//     private String currentPrompt;
//     private final ICallableValue handler;
//     private final IEvaluatorContext ctx;
//     private final ICallableValue completor;
//     private final IValueFactory vf;
//     
//     private ITuple visualization;
//     private ICallableValue consumerFunction;
//     private ISourceLocation http;
//     
//     private int scopeId;
//
//     public BacataREPL(IValueFactory vf, IConstructor repl, IEvaluatorContext ctx) throws IOException, URISyntaxException {
//         this.ctx = ctx;
//         this.vf = vf;
//         this.handler = (ICallableValue)repl.get("handler");
//         this.completor = (ICallableValue)repl.get("completor");
//         
//         if(repl.has("prompt"))
//             this.currentPrompt = ((IString)repl.get("prompt")).getValue();
//         if(repl.asWithKeywordParameters().hasParameter("visualization")){
//             this.visualization = (ITuple) repl.asWithKeywordParameters().getParameter("visualization");
//             consumerFunction = (ICallableValue) this.visualization.get(0);
//             http = (ISourceLocation) this.visualization.get(2);
//             this.scopeId = 0;
//         }
//         
//         stdout = ctx.getStdOut();
//         assert stdout != null;
//     }
//     
//     @Override
//     public void cancelRunningCommandRequested() {
//         ctx.interrupt();
//     }
//     
//     @Override
//     public void terminateRequested() {
//         ctx.interrupt();
//     }
//     
//     @Override
//     public void stop() {
//         ctx.interrupt();
//     }
//     
//     @Override
//     public void stackTraceRequested() {
//         StackTrace trace = ctx.getStackTrace();
//         Writer err = ctx.getStdErr();
//         try {
//             err.write("Current stack trace:\n");
//             trace.prettyPrintedString(err, new StandardTextWriter(true));
//             err.flush();
//         }
//         catch (IOException e) {
//         } 
//     }
//
//
//     @Override
//     public void initialize(Writer stdout, Writer stderr) {
//         this.stdout = new PrintWriter(stdout);
//         this.stderr = new PrintWriter(stderr);
//     }
//
//     @Override
//     public String getPrompt() {
//         return currentPrompt;
//     }
//
//     @Override
//     public void handleInput(String line, Map<String, InputStream> output, Map<String,String> metadata) throws InterruptedException {
//         
//         if (line.trim().length() == 0) {
//             // cancel command
//             // TODO: after doing calling this, the repl gets an unusual behavior, needs to be fixed
//             this.stderr.println(ReadEvalPrintDialogMessages.CANCELLED);
//             currentPrompt = ReadEvalPrintDialogMessages.PROMPT;
//             return;
//         } 
//         else {
//             // TODO: this could also be handled via the Content data-type, which should work 
//             // also for Salix apps...
//             IConstructor result = (IConstructor) call(handler, new Type[] { tf.stringType() }, new IValue[] { vf.string(line) });
//             if(result.has("result") && result.get("result") != null) {
//                 String str = ((IString)result.get("result")).getValue();
//                 // TODO: change the signature of the handler
//                 if(!str.equals("")) {
//                     if(str.startsWith("Error:")) {
//                         metadata.put("ERROR-LOG", "<div class = \"output_stderr\">" + str.substring("Error:".length(), str.length()) + "</div>");
//                     }
//                     else {
//                         output.put("text/html", stringStream("<div>" + str + "</div>"));
//                     }
//                 }
//             }
//             else {
//                 // Handle a Salix result
//                 ICallableValue salixApp = (ICallableValue) result.get("salixApp");
//                 String scope = "salixApp" + scopeId;
//                 call(this.consumerFunction, new Type[]{tf.valueType(), tf.stringType()}, new IValue[]{salixApp, vf.string(scope)});
//                 String out = "<script> \n var "+ scope +" = new Salix('"+ scope +"', '" + http.getURI().toString() +"'); \n google.charts.load('current', {'packages':['corechart']}); google.charts.setOnLoadCallback(function () { registerCharts("+scope+");\n registerDagre("+scope+");\n registerTreeView("+ scope +"); \n"+ scope + ".start();});\n </script> \n <div id=\""+scope+"\"> \n </div>";
//                 output.put("text/html", stringStream(out));
//                 this.scopeId++;
//                 
//             }
//             IWithKeywordParameters<? extends IConstructor> commandResult = result.asWithKeywordParameters();
//             if(commandResult.hasParameter("messages")) {
//                 IList messages = (IList) commandResult.getParameter("messages");
//                 for (IValue v: messages) {
//                     IConstructor msg = (IConstructor) v;
//                     if(msg.getName().equals("error")) {
//                         stderr.write( ((IString) msg.get("msg")).getValue());
//                     }
//                 }
//             }
//         }
//     }
//
//     private InputStream stringStream(String x) {
//         return new ByteArrayInputStream(x.getBytes(StandardCharsets.UTF_8));
//     }
//     
//     @Override
//     public boolean supportsCompletion() {
//         return true;
//     }
//
//     @Override
//     public boolean printSpaceAfterFullCompletion() {
//         return false;
//     }
//
//     private IValue call(ICallableValue f, Type[] types, IValue[] args) {
//         synchronized (ctx) {
//             Evaluator eval = (Evaluator)ctx;
//             PrintWriter prevErr = eval.getStdErr();
//             PrintWriter prevOut = eval.getStdOut();
//             try {
//                 eval.overrideDefaultWriters(stdout, stderr);
//                 return f.call(types, args, null).getValue();
//             }
//             finally {
//                 stdout.flush();
//                 stderr.flush();
//                 eval.overrideDefaultWriters(prevOut, prevErr);
//             }
//         }
//     }
//
//     @Override
//     public CompletionResult completeFragment(String line, int cursor) {
//         ITuple result = (ITuple)call(completor, new Type[] { tf.stringType(), tf.integerType() },
//                         new IValue[] { vf.string(line), vf.integer(cursor) }); 
//
//         List<String> suggestions = new ArrayList<>();
//
//         for (IValue v: (IList)result.get(1)) {
//             suggestions.add(((IString)v).getValue());
//         }
//
//         if (suggestions.isEmpty()) {
//             return null;
//         }
//
//         int offset = ((IInteger)result.get(0)).intValue();
//
//         return new CompletionResult(offset, suggestions);
//     }
//     
//     @Override
//     public void handleReset(Map<String, InputStream> output, Map<String, String> metadata) throws InterruptedException {
//         // TODO: add a rascal callback for this?
//         handleInput("", output, metadata);
//     }
//
//     @Override
//     public boolean isStatementComplete(String command) {
//         // TODO Auto-generated method stub
//         return true;
//     }
//}
