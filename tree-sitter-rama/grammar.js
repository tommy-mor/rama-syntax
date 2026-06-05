/**
 * @file Rama-like surface syntax (see examples/first.rama)
 */

/// <reference types="tree-sitter-cli/dsl" />
// @ts-check

module.exports = grammar({
  name: 'rama',

  extras: $ => [
    /\s/,
    $.comment,
  ],

  conflicts: $ => [
    [$._expression, $.map_expression],
    [$._expression, $.map_entry],
    [$.effect_statement],
  ],

  rules: {
    source_file: $ => repeat(choice($.ramaop_definition, $.ramafn_definition)),

    kw_ramaop: _ => token(prec(10, 'ramaop')),

    kw_ramafn: _ => token(prec(10, 'ramafn')),

    ramaop_definition: $ =>
      seq(
        $.kw_ramaop,
        field('name', $.operator_name),
        '(',
        field('parameters', optional($.parameter_list)),
        ')',
        field('body', $.block),
      ),

    ramafn_definition: $ =>
      seq(
        $.kw_ramafn,
        field('name', $.binding_name),
        '(',
        field('parameters', optional($.parameter_list)),
        ')',
        field('body', $.inline_block),
      ),

    block: $ => seq('{', repeat($.statement), '}'),

    inline_binding: $ =>
      seq(
        optional(field('value', $._expression)),
        $.binding_pipe,
        field('target', choice($.binding_target, $.call_expression)),
        optional($._semicolon),
      ),

    inline_block: $ => seq('{', repeat1($.inline_binding), '}'),

    statement: $ =>
      choice(
        $.anchor_statement,
        $.effect_statement,
        $.transform_statement,
        $.select_statement,
        $.hook_statement,
        $.unify_statement,
        $.if_statement,
        $.atomic_statement,
        $.sink_statement,
        $.ramafn_statement,
      ),

    ramafn_statement: $ =>
      prec(
        10,
        seq(
          $.kw_ramafn,
          field('name', $.binding_name),
          '(',
          field('parameters', optional($.parameter_list)),
          ')',
          field('body', $.inline_block),
          optional($._semicolon),
        ),
      ),

    kw_anchor: _ => token(prec(10, 'anchor')),

    anchor_statement: $ =>
      prec(
        10,
        seq($.kw_anchor, field('anchor', $.anchor_reference), $._semicolon),
      ),

    effect_statement: $ =>
      prec(
        -1,
        seq(
        field('value', $._expression),
        optional(
          seq(
            $.binding_pipe,
            field('target', $.binding_target),
            optional(seq('as', field('alias', $.binding_name))),
          ),
        ),
        optional($._semicolon),
        ),
      ),

    binding_statement: $ =>
      seq(
        field('value', $._expression),
        $.binding_pipe,
        field('target', $.binding_target),
        optional(seq('as', field('alias', $.binding_name))),
        $._semicolon,
      ),

    transform_statement: $ =>
      prec(
        2,
        seq(
          field('pstate', $.pstate_reference),
          '!<--',
          field('path', $.path_expression),
          optional('>'),
          $._semicolon,
        ),
      ),

    select_statement: $ =>
      prec(
        2,
        seq(
        field('pstate', $.pstate_reference),
        '-->',
        field('path', $.path_expression),
        optional(seq($.binding_pipe, field('target', $.binding_target))),
        $._semicolon,
        ),
      ),

    hook_statement: $ =>
      prec(
        2,
        choice(
        seq(
          'hook',
          ':',
          field('name', $.qualified_name),
          optional(seq('(', field('argument', $._expression), ')')),
          optional($._semicolon),
        ),
        seq('hook', field('anchor', $.anchor_reference), $._semicolon),
        ),
      ),

    unify_statement: $ =>
      prec(
        2,
        seq(
        'unify>',
        '(',
        field('anchors', $.anchor_list),
        ')',
        $._semicolon,
        ),
      ),

    if_statement: $ =>
      prec(
        2,
        seq(
          'if',
          '(',
          field('condition', $._expression),
          ')',
          field('consequence', $.block),
          optional($._trailing_semicolon),
          optional(seq('else', field('alternative', $.block), optional($._trailing_semicolon))),
        ),
      ),

    atomic_statement: $ =>
      prec(2, seq('atomic', field('body', $.block))),

    sink_statement: $ => seq('>', field('target', $.binding_target), $._semicolon),

    parameter_list: $ => commaSep1($.parameter),

    parameter: $ => choice($.binding_name, $.identifier),

    binding_target: $ =>
      choice($.binding_name, $.destructure_map, $.destructure_list),

    binding_name: _ => token(seq(choice('*', '%'), /[\w.-]+/)), // hyphen at end of class

    anchor_list: $ => commaSep1($.anchor_reference),

    anchor_reference: _ => token(seq('<', /[\w-]+/, '>')), // hyphen at end

    pstate_reference: _ => token(seq('$$', /[\w.-]+/)), // hyphen at end

    path_expression: $ => seq($._path_segment, repeat(seq(',', $._path_segment))),

    _path_segment: $ => $._expression,

    call_expression: $ =>
      seq(
        field('function', $._callable),
        '(',
        field('arguments', optional($.argument_list)),
        ')',
      ),

    _callable: $ =>
      choice($.operator_name, $.qualified_name, $.simple_identifier, $.keyword),

    // Trailing `>` for Rama operators (send-emits>, hook:emit>), not `->` or binding ` > `.
    operator_name: $ => token(prec(1, /[a-z][\w%|*.:/-]*(?:[\/:]|[\w])>/)),

    argument_list: $ => seq($.argument, repeat(seq(optional(','), $.argument))),

    argument: $ => $._expression,

    list_expression: $ =>
      seq('[', optional($.list_elements), ']'),

    list_elements: $ =>
      seq($._expression, repeat(seq(optional(','), $._expression))),

    map_expression: $ =>
      seq('{', optional(commaSep($.map_entry)), '}'),

    map_entry: $ =>
      seq(
        field('key', $._expression),
        optional(field('value', $._expression)),
      ),

    destructure_list: $ => $.list_expression,

    destructure_map: $ => $.map_expression,

    string_literal: _ => token(seq('"', repeat(choice(/[^"\\]/, /\\./)), '"')),

    keyword: _ => token(seq(':', /[\w-]+/)), // hyphen at end

    identifier: $ => choice($.qualified_name, $.simple_identifier),

    qualified_name: $ =>
      token(
        prec(
          -1,
          /[\w%|*./>:-]+(?:[\/:][\w%|*./>:-]+)+|[\w%|*./>:-]*->[\w%|*./>:-]+(?:[\/:][\w%|*./>:-]+)*/,
        ),
      ),

    simple_identifier: _ => token(prec(-1, /[=?\w%|*.-]+/)),

    pipe_variant: _ => token(seq('|', /[\w-]+/)), // hyphen at end

    _expression: $ =>
      choice(
        $.call_expression,
        $.list_expression,
        $.map_expression,
        $.string_literal,
        $.anchor_reference,
        $.keyword,
        $.binding_name,
        $.pstate_reference,
        $.pipe_variant,
        $.identifier,
      ),

    comment: _ =>
      token(choice(seq('//', /.*/), seq('/*', /[^*]*\*+([^/*][^*]*\*+)*/, '/'))),

    binding_pipe: _ => token(prec(5, seq(/\s+/, '>', /\s+/))),

    _semicolon: _ => ';',

    _trailing_semicolon: _ => ';',
  },
});

function commaSep1(rule) {
  return seq(rule, repeat(seq(',', rule)));
}

function commaSep(rule) {
  return optional(commaSep1(rule));
}
