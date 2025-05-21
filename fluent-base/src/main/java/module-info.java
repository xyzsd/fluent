module net.xyzsd.fluent {
    // TODO: transitive, static or 'static transitive' ? need to decide
    requires static transitive org.jspecify;
    // these are currently automatic modules ... careful
    requires com.ibm.icu;
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
    exports fluent.functions.list;
    exports fluent.functions.numeric;
    exports fluent.functions.string;
    exports fluent.functions.temporal;
    exports fluent.functions.list.reducer;
}