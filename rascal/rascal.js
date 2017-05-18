/**
 * Created by Mauricio Verano Merino on 16/05/2017.
 */

(function(mod) {
  if (typeof exports == 'object' && typeof module == 'object') // CommonJS
    mod(require('../../lib/codemirror'), require('../../addon/mode/simple'));
  else if (typeof define == 'function' && define.amd) // AMD
    define(['../../lib/codemirror', '../../addon/mode/simple'], mod);
  else // Plain browser env
    mod(CodeMirror);
})(function(CodeMirror) {
  'use strict';
  CodeMirror.defineSimpleMode('rascal', {
    // The start state contains the rules that are intially used
    start: [
      // The regex matches the token, the token property contains the type
      {
        regex: /"(?:[^\\]|\\.)*?(?:"|$)/,
        token: 'string',
      },
      {
        regex: /".+"|'.+'|$/,
        token: 'string',
      },
      {
        regex: /\b(syntax|keyword|lexical|break|continue|finally|private|fail|filter|if|tag|extend|append|non-assoc|assoc|test|anno|layout|data|join|it|bracket|in|import|all|solve|try|catch|notin|else|insert|switch|return|case|while|throws|visit|for|assert|default|map|alias|any|module|mod|public|one|throw|start)\b/,
        token: 'keyword',
      },
      {
        regex: /\b(true|false|value|loc|node|num|type|bag|int|rat|rel|lrel|real|tuple|str|bool|void|datetime|set|map|list)\b/,
        token: 'atom',
      },
      {
        regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
        token: 'number',
      },
      {
        regex: /\/\/.*$\n?/,
        token: 'comment',
      },
      {
        regex: /@[\w|\s]+{\n?/,
        token: 'comment',
        next: 'annotation',
        indent: true
      },
      {
        regex: /\/(?:[^\\]|\\.)*?\//,
        token: 'variable-3',
      },
      // A next property will cause the mode to move to a different state
      {
        regex: /\/\*/,
        token: 'comment',
        next: 'comment',
      },
      {
        regex: /[-+\/*=<>!]+/,
        token: 'operator',
      },
      // indent and dedent properties guide autoindentation
      {
        regex: /[\{\[\(]/,
        indent: true,
      },
      {
        regex: /[\}\]\)]/,
        dedent: true,
      },
      {
        regex: /[a-z$][\w$]*/,
        token: 'variable',
      },
      // You can embed other modes with the mode property. This rule
      // causes all code between << and >> to be highlighted with the XML
      // mode.
      {
        regex: /<</,
        token: 'meta',
        mode: {spec: 'xml', end: />>/},
      },
    ],
    annotation: [
      {regex: /.*}/, token: 'comment',dedent: true, next: 'start'},
      {regex: /[.*|\s*|\w*|<>():,;`-]+/, token: 'comment'}
    ],
    // The multi-line comment state.
    comment: [
      {regex: /.*?\*\//, token: 'comment', next: 'start'},
      {regex: /.*/, token: 'comment'},
    ],
    // The meta property contains global information about the mode. It
    // can contain properties like lineComment, which are supported by
    // all modes, and also directives like dontIndentStates, which are
    // specific to simple modes.
    meta: {
      dontIndentStates: ['comment'],
      lineComment: '//',
    },
  });
});