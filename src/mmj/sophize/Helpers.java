package mmj.sophize;

import mmj.lang.*;
import mmj.sophize.ioutils.Citation;
import mmj.sophize.ioutils.Language;
import mmj.sophize.ioutils.MetaLanguage;
import mmj.sophize.ioutils.Term;

import java.util.*;

class Helpers {
  static final String DEDUP_POSTFIX = "--";
  private static final Map<String, String> TYPE_TO_COLOR_LATEX =
      Map.of(
          "wff", "\\color{blue}",
          "class", "\\color{#C3C}",
          "setvar", "\\color{red}");

  static boolean areSameProps(Assrt a1, Assrt a2) {
    if (!a1.getFormula().toString().equals(a2.getFormula().toString())) return false;
    if (a1.getLogHypArray().length != a2.getLogHypArray().length) return false;

    for (int i = 0; i < a2.getLogHypArray().length; i++) {
      // If the order of same set of hypothesis is different in original and alternate, we treat
      // them as different propositions. We may want to change this in the future.
      if (!getFormulaForHypothesis(a1, i).equals(getFormulaForHypothesis(a2, i))) return false;
    }
    return true;
  }

  static Citation getCitation(String label) {
    Citation citation = new Citation();
    String citationText = "See " + label + " on Metamath";
    // TODO: this doesn't work for resources outside set.mm
    String citationLink = "http://us.metamath.org/mpegif/" + label + ".html";
    citation.setTextCitation("[" + citationText + "](" + citationLink + ")");
    return citation;
  }

  static String varToString(VarHyp hyp, Map<String, String> latexdefMap) {
    Cnst type = hyp.getTyp();
    Var var = (Var) hyp.getFormula().getSym()[1];
    String displayPhrase =
        "$" + TYPE_TO_COLOR_LATEX.get(type.getId()) + latexdefMap.get(var.getId()) + "$";
    return getTermWithDisplayPhrase(hyp.getLabel(), displayPhrase);
  }

  static long getNumConstChars(Stmt wffStatement) {
    return Arrays.stream(wffStatement.getFormula().getSym())
        .skip(1) // we don't want to count the typecode.
        .filter(sym -> !sym.getId().equals("(") && !sym.getId().equals(")"))
        .filter(sym -> !(sym instanceof Var))
        .count();
  }

  static String toResourceRemark(String description) {
    if (description == null) return "";
    String withoutNewlines = description.replaceAll("\\n\\s+", " ");
    return withoutNewlines;
    // TODO: labels be preceded with a tilde (~) and math symbol tokens be enclosed in grave
    // accents (` `)
  }

  static String getTermWithDisplayPhrase(String assignableId, String displayPhrase) {
    return "#(T_" + assignableId + ", '" + displayPhrase + "')";
  }

  static Stmt findDefinition(List<Stmt> orderedStmts, int startingIndex, Sym symId) {
    for (int i = startingIndex; i < orderedStmts.size(); i++) {
      Stmt stmt = orderedStmts.get(i);
      if (!(stmt instanceof Axiom)) {
        continue;
      }
      Sym[] formula = stmt.getFormula().getSym();
      if (!formula[0].getId().equals("|-")) continue;
      for (Sym formulaSym : formula) {
        if (formulaSym.getId().equals(symId.getId())) {
          return stmt;
        }
      }
    }
    return null;
  }

  static String getAssignableIdForTermInStmt(String stmtLabel, int termIndex, long totalTerm) {
    return stmtLabel + ((totalTerm > 1) ? (DEDUP_POSTFIX + (termIndex + 1)) : "");
  }

  static String getStatementForParseNode(ParseNode node, Map<String, String> latexdefMap) {
    List<String> childNodes = new ArrayList<>();
    for (ParseNode childNode : node.child) {
      childNodes.add(getStatementForParseNode(childNode, latexdefMap));
    }

    Sym[] formula = node.stmt.getFormula().getSym();

    if (node.stmt instanceof VarHyp) return varToString((VarHyp) node.stmt, latexdefMap);

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
    long numConstItems = getNumConstChars(node.stmt);

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

  static String getStatementForParseTree(
      ParseTree tree, String typeCodeLatex, Map<String, String> latexdefMap) {
    ParseNode root = tree.getRoot();
    return "$\\scriptsize \\color{#999}"
        + typeCodeLatex
        + "$ "
        + getStatementForParseNode(root, latexdefMap).replace("$$", "");
  }

  static <K, V> void putIfAbsent(Map<K, V> map, K key, V value) {
    myAssert(map.putIfAbsent(key, value) == null);
  }

  static void myAssert(boolean val) {
    if (!val) {
      throw new IllegalStateException("val");
    }
  }

  private static String getFormulaForHypothesis(Assrt assrt, int index) {
    return assrt.getLogHypArray()[index].getFormula().toString();
  }
}
