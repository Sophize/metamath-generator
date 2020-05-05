package mmj.sophize;

import mmj.lang.Assrt;
import mmj.lang.LogHyp;
import mmj.lang.Stmt;
import org.sophize.datamodel.Citation;
import org.sophize.datamodel.Language;
import org.sophize.datamodel.MetaLanguage;
import org.sophize.datamodel.Proposition;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static mmj.sophize.Helpers.*;

class TempProposition {
  List<Assrt> assrts;
  String distinctVarsString;

  TempProposition(Assrt assrt, String distinctVarsString) {
    this.assrts = Arrays.asList(assrt);
    this.distinctVarsString = distinctVarsString;
  }

  TempProposition(List<Assrt> assrts, String distinctVarsString) {
    this.assrts = assrts;
    this.distinctVarsString = distinctVarsString;
  }

  Proposition getProposition(Map<String, String> latexdefMap) {
    String label = assrts.get(0).getLabel();

    Proposition proposition = new Proposition();
    proposition.setMetaLanguage(MetaLanguage.METAMATH);
    proposition.setLanguage(Language.METAMATH_SET_MM);
    proposition.setRemarks(getRemarks());
    proposition.setStatement(
        getPropositionStatement(assrts.get(0), distinctVarsString, latexdefMap));

    String specialName = SPECIAL_THEOREM_NAMES.get(label);
    if (specialName != null) {
      proposition.setIndexable(true);
      proposition.setNames(new String[] {specialName, label});
    } else {
      proposition.setNames(new String[] {label});
    }

    proposition.setCitations(new Citation[] {getCitation(label)});
    return proposition;
  }

  private String getRemarks() {
    return TempTerm.combineIfNotIdentical(
        assrts.stream().map(assrt -> toResourceRemark(assrt.getDescription())).collect(toList()),
        "\n\n---\n\n");
  }

  static String getPropositionStatement(
      Assrt assrt, String distinctVarsString, Map<String, String> latexdefMap) {
    StringBuilder statement = new StringBuilder();
    LogHyp[] hypothesisList = assrt.getLogHypArray();
    boolean singleHypothesis = hypothesisList != null && hypothesisList.length == 1;
    boolean multipleHypotheses = hypothesisList != null && hypothesisList.length > 1;
    if (singleHypothesis) {
      statement
          .append("*If* ")
          .append(getPremiseOrConclusionStmt(hypothesisList[0], latexdefMap))
          .append("\n");
    } else if (multipleHypotheses) {
      statement.append("*Given the following hypotheses:*\\\n");
      for (int i = 0; i < hypothesisList.length; i++) {
        LogHyp hypothesis = hypothesisList[i];
        String hypStatement = getPremiseOrConclusionStmt(hypothesis, latexdefMap);
        if (i != hypothesisList.length - 1) hypStatement += "\\";
        statement.append(hypStatement).append("\n");
      }
    }

    if (singleHypothesis) {
      statement.append("\n*then,* ");
    } else if (multipleHypotheses) {
      statement.append("\n\n*we can assert that:*\\\n");
    }
    statement.append(getPremiseOrConclusionStmt(assrt, latexdefMap)).append(distinctVarsString);
    return statement.toString();
  }

  private static final Map<String, String> SPECIAL_THEOREM_NAMES = new HashMap<>();

  static {
    String fileName = "special_theorems";

    try {
      try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
        stream.forEach(
            line -> {
              String[] words = line.split(" ");
              int numLabels = Integer.parseInt(words[0]);
              List<String> labels = Arrays.stream(words).skip(1).limit(numLabels).collect(toList());
              // int metamath100Index = Integer.parseInt(words[labels.size() + 1]);
              String theoremNamePrefix =
                  String.join(" ", Arrays.copyOfRange(words, labels.size() + 2, words.length));

              for (int i = 0; i < labels.size(); i++) {
                String theoremPostFix = "";
                if (labels.size() > 1) theoremPostFix = " (" + (i + 1) + "/" + labels.size() + ")";
                String theoremName = theoremNamePrefix + theoremPostFix;
                SPECIAL_THEOREM_NAMES.put(labels.get(i), theoremName);
              }
            });
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String getPremiseOrConclusionStmt(Stmt stmt, Map<String, String> latexdefMap) {
    return "$\\scriptsize \\color{#999}"
        + stmt.getLabel()
        + "$ "
        + getStatementForParseTree(
            stmt.getExprParseTree(), latexdefMap.get(stmt.getTyp().getId()), latexdefMap);
  }
}
