package mmj.transforms;

import java.util.List;

import mmj.lang.*;
import mmj.pa.*;
import mmj.transforms.ImplicationInfo.ExtractImplResult;
import mmj.verify.VerifyProofs;

/**
 * This contains information for possible automatic transformations.
 * <p>
 * Canonical form for the parse node is a parse node with sorted
 * commutative/associative transformations.
 * <p>
 * The functions in this class are separated into 4 parts:
 * <ul>
 * <li>Data structures initialization
 * <li>Transformations
 * <li>Canonical form comparison
 * <li>Auxiliary functions
 * </ul>
 */
public class TransformationManager {

    public final boolean dbg;

    public final TrOutput output;

    /** It is necessary for formula construction */
    public final VerifyProofs verifyProofs;

    /** The symbol like |- in set.mm */
    public final Cnst provableLogicStmtTyp;

    /** The information about equivalence rules */
    public final EquivalenceInfo eqInfo;

    public final ImplicationInfo implInfo;

    public final ConjunctionInfo conjInfo;

    /** The information about replace rules */
    public final ReplaceInfo replInfo;

    /** The information about closure rules */
    public final ClosureInfo clInfo;

    public final AssociativeInfo assocInfo;

    public final CommutativeInfo comInfo;

    /**
     * Note: Here will be performed a lot of work during the construction of
     * this class!
     *
     * @param assrtList the list all library asserts
     * @param provableLogicStmtTyp this constant indicates
     *            "provable logic statement type"
     * @param messages the message manager
     * @param verifyProofs the proof verification is needed for some actions
     * @param debugOutput when it is true auto-transformation component will
     *            produce a lot of debug output
     */
    public TransformationManager(final List<Assrt> assrtList,
        final Cnst provableLogicStmtTyp, final Messages messages,
        final VerifyProofs verifyProofs, final boolean debugOutput)
    {
        output = new TrOutput(messages);
        this.verifyProofs = verifyProofs;
        this.provableLogicStmtTyp = provableLogicStmtTyp;
        dbg = debugOutput;

        eqInfo = new EquivalenceInfo(assrtList, output, dbg);

        implInfo = new ImplicationInfo(eqInfo, assrtList, output, dbg);

        conjInfo = new ConjunctionInfo(implInfo, assrtList, output, dbg);

        clInfo = new ClosureInfo(implInfo, conjInfo, assrtList, output, dbg);

        replInfo = new ReplaceInfo(eqInfo, implInfo, assrtList, output, dbg);

        assocInfo = new AssociativeInfo(eqInfo, clInfo, replInfo, conjInfo,
            implInfo, assrtList, output, dbg);

        comInfo = new CommutativeInfo(eqInfo, clInfo, conjInfo, implInfo,
            assrtList, output, dbg);
    }

    // ----------------------------

    // ------------------------------------------------------------------------
    // ---------------------Public transformation functions--------------------
    // ------------------------------------------------------------------------

    /**
     * The main function to create transformation.
     *
     * @param node the source node
     * @param info the information about previous steps
     * @return the transformation
     */
    public Transformation createTransformation(final ParseNode node,
        final WorksheetInfo info)
    {
        final Stmt stmt = node.getStmt();

        final boolean[] replAsserts = replInfo.getPossibleReplaces(stmt);

        boolean isCom = false;
        final GeneralizedStmt comProp = comInfo
            .getGenStmtForComNode(node, info);
        if (comProp != null)
            isCom = true;

        boolean isAssoc = false;
        final GeneralizedStmt assocProp = assocInfo.getGenStmtForAssocNode(
            node, info);
        if (assocProp != null)
            isAssoc = true;

        boolean isAssocCom = false;
        if (isAssoc)
            isAssocCom = comInfo.isComOp(assocProp);

        final boolean subTreesCouldBeRepl = replAsserts != null;

        if (!subTreesCouldBeRepl)
            return new IdentityTransformation(this, node);

        if (isAssocCom)
            // TODO: check the property!
            return new AssocComTransformation(this, node,
                AssocTree.createAssocTree(node, assocProp, info), assocProp);
        else if (isCom)
            return new CommutativeTransformation(this, node, comProp);

        else if (isAssoc)
            return new AssociativeTransformation(this, node,
                AssocTree.createAssocTree(node, assocProp, info), assocProp);
        else if (subTreesCouldBeRepl)
            return new ReplaceTransformation(this, node);

        // TODO: make the string constant!
        throw new IllegalStateException(
            "Error in createTransformation() algorithm");
    }
    public ParseNode getCanonicalForm(final ParseNode originalNode,
        final WorksheetInfo info)
    {
        return createTransformation(originalNode, info).getCanonicalNode(info);
    }

    // ------------------------------------------------------------------------
    // --------------------------Entry point part------------------------------
    // ------------------------------------------------------------------------

