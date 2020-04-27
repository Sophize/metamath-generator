package mmj.sophize;

import mmj.gmff.GMFFManager;
import mmj.lang.*;
import mmj.sophize.ioutils.Beliefset;
import mmj.verify.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static mmj.sophize.Helpers.*;

public class ResourceStore {
  static Map<String, String> latexdefMap;
  private static final List<String> TERM_DEFINING_AXIOM_TYPECODES =
      Arrays.asList("class", "wff", "setvar");
  private static Cnst isProvable = null;
  private static final int seqLimit = 421000; // 421000

  // Statement label (used to create the resource) to resources.
  final Map<String, TempTerm> termData = new HashMap<>();
  final Map<String, TempProposition> propositionData = new TreeMap<>();
  final Map<String, TempArgument> argumentData = new HashMap<>();

  // Statement(argument) label to dummy variables used in the argument.
  private final Map<String, List<Var>> dummyVariablesMap = new HashMap<>();

  // All axioms (with typecode |-) in the Grammar.
  Beliefset defaultBeliefset = null;

  private final Map<String, String> alternateToOriginalStmt = new HashMap<>();
  private final String databaseName;

  ResourceStore(String databaseName) {
    latexdefMap = GMFFManager.LATEXDEF_MAP;
    this.databaseName = databaseName;
  }

  String getDedupPostfix() {
    return Helpers.DEDUP_POSTFIX + this.databaseName.charAt(0);
  }

  void createResources(Grammar grammar) {
    isProvable = grammar.getProvableLogicStmtTypArray()[0];
    List<Stmt> orderedStmts =
        grammar.stmtTbl.values().stream()
            .sorted(Comparator.comparing(Stmt::getSeq))
            .collect(toList());

    addTerms(orderedStmts);
    addPropositions(orderedStmts, grammar.symTbl);
    addArguments(orderedStmts, grammar);
    defaultBeliefset = getDefaultBeliefset(grammar, databaseName, propositionData);
  }

  private void addTerms(List<Stmt> orderedStmts) {
    for (int i = 0; i < orderedStmts.size(); i++) {
      Stmt stmt = orderedStmts.get(i);
      if (stmt.getSeq() > seqLimit) continue;
      addTermsFromStmt(stmt, orderedStmts, i);
    }
  }

  private void addTermsFromStmt(Stmt stmt, List<Stmt> orderedStmts, int stmtIndex) {
    if (!isTermDefiningStatement(stmt)) return;
    if (stmt instanceof VarHyp) {
      TempTerm term = new TempTerm(stmt, null, null, "");
      String assignableId = term.getAssignableId();
      myAssert(!termData.containsKey(assignableId));
      termData.put(assignableId, term);
      return;
    }
    myAssert(stmt instanceof Axiom || stmt instanceof Theorem);
    long numConstChars = getNumConstChars(stmt);
    int constIndex = 0;
    Sym[] formula = stmt.getFormula().getSym();
    for (int i = 1; i < formula.length; i++) { // skip typecode
      Sym sym = formula[i];
      if (sym instanceof Var) continue;
      if (sym.getId().equals("(") || sym.getId().equals(")")) continue;
      Stmt defn = findDefinition(orderedStmts, stmtIndex + 1, sym);
      String assignableId =
          getAssignableIdForTermInStmt(stmt.getLabel(), constIndex, numConstChars);

      TempTerm term = new TempTerm(stmt, defn, sym, assignableId);

      myAssert(!termData.containsKey(assignableId));
      termData.put(assignableId, term);
      constIndex++;
    }
    myAssert(constIndex == numConstChars);
  }

  private void addPropositions(List<Stmt> orderedStmts, Map<String, Sym> symTbl) {
    for (Stmt stmt : orderedStmts) {
      Assrt assrt = getPropositionAssrt(stmt);
      String label = assrt.getLabel();
      boolean canBeUnprocessedDuplicate =
          (label.contains("ALT") || label.contains("OLD"))
              && !alternateToOriginalStmt.containsKey(label)
              && !alternateToOriginalStmt.values().contains(label);
      if (canBeUnprocessedDuplicate) {
        addDuplicatesToMap(assrt, orderedStmts);
      }
    }
    for (Stmt stmt : orderedStmts) {
      Assrt assrt = getPropositionAssrt(stmt);
      if (assrt == null) continue;
      String distinctVarString = getDistinctAndSaveDummyVariables(assrt, symTbl);
      if (!alternateToOriginalStmt.containsKey(assrt.getLabel())) {
        TempProposition prop = new TempProposition(assrt, distinctVarString);
        propositionData.put(assrt.getLabel(), prop);
      }
    }
  }

