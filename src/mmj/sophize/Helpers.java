package mmj.sophize;

import mmj.lang.*;
import org.sophize.datamodel.Citation;

import java.util.*;

class Helpers {
  static final String DEDUP_POSTFIX = "--";

  static boolean areSameProps(TempProposition t1, TempProposition t2) {
    Assrt a1 = t1.primaryAssrt();
    Assrt a2 = t2.primaryAssrt();

    if (a1 instanceof Axiom != a2 instanceof Axiom) return false;
    if (!t1.distinctVarsStatement().equals(t2.distinctVarsStatement())) return false;
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

  static String varToLookupTerm(VarHyp hyp) {
    return "#T_" + hyp.getLabel();
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

  static List<String> getLookupTermsForParseNode(ParseNode node) {
    List<List<String>> childNodes = new ArrayList<>();
    for (ParseNode childNode : node.child) {
      childNodes.add(getLookupTermsForParseNode(childNode));
    }

    Sym[] formula = node.stmt.getFormula().getSym();

    if (node.stmt instanceof VarHyp) return Arrays.asList(varToLookupTerm((VarHyp) node.stmt));

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
    List<String> lookupTerms = new ArrayList<>();
    for (int symIndex = 1; symIndex < formula.length; symIndex++) {
      Sym sym = formula[symIndex];
      String id = sym.getId();
      if (sym instanceof Cnst) {
        if (id.equals("(") || id.equals(")")) continue;

        String assignableId =
            getAssignableIdForTermInStmt(node.stmt.getLabel(), constItemIndex, numConstItems);
        lookupTerms.add("#T_" + assignableId);
        constItemIndex++;
      } else {
        myAssert(sym instanceof Var);
        Integer index = varIdToChildIndex.get(id);
        myAssert(index != null);
        lookupTerms.addAll(childNodes.get(index));
      }
    }
    return lookupTerms;
  }

  static <K, V> void putIfNotDifferent(Map<K, V> map, K key, V value) {
    V existing = map.putIfAbsent(key, value);

    myAssert(existing == null || value.equals(existing));
  }

  static void myAssert(boolean val) {
    if (!val) {
      throw new IllegalStateException("This shouldn't happen!");
    }
  }

  private static String getFormulaForHypothesis(Assrt assrt, int index) {
    return assrt.getLogHypArray()[index].getFormula().toString();
  }
}
