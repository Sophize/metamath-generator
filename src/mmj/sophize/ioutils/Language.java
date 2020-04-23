package mmj.sophize.ioutils;

import java.io.IOException;
import com.fasterxml.jackson.annotation.*;

public enum Language {
    INFORMAL, METAMATH_SET_MM;

    @JsonValue
    public String toValue() {
        switch (this) {
        case INFORMAL: return "INFORMAL";
        case METAMATH_SET_MM: return "METAMATH_SET_MM";
        }
        return null;
    }

    @JsonCreator
    public static Language forValue(String value) throws IOException {
        if (value.equals("INFORMAL")) return INFORMAL;
        if (value.equals("METAMATH_SET_MM")) return METAMATH_SET_MM;
        throw new IOException("Cannot deserialize Language");
    }
}
