package org.rascalmpl.bacata.repl.replization;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

import org.rascalmpl.library.util.TermREPL.TheREPL;
import org.rascalmpl.values.functions.IFunction;

import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;

public class REPLize2 extends TheREPL {

	private final IValue initConfig;
	private final IValue printer;

	private MutableValueGraph<GraphNode, String> graph;
	private GraphNode root;
	private GraphNode current;

	public REPLize2(IValueFactory vf, IString title, IString welcome, IString prompt, IString quit, ISourceLocation history,
			IFunction handler, IFunction completor, IValue stacktrace, IValue defaultConfig, IValue printer, InputStream input, OutputStream stderr, OutputStream stdout) {
		super(vf, title, welcome, prompt, quit, history, handler, completor, stacktrace, input, stderr,stdout);

		this.initConfig = defaultConfig;
		this.printer = printer;

		this.graph = ValueGraphBuilder.directed().build();

		this.root =  new GraphNode("", "Root", this.initConfig);
		this.current = this.root;

		this.graph.addNode(this.current);
	}

	@Override
	public void handleInput(String line, Map<String, InputStream> output, Map<String,String> metadata) throws InterruptedException {
		// The cell id comes as part of the meta-data to avoid having to change the ILanguageProtocol.
		String cellId = metadata.get("cell_id"); // This is used as the value of the edge

		// Front-end current node. In fact, this is the last executed node.
		String currentNode = metadata.get("current_cell"); // Represents the context to use for the execution.

		// If they are different means that the user wants to create a new path from a previous execution.
		// Result is a string representation of the config.
		if (!current.getResult().equals("Root") && !currentNode.equals(current.hashCode() + "")) {
			this.current = getNode(currentNode);
		}

		super.handleInput(line, output, metadata); // execute code

		processResult(line, output, metadata, cellId);
	}


	public GraphNode getNode(String result) {
		for (GraphNode node : graph.nodes()) {
			String hash = node.hashCode()+"";
			if (hash.equals(result))
				return node;
		}
		return null;
	}

	/**
	 * Process the result from the interpreter and produces the result for Bacatá
	 * @param commandResult Interpreter result
	 * @param output 
	 * @param metadata
	 * @param cellId this is the cell number in the front-end.
	 */
	public void processResult(String input, Map<String, InputStream> output, Map<String, String> metadata, String cellId) {
		@SuppressWarnings("unused")
		String g = "";
		@SuppressWarnings("unused")
		int d=3;
		//          GraphNode tmp =  new GraphNode(input, newconfig.toString(), newconfig);
		//          graph.putEdgeValue(current, tmp, cellId);
		//
		//          IConstructor result = printAnswer(this.current.getConfig(), newconfig);
		//          this.current = tmp;
		//          
		//          String rst = result.get("result").toString();
		//          rst = rst.replace("\\", "").replace("\"", "");
		//          
		//        output.put("text/html", stringStream(rst));
		//        addGraph2Metadata(metadata); 
	}

//	@Override
//	public CompletionResult completeFragment(String line, int cursor) {
//		ITuple result = (ITuple)call(completor, new Type[] { tf.stringType(), tf.integerType(), this.current.getConfig().getType() },
//				new IValue[] { vf.string(line), vf.integer(cursor), this.current.getConfig() }); 
//
//
//		List<String> suggestions = new ArrayList<>();
//		int offset = ((IInteger) result.get(0)).intValue();
//		IList suggs = (IList) result.get(1); 
//
//		for (IValue v: suggs) {
//			suggestions.add(((IString)v).getValue());
//		}
//		return new CompletionResult(offset, suggestions);
//	}

}
