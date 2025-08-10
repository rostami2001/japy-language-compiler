# japy-language-compiler

A compiler for the JAPY programming language developed as part of the Compiler Design course at Spring 2024.

## Project Overview

This project implements a complete compiler for the JAPY language (a Python/Java hybrid with custom modifications) using ANTLR4. The development followed a phased approach:

1. **Phase 1**: Lexical and syntactic analysis using ANTLR
2. **Phase 2**: Symbol table construction and scope management
3. **Phase 3**: Semantic analysis and error detection

## JAPY Language Features

- Object-oriented hybrid language combining Python/Java syntax
- Case-sensitive syntax
- Control structures: if/elif/else, while loops
- Data types: double, bool, string, and arrays
- Comprehensive scope management (class/method/block levels)


## Example JAPY Code

```japy
MAIN class Sample begin
    func main() returns double begin
        double a = 2.0
        var b = 1
        if(a == b)
            sout("True")
        else
            sout("False")
        return 1.0
    end
end
```

## Build & Execution

1. **Prerequisites:**
   
   java -version  # Requires Java 11+
   
   antlr4         # Requires ANTLR 4.9.3

3. **Generate parser:**
   
   antlr4 japy.g4 -o src/gen

4. **Compile and run:**
   
   javac src/compiler/*.java src/gen/*.java
   
   java -cp . src.compiler.Compiler samples/test.txt


## Sample Output

**Sample Output**

  <class 'Sample'>
    <function 'main', parameters: []>
    ...
    </function return (double)>
  </class>


**Symbol table output:**

  --- program:"samples/test.txt" ---
  key = class_Sample, value = {kind='class', name='Sample'}
  key = method_main, value = {kind='method', name='main', type='double'}


## Key Components

- **ProgramPrinter.java**: Generates structured output of the program's abstract syntax tree
- **SymbolTable.java**: Manages scopes and symbol information using hierarchical hash tables
- **SemanticError.java**: Performs type checking and detects semantic errors
