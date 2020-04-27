package mmj.sophize;

import mmj.lang.ParseTree;
import mmj.lang.Stmt;
import mmj.lang.Theorem;
import mmj.lang.Var;
import mmj.sophize.ioutils.Argument;
import mmj.verify.ProofDerivationStepEntry;

import java.util.*;
import java.util.stream.Collectors;

import static mmj.sophize.Helpers.*;

class TempArgument {
  private static final String ARGUMENT_TABLE_HEADER =
      "| Step | Hyp | Ref | Expression |\n|---|---|---|---|\n";

  Stmt stmt;
  List<ProofDerivationStepEntry> proofSteps;
  List<Var> dummyVariables;
  List<ParseTree> parsedSteps;
  Map<String, TempTerm> termData;
  Map<String, String> alternateToOriginalStmt;

  Map<String, String> dedupedPropsIdMap = new HashMap<>();

  TempArgument(
      Stmt stmt,
      List<ProofDerivationStepEntry> proofSteps,
      List<Var> dummyVariables,
      List<ParseTree> parsedSteps,
      Map<String, TempTerm> termData,
      Map<String, String> alternateToOriginalStmt) {
    this.stmt = stmt;
    this.proofSteps = proofSteps;
    this.dummyVariables = dummyVariables;
    this.parsedSteps = parsedSteps;
    this.termData = termData;
    this.alternateToOriginalStmt = alternateToOriginalStmt;
  }

  void setDedupedPropsIdMap(Map<String, String> dedupedPropsIdMap) {
    this.dedupedPropsIdMap = dedupedPropsIdMap;
  }

  Argument getArgument(
      Map<String, String> latexdefMap, Map<String, TempProposition> propositionData) {
    StringBuilder argumentStatement =
        new StringBuilder(getDummyVariablesHeader(latexdefMap) + ARGUMENT_TABLE_HEADER);
    Set<String> premises = new HashSet<>();
    try {
      for (int i = 0; i < proofSteps.size(); i++) {
        ProofDerivationStepEntry step = proofSteps.get(i);
        String stepLabel = getCorrectedPropReference(step.refLabel);
        argumentStatement.append("| ").append(i + 1).append(" | ");
        argumentStatement.append(getHypString(step.hypStep)).append(" | ");
        if (step.isHyp) {
          argumentStatement.append(stepLabel).append(" | ");
        } else {
          argumentStatement
              .append(getPropNavLinkWithDisplayPhrase(stepLabel, stepLabel))
              .append(" | ");
        }
        argumentStatement
            .append(getStatementForParseTree(parsedSteps.get(i), "\\vdash", latexdefMap))
            .append(" |\n");

        if (!step.isHyp) {
          String premiseLabel = getCorrectedPropReference(stepLabel);
          if (propositionData.containsKey(premiseLabel)) premises.add("#P_" + premiseLabel);
          else myAssert(termData.containsKey(premiseLabel));
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println(e.toString());
    }
    Argument argument = new Argument();
    argument.setPremises(premises.toArray(String[]::new));
    String conclusionLabel = getCorrectedPropReference(stmt.getLabel());
    argument.setConclusion("#P_" + conclusionLabel);
    if (!conclusionLabel.equals(stmt.getLabel()))
      argumentStatement.append("\n\\\n").append(toResourceRemark(stmt.getDescription()));
    argument.setArgumentText(argumentStatement.toString());
    return argument;
  }

  private String getCorrectedPropReference(String reference) {
    String original = alternateToOriginalStmt.getOrDefault(reference, reference);
    return dedupedPropsIdMap.getOrDefault(original, original);
  }

  private String getDummyVariablesHeader(Map<String, String> latexdefMap) {
    if (!(stmt instanceof Theorem)) return "";
    if (dummyVariables == null || dummyVariables.isEmpty()) return "";
    String dummyVariableString =
        dummyVariables.stream()
            .map(var -> varToString(var.getActiveVarHyp(), latexdefMap))
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

  private  String getPropNavLinkWithDisplayPhrase(String assignableId, String displayPhrase) {
    // Escape the '\' so that it doesn't get mixes with the table dividers.
    return "#(P_" + assignableId + ", '" + displayPhrase + "'\\|HIDE_TVI\\|NAV_LINK)";
  }

}
