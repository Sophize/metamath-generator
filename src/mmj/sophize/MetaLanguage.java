package mmj.sophize;

import java.util.*;
import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

public enum MetaLanguage {
    INFORMAL, METAMATH;

    @JsonValue
    public String toValue() {
        switch (this) {
        case INFORMAL: return "INFORMAL";
        case METAMATH: return "METAMATH";
        }
        return null;
    }

    @JsonCreator
    public static MetaLanguage forValue(String value) throws IOException {
        if (value.equals("INFORMAL")) return INFORMAL;
        if (value.equals("METAMATH")) return METAMATH;
        throw new IOException("Cannot deserialize MetaLanguage");
    }
}
