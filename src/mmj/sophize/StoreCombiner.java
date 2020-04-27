package mmj.sophize;

import mmj.lang.Assrt;
import mmj.lang.Stmt;
import mmj.sophize.ioutils.*;
import mmj.verify.Grammar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static mmj.sophize.TempTerm.createPrimitiveMetamathTerm;
import static mmj.sophize.ioutils.ResourceUtils.toJsonString;

public class StoreCombiner {
  private static final String OUTPUT_DIRECTORY = "output";
  private static List<ResourceStore> STORES = new ArrayList<>();
  private static final String databaseNames[] = {"set.mm", "iset.mm", "nf.mm"};

  public static void processGrammar(Grammar grammar) {
    try {
      ResourceStore resourceStore = new ResourceStore(databaseNames[STORES.size()]);
      resourceStore.createResources(grammar);
      STORES.add(resourceStore);
      if (STORES.size() == 2) StoreCombiner.combineAndWriteStores(STORES);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  private static void combineAndWriteStores(List<ResourceStore> stores) throws Exception {
    ResourceStore firstStore = stores.get(0);
    Map<String, TempTerm> combinedTerms = firstStore.termData;
    for (int i = 1; i < stores.size(); i++) {
      combinedTerms = combineTerms(combinedTerms, stores.get(i).termData);
    }

    Map<String, TempProposition> combinedPropositions = firstStore.propositionData;
    Map<String, TempArgument> combinedArguments = firstStore.argumentData;

    Map<String, Beliefset> beliefsets = new HashMap<>();
    fixAxioms(firstStore.defaultBeliefset, new HashMap<>());
    beliefsets.put("default", firstStore.defaultBeliefset);

    for (int i = 1; i < stores.size(); i++) {
      ResourceStore store = stores.get(i);
      Map<String, String> dedupedPropsIdMap = new TreeMap<>();
      String dedupPostfix = store.getDedupPostfix();
      combinedPropositions =
          combinePropositions(
              combinedPropositions, store.propositionData, dedupPostfix, dedupedPropsIdMap);
      combinedArguments =
          combineArguments(
              combinedArguments,
              store.argumentData,
              dedupPostfix,
              dedupedPropsIdMap,
              combinedPropositions);
      fixAxioms(store.defaultBeliefset, dedupedPropsIdMap);
      beliefsets.put(store.defaultBeliefset.getNames()[0], store.defaultBeliefset);
    }
    writeResources(
        combinedTerms,
        combinedPropositions,
        combinedArguments,
        beliefsets,
        ResourceStore.latexdefMap);
  }

  private static void fixAxioms(Beliefset beliefset, Map<String, String> dedupedPropsIdMap) {
    for (int i = 0; i < beliefset.getUnsupportedPropositionPtrs().length; i++) {
      String label = beliefset.getUnsupportedPropositionPtrs()[i].substring(3); // Remove the "#P_"

      String reference = "#P_" + dedupedPropsIdMap.getOrDefault(label, label);
      beliefset.getUnsupportedPropositionPtrs()[i] = reference;
    }
  }

  private static void writeResources(
      Map<String, TempTerm> termData,
      Map<String, TempProposition> propositionData,
      Map<String, TempArgument> argumentData,
      Map<String, Beliefset> beliefsets,
      Map<String, String> latexdefMap)
      throws IOException {
    String directory = getDirectory(Term.class);
    for (Map.Entry<String, Term> entry : getHardcodedTerms().entrySet()) {
      ResourceUtils.writeJson(directory, entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, TempTerm> entry : termData.entrySet()) {
      ResourceUtils.writeJson(directory, entry.getKey(), entry.getValue().getTerm(latexdefMap));
    }

    directory = getDirectory(Proposition.class);
    for (Map.Entry<String, TempProposition> entry : propositionData.entrySet()) {
      ResourceUtils.writeJson(
          directory, entry.getKey(), entry.getValue().getProposition(latexdefMap));
    }

    directory = getDirectory(Argument.class);
    for (Map.Entry<String, TempArgument> entry : argumentData.entrySet()) {
      ResourceUtils.writeJson(
          directory, entry.getKey(), entry.getValue().getArgument(latexdefMap, propositionData));
    }

    directory = getDirectory(Beliefset.class);
    for (Map.Entry<String, Beliefset> entry : beliefsets.entrySet()) {
      ResourceUtils.writeJson(directory, entry.getKey(), entry.getValue());
    }
  }

  private static Map<String, Term> getHardcodedTerms() {
    Term wffTerm = createPrimitiveMetamathTerm("wff", "A well formed formula");
    Term classTerm =
        createPrimitiveMetamathTerm(
            "class",
            "An expression that is a syntactically valid class expression. All valid set "
                + "expressions are also valid class expression, so expressions of sets normally "
                + "have the class typecode. Use the class typecode, not the setvar typecode, for "
                + "the type of set expressions unless you are specifically identifying a single "
                + "set variable.");
    Term setvarTerm =
        createPrimitiveMetamathTerm(
            "setvar",
            "Individual set variable type. Note that this is not the type of an arbitrary "
                + "set expression, instead, it is used to ensure that there is only a single "
                + "symbol used after quantifiers like for-all (∀) and there-exists (∃).");

    return Map.of("wff", wffTerm, "class", classTerm, "setvar", setvarTerm);
  }

  private static String getDirectory(Class tClass) throws IOException {
    Path dirPath = Paths.get(OUTPUT_DIRECTORY, tClass.getSimpleName().toLowerCase());
    Files.createDirectories(dirPath);
    return dirPath.toString();
  }

  private static Map<String, TempTerm> combineTerms(
      Map<String, TempTerm> terms1, Map<String, TempTerm> terms2) {
    Map<String, TempTerm> combined = new HashMap<>();
    for (Map.Entry<String, TempTerm> entry : terms1.entrySet()) {
      String id = entry.getKey();
      TempTerm term1 = entry.getValue();
      TempTerm term2 = terms2.get(id);
      if (term2 == null) {
        combined.put(id, term1);
      } else {
        combined.put(id, mergeTerms(term1, term2));
      }
    }
    for (Map.Entry<String, TempTerm> entry : terms2.entrySet()) {
      String id = entry.getKey();
      TempTerm term2 = entry.getValue();
      if (!terms1.containsKey(id)) {
        combined.put(id, term2);
      }
    }
    return combined;
  }

  private static Map<String, TempProposition> combinePropositions(
      Map<String, TempProposition> props1,
      Map<String, TempProposition> props2,
      String dedupPostfix,
      Map<String, String> dedupedPropsIdMap) {
    Map<String, TempProposition> combined = new HashMap<>();
    for (Map.Entry<String, TempProposition> entry : props1.entrySet()) {
      String id = entry.getKey();
      TempProposition prop1 = entry.getValue();
      TempProposition prop2 = props2.get(id);
      if (prop2 == null) {
        combined.put(id, prop1);
      } else {
        TempProposition merged = mergePropositions(prop1, prop2);
        if (merged != null) {
          combined.put(id, merged);
          for (int i = 0; i < merged.assrts.get(0).getLogHypArray().length; i++) {
            String newHypId = prop2.assrts.get(0).getLogHypArray()[i].getLabel();
            String mergedHypId = merged.assrts.get(0).getLogHypArray()[i].getLabel();
            if (newHypId.equals(mergedHypId)) continue;
            // Props are merged only when the order of hypothesis is same in original and alternate.
            dedupedPropsIdMap.put(newHypId, mergedHypId);
          }
        } else {
          combined.put(id, prop1);
          combined.put(id + dedupPostfix, prop2);
          dedupedPropsIdMap.put(id, id + dedupPostfix);
        }
      }
    }
    for (Map.Entry<String, TempProposition> entry : props2.entrySet()) {
      String id = entry.getKey();
      if (!props1.containsKey(id)) combined.put(id, entry.getValue());
    }
    return combined;
  }

  private static Map<String, TempArgument> combineArguments(
      Map<String, TempArgument> args1,
      Map<String, TempArgument> args2,
      String dedupPostfix,
      Map<String, String> dedupedPropsIdMap,
      Map<String, TempProposition> propositionData) {
    Map<String, TempArgument> combined = new HashMap<>();
    for (Map.Entry<String, TempArgument> entry : args1.entrySet()) {
      String id = entry.getKey();
      TempArgument arg1 = entry.getValue();
      TempArgument arg2 = args2.get(id);
      if (arg2 == null) {
        combined.put(id, arg1);
      } else {
        // TODO: check if bug: what if conclusion or premise has same id but has been deduped?
        arg2.setDedupedPropsIdMap(dedupedPropsIdMap);
        TempArgument merged = mergeArguments(arg1, arg2, propositionData);
        if (merged != null) combined.put(id, merged);
        else {
          combined.put(id, arg1);
          combined.put(id + dedupPostfix, arg2);
        }
      }
    }

    for (Map.Entry<String, TempArgument> entry : args2.entrySet()) {
      String id = entry.getKey();
      if (!args1.containsKey(id)) {
        TempArgument arg = entry.getValue();
        arg.setDedupedPropsIdMap(dedupedPropsIdMap);
        combined.put(id, entry.getValue());
      }
    }
    return combined;
  }

  private static TempTerm mergeTerms(TempTerm term1, TempTerm term2) {
    Map<String, String> latexdefMap = ResourceStore.latexdefMap;
    if (!term1.getPhrase(latexdefMap).equals(term2.getPhrase(latexdefMap))
        || !term1.getDefinition(latexdefMap).equals(term2.getDefinition(latexdefMap))
        || !term1.getAssignableId().equals(term2.getAssignableId())) {
      System.out.println("Languages are different, merge not possible.");
      throw new IllegalStateException("Languages are different, merge not possible.");
    }
    List<Stmt> syntaxes = new ArrayList<>(term1.syntaxes);
    syntaxes.addAll(term2.syntaxes);
    List<Stmt> defns = new ArrayList<>(term1.defns);
    defns.addAll(term2.defns);
    return new TempTerm(syntaxes, defns, term1.sym, term1.getAssignableId());
  }

  private static TempProposition mergePropositions(TempProposition prop1, TempProposition prop2) {
    if (!Helpers.areSameProps(prop1.assrts.get(0), prop2.assrts.get(0))
        || !prop1.distinctVarsString.equals(prop2.distinctVarsString)) return null;
    List<Assrt> assrts = new ArrayList<>(prop1.assrts);
    assrts.addAll(prop2.assrts);
    return new TempProposition(assrts, prop1.distinctVarsString);
  }

  private static TempArgument mergeArguments(
      TempArgument arg1, TempArgument arg2, Map<String, TempProposition> propositionData) {
    Map<String, String> latexdefMap = ResourceStore.latexdefMap;
    try {
      String arg1Str = toJsonString(arg1.getArgument(latexdefMap, propositionData));
      String arg2Str = toJsonString(arg2.getArgument(latexdefMap, propositionData));
      return arg1Str.equals(arg2Str) ? arg1 : null;
    } catch (Exception e) {
      return null;
    }
  }
}
