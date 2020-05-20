package mmj.sophize;

import mmj.lang.Assrt;
import mmj.lang.LogHyp;
import mmj.lang.Stmt;
import mmj.lang.Var;
import org.sophize.datamodel.Citation;
import org.sophize.datamodel.Language;
import org.sophize.datamodel.MetaLanguage;
import org.sophize.datamodel.Proposition;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static mmj.sophize.Helpers.*;

class TempProposition {
  List<Assrt> assrts;
  List<List<Var>> distinctVars;

  TempProposition(Assrt assrt, List<List<Var>> distinctVars) {
    this.assrts = Arrays.asList(assrt);
    this.distinctVars = distinctVars;
  }

  TempProposition(List<Assrt> assrts, List<List<Var>> distinctVars) {
    this.assrts = assrts;
    this.distinctVars = distinctVars;
  }

  Proposition getProposition() {
    String label = primaryAssrt().getLabel();

    Proposition proposition = new Proposition();
    proposition.setMetaLanguage(MetaLanguage.METAMATH);
    proposition.setLanguage(Language.METAMATH_SET_MM);
    proposition.setRemarks(getRemarks());
    proposition.setStatement(getPropositionStatement());
    proposition.setLookupTerms(getLookupTerms().toArray(String[]::new));

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

  Assrt primaryAssrt() {
    return assrts.get(0);
  }

  String distinctVarsStatement() {
    return distinctVars.stream()
        .map(group -> group.stream().map(Var::getId).collect(Collectors.joining(" ", "$d ", " $.")))
        .collect(Collectors.joining("\n"));
  }

  List<String> getLookupTerms() {
    List<String> lookupTerms = new ArrayList<>();
    Assrt assrt = primaryAssrt();
    for (LogHyp hyp : assrt.getLogHypArray()) {
      lookupTerms.addAll(getLookupTerms(hyp));
    }
    lookupTerms.addAll(getLookupTerms(assrt));

    lookupTerms.addAll(
        distinctVars.stream()
            .flatMap(List::stream)
            .map(var -> Helpers.varToLookupTerm(var.getActiveVarHyp()))
            .collect(Collectors.toList()));
    return lookupTerms;
  }

  private String getRemarks() {
    return TempTerm.combineIfNotIdentical(
        assrts.stream().map(assrt -> toResourceRemark(assrt.getDescription())).collect(toList()),
        "\n\n---\n\n");
  }

  private String getPropositionStatement() {
    Assrt assrt = primaryAssrt();
    LogHyp[] hypothesisList = assrt.getLogHypArray();
    List<String> stmtStrings = new ArrayList<>();
    for (LogHyp hyp : hypothesisList) {
      stmtStrings.add(stmtToString(hyp));
    }
    stmtStrings.add(stmtToString(assrt));
    stmtStrings.add(distinctVarsStatement());
    return stmtStrings.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"));
  }

  static List<String> getLookupTerms(Stmt stmt) {
    return getLookupTermsForParseNode(stmt.getExprParseTree().getRoot());
  }

  private static String stmtToString(Stmt stmt) {
    String startTag = stmt instanceof LogHyp ? "$e" : "$p";
    return String.join(" ", stmt.getLabel(), startTag, stmt.getFormula().toString(), "$.");
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
}
