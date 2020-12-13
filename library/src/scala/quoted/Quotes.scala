package scala.quoted

import scala.reflect.TypeTest

/** Current Quotes in scope */
inline def quotes(using q: Quotes): q.type = q

/** Quotation context provided by a macro expansion or in the scope of `scala.quoted.staging.run`.
 *  Used to perform all operations on quoted `Expr` or `Type`.
 *
 *  It contains the low-level Typed AST API metaprogramming API.
 *  This API does not have the static type guarantiees that `Expr` and `Type` provide.
 */
trait Quotes { self: runtime.QuoteUnpickler & runtime.QuoteMatching =>

  // Extension methods for `Expr[T]`
  extension [T](self: Expr[T])
    /** Show a source code like representation of this expression */
    def show: String

    /** Pattern matches `this` against `that`. Effectively performing a deep equality check.
    *  It does the equivalent of
    *  ```
    *  this match
    *    case '{...} => true // where the contents of the pattern are the contents of `that`
    *    case _ => false
    *  ```
    */
    def matches(that: Expr[Any]): Boolean

    @deprecated("Use `.value` instead. This will be removed in 3.0.0-RC1", "3.0.0-M3")
    def unlift(using FromExpr[T]): Option[T] = self.value

    @deprecated("Use `.valueOrError` instead. This will be removed in 3.0.0-RC1", "3.0.0-M3")
    def unliftOrError(using FromExpr[T]): T = self.valueOrError

    /** Return the value of this expression.
     *
     *  Returns `None` if the expression does not represent a value or possibly contains side effects.
     *  Otherwise returns the `Some` of the value.
     */
    def value(using FromExpr[T]): Option[T] =
      given Quotes = Quotes.this
      summon[FromExpr[T]].unapply(self)

    /** Return the value of this expression.
     *
     *  Emits an error and throws if the expression does not represent a value or possibly contains side effects.
     *  Otherwise returns the value.
     */
    def valueOrError(using FromExpr[T]): T =
      val fromExpr = summon[FromExpr[T]]
      def reportError =
        val msg = s"Expected a known value. \n\nThe value of: ${self.show}\ncould not be extracted using $fromExpr"
        reflect.report.throwError(msg, self)
      given Quotes = Quotes.this
      fromExpr.unapply(self).getOrElse(reportError)

  end extension

  // Extension methods for `Expr[Any]` that take another explicit type parameter
  extension [X](self: Expr[Any])
    /** Checks is the `quoted.Expr[?]` is valid expression of type `X` */
    def isExprOf(using Type[X]): Boolean

    /** Convert this to an `quoted.Expr[X]` if this expression is a valid expression of type `X` or throws */
    def asExprOf(using Type[X]): Expr[X]
  end extension

  /** Low-level Typed AST metaprogramming API.
   *
   *  Provides all functionality related to AST-based metaprogramming.
   *
   *  Usage:
   *  ```scala
   *  import scala.quoted._
   *  def f(expr: Expr[Int])(using Quotes) =
   *    import quotes.reflect._
   *    val ast: Term = expr.asTerm
   *    ...
   *  ```
   *
   *  See `reflectModule` for full API.
   *
   *  @syntax markdown
   */
  val reflect: reflectModule

  /** Low-level Typed AST metaprogramming API.
   *
   *  Provides all functionality related to AST-based metaprogramming.
   *
   *  Each type `XYZ` in the API is defined as an abstract type `type XYZ`.
   *  Methods on `XYZ` are provided by a `given XYZMethods` which implements extension methods on `XYZ` in the trait `XYZMethods`.
   *  The `XYZ` module is defined by a `val XYZ: XYZModule` which contains the methods defined in `XYZModule`.
   *  Type tests (`TypeTest`) are also given to perform subtype checks on these types.
   *
   *  Type hierarchy
   *  ```none
   *
   *  +- Tree -+- PackageClause
   *           |
   *           +- Statement -+- Import
   *           |             +- Export
   *           |             +- Definition --+- ClassDef
   *           |             |               +- TypeDef
   *           |             |               +- DefDef
   *           |             |               +- ValDef
   *           |             |
   *           |             +- Term --------+- Ref -+- Ident
   *           |                             |       +- Select
   *           |                             |
   *           |                             +- Literal
   *           |                             +- This
   *           |                             +- New
   *           |                             +- NamedArg
   *           |                             +- Apply
   *           |                             +- TypeApply
   *           |                             +- Super
   *           |                             +- Typed
   *           |                             +- Assign
   *           |                             +- Block
   *           |                             +- Closure
   *           |                             +- If
   *           |                             +- Match
   *           |                             +- SummonFrom
   *           |                             +- Try
   *           |                             +- Return
   *           |                             +- Repeated
   *           |                             +- Inlined
   *           |                             +- SelectOuter
   *           |                             +- While
   *           |
   *           |
   *           +- TypeTree ----+- Inferred
   *           |               +- TypeIdent
   *           |               +- TypeSelect
   *           |               +- TypeProjection
   *           |               +- Singleton
   *           |               +- Refined
   *           |               +- Applied
   *           |               +- Annotated
   *           |               +- MatchTypeTree
   *           |               +- ByName
   *           |               +- LambdaTypeTree
   *           |               +- TypeBind
   *           |               +- TypeBlock
   *           |
   *           +- TypeBoundsTree
   *           +- WildcardTypeTree
   *           |
   *           +- CaseDef
   *           |
   *           +- TypeCaseDef
   *           +- Bind
   *           +- Unapply
   *           +- Alternatives
   *
   *
   *  +- TypeRepr -+- NamedType -+- TermRef
   *               |             +- TypeRef
   *               +- ConstantType
   *               +- SuperType
   *               +- Refinement
   *               +- AppliedType
   *               +- AnnotatedType
   *               +- AndOrType -+- AndType
   *               |             +- OrType
   *               +- MatchType
   *               +- ByNameType
   *               +- ParamRef
   *               +- ThisType
   *               +- RecursiveThis
   *               +- RecursiveType
   *               +- LambdaType -+- MethodOrPoly -+- MethodType
   *               |              |                +- PolyType
   *               |              +- TypeLambda
   *               +- MatchCase
   *               +- TypeBounds
   *               +- NoPrefix
   *
   *  +- Selector -+- SimpleSelector
   *               +- RenameSelector
   *               +- OmitSelector
   *               +- GivenSelector
   *
   *  +- Signature
   *
   *  +- Position
   *
   *  +- SourceFile
   *
   *  +- Constant -+- BooleanConstant
   *               +- ByteConstant
   *               +- ShortConstant
   *               +- IntConstant
   *               +- LongConstant
   *               +- FloatConstant
   *               +- DoubleConstant
   *               +- CharConstant
   *               +- StringConstant
   *               +- UnitConstant
   *               +- NullConstant
   *               +- ClassOfConstant
   *  +- Symbol
   *
   *  +- Flags
   *
   *  ```
   *
   * @syntax markdown
   */
  trait reflectModule { self: reflect.type =>

    /** Returns the `Term` representation this expression */
    extension (expr: Expr[Any])
      def asTerm: Term

    ///////////////
    //   TREES   //
    ///////////////

    /** Tree representing code written in the source */
    type Tree <: AnyRef

    /** Module object of `type Tree`  */
    val Tree: TreeModule

    /** Methods of the module object `val Tree` */
    trait TreeModule { this: Tree.type =>
      /** Returns the Term representation this expression */
      @deprecated("Use `expr.asTerm` instead (must `import quotes.reflect._`). This will be removed in 3.0.0-RC1", "3.0.0-M3")
      def of(expr: Expr[Any]): Tree = expr.asTerm
    }

    /** Makes extension methods on `Tree` available without any imports */
    given TreeMethods: TreeMethods

    /** Extension methods of `Tree` */
    trait TreeMethods {

      extension (self: Tree)
        /** Position in the source code */
        def pos: Position

        /** Symbol of defined or referred by this tree */
        def symbol: Symbol

        /** Shows the tree as String */
        def show(using Printer[Tree]): String

        /** Does this tree represent a valid expression? */
        def isExpr: Boolean

        /** Convert this tree to an `quoted.Expr[Any]` if the tree is a valid expression or throws */
        def asExpr: Expr[Any]
      end extension

      /** Convert this tree to an `quoted.Expr[T]` if the tree is a valid expression or throws */
      extension [T](self: Tree)
        def asExprOf(using Type[T]): Expr[T]

      extension [ThisTree <: Tree](self: ThisTree)
        /** Changes the owner of the symbols in the tree */
        def changeOwner(newOwner: Symbol): ThisTree
      end extension

    }

    /** Tree representing a pacakage clause in the source code */
    type PackageClause <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `PackageClause` */
    given PackageClauseTypeTest: TypeTest[Tree, PackageClause]

    /** Module object of `type PackageClause`  */
    val PackageClause: PackageClauseModule

    /** Methods of the module object `val PackageClause` */
    trait PackageClauseModule { this: PackageClause.type =>
      def apply(pid: Ref, stats: List[Tree]): PackageClause
      def copy(original: Tree)(pid: Ref, stats: List[Tree]): PackageClause
      def unapply(tree: PackageClause): (Ref, List[Tree])
    }

    /** Makes extension methods on `PackageClause` available without any imports */
    given PackageClauseMethods: PackageClauseMethods

    /** Extension methods of `PackageClause` */
    trait PackageClauseMethods where
      extension (self: PackageClause)
        def pid: Ref
        def stats: List[Tree]
      end extension
    end PackageClauseMethods

    /** Tree representing an import in the source code */
    type Import <: Statement

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Import` */
    given ImportTypeTest: TypeTest[Tree, Import]

    /** Module object of `type Import`  */
    val Import: ImportModule

    /** Methods of the module object `val Import` */
    trait ImportModule { this: Import.type =>
      def apply(expr: Term, selectors: List[Selector]): Import
      def copy(original: Tree)(expr: Term, selectors: List[Selector]): Import
      def unapply(tree: Import): (Term, List[Selector])
    }

    /** Makes extension methods on `Import` available without any imports */
    given ImportMethods: ImportMethods

    /** Extension methods of `Import` */
    trait ImportMethods where
      extension (self: Import)
        def expr: Term
        def selectors: List[Selector]
      end extension
    end ImportMethods

    /** Tree representing an export clause in the source code.
     *  Export forwarders generated from this clause appear in the same scope.
     */
    type Export <: Statement

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Export` */
    given ExportTypeTest: TypeTest[Tree, Export]

    /** Module object of `type Export`  */
    val Export: ExportModule

    /** Methods of the module object `val Export` */
    trait ExportModule { this: Export.type =>
      def unapply(tree: Export): (Term, List[Selector])
    }

    /** Makes extension methods on `Export` available without any imports */
    given ExportMethods: ExportMethods

    /** Extension methods of `Export` */
    trait ExportMethods where
      extension (self: Export)
        def expr: Term
        def selectors: List[Selector]
      end extension
    end ExportMethods

    /** Tree representing a statement in the source code */
    type Statement <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Statement` */
    given StatementTypeTest: TypeTest[Tree, Statement]

    // ----- Definitions ----------------------------------------------

    /** Tree representing a definition in the source code. It can be `ClassDef`, `TypeDef`, `DefDef` or `ValDef` */
    type Definition <: Statement

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Definition` */
    given DefinitionTypeTest: TypeTest[Tree, Definition]

    /** Module object of `type Definition`  */
    val Definition: DefinitionModule

    /** Methods of the module object `val Definition` */
    trait DefinitionModule { this: Definition.type => }

    /** Makes extension methods on `Definition` available without any imports */
    given DefinitionMethods: DefinitionMethods

    /** Extension methods of `Definition` */
    trait DefinitionMethods where
      extension (self: Definition)
        def name: String
      end extension
    end DefinitionMethods

    // ClassDef

    /** Tree representing a class definition. This includes anonymous class definitions and the class of a module object */
    type ClassDef <: Definition

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `ClassDef` */
    given ClassDefTypeTest: TypeTest[Tree, ClassDef]

    /** Module object of `type ClassDef`  */
    val ClassDef: ClassDefModule

    /** Methods of the module object `val ClassDef` */
    trait ClassDefModule { this: ClassDef.type =>
      // TODO def apply(name: String, constr: DefDef, parents: List[TermOrTypeTree], selfOpt: Option[ValDef], body: List[Statement]): ClassDef
      def copy(original: Tree)(name: String, constr: DefDef, parents: List[Tree /* Term | TypeTree */], derived: List[TypeTree], selfOpt: Option[ValDef], body: List[Statement]): ClassDef
      def unapply(cdef: ClassDef): (String, DefDef, List[Tree /* Term | TypeTree */], List[TypeTree], Option[ValDef], List[Statement])
    }

    /** Makes extension methods on `ClassDef` available without any imports */
    given ClassDefMethods: ClassDefMethods

    /** Extension methods of `ClassDef` */
    trait ClassDefMethods where
      extension (self: ClassDef)
        def constructor: DefDef
        def parents: List[Tree /* Term | TypeTree */]
        def derived: List[TypeTree]
        def self: Option[ValDef]
        def body: List[Statement]
      end extension
    end ClassDefMethods

    // DefDef

    /** Tree representing a method definition in the source code */
    type DefDef <: Definition

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `DefDef` */
    given DefDefTypeTest: TypeTest[Tree, DefDef]

    /** Module object of `type DefDef`  */
    val DefDef: DefDefModule

    /** Methods of the module object `val DefDef` */
    trait DefDefModule { this: DefDef.type =>
      def apply(symbol: Symbol, rhsFn: List[TypeRepr] => List[List[Term]] => Option[Term]): DefDef
      def copy(original: Tree)(name: String, typeParams: List[TypeDef], paramss: List[List[ValDef]], tpt: TypeTree, rhs: Option[Term]): DefDef
      def unapply(ddef: DefDef): (String, List[TypeDef], List[List[ValDef]], TypeTree, Option[Term])
    }

    /** Makes extension methods on `DefDef` available without any imports */
    given DefDefMethods: DefDefMethods

    /** Extension methods of `DefDef` */
    trait DefDefMethods where
      extension (self: DefDef)
        def typeParams: List[TypeDef]
        def paramss: List[List[ValDef]]
        def returnTpt: TypeTree
        def rhs: Option[Term]
      end extension
    end DefDefMethods

    // ValDef

    /** Tree representing a value definition in the source code This inclues `val`, `lazy val`, `var`, `object` and parameter defintions. */
    type ValDef <: Definition

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `ValDef` */
    given ValDefTypeTest: TypeTest[Tree, ValDef]

    /** Module object of `type ValDef`  */
    val ValDef: ValDefModule

    /** Methods of the module object `val ValDef` */
    trait ValDefModule { this: ValDef.type =>
      def apply(symbol: Symbol, rhs: Option[Term]): ValDef
      def copy(original: Tree)(name: String, tpt: TypeTree, rhs: Option[Term]): ValDef
      def unapply(vdef: ValDef): (String, TypeTree, Option[Term])

      /** Creates a block `{ val <name> = <rhs: Term>; <body(x): Term> }` */
      def let(owner: Symbol, name: String, rhs: Term)(body: Ident => Term): Term

      /** Creates a block `{ val x = <rhs: Term>; <body(x): Term> }` */
      def let(owner: Symbol, rhs: Term)(body: Ident => Term): Term =
        let(owner, "x", rhs)(body)

      /** Creates a block `{ val x1 = <terms(0): Term>; ...; val xn = <terms(n-1): Term>; <body(List(x1, ..., xn)): Term> }` */
      def let(owner: Symbol, terms: List[Term])(body: List[Ident] => Term): Term
    }

    /** Makes extension methods on `ValDef` available without any imports */
    given ValDefMethods: ValDefMethods

    /** Extension methods of `ValDef` */
    trait ValDefMethods where
      extension (self: ValDef)
        def tpt: TypeTree
        def rhs: Option[Term]
      end extension
    end ValDefMethods

    // TypeDef

    /** Tree representing a type (parameter or member) definition in the source code */
    type TypeDef <: Definition

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeDef` */
    given TypeDefTypeTest: TypeTest[Tree, TypeDef]

    /** Module object of `type TypeDef`  */
    val TypeDef: TypeDefModule

    /** Methods of the module object `val TypeDef` */
    trait TypeDefModule { this: TypeDef.type =>
      def apply(symbol: Symbol): TypeDef
      def copy(original: Tree)(name: String, rhs: Tree /*TypeTree | TypeBoundsTree*/): TypeDef
      def unapply(tdef: TypeDef): (String, Tree /*TypeTree | TypeBoundsTree*/ /* TypeTree | TypeBoundsTree */)
    }

    /** Makes extension methods on `TypeDef` available without any imports */
    given TypeDefMethods: TypeDefMethods

    /** Extension methods of `TypeDef` */
    trait TypeDefMethods where
      extension (self: TypeDef)
        def rhs: Tree /*TypeTree | TypeBoundsTree*/
      end extension
    end TypeDefMethods


    // ----- Terms ----------------------------------------------------

    /** Tree representing an expression in the source code */
    type Term <: Statement

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Term` */
    given TermTypeTest: TypeTest[Tree, Term]

    /** Module object of `type Term`  */
    val Term: TermModule

    /** Methods of the module object `val Term` */
    trait TermModule { this: Term.type =>

      /** Returns the Term representation this expression */
      @deprecated("Use `expr.asTerm` instead (must `import quotes.reflect._`). This will be removed in 3.0.0-RC1", "3.0.0-M3")
      def of(expr: Expr[Any]): Term = expr.asTerm

