/** 
 * Copyright (c) 2020, Mauricio Verano Merino, Centrum Wiskunde & Informatica (NWOi - CWI) 
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package org.rascalmpl.bacata.repl.replization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.rascalmpl.bacata.repl.replization.ExecutionGraph.CustomEdge;
import org.rascalmpl.bacata.repl.replization.ExecutionGraph.CustomNode;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.RascalFunction;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.Gson;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public final class REPLize implements ILanguageProtocol {
	
	private final TypeFactory tf = TypeFactory.getInstance();
	private InputStream input;
    private OutputStream stdout;
    private OutputStream stderr;
    private String currentPrompt;
    
    private final ICallableValue parser;
    private final ICallableValue handler;
    private final ICallableValue printer;
    private final IValue initConfig;
    private final ICallableValue completor;
    private final ICallableValue varWatcher;
    private final IValueFactory vf;
    
    
    private MutableValueGraph<GraphNode, String> graph;
    private GraphNode root;
    private GraphNode current;
    
    public REPLize(IValueFactory vf, IConstructor repl, InputStream input, OutputStream stderr, OutputStream stdout) throws IOException, URISyntaxException {
        this.vf = vf;
        IWithKeywordParameters<? extends IConstructor> repl2 = repl.asWithKeywordParameters();
        
        this.parser = (ICallableValue) repl2.getParameter("parser");
        
        this.handler = (ICallableValue) repl2.getParameter("newHandler");
        this.printer = (ICallableValue) repl2.getParameter("printer");
        this.initConfig = (IValue) repl2.getParameter("initConfig");
        
        this.completor = (ICallableValue) repl2.getParameter("tabcompletor");
        
        this.varWatcher = (ICallableValue) repl2.getParameter("varWatcher");
        
        this.graph = ValueGraphBuilder.directed().build();
        
        this.input = input;
        this.stderr = stderr;
        
        this.stdout = stdout;
        assert stdout != null;
        
        this.root =  new GraphNode("", "Root", this.initConfig);
        this.current = this.root;
        
        this.graph.addNode(this.current);
    }

    @Override
    public void initialize(InputStream input, OutputStream stdout, OutputStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.input = input;
    }

    @Override
    public String getPrompt() {
        return currentPrompt;
    }
    int c = 0;
    @Override
    public void handleInput(String code, Map<String, InputStream> output, Map<String, String> metadata)
        throws InterruptedException {
    	c++;
        // The cell id comes as part of the meta-data to avoid having to change the ILanguageProtocol.
        String cellId = metadata.get("cell_id"); // This is used as the value of the edge
        
        String currentNode;
        // TODO: FIX. This is only to simulate
    	if (cellId == null) {
    		cellId = "cell-"+c;
    		currentNode = "Root";
    	}
    	else {
    		// Front-end current node. In fact, this is the last executed node.
    		currentNode = metadata.get("current_cell"); // Represents the context to use for the execution.
        
    	}
        
        // If they are different means that the user wants to create a new path from a previous execution.
        // Result is a string representation of the config.
        if (cellId != null && !current.getResult().equals("Root") && !currentNode.equals(current.hashCode() + "")) {
            this.current = getNode(currentNode);
        }
        
        // Execute the current cell (code) received as param.
        IConstructor parseTee = parseCodeSnippet(code);
        IValue result = interpretCode(parseTee);
        
        try {
			processResult(code, result, output, metadata, cellId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public GraphNode getNode(String result) {
        for (GraphNode node : graph.nodes()) {
            String hash = node.hashCode()+"";
            if (hash.equals(result))
                return node;
        }
        return null;
    }
    
    // TODO: Complete passing new context after each execution.
    @Deprecated
    public void runPredecessors(GraphNode currentNode) {
        // context
        Collection<GraphNode> predecesors = graph.predecessors(currentNode); // At most 1.
        if (!predecesors.isEmpty()) {
            GraphNode predecessir = predecesors.iterator().next();
            runPredecessors(predecessir);
            // context = result
        }
//        IConstructor result = interpretCode(currentNode.getSourceCode());            
        // do something with the result and return.
    }
    
    public IConstructor parseCodeSnippet(String code) {
        return (IConstructor) call(parser, new Type[] { tf.stringType()}, new IValue[] { vf.string(code)});
    }
    
    public IValue interpretCode(IConstructor parseTee) {
//    	if (this.current.getConfig() instanceof RascalFunction)
    	return call(handler, new Type[] { parseTee.getType(), this.current.getConfig().getType() }, new IValue[] { parseTee, (IValue) this.current.getConfig() });
//    	else {
//    		RascalFunction c = (RascalFunction) this.current.getConfig();
//    		return (IConstructor) call(handler, new Type[] { parseTee.getType(), c.getReturnType() }, new IValue[] { parseTee, (IValue) this.current.getConfig() });
//    	}
    }
    
    //TODO
    public IConstructor printAnswer(IValue oldConfig, IValue newConfig) {
        return (IConstructor) call(printer, new Type[] {oldConfig.getType(), newConfig.getType() }, new IValue[] {oldConfig, newConfig});
    }
    
    public IValue getVariableWatcher(IValue newConfig) {
        return call(varWatcher, new Type[] { newConfig.getType() }, new IValue[] {newConfig});
    }
    
    /**
     * Process the result from the interpreter and produces the result for Bacatá
     * @param commandResult Interpreter result
     * @param output 
     * @param metadata
     * @param cellId this is the cell number in the front-end.
     * @throws Exception 
     */
    public void processResult(String input, IValue newconfig, Map<String, InputStream> output, Map<String, String> metadata, String cellId) throws Exception {
//    		throw new Exception("hos");
        if (newconfig != null) {
            GraphNode tmp =  new GraphNode(input, newconfig.toString(), newconfig);
            graph.putEdgeValue(current, tmp, cellId);

            IConstructor result = printAnswer(this.current.getConfig(), newconfig);
            IConstructor response = (IConstructor) result.get(0);
            this.current = tmp;
            
            String content = response.get("content").toString();
            content = content.replace("\\", "").replace("\"", "");
            
            // used by the jupyterlab widget. 
            IMap variableWatcher = (IMap) getVariableWatcher(newconfig);
            metadata.put("config", variableWatcher.toString());
            
            
          output.put("text/html", stringStream(content));
          addGraph2Metadata(metadata); 
        }
    }

    /**
     * Encode the graph as part of the meta-data
     * @param metadata
     */
    public void addGraph2Metadata(Map<String, String> metadata) {
        Set<CustomEdge> edges = extractEdges(graph.edges());
        Set<ExecutionGraph.CustomNode> nodes = extractNodes(graph.nodes());
        ExecutionGraph n = new ExecutionGraph("" + current.hashCode(), nodes, edges);
        
        try {
            metadata.put("Graph", new Gson().toJson(n));
        }
        catch (Exception e) {
            e.getStackTrace();
        }
    }
    
    public Set<CustomNode> extractNodes(Set<GraphNode> set) {
        Set<CustomNode> rta = new HashSet<ExecutionGraph.CustomNode>();
        for (GraphNode graphNode : set) {
            rta.add(new CustomNode(graphNode.getSourceCode(), graphNode.getResult(), "" + graphNode.hashCode()));
        }
        return rta;
    }
    /**
     * Transform Guava Edge (EndpointPair) objects into CustomEdges (Simplified version)
     * @param set
     * @return
     */
    public Set<CustomEdge> extractEdges(Set<EndpointPair<GraphNode>> set) {
        Set<CustomEdge> newEdges =  new HashSet<CustomEdge>();
        
        for (EndpointPair<GraphNode> endpointPair : set) {
            String nodeU = endpointPair.nodeU().hashCode() + "";
            String nodeV = endpointPair.nodeV().hashCode()  +"";
            Optional<String> value  = graph.edgeValue(endpointPair);
            
            newEdges.add(new CustomEdge(nodeU, nodeV, value.get()));
        }
        return newEdges;
    }
    
    private InputStream stringStream(String x) {
        return new ByteArrayInputStream(x.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void handleReset(Map<String, InputStream> output, Map<String, String> metadata)
        throws InterruptedException {
        handleInput("", output, metadata);
    }

    @Override
    public boolean supportsCompletion() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean printSpaceAfterFullCompletion() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CompletionResult completeFragment(String line, int cursor) {
        ITuple result = (ITuple)call(completor, new Type[] { tf.stringType(), tf.integerType(), this.current.getConfig().getType() },
                        new IValue[] { vf.string(line), vf.integer(cursor), this.current.getConfig() }); 

        
        List<String> suggestions = new ArrayList<>();
        int offset = ((IInteger) result.get(0)).intValue();
        IList suggs = (IList) result.get(1); 
        
        for (IValue v: suggs) {
            suggestions.add(((IString)v).getValue());
        }
        return new CompletionResult(offset, suggestions);
    }

    @Override
    public void cancelRunningCommandRequested() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void terminateRequested() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stackTraceRequested() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isStatementComplete(String command) {
        return true;
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        
    }
    
    private IValue call(ICallableValue f, Type[] types, IValue[] args) {
        synchronized (f.getEval()) {
            Evaluator eval = (Evaluator) f.getEval();
            OutputStream prevErr = eval.getStdErr();
            OutputStream prevOut = eval.getStdOut();
            try {
                eval.overrideDefaultWriters(input, stdout, stderr);
                return f.call(types, args, null).getValue();
            }
            finally {
                try {
                    stdout.flush();
                    stderr.flush();
                    eval.overrideDefaultWriters(eval.getInput(), prevOut, prevErr);
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
    }
    
}