    private void performTransformation(final WorksheetInfo info,
        final ProofStepStmt source, final Assrt impl)
    {
        final Transformation dsTr = createTransformation(
            info.derivStep.formulaParseTree.getRoot(), info);
        final Transformation tr = createTransformation(
            source.formulaParseTree.getRoot(), info);

        final ProofStepStmt eqResult = tr.transformMeToTarget(dsTr, info);
        eqResult.toString();

        final boolean isNormalOrder = TrUtil.isVarNode(impl.getLogHypArray()[0]
            .getExprParseTree().getRoot());

        final ProofStepStmt[] hypDerivArray = isNormalOrder ? new ProofStepStmt[]{
                source, eqResult}
            : new ProofStepStmt[]{eqResult, source};

        info.finishDerivationStep(hypDerivArray, impl);
    }

    /**
     * Tries to unify the derivation step by some automatic transformations
     *
     * @param proofWorksheet the proof work sheet
     * @param derivStep the derivation step
     * @return true if it founds possible unification
     */
    private List<DerivationStep> tryToFindTransformationsCore(
        final ProofWorksheet proofWorksheet, final DerivationStep derivStep)
    {
        final WorksheetInfo info = new WorksheetInfo(proofWorksheet, derivStep,
            this);

        // If derivation step has form "prefix -> core", then this prefix could
        // be used in transformations
        final ExtractImplResult extrImplRes = implInfo
            .extractPrefixAndGetImplPart(info);
        if (extrImplRes != null)
            if (TrUtil.isVarNode(extrImplRes.implPrefix))
                // Now we support only simple "one-variable" prefixes
                info.setImplicationPrefix(extrImplRes.implPrefix,
                    extrImplRes.implStatement);

        final ParseNode derivRoot = derivStep.formulaParseTree.getRoot();
        final Cnst derivType = derivRoot.getStmt().getTyp();
        final Assrt implAssrt = implInfo.getEqImplication(derivType);
        if (implAssrt == null)
            return null;

        final Transformation dsTr = createTransformation(derivRoot, info);
        // Get canonical form for destination statement
        final ParseNode dsCanonicalForm = dsTr.getCanonicalNode(info);

        output.dbgMessage(dbg, "I-TR-DBG Step %s has canonical form: %s",
            derivStep, getFormula(dsCanonicalForm));

        for (final ProofWorkStmt proofWorkStmtObject : proofWorksheet
            .getProofWorkStmtList())
        {
            if (proofWorkStmtObject == derivStep)
                break;

            if (!(proofWorkStmtObject instanceof ProofStepStmt))
                continue;

            final ProofStepStmt candidate = (ProofStepStmt)proofWorkStmtObject;

            final ParseNode candCanon = getCanonicalForm(
                candidate.formulaParseTree.getRoot(), info);
            output.dbgMessage(dbg, "I-TR-DBG Step %s has canonical form: %s",
                candidate, getFormula(candCanon));

            // Compare canonical forms for destination and for candidate
            if (dsCanonicalForm.isDeepDup(candCanon)) {
                output.dbgMessage(dbg,
                    "I-TR-DBG found canonical forms correspondance: %s and %s",
                    candidate, derivStep);
                performTransformation(info, candidate, implAssrt);

                return info.newSteps;
            }
        }

        // Maybe it is closure assertion? Then we could automatically prove it!
        // TODO: Now the used algorithm could consume a lot of time for the
        // search!
        if (clInfo.performClosureTransformation(info))
            return info.newSteps;

        return null;
    }

    /**
     * The main entry point transformation function. This function tries to find
     * the transformation which leads to the derivation step from earlier steps.
     *
     * @param proofWorksheet the proof work sheet
     * @param derivStep the
     * @return the list of generated steps (and also derivStep) or null if the
     *         transformation was not found.
     */
    public List<DerivationStep> tryToFindTransformations(
        final ProofWorksheet proofWorksheet, final DerivationStep derivStep)
    {
        try {
            return tryToFindTransformationsCore(proofWorksheet, derivStep);
        } catch (final Throwable e) {
            // TODO: make string error constant!
            if (dbg)
                e.printStackTrace();

            output.errorMessage(TrConstants.ERRMSG_UNEXPECTED_EXCEPTION,
                e.toString());
            return null;
        }
    }
    // ------------------------------------------------------------------------
    // ------------------------Debug functions---------------------------------
    // ------------------------------------------------------------------------

    /**
     * This function is needed for debug
     *
     * @param node the input node
     * @return the corresponding formula
     */
    protected Formula getFormula(final ParseNode node) {
        final ParseTree tree = new ParseTree(node);
        final Formula generatedFormula = verifyProofs.convertRPNToFormula(
            tree.convertToRPN(), "tree"); // TODO: use constant
        return generatedFormula;
    }
}