  private static Assrt getPropositionAssrt(Stmt stmt) {
    if (!(stmt instanceof Assrt)) return null;
    if (!stmt.getFormula().getSym()[0].getId().equals("|-")) return null;
    if (stmt.getSeq() > seqLimit) return null;
    return (Assrt) stmt;
  }

  private void addDuplicatesToMap(Assrt assrt, List<Stmt> orderedStmts) {
    // Some proofs don't have a non-ALT version. Eg. trsspwALT1, trsspwALT2, trsspwALT3
    Set<Assrt> duplicates = new HashSet<>();
    duplicates.add(assrt);
    for (Stmt candidate : orderedStmts) {
      if (candidate == assrt) continue;
      Assrt originalCandidate = getPropositionAssrt(candidate);
      if (originalCandidate != null && areSameProps(assrt, originalCandidate)) {
        duplicates.add(originalCandidate);
      }
    }
    if (duplicates.size() <= 1) return;
    Assrt original = findOriginal(duplicates);
    for (Assrt duplicate : duplicates) {
      if (duplicate == original) continue;
      putIfAbsent(alternateToOriginalStmt, assrt.getLabel(), duplicate.getLabel());
      for (int i = 0; i < duplicate.getLogHypArray().length; i++) {
        // assume that the order of hypothesis is the same in original and alternate.
        String alternateHypId = assrt.getLogHypArray()[i].getLabel();
        String originalHypId = duplicate.getLogHypArray()[i].getLabel();
        if (alternateHypId.equals(originalHypId)) continue;
        putIfAbsent(alternateToOriginalStmt, alternateHypId, originalHypId);
      }
    }
  }

  private static Assrt findOriginal(Set<Assrt> duplicates) {
    List<Assrt> sortedDuplicates =
        duplicates.stream().sorted(Comparator.comparing(Stmt::getLabel)).collect(toList());
    return sortedDuplicates.stream()
        .filter(assrt -> !assrt.getLabel().contains("ALT") && !assrt.getLabel().contains("OLD"))
        .findFirst()
        .orElse(sortedDuplicates.get(0));
  }

  private static Beliefset getDefaultBeliefset(
      Grammar grammar, String beliefsetName, Map<String, TempProposition> propositionData) {
    List<String> axioms = new ArrayList<>();
    for (Map.Entry<String, Stmt> entry : grammar.stmtTbl.entrySet()) {
      Stmt stmt = entry.getValue();
      if (stmt.getSeq() > seqLimit) continue;
      if (!(stmt instanceof Axiom)) continue;
      if (!stmt.getTyp().getId().equals("|-")) continue;
      myAssert(propositionData.containsKey(stmt.getLabel()));
      axioms.add("#P_" + stmt.getLabel());
    }
    Beliefset beliefset = new Beliefset();
    beliefset.setNames(new String[] {beliefsetName});
    beliefset.setUnsupportedPropositionPtrs(axioms.toArray(String[]::new));
    return beliefset;
  }

  private static boolean isTermDefiningStatement(Stmt stmt) {
    Sym[] formula = stmt.getFormula().getSym();
    if (!TERM_DEFINING_AXIOM_TYPECODES.contains(formula[0].getId())) return false;
    myAssert(stmt instanceof VarHyp || stmt instanceof Axiom || stmt instanceof Theorem);
    return true;
  }

