package mmj.sophize.ioutils;

import java.util.*;
import com.fasterxml.jackson.annotation.*;

public class Citation {
    private String textCitation;

    @JsonProperty("textCitation")
    public String getTextCitation() { return textCitation; }
    @JsonProperty("textCitation")
    public void setTextCitation(String value) { this.textCitation = value; }
}
