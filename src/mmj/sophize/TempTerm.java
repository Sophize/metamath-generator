package mmj.sophize;

import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.VarHyp;
import mmj.sophize.ioutils.Citation;
import mmj.sophize.ioutils.Language;
import mmj.sophize.ioutils.MetaLanguage;
import mmj.sophize.ioutils.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static mmj.sophize.Helpers.*;

class TempTerm {

  List<Stmt> syntaxes = new ArrayList<>();

  List<Stmt> defns = new ArrayList<>();

  Sym sym;

  private String assignableId;

  TempTerm(Stmt syntax, Stmt defn, Sym sym, String assignableId) {
    if (syntax != null) this.syntaxes.add(syntax);
    if (defn != null) this.defns.add(defn);
    this.sym = sym;
    this.assignableId = assignableId;
  }

  TempTerm(List<Stmt> syntaxes, List<Stmt> defns, Sym sym, String assignableId) {
    this.syntaxes = syntaxes;
    this.defns = defns;
    this.sym = sym;
    this.assignableId = assignableId;
  }

  Term getTerm(Map<String, String> latexdefMap) {
    return createMetamathTerm(
        getPhrase(latexdefMap),
        getDefinition(latexdefMap),
        syntaxes.get(0).getLabel(),
        getRemarks());
  }

  String getPhrase(Map<String, String> latexdefMap) {
    if (primarySyntax() instanceof VarHyp) {
      String varId = primarySyntax().getFormula().getSym()[1].getId();
      return "$" + latexdefMap.get(varId) + "$";
    } else {
      return "$" + latexdefMap.get(sym.getId()) + "$";
    }
  }

  String getRemarks() {
    StringBuilder remarks = new StringBuilder();

    remarks.append(
        combineIfNotIdentical(
            syntaxes.stream()
                .map(syntax -> toResourceRemark(syntax.getDescription()) + "\n\n---")
                .collect(Collectors.toList()),
            ""));

    List<String> defnLabels =
        defns.stream().map(Stmt::getLabel).distinct().collect(Collectors.toList());
    if (defnLabels.size() == 1) {
      String definitionLink = "#P_" + defnLabels.get(0) + "|NAV_LINK|HIDE_TVI";
      remarks
          .append("\n\nThis syntax is primitive. The first axiom using it is ")
          .append(definitionLink)
          .append(".\n\n---");
    } else if (defnLabels.size() > 1) {
      remarks.append(
          "\n\n---\n\nThis syntax is primitive. It may be defined by the following axioms: \n");
      for (String label : defnLabels) {
        String definitionLink = "#P_" + label + "|NAV_LINK|HIDE_TVI";
        remarks.append(definitionLink).append("\n");
      }
    }
    return remarks.toString();
  }

  private Stmt primarySyntax() {
    return syntaxes.get(0);
  }

  String getDefinition(Map<String, String> latexdefMap) {
    if (syntaxes instanceof VarHyp) {
      String typecode = primarySyntax().getFormula().getSym()[0].getId();
      return "A " + getTermWithDisplayPhrase(typecode, typecode) + " variable.";
    } else {
      return getStatementForParseTree(primarySyntax().getExprParseTree(), "wff", latexdefMap);
    }
  }

  String getAssignableId() {
    return assignableId;
  }

  static String combineIfNotIdentical(List<String> strs, String delimiter) {
    if (strs.stream().distinct().count() <= 1) return strs.get(0);
    // TODO: Use better combination strategy
    return String.join(delimiter, strs);
  }

  static Term createPrimitiveMetamathTerm(String phrase, String remarks) {
    Term term = createMetamathTerm(phrase, "", null, remarks);
    term.setPrimitive(true);
    Citation bookCitation = new Citation();
    bookCitation.setTextCitation(
        "Section 4.2.5 of [metamath book](http://us.metamath.org/#book) p.125");
    term.setCitations(new Citation[] {bookCitation});
    return term;
  }

  private static Term createMetamathTerm(
      String phrase, String definition, String stmtLabel, String remarks) {
    Term term = new Term();
    term.setLanguage(Language.METAMATH_SET_MM);
    term.setMetaLanguage(MetaLanguage.METAMATH);
    term.setPhrase(phrase);
    term.setDefinition(definition);
    term.setRemarks(remarks);
    if (stmtLabel != null) term.setCitations(new Citation[] {getCitation(stmtLabel)});
    return term;
  }
}
