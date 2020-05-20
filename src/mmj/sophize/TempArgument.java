package mmj.sophize;

import mmj.lang.ParseTree;
import mmj.lang.Stmt;
import mmj.lang.Theorem;
import mmj.lang.Var;
import mmj.verify.ProofDerivationStepEntry;
import org.sophize.datamodel.Argument;
import org.sophize.datamodel.Language;
import org.sophize.datamodel.MetaLanguage;

import java.util.*;
import java.util.stream.Collectors;

import static mmj.sophize.Helpers.*;

class TempArgument {
  // This tag is used by Sophize to detect that the text is in "argument-table" format and not the
  // usual metamath language.
  private static final String ARGUMENT_PREFIX = "ARGUMENT";
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

  Argument getArgument(Map<String, TempProposition> propositionData) {
    List<String> statementLines =
        new ArrayList<>(
            Arrays.asList(ARGUMENT_PREFIX, getDummyVariablesHeader(), ARGUMENT_TABLE_HEADER));
    Set<String> premises = new HashSet<>();
    try {
      for (int i = 0; i < proofSteps.size(); i++) {
        ProofDerivationStepEntry step = proofSteps.get(i);
        String stepLabel = getCorrectedPropReference(step.refLabel);
        String[] stepParts = {
          Integer.toString(i + 1),
          getHypString(step.hypStep),
          (step.isHyp) ? stepLabel : getPropLink(stepLabel),
          proofSteps.get(i).formula.toString()
        };
        statementLines.add("| " + String.join(" | ", stepParts) + " |");

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
    argument.setMetaLanguage(MetaLanguage.METAMATH);
    argument.setLanguage(Language.METAMATH_SET_MM);
    argument.setPremises(premises.toArray(String[]::new));
    String conclusionLabel = getCorrectedPropReference(stmt.getLabel());
    argument.setConclusion("#P_" + conclusionLabel);
    argument.setLookupTerms(getLookupTerms().toArray(String[]::new));

    String argumentText = String.join("\n", statementLines);
    if (!conclusionLabel.equals(stmt.getLabel()))
      argumentText += "\n\\\n" + toResourceRemark(stmt.getDescription());
    argument.setArgumentText(argumentText);

    return argument;
  }

  List<String> getLookupTerms() {
    List<String> lookupTerms =
        dummyVariables.stream()
            .map(var -> Helpers.varToLookupTerm(var.getActiveVarHyp()))
            .collect(Collectors.toCollection(ArrayList::new));
    for (ParseTree step : parsedSteps) {
      lookupTerms.addAll(getLookupTermsForParseNode(step.getRoot()));
    }
    return lookupTerms;
  }

  private String getCorrectedPropReference(String reference) {
    String original = alternateToOriginalStmt.getOrDefault(reference, reference);
    return dedupedPropsIdMap.getOrDefault(original, original);
  }

  private String getDummyVariablesHeader() {
    if (!(stmt instanceof Theorem)) return "";
    if (dummyVariables == null || dummyVariables.isEmpty()) return "";
    return dummyVariables.stream().map(Var::getId).collect(Collectors.joining(" ")) + "\n";
  }

  private static String getHypString(String[] hypStep) {
    String[] updated = new String[hypStep.length];
    for (int i = 0; i < hypStep.length; i++) {
      updated[i] = String.valueOf(Integer.valueOf(hypStep[i]) + 1);
    }
    return String.join(", ", updated);
  }

  private String getPropLink(String assignableId) {
    // Escape the '\' so that it doesn't get mixes with the table dividers.
    return "#P_" + assignableId;
  }
}
