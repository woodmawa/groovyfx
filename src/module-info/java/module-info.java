import groovyx.javafx.beans.FXBindableASTTransformation;

@SuppressWarnings("JavaModuleNaming")
module org.groovyfx {
    requires  org.apache.groovy;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.web;
    requires transitive javafx.media;
    requires transitive javafx.swing;

    exports groovyx.javafx;
    exports groovyx.javafx.beans;

    opens groovyx.javafx to javafx.graphics, javafx.fxml, org.apache.groovy;
    opens groovyx.javafx.beans to org.apache.groovy;

    provides org.codehaus.groovy.transform.ASTTransformation
            with FXBindableASTTransformation;
}