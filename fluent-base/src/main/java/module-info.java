module net.xyzsd.fluent {
    requires com.ibm.icu;
    // TODO: transitive, static or 'static transitive' ? need to decide
    requires static transitive org.jspecify;
    requires org.jetbrains.annotations;
    //
    // we are exporting all for now.
    exports fluent.bundle;
    exports fluent.bundle.resolver;
    exports fluent.functions;
    exports fluent.syntax.AST;
    exports fluent.syntax.parser;
    exports fluent.types;
    //
    exports fluent.functions.icu;
    exports fluent.functions.icu.list;
    exports fluent.functions.icu.numeric;
    exports fluent.functions.icu.string;
    exports fluent.functions.icu.temporal;
}