package mmj.sophize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ResourceUtils {
  private static final Map<Class, String> CLASS_TO_RESOURCE_CODE = new HashMap<>();
  private static final Map<String, Class> RESOURCE_TO_CLASS_CODE = new HashMap<>();
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  static {
    CLASS_TO_RESOURCE_CODE.put(Term.class, "T");
    CLASS_TO_RESOURCE_CODE.put(Proposition.class, "P");
    CLASS_TO_RESOURCE_CODE.put(Argument.class, "A");
    CLASS_TO_RESOURCE_CODE.put(Beliefset.class, "B");
    CLASS_TO_RESOURCE_CODE.put(Article.class, "R");
    CLASS_TO_RESOURCE_CODE.put(Project.class, "J");
    for (Map.Entry<Class, String> entry : CLASS_TO_RESOURCE_CODE.entrySet()) {
      RESOURCE_TO_CLASS_CODE.put(entry.getValue(), entry.getKey());
    }
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static Path getFilePath(String directory, String assignableId, Class resourceClass) {
    String filename = CLASS_TO_RESOURCE_CODE.get(resourceClass) + "_" + assignableId + ".json";
    return Paths.get(directory, filename);
  }

  public static void writeJson(String directory, String assignableId, Resource resource)
      throws IOException {
    Files.writeString(
        getFilePath(directory, assignableId, resource.getClass()), toJsonString(resource));
  }

  public static String toJsonString(Resource resource) throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(resource);
  }

  public static Resource readResource(String filename) throws IOException {
    String jsonData = Files.readString(Paths.get(filename));
    ObjectMapper objectMapper = new ObjectMapper();
    String resourceCode = filename.substring(0, 1);
    return (Resource) objectMapper.readValue(jsonData, RESOURCE_TO_CLASS_CODE.get(resourceCode));
  }

  private ResourceUtils() {}
}