      /** Returns a term that is functionally equivalent to `t`,
      *  however if `t` is of the form `((y1, ..., yn) => e2)(e1, ..., en)`
      *  then it optimizes this the top most call by returning the `Some`
      *  with the result of beta-reducing the application.
      *  Otherwise returns `None`.
      *
      *   To retain semantics the argument `ei` is bound as `val yi = ei` and by-name arguments to `def yi = ei`.
      *   Some bindings may be elided as an early optimization.
      */
      def betaReduce(term: Term): Option[Term]

    }

    /** Makes extension methods on `Term` available without any imports */
    given TermMethods: TermMethods

    /** Extension methods of `Term` */
    trait TermMethods {
      extension (self: Term)

        /** TypeRepr of this term */
        def tpe: TypeRepr

        /** Replace Inlined nodes and InlineProxy references to underlying arguments */
        def underlyingArgument: Term

        /** Replace Ident nodes references to the underlying tree that defined them */
        def underlying: Term

        /** Converts a partally applied term into a lambda expression */
        def etaExpand(owner: Symbol): Term

        /** A unary apply node with given argument: `tree(arg)` */
        def appliedTo(arg: Term): Term

        /** An apply node with given arguments: `tree(arg, args0, ..., argsN)` */
        def appliedTo(arg: Term, args: Term*): Term

        /** An apply node with given argument list `tree(args(0), ..., args(args.length - 1))` */
        def appliedToArgs(args: List[Term]): Apply

        /** The current tree applied to given argument lists:
        *  `tree (argss(0)) ... (argss(argss.length -1))`
        */
        def appliedToArgss(argss: List[List[Term]]): Term

        /** The current tree applied to (): `tree()` */
        def appliedToNone: Apply

        /** The current tree applied to given type argument: `tree[targ]` */
        def appliedToType(targ: TypeRepr): Term

        /** The current tree applied to given type arguments: `tree[targ0, ..., targN]` */
        def appliedToTypes(targs: List[TypeRepr]): Term

        /** The current tree applied to given type argument list: `tree[targs(0), ..., targs(targs.length - 1)]` */
        def appliedToTypeTrees(targs: List[TypeTree]): Term

        /** A select node that selects the given symbol. */
        def select(sym: Symbol): Select

      end extension

    }

    /** Tree representing a reference to definition */
    type Ref <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Ref` */
    given RefTypeTest: TypeTest[Tree, Ref]

    /** Module object of `type Ref`  */
    val Ref: RefModule

    /** Methods of the module object `val Ref` */
    trait RefModule { this: Ref.type =>

      /** A tree representing the same reference as the given type */
      def term(tp: TermRef): Ref

      /** Create a reference tree from a symbol
      *
      *  If `sym` refers to a class member `foo` in class `C`,
      *  returns a tree representing `C.this.foo`.
      *
      *  If `sym` refers to a local definition `foo`, returns
      *  a tree representing `foo`.
      *
      *  @note In both cases, the constructed tree should only
      *  be spliced into the places where such accesses make sense.
      *  For example, it is incorrect to have `C.this.foo` outside
      *  the class body of `C`, or have `foo` outside the lexical
      *  scope for the definition of `foo`.
      */
      def apply(sym: Symbol): Ref
    }

    /** Tree representing a reference to definition with a given name */
    type Ident <: Ref

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Ident` */
    given IdentTypeTest: TypeTest[Tree, Ident]

    /** Module object of `type Ident`  */
    val Ident: IdentModule

    /** Methods of the module object `val Ident` */
    trait IdentModule { this: Ident.type =>
      def apply(tmref: TermRef): Term

      def copy(original: Tree)(name: String): Ident

      /** Matches a term identifier and returns its name */
      def unapply(tree: Ident): Some[String]
    }

    /** Makes extension methods on `Ident` available without any imports */
    given IdentMethods: IdentMethods

    /** Extension methods of `Ident` */
    trait IdentMethods where
      extension (self: Ident)
        def name: String
      end extension
    end IdentMethods

    /** Tree representing a selection of definition with a given name on a given prefix */
    type Select <: Ref

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Select` */
    given SelectTypeTest: TypeTest[Tree, Select]

    /** Module object of `type Select`  */
    val Select: SelectModule

    /** Methods of the module object `val Select` */
    trait SelectModule { this: Select.type =>
      /** Select a term member by symbol */
      def apply(qualifier: Term, symbol: Symbol): Select

      /** Select a field or a non-overloaded method by name
      *
      *  @note The method will produce an assertion error if the selected
      *        method is overloaded. The method `overloaded` should be used
      *        in that case.
      */
      def unique(qualifier: Term, name: String): Select

      /** Call an overloaded method with the given type and term parameters */
      def overloaded(qualifier: Term, name: String, targs: List[TypeRepr], args: List[Term]): Apply

      /** Call an overloaded method with the given type and term parameters */
      def overloaded(qualifier: Term, name: String, targs: List[TypeRepr], args: List[Term], returnType: TypeRepr): Apply

      def copy(original: Tree)(qualifier: Term, name: String): Select

      /** Matches `<qualifier: Term>.<name: String>` */
      def unapply(x: Select): (Term, String)
    }

    /** Makes extension methods on `Select` available without any imports */
    given SelectMethods: SelectMethods

    /** Extension methods of `Select` */
    trait SelectMethods where
      extension (self: Select)
        def qualifier: Term
        def name: String
        def signature: Option[Signature]
      end extension
    end SelectMethods

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Literal` */
    given LiteralTypeTest: TypeTest[Tree, Literal]

    /** Tree representing a literal value in the source code */
    type Literal <: Term

    /** Module object of `type Literal`  */
    val Literal: LiteralModule

    /** Methods of the module object `val Literal` */
    trait LiteralModule { this: Literal.type =>

      /** Create a literal constant */
      def apply(constant: Constant): Literal

      def copy(original: Tree)(constant: Constant): Literal

      /** Matches a literal constant */
      def unapply(x: Literal): Some[Constant]
    }

    /** Makes extension methods on `Literal` available without any imports */
    given LiteralMethods: LiteralMethods

    /** Extension methods of `Literal` */
    trait LiteralMethods where
      extension (self: Literal)
        def constant: Constant
      end extension
    end LiteralMethods

    /** Tree representing `this` in the source code */
    type This <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `This` */
    given ThisTypeTest: TypeTest[Tree, This]

    /** Module object of `type This`  */
    val This: ThisModule

    /** Methods of the module object `val This` */
    trait ThisModule { this: This.type =>

      /** Create a `this[<id: String]>` */
      def apply(cls: Symbol): This

      def copy(original: Tree)(qual: Option[String]): This

      /** Matches `this[<id: Option[String]>` */
      def unapply(x: This): Some[Option[String]]
    }

    /** Makes extension methods on `This` available without any imports */
    given ThisMethods: ThisMethods

    /** Extension methods of `This` */
    trait ThisMethods where
      extension (self: This)
        def id: Option[String]
      end extension
    end ThisMethods

    /** Tree representing `new` in the source code */
    type New <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `New` */
    given NewTypeTest: TypeTest[Tree, New]

    /** Module object of `type New`  */
    val New: NewModule

    /** Methods of the module object `val New` */
    trait NewModule { this: New.type =>

      /** Create a `new <tpt: TypeTree>` */
      def apply(tpt: TypeTree): New

      def copy(original: Tree)(tpt: TypeTree): New

      /** Matches a `new <tpt: TypeTree>` */
      def unapply(x: New): Some[TypeTree]
    }

    /** Makes extension methods on `New` available without any imports */
    given NewMethods: NewMethods

    /** Extension methods of `New` */
    trait NewMethods where
      extension (self: New)
        def tpt: TypeTree
      end extension
    end NewMethods

    /** Tree representing an argument passed with an explicit name. Such as `arg1 = x` in `foo(arg1 = x)` */
    type NamedArg <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `NamedArg` */
    given NamedArgTypeTest: TypeTest[Tree, NamedArg]

    /** Module object of `type NamedArg`  */
    val NamedArg: NamedArgModule

    /** Methods of the module object `val NamedArg` */
    trait NamedArgModule { this: NamedArg.type =>

      /** Create a named argument `<name: String> = <value: Term>` */
      def apply(name: String, arg: Term): NamedArg

      def copy(original: Tree)(name: String, arg: Term): NamedArg

      /** Matches a named argument `<name: String> = <value: Term>` */
      def unapply(x: NamedArg): (String, Term)
    }

    /** Makes extension methods on `NamedArg` available without any imports */
    given NamedArgMethods: NamedArgMethods

    /** Extension methods of `NamedArg` */
    trait NamedArgMethods where
      extension (self: NamedArg)
        def name: String
        def value: Term
      end extension
    end NamedArgMethods

    /** Tree an application of arguments. It represents a single list of arguments, multiple argument lists will have nested `Apply`s  */
    type Apply <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Apply` */
    given ApplyTypeTest: TypeTest[Tree, Apply]

    /** Module object of `type Apply`  */
    val Apply: ApplyModule

    /** Methods of the module object `val Apply` */
    trait ApplyModule { this: Apply.type =>

      /** Create a function application `<fun: Term>(<args: List[Term]>)` */
      def apply(fun: Term, args: List[Term]): Apply

      def copy(original: Tree)(fun: Term, args: List[Term]): Apply

      /** Matches a function application `<fun: Term>(<args: List[Term]>)` */
      def unapply(x: Apply): (Term, List[Term])
    }

    /** Makes extension methods on `Apply` available without any imports */
    given ApplyMethods: ApplyMethods

    /** Extension methods of `Apply` */
    trait ApplyMethods where
      extension (self: Apply)
        def fun: Term
        def args: List[Term]
      end extension
    end ApplyMethods

    /** Tree an application of type arguments */
    type TypeApply <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeApply` */
    given TypeApplyTypeTest: TypeTest[Tree, TypeApply]

    /** Module object of `type TypeApply`  */
    val TypeApply: TypeApplyModule

    /** Methods of the module object `val TypeApply` */
    trait TypeApplyModule { this: TypeApply.type =>

      /** Create a function type application `<fun: Term>[<args: List[TypeTree]>]` */
      def apply(fun: Term, args: List[TypeTree]): TypeApply

      def copy(original: Tree)(fun: Term, args: List[TypeTree]): TypeApply

      /** Matches a function type application `<fun: Term>[<args: List[TypeTree]>]` */
      def unapply(x: TypeApply): (Term, List[TypeTree])
    }

    /** Makes extension methods on `TypeApply` available without any imports */
    given TypeApplyMethods: TypeApplyMethods

