cmake_minimum_required(VERSION 3.13)

project(tree-sitter-rama
  VERSION "0.1.0"
  DESCRIPTION "Rama-like surface syntax grammar for tree-sitter"
  LANGUAGES C)

option(BUILD_SHARED_LIBS "Build using shared libraries" ON)

set(TREE_SITTER_ABI_VERSION 15 CACHE STRING "Tree-sitter ABI version")
if(NOT ${TREE_SITTER_ABI_VERSION} MATCHES "^[0-9]+$")
  unset(TREE_SITTER_ABI_VERSION CACHE)
  message(FATAL_ERROR "TREE_SITTER_ABI_VERSION must be an integer")
endif()

find_program(TREE_SITTER_CLI tree-sitter DOC "Tree-sitter CLI")

add_custom_command(
  OUTPUT "${CMAKE_CURRENT_SOURCE_DIR}/src/parser.c"
  DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/grammar.js"
  COMMAND "${TREE_SITTER_CLI}" generate
  WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
  COMMENT "Generating parser.c")

add_library(tree-sitter-rama src/parser.c)
target_include_directories(tree-sitter-rama PRIVATE src)

set_target_properties(tree-sitter-rama
  PROPERTIES
    C_STANDARD 11
    POSITION_INDEPENDENT_CODE ON)

install(TARGETS tree-sitter-rama LIBRARY DESTINATION lib)
