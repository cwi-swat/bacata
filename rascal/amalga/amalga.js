/**
 * Created by mveranom on 11/05/2017.
 */
/* Based on the example definition of a simple mode that understands a subset of JavaScript:
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
  CodeMirror.defineSimpleMode('amalga', {
    // The start state contains the rules that are intially used
    start: [
      // The regex matches the token, the token property contains the type
      {
        regex: /"(?:[^\\]|\\.)*?(?:"|$)/,
        token: 'string',
      },
      // You can match multiple tokens at once. Note that the captured
      // groups must span the whole string in this case
      {
        regex: /(function)(\s+)([a-z$][\w$]*)/,
        token: ['keyword', null, 'variable-2'],
      },
      // Rules are matched in the order in which they appear, so there is
      // no ambiguity between this one and the one above
      {
        regex: /(?:size|func|assert|primitive|for|ones|min|uint8_t|uint16_t|identity|cast|c|false|int32_t|img|in|renderImage|do|run|zeros|randomInt|module|uint32_t|true|o|int|return|case|exec|while|loadImage|od|x|function|y|Evaluate|else|randomFloat|end|let|if|save|randomUint)\b/,
        token: 'keyword',
      },
      {
        regex: /true|false|null|undefined/,
        token: 'atom',
      },
      {
        regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
        token: 'number',
      },
      {
        regex: /##.*/,
        token: 'comment',
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