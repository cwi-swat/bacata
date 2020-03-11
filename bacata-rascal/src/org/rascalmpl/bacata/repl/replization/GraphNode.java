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

import java.util.Objects;

import io.usethesource.vallang.IValue;


public final class GraphNode {

    private final String sourceCode;
    
    private final String result;
    
    private final IValue config;

    public GraphNode(String sourceCode, String result, IValue conf) {
        this.sourceCode = sourceCode;
        this.result = result;
        this.config = conf;
    }
    
    
    public String getSourceCode() {
        return sourceCode;
    }

    public String getResult() {
        return result;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof GraphNode) {
        GraphNode that = (GraphNode) other;
        return this.config.equals(that.config);
//        return this.sourceCode.equals(that.sourceCode)
//            && this.result == that.result;
      }
      return false;
    }
    
    
    @Override
    public int hashCode() {
      return Objects.hash(this.sourceCode, this.result);
    }
    
    @Override
    public String toString() {
      return "(" + sourceCode + ", " + this.result + "," + this.config + ")";
    }

    public IValue getConfig() {
        return config;
    }
    
}
