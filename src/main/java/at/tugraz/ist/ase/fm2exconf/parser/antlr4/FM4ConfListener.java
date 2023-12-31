/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser.antlr4;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModelException;
import at.tugraz.ist.ase.fm2exconf.parser.ParserException;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * Generated by ANTLR4 library
 *
 * This interface defines a complete listener for a parse tree produced by
 * {@link FM4ConfParser}.
 */
public interface FM4ConfListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#model}.
	 * @param ctx the parse tree
	 */
	void enterModel(FM4ConfParser.ModelContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#model}.
	 * @param ctx the parse tree
	 */
	void exitModel(FM4ConfParser.ModelContext ctx);
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#fm4confver}.
	 * @param ctx the parse tree
	 */
	void enterFm4confver(FM4ConfParser.Fm4confverContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#fm4confver}.
	 * @param ctx the parse tree
	 */
	void exitFm4confver(FM4ConfParser.Fm4confverContext ctx);
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#modelname}.
	 * @param ctx the parse tree
	 */
	void enterModelname(FM4ConfParser.ModelnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#modelname}.
	 * @param ctx the parse tree
	 */
	void exitModelname(FM4ConfParser.ModelnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#feature}.
	 * @param ctx the parse tree
	 */
	void enterFeature(FM4ConfParser.FeatureContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#feature}.
	 * @param ctx the parse tree
	 */
	void exitFeature(FM4ConfParser.FeatureContext ctx) throws ParserException, FeatureModelException;
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#relationship}.
	 * @param ctx the parse tree
	 */
	void enterRelationship(FM4ConfParser.RelationshipContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#relationship}.
	 * @param ctx the parse tree
	 */
	void exitRelationship(FM4ConfParser.RelationshipContext ctx);
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#constraint}.
	 * @param ctx the parse tree
	 */
	void enterConstraint(FM4ConfParser.ConstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#constraint}.
	 * @param ctx the parse tree
	 */
	void exitConstraint(FM4ConfParser.ConstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link FM4ConfParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(FM4ConfParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link FM4ConfParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(FM4ConfParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mandatory}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void enterMandatory(FM4ConfParser.MandatoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mandatory}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void exitMandatory(FM4ConfParser.MandatoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code optional}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void enterOptional(FM4ConfParser.OptionalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code optional}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void exitOptional(FM4ConfParser.OptionalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alternative}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void enterAlternative(FM4ConfParser.AlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alternative}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void exitAlternative(FM4ConfParser.AlternativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code or}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void enterOr(FM4ConfParser.OrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code or}
	 * labeled alternative in {@link FM4ConfParser#relationshiprule}.
	 * @param ctx the parse tree
	 */
	void exitOr(FM4ConfParser.OrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code requires}
	 * labeled alternative in {@link FM4ConfParser#constraintrule}.
	 * @param ctx the parse tree
	 */
	void enterRequires(FM4ConfParser.RequiresContext ctx);
	/**
	 * Exit a parse tree produced by the {@code requires}
	 * labeled alternative in {@link FM4ConfParser#constraintrule}.
	 * @param ctx the parse tree
	 */
	void exitRequires(FM4ConfParser.RequiresContext ctx);
	/**
	 * Enter a parse tree produced by the {@code excludes}
	 * labeled alternative in {@link FM4ConfParser#constraintrule}.
	 * @param ctx the parse tree
	 */
	void enterExcludes(FM4ConfParser.ExcludesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code excludes}
	 * labeled alternative in {@link FM4ConfParser#constraintrule}.
	 * @param ctx the parse tree
	 */
	void exitExcludes(FM4ConfParser.ExcludesContext ctx);
}