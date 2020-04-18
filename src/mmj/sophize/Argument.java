package mmj.sophize;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Argument extends Resource {
  private String argumentText;
  private String conclusion;
  private String[] lookupTerms;
  private String[] premises;

  @JsonProperty("argumentText")
  public String getArgumentText() {
    return argumentText;
  }

  @JsonProperty("argumentText")
  public void setArgumentText(String value) {
    this.argumentText = value;
  }

  @JsonProperty("conclusion")
  public String getConclusion() {
    return conclusion;
  }

  @JsonProperty("conclusion")
  public void setConclusion(String value) {
    this.conclusion = value;
  }

  @JsonProperty("lookupTerms")
  public String[] getLookupTerms() {
    return lookupTerms;
  }

  @JsonProperty("lookupTerms")
  public void setLookupTerms(String[] value) {
    this.lookupTerms = value;
  }

  @JsonProperty("premises")
  public String[] getPremises() {
    return premises;
  }

  @JsonProperty("premises")
  public void setPremises(String[] value) {
    this.premises = value;
  }
}