  private String getDistinctAndSaveDummyVariables(Assrt assrt, Map<String, Sym> symTbl) {
    Set<String> idsInProp = new HashSet<>();
    Set<String> dummyVariableIds = new HashSet<>();

    final DjVars[] comboDvArray =
        DjVars.sortAndCombineDvArrays(
            assrt.getMandFrame().djVarsArray,
            (assrt instanceof Theorem) ? ((Theorem) assrt).getOptFrame().djVarsArray : null);

    addFormulaVarsToSet(assrt.getFormula(), idsInProp);
    Arrays.stream(assrt.getLogHypArray())
        .forEach(hyp -> addFormulaVarsToSet(hyp.getFormula(), idsInProp));

    if (assrt instanceof Theorem) {
      List<ProofDerivationStepEntry> proofSteps;
      try {
        proofSteps =
            new VerifyProofs()
                .getProofDerivationSteps((Theorem) assrt, true, HypsOrder.Correct, isProvable);
      } catch (VerifyException | ArrayIndexOutOfBoundsException e) {
        proofSteps = new ArrayList<>();
      }

      proofSteps.forEach(proofStep -> addFormulaVarsToSet(proofStep.formula, dummyVariableIds));
    }
    dummyVariableIds.removeAll(idsInProp);
    dummyVariablesMap.put(
        assrt.getLabel(),
        dummyVariableIds.stream()
            .map(id -> (Var) symTbl.get(id))
            .sorted(Comparator.comparing(Var::getId))
            .collect(toList()));

    final List<List<Var>> comboDvGroups = ScopeFrame.consolidateDvGroups(comboDvArray);
    String distinctVariables =
        comboDvGroups.stream()
            .map(
                dvGroup ->
                    dvGroup.stream()
                        .filter(var -> idsInProp.contains(var.getId()))
                        .sorted(Comparator.comparing(Var::getId))
                        .collect(toList()))
            .filter(dvGroup -> dvGroup.size() > 1)
            .map(
                dvGroup ->
                    dvGroup.stream()
                        .map(var -> varToString(var.getActiveVarHyp(), latexdefMap))
                        .collect(Collectors.joining(",")))
            .sorted()
            .collect(Collectors.joining(" &nbsp;&nbsp;"));
    if (distinctVariables == null || distinctVariables.isEmpty()) {
      return "";
    }
    return "\n\n*when the following groups of variables are #(T_distinct_variable, 'distinct' ):* "
        + "&nbsp;&nbsp;"
        + distinctVariables;
  }

  private void addArguments(List<Stmt> orderedStmts, Grammar grammar) {
    // Below maybe wrong! Hyps should be based on the context of the formula, not all hyps.
    Hyp[] hyps = orderedStmts.stream().filter(s -> s instanceof Hyp).toArray(Hyp[]::new);

    for (Stmt stmt : orderedStmts) {
      if (stmt.getSeq() > seqLimit) break;
      TempArgument argument = getArgument(stmt, hyps, grammar);
      if (argument != null) argumentData.put(stmt.getLabel(), argument);
    }
  }

  private TempArgument getArgument(Stmt stmt, Hyp[] hyps, Grammar grammar) {
    // mmj.verify.VerifyException: E-PR-0020 Theorem bj-0: Step 5: Proof Worksheet generation
    // halted because theorem has invalid proof? No proof steps created for Proof Worksheet!
    if (stmt.getLabel().equals("bj-0")) return null;
    if (!(stmt instanceof Theorem)) return null;
    try {
      List<ProofDerivationStepEntry> proofSteps =
          new VerifyProofs()
              .getProofDerivationSteps((Theorem) stmt, true, HypsOrder.Correct, isProvable);

      List<ParseTree> parsedSteps =
          proofSteps.stream()
              .map(
                  step -> {
                    if (step.formulaParseTree != null) return step.formulaParseTree;
                    return grammar.parseFormulaWithoutSafetyNet(step.formula, hyps, stmt.getSeq());
                  })
              .collect(toList());
      return new TempArgument(
          stmt,
          proofSteps,
          dummyVariablesMap.get(stmt.getLabel()),
          parsedSteps,
          termData,
          alternateToOriginalStmt);
    } catch (VerifyException | ArrayIndexOutOfBoundsException e) {
      System.out.println(e.toString());
      return null;
    }
  }

  private static void addFormulaVarsToSet(Formula formula, Set<String> varIdSet) {
    Arrays.stream(formula.getSym())
        .filter(sym -> sym instanceof Var)
        .forEach(sym -> varIdSet.add(sym.getId()));
  }
}
