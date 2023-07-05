/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.FeatureModelException;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import at.tugraz.ist.ase.fm2exconf.parser.antlr4.FM4ConfBaseListener;
import at.tugraz.ist.ase.fm2exconf.parser.antlr4.FM4ConfLexer;
import at.tugraz.ist.ase.fm2exconf.parser.antlr4.FM4ConfParser;
import at.tugraz.ist.ase.fm2exconf.parser.antlr4.FM4ConfParser.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser for the descriptive format
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class DescriptiveFormatParser implements BaseParser {

    /**
     * Check whether the format of the given file is Descriptive format
     *
     * @param filePath - a {@link File}
     * @@return true - if the format of the given file is Descriptive format
     *          false - otherwise
     */
    @Override
    public boolean checkFormat(File filePath) {
        return isFM4ConfParser(filePath);
    }

    /**
     * Check whether the format of the given file is Descriptive format
     *
     * @param filePath - a {@link File}
     * @return true - if the format of the given file is Descriptive format
     *         false - otherwise
     */
    private boolean isFM4ConfParser(File filePath) {
        if (filePath == null) return false;
        // first, check the extension of file
        if (!filePath.getName().endsWith(".fm4conf")) {
            return false;
        }
        // second, check the structure of file
        try {
            // use ANTLR4 to parse, if it raise an exception
            InputStream is = new FileInputStream(filePath);

            ANTLRInputStream input = new ANTLRInputStream(is);
            FM4ConfLexer lexer = new FM4ConfLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            FM4ConfParser parser = new FM4ConfParser(tokens);
            ParseTree tree = parser.model();
        } catch (Exception e) {
            return false; // it's not Descriptive format
        }
        return true;
    }

    /**
     * @return the {@link FMFormat} of the parser
     */
    @Override
    public FMFormat getFormat() {
        return FMFormat.DESCRIPTIVE;
    }

    @Override
    public FeatureModel parse(File filePath) throws ParserException {
        if (filePath == null) throw new ParserException("filePath cannot be empty!");
        if (!isFM4ConfParser(filePath)) throw new ParserException("The format of file is not Descriptive format or there exists errors in the file!");

        try {
            InputStream is = new FileInputStream(filePath);

            ANTLRInputStream input = new ANTLRInputStream(is);
            FM4ConfLexer lexer = new FM4ConfLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            FM4ConfParser parser = new FM4ConfParser(tokens);
            ParseTree tree = parser.model();

            // create a standard ANTLR parse tree walker
            ParseTreeWalker walker = new ParseTreeWalker();
            // create listener then feed to walker
            FM4ConfListener listener = new FM4ConfListener();

            FeatureModel fm = new FeatureModel(getFormat(), filePath);
            listener.featureModel = fm;

            walker.walk(listener, tree); // walk parse tree

            return fm;
        } catch (IOException | NullPointerException e) {
            throw new ParserException(e.getMessage());
        }
    }

    public static class FM4ConfListener extends FM4ConfBaseListener {
        public FeatureModel featureModel;

        @Override
        public void exitFeature(FeatureContext ctx) throws ParserException, FeatureModelException {
            List<IdentifierContext> ids = ctx.identifier();
            for (IdentifierContext idCx : ids) {
                String name = idCx.getText();
                if (name.isEmpty())
                {
                    throw new ParserException("The feature name could not be blank!");
                }
                if (!name.matches("[a-zA-Z_][0-9a-zA-Z_\\s\\-]*")
                        || name.matches("[a-zA-Z]{1,3}\\$?[0-9]{1,7}(\\:?\\$?[a-zA-Z]{1,3}\\$?[0-9]{1,7})?")
                        || name.length() > 255) {
                    StringBuilder st = new StringBuilder("\"" + name + "\" is a wrong name!");
                    st.append("\n").append("The first character of a feature name must be a letter, or an underscore character (_).");
                    st.append("\n").append("Remaining characters in the feature name can be letters, numbers, periods and underscore characters.");
                    st.append("\n").append("Names cannot be the same as a cell reference, such as ABC100 or Z$100 or R1C1.");
                    st.append("\n").append("A name can contain up to 255 characters.");

                    st.append("\n\n").append("Invalid name: " + name);

                    throw new ParserException(st.toString());
                }

                featureModel.addFeature(name);
            }
        }

        @Override
        public void exitMandatory(MandatoryContext ctx) {
            addRelationship(Relationship.RelationshipType.MANDATORY, ctx.identifier());
        }

        @Override
        public void exitOptional(OptionalContext ctx) {
            addRelationship(Relationship.RelationshipType.OPTIONAL, ctx.identifier());
        }

        @Override
        public void exitAlternative(AlternativeContext ctx) {
            addRelationship(Relationship.RelationshipType.ALTERNATIVE, ctx.identifier());
        }

        @Override
        public void exitOr(OrContext ctx) {
            addRelationship(Relationship.RelationshipType.OR, ctx.identifier());
        }

        @Override
        public void exitRequires(RequiresContext ctx) {
            addConstraint(Relationship.RelationshipType.REQUIRES, ctx.identifier());
        }

        @Override
        public void exitExcludes(ExcludesContext ctx) {
            addConstraint(Relationship.RelationshipType.EXCLUDES, ctx.identifier());
        }

        private void addRelationship(Relationship.RelationshipType type, List<IdentifierContext> ids) {
            String leftSide = ids.get(0).getText();
            ArrayList<String> rightSide = new ArrayList<>();
            for (int i = 1; i < ids.size(); i++) {
                rightSide.add(ids.get(i).getText());
            }

            featureModel.addRelationship(type,
                    leftSide,
                    rightSide.toArray(new String[0]));
        }

        private void addConstraint(Relationship.RelationshipType type, List<IdentifierContext> ids) {
            String leftSide = ids.get(0).getText();
            ArrayList<String> rightSide = new ArrayList<>();
            for (int i = 1; i < ids.size(); i++) {
                rightSide.add(ids.get(i).getText());
            }

            featureModel.addConstraint(type,
                    leftSide,
                    rightSide.toArray(new String[0]));
        }
    }
}
