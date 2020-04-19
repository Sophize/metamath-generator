package mmj.sophize;

import mmj.lang.*;
import mmj.verify.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Sophize {
  private static Map<String, String> latexdefMap;
  private static final String OUTPUT_DIRECTORY = "output";
  private static final Set<String> termDefiningAxiomTypecodes =
          new HashSet<>(Arrays.asList("class", "wff", "setvar"));

  private static final Map<String, Term> termData = new HashMap();
  private static final Map<String, Proposition> propositionData = new HashMap();
  private static final Map<String, Argument> argumentData = new HashMap();
  private static final Map<String, List<Var>> dummyVariablesMap = new HashMap();
  private static Beliefset defaultBeliefset = null;
  private static Cnst isProvable = null;
  private static final int seqLimit = 414000;

  public static void createAndWriteResources(Grammar grammar) throws IOException,
          ClassNotFoundException {
    isProvable = grammar.stmtTbl.get("retancl").getFormula().getTyp();
    readLatexMap();

    createTerms(grammar);
    createPropositions(grammar);
    createArguments(grammar);
    defaultBeliefset = getDefaultBeliefset(grammar);

    writeResources();
  }

  private static void writeResources() throws IOException {
    String directory = getDirectory(Term.class);
    for (Map.Entry<String, Term> entry : termData.entrySet()) {
      ResourceUtils.writeJson(directory, entry.getKey(), entry.getValue());
    }

    directory = getDirectory(Proposition.class);
    for (Map.Entry<String, Proposition> entry : propositionData.entrySet()) {
      ResourceUtils.writeJson(directory, entry.getKey(), entry.getValue());
    }

    directory = getDirectory(Argument.class);
    for (Map.Entry<String, Argument> entry : argumentData.entrySet()) {
      ResourceUtils.writeJson(directory, entry.getKey(), entry.getValue());
    }

    directory = getDirectory(Beliefset.class);
    ResourceUtils.writeJson(directory, "default", defaultBeliefset);
  }

  private static String getDirectory(Class tClass) throws IOException {
    Path dirPath = Paths.get(OUTPUT_DIRECTORY, tClass.getSimpleName().toLowerCase());
    Files.createDirectories(dirPath);
    return dirPath.toString();
  }

  private static Beliefset getDefaultBeliefset(Grammar grammar) {
    Beliefset beliefset = new Beliefset();
    beliefset.setNames(new String[]{"set.mm"});
    List<String> axioms = new ArrayList<>();
    for (Map.Entry<String, Stmt> entry : grammar.stmtTbl.entrySet()) {
      Stmt stmt = entry.getValue();
      if (stmt.getSeq() > seqLimit) continue;
      if (!(stmt instanceof Axiom)) continue;
      if (!stmt.getTyp().getId().equals("|-")) continue;
      myAssert(propositionData.containsKey(stmt.getLabel()));
      axioms.add("#P_" + stmt.getLabel());
    }
    beliefset.setUnsupportedPropositionPtrs(axioms.toArray(String[]::new));
    return beliefset;
  }

  private static void createArguments(Grammar grammar) {
    Cnst isProvable = grammar.stmtTbl.get("retancl").getFormula().getTyp();
    // Below maybe wrong! Hyps should be based on the context of the formula, not all hyps.
    Hyp[] hyps =
            grammar.stmtTbl.values().stream().filter(s -> s instanceof Hyp).toArray(Hyp[]::new);

    String argumentTableHeader = "| Step | Hyp | Ref | Expression |\n|---|---|---|---|\n";

    for (Stmt stmt : grammar.stmtTbl.values()) {
      if (stmt.getSeq() > seqLimit) continue;
      if (stmt.getLabel().equals("bj-0")) {
        // mmj.verify.VerifyException: E-PR-0020 Theorem bj-0: Step 5: Proof Worksheet generation
        // halted because theorem has invalid proof? No proof steps created for Proof Worksheet!
        continue;
      }
      if (!(stmt instanceof Theorem)) {
        continue;
      }
      StringBuilder argumentStatement =
              new StringBuilder(getDummyVariablesHeader(stmt) + argumentTableHeader);
      Set<String> premises = new HashSet<>();
      try {
        List<ProofDerivationStepEntry> proofSteps =
                new VerifyProofs()
                        .getProofDerivationSteps((Theorem) stmt, true, HypsOrder.Correct,
                                                 isProvable);
        for (int i = 0; i < proofSteps.size(); i++) {
          ProofDerivationStepEntry step = proofSteps.get(i);
          String label = step.refLabel;
          argumentStatement.append("| ").append(i + 1).append(" | ");
          argumentStatement.append(getHypString(step.hypStep)).append(" | "); // Do +1
          if (step.isHyp) {
            argumentStatement.append(label).append(" | ");
          } else {
            argumentStatement.append(getPropNavLinkWithDisplayPhrase(label, label)).append(" | ");
          }
          argumentStatement.append(getStatement(step, grammar, hyps, stmt.getSeq())).append(" |\n");

          if (!step.isHyp) {
            if (propositionData.containsKey(label)) {
              premises.add("#P_" + label);
            }
            myAssert(termData.containsKey(label));
          }
        }
      } catch (Exception e) {
        System.out.println(e.toString());
      }
      Argument argument = new Argument();
      argument.setArgumentText(argumentStatement.toString());
      argument.setConclusion("#P_" + stmt.getLabel());
      argument.setPremises(premises.toArray(String[]::new));
      argumentData.put(stmt.getLabel(), argument);
    }
  }

  private static String getDummyVariablesHeader(Stmt stmt) {
    if (!(stmt instanceof Theorem)) return "";
    List<Var> dummyVariables = dummyVariablesMap.get(stmt.getLabel());
    if (dummyVariables == null || dummyVariables.isEmpty()) return "";
    String dummyVariableString =
            dummyVariables.stream()
                    .map(var -> varToString(var.getActiveVarHyp()))
                    .collect(Collectors.joining(" &nbsp;"));
    return "*#(T_dummy_variable, 'Dummy variables')* "
            + dummyVariableString
            + " *are mutually distinct and distinct from all other variables.*\n\n";
  }

  private static String getHypString(String[] hypStep) {
    String[] updated = new String[hypStep.length];
    for (int i = 0; i < hypStep.length; i++) {
      updated[i] = String.valueOf(Integer.valueOf(hypStep[i]) + 1);
    }
    return String.join(", ", updated);
  }

  private static String getStatement(
          ProofDerivationStepEntry step, Grammar grammar, Hyp[] hyps, int seq) {
    ParseTree tree = step.formulaParseTree;
    if (tree == null) {
      tree = grammar.parseFormulaWithoutSafetyNet(step.formula, hyps, seq);
    }
    return getStatement(tree, "\\vdash");
  }

  private static int getNumConstChars(Stmt wffStatement) {
    int numChars = 0;
    for (Sym sym : wffStatement.getFormula().getSym()) {
      if (sym.getId().equals("(") || sym.getId().equals(")")) continue;
      if (sym instanceof Var) continue;
      numChars++;
    }
    return numChars - 1; // -1 to because we don't want to count the typecode.
  }

  private static boolean isTermDefiningStatement(Stmt stmt) {
    Sym[] formula = stmt.getFormula().getSym();
    boolean isSyntax = termDefiningAxiomTypecodes.contains(formula[0].getId());
    if (!isSyntax) return false;
    myAssert(stmt instanceof VarHyp || stmt instanceof Axiom || stmt instanceof Theorem);
    return true;
  }

  private static void createTerms(Grammar grammar) {
    List<Stmt> orderedStmts =
            grammar.stmtTbl.values().stream()
                    .sorted(Comparator.comparing(Stmt::getSeq))
                    .collect(Collectors.toList());

    termData.put("wff", getPrimitiveMetamathTerm("wff", "A well formed formula"));
    termData.put(
            "class",
            getPrimitiveMetamathTerm(
                    "class",
                    "An expression that is a syntactically valid class expression. "
                            +
                            "All valid set expressions are also valid class expression, so " +
                            "expressions"
                            + "of sets normally have the class typecode. Use the class typecode, "
                            +
                            "not the setvar typecode, for the type of set expressions unless you " +
                            "are "
                            + "specifically identifying a single set variable."));
    termData.put(
            "setvar",
            getPrimitiveMetamathTerm(
                    "setvar",
                    "Individual set variable type. Note that this is not the type of an arbitrary "
                            +
                            "set expression, instead, it is used to ensure that there is only a " +
                            "single "
                            +
                            "symbol used after quantifiers like for-all (∀) and there-exists (∃)" +
                            "."));
    for (int i = 0; i < orderedStmts.size(); i++) {
      Stmt stmt = orderedStmts.get(i);
      if (stmt.getSeq() > seqLimit) continue;

      if (!isTermDefiningStatement(stmt)) continue;
      List<Term> newTerms = new ArrayList<>();
      if (stmt instanceof VarHyp) {
        String typecode = stmt.getFormula().getSym()[0].getId();
        String varId = stmt.getFormula().getSym()[1].getId();
        String definition = "A " + getTermWithDisplayPhrase(typecode, typecode) + " variable.";
        newTerms.add(getMetamathTerm(varId, definition, stmt.getLabel()));
      } else {
        myAssert(stmt instanceof Axiom || stmt instanceof Theorem);

        String definition = getStatement(stmt.getExprParseTree(), "wff");
        Sym[] formula = stmt.getFormula().getSym();
        for (int j = 1; j < formula.length; j++) { // skip typecode
          Sym sym = formula[j];
          if (!(sym instanceof Cnst)) continue;
          if (sym.getId().equals("(") || sym.getId().equals(")")) continue;
          newTerms.add(getMetamathTerm(sym.getId(), definition, stmt.getLabel()));
        }
      }

      for (int newTermIndex = 0; newTermIndex < newTerms.size(); newTermIndex++) {
        Term newTerm = newTerms.get(newTermIndex);
        Stmt definitionStmt = null;
        if (!(stmt instanceof VarHyp)) {
          definitionStmt = findDefinition(orderedStmts, i + 1, newTerm.getPhrase());
        }
        String assignableId =
                getAssignableIdForTermInStmt(stmt.getLabel(), newTermIndex, newTerms.size());
        String remarks = getRemarks(stmt.getDescription());
        if (definitionStmt != null) {
          String definitionLink = "#P_" + definitionStmt.getLabel() + "|NAV_LINK|HIDE_TVI";
          remarks +=
                  "\n\n---\n\nThis syntax is primitive. The first axiom using it is "
                          + definitionLink
                          + ".\n\n---";
        }
        newTerm.setRemarks(remarks);
        myAssert(!termData.containsKey(assignableId));
        termData.put(assignableId, newTerm);
      }
    }

    for (Term term : termData.values()) {
      // HACK: use latex-ed phrase till we get METAMATH frontend support.
      String latexReplacement = latexdefMap.get(term.getPhrase());
      myAssert(latexReplacement != null);
      term.setPhrase("$" + latexReplacement + "$");
    }
  }

  private static Term getMetamathTerm(String phrase, String definition, String stmtLabel) {
    Term term = new Term();
    term.setLanguage(Language.METAMATH_SET_MM);
    term.setMetaLanguage(MetaLanguage.METAMATH);
    term.setPhrase(phrase);
    term.setDefinition(definition);
    if (stmtLabel != null) {
      term.setCitations(new Citation[]{getCitation(stmtLabel)});
    }
    return term;
  }

  private static Term getPrimitiveMetamathTerm(String phrase, String remarks) {
    Term term = getMetamathTerm(phrase, "", null);
    term.setPrimitive(true);
    term.setRemarks(remarks);
    Citation bookCitation = new Citation();
    bookCitation.setTextCitation(
            "Section 4.2.5 of [metamath book](http://us.metamath.org/#book) p.125");
    term.setCitations(new Citation[]{bookCitation});
    return term;
  }

  private static String getTermWithDisplayPhrase(String assignableId, String displayPhrase) {
    return "#(T_" + assignableId + ", '" + displayPhrase + "')";
  }

  private static String getPropNavLinkWithDisplayPhrase(String assignableId, String displayPhrase) {
    return "#(P_" + assignableId + ", '" + displayPhrase + "'\\|HIDE_TVI\\|NAV_LINK)";
  }

  private static Stmt findDefinition(List<Stmt> orderedStmts, int startingIndex, String symId) {
    for (int i = startingIndex; i < orderedStmts.size(); i++) {
      Stmt stmt = orderedStmts.get(i);
      if (!(stmt instanceof Axiom)) {
        continue;
      }
      Sym[] formula = stmt.getFormula().getSym();
      if (!formula[0].getId().equals("|-")) continue;
      for (Sym formulaSym : formula) {
        if (formulaSym.getId().equals(symId)) {
          return stmt;
        }
      }
    }
    return null;
  }

  private static String getAssignableIdForTermInStmt(
          String stmtLabel, int termIndex, int totalTerm) {
    return (totalTerm > 1) ? (stmtLabel + "." + (termIndex + 1)) : stmtLabel;
  }

  private static void readLatexMap() throws IOException, ClassNotFoundException {
    File file = new File("latexdef_map");
    FileInputStream f = new FileInputStream(file);
    ObjectInputStream s = new ObjectInputStream(f);
    latexdefMap = (HashMap<String, String>) s.readObject();
    s.close();
  }

  private static void createPropositions(Grammar grammar) {
    for (Stmt stmt : grammar.stmtTbl.values()) {
      if (!(stmt instanceof Assrt)) {
        myAssert(stmt instanceof Hyp);
        continue;
      }
      if (stmt.getSeq() > seqLimit) continue;
      if (!stmt.getFormula().getSym()[0].getId().equals("|-")) continue;
      createProposition((Assrt) stmt, grammar);
    }
  }

  private static void createProposition(Assrt assrt, Grammar grammar) {
    String statement = "";
    LogHyp[] hypothesisList = assrt.getLogHypArray();
    boolean singleHypothesis = hypothesisList != null && hypothesisList.length == 1;
    boolean multipleHypotheses = hypothesisList != null && hypothesisList.length > 1;
    if (singleHypothesis) {
      statement += "*If* " + getStatement(hypothesisList[0]) + "\n";
    } else if (multipleHypotheses) {
      statement += "*Given the following hypotheses:*\\\n";
      for (int i = 0; i < hypothesisList.length; i++) {
        LogHyp hypothesis = hypothesisList[i];
        String hypStatement = getStatement(hypothesis);
        if (i != hypothesisList.length - 1) hypStatement += "\\";
        statement += hypStatement + "\n";
      }
    }

    if (singleHypothesis) {
      statement += "\n*then,* ";
    } else if (multipleHypotheses) {
      statement += "\n\n*we can assert that:*\\\n";
    }
    statement += getStatement(assrt) + getDistinctAndSaveDummyVariables(assrt, grammar);

    String label = assrt.getLabel();

    Proposition proposition = new Proposition();
    proposition.setMetaLanguage(MetaLanguage.METAMATH);
    proposition.setLanguage(Language.METAMATH_SET_MM);
    proposition.setRemarks(assrt.getDescription());
    proposition.setStatement(statement);
    proposition.setNames(new String[]{label});

    proposition.setCitations(new Citation[]{getCitation(label)});

    propositionData.put(label, proposition);
  }

  private static String getDistinctAndSaveDummyVariables(Assrt assrt, Grammar grammar) {
    Set<String> idsInProp = new HashSet<>();
    Set<String> dummyVariableIds = new HashSet<>();

    final DjVars[] comboDvArray =
            DjVars.sortAndCombineDvArrays(
                    assrt.getMandFrame().djVarsArray,
                    (assrt instanceof Theorem) ? ((Theorem) assrt).getOptFrame().djVarsArray :
                            null);

    addVarToSet(assrt.getFormula(), idsInProp);
    Arrays.stream(assrt.getLogHypArray()).forEach(hyp -> addVarToSet(hyp.getFormula(), idsInProp));

    if (assrt instanceof Theorem) {
      List<ProofDerivationStepEntry> proofSteps;
      try {
        proofSteps =
                new VerifyProofs()
                        .getProofDerivationSteps((Theorem) assrt, true, HypsOrder.Correct,
                                                 isProvable);
      } catch (VerifyException e) {
        proofSteps = new ArrayList<>();
      }

      proofSteps.stream().forEach(proofStep -> addVarToSet(proofStep.formula, dummyVariableIds));
    }
    dummyVariableIds.removeAll(idsInProp);
    dummyVariablesMap.put(
            assrt.getLabel(),
            dummyVariableIds.stream()
                    .map(id -> (Var) grammar.symTbl.get(id))
                    .sorted(Comparator.comparing(Var::getId))
                    .collect(Collectors.toList()));

    final List<List<Var>> comboDvGroups = ScopeFrame.consolidateDvGroups(comboDvArray);
    String distinctVariables =
            comboDvGroups.stream()
                    .map(
                            dvGroup ->
                                    dvGroup.stream()
                                            .filter(var -> idsInProp.contains(var.getId()))
                                            .sorted(Comparator.comparing(Var::getId))
                                            .collect(Collectors.toList()))
                    .filter(usable -> usable.size() > 1)
                    .map(
                            dvGroup ->
                                    dvGroup.stream()
                                            .map(var -> varToString(var.getActiveVarHyp()))
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

  private static void addVarToSet(Formula formula, Set<String> varIdSet) {
    Arrays.stream(formula.getSym())
            .filter(sym -> sym instanceof Var)
            .forEach(sym -> varIdSet.add(sym.getId()));
  }

  private static String getStatement(Stmt stmt) {
    return "$\\scriptsize \\color{#999}"
            + stmt.getLabel()
            + "$ "
            + getStatement(stmt.getExprParseTree(), latexdefMap.get(stmt.getTyp().getId()));
  }

  private static Citation getCitation(String label) {
    Citation citation = new Citation();
    String citationText = "See " + label + " on Metamath";
    String citationLink = "http://us.metamath.org/mpegif/" + label + ".html";
    citation.setTextCitation("[" + citationText + "](" + citationLink + ")");
    return citation;
  }

  private static String getStatement(ParseTree tree, String typeCodeLatex) {
    ParseNode root = tree.getRoot();
    return "$\\scriptsize \\color{#999}"
            + typeCodeLatex
            + "$ "
            + getStatement(root).replace("$$", "");
  }

  private static final Map<String, String> typeToColorCode =
          Map.of(
                  "wff", "\\color{blue}",
                  "class", "\\color{#C3C}",
                  "setvar", "\\color{red}");

  private static String varToString(VarHyp hyp) {
    Cnst type = hyp.getTyp();
    Var var = (Var) hyp.getFormula().getSym()[1];
    String displayPhrase =
            "$" + typeToColorCode.get(type.getId()) + latexdefMap.get(var.getId()) + "$";
    return getTermWithDisplayPhrase(hyp.getLabel(), displayPhrase);
  }

  private static String getStatement(ParseNode node) {
    List<String> childNodes = new ArrayList<>();
    for (ParseNode childNode : node.child) {
      childNodes.add(getStatement(childNode));
    }

    Sym[] formula = node.stmt.getFormula().getSym();

    if (node.stmt instanceof VarHyp) return varToString((VarHyp) node.stmt);

    VarHyp[] varHypArray = node.stmt.getMandVarHypArray();
    /// TODO: check comment in ParseTree.java
    myAssert(childNodes.size() == varHypArray.length);

    /*
    Using VarHyp as key doesn't work in some cases: for example:
    In wcel, symbols refer to activeVarHyp - cA and cB
    But in varHypArray has wcel.cA and wcel.cB
    */
    Map<String, Integer> varIdToChildIndex = new HashMap<>();
    for (int i = 0; i < varHypArray.length; i++) {
      varIdToChildIndex.put(varHypArray[i].getFormula().getSym()[1].getId(), i);
    }
    int numConstItems = getNumConstChars(node.stmt);

    int constItemIndex = 0;
    StringBuilder builder = new StringBuilder();
    for (int symIndex = 1; symIndex < formula.length; symIndex++) {
      Sym sym = formula[symIndex];
      String id = sym.getId();
      if (sym instanceof Cnst) {
        if (id.equals("(")) {
          builder.append("$($");
          continue;
        }
        if (id.equals(")")) {
          builder.append("$)$");
          continue;
        }
        myAssert(latexdefMap.containsKey(id));
        String latexString = "$" + latexdefMap.get(id) + "$";
        String assignableId =
                getAssignableIdForTermInStmt(node.stmt.getLabel(), constItemIndex, numConstItems);
        builder.append(getTermWithDisplayPhrase(assignableId, latexString));
        constItemIndex++;
      } else {
        myAssert(sym instanceof Var);
        if (childNodes.size() == 0) {
          builder.append("$" + latexdefMap.get(id) + "$");
        } else {
          Integer index = varIdToChildIndex.get(id);
          myAssert(index != null);
          builder.append(childNodes.get(index));
        }
      }
    }
    return builder.toString().trim();
  }

  private static String getRemarks(String description) {
    if (description == null) return "";
    String withoutNewlines = description.replaceAll("\\n\\s+", " ");
    return withoutNewlines;
    // TODO: labels be preceded with a tilde (~) and math symbol tokens be enclosed in grave
    // accents (` `)
  }

  private static void myAssert(boolean val) {
    if (!val) {
      throw new IllegalStateException("val");
    }
  }
}