    /** Extension methods of `TypeApply` */
    trait TypeApplyMethods where
      extension (self: TypeApply)
        def fun: Term
        def args: List[TypeTree]
      end extension
    end TypeApplyMethods

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Super` */
    given SuperTypeTest: TypeTest[Tree, Super]

    /** Tree representing `super` in the source code */
    type Super <: Term

    /** Module object of `type Super`  */
    val Super: SuperModule

    /** Methods of the module object `val Super` */
    trait SuperModule { this: Super.type =>

      /** Creates a `<qualifier: Term>.super[<id: Option[Id]>` */
      def apply(qual: Term, mix: Option[String]): Super

      def copy(original: Tree)(qual: Term, mix: Option[String]): Super

      /** Matches a `<qualifier: Term>.super[<id: Option[Id]>` */
      def unapply(x: Super): (Term, Option[String])
    }

    /** Makes extension methods on `Super` available without any imports */
    given SuperMethods: SuperMethods

    /** Extension methods of `Super` */
    trait SuperMethods where
      extension (self: Super)
        def qualifier: Term
        def id: Option[String]
        def idPos: Position
      end extension
    end SuperMethods

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Typed` */
    given TypedTypeTest: TypeTest[Tree, Typed]

    /** Tree representing a type ascription `x: T` in the source code */
    type Typed <: Term

    /** Module object of `type Typed`  */
    val Typed: TypedModule

    /** Methods of the module object `val Typed` */
    trait TypedModule { this: Typed.type =>

      /** Create a type ascription `<x: Term>: <tpt: TypeTree>` */
      def apply(expr: Term, tpt: TypeTree): Typed

      def copy(original: Tree)(expr: Term, tpt: TypeTree): Typed

      /** Matches `<expr: Term>: <tpt: TypeTree>` */
      def unapply(x: Typed): (Term, TypeTree)
    }

    /** Makes extension methods on `Typed` available without any imports */
    given TypedMethods: TypedMethods

    /** Extension methods of `Typed` */
    trait TypedMethods where
      extension (self: Typed)
        def expr: Term
        def tpt: TypeTree
      end extension
    end TypedMethods

    /** Tree representing an assignment `x = y` in the source code */
    type Assign <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Assign` */
    given AssignTypeTest: TypeTest[Tree, Assign]

    /** Module object of `type Assign`  */
    val Assign: AssignModule

    /** Methods of the module object `val Assign` */
    trait AssignModule { this: Assign.type =>

      /** Create an assignment `<lhs: Term> = <rhs: Term>` */
      def apply(lhs: Term, rhs: Term): Assign

      def copy(original: Tree)(lhs: Term, rhs: Term): Assign

      /** Matches an assignment `<lhs: Term> = <rhs: Term>` */
      def unapply(x: Assign): (Term, Term)
    }

    /** Makes extension methods on `Assign` available without any imports */
    given AssignMethods: AssignMethods

    /** Extension methods of `Assign` */
    trait AssignMethods where
      extension (self: Assign)
        def lhs: Term
        def rhs: Term
      end extension
    end AssignMethods

    /** Tree representing a block `{ ... }` in the source code */
    type Block <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Block` */
    given BlockTypeTest: TypeTest[Tree, Block]

    /** Module object of `type Block`  */
    val Block: BlockModule

    /** Methods of the module object `val Block` */
    trait BlockModule { this: Block.type =>

      /** Creates a block `{ <statements: List[Statement]>; <expr: Term> }` */
      def apply(stats: List[Statement], expr: Term): Block

      def copy(original: Tree)(stats: List[Statement], expr: Term): Block

      /** Matches a block `{ <statements: List[Statement]>; <expr: Term> }` */
      def unapply(x: Block): (List[Statement], Term)
    }

    /** Makes extension methods on `Block` available without any imports */
    given BlockMethods: BlockMethods

    /** Extension methods of `Block` */
    trait BlockMethods where
      extension (self: Block)
        def statements: List[Statement]
        def expr: Term
      end extension
    end BlockMethods

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Closure` */
    given ClosureTypeTest: TypeTest[Tree, Closure]

    /** A lambda `(...) => ...` in the source code is represented as
     *  a local method and a closure:
     *
     *  {
     *    def m(...) = ...
     *    closure(m)
     *  }
     *
     */
    type Closure <: Term

    /** Module object of `type Closure`  */
    val Closure: ClosureModule

    /** Methods of the module object `val Closure` */
    trait ClosureModule { this: Closure.type =>

      def apply(meth: Term, tpe: Option[TypeRepr]): Closure

      def copy(original: Tree)(meth: Tree, tpe: Option[TypeRepr]): Closure

      def unapply(x: Closure): (Term, Option[TypeRepr])
    }

    /** Makes extension methods on `Closure` available without any imports */
    given ClosureMethods: ClosureMethods

    /** Extension methods of `Closure` */
    trait ClosureMethods where
      extension (self: Closure)
        def meth: Term
        def tpeOpt: Option[TypeRepr]
      end extension
    end ClosureMethods

    /** A lambda `(...) => ...` in the source code is represented as
     *  a local method and a closure:
     *
     *  {
     *    def m(...) = ...
     *    closure(m)
     *  }
     *
     *  @note Due to the encoding, in pattern matches the case for `Lambda`
     *        should come before the case for `Block` to avoid mishandling
     *        of `Lambda`.
     */
    val Lambda: LambdaModule

    /** Methods of the module object `val Lambda` */
    trait LambdaModule { this: Lambda.type =>
      /** Matches a lambda definition of the form
       *  ```
       *  Block((DefDef(_, _, params :: Nil, _, Some(body))) :: Nil, Closure(meth, _))
       *  ```
       *  Extracts the parameter definitions and body.
       *
       */
      def unapply(tree: Block): Option[(List[ValDef], Term)]

      /** Generates a lambda with the given method type.
       *  ```
       *  Block((DefDef(_, _, params :: Nil, _, Some(rhsFn(meth, paramRefs)))) :: Nil, Closure(meth, _))
       *  ```
       * @param owner: owner of the generated `meth` symbol
       * @param tpe: Type of the definition
       * @param rhsFn: Funtion that recieves the `meth` symbol and the a list of references to the `params`
       */
      def apply(owner: Symbol, tpe: MethodType, rhsFn: (Symbol, List[Tree]) => Tree): Block
    }

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `If` */
    given IfTypeTest: TypeTest[Tree, If]

    /** Tree representing an if/then/else `if (...) ... else ...` in the source code */
    type If <: Term

    /** Module object of `type If` */
    val If: IfModule

    /** Methods of the module object `val If` */
    trait IfModule { this: If.type =>

      /** Create an if/then/else `if (<cond: Term>) <thenp: Term> else <elsep: Term>` */
      def apply(cond: Term, thenp: Term, elsep: Term): If

      def copy(original: Tree)(cond: Term, thenp: Term, elsep: Term): If

      /** Matches an if/then/else `if (<cond: Term>) <thenp: Term> else <elsep: Term>` */
      def unapply(tree: If): (Term, Term, Term)
    }

    /** Makes extension methods on `If` available without any imports */
    given IfMethods: IfMethods

    /** Extension methods of `If` */
    trait IfMethods where
      extension (self: If)
        def cond: Term
        def thenp: Term
        def elsep: Term
        def isInline: Boolean
      end extension
    end IfMethods

    /** Tree representing a pattern match `x match  { ... }` in the source code */
    type Match <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Match` */
    given MatchTypeTest: TypeTest[Tree, Match]

    /** Module object of `type Match`  */
    val Match: MatchModule

    /** Methods of the module object `val Match` */
    trait MatchModule { this: Match.type =>

      /** Creates a pattern match `<scrutinee: Term> match { <cases: List[CaseDef]> }` */
      def apply(selector: Term, cases: List[CaseDef]): Match

      def copy(original: Tree)(selector: Term, cases: List[CaseDef]): Match

      /** Matches a pattern match `<scrutinee: Term> match { <cases: List[CaseDef]> }` */
      def unapply(x: Match): (Term, List[CaseDef])
    }

    /** Makes extension methods on `Match` available without any imports */
    given MatchMethods: MatchMethods

    /** Extension methods of `Match` */
    trait MatchMethods where
      extension (self: Match)
        def scrutinee: Term
        def cases: List[CaseDef]
        def isInline: Boolean
      end extension
    end MatchMethods

    /** Tree representing a summoning match `summonFrom { ... }` in the source code */
    type SummonFrom <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `SummonFrom` */
    given SummonFromTypeTest: TypeTest[Tree, SummonFrom]

    /** Module object of `type SummonFrom`  */
    val SummonFrom: SummonFromModule

    /** Methods of the module object `val SummonFrom` */
    trait SummonFromModule { this: SummonFrom.type =>

      /** Creates a pattern match `given match { <cases: List[CaseDef]> }` */
      def apply(cases: List[CaseDef]): SummonFrom

      def copy(original: Tree)(cases: List[CaseDef]): SummonFrom

      /** Matches a pattern match `given match { <cases: List[CaseDef]> }` */
      def unapply(x: SummonFrom): Some[List[CaseDef]]
    }

    /** Makes extension methods on `SummonFrom` available without any imports */
    given SummonFromMethods: SummonFromMethods

    /** Extension methods of `SummonFrom` */
    trait SummonFromMethods where
      extension (self: SummonFrom)
        def cases: List[CaseDef]
      end extension
    end SummonFromMethods

    /** Tree representing a try catch `try x catch { ... } finally { ... }` in the source code */
    type Try <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Try` */
    given TryTypeTest: TypeTest[Tree, Try]

    /** Module object of `type Try`  */
    val Try: TryModule

    /** Methods of the module object `val Try` */
    trait TryModule { this: Try.type =>

      /** Create a try/catch `try <body: Term> catch { <cases: List[CaseDef]> } finally <finalizer: Option[Term]>` */
      def apply(expr: Term, cases: List[CaseDef], finalizer: Option[Term]): Try

      def copy(original: Tree)(expr: Term, cases: List[CaseDef], finalizer: Option[Term]): Try

      /** Matches a try/catch `try <body: Term> catch { <cases: List[CaseDef]> } finally <finalizer: Option[Term]>` */
      def unapply(x: Try): (Term, List[CaseDef], Option[Term])
    }

    /** Makes extension methods on `Try` available without any imports */
    given TryMethods: TryMethods

    /** Extension methods of `Try` */
    trait TryMethods where
      extension (self: Try)
        def body: Term
        def cases: List[CaseDef]
        def finalizer: Option[Term]
      end extension
    end TryMethods

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Return` */
    given ReturnTypeTest: TypeTest[Tree, Return]

    /** Tree representing a `return` in the source code */
    type Return <: Term

    /** Module object of `type Return`  */
    val Return: ReturnModule

    /** Methods of the module object `val Return` */
    trait ReturnModule { this: Return.type =>

      /** Creates `return <expr: Term>` */
      def apply(expr: Term, from: Symbol): Return

      def copy(original: Tree)(expr: Term, from: Symbol): Return

      /** Matches `return <expr: Term>` and extracts the expression and symbol of the method */
      def unapply(x: Return): (Term, Symbol)
    }

    /** Makes extension methods on `Return` available without any imports */
    given ReturnMethods: ReturnMethods

    /** Extension methods of `Return` */
    trait ReturnMethods where
      extension (self: Return)
        def expr: Term
        def from: Symbol
      end extension
    end ReturnMethods

    /** Tree representing a variable argument list in the source code */
    type Repeated <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Repeated` */
    given RepeatedTypeTest: TypeTest[Tree, Repeated]

    /** Module object of `type Repeated`  */
    val Repeated: RepeatedModule

    /** Methods of the module object `val Repeated` */
    trait RepeatedModule { this: Repeated.type =>
      def apply(elems: List[Term], tpt: TypeTree): Repeated
      def copy(original: Tree)(elems: List[Term], tpt: TypeTree): Repeated
      def unapply(x: Repeated): (List[Term], TypeTree)
    }

    /** Makes extension methods on `Repeated` available without any imports */
    given RepeatedMethods: RepeatedMethods

    /** Extension methods of `Repeated` */
    trait RepeatedMethods where
      extension (self: Repeated)
        def elems: List[Term]
        def elemtpt: TypeTree
      end extension
    end RepeatedMethods

    /** Tree representing the scope of an inlined tree */
    type Inlined <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Inlined` */
    given InlinedTypeTest: TypeTest[Tree, Inlined]

    /** Module object of `type Inlined`  */
    val Inlined: InlinedModule

    /** Methods of the module object `val Inlined` */
    trait InlinedModule { this: Inlined.type =>
      def apply(call: Option[Tree /* Term | TypeTree */], bindings: List[Definition], expansion: Term): Inlined
      def copy(original: Tree)(call: Option[Tree /* Term | TypeTree */], bindings: List[Definition], expansion: Term): Inlined
      def unapply(x: Inlined): (Option[Tree /* Term | TypeTree */], List[Definition], Term)
    }

    /** Makes extension methods on `Inlined` available without any imports */
    given InlinedMethods: InlinedMethods

    /** Extension methods of `Inlined` */
    trait InlinedMethods where
      extension (self: Inlined)
        def call: Option[Tree /* Term | TypeTree */]
        def bindings: List[Definition]
        def body: Term
      end extension
    end InlinedMethods

    /** Tree representing a selection of definition with a given name on a given prefix and number of nested scopes of inlined trees */
    type SelectOuter <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `SelectOuter` */
    given SelectOuterTypeTest: TypeTest[Tree, SelectOuter]

    /** Module object of `type SelectOuter`  */
    val SelectOuter: SelectOuterModule

    /** Methods of the module object `val SelectOuter` */
    trait SelectOuterModule { this: SelectOuter.type =>
      def apply(qualifier: Term, name: String, levels: Int): SelectOuter
      def copy(original: Tree)(qualifier: Term, name: String, levels: Int): SelectOuter
      def unapply(x: SelectOuter): (Term, String, Int)
    }

    /** Makes extension methods on `SelectOuter` available without any imports */
    given SelectOuterMethods: SelectOuterMethods

    /** Extension methods of `SelectOuter` */
    trait SelectOuterMethods where
      extension (self: SelectOuter)
        def qualifier: Term
        def name: String
        def level: Int
      end extension
    end SelectOuterMethods

    /** Tree representing a while loop */
    type While <: Term

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `While` */
    given WhileTypeTest: TypeTest[Tree, While]

    /** Module object of `type While`  */
    val While: WhileModule

    /** Methods of the module object `val While` */
    trait WhileModule { this: While.type =>

      /** Creates a while loop `while (<cond>) <body>` and returns (<cond>, <body>) */
      def apply(cond: Term, body: Term): While

      def copy(original: Tree)(cond: Term, body: Term): While

      /** Extractor for while loops. Matches `while (<cond>) <body>` and returns (<cond>, <body>) */
      def unapply(x: While): (Term, Term)
    }

    /** Makes extension methods on `While` available without any imports */
    given WhileMethods: WhileMethods

    /** Extension methods of `While` */
    trait WhileMethods where
      extension (self: While)
        def cond: Term
        def body: Term
      end extension
    end WhileMethods

    // ----- TypeTrees ------------------------------------------------

    /** Type tree representing a type written in the source */
    type TypeTree <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeTree` */
    given TypeTreeTypeTest: TypeTest[Tree, TypeTree]

    /** Module object of `type TypeTree`  */
    val TypeTree: TypeTreeModule

    /** Methods of the module object `val TypeTree` */
    trait TypeTreeModule { this: TypeTree.type =>
      /** Returns the tree of type or kind (TypeTree) of T */
      def of[T <: AnyKind](using Type[T]): TypeTree
    }

    /** Makes extension methods on `TypeTree` available without any imports */
    given TypeTreeMethods: TypeTreeMethods

    /** Extension methods of `TypeTree` */
    trait TypeTreeMethods where
      extension (self: TypeTree)
        /** TypeRepr of this type tree */
        def tpe: TypeRepr
      end extension
    end TypeTreeMethods

    /** Type tree representing an inferred type */
    type Inferred <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Inferred` */
    given InferredTypeTest: TypeTest[Tree, Inferred]

    /** Module object of `type Inferred`  */
    val Inferred: InferredModule

    /** Methods of the module object `val Inferred` */
    trait InferredModule { this: Inferred.type =>
      def apply(tpe: TypeRepr): Inferred
      /** Matches a TypeTree containing an inferred type */
      def unapply(x: Inferred): true
    }

    /** Type tree representing a reference to definition with a given name */
    type TypeIdent <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeIdent` */
    given TypeIdentTypeTest: TypeTest[Tree, TypeIdent]

    /** Module object of `type TypeIdent`  */
    val TypeIdent: TypeIdentModule

    /** Methods of the module object `val TypeIdent` */
    trait TypeIdentModule { this: TypeIdent.type =>
      def apply(sym: Symbol): TypeTree
      def copy(original: Tree)(name: String): TypeIdent
      def unapply(x: TypeIdent): Some[String]
    }

    /** Makes extension methods on `TypeIdent` available without any imports */
    given TypeIdentMethods: TypeIdentMethods

    /** Extension methods of `TypeIdent` */
    trait TypeIdentMethods where
      extension (self: TypeIdent)
        def name: String
      end extension
    end TypeIdentMethods

    /** Type tree representing a selection of definition with a given name on a given term prefix */
    type TypeSelect <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeSelect` */
    given TypeSelectTypeTest: TypeTest[Tree, TypeSelect]

    /** Module object of `type TypeSelect`  */
    val TypeSelect: TypeSelectModule

    /** Methods of the module object `val TypeSelect` */
    trait TypeSelectModule { this: TypeSelect.type =>
      def apply(qualifier: Term, name: String): TypeSelect
      def copy(original: Tree)(qualifier: Term, name: String): TypeSelect
      def unapply(x: TypeSelect): (Term, String)
    }

    /** Makes extension methods on `TypeSelect` available without any imports */
    given TypeSelectMethods: TypeSelectMethods

    /** Extension methods of `TypeSelect` */
    trait TypeSelectMethods where
      extension (self: TypeSelect)
        def qualifier: Term
        def name: String
      end extension
    end TypeSelectMethods

    /** Type tree representing a selection of definition with a given name on a given type prefix */
    type TypeProjection <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeProjection` */
    given TypeProjectionTypeTest: TypeTest[Tree, TypeProjection]

    /** Module object of `type TypeProjection`  */
    val TypeProjection: TypeProjectionModule

    /** Methods of the module object `val TypeProjection` */
    trait TypeProjectionModule { this: TypeProjection.type =>
      // TODO def apply(qualifier: TypeTree, name: String): Project
      def copy(original: Tree)(qualifier: TypeTree, name: String): TypeProjection
      def unapply(x: TypeProjection): (TypeTree, String)
    }

    /** Makes extension methods on `TypeProjection` available without any imports */
    given TypeProjectionMethods: TypeProjectionMethods

    /** Extension methods of `TypeProjection` */
    trait TypeProjectionMethods where
      extension (self: TypeProjection)
        def qualifier: TypeTree
        def name: String
      end extension
    end TypeProjectionMethods

    /** Type tree representing a singleton type */
    type Singleton <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Singleton` */
    given SingletonTypeTest: TypeTest[Tree, Singleton]

    /** Module object of `type Singleton`  */
    val Singleton: SingletonModule

    /** Methods of the module object `val Singleton` */
    trait SingletonModule { this: Singleton.type =>
      def apply(ref: Term): Singleton
      def copy(original: Tree)(ref: Term): Singleton
      def unapply(x: Singleton): Some[Term]
    }

    /** Makes extension methods on `Singleton` available without any imports */
    given SingletonMethods: SingletonMethods

    /** Extension methods of `Singleton` */
    trait SingletonMethods where
      extension (self: Singleton)
        def ref: Term
      end extension
    end SingletonMethods

    /** Type tree representing a type refinement */
    type Refined <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Refined` */
    given RefinedTypeTest: TypeTest[Tree, Refined]

    /** Module object of `type Refined`  */
    val Refined: RefinedModule

    /** Methods of the module object `val Refined` */
    trait RefinedModule { this: Refined.type =>
      // TODO def apply(tpt: TypeTree, refinements: List[Definition]): Refined
      def copy(original: Tree)(tpt: TypeTree, refinements: List[Definition]): Refined
      def unapply(x: Refined): (TypeTree, List[Definition])
    }

    /** Makes extension methods on `Refined` available without any imports */
    given RefinedMethods: RefinedMethods

    /** Extension methods of `Refined` */
    trait RefinedMethods where
      extension (self: Refined)
        def tpt: TypeTree
        def refinements: List[Definition]
      end extension
    end RefinedMethods

    /** Type tree representing a type application */
    type Applied <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Applied` */
    given AppliedTypeTest: TypeTest[Tree, Applied]

    /** Module object of `type Applied`  */
    val Applied: AppliedModule

    /** Methods of the module object `val Applied` */
    trait AppliedModule { this: Applied.type =>
      def apply(tpt: TypeTree, args: List[Tree /*TypeTree | TypeBoundsTree*/]): Applied
      def copy(original: Tree)(tpt: TypeTree, args: List[Tree /*TypeTree | TypeBoundsTree*/]): Applied
      def unapply(x: Applied): (TypeTree, List[Tree /*TypeTree | TypeBoundsTree*/])
    }

    /** Makes extension methods on `Applied` available without any imports */
    given AppliedMethods: AppliedMethods

    /** Extension methods of `Applied` */
    trait AppliedMethods where
      extension (self: Applied)
        def tpt: TypeTree
        def args: List[Tree /*TypeTree | TypeBoundsTree*/]
      end extension
    end AppliedMethods

    /** Type tree representing an annotated type */
    type Annotated <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Annotated` */
    given AnnotatedTypeTest: TypeTest[Tree, Annotated]

    /** Module object of `type Annotated`  */
    val Annotated: AnnotatedModule

    /** Methods of the module object `val Annotated` */
    trait AnnotatedModule { this: Annotated.type =>
      def apply(arg: TypeTree, annotation: Term): Annotated
      def copy(original: Tree)(arg: TypeTree, annotation: Term): Annotated
      def unapply(x: Annotated): (TypeTree, Term)
    }

    /** Makes extension methods on `Annotated` available without any imports */
    given AnnotatedMethods: AnnotatedMethods

    /** Extension methods of `Annotated` */
    trait AnnotatedMethods where
      extension (self: Annotated)
        def arg: TypeTree
        def annotation: Term
      end extension
    end AnnotatedMethods

    /** Type tree representing a type match */
    type MatchTypeTree <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `MatchTypeTree` */
    given MatchTypeTreeTypeTest: TypeTest[Tree, MatchTypeTree]

    /** Module object of `type MatchTypeTree`  */
    val MatchTypeTree: MatchTypeTreeModule

    /** Methods of the module object `val MatchTypeTree` */
    trait MatchTypeTreeModule { this: MatchTypeTree.type =>
      def apply(bound: Option[TypeTree], selector: TypeTree, cases: List[TypeCaseDef]): MatchTypeTree
      def copy(original: Tree)(bound: Option[TypeTree], selector: TypeTree, cases: List[TypeCaseDef]): MatchTypeTree
      def unapply(x: MatchTypeTree): (Option[TypeTree], TypeTree, List[TypeCaseDef])
    }

    /** Makes extension methods on `MatchTypeTree` available without any imports */
    given MatchTypeTreeMethods: MatchTypeTreeMethods

    /** Extension methods of `MatchTypeTree` */
    trait MatchTypeTreeMethods where
      extension (self: MatchTypeTree)
        def bound: Option[TypeTree]
        def selector: TypeTree
        def cases: List[TypeCaseDef]
      end extension
    end MatchTypeTreeMethods

    /** Type tree representing a by name parameter */
    type ByName <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `ByName` */
    given ByNameTypeTest: TypeTest[Tree, ByName]

    /** Module object of `type ByName`  */
    val ByName: ByNameModule

    /** Methods of the module object `val ByName` */
    trait ByNameModule { this: ByName.type =>
      def apply(result: TypeTree): ByName
      def copy(original: Tree)(result: TypeTree): ByName
      def unapply(x: ByName): Some[TypeTree]
    }

    /** Makes extension methods on `ByName` available without any imports */
    given ByNameMethods: ByNameMethods

    /** Extension methods of `ByName` */
    trait ByNameMethods where
      extension (self: ByName)
        def result: TypeTree
      end extension
    end ByNameMethods

    /** Type tree representing a lambda abstraction type */
    type LambdaTypeTree <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `LambdaTypeTree` */
    given LambdaTypeTreeTypeTest: TypeTest[Tree, LambdaTypeTree]

    /** Module object of `type LambdaTypeTree`  */
    val LambdaTypeTree: LambdaTypeTreeModule

    /** Methods of the module object `val LambdaTypeTree` */
    trait LambdaTypeTreeModule { this: LambdaTypeTree.type =>
      def apply(tparams: List[TypeDef], body: Tree /*TypeTree | TypeBoundsTree*/): LambdaTypeTree
      def copy(original: Tree)(tparams: List[TypeDef], body: Tree /*TypeTree | TypeBoundsTree*/): LambdaTypeTree
      def unapply(tree: LambdaTypeTree): (List[TypeDef], Tree /*TypeTree | TypeBoundsTree*/)
    }

    /** Makes extension methods on `LambdaTypeTree` available without any imports */
    given LambdaTypeTreeMethods: LambdaTypeTreeMethods

    /** Extension methods of `LambdaTypeTree` */
    trait LambdaTypeTreeMethods where
      extension (self: LambdaTypeTree)
        def tparams: List[TypeDef]
        def body: Tree /*TypeTree | TypeBoundsTree*/
      end extension
    end LambdaTypeTreeMethods

    /** Type tree representing a type binding */
    type TypeBind <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeBind` */
    given TypeBindTypeTest: TypeTest[Tree, TypeBind]

    /** Module object of `type TypeBind`  */
    val TypeBind: TypeBindModule

    /** Methods of the module object `val TypeBind` */
    trait TypeBindModule { this: TypeBind.type =>
      // TODO def apply(name: String, tree: Tree): TypeBind
      def copy(original: Tree)(name: String, tpt: Tree /*TypeTree | TypeBoundsTree*/): TypeBind
      def unapply(x: TypeBind): (String, Tree /*TypeTree | TypeBoundsTree*/)
    }

    /** Makes extension methods on `TypeBind` available without any imports */
    given TypeBindMethods: TypeBindMethods

    /** Extension methods of `TypeBind` */
    trait TypeBindMethods where
      extension (self: TypeBind)
        def name: String
        def body: Tree /*TypeTree | TypeBoundsTree*/
      end extension
    end TypeBindMethods

    /** Type tree within a block with aliases `{ type U1 = ... ; T[U1, U2] }` */
    type TypeBlock <: TypeTree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeBlock` */
    given TypeBlockTypeTest: TypeTest[Tree, TypeBlock]

    /** Module object of `type TypeBlock`  */
    val TypeBlock: TypeBlockModule

    /** Methods of the module object `val TypeBlock` */
    trait TypeBlockModule { this: TypeBlock.type =>
      def apply(aliases: List[TypeDef], tpt: TypeTree): TypeBlock
      def copy(original: Tree)(aliases: List[TypeDef], tpt: TypeTree): TypeBlock
      def unapply(x: TypeBlock): (List[TypeDef], TypeTree)
    }

    /** Makes extension methods on `TypeBlock` available without any imports */
    given TypeBlockMethods: TypeBlockMethods

    /** Extension methods of `TypeBlock` */
    trait TypeBlockMethods where
      extension (self: TypeBlock)
        def aliases: List[TypeDef]
        def tpt: TypeTree
      end extension
    end TypeBlockMethods

    // ----- TypeBoundsTrees ------------------------------------------------

    /** Type tree representing a type bound written in the source */
    type TypeBoundsTree <: Tree /*TypeTree | TypeBoundsTree*/

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeBoundsTree` */
    given TypeBoundsTreeTypeTest: TypeTest[Tree, TypeBoundsTree]

    /** Module object of `type TypeBoundsTree`  */
    val TypeBoundsTree: TypeBoundsTreeModule

    /** Methods of the module object `val TypeBoundsTree` */
    trait TypeBoundsTreeModule { this: TypeBoundsTree.type =>
      def apply(low: TypeTree, hi: TypeTree): TypeBoundsTree
      def copy(original: Tree)(low: TypeTree, hi: TypeTree): TypeBoundsTree
      def unapply(x: TypeBoundsTree): (TypeTree, TypeTree)
    }

    /** Makes extension methods on `TypeBoundsTree` available without any imports */
    given TypeBoundsTreeMethods: TypeBoundsTreeMethods

    /** Extension methods of `TypeBoundsTree` */
    trait TypeBoundsTreeMethods where
      extension (self: TypeBoundsTree)
        def tpe: TypeBounds
        def low: TypeTree
        def hi: TypeTree
      end extension
    end TypeBoundsTreeMethods

    /** Type tree representing wildcard type bounds written in the source.
    *  The wildcard type `_` (for example in in `List[_]`) will be a type tree that
    *  represents a type but has `TypeBound`a inside.
    */
    type WildcardTypeTree  <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `WildcardTypeTree` */
    given WildcardTypeTreeTypeTest: TypeTest[Tree, WildcardTypeTree]

    /** Module object of `type WildcardTypeTree`  */
    val WildcardTypeTree: WildcardTypeTreeModule

    /** Methods of the module object `val WildcardTypeTree` */
    trait WildcardTypeTreeModule { this: WildcardTypeTree.type =>
      def apply(tpe: TypeRepr): WildcardTypeTree
      /** Matches a TypeBoundsTree containing wildcard type bounds */
      def unapply(x: WildcardTypeTree): true
    }

    /** Makes extension methods on `WildcardTypeTree` available without any imports */
    given WildcardTypeTreeMethods: WildcardTypeTreeMethods

    /** Extension methods of `WildcardTypeTree` */
    trait WildcardTypeTreeMethods where
      extension (self: WildcardTypeTree)
        def tpe: TypeRepr
      end extension
    end WildcardTypeTreeMethods

    // ----- CaseDefs ------------------------------------------------

    /** Branch of a pattern match or catch clause */
    type CaseDef <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `CaseDef` */
    given CaseDefTypeTest: TypeTest[Tree, CaseDef]

    /** Module object of `type CaseDef`  */
    val CaseDef: CaseDefModule

    /** Methods of the module object `val CaseDef` */
    trait CaseDefModule { this: CaseDef.type =>
      def apply(pattern: Tree, guard: Option[Term], rhs: Term): CaseDef
      def copy(original: Tree)(pattern: Tree, guard: Option[Term], rhs: Term): CaseDef
      def unapply(x: CaseDef): (Tree, Option[Term], Term)
    }

    /** Makes extension methods on `CaseDef` available without any imports */
    given CaseDefMethods: CaseDefMethods

    /** Extension methods of `CaseDef` */
    trait CaseDefMethods where
      extension (self: CaseDef)
        def pattern: Tree
        def guard: Option[Term]
        def rhs: Term
      end extension
    end CaseDefMethods

    /** Branch of a type pattern match */
    type TypeCaseDef <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `TypeCaseDef` */
    given TypeCaseDefTypeTest: TypeTest[Tree, TypeCaseDef]

    /** Module object of `type TypeCaseDef`  */
    val TypeCaseDef: TypeCaseDefModule

    /** Methods of the module object `val TypeCaseDef` */
    trait TypeCaseDefModule { this: TypeCaseDef.type =>
      def apply(pattern: TypeTree, rhs: TypeTree): TypeCaseDef
      def copy(original: Tree)(pattern: TypeTree, rhs: TypeTree): TypeCaseDef
      def unapply(tree: TypeCaseDef): (TypeTree, TypeTree)
    }

    /** Makes extension methods on `TypeCaseDef` available without any imports */
    given TypeCaseDefMethods: TypeCaseDefMethods

    /** Extension methods of `TypeCaseDef` */
    trait TypeCaseDefMethods where
      extension (self: TypeCaseDef)
        def pattern: TypeTree
        def rhs: TypeTree
      end extension
    end TypeCaseDefMethods

    // ----- Patterns ------------------------------------------------

    /** Pattern representing a `_ @ _` binding. */
    type Bind <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is a `Bind` */
    given BindTypeTest: TypeTest[Tree, Bind]

    /** Module object of `type Bind`  */
    val Bind: BindModule

    /** Methods of the module object `val Bind` */
    trait BindModule { this: Bind.type =>
      def apply(sym: Symbol, pattern: Tree): Bind
      def copy(original: Tree)(name: String, pattern: Tree): Bind
      def unapply(pattern: Bind): (String, Tree)
    }

    /** Makes extension methods on `Bind` available without any imports */
    given BindMethods: BindMethods

    /** Extension methods of `Bind` */
    trait BindMethods where
      extension (self: Bind)
        def name: String
        def pattern: Tree
      end extension
    end BindMethods

    /** Pattern representing a `Xyz(...)` unapply. */
    type Unapply <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Unapply` */
    given UnapplyTypeTest: TypeTest[Tree, Unapply]

    /** Module object of `type Unapply`  */
    val Unapply: UnapplyModule

    /** Methods of the module object `val Unapply` */
    trait UnapplyModule { this: Unapply.type =>
      // TODO def apply(fun: Term, implicits: List[Term], patterns: List[Tree]): Unapply
      def copy(original: Tree)(fun: Term, implicits: List[Term], patterns: List[Tree]): Unapply
      def unapply(x: Unapply): (Term, List[Term], List[Tree])
    }

    /** Makes extension methods on `Unapply` available without any imports */
    given UnapplyMethods: UnapplyMethods

    /** Extension methods of `Unapply` */
    trait UnapplyMethods where
      extension (self: Unapply)
        def fun: Term
        def implicits: List[Term]
        def patterns: List[Tree]
      end extension
    end UnapplyMethods

    /** Pattern representing `X | Y | ...` alternatives. */
    type Alternatives <: Tree

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Tree` is an `Alternatives` */
    given AlternativesTypeTest: TypeTest[Tree, Alternatives]

    /** Module object of `type Alternatives`  */
    val Alternatives: AlternativesModule

    /** Methods of the module object `val Alternatives` */
    trait AlternativesModule { this: Alternatives.type =>
      def apply(patterns: List[Tree]): Alternatives
      def copy(original: Tree)(patterns: List[Tree]): Alternatives
      def unapply(x: Alternatives): Some[List[Tree]]
    }

    /** Makes extension methods on `Alternatives` available without any imports */
    given AlternativesMethods: AlternativesMethods

    /** Extension methods of `Alternatives` */
    trait AlternativesMethods where
      extension (self: Alternatives)
        def patterns: List[Tree]
      end extension
    end AlternativesMethods

    //////////////////////
    //    SELECTORS     //
    /////////////////////

    /** Import/Export selectors:
    *   * SimpleSelector: `.bar` in `import foo.bar`
    *   * RenameSelector: `.{bar => baz}` in `export foo.{bar => baz}`
    *   * OmitSelector: `.{bar => _}` in `import foo.{bar => _}`
    *   * GivenSelector: `.given`/`.{given T}` in `export foo.given`/`import foo.{given T}`
    */
    type Selector <: AnyRef

    /** Module object of `type Selector`  */
    val Selector: SelectorModule

    /** Methods of the module object `val Selector` */
    trait SelectorModule { this: Selector.type => }

    /** Simple import/export selector: `.bar` in `import foo.bar` */
    type SimpleSelector <: Selector

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Selector` is a `SimpleSelector` */
    given SimpleSelectorTypeTest: TypeTest[Selector, SimpleSelector]

    /** Module object of `type SimpleSelector`  */
    val SimpleSelector: SimpleSelectorModule

    /** Methods of the module object `val SimpleSelector` */
    trait SimpleSelectorModule { this: SimpleSelector.type =>
      def unapply(x: SimpleSelector): Some[String]
    }

    /** Makes extension methods on `SimpleSelector` available without any imports */
    given SimpleSelectorMethods: SimpleSelectorMethods

    /** Extension methods of `SimpleSelector` */
    trait SimpleSelectorMethods where
      extension (self: SimpleSelector)
        def name: String
        def namePos: Position
      end extension
    end SimpleSelectorMethods

    /** Rename import/export selector: `.{bar => baz}` in `import foo.{bar => baz}` */
    type RenameSelector <: Selector

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Selector` is a `RenameSelector` */
    given RenameSelectorTypeTest: TypeTest[Selector, RenameSelector]

    /** Module object of `type RenameSelector`  */
    val RenameSelector: RenameSelectorModule

    /** Methods of the module object `val RenameSelector` */
    trait RenameSelectorModule { this: RenameSelector.type =>
      def unapply(x: RenameSelector): (String, String)
    }

    /** Makes extension methods on `RenameSelector` available without any imports */
    given RenameSelectorMethods: RenameSelectorMethods

    /** Extension methods of `RenameSelector` */
    trait RenameSelectorMethods where
      extension (self: RenameSelector)
        def fromName: String
        def fromPos: Position
        def toName: String
        def toPos: Position
      end extension
    end RenameSelectorMethods

    /** Omit import/export selector: `.{bar => _}` in `import foo.{bar => _}` */
    type OmitSelector <: Selector

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Selector` is an `OmitSelector` */
    given OmitSelectorTypeTest: TypeTest[Selector, OmitSelector]

    /** Module object of `type OmitSelector`  */
    val OmitSelector: OmitSelectorModule

    /** Methods of the module object `val OmitSelector` */
    trait OmitSelectorModule { this: OmitSelector.type =>
      def unapply(x: OmitSelector): Some[String]
    }

    /** Makes extension methods on `OmitSelector` available without any imports */
    given OmitSelectorMethods: OmitSelectorMethods

    /** Extension methods of `OmitSelector` */
    trait OmitSelectorMethods where
      extension (self: OmitSelector)
        def name: String
        def namePos: Position
    end OmitSelectorMethods

    /** given import/export selector: `.given`/`.{given T}` in `import foo.given`/`export foo.{given T}` */
    type GivenSelector <: Selector

    /** `TypeTest` that allows testing at runtime in a pattern match if an `Selector` is a `GivenSelector` */
    given GivenSelectorTypeTest: TypeTest[Selector, GivenSelector]

    /** Module object of `type GivenSelector`  */
    val GivenSelector: GivenSelectorModule

    /** Methods of the module object `val GivenSelector` */
    trait GivenSelectorModule { this: GivenSelector.type =>
      def unapply(x: GivenSelector): Some[Option[TypeTree]]
    }

    /** Makes extension methods on `GivenSelector` available without any imports */
    given GivenSelectorMethods: GivenSelectorMethods

    /** Extension methods of `GivenSelector` */
    trait GivenSelectorMethods where
      extension (self: GivenSelector)
        def bound: Option[TypeTree]
    end GivenSelectorMethods

    ///////////////
    //   TYPES   //
    ///////////////

    // ----- Types ----------------------------------------------------

    /** A type, type constructors, type bounds or NoPrefix */
    type TypeRepr

    /** Module object of `type TypeRepr`  */
    val TypeRepr: TypeReprModule

    /** Methods of the module object `val TypeRepr` */
    trait TypeReprModule { this: TypeRepr.type =>
      /** Returns the type or kind (TypeRepr) of T */
      def of[T <: AnyKind](using Type[T]): TypeRepr

      /** Returns the type constructor of the runtime (erased) class */
      def typeConstructorOf(clazz: Class[?]): TypeRepr
    }

    /** Makes extension methods on `TypeRepr` available without any imports */
    given TypeReprMethods: TypeReprMethods

    /** Extension methods of `TypeRepr` */
    trait TypeReprMethods {
      extension (self: TypeRepr)

        /** Shows the type as a String */
        def show(using Printer[TypeRepr]): String

        /** Convert this `TypeRepr` to an `Type[?]`
        *
        *  Usage:
        *  ```
        *  typeRepr.asType match
        *    case '[$t] =>
        *      '{ val x: t = ... }
        *  ```
        */
        def asType: Type[?]

        /** Is `self` type the same as `that` type?
        *  This is the case iff `self <:< that` and `that <:< self`.
        */
        def =:=(that: TypeRepr): Boolean

        /** Is this type a subtype of that type? */
        def <:<(that: TypeRepr): Boolean

        /** Widen from singleton type to its underlying non-singleton
        *  base type by applying one or more `underlying` dereferences,
        *  Also go from => T to T.
        *  Identity for all other types. Example:
        *
        *  class Outer { class C ; val x: C }
        *  def o: Outer
        *  <o.x.type>.widen = o.C
        */
        def widen: TypeRepr

        /** Widen from TermRef to its underlying non-termref
          *  base type, while also skipping `=>T` types.
          */
        def widenTermRefExpr: TypeRepr

        /** Follow aliases and dereferences LazyRefs, annotated types and instantiated
          *  TypeVars until type is no longer alias type, annotated type, LazyRef,
          *  or instantiated type variable.
          */
        def dealias: TypeRepr

        /** A simplified version of this type which is equivalent wrt =:= to this type.
        *  Reduces typerefs, applied match types, and and or types.
        */
        def simplified: TypeRepr

        def classSymbol: Option[Symbol]
        def typeSymbol: Symbol
        def termSymbol: Symbol
        def isSingleton: Boolean
        def memberType(member: Symbol): TypeRepr

        /** The base classes of this type with the class itself as first element. */
        def baseClasses: List[Symbol]

        /** The least type instance of given class which is a super-type
        *  of this type.  Example:
        *  {{{
        *    class D[T]
        *    class C extends p.D[Int]
        *    ThisType(C).baseType(D) = p.D[Int]
        * }}}
        */
        def baseType(cls: Symbol): TypeRepr

        /** Is this type an instance of a non-bottom subclass of the given class `cls`? */
        def derivesFrom(cls: Symbol): Boolean

        /** Is this type a function type?
        *
        *  @return true if the dealised type of `self` without refinement is `FunctionN[T1, T2, ..., Tn]`
        *
        *  @note The function
        *
        *     - returns true for `given Int => Int` and `erased Int => Int`
        *     - returns false for `List[Int]`, despite that `List[Int] <:< Int => Int`.
        */
        def isFunctionType: Boolean

        /** Is this type an context function type?
        *
        *  @see `isFunctionType`
        */
        def isContextFunctionType: Boolean

        /** Is this type an erased function type?
        *
        *  @see `isFunctionType`
        */
        def isErasedFunctionType: Boolean

        /** Is this type a dependent function type?
        *
        *  @see `isFunctionType`
        */
        def isDependentFunctionType: Boolean

        /** The type <this . sym>, reduced if possible */
        def select(sym: Symbol): TypeRepr

        /** The current type applied to given type arguments: `this[targ]` */
        def appliedTo(targ: TypeRepr): TypeRepr

        /** The current type applied to given type arguments: `this[targ0, ..., targN]` */
        def appliedTo(targs: List[TypeRepr]): TypeRepr

      end extension
    }

    /** A singleton type representing a known constant value */
    type ConstantType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `ConstantType` */
    given ConstantTypeTypeTest: TypeTest[TypeRepr, ConstantType]

    /** Module object of `type ConstantType`  */
    val ConstantType: ConstantTypeModule

    /** Methods of the module object `val Type` */
    trait ConstantTypeModule { this: ConstantType.type =>
      def apply(x : Constant): ConstantType
      def unapply(x: ConstantType): Some[Constant]
    }

    /** Makes extension methods on `ConstantType` available without any imports */
    given ConstantTypeMethods: ConstantTypeMethods

    /** Extension methods of `ConstantType` */
    trait ConstantTypeMethods where
      extension (self: ConstantType)
        def constant: Constant
      end extension
    end ConstantTypeMethods

    /** Type of a reference to a type or term symbol */
    type NamedType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `NamedType` */
    given NamedTypeTypeTest: TypeTest[TypeRepr, NamedType]

    /** Makes extension methods on `NamedType` available without any imports */
    given NamedTypeMethods: NamedTypeMethods

    /** Extension methods of `NamedType` */
    trait NamedTypeMethods where
      extension (self: NamedType)
        def qualifier: TypeRepr
        def name: String
      end extension
    end NamedTypeMethods

    /** Type of a reference to a term symbol */
    type TermRef <: NamedType

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `TermRef` */
    given TermRefTypeTest: TypeTest[TypeRepr, TermRef]

    /** Module object of `type TermRef`  */
    val TermRef: TermRefModule

    /** Methods of the module object `val TermRef` */
    trait TermRefModule { this: TermRef.type =>
      def apply(qual: TypeRepr, name: String): TermRef
      def unapply(x: TermRef): (TypeRepr, String)
    }

    /** Type of a reference to a type symbol */
    type TypeRef <: NamedType

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `TypeRef` */
    given TypeRefTypeTest: TypeTest[TypeRepr, TypeRef]

    /** Module object of `type TypeRef`  */
    val TypeRef: TypeRefModule

    /** Methods of the module object `val TypeRef` */
    trait TypeRefModule { this: TypeRef.type =>
      def unapply(x: TypeRef): (TypeRepr, String)
    }

    /** Makes extension methods on `TypeRef` available without any imports */
    given TypeRefMethods: TypeRefMethods

    /** Extension methods of `TypeRef` */
    trait TypeRefMethods where
      extension (self: TypeRef)
        def isOpaqueAlias: Boolean
        def translucentSuperType: TypeRepr
      end extension
    end TypeRefMethods

    /** Type of a `super` reference */
    type SuperType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `SuperType` */
    given SuperTypeTypeTest: TypeTest[TypeRepr, SuperType]

    /** Module object of `type SuperType`  */
    val SuperType: SuperTypeModule

    /** Methods of the module object `val SuperType` */
    trait SuperTypeModule { this: SuperType.type =>
      def apply(thistpe: TypeRepr, supertpe: TypeRepr): SuperType
      def unapply(x: SuperType): (TypeRepr, TypeRepr)
    }

    /** Makes extension methods on `SuperType` available without any imports */
    given SuperTypeMethods: SuperTypeMethods

    /** Extension methods of `SuperType` */
    trait SuperTypeMethods { this: SuperTypeMethods =>
      extension (self: SuperType)
        def thistpe: TypeRepr
        def supertpe: TypeRepr
      end extension
    }

    /** A type with a type refinement `T { type U }` */
    type Refinement <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `Refinement` */
    given RefinementTypeTest: TypeTest[TypeRepr, Refinement]

    /** Module object of `type Refinement`  */
    val Refinement: RefinementModule

    /** Methods of the module object `val Refinement` */
    trait RefinementModule { this: Refinement.type =>
      def apply(parent: TypeRepr, name: String, info: TypeRepr): Refinement
      def unapply(x: Refinement): (TypeRepr, String, TypeRepr)
    }

    /** Makes extension methods on `Refinement` available without any imports */
    given RefinementMethods: RefinementMethods

    /** Extension methods of `Refinement` */
    trait RefinementMethods where
      extension (self: Refinement)
        def parent: TypeRepr
        def name: String
        def info: TypeRepr
      end extension
    end RefinementMethods

    /** A higher kinded type applied to some types `T[U]` */
    type AppliedType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is an `AppliedType` */
    given AppliedTypeTypeTest: TypeTest[TypeRepr, AppliedType]

    /** Module object of `type AppliedType`  */
    val AppliedType: AppliedTypeModule

    /** Methods of the module object `val AppliedType` */
    trait AppliedTypeModule { this: AppliedType.type =>
      def unapply(x: AppliedType): (TypeRepr, List[TypeRepr])
    }

    /** Makes extension methods on `AppliedType` available without any imports */
    given AppliedTypeMethods: AppliedTypeMethods

    /** Extension methods of `AppliedType` */
    trait AppliedTypeMethods where
      extension (self: AppliedType)
        def tycon: TypeRepr
        def args: List[TypeRepr]
      end extension
    end AppliedTypeMethods

    /** A type with an anottation `T @foo` */
    type AnnotatedType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is an `AnnotatedType` */
    given AnnotatedTypeTypeTest: TypeTest[TypeRepr, AnnotatedType]

    /** Module object of `type AnnotatedType`  */
    val AnnotatedType: AnnotatedTypeModule

    /** Methods of the module object `val AnnotatedType` */
    trait AnnotatedTypeModule { this: AnnotatedType.type =>
      def apply(underlying: TypeRepr, annot: Term): AnnotatedType
      def unapply(x: AnnotatedType): (TypeRepr, Term)
    }

    /** Makes extension methods on `AnnotatedType` available without any imports */
    given AnnotatedTypeMethods: AnnotatedTypeMethods

    /** Extension methods of `AnnotatedType` */
    trait AnnotatedTypeMethods where
      extension (self: AnnotatedType)
        def underlying: TypeRepr
        def annotation: Term
      end extension
    end AnnotatedTypeMethods


    /** Intersection type `T & U` or an union type `T | U` */
    type AndOrType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is an `AndOrType` */
    given AndOrTypeTypeTest: TypeTest[TypeRepr, AndOrType]

    /** Makes extension methods on `AndOrType` available without any imports */
    given AndOrTypeMethods: AndOrTypeMethods

    /** Extension methods of `AndOrType` */
    trait AndOrTypeMethods where
      extension (self: AndOrType)
        def left: TypeRepr
        def right: TypeRepr
      end extension
    end AndOrTypeMethods

    /** Intersection type `T & U` */
    type AndType <: AndOrType

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is an `AndType` */
    given AndTypeTypeTest: TypeTest[TypeRepr, AndType]

    /** Module object of `type AndType`  */
    val AndType: AndTypeModule

    /** Methods of the module object `val AndType` */
    trait AndTypeModule { this: AndType.type =>
      def apply(lhs: TypeRepr, rhs: TypeRepr): AndType
      def unapply(x: AndType): (TypeRepr, TypeRepr)
    }

    /** Union type `T | U` */
    type OrType <: AndOrType

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is an `OrType` */
    given OrTypeTypeTest: TypeTest[TypeRepr, OrType]

    /** Module object of `type OrType`  */
    val OrType: OrTypeModule

    /** Methods of the module object `val OrType` */
    trait OrTypeModule { this: OrType.type =>
      def apply(lhs: TypeRepr, rhs: TypeRepr): OrType
      def unapply(x: OrType): (TypeRepr, TypeRepr)
    }

    /** Type match `T match { case U => ... }` */
    type MatchType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `MatchType` */
    given MatchTypeTypeTest: TypeTest[TypeRepr, MatchType]

    /** Module object of `type MatchType`  */
    val MatchType: MatchTypeModule

    /** Methods of the module object `val MatchType` */
    trait MatchTypeModule { this: MatchType.type =>
      def apply(bound: TypeRepr, scrutinee: TypeRepr, cases: List[TypeRepr]): MatchType
      def unapply(x: MatchType): (TypeRepr, TypeRepr, List[TypeRepr])
    }

    /** Makes extension methods on `MatchType` available without any imports */
    given MatchTypeMethods: MatchTypeMethods

    /** Extension methods of `MatchType` */
    trait MatchTypeMethods where
      extension (self: MatchType)
        def bound: TypeRepr
        def scrutinee: TypeRepr
        def cases: List[TypeRepr]
      end extension
    end MatchTypeMethods

    /** Type of a by by name parameter */
    type ByNameType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `ByNameType` */
    given ByNameTypeTypeTest: TypeTest[TypeRepr, ByNameType]

    /** Module object of `type ByNameType`  */
    val ByNameType: ByNameTypeModule

    /** Methods of the module object `val ByNameType` */
    trait ByNameTypeModule { this: ByNameType.type =>
      def apply(underlying: TypeRepr): TypeRepr
      def unapply(x: ByNameType): Some[TypeRepr]
    }

    /** Makes extension methods on `ByNameType` available without any imports */
    given ByNameTypeMethods: ByNameTypeMethods

    /** Extension methods of `ByNameType` */
    trait ByNameTypeMethods where
      extension (self: ByNameType)
        def underlying: TypeRepr
      end extension
    end ByNameTypeMethods

    /** Type of a parameter reference */
    type ParamRef <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `ParamRef` */
    given ParamRefTypeTest: TypeTest[TypeRepr, ParamRef]

    /** Module object of `type ParamRef`  */
    val ParamRef: ParamRefModule

    /** Methods of the module object `val ParamRef` */
    trait ParamRefModule { this: ParamRef.type =>
      def unapply(x: ParamRef): (TypeRepr, Int)
    }

    /** Makes extension methods on `ParamRef` available without any imports */
    given ParamRefMethods: ParamRefMethods

    /** Extension methods of `ParamRef` */
    trait ParamRefMethods where
      extension (self: ParamRef)
        def binder: TypeRepr
        def paramNum: Int
      end extension
    end ParamRefMethods

    /** Type of `this` */
    type ThisType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `ThisType` */
    given ThisTypeTypeTest: TypeTest[TypeRepr, ThisType]

    /** Module object of `type ThisType`  */
    val ThisType: ThisTypeModule

    /** Methods of the module object `val ThisType` */
    trait ThisTypeModule { this: ThisType.type =>
      def unapply(x: ThisType): Some[TypeRepr]
    }

    /** Makes extension methods on `ThisType` available without any imports */
    given ThisTypeMethods: ThisTypeMethods

    /** Extension methods of `ThisType` */
    trait ThisTypeMethods where
      extension (self: ThisType)
        def tref: TypeRepr
      end extension
    end ThisTypeMethods

    /** A type that is recursively defined `this` */
    type RecursiveThis <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `RecursiveThis` */
    given RecursiveThisTypeTest: TypeTest[TypeRepr, RecursiveThis]

    /** Module object of `type RecursiveThis`  */
    val RecursiveThis: RecursiveThisModule

    /** Methods of the module object `val RecursiveThis` */
    trait RecursiveThisModule { this: RecursiveThis.type =>
      def unapply(x: RecursiveThis): Some[RecursiveType]
    }

    /** Makes extension methods on `RecursiveThis` available without any imports */
    given RecursiveThisMethods: RecursiveThisMethods

    /** Extension methods of `RecursiveThis` */
    trait RecursiveThisMethods where
      extension (self: RecursiveThis)
        def binder: RecursiveType
      end extension
    end RecursiveThisMethods

    /** A type that is recursively defined */
    type RecursiveType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `RecursiveType` */
    given RecursiveTypeTypeTest: TypeTest[TypeRepr, RecursiveType]

    /** Module object of `type RecursiveType`  */
    val RecursiveType: RecursiveTypeModule

    /** Methods of the module object `val RecursiveType` */
    trait RecursiveTypeModule { this: RecursiveType.type =>

      /** Create a RecType, normalizing its contents. This means:
      *
      *   1. Nested Rec types on the type's spine are merged with the outer one.
      *   2. Any refinement of the form `type T = z.T` on the spine of the type
      *      where `z` refers to the created rec-type is replaced by
      *      `type T`. This avoids infinite recursions later when we
      *      try to follow these references.
      */
      def apply(parentExp: RecursiveType => TypeRepr): RecursiveType

      def unapply(x: RecursiveType): Some[TypeRepr]
    }

    /** Makes extension methods on `RecursiveType` available without any imports */
    given RecursiveTypeMethods: RecursiveTypeMethods

    /** Extension methods of `RecursiveType` */
    trait RecursiveTypeMethods where
      extension (self: RecursiveType)
        def underlying: TypeRepr
        def recThis: RecursiveThis
      end extension
    end RecursiveTypeMethods

    /** Type of the definition of a method taking a single list of type or term parameters */
    type LambdaType <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `LambdaType` */
    given LambdaTypeTypeTest: TypeTest[TypeRepr, LambdaType]

    /** Makes extension methods on `LambdaType` available without any imports */
    given LambdaTypeMethods: LambdaTypeMethods

    /** Extension methods of `LambdaType` */
    trait LambdaTypeMethods where
      extension (self: LambdaType)
        def paramNames: List[String]
        def paramTypes: List[TypeRepr]
        def resType: TypeRepr
      end extension
    end LambdaTypeMethods

    /** Type of the definition of a method taking a single list of type or term parameters */
    type MethodOrPoly <: LambdaType

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `MethodOrPoly` */
    given MethodOrPolyTypeTest: TypeTest[TypeRepr, MethodOrPoly]

    /** Type of the definition of a method taking a single list of parameters. It's return type may be a MethodType. */
    type MethodType <: MethodOrPoly

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `MethodType` */
    given MethodTypeTypeTest: TypeTest[TypeRepr, MethodType]

    /** Module object of `type MethodType`  */
    val MethodType: MethodTypeModule

    /** Methods of the module object `val MethodType` */
    trait MethodTypeModule { this: MethodType.type =>
      def apply(paramNames: List[String])(paramInfosExp: MethodType => List[TypeRepr], resultTypeExp: MethodType => TypeRepr): MethodType
      def unapply(x: MethodType): (List[String], List[TypeRepr], TypeRepr)
    }

    /** Makes extension methods on `MethodType` available without any imports */
    given MethodTypeMethods: MethodTypeMethods

    /** Extension methods of `MethodType` */
    trait MethodTypeMethods where
      extension (self: MethodType)
        def isImplicit: Boolean
        def isErased: Boolean
        def param(idx: Int): TypeRepr
      end extension
    end MethodTypeMethods

    /** Type of the definition of a method taking a list of type parameters. It's return type may be a MethodType. */
    type PolyType <: MethodOrPoly

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `PolyType` */
    given PolyTypeTypeTest: TypeTest[TypeRepr, PolyType]

    /** Module object of `type PolyType`  */
    val PolyType: PolyTypeModule

    /** Methods of the module object `val PolyType` */
    trait PolyTypeModule { this: PolyType.type =>
      def apply(paramNames: List[String])(paramBoundsExp: PolyType => List[TypeBounds], resultTypeExp: PolyType => TypeRepr): PolyType
      def unapply(x: PolyType): (List[String], List[TypeBounds], TypeRepr)
    }

    /** Makes extension methods on `PolyType` available without any imports */
    given PolyTypeMethods: PolyTypeMethods

    /** Extension methods of `PolyType` */
    trait PolyTypeMethods where
      extension (self: PolyType)
        def param(idx: Int): TypeRepr
        def paramBounds: List[TypeBounds]
      end extension
    end PolyTypeMethods

    /** Type of the definition of a type lambda taking a list of type parameters. It's return type may be a TypeLambda. */
    type TypeLambda <: LambdaType

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `TypeLambda` */
    given TypeLambdaTypeTest: TypeTest[TypeRepr, TypeLambda]

    /** Module object of `type TypeLambda`  */
    val TypeLambda: TypeLambdaModule

    /** Methods of the module object `val TypeLambda` */
    trait TypeLambdaModule { this: TypeLambda.type =>
      def apply(paramNames: List[String], boundsFn: TypeLambda => List[TypeBounds], bodyFn: TypeLambda => TypeRepr): TypeLambda
      def unapply(x: TypeLambda): (List[String], List[TypeBounds], TypeRepr)
    }

    /** Makes extension methods on `TypeLambda` available without any imports */
    given TypeLambdaMethods: TypeLambdaMethods

    /** Extension methods of `TypeLambda` */
    trait TypeLambdaMethods where
      extension (self: TypeLambda)
        def param(idx: Int) : TypeRepr
        def paramBounds: List[TypeBounds]
      end extension
    end TypeLambdaMethods

    /** Case of a `MatchType` containing pattern `case P => R`.
     *
     *  Note: cases with type bindings are represented nested in a `TypeLambda`.
     */
    type MatchCase <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `MatchCase` */
    given MatchCaseTypeTest: TypeTest[TypeRepr, MatchCase]

    /** Module object of `type MatchCase`  */
    val MatchCase: MatchCaseModule

    /** Methods of the module object `val MatchCase` */
    trait MatchCaseModule { this: MatchCase.type =>
      /* Create match type case `case <pattern> => <rhs>` */
      def apply(pattern: TypeRepr, rhs: TypeRepr): MatchCase
      /* Matches a match type case `case <pattern> => <rhs>` */
      def unapply(x: MatchCase): (TypeRepr, TypeRepr)
    }

    /** Makes extension methods on `MatchCase` available without any imports */
    given MatchCaseMethods: MatchCaseMethods

    /** Extension methods of `MatchCase` */
    trait MatchCaseMethods where
      extension (self: MatchCase)
        /** Pattern `P` of `case P => R` in a `MatchType` */
        def pattern: TypeRepr
        /** RHS `R` of `case P => R` in a `MatchType` */
        def rhs: TypeRepr
      end extension
    end MatchCaseMethods


    // ----- TypeBounds -----------------------------------------------

    /** Type bounds */
    type TypeBounds <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `TypeBounds` */
    given TypeBoundsTypeTest: TypeTest[TypeRepr, TypeBounds]

    /** Module object of `type TypeBounds`  */
    val TypeBounds: TypeBoundsModule

    /** Methods of the module object `val TypeBounds` */
    trait TypeBoundsModule { this: TypeBounds.type =>
      def apply(low: TypeRepr, hi: TypeRepr): TypeBounds
      def unapply(x: TypeBounds): (TypeRepr, TypeRepr)
      def empty: TypeBounds
      def upper(hi: TypeRepr): TypeBounds
      def lower(lo: TypeRepr): TypeBounds
    }

    /** Makes extension methods on `TypeBounds` available without any imports */
    given TypeBoundsMethods: TypeBoundsMethods

    /** Extension methods of `TypeBounds` */
    trait TypeBoundsMethods where
      extension (self: TypeBounds)
        def low: TypeRepr
        def hi: TypeRepr
      end extension
    end TypeBoundsMethods

    // ----- NoPrefix -------------------------------------------------

    /** NoPrefix for a type selection */
    type NoPrefix <: TypeRepr

    /** `TypeTest` that allows testing at runtime in a pattern match if a `TypeRepr` is a `NoPrefix` */
    given NoPrefixTypeTest: TypeTest[TypeRepr, NoPrefix]

    /** Module object of `type NoPrefix`  */
    val NoPrefix: NoPrefixModule

    /** Methods of the module object `val NoPrefix` */
    trait NoPrefixModule { this: NoPrefix.type =>
      def unapply(x: NoPrefix): true
    }

    ///////////////
    // CONSTANTS //
    ///////////////

    /** Constant value represented as the constant itself */
    type Constant <: AnyRef

    /** Constant value represented as the constant itself */
    val Constant: ConstantModule

    /** Constant value represented as the constant itself */
    trait ConstantModule { this: Constant.type => }

    /** Makes extension methods on `Constant` available without any imports */
    given ConstantMethods: ConstantMethods

    /** Extension methods of `Constant` */
    trait ConstantMethods {
      extension (self: Constant)
        /** Returns the value of the constant */
        def value: Any

        /** Shows the constant as a String */
        def show(using Printer[Constant]): String

      end extension
    }

    /** Constant Boolean value */
    type BooleanConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `BooleanConstant` */
    given BooleanConstantTypeTest: TypeTest[Constant, BooleanConstant]

    /** Module object of `type BooleanConstant` */
    val BooleanConstant: BooleanConstantModule

    /** Methods of the module object `val BooleanConstant` */
    trait BooleanConstantModule { this: BooleanConstant.type =>
      /** Create a constant Boolean value */
      def apply(x: Boolean): BooleanConstant
      /** Match Boolean value constant and extract its value */
      def unapply(constant: BooleanConstant): Some[Boolean]
    }

    /** Constant Byte value */
    type ByteConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `ByteConstant` */
    given ByteConstantTypeTest: TypeTest[Constant, ByteConstant]

    /** Module object of `type ByteConstant` */
    val ByteConstant: ByteConstantModule

    /** Methods of the module object `val ByteConstant` */
    trait ByteConstantModule { this: ByteConstant.type =>
      /** Create a constant Byte value */
      def apply(x: Byte): ByteConstant
      /** Match Byte value constant and extract its value */
      def unapply(constant: ByteConstant): Some[Byte]
    }

    /** Constant Short value */
    type ShortConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `ShortConstant` */
    given ShortConstantTypeTest: TypeTest[Constant, ShortConstant]

    /** Module object of `type ShortConstant` */
    val ShortConstant: ShortConstantModule

    /** Methods of the module object `val Short` */
    trait ShortConstantModule { this: ShortConstant.type =>
      /** Create a constant Short value */
      def apply(x: Short): ShortConstant
      /** Match Short value constant and extract its value */
      def unapply(constant: ShortConstant): Some[Short]
    }

    /** Constant Int value */
    type IntConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `IntConstant` */
    given IntConstantTypeTest: TypeTest[Constant, IntConstant]

    /** Module object of `type IntConstant` */
    val IntConstant: IntConstantModule

    /** Methods of the module object `val IntConstant` */
    trait IntConstantModule { this: IntConstant.type =>
      /** Create a constant Int value */
      def apply(x: Int): IntConstant
      /** Match Int value constant and extract its value */
      def unapply(constant: IntConstant): Some[Int]
    }

    /** Constant Long value */
    type LongConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `LongConstant` */
    given LongConstantTypeTest: TypeTest[Constant, LongConstant]

    /** Module object of `type LongConstant` */
    val LongConstant: LongConstantModule

    /** Methods of the module object `val LongConstant` */
    trait LongConstantModule { this: LongConstant.type =>
      /** Create a constant Long value */
      def apply(x: Long): LongConstant
      /** Match Long value constant and extract its value */
      def unapply(constant: LongConstant): Some[Long]
    }

    /** Constant Float value */
    type FloatConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `FloatConstant` */
    given FloatConstantTypeTest: TypeTest[Constant, FloatConstant]

    /** Module object of `type FloatConstant` */
    val FloatConstant: FloatConstantModule

    /** Methods of the module object `val FloatConstant` */
    trait FloatConstantModule { this: FloatConstant.type =>
      /** Create a constant Float value */
      def apply(x: Float): FloatConstant
      /** Match Float value constant and extract its value */
      def unapply(constant: FloatConstant): Some[Float]
    }

    /** Constant Double value */
    type DoubleConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `DoubleConstant` */
    given DoubleConstantTypeTest: TypeTest[Constant, DoubleConstant]

    /** Module object of `type DoubleConstant` */
    val DoubleConstant: DoubleConstantModule

    /** Methods of the module object `val DoubleConstant` */
    trait DoubleConstantModule { this: DoubleConstant.type =>
      /** Create a constant Double value */
      def apply(x: Double): DoubleConstant
      /** Match Double value constant and extract its value */
      def unapply(constant: DoubleConstant): Some[Double]
    }

    /** Constant Char value */
    type CharConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `CharConstant` */
    given CharConstantTypeTest: TypeTest[Constant, CharConstant]

    /** Module object of `type CharConstant` */
    val CharConstant: CharConstantModule

    /** Methods of the module object `val CharConstant` */
    trait CharConstantModule { this: CharConstant.type =>
      /** Create a constant Char value */
      def apply(x: Char): CharConstant
      /** Match Char value constant and extract its value */
      def unapply(constant: CharConstant): Some[Char]
    }

    /** Constant String value */
    type StringConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `StringConstant` */
    given StringConstantTypeTest: TypeTest[Constant, StringConstant]

    /** Module object of `type StringConstant` */
    val StringConstant: StringConstantModule

    /** Methods of the module object `val StringConstant` */
    trait StringConstantModule { this: StringConstant.type =>
      /** Create a constant String value */
      def apply(x: String): StringConstant
      /** Match String value constant and extract its value */
      def unapply(constant: StringConstant): Some[String]
    }

    /** Constant Unit value */
    type UnitConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `UnitConstant` */
    given UnitConstantTypeTest: TypeTest[Constant, UnitConstant]

    /** Module object of `type UnitConstant` */
    val UnitConstant: UnitConstantModule

    /** Methods of the module object `val UnitConstant` */
    trait UnitConstantModule { this: UnitConstant.type =>
      /** Create a constant Unit value */
      def apply(): UnitConstant
      /** Match Unit value constant */
      def unapply(constant: UnitConstant): true
    }

    /** Constant null value */
    type NullConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `NullConstant` */
    given NullConstantTypeTest: TypeTest[Constant, NullConstant]

    /** Module object of `type NullConstant` */
    val NullConstant: NullConstantModule

    /** Methods of the module object `val NullConstant` */
    trait NullConstantModule { this: NullConstant.type =>
      /** Create a constant null value */
      def apply(): NullConstant
      /** Match null value constant */
      def unapply(constant: NullConstant): Boolean
    }

    /** Constant class value representing a `classOf[T]` */
    type ClassOfConstant <: Constant

    /** `TypeTest` that allows testing at runtime in a pattern match if a `Constant` is a `ClassOfConstant` */
    given ClassOfConstantTypeTest: TypeTest[Constant, ClassOfConstant]

    /** Module object of `type ClassOfConstant` */
    val ClassOfConstant: ClassOfConstantModule

    /** Methods of the module object `val ClassOf` */
    trait ClassOfConstantModule { this: ClassOfConstant.type =>
      /** Create a constant class value representing `classOf[<tpe>]` */
      def apply(tpe: TypeRepr): ClassOfConstant
      /** Match a class value constant representing `classOf[<tpe>]` and extract its type */
      def unapply(constant: ClassOfConstant): Option[TypeRepr]
    }

    /////////////////////
    // IMPLICIT SEARCH //
    /////////////////////

    /** Module object of `type Implicits`  */
    val Implicits: ImplicitsModule

    /** Methods of the module object `val Implicits` */
    trait ImplicitsModule { self: Implicits.type =>
      /** Find a given instance of type `T` in the current scope provided by the current enclosing splice.
      *  Return an `ImplicitSearchResult`.
      *
      *  @param tpe type of the implicit parameter
      */
      def search(tpe: TypeRepr): ImplicitSearchResult
    }

    /** Result of a given instance search */
    type ImplicitSearchResult <: AnyRef

    /** `TypeTest` that allows testing at runtime in a pattern match if an `ImplicitSearchResult` is an `ImplicitSearchSuccess` */
    given ImplicitSearchSuccessTypeTest: TypeTest[ImplicitSearchResult, ImplicitSearchSuccess]

    type ImplicitSearchSuccess <: ImplicitSearchResult

    /** Makes extension methods on `ImplicitSearchSuccess` available without any imports */
    given ImplicitSearchSuccessMethods: ImplicitSearchSuccessMethods

    /** Extension methods of `ImplicitSearchSuccess` */
    trait ImplicitSearchSuccessMethods where
      extension (self: ImplicitSearchSuccess)
        def tree: Term
      end extension
    end ImplicitSearchSuccessMethods

    type ImplicitSearchFailure <: ImplicitSearchResult

    /** `TypeTest` that allows testing at runtime in a pattern match if an `ImplicitSearchResult` is an `ImplicitSearchFailure` */
    given ImplicitSearchFailureTypeTest: TypeTest[ImplicitSearchResult, ImplicitSearchFailure]

    /** Makes extension methods on `ImplicitSearchFailure` available without any imports */
    given ImplicitSearchFailureMethods: ImplicitSearchFailureMethods

    /** Extension methods of `ImplicitSearchFailure` */
    trait ImplicitSearchFailureMethods where
      extension (self: ImplicitSearchFailure)
        def explanation: String
      end extension
    end ImplicitSearchFailureMethods

    type DivergingImplicit <: ImplicitSearchFailure

    /** `TypeTest` that allows testing at runtime in a pattern match if an `ImplicitSearchResult` is a `DivergingImplicit` */
    given DivergingImplicitTypeTest: TypeTest[ImplicitSearchResult, DivergingImplicit]

    type NoMatchingImplicits <: ImplicitSearchFailure

    /** `TypeTest` that allows testing at runtime in a pattern match if an `ImplicitSearchResult` is a `NoMatchingImplicits` */
    given NoMatchingImplicitsTypeTest: TypeTest[ImplicitSearchResult, NoMatchingImplicits]

    type AmbiguousImplicits <: ImplicitSearchFailure

    /** `TypeTest` that allows testing at runtime in a pattern match if an `ImplicitSearchResult` is an `AmbiguousImplicits` */
    given AmbiguousImplicitsTypeTest: TypeTest[ImplicitSearchResult, AmbiguousImplicits]

    /////////////
    // SYMBOLS //
    /////////////

    /** Symbol of a definition.
    *  Then can be compared with == to know if the definition is the same.
    */
    type Symbol <: AnyRef

    /** Module object of `type Symbol`  */
    val Symbol: SymbolModule

    /** Methods of the module object `val Symbol` */
    trait SymbolModule { this: Symbol.type =>

      /** Symbol of the definition that encloses the current splicing context.
       *
       *  For example, the following call to `spliceOwner` would return the symbol `x`.
       *  ```
       *  val x = ${ ... Symbol.spliceOwner ... }
       *  ```
       *
       *   For a macro splice, it is the symbol of the definition where the macro expansion happens.
       */
      def spliceOwner: Symbol

      /** Get package symbol if package is either defined in current compilation run or present on classpath. */
      def requiredPackage(path: String): Symbol

      /** Get class symbol if class is either defined in current compilation run or present on classpath. */
      def requiredClass(path: String): Symbol

      /** Get module symbol if module is either defined in current compilation run or present on classpath. */
      def requiredModule(path: String): Symbol

      /** Get method symbol if method is either defined in current compilation run or present on classpath. Throws if the method has an overload. */
      def requiredMethod(path: String): Symbol

      /** The class Symbol of a global class definition */
      def classSymbol(fullName: String): Symbol

      /** Generates a new method symbol with the given parent, name and type.
      *
      *  This symbol starts without an accompanying definition.
      *  It is the meta-programmer's responsibility to provide exactly one corresponding definition by passing
      *  this symbol to the DefDef constructor.
      *
      *  @note As a macro can only splice code into the point at which it is expanded, all generated symbols must be
      *        direct or indirect children of the reflection context's owner.
      */
      def newMethod(parent: Symbol, name: String, tpe: TypeRepr): Symbol

      /** Works as the other newMethod, but with additional parameters.
      *
      *  @param flags extra flags to with which the symbol should be constructed
      *  @param privateWithin the symbol within which this new method symbol should be private. May be noSymbol.
      */
      def newMethod(parent: Symbol, name: String, tpe: TypeRepr, flags: Flags, privateWithin: Symbol): Symbol

      /** Generates a new val/var/lazy val symbol with the given parent, name and type.
      *
      *  This symbol starts without an accompanying definition.
      *  It is the meta-programmer's responsibility to provide exactly one corresponding definition by passing
      *  this symbol to the ValDef constructor.
      *
      *  Note: Also see reflect.let
      *
      *  @param flags extra flags to with which the symbol should be constructed
      *  @param privateWithin the symbol within which this new method symbol should be private. May be noSymbol.
      *  @note As a macro can only splice code into the point at which it is expanded, all generated symbols must be
      *        direct or indirect children of the reflection context's owner.
      */
      def newVal(parent: Symbol, name: String, tpe: TypeRepr, flags: Flags, privateWithin: Symbol): Symbol

      /** Generates a pattern bind symbol with the given parent, name and type.
      *
      *  This symbol starts without an accompanying definition.
      *  It is the meta-programmer's responsibility to provide exactly one corresponding definition by passing
      *  this symbol to the BindDef constructor.
      *
      *  @param flags extra flags to with which the symbol should be constructed
      *  @note As a macro can only splice code into the point at which it is expanded, all generated symbols must be
      *        direct or indirect children of the reflection context's owner.
      */
      def newBind(parent: Symbol, name: String, flags: Flags, tpe: TypeRepr): Symbol

      /** Definition not available */
      def noSymbol: Symbol
    }

    /** Makes extension methods on `Symbol` available without any imports */
    given SymbolMethods: SymbolMethods

    /** Extension methods of `Symbol` */
    trait SymbolMethods {
      extension (self: Symbol)

        /** Owner of this symbol. The owner is the symbol in which this symbol is defined. Throws if this symbol does not have an owner. */
        def owner: Symbol

        /** Owner of this symbol. The owner is the symbol in which this symbol is defined. Returns `NoSymbol` if this symbol does not have an owner. */
        def maybeOwner: Symbol

        /** Flags of this symbol */
        def flags: Flags

        /** This symbol is private within the resulting type */
        def privateWithin: Option[TypeRepr]

        /** This symbol is protected within the resulting type */
        def protectedWithin: Option[TypeRepr]

        /** The name of this symbol */
        def name: String

        /** The full name of this symbol up to the root package */
        def fullName: String

        /** The position of this symbol */
        def pos: Option[Position]

        /** The documentation for this symbol, if any */
        def docstring: Option[String]

        /** Tree of this definition
        *
        *  If this symbol `isClassDef` it will return `a `ClassDef`,
        *  if this symbol `isTypeDef` it will return `a `TypeDef`,
        *  if this symbol `isValDef` it will return `a `ValDef`,
        *  if this symbol `isDefDef` it will return `a `DefDef`
        *  if this symbol `isBind` it will return `a `Bind`,
        *  else will throw
        */
        def tree: Tree

        /** Is the annotation defined with `annotSym` attached to this symbol? */
        def hasAnnotation(annotSym: Symbol): Boolean

        /** Get the annotation defined with `annotSym` attached to this symbol */
        def getAnnotation(annotSym: Symbol): Option[Term]

        /** Annotations attached to this symbol */
        def annotations: List[Term]

        /** Does this symbol come from a currently compiled source file? */
        def isDefinedInCurrentRun: Boolean

        /** Dummy val symbol that owns all statements within the initialization of the class.
        *  This may also contain local definitions such as classes defined in a `locally` block in the class.
        */
        def isLocalDummy: Boolean

        /** Is this symbol a class representing a refinement? */
        def isRefinementClass: Boolean

        /** Is this symbol an alias type? */
        def isAliasType: Boolean

        /** Is this symbol an anonymous class? */
        def isAnonymousClass: Boolean

        /** Is this symbol an anonymous function? */
        def isAnonymousFunction: Boolean

        /** Is this symbol an abstract type? */
        def isAbstractType: Boolean

        /** Is this the constructor of a class? */
        def isClassConstructor: Boolean

        /** Is this the definition of a type? */
        def isType: Boolean

        /** Is this the definition of a term? */
        def isTerm: Boolean

        /** Is this the definition of a PackageDef tree? */
        def isPackageDef: Boolean

        /** Is this the definition of a ClassDef tree? */
        def isClassDef: Boolean

        /** Is this the definition of a TypeDef tree */
        def isTypeDef: Boolean

        /** Is this the definition of a ValDef tree? */
        def isValDef: Boolean

        /** Is this the definition of a DefDef tree? */
        def isDefDef: Boolean

        /** Is this the definition of a Bind pattern? */
        def isBind: Boolean

        /** Does this symbol represent a no definition? */
        def isNoSymbol: Boolean

        /** Does this symbol represent a definition? */
        def exists: Boolean

        /** Field with the given name directly declared in the class */
        def declaredField(name: String): Symbol

        /** Fields directly declared in the class */
        def declaredFields: List[Symbol]

        /** Get named non-private fields declared or inherited */
        def memberField(name: String): Symbol

        /** Get all non-private fields declared or inherited */
        def memberFields: List[Symbol]

        /** Get non-private named methods defined directly inside the class */
        def declaredMethod(name: String): List[Symbol]

        /** Get all non-private methods defined directly inside the class, exluding constructors */
        def declaredMethods: List[Symbol]

        /** Get named non-private methods declared or inherited */
        def memberMethod(name: String): List[Symbol]

        /** Get all non-private methods declared or inherited */
        def memberMethods: List[Symbol]

        /** Get non-private named methods defined directly inside the class */
        def declaredType(name: String): List[Symbol]

        /** Get all non-private methods defined directly inside the class, exluding constructors */
        def declaredTypes: List[Symbol]

        /** Type member with the given name directly declared in the class */
        def memberType(name: String): Symbol

        /** Type member directly declared in the class */
        def memberTypes: List[Symbol]

        /** All members directly declared in the class */
        def declarations: List[Symbol]

        /** The symbols of each type parameter list and value parameter list of this
          *  method, or Nil if this isn't a method.
          */
        def paramSymss: List[List[Symbol]]

        /** Returns all symbols overridden by this symbol. */
        def allOverriddenSymbols: Iterator[Symbol]

        /** The symbol overriding this symbol in given subclass `ofclazz`.
         *
         *  @param ofclazz is a subclass of this symbol's owner
         */
        def overridingSymbol(ofclazz: Symbol): Symbol

        /** The primary constructor of a class or trait, `noSymbol` if not applicable. */
        def primaryConstructor: Symbol

        /** Fields of a case class type -- only the ones declared in primary constructor */
        def caseFields: List[Symbol]

        def isTypeParam: Boolean

        /** Signature of this definition */
        def signature: Signature

        /** The class symbol of the companion module class */
        def moduleClass: Symbol

        /** The symbol of the companion class */
        def companionClass: Symbol

        /** The symbol of the companion module */
        def companionModule: Symbol

        /** Case class or case object children of a sealed trait */
        def children: List[Symbol]
      end extension
    }

    ////////////////
    // SIGNATURES //
    ////////////////

    /** The signature of a method */
    type Signature <: AnyRef

    /** Module object of `type Signature`  */
    val Signature: SignatureModule

    /** Methods of the module object `val Signature` */
    trait SignatureModule { this: Signature.type =>
      /** Matches the method signature and returns its parameters and result type. */
      def unapply(sig: Signature): (List[String | Int], String)
    }

    /** Makes extension methods on `Signature` available without any imports */
    given SignatureMethods: SignatureMethods

    /** Extension methods of `Signature` */
    trait SignatureMethods {
      extension (self: Signature)

        /** The signatures of the method parameters.
          *
          *  Each *type parameter section* is represented by a single Int corresponding
          *  to the number of type parameters in the section.
          *  Each *term parameter* is represented by a String corresponding to the fully qualified
          *  name of the parameter type.
          */
        def paramSigs: List[String | Int]

        /** The signature of the result type */
        def resultSig: String

      end extension
    }

    //////////////////////////
    // STANDARD DEFINITIONS //
    //////////////////////////

    /** A value containing all standard definitions */
    val defn: defnModule

    /** Methods of the module object `val defn` */
    trait defnModule { self: defn.type =>

      /** The module symbol of root package `_root_`. */
      def RootPackage: Symbol

      /** The class symbol of root package `_root_`. */
      def RootClass: Symbol

      /** The class symbol of empty package `_root_._empty_`. */
      def EmptyPackageClass: Symbol

      /** The module symbol of package `scala`. */
      def ScalaPackage: Symbol

      /** The class symbol of package `scala`. */
      def ScalaPackageClass: Symbol

      /** The class symbol of core class `scala.Any`. */
      def AnyClass: Symbol

      /** The class symbol of core class `scala.AnyVal`. */
      def AnyValClass: Symbol

      /** The class symbol of core class `java.lang.Object`. */
      def ObjectClass: Symbol

      /** The type symbol of core class `scala.AnyRef`. */
      def AnyRefClass: Symbol

      /** The class symbol of core class `scala.Null`. */
      def NullClass: Symbol

      /** The class symbol of core class `scala.Nothing`. */
      def NothingClass: Symbol

      /** The class symbol of primitive class `scala.Unit`. */
      def UnitClass: Symbol

      /** The class symbol of primitive class `scala.Byte`. */
      def ByteClass: Symbol

      /** The class symbol of primitive class `scala.Short`. */
      def ShortClass: Symbol

      /** The class symbol of primitive class `scala.Char`. */
      def CharClass: Symbol

      /** The class symbol of primitive class `scala.Int`. */
      def IntClass: Symbol

      /** The class symbol of primitive class `scala.Long`. */
      def LongClass: Symbol

      /** The class symbol of primitive class `scala.Float`. */
      def FloatClass: Symbol

      /** The class symbol of primitive class `scala.Double`. */
      def DoubleClass: Symbol

      /** The class symbol of primitive class `scala.Boolean`. */
      def BooleanClass: Symbol

      /** The class symbol of class `scala.String`. */
      def StringClass: Symbol

      /** The class symbol of class `java.lang.Class`. */
      def ClassClass: Symbol

      /** The class symbol of class `scala.Array`. */
      def ArrayClass: Symbol

      /** The module symbol of module `scala.Predef`. */
      def PredefModule: Symbol

      /** The method symbol of method `scala.Predef.classOf`. */
      def Predef_classOf: Symbol

      /** The module symbol of package `java.lang`. */
      def JavaLangPackage: Symbol

      /** The module symbol of module `scala.Array`. */
      def ArrayModule: Symbol

      /** The method symbol of method `apply` in class `scala.Array`. */
      def Array_apply: Symbol

      /** The method symbol of method `clone` in class `scala.Array`. */
      def Array_clone: Symbol

      /** The method symbol of method `length` in class `scala.Array`. */
      def Array_length: Symbol

      /** The method symbol of method `update` in class `scala.Array`. */
      def Array_update: Symbol

      /** A dummy class symbol that is used to indicate repeated parameters
      *  compiled by the Scala compiler.
      */
      def RepeatedParamClass: Symbol

      /** The class symbol of class `scala.annotation.reflection.Repeated` */
      def RepeatedAnnot: Symbol

      /** The class symbol of class `scala.Option`. */
      def OptionClass: Symbol

      /** The module symbol of module `scala.None`. */
      def NoneModule: Symbol

      /** The module symbol of module `scala.Some`. */
      def SomeModule: Symbol

      /** Function-like object that maps arity to symbols for classes `scala.Product` */
      def ProductClass: Symbol

      /** Function-like object that maps arity to symbols for classes `scala.FunctionX`.
      *   -  0th element is `Function0`
      *   -  1st element is `Function1`
      *   -  ...
      *   -  Nth element is `FunctionN`
      */
      def FunctionClass(arity: Int, isImplicit: Boolean = false, isErased: Boolean = false): Symbol

      /** Function-like object that maps arity to symbols for classes `scala.TupleX`.
      *   -  0th element is `NoSymbol`
      *   -  1st element is `NoSymbol`
      *   -  2st element is `Tuple2`
      *   -  ...
      *   - 22nd element is `Tuple22`
      *   - 23nd element is `NoSymbol`  // TODO update when we will have more tuples
      *   - ...
      */
      def TupleClass(arity: Int): Symbol

      /** Returns `true` if `sym` is a `Tuple1`, `Tuple2`, ... `Tuple22` */
      def isTupleClass(sym: Symbol): Boolean

      /** Contains Scala primitive value classes:
      *   - Byte
      *   - Short
      *   - Int
      *   - Long
      *   - Float
      *   - Double
      *   - Char
      *   - Boolean
      *   - Unit
      */
      def ScalaPrimitiveValueClasses: List[Symbol]

      /** Contains Scala numeric value classes:
      *   - Byte
      *   - Short
      *   - Int
      *   - Long
      *   - Float
      *   - Double
      *   - Char
      */
      def ScalaNumericValueClasses: List[Symbol]

    }

    ///////////////
    //   FLAGS   //
    ///////////////

    /** FlagSet of a Symbol */
    type Flags

    /** Module object of `type Flags`  */
    val Flags: FlagsModule

    /** Methods of the module object `val Flags` */
    trait FlagsModule { this: Flags.type =>

      /** Is this symbol `abstract` */
      def Abstract: Flags

      /** Is this generated by Scala compiler.
       *  Coresponds to ACC_SYNTHETIC in the JVM.
       */
      def Artifact: Flags

      /** Is this symbol `case` */
      def Case: Flags

      /** Is this symbol a getter for case class parameter */
      def CaseAccessor: Flags

      /** Is this symbol a type parameter marked as contravariant `-` */
      def Contravariant: Flags

      /** Is this symbol a type parameter marked as covariant `+` */
      def Covariant: Flags

      /** Is a declared, but not defined member */
      def Deferred: Flags

      /** The empty set of flags */
      def EmptyFlags: Flags

      /** Is this symbol an enum */
      def Enum: Flags

      /** Is this symbol `erased` */
      def Erased: Flags

      /** Is this symbol exported from provided instance */
      def Exported: Flags

      /** Is this symbol a `def` defined in an `extension` */
      def ExtensionMethod: Flags

      /** Is this symbol a getter or a setter */
      def FieldAccessor: Flags

      /** Is this symbol `final` */
      def Final: Flags

      /** Is this symbol an inferable ("given") parameter */
      def Given: Flags

      /** Is this symbol a parameter with a default value? */
      def HasDefault: Flags

      /** Is this symbol `implicit` */
      def Implicit: Flags

      /** Is an infix method or type */
      def Infix: Flags

      /** Is this symbol `inline` */
      def Inline: Flags

      /** Is this symbol defined in a Java class */
      def JavaDefined: Flags

      /** Is implemented as a Java static */
      def JavaStatic: Flags

      /** Is this symbol `lazy` */
      def Lazy: Flags

      /** Is this symbol local? Used in conjunction with private/private[T] to mean private[this] extends Modifier proctected[this] */
      def Local: Flags

      /** Is this symbol marked as a macro. An inline method containing toplevel splices */
      def Macro: Flags

      def Method: Flags

      /** Is this symbol an object or its class (used for a ValDef or a ClassDef extends Modifier respectively) */
      def Module: Flags

      /** Is this symbol a `var` (when used on a ValDef) */
      def Mutable: Flags

      /** Trait does not have fields or initialization code. */
      def NoInits: Flags

      /** Is this symbol `opaque` */
      def Opaque: Flags

      /** Is this symbol `open` */
      def Open: Flags

      /** Is this symbol `override` */
      def Override: Flags

      /** Is this symbol a package */
      def Package: Flags

      /** Is this symbol a parameter */
      def Param: Flags

      /** Is this symbol a parameter accessor */
      def ParamAccessor: Flags

      /** Is this symbol `private` */
      def Private: Flags

      /** Is this symbol labeled private[this] */
      def PrivateLocal: Flags

      /** Is this symbol `protected` */
      def Protected: Flags

      /** Was this symbol imported from Scala2.x */
      def Scala2x: Flags

      /** Is this symbol `sealed` */
      def Sealed: Flags

      /** Is this symbol member that is assumed to be stable and realizable */
      def StableRealizable: Flags

      /** Is this symbol marked as static. Mapped to static Java member */
      def Static: Flags

      /** Is this symbol to be tagged Java Synthetic */
      def Synthetic: Flags

      /** Is this symbol a trait */
      def Trait: Flags

      /** Is a transparent inline method or trait */
      def Transparent: Flags

    }

    /** Makes extension methods on `Flags` available without any imports */
    given FlagsMethods: FlagsMethods

    /** Extension methods of `Flags` */
    trait FlagsMethods {
      extension (self: Flags)
        /** Is the given flag set a subset of this flag sets */
        def is(that: Flags): Boolean

        /** Union of the two flag sets */
        def |(that: Flags): Flags

        /** Intersection of the two flag sets */
        def &(that: Flags): Flags

        /** Shows the flags as a String */
        def show: String

      end extension
    }

    ///////////////
    // POSITIONS //
    ///////////////


    /** Position in a source file */
    type Position <: AnyRef

    /** Module object of `type Position`  */
    val Position: PositionModule

    /** Methods of the module object `val Position` */
    trait PositionModule { this: Position.type =>
      /** Position of the expansion site of the macro */
      def ofMacroExpansion: Position

      /** Create a new position in the source with the given range. The range must be contained in the file. */
      def apply(sourceFile: SourceFile, start: Int, end: Int): Position
    }

    /** Makes extension methods on `Position` available without any imports */
    given PositionMethods: PositionMethods

    /** Extension methods of `Position` */
    trait PositionMethods {
      extension (self: Position)

        /** The start offset in the source file */
        def start: Int

        /** The end offset in the source file */
        def end: Int

        /** Source file in which this position is located */
        def sourceFile: SourceFile

        /** The start line in the source file */
        def startLine: Int

        /** The end line in the source file */
        def endLine: Int

        /** The start column in the source file */
        def startColumn: Int

        /** The end column in the source file */
        def endColumn: Int

        /** Source code within the position */
        def sourceCode: Option[String]

      end extension
    }

    /** Scala source file */
    type SourceFile <: AnyRef

    /** Module object of `type SourceFile`  */
    val SourceFile: SourceFileModule

    /** Methods of the module object `val SourceFile` */
    trait SourceFileModule { this: SourceFile.type =>
      /** Returns the source file being compiled. The path is relative to the current working directory. */
      def current: SourceFile
    }

    /** Makes extension methods on `SourceFile` available without any imports */
    given SourceFileMethods: SourceFileMethods

    /** Extension methods of `SourceFile` */
    trait SourceFileMethods {
      extension (self: SourceFile)
        /** Path to this source file */
        def jpath: java.nio.file.Path

        /** Content of this source file */
        def content: Option[String]
      end extension
    }

    ///////////////
    // REPORTING //
    ///////////////

    /** Module containg error and waring reporiting. */
    val report: reportModule

    /** Methods of the module object `val report` */
    trait reportModule { self: report.type =>

      /** Report an error at the position of the macro expansion */
      def error(msg: String): Unit

      /** Report an error at the position of `expr` */
      def error(msg: String, expr: Expr[Any]): Unit

      /** Report an error message at the given position */
      def error(msg: String, pos: Position): Unit

      /** Report an error at the position of the macro expansion and throws a StopMacroExpansion */
      def throwError(msg: String): Nothing

      /** Report an error at the position of `expr` */
      def throwError(msg: String, expr: Expr[Any]): Nothing

      /** Report an error message at the given position and throws a StopMacroExpansion */
      def throwError(msg: String, pos: Position): Nothing

      /** Report a warning at the position of the macro expansion */
      def warning(msg: String): Unit

      /** Report a warning at the on the position of `expr` */
      def warning(msg: String, expr: Expr[Any]): Unit

      /** Report an warning message at the given position */
      def warning(msg: String, pos: Position): Unit

    }



    ///////////////
    //   UTILS   //
    ///////////////

    /** Customizable Tree accumulator.
    *
    *  Usage:
    *  ```
    *  import qctx.reflect._
    *  class MyTreeAccumulator extends TreeAccumulator[X] {
    *    def foldTree(x: X, tree: Tree)(owner: Symbol): X = ...
    *  }
    *  ```
    */
    trait TreeAccumulator[X] where

      // Ties the knot of the traversal: call `foldOver(x, tree))` to dive in the `tree` node.
      def foldTree(x: X, tree: Tree)(owner: Symbol): X

      def foldTrees(x: X, trees: Iterable[Tree])(owner: Symbol): X = trees.foldLeft(x)((acc, y) => foldTree(acc, y)(owner))

      def foldOverTree(x: X, tree: Tree)(owner: Symbol): X = {
        tree match {
          case Ident(_) =>
            x
          case Select(qualifier, _) =>
            foldTree(x, qualifier)(owner)
          case This(qual) =>
            x
          case Super(qual, _) =>
            foldTree(x, qual)(owner)
          case Apply(fun, args) =>
            foldTrees(foldTree(x, fun)(owner), args)(owner)
          case TypeApply(fun, args) =>
            foldTrees(foldTree(x, fun)(owner), args)(owner)
          case Literal(const) =>
            x
          case New(tpt) =>
            foldTree(x, tpt)(owner)
          case Typed(expr, tpt) =>
            foldTree(foldTree(x, expr)(owner), tpt)(owner)
          case NamedArg(_, arg) =>
            foldTree(x, arg)(owner)
          case Assign(lhs, rhs) =>
            foldTree(foldTree(x, lhs)(owner), rhs)(owner)
          case Block(stats, expr) =>
            foldTree(foldTrees(x, stats)(owner), expr)(owner)
          case If(cond, thenp, elsep) =>
            foldTree(foldTree(foldTree(x, cond)(owner), thenp)(owner), elsep)(owner)
          case While(cond, body) =>
            foldTree(foldTree(x, cond)(owner), body)(owner)
          case Closure(meth, tpt) =>
            foldTree(x, meth)(owner)
          case Match(selector, cases) =>
            foldTrees(foldTree(x, selector)(owner), cases)(owner)
          case Return(expr, _) =>
            foldTree(x, expr)(owner)
          case Try(block, handler, finalizer) =>
            foldTrees(foldTrees(foldTree(x, block)(owner), handler)(owner), finalizer)(owner)
          case Repeated(elems, elemtpt) =>
            foldTrees(foldTree(x, elemtpt)(owner), elems)(owner)
          case Inlined(call, bindings, expansion) =>
            foldTree(foldTrees(x, bindings)(owner), expansion)(owner)
          case vdef @ ValDef(_, tpt, rhs) =>
            val owner = vdef.symbol
            foldTrees(foldTree(x, tpt)(owner), rhs)(owner)
          case ddef @ DefDef(_, tparams, vparamss, tpt, rhs) =>
            val owner = ddef.symbol
            foldTrees(foldTree(vparamss.foldLeft(foldTrees(x, tparams)(owner))((acc, y) => foldTrees(acc, y)(owner)), tpt)(owner), rhs)(owner)
          case tdef @ TypeDef(_, rhs) =>
            val owner = tdef.symbol
            foldTree(x, rhs)(owner)
          case cdef @ ClassDef(_, constr, parents, derived, self, body) =>
            val owner = cdef.symbol
            foldTrees(foldTrees(foldTrees(foldTrees(foldTree(x, constr)(owner), parents)(owner), derived)(owner), self)(owner), body)(owner)
          case Import(expr, _) =>
            foldTree(x, expr)(owner)
          case Export(expr, _) =>
            foldTree(x, expr)(owner)
          case clause @ PackageClause(pid, stats) =>
            foldTrees(foldTree(x, pid)(owner), stats)(clause.symbol)
          case Inferred() => x
          case TypeIdent(_) => x
          case TypeSelect(qualifier, _) => foldTree(x, qualifier)(owner)
          case TypeProjection(qualifier, _) => foldTree(x, qualifier)(owner)
          case Singleton(ref) => foldTree(x, ref)(owner)
          case Refined(tpt, refinements) => foldTrees(foldTree(x, tpt)(owner), refinements)(owner)
          case Applied(tpt, args) => foldTrees(foldTree(x, tpt)(owner), args)(owner)
          case ByName(result) => foldTree(x, result)(owner)
          case Annotated(arg, annot) => foldTree(foldTree(x, arg)(owner), annot)(owner)
          case LambdaTypeTree(typedefs, arg) => foldTree(foldTrees(x, typedefs)(owner), arg)(owner)
          case TypeBind(_, tbt) => foldTree(x, tbt)(owner)
          case TypeBlock(typedefs, tpt) => foldTree(foldTrees(x, typedefs)(owner), tpt)(owner)
          case MatchTypeTree(boundopt, selector, cases) =>
            foldTrees(foldTree(boundopt.fold(x)(y => foldTree(x, y)(owner)), selector)(owner), cases)(owner)
          case WildcardTypeTree() => x
          case TypeBoundsTree(lo, hi) => foldTree(foldTree(x, lo)(owner), hi)(owner)
          case CaseDef(pat, guard, body) => foldTree(foldTrees(foldTree(x, pat)(owner), guard)(owner), body)(owner)
          case TypeCaseDef(pat, body) => foldTree(foldTree(x, pat)(owner), body)(owner)
          case Bind(_, body) => foldTree(x, body)(owner)
          case Unapply(fun, implicits, patterns) => foldTrees(foldTrees(foldTree(x, fun)(owner), implicits)(owner), patterns)(owner)
          case Alternatives(patterns) => foldTrees(x, patterns)(owner)
        }
      }
    end TreeAccumulator


    /** Customizable tree traverser.
    *
    *  Usage:
    *  ```
    *  import qctx.relfect._
    *  class MyTraverser extends TreeTraverser {
    *    override def traverseTree(tree: Tree)(owner: Symbol): Unit = ...
    *  }
    *  ```
    */
    trait TreeTraverser extends TreeAccumulator[Unit] where

      def traverseTree(tree: Tree)(owner: Symbol): Unit = traverseTreeChildren(tree)(owner)

      def foldTree(x: Unit, tree: Tree)(owner: Symbol): Unit = traverseTree(tree)(owner)

      protected def traverseTreeChildren(tree: Tree)(owner: Symbol): Unit = foldOverTree((), tree)(owner)

    end TreeTraverser

    /** Customizable tree mapper.
    *
    *  Usage:
    *  ```
    *  import quotes.reflect._
    *  class MyTreeMap extends TreeMap {
    *    override def transformTree(tree: Tree)(owner: Symbol): Tree = ...
    *  }
    *  ```
    */
    trait TreeMap where

      def transformTree(tree: Tree)(owner: Symbol): Tree = {
        tree match {
          case tree: PackageClause =>
            PackageClause.copy(tree)(transformTerm(tree.pid).asInstanceOf[Ref], transformTrees(tree.stats)(tree.symbol))
          case tree: Import =>
            Import.copy(tree)(transformTerm(tree.expr)(owner), tree.selectors)
          case tree: Export =>
            tree
          case tree: Statement =>
            transformStatement(tree)(owner)
          case tree: TypeTree => transformTypeTree(tree)(owner)
          case tree: TypeBoundsTree =>
            TypeBoundsTree.copy(tree)(transformTypeTree(tree.low)(owner), transformTypeTree(tree.hi)(owner))
          case tree: WildcardTypeTree => tree
          case tree: CaseDef =>
            transformCaseDef(tree)(owner)
          case tree: TypeCaseDef =>
            transformTypeCaseDef(tree)(owner)
          case pattern: Bind =>
            Bind.copy(pattern)(pattern.name, pattern.pattern)
          case pattern: Unapply =>
            Unapply.copy(pattern)(transformTerm(pattern.fun)(owner), transformSubTrees(pattern.implicits)(owner), transformTrees(pattern.patterns)(owner))
          case pattern: Alternatives =>
            Alternatives.copy(pattern)(transformTrees(pattern.patterns)(owner))
        }
      }

      def transformStatement(tree: Statement)(owner: Symbol): Statement = {
        tree match {
          case tree: Term =>
            transformTerm(tree)(owner)
          case tree: ValDef =>
            val owner = tree.symbol
            val tpt1 = transformTypeTree(tree.tpt)(owner)
            val rhs1 = tree.rhs.map(x => transformTerm(x)(owner))
            ValDef.copy(tree)(tree.name, tpt1, rhs1)
          case tree: DefDef =>
            val owner = tree.symbol
            DefDef.copy(tree)(tree.name, transformSubTrees(tree.typeParams)(owner), tree.paramss mapConserve (x => transformSubTrees(x)(owner)), transformTypeTree(tree.returnTpt)(owner), tree.rhs.map(x => transformTerm(x)(owner)))
          case tree: TypeDef =>
            val owner = tree.symbol
            TypeDef.copy(tree)(tree.name, transformTree(tree.rhs)(owner))
          case tree: ClassDef =>
            ClassDef.copy(tree)(tree.name, tree.constructor, tree.parents, tree.derived, tree.self, tree.body)
          case tree: Import =>
            Import.copy(tree)(transformTerm(tree.expr)(owner), tree.selectors)
          case tree: Export =>
            tree
        }
      }

      def transformTerm(tree: Term)(owner: Symbol): Term = {
        tree match {
          case Ident(name) =>
            tree
          case Select(qualifier, name) =>
            Select.copy(tree)(transformTerm(qualifier)(owner), name)
          case This(qual) =>
            tree
          case Super(qual, mix) =>
            Super.copy(tree)(transformTerm(qual)(owner), mix)
          case Apply(fun, args) =>
            Apply.copy(tree)(transformTerm(fun)(owner), transformTerms(args)(owner))
          case TypeApply(fun, args) =>
            TypeApply.copy(tree)(transformTerm(fun)(owner), transformTypeTrees(args)(owner))
          case Literal(const) =>
            tree
          case New(tpt) =>
            New.copy(tree)(transformTypeTree(tpt)(owner))
          case Typed(expr, tpt) =>
            Typed.copy(tree)(transformTerm(expr)(owner), transformTypeTree(tpt)(owner))
          case tree: NamedArg =>
            NamedArg.copy(tree)(tree.name, transformTerm(tree.value)(owner))
          case Assign(lhs, rhs) =>
            Assign.copy(tree)(transformTerm(lhs)(owner), transformTerm(rhs)(owner))
          case Block(stats, expr) =>
            Block.copy(tree)(transformStats(stats)(owner), transformTerm(expr)(owner))
          case If(cond, thenp, elsep) =>
            If.copy(tree)(transformTerm(cond)(owner), transformTerm(thenp)(owner), transformTerm(elsep)(owner))
          case Closure(meth, tpt) =>
            Closure.copy(tree)(transformTerm(meth)(owner), tpt)
          case Match(selector, cases) =>
            Match.copy(tree)(transformTerm(selector)(owner), transformCaseDefs(cases)(owner))
          case Return(expr, from) =>
            Return.copy(tree)(transformTerm(expr)(owner), from)
          case While(cond, body) =>
            While.copy(tree)(transformTerm(cond)(owner), transformTerm(body)(owner))
          case Try(block, cases, finalizer) =>
            Try.copy(tree)(transformTerm(block)(owner), transformCaseDefs(cases)(owner), finalizer.map(x => transformTerm(x)(owner)))
          case Repeated(elems, elemtpt) =>
            Repeated.copy(tree)(transformTerms(elems)(owner), transformTypeTree(elemtpt)(owner))
          case Inlined(call, bindings, expansion) =>
            Inlined.copy(tree)(call, transformSubTrees(bindings)(owner), transformTerm(expansion)(owner))
        }
      }

      def transformTypeTree(tree: TypeTree)(owner: Symbol): TypeTree = tree match {
        case Inferred() => tree
        case tree: TypeIdent => tree
        case tree: TypeSelect =>
          TypeSelect.copy(tree)(tree.qualifier, tree.name)
        case tree: TypeProjection =>
          TypeProjection.copy(tree)(tree.qualifier, tree.name)
        case tree: Annotated =>
          Annotated.copy(tree)(tree.arg, tree.annotation)
        case tree: Singleton =>
          Singleton.copy(tree)(transformTerm(tree.ref)(owner))
        case tree: Refined =>
          Refined.copy(tree)(transformTypeTree(tree.tpt)(owner), transformTrees(tree.refinements)(owner).asInstanceOf[List[Definition]])
        case tree: Applied =>
          Applied.copy(tree)(transformTypeTree(tree.tpt)(owner), transformTrees(tree.args)(owner))
        case tree: MatchTypeTree =>
          MatchTypeTree.copy(tree)(tree.bound.map(b => transformTypeTree(b)(owner)), transformTypeTree(tree.selector)(owner), transformTypeCaseDefs(tree.cases)(owner))
        case tree: ByName =>
          ByName.copy(tree)(transformTypeTree(tree.result)(owner))
        case tree: LambdaTypeTree =>
          LambdaTypeTree.copy(tree)(transformSubTrees(tree.tparams)(owner), transformTree(tree.body)(owner))
        case tree: TypeBind =>
          TypeBind.copy(tree)(tree.name, tree.body)
        case tree: TypeBlock =>
          TypeBlock.copy(tree)(tree.aliases, tree.tpt)
      }

      def transformCaseDef(tree: CaseDef)(owner: Symbol): CaseDef = {
        CaseDef.copy(tree)(transformTree(tree.pattern)(owner), tree.guard.map(x => transformTerm(x)(owner)), transformTerm(tree.rhs)(owner))
      }

      def transformTypeCaseDef(tree: TypeCaseDef)(owner: Symbol): TypeCaseDef = {
        TypeCaseDef.copy(tree)(transformTypeTree(tree.pattern)(owner), transformTypeTree(tree.rhs)(owner))
      }

      def transformStats(trees: List[Statement])(owner: Symbol): List[Statement] =
        trees mapConserve (x => transformStatement(x)(owner))

      def transformTrees(trees: List[Tree])(owner: Symbol): List[Tree] =
        trees mapConserve (x => transformTree(x)(owner))

      def transformTerms(trees: List[Term])(owner: Symbol): List[Term] =
        trees mapConserve (x => transformTerm(x)(owner))

      def transformTypeTrees(trees: List[TypeTree])(owner: Symbol): List[TypeTree] =
        trees mapConserve (x => transformTypeTree(x)(owner))

      def transformCaseDefs(trees: List[CaseDef])(owner: Symbol): List[CaseDef] =
        trees mapConserve (x => transformCaseDef(x)(owner))

      def transformTypeCaseDefs(trees: List[TypeCaseDef])(owner: Symbol): List[TypeCaseDef] =
        trees mapConserve (x => transformTypeCaseDef(x)(owner))

      def transformSubTrees[Tr <: Tree](trees: List[Tr])(owner: Symbol): List[Tr] =
        transformTrees(trees)(owner).asInstanceOf[List[Tr]]

    end TreeMap

    /** Type class used in `show` methods to provide customizable `String` representations */
    trait Printer[T] where
      /** Show the arguments as a `String` */
      def show(x: T): String
    end Printer

    /** Default pinter for `Tree` used when calling `tree.show` */
    given TreePrinter: Printer[Tree] = Printer.TreeCode

    /** Default pinter for `TypeRepr` used when calling `tpe.show` */
    given TypeReprPrinter: Printer[TypeRepr] = Printer.TypeReprCode

    /** Default pinter for `Constant` used when calling `const.show` */
    given ConstantPrinter: Printer[Constant] = Printer.ConstantCode

    /** Module object of `type Printer`.
     *  Contains custom printers such as `TreeCode`, `TreeAnsiCode`, `TreeCases`, `TypeReprCode`, ..., `SymbolFullName` and `FlagsCombination`.
     */
    val Printer: PrinterModule

    /** Methods of the module object `val Printer` */
    trait PrinterModule { self: Printer.type =>
      /** Prints fully elaborated vesion of the source code. */
      def TreeCode: Printer[Tree]

      /** Prints fully elaborated vesion of the source code.
       *  Same as `TreeCode` but does not print full package prefixes.
       */
      def TreeShortCode: Printer[Tree]

      /** Prints fully elaborated vesion of the source code using ANSI colors. */
      def TreeAnsiCode: Printer[Tree]

      /** Prints a pattern like representation of the `Tree`.
       *  It displays the structure of the AST.
       */
      def TreeStructure: Printer[Tree]

      /** Prints the type in source code. */
      def TypeReprCode: Printer[TypeRepr]

      /** Prints the type in source code.
       *  Same as `TypeReprCode` but does not print full package prefixes.
       */
      def TypeReprShortCode: Printer[TypeRepr]

      /** Prints the type in source code using ANSI colors. */
      def TypeReprAnsiCode: Printer[TypeRepr]

      /** Prints a pattern like representation of the `TypeRepr`.
       *  It displays the structure of the type.
       */
      def TypeReprStructure: Printer[TypeRepr]

      /** Prints the constant in source code. */
      def ConstantCode: Printer[Constant]

      /** Prints a pattern like representation of the `Constant`. */
      def ConstantStructure: Printer[Constant]
    }

  }

  /** Type of a `Quotes` provided by a splice within a quote that took this context. */
  type Nested = Quotes

}
